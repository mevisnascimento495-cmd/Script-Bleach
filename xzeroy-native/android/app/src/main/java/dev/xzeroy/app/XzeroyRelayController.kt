package dev.xzeroy.app

import android.content.Context
import android.content.SharedPreferences
import dev.sora.relay.MinecraftRelay
import dev.sora.relay.MinecraftRelayListener
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.cheat.value.BoolValue
import dev.sora.relay.cheat.value.ChoiceValue
import dev.sora.relay.cheat.value.FloatValue
import dev.sora.relay.cheat.value.IntRangeValue
import dev.sora.relay.cheat.value.IntValue
import dev.sora.relay.cheat.value.ListValue
import dev.sora.relay.cheat.value.StringValue
import dev.sora.relay.cheat.value.Value
import com.google.gson.JsonParser
import dev.sora.relay.game.GameSession
import dev.sora.relay.session.MinecraftRelaySession
import dev.sora.relay.session.listener.RelayListenerAutoCodec
import dev.sora.relay.session.listener.RelayListenerEncryptedSession
import dev.sora.relay.session.listener.RelayListenerNetworkSettings
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

enum class RelayStatus(val label: String) { STOPPED("Parado"), STARTING("Iniciando"), RUNNING("Ativo"), STOPPING("Encerrando"), FAILED("Falhou") }
data class RelaySnapshot(val status: RelayStatus = RelayStatus.STOPPED, val targetHost: String = "", val targetPort: Int = 19132, val localPort: Int = 19136, val message: String = "Relay parado.")
data class EngineValueSnapshot(
    val name: String,
    val type: String,
    val currentValue: String,
    val choices: List<String> = emptyList(),
    val hint: String = ""
)

/** Adapter that connects the Android panel to the GPL-licensed ProtoHax relay. */
class XzeroyRelayController private constructor(context: Context) {
    companion object {
        @Volatile private var singleton: XzeroyRelayController? = null
        fun get(context: Context): XzeroyRelayController = singleton ?: synchronized(this) {
            singleton ?: XzeroyRelayController(context.applicationContext).also { singleton = it }
        }
    }

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences("xzeroy_settings", Context.MODE_PRIVATE)
    private val lock = Any()
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "XZeroy-Relay-Control").apply { isDaemon = true } }
    private val listeners = CopyOnWriteArrayList<(RelaySnapshot) -> Unit>()
    @Volatile private var current = RelaySnapshot()
    @Volatile private var relay: MinecraftRelay? = null
    @Volatile private var moduleManager: ModuleManager? = null
    @Volatile private var clientSessionAttached = false

    @Suppress("unused")
    private val moduleSubscription = XzeroyModuleEvents.observe { change ->
        worker.execute {
            if (!clientSessionAttached) return@execute
            moduleManager?.let { applyModuleState(it, change.moduleId, change.enabled) }
        }
    }

    fun snapshot(): RelaySnapshot = current

    fun engineValues(moduleId: String): List<EngineValueSnapshot>? {
        val manager = moduleManager ?: return null
        val definition = XzeroyModuleRegistry.find(moduleId) ?: return null
        val className = definition.engineClass ?: return null
        val module = manager.modules.firstOrNull { it.javaClass.simpleName == className } ?: return null
        return module.visibleValues.map { value ->
            when (value) {
                is BoolValue -> EngineValueSnapshot(value.name, "boolean", value.value.toString())
                is ChoiceValue -> EngineValueSnapshot(value.name, "choice", value.value.choiceName, value.values.map { it.choiceName })
                is ListValue<*> -> EngineValueSnapshot(value.name, "choice", value.value.choiceName, value.values.map { it.choiceName })
                is IntRangeValue -> EngineValueSnapshot(value.name, "range", value.value.first.toString() + ".." + value.value.last, hint = value.range.first.toString() + ".." + value.range.last)
                is IntValue -> EngineValueSnapshot(value.name, "integer", value.value.toString(), hint = value.range.first.toString() + "–" + value.range.last)
                is FloatValue -> EngineValueSnapshot(value.name, "decimal", value.value.toString(), hint = value.range.start.toString() + "–" + value.range.endInclusive.toString())
                is StringValue -> EngineValueSnapshot(value.name, "text", value.value, hint = "Texto")
                else -> EngineValueSnapshot(value.name, "text", value.value.toString(), hint = "Valor")
            }
        }
    }

    /** Returns null on success or a useful validation error. */
    fun saveEngineValues(moduleId: String, values: Map<String, String>): String? {
        val manager = moduleManager ?: return "Inicie o Relay primeiro para carregar a configuração interna da engine."
        val definition = XzeroyModuleRegistry.find(moduleId) ?: return "Módulo desconhecido."
        val className = definition.engineClass ?: return "Este módulo não usa a engine ProtoHax."
        val module = manager.modules.firstOrNull { it.javaClass.simpleName == className } ?: return "Módulo indisponível na engine."

        val allValues = module.values.associateBy { it.name }
        val prepared = values.map { (name, raw) ->
            val value = allValues[name] ?: return "A opção '" + name + "' não existe mais nesse módulo."
            if (!validateEngineValue(value, raw)) {
                return "Valor inválido para '" + name + "'. Confira o tipo, as opções e os limites exibidos."
            }
            value to raw
        }
        val backup = prepared.associate { (value, _) -> value to value.toJson()?.toString() }
        try {
            prepared.forEach { (value, raw) -> value.fromString(raw) }
        } catch (error: Throwable) {
            backup.forEach { (value, raw) ->
                if (raw != null) runCatching { value.fromJson(JsonParser.parseString(raw)) }
            }
            return "Não foi possível aplicar os valores: " + (error.message?.take(120) ?: error.javaClass.simpleName)
        }

        val editor = prefs.edit()
        prepared.forEach { (value, _) ->
            val json = value.toJson()?.toString() ?: return "A engine não conseguiu serializar '" + value.name + "'."
            editor.putString(engineValueKey(moduleId, value.name), json)
        }
        editor.apply()
        return null
    }

    fun resetEngineValues(moduleId: String): Boolean {
        val manager = moduleManager ?: return false
        val definition = XzeroyModuleRegistry.find(moduleId) ?: return false
        val className = definition.engineClass ?: return false
        val module = manager.modules.firstOrNull { it.javaClass.simpleName == className } ?: return false
        val editor = prefs.edit()
        module.values.forEach { value ->
            value.reset()
            editor.remove(engineValueKey(moduleId, value.name))
        }
        editor.apply()
        return true
    }

    fun reloadStoredEngineValues(): Boolean {
        val manager = moduleManager ?: return false
        applyStoredEngineValues(manager)
        return true
    }

    private fun engineValueKey(moduleId: String, name: String): String =
        "engine_value::" + moduleId + "::" + name

    private fun validateEngineValue(value: Value<*>, rawValue: String): Boolean {
        val raw = rawValue.trim()
        return when (value) {
            is BoolValue -> raw.equals("true", true) || raw.equals("false", true) || raw.equals("on", true) || raw.equals("off", true)
            is ChoiceValue -> value.values.any { it.choiceName.equals(raw, true) }
            is ListValue<*> -> value.values.any { it.choiceName.equals(raw, true) }
            is IntRangeValue -> {
                val parts = raw.split("..")
                if (parts.size != 2) false else {
                    val first = parts[0].trim().toIntOrNull()
                    val last = parts[1].trim().toIntOrNull()
                    first != null && last != null && first <= last && first in value.range && last in value.range
                }
            }
            is IntValue -> raw.toIntOrNull()?.let { it in value.range } == true
            is FloatValue -> raw.toFloatOrNull()?.let { it.isFinite() && it >= value.range.start && it <= value.range.endInclusive } == true
            is StringValue -> rawValue.length <= 512
            else -> rawValue.length <= 512
        }
    }

    private fun applyStoredEngineValues(manager: ModuleManager) {
        manager.modules.forEach { engineModule ->
            val definition = XzeroyModuleRegistry.all.firstOrNull { it.engineClass == engineModule.javaClass.simpleName } ?: return@forEach
            engineModule.values.forEach { value ->
                val raw = prefs.getString(engineValueKey(definition.id, value.name), null) ?: return@forEach
                runCatching { value.fromJson(JsonParser.parseString(raw)) }
            }
        }
    }

    fun observe(listener: (RelaySnapshot) -> Unit): AutoCloseable {
        listeners.add(listener)
        runCatching { listener(current) }
        return AutoCloseable { listeners.remove(listener) }
    }

    fun start(targetHost: String, targetPort: Int, localPort: Int): Boolean {
        val host = targetHost.trim()
        if (host.isBlank() || host.any { it.isWhitespace() } || targetPort !in 1..65535 || localPort !in 1024..65535) return false
        synchronized(lock) {
            if (current.status == RelayStatus.STARTING || current.status == RelayStatus.RUNNING || current.status == RelayStatus.STOPPING) return false
            update(RelaySnapshot(RelayStatus.STARTING, host, targetPort, localPort, "Resolvendo destino e preparando a sessão Bedrock…"))
        }
        worker.execute {
            var candidate: MinecraftRelay? = null
            try {
                val target = InetSocketAddress(InetAddress.getByName(host), targetPort)
                val game = GameSession()
                val manager = ModuleManager(game).apply { init() }
                applyStoredEngineValues(manager)
                moduleManager = manager
                clientSessionAttached = false
                candidate = MinecraftRelay(object : MinecraftRelayListener {
                    override fun onSessionCreation(session: MinecraftRelaySession): InetSocketAddress {
                        session.listeners.add(RelayListenerNetworkSettings(session))
                        session.listeners.add(RelayListenerAutoCodec(session))
                        game.netSession = session
                        session.listeners.add(game)
                        // This listener creates an offline identity; it is not Xbox Live authentication.
                        session.listeners.add(RelayListenerEncryptedSession(session))
                        clientSessionAttached = true
                        applyConfiguredModuleStates(manager)
                        return target
                    }
                })
                relay = candidate
                candidate.bind(InetSocketAddress("0.0.0.0", localPort))
                update(current.copy(status = RelayStatus.RUNNING, message = "Relay UDP ativo. Conecte o Minecraft em 127.0.0.1:${localPort}."))
            } catch (error: Throwable) {
                runCatching { candidate?.takeIf { it.isRunning }?.stop() }
                relay = null
                clientSessionAttached = false
                moduleManager = null
                update(current.copy(status = RelayStatus.FAILED, message = "Falha ao iniciar o relay: " + (error.message?.take(180)?.ifBlank { null } ?: error.javaClass.simpleName)))
            }
        }
        return true
    }

    fun stop(): Boolean {
        synchronized(lock) {
            if (current.status == RelayStatus.STOPPED || current.status == RelayStatus.STOPPING) return false
            update(current.copy(status = RelayStatus.STOPPING, message = "Encerrando o relay…"))
        }
        worker.execute {
            clientSessionAttached = false
            try {
                relay?.let { if (it.isRunning) it.stop() }
                relay = null
                moduleManager = null
                update(current.copy(status = RelayStatus.STOPPED, message = "Relay parado."))
            } catch (error: Throwable) {
                relay = null
                moduleManager = null
                update(current.copy(status = RelayStatus.FAILED, message = "Erro ao encerrar o relay: " + (error.message?.take(160)?.ifBlank { null } ?: error.javaClass.simpleName)))
            }
        }
        return true
    }

    private fun applyConfiguredModuleStates(manager: ModuleManager) {
        XzeroyModuleRegistry.all.filter { it.engineClass != null && it.enabledPreference != null }.forEach {
            applyModuleState(manager, it.id, prefs.getBoolean(it.enabledPreference!!, false))
        }
    }
    private fun applyModuleState(manager: ModuleManager, id: String, enabled: Boolean) {
        val className = XzeroyModuleRegistry.find(id)?.engineClass ?: return
        manager.modules.firstOrNull { it.javaClass.simpleName == className }?.let { module -> runCatching { module.state = enabled } }
    }
    private fun update(value: RelaySnapshot) {
        current = value
        listeners.forEach { listener -> runCatching { listener(value) } }
    }
}

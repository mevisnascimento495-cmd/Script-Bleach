package dev.xzeroy.app

import android.content.Context
import android.content.SharedPreferences
import dev.sora.relay.MinecraftRelay
import dev.sora.relay.MinecraftRelayListener
import dev.sora.relay.cheat.module.ModuleManager
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

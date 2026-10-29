package dev.xzeroy.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList

enum class ModuleCategory(val label: String) {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    VISUAL("Visual"),
    MISC("Misc")
}

data class ModuleSettingDefinition(
    val key: String,
    val label: String,
    val defaultValue: Int,
    val minValue: Int,
    val maxValue: Int
)

data class XzeroyModuleDefinition(
    val id: String,
    val name: String,
    val description: String,
    val category: ModuleCategory,
    val implemented: Boolean,
    val enabledPreference: String? = null,
    val settings: List<ModuleSettingDefinition> = emptyList()
)

data class ModuleStateChanged(val moduleId: String, val enabled: Boolean)

object XzeroyModuleEvents {
    private val listeners = CopyOnWriteArrayList<(ModuleStateChanged) -> Unit>()

    fun observe(listener: (ModuleStateChanged) -> Unit): AutoCloseable {
        listeners.add(listener)
        return AutoCloseable { listeners.remove(listener) }
    }

    fun publish(moduleId: String, enabled: Boolean) {
        val event = ModuleStateChanged(moduleId, enabled)
        listeners.forEach { listener -> runCatching { listener(event) } }
    }
}

/**
 * Metadata is centralized here so the dashboard and floating overlay show the same modules.
 * Only modules backed by a real implementation are marked implemented=true.
 */
object XzeroyModuleRegistry {
    val all: List<XzeroyModuleDefinition> = listOf(
        XzeroyModuleDefinition(
            "auto_clicker", "Auto Clicker",
            "Envia toques repetidos em uma coordenada fixa enquanto o Minecraft está em primeiro plano.",
            ModuleCategory.COMBAT, true, "auto_clicker",
            listOf(
                ModuleSettingDefinition("click_x", "Coordenada X (%)", 82, 0, 100),
                ModuleSettingDefinition("click_y", "Coordenada Y (%)", 72, 0, 100),
                ModuleSettingDefinition("click_interval", "Intervalo (ms)", 180, 120, 2000)
            )
        ),
        XzeroyModuleDefinition(
            "kill_aura", "Kill Aura",
            "Motor de combate e seleção de entidades ainda não existe.",
            ModuleCategory.COMBAT, false
        ),
        XzeroyModuleDefinition(
            "velocity", "Velocity",
            "Manipulação de pacotes de knockback ainda não existe.",
            ModuleCategory.COMBAT, false
        ),
        XzeroyModuleDefinition(
            "reach", "Reach",
            "A camada de entidades e o motor de pacotes Bedrock ainda não estão conectados.",
            ModuleCategory.COMBAT, false
        ),
        XzeroyModuleDefinition(
            "target_lock", "Target Lock",
            "A seleção de alvos depende do futuro estado de entidades do jogo.",
            ModuleCategory.COMBAT, false
        ),
        XzeroyModuleDefinition(
            "auto_jump", "Auto Jump",
            "Envia toques periódicos na posição configurada para o botão de pulo.",
            ModuleCategory.MOVEMENT, true, "auto_jump",
            listOf(
                ModuleSettingDefinition("jump_x", "Coordenada X (%)", 92, 0, 100),
                ModuleSettingDefinition("jump_y", "Coordenada Y (%)", 55, 0, 100),
                ModuleSettingDefinition("jump_interval", "Intervalo (ms)", 700, 250, 5000)
            )
        ),
        XzeroyModuleDefinition("air_jump", "Air Jump", "Exige integração real com estado e movimento do jogador.", ModuleCategory.MOVEMENT, false),
        XzeroyModuleDefinition("fly", "Fly", "Exige suporte do motor de rede e compatibilidade com o servidor.", ModuleCategory.MOVEMENT, false),
        XzeroyModuleDefinition("speed", "Speed", "Exige integração com o movimento do jogador e o protocolo Bedrock.", ModuleCategory.MOVEMENT, false),
        XzeroyModuleDefinition("no_fall", "No Fall", "Exige uma implementação compatível com a versão de protocolo do jogo.", ModuleCategory.MOVEMENT, false),
        XzeroyModuleDefinition("clip", "Clip", "Não implementado: não altera colisão ou pacotes do jogo.", ModuleCategory.MOVEMENT, false),
        XzeroyModuleDefinition("blink", "Blink", "Não implementado: não intercepta nem enfileira pacotes de movimento.", ModuleCategory.MOVEMENT, false),
        XzeroyModuleDefinition("esp", "ESP", "A leitura de entidades e a camada de renderização de jogo ainda não existem.", ModuleCategory.VISUAL, false),
        XzeroyModuleDefinition("anti_blind", "Anti Blind", "Não há interceptação de efeitos de jogo nesta versão.", ModuleCategory.VISUAL, false),
        XzeroyModuleDefinition("hit_effect", "Hit Effect", "Os eventos de combate do jogo ainda não estão disponíveis.", ModuleCategory.VISUAL, false),
        XzeroyModuleDefinition("no_hurt_camera", "No Hurt Camera", "A câmera do Minecraft não é modificada pelo XZeroy.", ModuleCategory.VISUAL, false),
        XzeroyModuleDefinition("entity_info", "Entity Info", "O modelo de entidades do cliente ainda não foi integrado.", ModuleCategory.VISUAL, false),
        XzeroyModuleDefinition("fast_break", "Fast Break", "A interação com blocos por pacotes não está implementada.", ModuleCategory.MISC, false),
        XzeroyModuleDefinition("inventory_helper", "Inventory Helper", "A sincronização do inventário do Bedrock não está implementada.", ModuleCategory.MISC, false),
        XzeroyModuleDefinition("device_spoof", "Device Spoof", "Os dados de login de dispositivo ainda não são alterados.", ModuleCategory.MISC, false),
        XzeroyModuleDefinition("resource_pack", "Resource Pack Tools", "A negociação de resource packs não está implementada.", ModuleCategory.MISC, false),
        XzeroyModuleDefinition("world_download", "World Download", "A captura de chunks e a gravação de mundo ainda não existem.", ModuleCategory.MISC, false)
    )

    fun forCategory(category: ModuleCategory): List<XzeroyModuleDefinition> =
        all.filter { it.category == category }

    fun find(id: String): XzeroyModuleDefinition? = all.firstOrNull { it.id == id }
}

class XzeroySettingsStore(context: Context) {
    val preferences: SharedPreferences =
        context.getSharedPreferences("xzeroy_settings", Context.MODE_PRIVATE)

    fun isEnabled(module: XzeroyModuleDefinition): Boolean =
        module.enabledPreference?.let { preferences.getBoolean(it, false) } ?: false

    fun toggle(module: XzeroyModuleDefinition): Boolean {
        val key = module.enabledPreference ?: return false
        val enabled = !preferences.getBoolean(key, false)
        preferences.edit().putBoolean(key, enabled).apply()
        XzeroyModuleEvents.publish(module.id, enabled)
        return enabled
    }

    fun setEnabled(module: XzeroyModuleDefinition, enabled: Boolean) {
        val key = module.enabledPreference ?: return
        preferences.edit().putBoolean(key, enabled).apply()
        XzeroyModuleEvents.publish(module.id, enabled)
    }

    fun saveSettings(module: XzeroyModuleDefinition, values: Map<String, Int>) {
        val editor = preferences.edit()
        module.settings.forEach { setting ->
            val value = values[setting.key] ?: setting.defaultValue
            editor.putInt(setting.key, value.coerceIn(setting.minValue, setting.maxValue))
        }
        editor.apply()
    }

    fun disableAll() {
        val editor = preferences.edit()
        XzeroyModuleRegistry.all.forEach { module ->
            module.enabledPreference?.let { editor.putBoolean(it, false) }
        }
        editor.apply()
        XzeroyModuleRegistry.all.forEach { module ->
            if (module.enabledPreference != null) XzeroyModuleEvents.publish(module.id, false)
        }
    }
}

/** Simple, versioned local profiles; only known keys are ever restored. */
class XzeroyProfileStore(context: Context) {
    private val profiles = context.getSharedPreferences("xzeroy_profiles", Context.MODE_PRIVATE)
    private val prefix = "profile::"

    fun listProfiles(): List<String> =
        profiles.all.keys.filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }.sorted()

    fun saveProfile(name: String, settings: SharedPreferences): String? {
        val safeName = name.trim().take(32).replace(Regex("[^\\p{L}\\p{N} _-]"), "")
        if (safeName.isBlank()) return null

        val modules = JSONArray()
        XzeroyModuleRegistry.all.forEach { module ->
            if (module.enabledPreference == null) return@forEach
            val item = JSONObject()
                .put("id", module.id)
                .put("enabled", settings.getBoolean(module.enabledPreference, false))
            module.settings.forEach { setting ->
                item.put(setting.key, settings.getInt(setting.key, setting.defaultValue))
            }
            modules.put(item)
        }
        val data = JSONObject()
            .put("schema", 1)
            .put("name", safeName)
            .put("modules", modules)
        profiles.edit().putString(prefix + safeName, data.toString()).apply()
        return safeName
    }

    fun loadProfile(name: String, settings: SharedPreferences): Boolean {
        val raw = profiles.getString(prefix + name, null) ?: return false
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return false
        if (root.optInt("schema", -1) != 1) return false
        val array = root.optJSONArray("modules") ?: return false
        val byId = mutableMapOf<String, JSONObject>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            byId[item.optString("id")] = item
        }

        val editor = settings.edit()
        val changed = mutableListOf<Pair<String, Boolean>>()
        XzeroyModuleRegistry.all.forEach { module ->
            val item = byId[module.id] ?: return@forEach
            module.enabledPreference?.let { key ->
                val enabled = item.optBoolean("enabled", false)
                editor.putBoolean(key, enabled)
                changed.add(module.id to enabled)
            }
            module.settings.forEach { setting ->
                if (item.has(setting.key)) {
                    editor.putInt(
                        setting.key,
                        item.optInt(setting.key, setting.defaultValue).coerceIn(setting.minValue, setting.maxValue)
                    )
                }
            }
        }
        editor.apply()
        changed.forEach { (id, enabled) -> XzeroyModuleEvents.publish(id, enabled) }
        return true
    }

    fun deleteProfile(name: String): Boolean =
        if (!profiles.contains(prefix + name)) false
        else {
            profiles.edit().remove(prefix + name).apply()
            true
        }
}

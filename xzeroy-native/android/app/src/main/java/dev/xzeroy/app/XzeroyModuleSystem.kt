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
    val settings: List<ModuleSettingDefinition> = emptyList(),
    val engineClass: String? = null
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
        XzeroyModuleDefinition("auto_clicker", "Auto Clicker", "Envia toques repetidos em coordenada fixa enquanto o Minecraft está em primeiro plano.", ModuleCategory.COMBAT, true, "auto_clicker",
            listOf(ModuleSettingDefinition("click_x", "Coordenada X (%)", 82, 0, 100), ModuleSettingDefinition("click_y", "Coordenada Y (%)", 72, 0, 100), ModuleSettingDefinition("click_interval", "Intervalo (ms)", 180, 120, 2000))),
        XzeroyModuleDefinition("kill_aura", "Kill Aura", "Módulo de combate ProtoHax; requer Relay ativo e servidor compatível.", ModuleCategory.COMBAT, true, "kill_aura", engineClass = "ModuleKillAura"),
        XzeroyModuleDefinition("velocity", "Velocity", "Manipula eventos de knockback por pacotes; requer Relay e compatibilidade.", ModuleCategory.COMBAT, true, "velocity", engineClass = "ModuleVelocity"),
        XzeroyModuleDefinition("targets", "Target Filters", "Gerencia filtros de alvos para módulos de combate; requer Relay.", ModuleCategory.COMBAT, true, "targets", engineClass = "ModuleTargets"),
        XzeroyModuleDefinition("surround", "Surround", "Módulo defensivo ProtoHax; exige itens e servidor compatível.", ModuleCategory.COMBAT, true, "surround", engineClass = "ModuleSurround"),
        XzeroyModuleDefinition("crystal_aura", "Crystal Aura", "Módulo de combate ProtoHax; requer Relay e servidor compatível.", ModuleCategory.COMBAT, true, "crystal_aura", engineClass = "ModuleCrystalAura"),
        XzeroyModuleDefinition("auto_jump", "Auto Jump", "Envia toques periódicos na posição configurada para pulo.", ModuleCategory.MOVEMENT, true, "auto_jump",
            listOf(ModuleSettingDefinition("jump_x", "Coordenada X (%)", 92, 0, 100), ModuleSettingDefinition("jump_y", "Coordenada Y (%)", 55, 0, 100), ModuleSettingDefinition("jump_interval", "Intervalo (ms)", 700, 250, 5000))),
        XzeroyModuleDefinition("air_jump", "Air Jump", "Módulo de movimento ProtoHax; requer Relay e servidor compatível.", ModuleCategory.MOVEMENT, true, "air_jump", engineClass = "ModuleAirJump"),
        XzeroyModuleDefinition("block_fly", "Block Fly", "Módulo ProtoHax; requer Relay, itens e servidor compatível.", ModuleCategory.MOVEMENT, true, "block_fly", engineClass = "ModuleBlockFly"),
        XzeroyModuleDefinition("blink", "Blink", "Módulo de movimento ProtoHax; requer Relay e servidor compatível.", ModuleCategory.MOVEMENT, true, "blink", engineClass = "ModuleBlink"),
        XzeroyModuleDefinition("clip", "Clip", "Módulo de movimento ProtoHax; depende do servidor.", ModuleCategory.MOVEMENT, true, "clip", engineClass = "ModuleClip"),
        XzeroyModuleDefinition("fly", "Fly", "Módulo de movimento ProtoHax; o servidor pode rejeitar o movimento.", ModuleCategory.MOVEMENT, true, "fly", engineClass = "ModuleFly"),
        XzeroyModuleDefinition("no_fall", "No Fall", "Módulo ProtoHax; requer Relay e protocolo compatível.", ModuleCategory.MOVEMENT, true, "no_fall", engineClass = "ModuleNoFall"),
        XzeroyModuleDefinition("speed", "Speed", "Módulo ProtoHax; requer Relay e servidor compatível.", ModuleCategory.MOVEMENT, true, "speed", engineClass = "ModuleSpeed"),
        XzeroyModuleDefinition("anti_blind", "Anti Blind", "Módulo visual ProtoHax; requer Relay.", ModuleCategory.VISUAL, true, "anti_blind", engineClass = "ModuleAntiBlind"),
        XzeroyModuleDefinition("hit_effect", "Hit Effect", "Módulo visual ProtoHax; requer Relay e eventos de combate.", ModuleCategory.VISUAL, true, "hit_effect", engineClass = "ModuleHitEffect"),
        XzeroyModuleDefinition("no_hurt_camera", "No Hurt Camera", "Módulo visual ProtoHax; requer Relay.", ModuleCategory.VISUAL, true, "no_hurt_camera", engineClass = "ModuleNoHurtCam"),
        XzeroyModuleDefinition("esp", "ESP", "Renderização de entidades sobre o Minecraft ainda não está integrada.", ModuleCategory.VISUAL, false),
        XzeroyModuleDefinition("entity_info", "Entity Info", "Camada e renderização de entidades ainda não estão integradas.", ModuleCategory.VISUAL, false),
        XzeroyModuleDefinition("bgm", "BGM", "Módulo auxiliar ProtoHax; requer Relay.", ModuleCategory.MISC, true, "bgm", engineClass = "ModuleBGM"),
        XzeroyModuleDefinition("device_spoof", "Device Spoof", "Módulo de dados de dispositivo ProtoHax; não concede permissões.", ModuleCategory.MISC, true, "device_spoof", engineClass = "ModuleDeviceSpoof"),
        XzeroyModuleDefinition("disabler", "Disabler", "Módulo experimental; pode quebrar a conexão. Use apenas onde permitido.", ModuleCategory.MISC, true, "disabler", engineClass = "ModuleDisabler"),
        XzeroyModuleDefinition("fast_break", "Fast Break", "Módulo ProtoHax; requer Relay e servidor compatível.", ModuleCategory.MISC, true, "fast_break", engineClass = "ModuleFastBreak"),
        XzeroyModuleDefinition("inventory_helper", "Inventory Helper", "Módulo ProtoHax; requer Relay e protocolo compatível.", ModuleCategory.MISC, true, "inventory_helper", engineClass = "ModuleInventoryHelper"),
        XzeroyModuleDefinition("miner", "Miner", "Módulo ProtoHax; requer Relay e servidor compatível.", ModuleCategory.MISC, true, "miner", engineClass = "ModuleMiner"),
        XzeroyModuleDefinition("no_skin", "No Skin", "Módulo de perfil ProtoHax; requer Relay.", ModuleCategory.MISC, true, "no_skin", engineClass = "ModuleNoSkin"),
        XzeroyModuleDefinition("op_fight_bot", "OP Fight Bot", "Módulo experimental ProtoHax; requer Relay.", ModuleCategory.MISC, true, "op_fight_bot", engineClass = "ModuleOpFightBot"),
        XzeroyModuleDefinition("resource_pack", "Resource Pack Spoof", "Módulo ProtoHax; requer Relay.", ModuleCategory.MISC, true, "resource_pack", engineClass = "ModuleResourcePackSpoof"),
        XzeroyModuleDefinition("spammer", "Spammer", "Mensagens automatizadas; use apenas onde permitido.", ModuleCategory.MISC, true, "spammer", engineClass = "ModuleSpammer"),
        XzeroyModuleDefinition("world_download", "World Download", "Comando de download ainda não está ligado à interface Android.", ModuleCategory.MISC, false)
    )
    fun forCategory(category: ModuleCategory): List<XzeroyModuleDefinition> = all.filter { it.category == category }
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
        val engineValues = JSONObject()
        settings.all.forEach { (key, value) ->
            if (key.startsWith("engine_value::") && value is String) engineValues.put(key, value)
        }
        val data = JSONObject()
            .put("schema", 1)
            .put("name", safeName)
            .put("modules", modules)
            .put("engineValues", engineValues)
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
        val engineValues = root.optJSONObject("engineValues")
        if (engineValues != null) {
            val keys = engineValues.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (!key.startsWith("engine_value::")) continue
                val moduleId = key.removePrefix("engine_value::").substringBefore("::")
                if (XzeroyModuleRegistry.find(moduleId)?.engineClass == null) continue
                editor.putString(key, engineValues.optString(key))
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

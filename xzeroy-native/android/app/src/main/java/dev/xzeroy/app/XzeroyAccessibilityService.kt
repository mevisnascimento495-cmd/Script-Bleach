package dev.xzeroy.app

import android.app.AlertDialog

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class XzeroyAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val moduleStore by lazy { XzeroySettingsStore(this) }
    private val relayController by lazy { XzeroyRelayController.get(applicationContext) }
    private val moduleEventSubscription = XzeroyModuleEvents.observe {
        handler.post {
            refreshChip()
            if (menu != null) {
                hideMenu()
                showMenu()
            }
        }
    }
    private var chip: TextView? = null
    private var menu: LinearLayout? = null
    private var selectedCategory = ModuleCategory.COMBAT.label
    private var gestureInProgress = false
    private var lastClickAt = 0L
    private var lastJumpAt = 0L

    private val ticker = object : Runnable {
        override fun run() {
            val prefs = getSharedPreferences("xzeroy_settings", MODE_PRIVATE)
            val activePackage = rootInActiveWindow?.packageName?.toString()
            val isMinecraft = activePackage == MINECRAFT_PACKAGE
            val now = SystemClock.uptimeMillis()
            if (activePackage != null) prefs.edit().putBoolean("minecraft_foreground", isMinecraft).apply()

            if (isMinecraft && !gestureInProgress) {
                val clickEnabled = prefs.getBoolean("auto_clicker", false)
                val jumpEnabled = prefs.getBoolean("auto_jump", false)
                val clickInterval = prefs.getInt("click_interval", 180).coerceIn(120, 2000)
                val jumpInterval = prefs.getInt("jump_interval", 700).coerceIn(250, 5000)
                val clickDue = clickEnabled && now - lastClickAt >= clickInterval
                val jumpDue = jumpEnabled && now - lastJumpAt >= jumpInterval
                when {
                    clickDue && jumpDue -> if (now - lastClickAt - clickInterval >= now - lastJumpAt - jumpInterval) {
                        lastClickAt = now
                        tapPercent(prefs.getInt("click_x", 82), prefs.getInt("click_y", 72))
                    } else {
                        lastJumpAt = now
                        tapPercent(prefs.getInt("jump_x", 92), prefs.getInt("jump_y", 55))
                    }
                    clickDue -> {
                        lastClickAt = now
                        tapPercent(prefs.getInt("click_x", 82), prefs.getInt("click_y", 72))
                    }
                    jumpDue -> {
                        lastJumpAt = now
                        tapPercent(prefs.getInt("jump_x", 92), prefs.getInt("jump_y", 55))
                    }
                }
            }
            refreshChip()
            handler.postDelayed(this, 80L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        showChip()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        getSharedPreferences("xzeroy_settings", MODE_PRIVATE).edit()
            .putBoolean("minecraft_foreground", packageName == MINECRAFT_PACKAGE).apply()
    }

    override fun onInterrupt() { gestureInProgress = false }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        try { menu?.let { wm.removeView(it) } } catch (_: Exception) {}
        try { chip?.let { wm.removeView(it) } } catch (_: Exception) {}
        menu = null
        chip = null
        moduleEventSubscription.close()
        super.onDestroy()
    }

    private fun tapPercent(xRaw: Int, yRaw: Int) {
        val metrics = resources.displayMetrics
        val x = (metrics.widthPixels * xRaw.coerceIn(0, 100) / 100f).coerceIn(1f, (metrics.widthPixels - 1).toFloat())
        val y = (metrics.heightPixels * yRaw.coerceIn(0, 100) / 100f).coerceIn(1f, (metrics.heightPixels - 1).toFloat())
        val path = Path().apply {
            moveTo(x, y)
            lineTo((x + 0.5f).coerceAtMost(metrics.widthPixels - 1f), (y + 0.5f).coerceAtMost(metrics.heightPixels - 1f))
        }
        val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 50)).build()
        gestureInProgress = true
        val accepted = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) { gestureInProgress = false }
            override fun onCancelled(gestureDescription: GestureDescription?) { gestureInProgress = false }
        }, null)
        if (!accepted) gestureInProgress = false
    }

    private fun showChip() {
        if (chip != null) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val view = TextView(this).apply {
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(15, 12, 15, 12)
            background = rounded(Color.rgb(39, 62, 116), Color.rgb(125, 158, 255))
            contentDescription = "Abrir menu de sobreposição XZeroy"
            setOnClickListener { if (menu == null) showMenu() else hideMenu() }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = 18; y = 180 }
        view.setOnTouchListener(DragListener { dx, dy ->
            val lp = view.layoutParams as? WindowManager.LayoutParams ?: return@DragListener
            lp.x = (lp.x - dx).toInt().coerceAtLeast(0)
            lp.y = (lp.y + dy).toInt().coerceAtLeast(0)
            wm.updateViewLayout(view, lp)
        })
        wm.addView(view, params)
        chip = view
        refreshChip()
    }

    private fun showMenu() {
        if (menu != null) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            background = rounded(Color.rgb(12, 15, 26), Color.rgb(105, 83, 230))
            elevation = 16f
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleColumn.addView(TextView(this).apply {
            text = "XZEROY"
            textSize = 18f
            setTextColor(Color.rgb(174, 163, 255))
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        titleColumn.addView(TextView(this).apply {
            text = "BEDROCK CLIENT  •  CLICKGUI"
            textSize = 9f
            setTextColor(Color.rgb(155, 164, 188))
            setPadding(0, 2, 0, 0)
        })
        header.addView(titleColumn, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(this).apply {
            text = "×"
            textSize = 25f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(12, 0, 5, 2)
            setOnClickListener { hideMenu() }
        })
        root.addView(header)

        val prefs = getSharedPreferences("xzeroy_settings", MODE_PRIVATE)
        val relay = relayController.snapshot()
        val foreground = if (prefs.getBoolean("minecraft_foreground", false)) "EM PRIMEIRO PLANO" else "AGUARDANDO"
        root.addView(TextView(this).apply {
            text = "JOGO: " + foreground + "     •     RELAY: " + relay.status.label.uppercase()
            textSize = 10f
            setTextColor(if (relay.status == RelayStatus.RUNNING) Color.rgb(110, 230, 172) else Color.rgb(180, 187, 208))
            setPadding(2, 7, 2, 10)
        })

        val categories = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 8)
        }
        (ModuleCategory.values().map { it.label } + "Settings").forEach { categoryName ->
            categories.addView(TextView(this).apply {
                text = categoryName
                textSize = 9f
                gravity = Gravity.CENTER
                setTextColor(if (categoryName == selectedCategory) Color.WHITE else Color.rgb(177, 185, 208))
                setPadding(4, 10, 4, 10)
                background = rounded(
                    if (categoryName == selectedCategory) Color.rgb(80, 64, 177) else Color.rgb(26, 31, 47),
                    if (categoryName == selectedCategory) Color.rgb(153, 135, 255) else Color.rgb(48, 55, 75)
                )
                setOnClickListener {
                    selectedCategory = categoryName
                    hideMenu()
                    showMenu()
                }
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(2, 0, 2, 0) })
        }
        root.addView(categories)

        val scroll = ScrollView(this)
        val items = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(items)

        if (selectedCategory == "Settings") {
            section(items, "CONFIGURAÇÕES DO CLIENT")
            moduleAction(items, "Abrir configurações de Acessibilidade") {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            moduleAction(items, "Desativar todos os módulos") {
                moduleStore.disableAll()
                hideMenu()
                showMenu()
            }
            moduleAction(items, "Abrir painel XZeroy") {
                val launch = packageManager.getLaunchIntentForPackage(packageName)
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launch)
                } else {
                    Toast.makeText(this, "Abra o XZeroy pela tela de aplicativos.", Toast.LENGTH_SHORT).show()
                }
            }
            moduleAction(items, "Fechar menu") { hideMenu() }
        } else {
            val category = ModuleCategory.values().firstOrNull { it.label == selectedCategory } ?: ModuleCategory.COMBAT
            val modules = XzeroyModuleRegistry.forCategory(category)
            section(items, category.label.uppercase() + "   •   " + modules.count { it.implemented } + " MÓDULOS")
            modules.forEach { module ->
                if (module.implemented && module.enabledPreference != null) {
                    moduleToggle(items, module)
                } else {
                    disabledModule(items, module.name, module.description)
                }
            }
            if (modules.isEmpty()) {
                TextView(this).apply {
                    text = "Nenhum módulo nesta categoria."
                    textSize = 12f
                    setTextColor(Color.LTGRAY)
                    setPadding(8, 12, 8, 12)
                }.also { items.addView(it) }
            }
        }

        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(TextView(this).apply {
            val enabledCount = XzeroyModuleRegistry.all.count { moduleStore.isEnabled(it) }
            text = "XZeroy 0.6.0  •  ${enabledCount ATIVOS  •  TOQUE DUPLO = CONFIGURAR"
            textSize = 9f
            setTextColor(Color.rgb(135, 145, 171))
            gravity = Gravity.CENTER
            setPadding(0, 9, 0, 1)
        })

        val params = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.90f).toInt().coerceAtMost(460),
            (resources.displayMetrics.heightPixels * 0.76f).toInt(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        wm.addView(root, params)
        menu = root
    }

    private fun hideMenu() {
        menu?.let { try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) } catch (_: Exception) {} }
        menu = null
    }

    private fun section(parent: LinearLayout, label: String) {
        parent.addView(TextView(this).apply {
            text = label; textSize = 10f; setTextColor(Color.rgb(120, 150, 230)); setPadding(4, 8, 4, 5)
        })
    }

    private fun moduleToggle(parent: LinearLayout, module: XzeroyModuleDefinition) {
        val key = module.enabledPreference ?: return
        val supportsConfig = module.settings.isNotEmpty() || module.engineClass != null
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(11, 9, 11, 9)
            background = rounded(Color.rgb(23, 29, 45), Color.rgb(53, 61, 85))
        }
        val line = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val name = TextView(this).apply {
            text = module.name + "\n" + when {
                module.engineClass != null && relayController.snapshot().status != RelayStatus.RUNNING -> "REQUER RELAY ATIVO"
                supportsConfig -> "TOQUE DUPLO PARA CONFIGURAR"
                else -> "SEM OPÇÕES EDITÁVEIS"
            }
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(0, 3, 4, 3)
        }
        var lastTap = 0L
        name.setOnClickListener {
            val now = SystemClock.uptimeMillis()
            if (now - lastTap <= 450L) {
                if (supportsConfig) showModuleSettings(module)
                else Toast.makeText(this, "${module.name não tem opções editáveis.", Toast.LENGTH_SHORT).show()
                lastTap = 0L
            } else {
                lastTap = now
                Toast.makeText(this, "Toque novamente em ${module.name para configurar.", Toast.LENGTH_SHORT).show()
            }
        }
        line.addView(name, LinearLayout.LayoutParams(0, -2, 1f))
        if (supportsConfig) {
            line.addView(Button(this).apply {
                text = "⚙"
                isAllCaps = false
                textSize = 12f
                setTextColor(Color.rgb(190, 181, 255))
                background = rounded(Color.rgb(40, 36, 67), Color.rgb(83, 72, 133))
                setOnClickListener { showModuleSettings(module) }
            }, LinearLayout.LayoutParams(-2, -2).apply { setMargins(3, 0, 3, 0) })
        }
        val toggle = Button(this).apply {
            text = if (moduleStore.isEnabled(module)) "ON" else "OFF"
            isAllCaps = false
            textSize = 10f
            setTextColor(Color.WHITE)
            background = rounded(
                if (moduleStore.isEnabled(module)) Color.rgb(30, 125, 91) else Color.rgb(47, 54, 73),
                if (moduleStore.isEnabled(module)) Color.rgb(77, 218, 166) else Color.rgb(71, 80, 105)
            )
            setOnClickListener {
                val enabled = moduleStore.toggle(module)
                text = if (enabled) "ON" else "OFF"
                background = rounded(
                    if (enabled) Color.rgb(30, 125, 91) else Color.rgb(47, 54, 73),
                    if (enabled) Color.rgb(77, 218, 166) else Color.rgb(71, 80, 105)
                )
                refreshChip()
            }
        }
        line.addView(toggle)
        row.addView(line)
        row.addView(TextView(this).apply {
            val relayHint = if (module.engineClass != null && relayController.snapshot().status != RelayStatus.RUNNING) {
                "\n\nAtive o Relay e conecte o Minecraft através da porta local para aplicar este módulo."
            } else ""
            text = module.description + relayHint
            textSize = 10f
            setTextColor(Color.rgb(166, 175, 197))
            setPadding(0, 4, 0, 0)
        })
        parent.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 3, 0, 5) })
    }

    private fun showModuleSettings(module: XzeroyModuleDefinition) {
        if (module.settings.isNotEmpty()) {
            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 8, 20, 8)
            }
            val inputs = linkedMapOf<String, android.widget.EditText>()
            module.settings.forEach { setting ->
                content.addView(TextView(this).apply {
                    text = "${setting.label  •  ${setting.minValue–${setting.maxValue"
                    textSize = 12f
                    setTextColor(Color.LTGRAY)
                    setPadding(0, 7, 0, 2)
                })
                val input = android.widget.EditText(this).apply {
                    setSingleLine(true)
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    setText(getSharedPreferences("xzeroy_settings", MODE_PRIVATE).getInt(setting.key, setting.defaultValue).toString())
                    setTextColor(Color.WHITE)
                    setHintTextColor(Color.GRAY)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(133, 113, 255))
                }
                content.addView(input)
                inputs[setting.key] = input
            }
            AlertDialog.Builder(this)
                .setTitle("Configurar • ${module.name")
                .setView(content)
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Desativar") { _, _ -> moduleStore.setEnabled(module, false) }
                .setPositiveButton("Salvar") { _, _ ->
                    val values = inputs.mapValues { (key, input) ->
                        val spec = module.settings.first { it.key == key }
                        (input.text.toString().toIntOrNull() ?: spec.defaultValue).coerceIn(spec.minValue, spec.maxValue)
                    }
                    moduleStore.saveSettings(module, values)
                    Toast.makeText(this, "Configurações de ${module.name salvas.", Toast.LENGTH_SHORT).show()
                }
                .show()
            return
        }

        if (module.engineClass == null) {
            Toast.makeText(this, "Este módulo não tem opções editáveis.", Toast.LENGTH_SHORT).show()
            return
        }
        val values = relayController.engineValues(module.id)
        if (values == null) {
            Toast.makeText(this, "Inicie o Relay na aba Relay para carregar as opções da engine.", Toast.LENGTH_LONG).show()
            return
        }
        if (values.isEmpty()) {
            Toast.makeText(this, "Este módulo não expõe opções editáveis.", Toast.LENGTH_SHORT).show()
            return
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 8, 18, 8)
        }
        val edits = linkedMapOf<String, android.widget.EditText>()
        val checks = linkedMapOf<String, android.widget.CheckBox>()
        val choices = linkedMapOf<String, android.widget.Spinner>()
        values.forEach { setting ->
            when (setting.type) {
                "boolean" -> {
                    val check = android.widget.CheckBox(this).apply {
                        text = setting.name
                        setTextColor(Color.WHITE)
                        isChecked = setting.currentValue.equals("true", true)
                    }
                    content.addView(check)
                    checks[setting.name] = check
                }
                "choice" -> {
                    content.addView(TextView(this).apply {
                        text = setting.name
                        textSize = 12f
                        setTextColor(Color.LTGRAY)
                        setPadding(0, 8, 0, 3)
                    })
                    val spinner = android.widget.Spinner(this)
                    spinner.adapter = android.widget.ArrayAdapter(
                        this, android.R.layout.simple_spinner_item, setting.choices
                    ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                    val selected = setting.choices.indexOfFirst { it.equals(setting.currentValue, true) }
                    if (selected >= 0) spinner.setSelection(selected)
                    content.addView(spinner)
                    choices[setting.name] = spinner
                }
                else -> {
                    content.addView(TextView(this).apply {
                        text = if (setting.hint.isBlank()) setting.name else "${setting.name  •  ${setting.hint"
                        textSize = 12f
                        setTextColor(Color.LTGRAY)
                        setPadding(0, 8, 0, 3)
                    })
                    val input = android.widget.EditText(this).apply {
                        setSingleLine(true)
                        setText(setting.currentValue)
                        setTextColor(Color.WHITE)
                        setHintTextColor(Color.GRAY)
                        inputType = when (setting.type) {
                            "integer" -> android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                            "decimal" -> android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                            else -> android.text.InputType.TYPE_CLASS_TEXT
                        }
                    }
                    content.addView(input)
                    edits[setting.name] = input
                }
            }
        }
        val scroll = ScrollView(this).apply { addView(content) }
        AlertDialog.Builder(this)
            .setTitle("Configurar • ${module.name")
            .setView(scroll)
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Restaurar padrões") { _, _ ->
                if (relayController.resetEngineValues(module.id)) {
                    Toast.makeText(this, "Valores padrão restaurados.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Não foi possível restaurar os padrões.", Toast.LENGTH_SHORT).show()
                }
            }
            .setPositiveButton("Salvar") { _, _ ->
                val updated = linkedMapOf<String, String>()
                edits.forEach { (key, input) -> updated[key] = input.text.toString() }
                checks.forEach { (key, check) -> updated[key] = check.isChecked.toString() }
                choices.forEach { (key, spinner) -> updated[key] = spinner.selectedItem?.toString().orEmpty() }
                val error = relayController.saveEngineValues(module.id, updated)
                if (error != null) Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                else Toast.makeText(this, "Opções de ${module.name salvas.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun disabledModule(parent: LinearLayout, label: String, description: String) {
        parent.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(10, 9, 10, 9)
            background = rounded(Color.rgb(24, 30, 44), Color.rgb(40, 46, 62))
            addView(TextView(this@XzeroyAccessibilityService).apply {
                text = "$label  •  EM BREVE"; textSize = 13f; setTextColor(Color.GRAY)
            })
            addView(TextView(this@XzeroyAccessibilityService).apply {
                text = description; textSize = 10f; setTextColor(Color.DKGRAY); setPadding(0, 3, 0, 0)
            })
        }, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 2, 0, 5) })
    }

    private fun moduleAction(parent: LinearLayout, label: String, action: () -> Unit) {
        parent.addView(Button(this).apply { text = label; isAllCaps = false; setOnClickListener { action() } },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = 4 })
    }

    private fun refreshChip() {
        val prefs = getSharedPreferences("xzeroy_settings", MODE_PRIVATE)
        chip?.text = "XZ  AC:" + (if (prefs.getBoolean("auto_clicker", false)) "ON" else "OFF") +
            "  AJ:" + (if (prefs.getBoolean("auto_jump", false)) "ON" else "OFF")
    }

    private fun rounded(fill: Int, stroke: Int) = GradientDrawable().apply {
        setColor(fill); cornerRadius = 18f; setStroke(1, stroke)
    }

    private class DragListener(val move: (Float, Float) -> Unit) : View.OnTouchListener {
        private var downX = 0f; private var downY = 0f
        private var lastX = 0f; private var lastY = 0f; private var dragged = false
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY; lastX = event.rawX; lastY = event.rawY; dragged = false
                    return false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastX; val dy = event.rawY - lastY
                    if (abs(event.rawX - downX) > 8 || abs(event.rawY - downY) > 8) dragged = true
                    if (dragged) move(dx, dy)
                    lastX = event.rawX; lastY = event.rawY
                    return dragged
                }
                MotionEvent.ACTION_UP -> return dragged
            }
            return false
        }
    }

    companion object { private const val MINECRAFT_PACKAGE = "com.mojang.minecraftpe" }
}

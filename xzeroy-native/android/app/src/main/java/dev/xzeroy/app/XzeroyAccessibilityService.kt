package dev.xzeroy.app

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
    private var chip: TextView? = null
    private var menu: LinearLayout? = null
    private var selectedCategory = "Combat"
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
            setPadding(12, 10, 12, 10)
            background = rounded(Color.rgb(16, 21, 35), Color.rgb(92, 126, 220))
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text = "XZEROY  •  MENU"
            textSize = 16f
            setTextColor(Color.rgb(150, 180, 255))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(this).apply {
            text = "×"; textSize = 24f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setPadding(12, 0, 12, 2); setOnClickListener { hideMenu() }
        })
        root.addView(header)
        val prefs = getSharedPreferences("xzeroy_settings", MODE_PRIVATE)
        root.addView(TextView(this).apply {
            text = "BEDROCK  •  " + if (prefs.getBoolean("minecraft_foreground", false)) "EM JOGO" else "AGUARDANDO JOGO"
            textSize = 10f; setTextColor(Color.LTGRAY); setPadding(2, 3, 2, 8)
        })

        val categories = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Combat", "Player", "World", "Settings").forEach { category ->
            categories.addView(TextView(this).apply {
                text = category; textSize = 11f; gravity = Gravity.CENTER
                setTextColor(if (category == selectedCategory) Color.WHITE else Color.LTGRAY)
                setPadding(5, 10, 5, 10)
                background = rounded(if (category == selectedCategory) Color.rgb(48, 68, 116) else Color.rgb(28, 35, 52),
                    if (category == selectedCategory) Color.rgb(100, 137, 230) else Color.rgb(45, 52, 72))
                setOnClickListener { selectedCategory = category; hideMenu(); showMenu() }
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(2, 0, 2, 8) })
        }
        root.addView(categories)

        val scroll = ScrollView(this)
        val items = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(items)
        when (selectedCategory) {
            "Combat" -> {
                section(items, "AUTOMATION")
                moduleToggle(items, "Auto Clicker", "auto_clicker", "Toques numa posição configurada.")
                moduleToggle(items, "Auto Jump", "auto_jump", "Toques periódicos na posição de pulo.")
                section(items, "EM DESENVOLVIMENTO")
                disabledModule(items, "Reach", "Ainda não implementado")
                disabledModule(items, "Aim Assist", "Ainda não implementado")
            }
            "Player" -> {
                section(items, "PLAYER")
                disabledModule(items, "ESP", "Ainda não implementado")
                disabledModule(items, "Entity Info", "Ainda não implementado")
                disabledModule(items, "Inventory", "Ainda não implementado")
            }
            "World" -> {
                section(items, "WORLD")
                disabledModule(items, "World Info", "Ainda não implementado")
                disabledModule(items, "Server Admin", "Exige permissões autorizadas no servidor")
            }
            else -> {
                section(items, "CONFIGURAÇÕES")
                moduleAction(items, "Abrir Acessibilidade") {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                moduleAction(items, "Desligar todos os módulos") {
                    prefs.edit().putBoolean("auto_clicker", false).putBoolean("auto_jump", false).apply()
                    hideMenu()
                    showMenu()
                }
                moduleAction(items, "Fechar menu") { hideMenu() }
            }
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(TextView(this).apply {
            text = "XZeroy 0.4.0 • beta"; textSize = 10f; setTextColor(Color.GRAY); gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        })
        val params = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.84f).toInt().coerceAtMost(420),
            (resources.displayMetrics.heightPixels * 0.70f).toInt(),
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

    private fun moduleToggle(parent: LinearLayout, label: String, key: String, description: String) {
        val prefs = getSharedPreferences("xzeroy_settings", MODE_PRIVATE)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(10, 8, 10, 8)
            background = rounded(Color.rgb(28, 35, 52), Color.rgb(44, 53, 75))
        }
        val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        line.addView(TextView(this).apply {
            text = label; textSize = 14f; setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        line.addView(Button(this).apply {
            text = if (prefs.getBoolean(key, false)) "ON" else "OFF"; isAllCaps = false; textSize = 11f
            setOnClickListener {
                val enabled = !prefs.getBoolean(key, false)
                prefs.edit().putBoolean(key, enabled).apply()
                text = if (enabled) "ON" else "OFF"
                refreshChip()
            }
        })
        row.addView(line)
        row.addView(TextView(this).apply {
            text = description; textSize = 11f; setTextColor(Color.LTGRAY); setPadding(0, 4, 0, 0)
        })
        parent.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 2, 0, 5) })
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

package dev.xzeroy.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import android.widget.Toast

class XzeroyAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var chip: TextView? = null
    private var gestureInProgress = false
    private var lastClickAt = 0L
    private var lastJumpAt = 0L

    private val ticker = object : Runnable {
        override fun run() {
            val prefs = getSharedPreferences("xzeroy_settings", MODE_PRIVATE)
            val activePackage = rootInActiveWindow?.packageName?.toString()
            val isMinecraft = activePackage == "com.mojang.minecraftpe"
            val now = SystemClock.uptimeMillis()

            if (isMinecraft && !gestureInProgress) {
                val clickEnabled = prefs.getBoolean("auto_clicker", false)
                val jumpEnabled = prefs.getBoolean("auto_jump", false)
                val clickInterval = prefs.getInt("click_interval", 180).coerceIn(120, 2000)
                val jumpInterval = prefs.getInt("jump_interval", 700).coerceIn(250, 5000)

                if (clickEnabled && now - lastClickAt >= clickInterval) {
                    lastClickAt = now
                    tapPercent(prefs.getInt("click_x", 82), prefs.getInt("click_y", 72))
                } else if (jumpEnabled && now - lastJumpAt >= jumpInterval) {
                    lastJumpAt = now
                    tapPercent(prefs.getInt("jump_x", 92), prefs.getInt("jump_y", 55))
                }
            }
            refreshChip(prefs.getBoolean("auto_clicker", false), prefs.getBoolean("auto_jump", false))
            handler.postDelayed(this, 80L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        showChip()
        handler.post(ticker)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Gate actual taps on the foreground Minecraft package in the ticker.
    }

    override fun onInterrupt() {
        gestureInProgress = false
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        chip?.let {
            try {
                (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it)
            } catch (_: IllegalArgumentException) {
                // View was already removed by Android.
            }
        }
        chip = null
        super.onDestroy()
    }

    private fun tapPercent(xPercentRaw: Int, yPercentRaw: Int) {
        val metrics = resources.displayMetrics
        val xPercent = xPercentRaw.coerceIn(0, 100)
        val yPercent = yPercentRaw.coerceIn(0, 100)
        val x = (metrics.widthPixels * xPercent / 100f).coerceIn(1f, (metrics.widthPixels - 1).toFloat())
        val y = (metrics.heightPixels * yPercent / 100f).coerceIn(1f, (metrics.heightPixels - 1).toFloat())
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 45))
            .build()

        gestureInProgress = true
        val accepted = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                gestureInProgress = false
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                gestureInProgress = false
            }
        }, null)
        if (!accepted) gestureInProgress = false
    }

    private fun showChip() {
        if (chip != null) return
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val background = GradientDrawable().apply {
            setColor(Color.rgb(35, 42, 60))
            cornerRadius = 28f
            setStroke(2, Color.rgb(110, 140, 230))
        }
        val view = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(18, 12, 18, 12)
            this.background = background
            contentDescription = "XZeroy: toque para alternar Auto Clicker; toque longo para alternar Auto Jump"
            setOnClickListener {
                val prefs = getSharedPreferences("xzeroy_settings", MODE_PRIVATE)
                val enabled = !prefs.getBoolean("auto_clicker", false)
                prefs.edit().putBoolean("auto_clicker", enabled).apply()
                Toast.makeText(this@XzeroyAccessibilityService,
                    if (enabled) "XZeroy Auto Clicker ligado" else "XZeroy Auto Clicker desligado",
                    Toast.LENGTH_SHORT).show()
                refreshChip(enabled, prefs.getBoolean("auto_jump", false))
            }
            setOnLongClickListener {
                val prefs = getSharedPreferences("xzeroy_settings", MODE_PRIVATE)
                val enabled = !prefs.getBoolean("auto_jump", false)
                prefs.edit().putBoolean("auto_jump", enabled).apply()
                Toast.makeText(this@XzeroyAccessibilityService,
                    if (enabled) "XZeroy Auto Jump ligado" else "XZeroy Auto Jump desligado",
                    Toast.LENGTH_SHORT).show()
                refreshChip(prefs.getBoolean("auto_clicker", false), enabled)
                true
            }
        }
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 180
        }
        windowManager.addView(view, layoutParams)
        chip = view
        refreshChip(false, false)
    }

    private fun refreshChip(clickEnabled: Boolean, jumpEnabled: Boolean) {
        chip?.text = "XZeroy  AC:" + (if (clickEnabled) "ON" else "OFF") +
            "  AJ:" + (if (jumpEnabled) "ON" else "OFF")
    }
}

package dev.xzeroy.app

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("xzeroy_settings", MODE_PRIVATE) }
    private lateinit var status: TextView
    private lateinit var clickerButton: Button
    private lateinit var jumpButton: Button
    private lateinit var clickX: EditText
    private lateinit var clickY: EditText
    private lateinit var clickInterval: EditText
    private lateinit var jumpX: EditText
    private lateinit var jumpY: EditText
    private lateinit var jumpInterval: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(16, 18, 26)
        window.navigationBarColor = Color.rgb(16, 18, 26)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(Color.rgb(20, 23, 33))
        }
        content.addView(TextView(this).apply {
            text = "XZeroy"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        })
        content.addView(TextView(this).apply {
            text = "Bedrock • controle por Acessibilidade"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 20)
        })
        status = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.rgb(255, 210, 120))
            setPadding(0, 8, 0, 16)
        }
        content.addView(status)

        content.addView(sectionTitle("Auto Clicker"))
        clickX = numberField("Ponto X (%) — 0 a 100", prefs.getInt("click_x", 82))
        clickY = numberField("Ponto Y (%) — 0 a 100", prefs.getInt("click_y", 72))
        clickInterval = numberField("Intervalo (ms) — mínimo 120", prefs.getInt("click_interval", 180))
        content.addView(clickX)
        content.addView(clickY)
        content.addView(clickInterval)

        content.addView(sectionTitle("Auto Jump"))
        jumpX = numberField("Pulo X (%) — 0 a 100", prefs.getInt("jump_x", 92))
        jumpY = numberField("Pulo Y (%) — 0 a 100", prefs.getInt("jump_y", 55))
        jumpInterval = numberField("Intervalo do pulo (ms)", prefs.getInt("jump_interval", 700))
        content.addView(jumpX)
        content.addView(jumpY)
        content.addView(jumpInterval)

        content.addView(Button(this).apply {
            text = "Salvar coordenadas e intervalos"
            setOnClickListener { saveSettings() }
        })
        clickerButton = Button(this).apply {
            setOnClickListener {
                toggle("auto_clicker")
                refreshUi()
            }
        }
        content.addView(clickerButton)
        jumpButton = Button(this).apply {
            setOnClickListener {
                toggle("auto_jump")
                refreshUi()
            }
        }
        content.addView(jumpButton)

        content.addView(Button(this).apply {
            text = "Abrir configurações de Acessibilidade"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })

        content.addView(sectionTitle("Ainda não implementado"))
        content.addView(TextView(this).apply {
            text = "ESP, leitura de coordenadas do jogo e comandos administrativos exigem integração específica. Este app não modifica memória do Minecraft nem contorna permissões do servidor."
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, 16)
        })
        setContentView(ScrollView(this).apply { addView(content) })
        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refreshUi()
    }

    private fun sectionTitle(value: String) = TextView(this).apply {
        text = value
        textSize = 19f
        setTextColor(Color.WHITE)
        setPadding(0, 18, 0, 8)
    }

    private fun numberField(label: String, value: Int) = EditText(this).apply {
        hint = label
        setText(value.toString())
        inputType = InputType.TYPE_CLASS_NUMBER
        textSize = 14f
        setSingleLine(true)
        setTextColor(Color.WHITE)
        setHintTextColor(Color.LTGRAY)
    }

    private fun saveSettings() {
        fun parsed(field: EditText, fallback: Int, min: Int, max: Int) =
            (field.text.toString().toIntOrNull() ?: fallback).coerceIn(min, max)

        prefs.edit()
            .putInt("click_x", parsed(clickX, 82, 0, 100))
            .putInt("click_y", parsed(clickY, 72, 0, 100))
            .putInt("click_interval", parsed(clickInterval, 180, 120, 2000))
            .putInt("jump_x", parsed(jumpX, 92, 0, 100))
            .putInt("jump_y", parsed(jumpY, 55, 0, 100))
            .putInt("jump_interval", parsed(jumpInterval, 700, 250, 5000))
            .apply()
        refreshUi()
    }

    private fun toggle(key: String) {
        prefs.edit().putBoolean(key, !prefs.getBoolean(key, false)).apply()
    }

    private fun refreshUi() {
        val clickEnabled = prefs.getBoolean("auto_clicker", false)
        val jumpEnabled = prefs.getBoolean("auto_jump", false)
        clickerButton.text = "Auto Clicker: " + if (clickEnabled) "LIGADO" else "DESLIGADO"
        jumpButton.text = "Auto Jump: " + if (jumpEnabled) "LIGADO" else "DESLIGADO"
        val serviceEnabled = isAccessibilityServiceEnabled()
        status.text = when {
            !serviceEnabled -> "Ação necessária: habilite o serviço XZeroy nas configurações de Acessibilidade."
            clickEnabled || jumpEnabled -> "Preparado. Os toques só são enviados quando Minecraft Bedrock está em primeiro plano."
            else -> "Acessibilidade habilitada; módulos desligados."
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(this, XzeroyAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }
}

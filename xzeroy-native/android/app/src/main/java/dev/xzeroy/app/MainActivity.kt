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
    private lateinit var body: LinearLayout
    private lateinit var connectionStatus: TextView
    private var currentTab = "Início"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 15, 25)
        window.navigationBarColor = Color.rgb(12, 15, 25)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(16, 20, 32)) }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(22, 22, 22, 18); setBackgroundColor(Color.rgb(22, 29, 48))
        }
        header.addView(TextView(this).apply {
            text = "XZEROY"; textSize = 27f; setTextColor(Color.rgb(130, 165, 255)); gravity = Gravity.CENTER_VERTICAL
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        header.addView(TextView(this).apply {
            text = "BEDROCK CHEAT CLIENT • BETA"; textSize = 11f; setTextColor(Color.LTGRAY); setPadding(0, 3, 0, 0)
        })
        connectionStatus = TextView(this).apply { textSize = 13f; setTextColor(Color.rgb(255, 211, 128)); setPadding(0, 12, 0, 0) }
        header.addView(connectionStatus)
        root.addView(header)
        val scroll = ScrollView(this)
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 12, 18, 22) }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(4, 7, 4, 7); setBackgroundColor(Color.rgb(22, 29, 48)) }
        listOf("Início", "Módulos", "Conta", "Ajustes").forEach { tab ->
            nav.addView(Button(this).apply {
                text = tab; textSize = 11f; isAllCaps = false
                setOnClickListener { currentTab = tab; renderTab() }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        root.addView(nav)
        setContentView(root)
        renderTab()
    }

    override fun onResume() { super.onResume(); if (::body.isInitialized) renderTab() }

    private fun renderTab() {
        body.removeAllViews()
        val serviceOn = isAccessibilityServiceEnabled()
        val minecraftInstalled = packageManager.getLaunchIntentForPackage("com.mojang.minecraftpe") != null
        connectionStatus.text = when {
            !serviceOn -> "SERVIÇO: desligado • Ative em Ajustes"
            prefs.getBoolean("minecraft_foreground", false) -> "MINECRAFT: detectado em primeiro plano"
            minecraftInstalled -> "MINECRAFT: instalado • ainda não conectado"
            else -> "MINECRAFT: não encontrado neste aparelho"
        }
        when (currentTab) {
            "Início" -> {
                title("Painel")
                card("Conexão com Minecraft", "O XZeroy é um app separado. Abra o Bedrock pelo botão abaixo; o serviço detecta quando o jogo está em primeiro plano.") {
                    actionButton("Abrir Minecraft Bedrock") {
                        val intent = packageManager.getLaunchIntentForPackage("com.mojang.minecraftpe")
                        if (intent != null) startActivity(intent) else message("Minecraft Bedrock não foi encontrado neste aparelho.")
                    }
                }
                card("Estado dos módulos", "Auto Clicker: " + onOff("auto_clicker") + "\nAuto Jump: " + onOff("auto_jump") + "\n\nConfigure os módulos na aba Módulos.")
                card("Versão beta", "Ainda não existe integração interna com o jogo. ESP, leitura de entidades, conta Microsoft e funções administrativas não estão implementados.")
            }
            "Módulos" -> {
                title("Módulos")
                card("Auto Clicker", "Envia toques em posição fixa quando Minecraft está em primeiro plano. Não detecta inimigos.") {
                    val x = field("Coordenada X (%)", prefs.getInt("click_x", 82))
                    val y = field("Coordenada Y (%)", prefs.getInt("click_y", 72))
                    val interval = field("Intervalo em ms (120–2000)", prefs.getInt("click_interval", 180))
                    addView(x); addView(y); addView(interval)
                    actionButton("Salvar Auto Clicker") {
                        prefs.edit().putInt("click_x", number(x,82,0,100)).putInt("click_y",number(y,72,0,100))
                            .putInt("click_interval",number(interval,180,120,2000)).apply()
                        message("Configuração salva.")
                    }
                    actionButton("Alternar Auto Clicker (" + onOff("auto_clicker") + ")") { toggle("auto_clicker"); renderTab() }
                }
                card("Auto Jump", "Envia toques em posição fixa; depende do layout de controles do jogo.") {
                    val x = field("Coordenada X (%)", prefs.getInt("jump_x", 92))
                    val y = field("Coordenada Y (%)", prefs.getInt("jump_y", 55))
                    val interval = field("Intervalo em ms (250–5000)", prefs.getInt("jump_interval", 700))
                    addView(x); addView(y); addView(interval)
                    actionButton("Salvar Auto Jump") {
                        prefs.edit().putInt("jump_x",number(x,92,0,100)).putInt("jump_y",number(y,55,0,100))
                            .putInt("jump_interval",number(interval,700,250,5000)).apply()
                        message("Configuração salva.")
                    }
                    actionButton("Alternar Auto Jump (" + onOff("auto_jump") + ")") { toggle("auto_jump"); renderTab() }
                }
                listOf("ESP", "Reach / alcance", "Player / entidades", "Inventário", "Admin de servidor").forEach { module ->
                    card(module, "Ainda não implementado nesta versão. O painel não simula que este módulo já funciona.")
                }
            }
            "Conta" -> {
                title("Conta")
                card("Conta Microsoft / Minecraft", "O login oficial ainda não foi integrado. Esta versão não pede nem armazena sua senha Microsoft. A integração futura deve usar o fluxo oficial de autenticação.") {
                    actionButton("Login Microsoft — em desenvolvimento") { message("Login oficial ainda não disponível nesta versão.") }
                    actionButton("Estado da conta") { message("Nenhuma conta vinculada ao XZeroy.") }
                }
                card("Sessão", "Conta vinculada: não\nGamertag: —\nToken: não armazenado")
            }
            "Ajustes" -> {
                title("Ajustes")
                card("Integração Android", "O serviço é opcional e precisa ser ativado manualmente. Ele permite toques configurados; não injeta código no Minecraft.") {
                    actionButton("Abrir Acessibilidade do Android") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    actionButton("Desativar todos os módulos") {
                        prefs.edit().putBoolean("auto_clicker",false).putBoolean("auto_jump",false).apply()
                        renderTab()
                    }
                }
                card("Sobre", "XZeroy Android • versão 0.2.0 beta\nBuild de desenvolvimento\nMódulos experimentais.")
            }
        }
    }

    private fun title(value: String) {
        body.addView(TextView(this).apply {
            text = value; textSize = 23f; setTextColor(Color.WHITE); setTypeface(null, android.graphics.Typeface.BOLD); setPadding(2, 8, 0, 14)
        })
    }

    private fun card(name: String, description: String, content: (LinearLayout.() -> Unit)? = null) {
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 14, 16, 14); setBackgroundColor(Color.rgb(27, 35, 54)) }
        panel.addView(TextView(this).apply {
            text = name; textSize = 17f; setTextColor(Color.rgb(160, 184, 255)); setTypeface(null, android.graphics.Typeface.BOLD)
        })
        panel.addView(TextView(this).apply { text = description; textSize = 13f; setTextColor(Color.LTGRAY); setPadding(0, 7, 0, 4) })
        content?.invoke(panel)
        body.addView(panel, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 })
    }

    private fun actionButton(label: String, action: () -> Unit) {
        body.addView(Button(this).apply { text = label; isAllCaps = false; setOnClickListener { action() } })
    }

    private fun field(label: String, value: Int) = EditText(this).apply {
        hint = label; setText(value.toString()); setSingleLine(true); inputType = InputType.TYPE_CLASS_NUMBER
        textSize = 14f; setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY)
    }
    private fun number(field: EditText, fallback: Int, min: Int, max: Int) = (field.text.toString().toIntOrNull() ?: fallback).coerceIn(min, max)
    private fun onOff(key: String) = if (prefs.getBoolean(key, false)) "LIGADO" else "DESLIGADO"
    private fun toggle(key: String) { prefs.edit().putBoolean(key, !prefs.getBoolean(key, false)).apply() }
    private fun message(value: String) { android.widget.Toast.makeText(this, value, android.widget.Toast.LENGTH_SHORT).show() }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(this, XzeroyAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }
}

package dev.xzeroy.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
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
import android.widget.Toast
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("xzeroy_settings", MODE_PRIVATE) }
    private val networkExecutor = Executors.newSingleThreadExecutor()
    private lateinit var body: LinearLayout
    private lateinit var connectionStatus: TextView
    private var currentTab = "Início"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 15, 25)
        window.navigationBarColor = Color.rgb(12, 15, 25)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(16, 20, 32))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 22, 22, 18)
            setBackgroundColor(Color.rgb(22, 29, 48))
        }
        header.addView(TextView(this).apply {
            text = "XZEROY"
            textSize = 27f
            setTextColor(Color.rgb(130, 165, 255))
            gravity = Gravity.CENTER_VERTICAL
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        header.addView(TextView(this).apply {
            text = "BEDROCK CLIENT • BETA"
            textSize = 11f
            setTextColor(Color.LTGRAY)
            setPadding(0, 3, 0, 0)
        })
        connectionStatus = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.rgb(255, 211, 128))
            setPadding(0, 12, 0, 0)
        }
        header.addView(connectionStatus)
        root.addView(header)

        val scroll = ScrollView(this)
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 12, 18, 22)
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(4, 7, 4, 7)
            setBackgroundColor(Color.rgb(22, 29, 48))
        }
        listOf("Início", "Módulos", "Conta", "Ajustes").forEach { tab ->
            nav.addView(Button(this).apply {
                text = tab
                textSize = 11f
                isAllCaps = false
                setOnClickListener {
                    currentTab = tab
                    renderTab()
                }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        root.addView(nav)
        setContentView(root)
        renderTab()
    }

    override fun onResume() {
        super.onResume()
        if (::body.isInitialized) renderTab()
    }

    override fun onDestroy() {
        networkExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun renderTab() {
        body.removeAllViews()
        val serviceOn = isAccessibilityServiceEnabled()
        val minecraftInstalled = isMinecraftInstalled()
        val minecraftForeground = prefs.getBoolean("minecraft_foreground", false)

        connectionStatus.text = when {
            minecraftForeground && minecraftInstalled -> "MINECRAFT: detectado em primeiro plano"
            minecraftInstalled && !serviceOn -> "MINECRAFT: instalado • ative Acessibilidade para módulos"
            minecraftInstalled -> "MINECRAFT: instalado • abra o jogo para detectar"
            else -> "MINECRAFT: não encontrado • confira se é o Bedrock oficial"
        }
        when (currentTab) {
            "Início" -> {
                title("Painel")
                card(
                    "Conexão com Minecraft",
                    "Detecção por pacote do Android. O XZeroy abre o Bedrock e detecta o jogo em primeiro plano quando o serviço de Acessibilidade está ativo."
                ) {
                    actionButton("Abrir Minecraft Bedrock") {
                        val intent = packageManager.getLaunchIntentForPackage(MINECRAFT_PACKAGE)
                        if (intent != null) {
                            try {
                                startActivity(intent)
                            } catch (_: Exception) {
                                message("O Android não conseguiu abrir o Minecraft. Confira se o jogo pode ser iniciado normalmente.")
                            }
                        } else if (isMinecraftInstalled()) {
                            message("O pacote está instalado, mas não foi encontrada uma atividade de inicialização.")
                        } else {
                            message("O Minecraft não foi encontrado. A versão oficial Android normalmente usa o pacote com.mojang.minecraftpe.")
                        }
                    }
                    actionButton("Verificar novamente") { renderTab() }
                    actionButton("Abrir Acessibilidade") {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                }
                card(
                    "Estado dos módulos",
                    "Auto Clicker: " + onOff("auto_clicker") +
                        "\nAuto Jump: " + onOff("auto_jump") +
                        "\n\nConfigure as posições e os intervalos na aba Módulos. São toques fixos, não detecção de alvos."
                )
                card(
                    "Limites desta versão",
                    "ESP, leitura de entidades, alteração de alcance e permissões administrativas do servidor ainda não estão implementados. A interface não os marcará como funcionais."
                )
            }

            "Módulos" -> {
                title("Módulos")
                card("Auto Clicker", "Envia toques em posição fixa quando o Minecraft está em primeiro plano.") {
                    val x = field("Coordenada X (%)", prefs.getInt("click_x", 82))
                    val y = field("Coordenada Y (%)", prefs.getInt("click_y", 72))
                    val interval = field("Intervalo em ms (120–2000)", prefs.getInt("click_interval", 180))
                    addView(x)
                    addView(y)
                    addView(interval)
                    actionButton("Salvar Auto Clicker") {
                        prefs.edit()
                            .putInt("click_x", number(x, 82, 0, 100))
                            .putInt("click_y", number(y, 72, 0, 100))
                            .putInt("click_interval", number(interval, 180, 120, 2000))
                            .apply()
                        message("Configuração salva.")
                    }
                    actionButton("Alternar Auto Clicker (" + onOff("auto_clicker") + ")") {
                        toggle("auto_clicker")
                        renderTab()
                    }
                }
                card("Auto Jump", "Envia toques em posição fixa; depende do layout dos controles. Pode funcionar junto do Auto Clicker.") {
                    val x = field("Coordenada X (%)", prefs.getInt("jump_x", 92))
                    val y = field("Coordenada Y (%)", prefs.getInt("jump_y", 55))
                    val interval = field("Intervalo em ms (250–5000)", prefs.getInt("jump_interval", 700))
                    addView(x)
                    addView(y)
                    addView(interval)
                    actionButton("Salvar Auto Jump") {
                        prefs.edit()
                            .putInt("jump_x", number(x, 92, 0, 100))
                            .putInt("jump_y", number(y, 55, 0, 100))
                            .putInt("jump_interval", number(interval, 700, 250, 5000))
                            .apply()
                        message("Configuração salva.")
                    }
                    actionButton("Alternar Auto Jump (" + onOff("auto_jump") + ")") {
                        toggle("auto_jump")
                        renderTab()
                    }
                }
                listOf("ESP", "Reach / alcance", "Player / entidades", "Inventário", "Admin de servidor").forEach { module ->
                    card(module, "Ainda não implementado nesta versão. Nenhum recurso é simulado como se já funcionasse.")
                }
            }

            "Conta" -> {
                title("Conta Microsoft")
                if (prefs.getBoolean("ms_signed_in", false)) {
                    card("Conta conectada",
                        "Nome: " + prefs.getString("ms_display_name", "Conta Microsoft") +
                            "\nConta: " + prefs.getString("ms_username", "") +
                            "\n\nO perfil Microsoft foi conectado. A sessão Xbox/Minecraft exige integração adicional.") {
                        actionButton("Desconectar conta") {
                            prefs.edit().remove("ms_signed_in").remove("ms_display_name")
                                .remove("ms_username").remove("ms_user_id").apply()
                            renderTab()
                        }
                    }
                } else {
                    card("Entrar com Microsoft",
                        "Conecte sua conta pela página oficial da Microsoft. Você não precisa digitar ID nem senha dentro do XZeroy.") {
                        actionButton("Entrar com Microsoft") {
                            val clientId = BuildConfig.MICROSOFT_CLIENT_ID.trim()
                            if (!isGuid(clientId)) {
                                message("O login ainda não foi configurado pelo desenvolvedor. Você não precisa informar nenhum ID; essa configuração deve vir no aplicativo.")
                            } else {
                                startMicrosoftSignIn(clientId)
                            }
                        }
                    }
                    card("Status",
                        if (BuildConfig.MICROSOFT_CLIENT_ID.isBlank())
                            "O campo de ID foi removido. Para que o botão autentique de verdade, o desenvolvedor precisa configurar o registro oficial do XZeroy antes de gerar o APK."
                        else "O login usa a página oficial da Microsoft e consulta o perfil básico.")
                }
                card("Importante",
                    "O perfil Microsoft não equivale a uma sessão Xbox Live/XSTS nem confirma acesso ao Minecraft. Essa integração é uma etapa separada.")
            }

            "Ajustes" -> {
                title("Ajustes")
                card("Integração Android", "O serviço é opcional e precisa ser ativado manualmente. Ele envia toques configurados; não injeta código no Minecraft.") {
                    actionButton("Abrir Acessibilidade do Android") {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    actionButton("Desativar todos os módulos") {
                        prefs.edit().putBoolean("auto_clicker", false).putBoolean("auto_jump", false).apply()
                        renderTab()
                    }
                }
                card("Sobre", "XZeroy Android • versão 0.4.0 beta\nBuild de desenvolvimento")
            }
        }
    }

    private fun startMicrosoftSignIn(clientId: String) {
        message("Solicitando código de login Microsoft…")
        networkExecutor.execute {
            try {
                val device = MicrosoftDeviceAuth.requestDeviceCode(clientId)
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    val instructions = TextView(this).apply {
                        text = "Abra: " + device.verificationUri +
                            "\n\nCódigo: " + device.userCode +
                            "\n\nUse esse código na página oficial da Microsoft. Mantenha o XZeroy instalado e aguarde a conclusão do login."
                        textSize = 16f
                        setTextColor(Color.WHITE)
                        setTextIsSelectable(true)
                        setPadding(28, 22, 28, 22)
                    }
                    AlertDialog.Builder(this)
                        .setTitle("Entrar com Microsoft")
                        .setView(instructions)
                        .setPositiveButton("Abrir navegador") { _, _ ->
                            try {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(device.verificationUri)))
                            } catch (_: Exception) {
                                message("Abra o endereço exibido manualmente no navegador.")
                            }
                        }
                        .setNegativeButton("Fechar", null)
                        .show()
                }

                val profile = MicrosoftDeviceAuth.waitForProfile(clientId, device)
                prefs.edit()
                    .putBoolean("ms_signed_in", true)
                    .putString("ms_user_id", profile.id)
                    .putString("ms_display_name", profile.displayName)
                    .putString("ms_username", profile.username)
                    .apply()

                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        message("Perfil Microsoft conectado: " + profile.displayName)
                        if (currentTab == "Conta") renderTab()
                    }
                }
            } catch (error: Exception) {
                prefs.edit().putBoolean("ms_signed_in", false).apply()
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        val reason = error.message?.take(180)?.ifBlank { null } ?: "Erro desconhecido."
                        message("Login Microsoft não concluído: " + reason)
                        if (currentTab == "Conta") renderTab()
                    }
                }
            }
        }
    }

    private fun isGuid(value: String): Boolean =
        value.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))

    private fun isMinecraftInstalled(): Boolean {
        return try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(MINECRAFT_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun title(value: String) {
        body.addView(TextView(this).apply {
            text = value
            textSize = 23f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(2, 8, 0, 14)
        })
    }

    private fun card(name: String, description: String, content: (LinearLayout.() -> Unit)? = null) {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 14)
            setBackgroundColor(Color.rgb(27, 35, 54))
        }
        panel.addView(TextView(this).apply {
            text = name
            textSize = 17f
            setTextColor(Color.rgb(160, 184, 255))
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        panel.addView(TextView(this).apply {
            text = description
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(0, 7, 0, 8)
        })
        content?.invoke(panel)
        body.addView(panel, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 })
    }

    // Buttons belong to the card that owns their controls, not the whole scroll body.
    private fun LinearLayout.actionButton(label: String, action: () -> Unit) {
        addView(Button(this@MainActivity).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 5
        })
    }

    private fun field(label: String, value: Int) = EditText(this).apply {
        hint = label
        setText(value.toString())
        setSingleLine(true)
        inputType = InputType.TYPE_CLASS_NUMBER
        textSize = 14f
        setTextColor(Color.WHITE)
        setHintTextColor(Color.LTGRAY)
    }

    private fun textField(label: String, value: String) = EditText(this).apply {
        hint = label
        setText(value)
        setSingleLine(true)
        inputType = InputType.TYPE_CLASS_TEXT
        textSize = 14f
        setTextColor(Color.WHITE)
        setHintTextColor(Color.LTGRAY)
        setSelectAllOnFocus(false)
    }

    private fun number(field: EditText, fallback: Int, min: Int, max: Int) =
        (field.text.toString().toIntOrNull() ?: fallback).coerceIn(min, max)

    private fun onOff(key: String) =
        if (prefs.getBoolean(key, false)) "LIGADO" else "DESLIGADO"

    private fun toggle(key: String) {
        prefs.edit().putBoolean(key, !prefs.getBoolean(key, false)).apply()
    }

    private fun message(value: String) {
        Toast.makeText(this, value, Toast.LENGTH_LONG).show()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(this, XzeroyAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    companion object {
        private const val MINECRAFT_PACKAGE = "com.mojang.minecraftpe"
    }
}

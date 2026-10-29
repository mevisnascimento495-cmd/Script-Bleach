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
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("xzeroy_settings", MODE_PRIVATE) }
    private val networkExecutor = Executors.newSingleThreadExecutor()
    private lateinit var body: LinearLayout
    private lateinit var connectionStatus: TextView
    private var currentTab = "Início"
    private var selectedModuleCategory = ModuleCategory.COMBAT
    private var moduleSearchQuery = ""
    private var moduleListContainer: LinearLayout? = null
    private val profileStore by lazy { XzeroyProfileStore(this) }
    private val moduleStore by lazy { XzeroySettingsStore(this) }
    private val relayController by lazy { XzeroyRelayController.get(applicationContext) }
    private var relayObservation: AutoCloseable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(10, 12, 21)
        window.navigationBarColor = Color.rgb(10, 12, 21)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(10, 12, 21))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 22, 22, 18)
            background = uiPanel(Color.rgb(19, 24, 40), Color.rgb(53, 61, 88))
        }
        header.addView(TextView(this).apply {
            text = "XZEROY"
            textSize = 27f
            setTextColor(Color.rgb(176, 160, 255))
            gravity = Gravity.CENTER_VERTICAL
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        header.addView(TextView(this).apply {
            text = "BEDROCK CLIENT  •  ANDROID BETA"
            textSize = 11f
            setTextColor(Color.LTGRAY)
            setPadding(0, 3, 0, 0)
        })
        connectionStatus = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.rgb(193, 184, 255))
            setPadding(10, 10, 10, 10)
            background = uiPanel(Color.rgb(31, 34, 58), Color.rgb(75, 67, 121))
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
            background = uiPanel(Color.rgb(18, 23, 38), Color.rgb(44, 51, 72))
        }
        listOf("Início", "Módulos", "Relay", "Conta", "Ajustes").forEach { tab ->
            nav.addView(Button(this).apply {
                text = tab
                textSize = 10f
                isAllCaps = false
                setTextColor(if (currentTab == tab) Color.WHITE else Color.rgb(175, 183, 205))
                background = uiPanel(
                    if (currentTab == tab) Color.rgb(76, 61, 156) else Color.rgb(24, 29, 46),
                    if (currentTab == tab) Color.rgb(150, 129, 255) else Color.rgb(43, 50, 72)
                )
                setOnClickListener {
                    currentTab = tab
                    renderTab()
                }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(2, 0, 2, 0)
            })
        }
        root.addView(nav)
        setContentView(root)
        relayObservation = relayController.observe {
            runOnUiThread { if (!isFinishing && !isDestroyed && ::body.isInitialized && currentTab == "Relay") renderTab() }
        }
        renderTab()
    }

    override fun onResume() {
        super.onResume()
        if (::body.isInitialized) renderTab()
    }

    override fun onDestroy() {
        relayObservation?.close()
        networkExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun renderTab() {
        body.removeAllViews()
        val serviceOn = isAccessibilityServiceEnabled()
        val minecraftInstalled = isMinecraftInstalled()
        val minecraftForeground = prefs.getBoolean("minecraft_foreground", false)

        connectionStatus.text = when {
            relayController.snapshot().status == RelayStatus.RUNNING -> "RELAY: ativo • UDP " + relayController.snapshot().localPort
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
                    "O relay e a engine ProtoHax estão integrados, mas exigem protocolo Bedrock compatível e não fornecem autenticação Xbox/Minecraft. Renderização ESP e leitura visual de entidades continuam pendentes."
                )
            }

            "Módulos" -> {
                title("Client")
                body.addView(TextView(this).apply {
                    text = "MÓDULOS  /  CONFIGURAÇÃO"
                    textSize = 10f
                    setTextColor(Color.rgb(160, 149, 235))
                    setPadding(3, 0, 0, 8)
                })
                val search = textField("Buscar módulo...", moduleSearchQuery).apply {
                    textSize = 14f
                    background = uiPanel(Color.rgb(20, 25, 41), Color.rgb(58, 65, 91))
                    setPadding(14, 10, 14, 10)
                    addTextChangedListener(object : android.text.TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            moduleSearchQuery = s?.toString().orEmpty()
                            renderModuleList()
                        }
                        override fun afterTextChanged(s: android.text.Editable?) = Unit
                    })
                }
                body.addView(search, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
                renderCategorySelector()
                moduleListContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                body.addView(moduleListContainer, LinearLayout.LayoutParams(-1, -2))
                renderModuleList()
            }

            "Relay" -> renderRelayTab()

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
                card("Perfis de configuração", "Salve e carregue combinações de módulos e valores. Os perfis ficam apenas neste dispositivo.") {
                    actionButton("Salvar perfil atual") { showSaveProfileDialog() }
                    actionButton("Carregar perfil") { showLoadProfileDialog() }
                    actionButton("Excluir perfil") { showDeleteProfileDialog() }
                }
                card("Integração Android", "O serviço é opcional e precisa ser ativado manualmente. Ele envia toques configurados; não injeta código no Minecraft.") {
                    actionButton("Abrir Acessibilidade do Android") {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    actionButton("Desativar todos os módulos") {
                        moduleStore.disableAll()
                        renderTab()
                    }
                }
                card("Sobre", "XZeroy Android • versão 0.6.0 beta\nBuild de desenvolvimento")
            }
        }
    }


    private fun renderRelayTab() {
        title("Relay Bedrock")
        val state = relayController.snapshot()
        card("Estado: " + state.status.label, state.message +
            "\nDestino: " + (state.targetHost.ifBlank { "não configurado" }) + ":" + state.targetPort +
            "\nPorta local: UDP " + state.localPort) {
            actionButton("Atualizar estado") { renderTab() }
            actionButton("Parar relay") {
                if (!relayController.stop()) message("O relay já está parado ou encerrando.")
                renderTab()
            }
        }
        val hostField = textField("IP ou domínio do servidor", prefs.getString("relay_target_host", "") ?: "")
        val targetPortField = field("Porta do servidor de destino", prefs.getInt("relay_target_port", 19132))
        val localPortField = field("Porta local do relay", prefs.getInt("relay_local_port", 19136))
        card("Destino do servidor", "O XZeroy recebe a conexão do Minecraft em uma porta UDP local e encaminha os pacotes ao destino configurado.") {
            addView(hostField)
            addView(targetPortField)
            addView(localPortField)
            actionButton("Iniciar relay") {
                val host = hostField.text.toString().trim()
                val targetPort = number(targetPortField, 19132, 1, 65535)
                val localPort = number(localPortField, 19136, 1024, 65535)
                if (host.isBlank()) {
                    message("Informe o IP ou domínio do servidor de destino.")
                    return@actionButton
                }
                prefs.edit().putString("relay_target_host", host).putInt("relay_target_port", targetPort).putInt("relay_local_port", localPort).apply()
                if (!relayController.start(host, targetPort, localPort)) message("O relay já está iniciando, ativo ou com dados inválidos.")
                renderTab()
            }
        }
        card("Como conectar", "Quando o estado ficar Ativo, abra o Minecraft Bedrock e adicione um servidor com endereço 127.0.0.1 e a porta local exibida.")
        card("Limitação de autenticação", "Esta integração usa o listener de sessão offline do ProtoHax. Ela NÃO cria tokens Xbox Live/XSTS/Minecraft; servidores públicos que exigem autenticação Xbox podem recusar o login.")
        card("Compatibilidade", "Os codecs do ProtoHax estão fixados em versões anteriores e ainda não foram validados com o Bedrock atual. Teste apenas em um servidor que você controla ou tem permissão para usar.")
    }

    private fun renderCategorySelector() {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 10)
        }
        ModuleCategory.values().forEach { category ->
            row.addView(Button(this).apply {
                text = category.label
                textSize = 10f
                isAllCaps = false
                setTextColor(if (selectedModuleCategory == category) Color.WHITE else Color.rgb(174, 182, 206))
                background = uiPanel(
                    if (selectedModuleCategory == category) Color.rgb(76, 61, 156) else Color.rgb(22, 27, 43),
                    if (selectedModuleCategory == category) Color.rgb(150, 129, 255) else Color.rgb(44, 51, 73)
                )
                setOnClickListener {
                    selectedModuleCategory = category
                    renderTab()
                }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(2, 0, 2, 0)
            })
        }
        body.addView(row, LinearLayout.LayoutParams(-1, -2))
    }

    private fun renderModuleList() {
        val container = moduleListContainer ?: return
        container.removeAllViews()
        val query = moduleSearchQuery.trim().lowercase()
        val modules = XzeroyModuleRegistry.forCategory(selectedModuleCategory).filter {
            query.isBlank() || it.name.lowercase().contains(query) || it.description.lowercase().contains(query)
        }
        val active = modules.count { moduleStore.isEnabled(it) }
        container.addView(TextView(this).apply {
            text = "\${active} ATIVOS  •  \${modules.size} MÓDULOS"
            textSize = 10f
            setTextColor(Color.rgb(155, 149, 195))
            setPadding(3, 1, 0, 8)
        })
        if (modules.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "Nenhum módulo encontrado nesta categoria."
                textSize = 13f
                setTextColor(Color.LTGRAY)
                setPadding(6, 15, 6, 20)
            })
            return
        }
        modules.forEach { renderModuleCard(it) }
    }

    private fun renderModuleCard(module: XzeroyModuleDefinition) {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            background = uiPanel(Color.rgb(18, 23, 38), Color.rgb(48, 56, 79))
            elevation = 2f
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleColumn.addView(TextView(this).apply {
            text = module.name
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
        })
        val relayWaiting = module.engineClass != null && relayController.snapshot().status != RelayStatus.RUNNING
        val stateLabel = TextView(this).apply {
            text = when {
                !module.implemented -> "EM DESENVOLVIMENTO"
                module.enabledPreference != null && moduleStore.isEnabled(module) && relayWaiting -> "ATIVO • AGUARDANDO RELAY"
                module.enabledPreference != null && moduleStore.isEnabled(module) -> "ATIVO"
                else -> "DESATIVADO"
            }
            textSize = 9f
            setTextColor(when {
                !module.implemented -> Color.rgb(230, 174, 112)
                moduleStore.isEnabled(module) -> Color.rgb(102, 222, 169)
                else -> Color.rgb(147, 157, 182)
            })
            setPadding(0, 4, 0, 0)
        }
        titleColumn.addView(stateLabel)
        top.addView(titleColumn, LinearLayout.LayoutParams(0, -2, 1f))

        if (module.implemented && module.enabledPreference != null) {
            top.addView(android.widget.Switch(this).apply {
                isChecked = moduleStore.isEnabled(module)
                text = ""
                setOnCheckedChangeListener { _, enabled ->
                    moduleStore.setEnabled(module, enabled)
                    stateLabel.text = when {
                        enabled && module.engineClass != null && relayController.snapshot().status != RelayStatus.RUNNING -> "ATIVO • AGUARDANDO RELAY"
                        enabled -> "ATIVO"
                        else -> "DESATIVADO"
                    }
                    stateLabel.setTextColor(if (enabled) Color.rgb(102, 222, 169) else Color.rgb(147, 157, 182))
                }
            })
        } else {
            top.addView(TextView(this).apply {
                text = "—"
                textSize = 14f
                setTextColor(Color.GRAY)
                setPadding(8, 0, 0, 0)
            })
        }
        panel.addView(top)

        panel.addView(TextView(this).apply {
            val relayHint = if (module.engineClass != null && relayWaiting) {
                "\n\nEste módulo depende do Relay e de uma sessão Bedrock compatível."
            } else ""
            text = module.description + relayHint
            textSize = 12f
            setTextColor(Color.rgb(174, 183, 204))
            setPadding(0, 8, 0, 9)
        })

        val hasSettings = module.settings.isNotEmpty() || module.engineClass != null
        if (hasSettings) {
            panel.addView(Button(this).apply {
                text = "⚙  Configurar opções"
                textSize = 11f
                isAllCaps = false
                setTextColor(Color.rgb(204, 196, 255))
                background = uiPanel(Color.rgb(38, 34, 63), Color.rgb(84, 72, 134))
                setOnClickListener {
                    if (module.engineClass != null) showEngineModuleSettings(module)
                    else showSimpleModuleSettings(module)
                }
            }, LinearLayout.LayoutParams(-1, -2))
        } else if (!module.implemented) {
            panel.addView(TextView(this).apply {
                text = "Ainda não disponível nesta versão"
                textSize = 10f
                setTextColor(Color.rgb(219, 171, 116))
            })
        }
        moduleListContainer?.addView(panel, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = 9
        })
    }

    private fun showSimpleModuleSettings(module: XzeroyModuleDefinition) {
        if (module.settings.isEmpty()) {
            message("Este módulo não tem opções editáveis.")
            return
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 8, 20, 8)
        }
        val fields = linkedMapOf<String, EditText>()
        module.settings.forEach { setting ->
            content.addView(TextView(this).apply {
                text = "\${setting.label}  •  \${setting.minValue}–\${setting.maxValue}"
                textSize = 12f
                setTextColor(Color.LTGRAY)
                setPadding(0, 8, 0, 2)
            })
            fields[setting.key] = EditText(this).apply {
                setSingleLine(true)
                inputType = InputType.TYPE_CLASS_NUMBER
                setText(prefs.getInt(setting.key, setting.defaultValue).toString())
                setTextColor(Color.WHITE)
                setHintTextColor(Color.GRAY)
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(133, 113, 255))
            }.also { content.addView(it) }
        }
        AlertDialog.Builder(this)
            .setTitle("Configurar • \${module.name}")
            .setView(content)
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Desativar") { _, _ -> moduleStore.setEnabled(module, false) }
            .setPositiveButton("Salvar") { _, _ ->
                moduleStore.saveSettings(module, fields.mapValues { (key, input) ->
                    val spec = module.settings.first { it.key == key }
                    (input.text.toString().toIntOrNull() ?: spec.defaultValue).coerceIn(spec.minValue, spec.maxValue)
                })
                message("Configurações de \${module.name} salvas.")
            }
            .show()
    }

    private fun showEngineModuleSettings(module: XzeroyModuleDefinition) {
        val settings = relayController.engineValues(module.id)
        if (settings == null) {
            message("Inicie o Relay na aba Relay para carregar os valores internos desse módulo.")
            return
        }
        if (settings.isEmpty()) {
            message("Esse módulo não expõe valores editáveis.")
            return
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 8, 16, 8)
        }
        val edits = linkedMapOf<String, EditText>()
        val checks = linkedMapOf<String, CheckBox>()
        val choices = linkedMapOf<String, Spinner>()

        settings.forEach { setting ->
            when (setting.type) {
                "boolean" -> {
                    val check = CheckBox(this).apply {
                        text = setting.name
                        setTextColor(Color.WHITE)
                        isChecked = setting.currentValue.equals("true", true)
                    }
                    container.addView(check)
                    checks[setting.name] = check
                }
                "choice" -> {
                    container.addView(TextView(this).apply {
                        text = setting.name
                        textSize = 13f
                        setTextColor(Color.LTGRAY)
                        setPadding(0, 8, 0, 3)
                    })
                    val spinner = Spinner(this)
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, setting.choices)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = adapter
                    val index = setting.choices.indexOfFirst { it.equals(setting.currentValue, true) }
                    if (index >= 0) spinner.setSelection(index)
                    container.addView(spinner)
                    choices[setting.name] = spinner
                }
                else -> {
                    container.addView(TextView(this).apply {
                        text = if (setting.hint.isBlank()) setting.name else setting.name + " • " + setting.hint
                        textSize = 13f
                        setTextColor(Color.LTGRAY)
                        setPadding(0, 8, 0, 3)
                    })
                    val input = EditText(this).apply {
                        setSingleLine(true)
                        setText(setting.currentValue)
                        hint = setting.type
                        inputType = when (setting.type) {
                            "integer" -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
                            "decimal" -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_NUMBER_FLAG_DECIMAL
                            else -> InputType.TYPE_CLASS_TEXT
                        }
                    }
                    container.addView(input)
                    edits[setting.name] = input
                }
            }
        }

        val scroll = ScrollView(this).apply { addView(container) }
        AlertDialog.Builder(this)
            .setTitle("Configurar • " + module.name)
            .setView(scroll)
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Restaurar padrões") { _, _ ->
                if (relayController.resetEngineValues(module.id)) {
                    message("Valores padrão restaurados para " + module.name + ".")
                    renderTab()
                } else {
                    message("Não foi possível restaurar os padrões.")
                }
            }
            .setPositiveButton("Salvar") { _, _ ->
                val updated = linkedMapOf<String, String>()
                edits.forEach { (key, input) -> updated[key] = input.text.toString() }
                checks.forEach { (key, check) -> updated[key] = check.isChecked.toString() }
                choices.forEach { (key, spinner) -> updated[key] = spinner.selectedItem?.toString().orEmpty() }
                val error = relayController.saveEngineValues(module.id, updated)
                if (error != null) message(error)
                else {
                    message("Valores da engine salvos para " + module.name + ".")
                    renderTab()
                }
            }
            .show()
    }

    private fun showSaveProfileDialog() {
        val input = EditText(this).apply {
            setSingleLine(true)
            hint = "Nome do perfil"
            setText("Meu perfil")
            inputType = InputType.TYPE_CLASS_TEXT
        }
        AlertDialog.Builder(this)
            .setTitle("Salvar perfil")
            .setMessage("O perfil guarda estados e valores dos módulos conhecidos.")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ ->
                val name = profileStore.saveProfile(input.text.toString(), prefs)
                if (name == null) message("Escolha um nome válido (até 32 caracteres).")
                else message("Perfil '" + name + "' salvo.")
            }
            .show()
    }

    private fun showLoadProfileDialog() {
        val names = profileStore.listProfiles()
        if (names.isEmpty()) {
            message("Ainda não há perfis salvos.")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Carregar perfil")
            .setItems(names.toTypedArray()) { _, index ->
                if (profileStore.loadProfile(names[index], prefs)) {
                    relayController.reloadStoredEngineValues()
                    message("Perfil '" + names[index] + "' carregado.")
                    renderTab()
                } else {
                    message("Não foi possível carregar esse perfil.")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteProfileDialog() {
        val names = profileStore.listProfiles()
        if (names.isEmpty()) {
            message("Ainda não há perfis salvos.")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Excluir perfil")
            .setItems(names.toTypedArray()) { _, index ->
                if (profileStore.deleteProfile(names[index])) message("Perfil excluído.")
                else message("Não foi possível excluir o perfil.")
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
            background = uiPanel(Color.rgb(18, 23, 38), Color.rgb(48, 56, 79))
            elevation = 2f
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
            textSize = 12f
            setTextColor(Color.rgb(213, 207, 255))
            background = uiPanel(Color.rgb(37, 34, 60), Color.rgb(79, 69, 126))
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 5
        })
    }

    private fun uiPanel(fill: Int, stroke: Int) = android.graphics.drawable.GradientDrawable().apply {
        setColor(fill)
        cornerRadius = 18f
        setStroke(1, stroke)
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
        val next = !prefs.getBoolean(key, false)
        prefs.edit().putBoolean(key, next).apply()
        XzeroyModuleRegistry.all.firstOrNull { it.enabledPreference == key }?.let {
            XzeroyModuleEvents.publish(it.id, next)
        }
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

package dev.xzeroy.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val enabled = linkedSetOf<String>()
    private lateinit var status: TextView

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
            text = "Bedrock • protótipo nativo"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 20)
        })
        status = TextView(this).apply {
            text = "Interface local; Minecraft não conectado."
            textSize = 14f
            setTextColor(Color.rgb(255, 210, 120))
            setPadding(0, 8, 0, 16)
        }
        content.addView(status)

        listOf("Auto Jump", "Auto Clicker", "Coordenadas", "ESP (pendente)").forEach { module ->
            content.addView(Button(this).apply {
                text = "$module • DESLIGADO"
                setOnClickListener {
                    if (enabled.remove(module)) {
                        text = "$module • DESLIGADO"
                    } else {
                        enabled.add(module)
                        text = "$module • LIGADO"
                    }
                    status.text = "Seleção visual: " + enabled.size + " módulo(s). Sem integração com o jogo."
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        content.addView(TextView(this).apply {
            text = "\nAviso: os botões são demonstrativos. Não alteram o jogo nem concedem permissões no servidor."
            textSize = 13f
            setTextColor(Color.LTGRAY)
        })
        setContentView(ScrollView(this).apply { addView(content) })
    }
}

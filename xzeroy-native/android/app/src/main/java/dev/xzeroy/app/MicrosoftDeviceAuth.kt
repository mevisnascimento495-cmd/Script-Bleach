package dev.xzeroy.app

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Microsoft identity-platform device-code sign-in.
 * Access/refresh tokens stay in memory and are never persisted by this class.
 */
internal object MicrosoftDeviceAuth {
    data class DeviceCode(
        val deviceCode: String,
        val userCode: String,
        val verificationUri: String,
        val intervalSeconds: Int,
        val expiresInSeconds: Int,
        val message: String
    )

    data class Profile(
        val id: String,
        val displayName: String,
        val username: String
    )

    fun requestDeviceCode(clientId: String): DeviceCode {
        val json = postForm(
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode",
            mapOf(
                "client_id" to clientId,
                "scope" to "User.Read openid profile offline_access"
            )
        )
        throwIfError(json)
        return DeviceCode(
            deviceCode = required(json, "device_code"),
            userCode = required(json, "user_code"),
            verificationUri = required(json, "verification_uri"),
            intervalSeconds = json.optInt("interval", 5).coerceAtLeast(1),
            expiresInSeconds = json.optInt("expires_in", 900).coerceAtLeast(60),
            message = json.optString("message", "Abra a página de verificação e informe o código exibido.")
        )
    }

    fun waitForProfile(clientId: String, device: DeviceCode): Profile {
        val expiresAt = System.currentTimeMillis() + device.expiresInSeconds * 1000L
        var intervalSeconds = device.intervalSeconds

        while (System.currentTimeMillis() < expiresAt) {
            Thread.sleep(intervalSeconds * 1000L)
            if (Thread.currentThread().isInterrupted) {
                throw InterruptedException("Login cancelado.")
            }

            val token = postForm(
                "https://login.microsoftonline.com/consumers/oauth2/v2.0/token",
                mapOf(
                    "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                    "client_id" to clientId,
                    "device_code" to device.deviceCode
                )
            )

            if (token.has("access_token")) {
                val accessToken = required(token, "access_token")
                return fetchProfile(accessToken)
            }

            when (token.optString("error")) {
                "authorization_pending" -> Unit
                "slow_down" -> intervalSeconds += 5
                "authorization_declined" -> throw IllegalStateException("O login foi recusado na página da Microsoft.")
                "expired_token", "bad_verification_code" -> throw IllegalStateException("O código de login expirou. Inicie o login novamente.")
                else -> throwIfError(token)
            }
        }
        throw IllegalStateException("O código de login expirou. Inicie o login novamente.")
    }

    private fun fetchProfile(accessToken: String): Profile {
        val connection = (URL("https://graph.microsoft.com/v1.0/me?\$select=id,displayName,userPrincipalName,mail").openConnection() as HttpURLConnection)
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("Authorization", "Bearer " + accessToken)
        connection.setRequestProperty("Accept", "application/json")

        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = JSONObject(body.ifBlank { "{}" })
            if (status !in 200..299) {
                throw IllegalStateException(
                    json.optJSONObject("error")?.optString("message")
                        ?: "A Microsoft autenticou, mas não foi possível ler o perfil (HTTP " + status + ")."
                )
            }
            return Profile(
                id = required(json, "id"),
                displayName = json.optString("displayName", "Conta Microsoft"),
                username = json.optString("mail").ifBlank { json.optString("userPrincipalName") }
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun postForm(url: String, values: Map<String, String>): JSONObject {
        val encoded = values.entries.joinToString("&") { (key, value) ->
            encode(key) + "=" + encode(value)
        }
        val connection = (URL(url).openConnection() as HttpURLConnection)
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        connection.setRequestProperty("Accept", "application/json")

        try {
            connection.outputStream.use { output ->
                output.write(encoded.toByteArray(StandardCharsets.UTF_8))
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = JSONObject(body.ifBlank { "{}" })
            if (status !in 200..299 && !json.has("error")) {
                throw IllegalStateException("Falha no serviço de autenticação Microsoft (HTTP " + status + ").")
            }
            return json
        } finally {
            connection.disconnect()
        }
    }

    private fun throwIfError(json: JSONObject) {
        if (json.has("error")) {
            val description = json.optString("error_description").substringBefore("\r").substringBefore("\n")
            val error = json.optString("error")
            throw IllegalStateException(
                if (description.isNotBlank()) description.take(220)
                else "Erro de autenticação Microsoft: " + error
            )
        }
    }

    private fun required(json: JSONObject, key: String): String {
        return json.optString(key).takeIf { it.isNotBlank() && it != "null" }
            ?: throw IllegalStateException("Resposta inesperada do serviço Microsoft: faltou " + key + ".")
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}

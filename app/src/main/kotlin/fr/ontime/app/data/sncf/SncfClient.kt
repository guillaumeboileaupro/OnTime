package fr.ontime.app.data.sncf

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Base64

sealed interface SncfResponse {
    data class Body(val json: String) : SncfResponse
    data object Unauthorized : SncfResponse
    data object QuotaExceeded : SncfResponse
    data object Failed : SncfResponse
}

/** Authenticated GET on the SNCF coverage; [pathAndQuery] starts with `/`. */
fun interface SncfApi {
    fun get(pathAndQuery: String): SncfResponse
}

internal fun encodeSegment(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

/** Blocking client for the SNCF (Navitia) API; call it off the main thread. */
class SncfClient(
    private val apiKey: String,
    private val baseUrl: String = "https://api.sncf.com/v1/coverage/sncf",
) : SncfApi {
    init {
        require(apiKey.isNotBlank())
    }

    override fun get(pathAndQuery: String): SncfResponse {
        val connection = URL(baseUrl + pathAndQuery).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            val token = Base64.getEncoder().encodeToString("$apiKey:".toByteArray(Charsets.UTF_8))
            connection.setRequestProperty("Authorization", "Basic $token")
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK ->
                    SncfResponse.Body(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN -> SncfResponse.Unauthorized
                HTTP_TOO_MANY_REQUESTS -> SncfResponse.QuotaExceeded
                else -> SncfResponse.Failed
            }
        } catch (_: IOException) {
            SncfResponse.Failed
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}

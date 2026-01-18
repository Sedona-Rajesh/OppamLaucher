package com.oppam.oppamlauncher.location

import android.content.Context
import android.util.Log
import com.oppam.oppamlauncher.auth.UserPreferences
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * Sends location updates to a caregiver platform via HTTP POST (JSON payload).
 * Endpoint is configured via UserPreferences (caregiverPlatformUrl).
 */
class WebhookLocationTransmitter : LocationTransmitter {
    override fun sendLocationUpdate(
        context: Context,
        lat: Double,
        lng: Double,
        accuracy: Float,
        timestamp: Long
    ) {
        val prefs = UserPreferences(context)
        val endpoint = prefs.getCaregiverPlatformUrl()
        if (endpoint.isBlank()) {
            Log.d("WebhookLocationTx", "No platform URL configured; skipping webhook")
            return
        }
        try {
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            val payload = JSONObject().apply {
                put("type", "OPPAM_LOC")
                put("lat", lat)
                put("lng", lng)
                put("accuracy", accuracy)
                put("timestamp", timestamp)
                put("elderName", prefs.getUserName())
                put("elderPhone", prefs.getUserPhone())
                put("caregiverName", prefs.getCaregiverName())
                put("caregiverPhone", prefs.getCaregiverPhone())
            }.toString()
            BufferedOutputStream(conn.outputStream).use { out ->
                out.write(payload.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            val code = conn.responseCode
            val resp = try {
                BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            } catch (e: Exception) {
                ""
            }
            Log.i("WebhookLocationTx", "POST $endpoint -> $code; resp=${resp.take(120)}")
            conn.disconnect()
        } catch (e: Exception) {
            Log.e("WebhookLocationTx", "Failed to POST location", e)
        }
    }
}

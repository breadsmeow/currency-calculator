package com.yendollar

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Fetches the USD→JPY rate and keeps the last good value on the phone. */
object RateRepository {
    private const val PREFS = "rates"
    private const val KEY_RATE = "jpy_per_usd"
    private const val KEY_UPDATED = "updated_at"
    private const val RATE_URL = "https://open.er-api.com/v6/latest/USD"

    /** Used only until the first successful fetch (mid-market, Oct 22 2026). */
    const val FALLBACK_RATE = 157.2

    /** Yen per 1 US dollar. */
    fun rate(context: Context): Double =
        prefs(context).getString(KEY_RATE, null)?.toDoubleOrNull() ?: FALLBACK_RATE

    /** When the saved rate was fetched, in epoch millis, or 0 if never. */
    fun updatedAt(context: Context): Long = prefs(context).getLong(KEY_UPDATED, 0L)

    suspend fun refresh(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL(RATE_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            try {
                if (conn.responseCode != 200) return@withContext false
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                if (json.optString("result") != "success") return@withContext false
                val jpy = json.getJSONObject("rates").getDouble("JPY")
                if (jpy <= 0.0) return@withContext false
                prefs(context).edit()
                    .putString(KEY_RATE, jpy.toString())
                    .putLong(KEY_UPDATED, System.currentTimeMillis())
                    .apply()
                true
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

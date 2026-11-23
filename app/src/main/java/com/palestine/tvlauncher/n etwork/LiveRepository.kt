package com.palestine.tvlauncher.network

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.palestine.tvlauncher.data.PalestineData
import com.palestine.tvlauncher.data.PalestinianCity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class MatchItem(val name: String, val status: String)

data class LiveFeedState(
    val currentTemp: Int = 25,
    val dailyMax: List<Int> = listOf(27, 28, 26, 25, 24),
    val dailyMin: List<Int> = listOf(16, 17, 15, 15, 14),
    val news: List<String> = listOf("أهلاً بكم في لانشر فلسطين TV الذكي"),
    val matches: List<MatchItem> = listOf(MatchItem("متابعة حية للمباريات", "مباشر")),
    val prices: Map<String, Pair<String, Boolean>> = mapOf(
        "USD/ILS" to Pair("3.65", true),
        "JOD/ILS" to Pair("5.15", true),
        "EUR/ILS" to Pair("3.96", false),
        "GOLD" to Pair("2650$", true),
        "BRENT" to Pair("74.2$", false),
        "BTC" to Pair("91200$", true),
        "ETH" to Pair("3120$", true)
    )
)

data class GitHubUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val releaseNotes: String,
    val apkDownloadUrl: String
)

object LiveRepository {
    private const val OWNER = "wael0546264638-dev"
    private const val REPO = "Palestine-TV-Launcher"

    val natureWallpapers = listOf(
        "افتراضي (ليلي هادئ)" to "",
        "جبال وأودية فلسطين" to "https://pexels.com",
        "شاطئ البحر وقت الغروب" to "https://pexels.com",
        "غابة خضراء وضباب" to "https://pexels.com",
        "سماء النجوم والليل" to "https://pexels.com"
    )

    private fun httpGet(urlStr: String): String {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    suspend fun autoDetectLocation(): PalestinianCity? = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(httpGet("http://ip-api.com"))
            if (json.optString("status") == "success") {
                val city = json.optString("city", "أريحا")
                val country = json.optString("country", "فلسطين")
                val lat = json.optDouble("lat", 32.0450)
                val lon = json.optDouble("lon", 35.4580)
                val tz = json.optString("timezone", "Asia/Hebron")
                PalestinianCity(city, country, lat, lon, tz)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun searchLocationByName(query: String): List<PalestinianCity> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://open-meteo.com"
            val json = JSONObject(httpGet(url))
            val results = json.optJSONArray("results") ?: return@withContext emptyList()
            List(results.length()) { idx ->
                val item = results.getJSONObject(idx)
                val name = item.optString("name", query)
                val country = item.optString("country", "")
                val lat = item.optDouble("latitude", 32.0450)
                val lon = item.optDouble("longitude", 35.4580)
                val tz = item.optString("timezone", "Asia/Hebron")
                PalestinianCity(name, country, lat, lon, tz)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchExactPrayers(
        lat: Double,
        lon: Double,
        timeZoneId: String = "Asia/Hebron"
    ): PalestineData.PrayerDisplay = withContext(Dispatchers.IO) {
        try {
            val url = "https://aladhan.com"
            val json = JSONObject(httpGet(url))
            val timings = json.getJSONObject("data").getJSONObject("timings")
            fun cleanTime(key: String): String =
                timings.optString(key, "05:00").substringBefore(" ")

            val raw = PalestineData.PrayerDisplay(
                fajr = cleanTime("Fajr"),
                sunrise = cleanTime("Sunrise"),
                dhuhr = cleanTime("Dhuhr"),
                asr = cleanTime("Asr"),
                maghrib = cleanTime("Maghrib"),
                isha = cleanTime("Isha")
            )
            PalestineData.computeNextPrayer(raw, timeZoneId)
        } catch (_: Exception) {
            PalestineData.computeNextPrayer(PalestineData.PrayerDisplay(), timeZoneId)
        }
    }

    suspend fun fetchGitHubFeed(): LiveFeedState = withContext(Dispatchers.IO) {
        try {
            val url = "https://githubusercontent.com"
            val json = JSONObject(httpGet(url))
            val weather = json.optJSONObject("weather")
            val curTemp = weather?.optInt("current_temp", 25) ?: 25
            val maxArr = weather?.optJSONArray("daily_max")
            val minArr = weather?.optJSONArray("daily_min")
            val maxList = if (maxArr != null && maxArr.length() > 0) {
                List(maxArr.length()) { maxArr.getInt(it) }
            } else listOf(27, 28, 26, 25, 24)
            val minList = if (minArr != null && minArr.length() > 0) {
                List(minArr.length()) { minArr.getInt(it) }
            } else listOf(16, 17, 15, 15, 14)

            val newsArr = json.optJSONArray("news")
            val newsList = if (newsArr != null && newsArr.length() > 0) {
                List(newsArr.length()) { newsArr.getString(it) }
            } else listOf("متابعة مستمرة للأحوال الجوية والأسعار المحلية في فلسطين")

            val matchArr = json.optJSONArray("matches")
            val matchList = if (matchArr != null && matchArr.length() > 0) {
                List(matchArr.length()) {
                    val m = matchArr.getJSONObject(it)
                    MatchItem(m.optString("name"), m.optString("status"))
                }
            } else listOf(MatchItem("مباريات اليوم", "مباشر"))

            LiveFeedState(
                currentTemp = curTemp,
                dailyMax = maxList,
                dailyMin = minList,
                news = newsList,
                matches = matchList
            )
        } catch (_: Exception) {
            LiveFeedState()
        }
    }

    suspend fun fetchCityWeather(
        lat: Double,
        lon: Double,
        timeZoneId: String = "Asia/Hebron"
    ): Triple<Int, List<Int>, List<Int>>? = withContext(Dispatchers.IO) {
        try {
            val tzEnc = URLEncoder.encode(timeZoneId, "UTF-8")
            val url = "https://open-meteo.com" +
                "&current=temperature_2m&daily=temperature_2m_max,temperature_2m_min&timezone=$tzEnc"
            val json = JSONObject(httpGet(url))
            val cur = json.getJSONObject("current").getDouble("temperature_2m").toInt()
            val daily = json.getJSONObject("daily")
            val maxA = daily.getJSONArray("temperature_2m_max")
            val minA = daily.getJSONArray("temperature_2m_min")
            val maxL = List(minOf(5, maxA.length())) { maxA.getDouble(it).toInt() }
            val minL = List(minOf(5, minA.length())) { minA.getDouble(it).toInt() }
            Triple(cur, maxL, minL)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchInstantPrices(): Map<String, Pair<String, Boolean>> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, Pair<String, Boolean>>()
        try {
            val fxJson = JSONObject(httpGet("https://er-api.com"))
            val rates = fxJson.getJSONObject("rates")
            val ils = rates.getDouble("ILS")
            val jod = rates.getDouble("JOD")
            val eur = rates.getDouble("EUR")
            result["USD/ILS"] = Pair(String.format("%.2f", ils), true)
            result["JOD/ILS"] = Pair(String.format("%.2f", ils / jod), true)
            result["EUR/ILS"] = Pair(String.format("%.2f", ils / eur), false)
        } catch (_: Exception) {}

        try {
            val cryptoUrl = "https://coingecko.com"
            val cJson = JSONObject(httpGet(cryptoUrl))
            val btc = cJson.getJSONObject("bitcoin").getInt("usd")
            val eth = cJson.getJSONObject("ethereum").getInt("usd")
            result["BTC"] = Pair("$btc$", true)
            result["ETH"] = Pair("$eth$", true)
        } catch (_: Exception) {}
        result
    }

    suspend fun checkGitHubUpdate(currentVersion: String): GitHubUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val u = "https://github.com" + "$OWNER/$REPO/releases/latest"
            val json = JSONObject(httpGet(u))
            val tag = json.optString("tag_name", "").removePrefix("v")
            val notes = json.optString("body", "تحديث جديد متاح")
            val assets = json.optJSONArray("assets")
            var apkUrl = ""
            if (assets != null && assets.length() > 0) {
                apkUrl = assets.getJSONObject(0).optString("browser_download_url", "")
            }
            if (tag.isNotEmpty() && tag != currentVersion && apkUrl.isNotEmpty()) {
                                GitHubUpdateInfo(true, tag, apkUrl, notes)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}


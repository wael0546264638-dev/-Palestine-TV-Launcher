package com.palestine.tvlauncher.network

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class MatchInfo(val name: String, val status: String)

data class LiveFeedState(
    val currentTemp: Int = 26,
    val dailyMax: List<Int> = listOf(27, 28, 26, 25, 27),
    val dailyMin: List<Int> = listOf(15, 16, 14, 14, 15),
    val news: List<String> = listOf(
        "أخبار فلسطين: متابعة مستمرة للأحوال الجوية والأسعار المحلية",
        "أريحا والأغوار: أجواء مستقرة ومعتدلة خلال ساعات النهار"
    ),
    val matches: List<MatchInfo> = listOf(
        MatchInfo("الدوري الإسباني: ريال مدريد × برشلونة", "22:00"),
        MatchInfo("الدوري الإنجليزي: ليفربول × أرسنال", "19:30"),
        MatchInfo("دوري أبطال أوروبا: بايرن ميونخ × إنتر", "22:00")
    ),
    val prices: Map<String, Pair<String, Boolean>> = mapOf(
        "USD/ILS" to Pair("3.35", true),
        "JOD/ILS" to Pair("4.72", true),
        "EUR/ILS" to Pair("3.65", false),
        "GOLD" to Pair("$2,740", true),
        "BRENT" to Pair("$74.50", false),
        "BTC" to Pair("$91,500", true),
        "ETH" to Pair("$3,120", true)
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
        "أزرق ليلي فخم" to "",
        "جبال الضباب" to "https://unsplash.com",
        "غابات الصنوبر" to "https://unsplash.com",
        "غروب هادئ" to "https://unsplash.com",
        "بحيرة الطبيعة" to "https://unsplash.com"
    )

    suspend fun fetchGitHubFeed(): LiveFeedState =
        withContext(Dispatchers.IO) {
            var state = LiveFeedState()
            try {
                val base = "https://githubusercontent.com"
                val path = "/$OWNER/$REPO/main/live_data.json"
                val conn = URL(base + path)
                    .openConnection() as HttpURLConnection
                conn.connectTimeout = 6000
                if (conn.responseCode == 200) {
                    val txt = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(txt)
                    val w = json.getJSONObject("weather")
                    val maxA = w.getJSONArray("daily_max")
                    val minA = w.getJSONArray("daily_min")
                    val newsA = json.getJSONArray("news")
                    val matchA = json.getJSONArray("matches")

                    val fetchedNews = List(newsA.length()) { newsA.getString(it) }
                    val fetchedMatches = List(matchA.length()) {
                        val m = matchA.getJSONObject(it)
                        MatchInfo(m.optString("name"), m.optString("status"))
                    }

                    state = state.copy(
                        currentTemp = w.optInt("current_temp", 26),
                        dailyMax = List(maxA.length()) { maxA.getInt(it) },
                        dailyMin = List(minA.length()) { minA.getInt(it) },
                        news = if (fetchedNews.isNotEmpty()) fetchedNews else state.news,
                        matches = if (fetchedMatches.isNotEmpty()) fetchedMatches else state.matches
                    )
                }
            } catch (_: Exception) { }

            try {
                val rssUrl = "https://aljazeera.net"
                val rc = URL(rssUrl).openConnection() as HttpURLConnection
                rc.connectTimeout = 5000
                if (rc.responseCode == 200) {
                    val xml = rc.inputStream.bufferedReader().readText()
                    val titles = Regex("<title><!\\[CDATA\\[(.*?)]]></title>|<title>(.*?)</title>")
                        .findAll(xml)
                        .map { it.groupValues[1].ifBlank { it.groupValues[2] } }
                        .filter { it.length > 15 && !it.contains("الجزيرة") }
                        .take(8)
                        .toList()
                    if (titles.isNotEmpty()) {
                        state = state.copy(news = titles)
                    }
                }
            } catch (_: Exception) { }
            state
        }

    suspend fun fetchCityWeather(
        lat: Double,
        lon: Double
    ): Triple<Int, List<Int>, List<Int>>? =
        withContext(Dispatchers.IO) {
            try {
                val u1 = "https://open-meteo.com"
                val u2 = "?latitude=$lat&longitude=$lon"
                val u3 = "&current=temperature_2m"
                val u4 = "&daily=temperature_2m_max,temperature_2m_min"
                val u5 = "&timezone=Asia%2FHebron"
                val conn = URL(u1 + u2 + u3 + u4 + u5)
                    .openConnection() as HttpURLConnection
                conn.connectTimeout = 6000
                if (conn.responseCode == 200) {
                    val txt = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(txt)
                    val cur = Math.round(
                        json.getJSONObject("current")
                            .getDouble("temperature_2m")
                    ).toInt()
                    val d = json.getJSONObject("daily")
                    val mx = d.getJSONArray("temperature_2m_max")
                    val mn = d.getJSONArray("temperature_2m_min")
                    Triple(
                        cur,
                        List(minOf(5, mx.length())) {
                            Math.round(mx.getDouble(it)).toInt()
                        },
                        List(minOf(5, mn.length())) {
                            Math.round(mn.getDouble(it)).toInt()
                        }
                    )
                } else null
            } catch (_: Exception) {
                null
            }
        }

    suspend fun fetchInstantPrices(): Map<String, Pair<String, Boolean>> =
        withContext(Dispatchers.IO) {
            val map = mutableMapOf<String, Pair<String, Boolean>>()
            try {
                val u = "https://er-api.com"
                val c = URL(u).openConnection() as HttpURLConnection
                c.connectTimeout = 5000
                if (c.responseCode == 200) {
                    val txt = c.inputStream.bufferedReader().readText()
                    val rates = JSONObject(txt).getJSONObject("rates")
                    val ils = rates.optDouble("ILS", 3.35)
                    val jod = rates.optDouble("JOD", 0.709)
                    val eur = rates.optDouble("EUR", 0.92)
                    map["USD/ILS"] = Pair(String.format("%.2f", ils), true)
                    map["JOD/ILS"] = Pair(String.format("%.2f", ils / jod), true)
                    map["EUR/ILS"] = Pair(String.format("%.2f", ils / eur), true)
                }
            } catch (_: Exception) { }

            try {
                val bUrl = "https://binance.com"
                val bc = URL(bUrl).openConnection() as HttpURLConnection
                bc.connectTimeout = 4000
                if (bc.responseCode == 200) {
                    val bj = JSONObject(bc.inputStream.bufferedReader().readText())
                    val p = bj.getDouble("lastPrice").toInt()
                    val ch = bj.getDouble("priceChangePercent")
                    map["BTC"] = Pair("$${String.format("%,d", p)}", ch >= 0)
                }
            } catch (_: Exception) { }
            map
        }

    suspend fun checkGitHubUpdate(
        currentVer: String
    ): GitHubUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val u = "https://github.com"
            val c = URL(u).openConnection() as HttpURLConnection
            c.connectTimeout = 6000
            if (c.responseCode == 200) {
                val j = JSONObject(c.inputStream.bufferedReader().readText())
                val tag = j.optString("tag_name", "").removePrefix("v")
                val body = j.optString("body", "تحديث جديد متاح")
                val assets = j.optJSONArray("assets")
                var apk = ""
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        if (a.optString("name").endsWith(".apk")) {
                            apk = a.optString("browser_download_url")
                            break
                        }
                    }
                }
                if (tag.isNotBlank() && tag != currentVer && apk.isNotBlank()) {
                    return@withContext GitHubUpdateInfo(true, tag, body, apk)
                }
            }
        } catch (_: Exception) { }
        null
    }

    fun downloadAndInstallApk(context: Context, apkUrl: String) {
        val req = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("تحديث لانشر فلسطين TV")
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "update.apk"
            )
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(req)}
}

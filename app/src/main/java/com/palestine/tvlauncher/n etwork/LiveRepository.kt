
package com.palestine.tvlauncher.network

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class MatchInfo(val name: String, val status: String)

data class LiveFeedState(
    val currentTemp: Int = 26,
    val dailyMax: List<Int> = listOf(27, 28, 26, 25, 27),
    val dailyMin: List<Int> = listOf(15, 16, 14, 14, 15),
    val news: List<String> = listOf("جاري جلب آخر الأخبار العاجلة..."),
    val matches: List<MatchInfo> = listOf(MatchInfo("جاري تحميل المباريات...", "")),
    val prices: Map<String, Pair<String, Boolean>> = mapOf(
        "USD/ILS" to Pair("3.35", true),
        "JOD/ILS" to Pair("4.72", true),
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

    private const val REPO_OWNER = "wael0546264638-dev"
    private const val REPO_NAME = "Palestine-TV-Launcher"

    suspend fun fetchGitHubFeed(): LiveFeedState = withContext(Dispatchers.IO) {
        var current = LiveFeedState()
        try {
            val url = URL("https://githubusercontent.com")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val weatherObj = json.getJSONObject("weather")
                val maxArr = weatherObj.getJSONArray("daily_max")
                val minArr = weatherObj.getJSONArray("daily_min")
                val newsArr = json.getJSONArray("news")
                val matchesArr = json.getJSONArray("matches")

                current = current.copy(
                    currentTemp = weatherObj.optInt("current_temp", 26),
                    dailyMax = List(maxArr.length()) { maxArr.getInt(it) },
                    dailyMin = List(minArr.length()) { minArr.getInt(it) },
                    news = List(newsArr.length()) { newsArr.getString(it) },
                    matches = List(matchesArr.length()) {
                        val m = matchesArr.getJSONObject(it)
                        MatchInfo(m.optString("name"), m.optString("status"))
                    }
                )
            }
        } catch (_: Exception) { }
        current
    }

    suspend fun fetchCityWeather(lat: Double, lon: Double): Triple<Int, List<Int>, List<Int>>? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(
                    "https://open-meteo.com" +
                            "&current=temperature_2m&daily=temperature_2m_max,temperature_2m_min&timezone=Asia%2FHebron"
                )
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 6000
                }
                if (conn.responseCode == 200) {
                    val json = JSONObject(conn.inputStream.bufferedReader().readText())
                    val curr = Math.round(json.getJSONObject("current").getDouble("temperature_2m")).toInt()
                    val daily = json.getJSONObject("daily")
                    val maxA = daily.getJSONArray("temperature_2m_max")
                    val minA = daily.getJSONArray("temperature_2m_min")
                    Triple(
                        curr,
                        List(minOf(5, maxA.length())) { Math.round(maxA.getDouble(it)).toInt() },
                        List(minOf(5, minA.length())) { Math.round(minA.getDouble(it)).toInt() }
                    )
                } else null
            } catch (_: Exception) {
                null
            }
        }

    suspend fun fetchInstantPrices(): Map<String, Pair<String, Boolean>> = withContext(Dispatchers.IO) {
        val updated = mutableMapOf<String, Pair<String, Boolean>>()
        try {
            // Live Fiat Exchange Rates (USD & JOD to ILS)
            val fxUrl = URL("https://er-api.com")
            val fxConn = (fxUrl.openConnection() as HttpURLConnection).apply { connectTimeout = 5000 }
            if (fxConn.responseCode == 200) {
                val fxJson = JSONObject(fxConn.inputStream.bufferedReader().readText())
                val rates = fxJson.getJSONObject("rates")
                val ils = rates.optDouble("ILS", 3.35)
                val jod = rates.optDouble("JOD", 0.709)
                val eur = rates.optDouble("EUR", 0.92)
                updated["USD/ILS"] = Pair(String.format("%.2f", ils), true)
                updated["JOD/ILS"] = Pair(String.format("%.2f", ils / jod), true)
                updated["EUR/ILS"] = Pair(String.format("%.2f", ils / eur), true)
            }
        } catch (_: Exception) { }

        try {
            // Live Crypto via Binance Public Ticker
            val btcUrl = URL("https://binance.com")
            val btcConn = (btcUrl.openConnection() as HttpURLConnection).apply { connectTimeout = 4000 }
            if (btcConn.responseCode == 200) {
                val btcJson = JSONObject(btcConn.inputStream.bufferedReader().readText())
                val price = btcJson.getDouble("lastPrice").toInt()
                val change = btcJson.getDouble("priceChangePercent")
                updated["BTC"] = Pair("$${String.format("%,d", price)}", change >= 0)
            }
            val ethUrl = URL("https://binance.com")
            val ethConn = (ethUrl.openConnection() as HttpURLConnection).apply { connectTimeout = 4000 }
            if (ethConn.responseCode == 200) {
                val ethJson = JSONObject(ethConn.inputStream.bufferedReader().readText())
                val price = ethJson.getDouble("lastPrice").toInt()
                val change = ethJson.getDouble("priceChangePercent")
                updated["ETH"] = Pair("$${String.format("%,d", price)}", change >= 0)
            }
        } catch (_: Exception) { }

        updated
    }

    suspend fun checkGitHubUpdate(currentVersionName: String): GitHubUpdateInfo? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://github.com")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                }
                if (conn.responseCode == 200) {
                    val json = JSONObject(conn.inputStream.bufferedReader().readText())
                    val tag = json.optString("tag_name", "").removePrefix("v")
                    val body = json.optString("body", "تحديث جديد لتحسين الأداء وإضافة مميزات جديدة.")
                    val assets = json.optJSONArray("assets")
                    var apkUrl = ""
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            if (asset.optString("name").endsWith(".apk")) {
                                apkUrl = asset.optString("browser_download_url")
                                break
                            }
                        }
                    }
                    if (tag.isNotBlank() && tag != currentVersionName && apkUrl.isNotBlank()) {
                        return@withContext GitHubUpdateInfo(true, tag, body, apkUrl)
                    }
                }
            } catch (_: Exception) { }
            null
        }

    fun downloadAndInstallApk(context: Context, apkUrl: String) {
        val fileName = "PalestineLauncherUpdate.apk"
        val destFile = File(context.externalCacheDir, fileName)
        if (destFile.exists()) destFile.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("تحديث لانشر فلسطين TV")
            .setDescription("جاري تحميل التحديث الجديد من GitHub...")
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    context.unregisterReceiver(this)
                    val downloadedFile = File(
                        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                        fileName
                    )
                    if (downloadedFile.exists()) {
                        val apkUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            downloadedFile
                        )
                        val installIntent = Intent(Intent.ACTION_VIEW).apply {

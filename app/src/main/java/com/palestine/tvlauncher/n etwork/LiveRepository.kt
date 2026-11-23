package com.palestine.tvlauncher.network

import com.palestine.tvlauncher.data.PalestineData
import com.palestine.tvlauncher.data.PalestinianCity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

data class MatchItem(val name: String, val status: String)
data class GitHubUpdateInfo(val hasUpdate: Boolean, val latestVersion: String, val apkDownloadUrl: String, val releaseNotes: String)

data class LiveFeedState(
    val currentTemp: String = "26",
    val dailyMax: String = "28",
    val dailyMin: String = "17",
    val forecastDays: List<Pair<String, String>> = listOf("27" to "16", "28" to "17", "26" to "15"),
    val news: List<String> = listOf("أهلاً بكم في لانشر فلسطين TV المحدّث"),
    val matches: List<MatchItem> = listOf(MatchItem("دوري أبطال أوروبا - الليلة", "٢٢:٠٠")),
    val prices: Map<String, Pair<String, Boolean>> = mapOf(
        "USD/ILS" to ("3.65" to true),
        "JOD/ILS" to ("5.15" to true),
        "EUR/ILS" to ("3.95" to false),
        "GOLD" to ("2640$" to true)
    )
)

data class SurahHeader(val number: Int, val name: String, val ayahCount: Int, val revelationType: String)
data class FullSurahContent(val number: Int, val name: String, val versesText: String, val pageRange: String)

object LiveRepository {

    val natureWallpapers = listOf(
        "غروب هادئ" to "https://unsplash.com",
        "جبال الضباب" to "https://unsplash.com",
        "غابة خضراء" to "https://unsplash.com",
        "شاطئ ليلي" to "https://unsplash.com",
        "صحراء ذهبية" to "https://unsplash.com",
        "بدون خلفية (داكن)" to ""
    )

    val verifiedQuranRadios = listOf(
        "إذاعة القرآن الكريم - القاهرة" to "https://radiojar.com",
        "تراتيل قصيرة خاشعة (mp3quran)" to "https://qurango.net",
        "الشيخ ماهر المعيقلي (mp3quran)" to "https://qurango.net",
        "الشيخ عبدالباسط عبدالصمد" to "https://qurango.net",
        "الشيخ مشاري العفاسي" to "https://qurango.net",
        "الشيخ ياسر الدوسري" to "https://qurango.net"
    )

    private var cachedSurahList: List<SurahHeader>? = null
    private val cachedSurahTexts = mutableMapOf<Int, FullSurahContent>()

    private fun httpGet(urlStr: String): String? {
        return try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 9000
                readTimeout = 9000
                setRequestProperty("User-Agent", "PalestineTVLauncher/1.2")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchAll114Surahs(): List<SurahHeader> = withContext(Dispatchers.IO) {
        cachedSurahList?.let { return@withContext it }
        try {
            val raw = httpGet("https://alquran.cloud") ?: return@withContext emptyList()
            val arr = JSONObject(raw).getJSONArray("data")
            val list = mutableListOf<SurahHeader>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val type = if (obj.optString("revelationType") == "Meccan") "مكية" else "مدنية"
                list.add(
                    SurahHeader(
                        number = obj.getInt("number"),
                        name = obj.getString("name"),
                        ayahCount = obj.getInt("numberOfAyahs"),
                        revelationType = type
                    )
                )
            }
            cachedSurahList = list
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchFullSurahText(surahNumber: Int): FullSurahContent? = withContext(Dispatchers.IO) {
        cachedSurahTexts[surahNumber]?.let { return@withContext it }
        try {
            val raw = httpGet("https://alquran.cloud/$surahNumber/quran-uthmani") ?: return@withContext null
            val data = JSONObject(raw).getJSONObject("data")
            val name = data.getString("name")
            val ayahs = data.getJSONArray("ayahs")
            var firstPage = 0
            var lastPage = 0
            val sb = StringBuilder()
            if (surahNumber != 1 && surahNumber != 9) {
                sb.append("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\n\n")
            }
            for (i in 0 until ayahs.length()) {
                val a = ayahs.getJSONObject(i)
                val num = a.getInt("numberInSurah")
                val page = a.optInt("page", 1)
                if (i == 0) firstPage = page
                lastPage = page
                var txt = a.getString("text")
                if (i == 0 && surahNumber != 1 && txt.startsWith("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ")) {
                    txt = txt.removePrefix("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ").trim()
                }
                sb.append("$txt ﴿${PalestineData.toArabicDigits(num.toString())}﴾ ")
            }
            val pageStr = if (firstPage == lastPage) "صفحة ${PalestineData.toArabicDigits(firstPage.toString())}"
            else "صفحة ${PalestineData.toArabicDigits("$firstPage - $lastPage")}"
            val result = FullSurahContent(surahNumber, name, sb.toString().trim(), pageStr)
            cachedSurahTexts[surahNumber] = result
            result
        } catch (_: Exception) {
            null
        }
    }
    suspend fun autoDetectLocation(): PalestinianCity? = withContext(Dispatchers.IO) {
        try {
            val raw = httpGet("https://ipapi.co") ?: return@withContext null
            val json = JSONObject(raw)
            val city = json.optString("city").ifBlank { "موقعي الحالي" }
            val country = json.optString("country_name")
            val lat = json.optDouble("latitude", 32.0450)
            val lon = json.optDouble("longitude", 35.4580)
            val tz = json.optString("timezone").ifBlank { "Asia/Hebron" }
            PalestinianCity(city, country, lat, lon, tz)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun searchLocationByName(query: String): List<PalestinianCity> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val raw = httpGet("https://open-meteo.com")
                ?: return@withContext emptyList()
            val results = JSONObject(raw).optJSONArray("results") ?: return@withContext emptyList()
            val list = mutableListOf<PalestinianCity>()
            for (i in 0 until results.length()) {
                val obj = results.getJSONObject(i)
                list.add(
                    PalestinianCity(
                        name = obj.optString("name"),
                        country = obj.optString("country"),
                        lat = obj.optDouble("latitude"),
                        lon = obj.optDouble("longitude"),
                        timeZoneId = obj.optString("timezone").ifBlank { "Asia/Hebron" }
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

        suspend fun fetchExactPrayers(lat: Double, lon: Double, timeZoneId: String): PalestineData.PrayerDisplay = withContext(Dispatchers.IO) {
        try {
            val tz = TimeZone.getTimeZone(timeZoneId)
            val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).apply { timeZone = tz }.format(Date())
            val raw = httpGet("https://aladhan.com")
                ?: return@withContext PalestineData.PrayerDisplay()
            val timings = JSONObject(raw).getJSONObject("data").getJSONObject("timings")
            val fajr = timings.getString("Fajr").take(5)
            val sunrise = timings.getString("Sunrise").take(5)
            val dhuhr = timings.getString("Dhuhr").take(5)
            val asr = timings.getString("Asr").take(5)
            val maghrib = timings.getString("Maghrib").take(5)
            val isha = timings.getString("Isha").take(5)

            val nowCal = Calendar.getInstance(tz)
            val nowMins = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
            val list = listOf(
                "الفجر" to fajr,
                "الظهر" to dhuhr,
                "العصر" to asr,
                "المغرب" to maghrib,
                "العشاء" to isha
            )
            var nextName = "الفجر"
            var diffMins = -1
            for ((name, tStr) in list) {
                val p = tStr.split(":")
                if (p.size == 2) {
                    val m = (p[0].toIntOrNull() ?: 0) * 60 + (p[1].toIntOrNull() ?: 0)
                    if (m > nowMins) {
                        nextName = name
                        diffMins = m - nowMins
                        break
                    }
                }
            }
            if (diffMins < 0) {
                val p = fajr.split(":")
                val m = (p.getOrNull(0)?.toIntOrNull() ?: 5) * 60 + (p.getOrNull(1)?.toIntOrNull() ?: 0)
                diffMins = (24 * 60 - nowMins) + m
            }
            val remH = diffMins / 60
            val remM = diffMins % 60
            val remText = PalestineData.toArabicDigits("متبقي لصلاة $nextName: $remH س و $remM د")

            PalestineData.PrayerDisplay(
                fajr = PalestineData.toArabicDigits(fajr),
                sunrise = PalestineData.toArabicDigits(sunrise),
                dhuhr = PalestineData.toArabicDigits(dhuhr),
                asr = PalestineData.toArabicDigits(asr),
                maghrib = PalestineData.toArabicDigits(maghrib),
                isha = PalestineData.toArabicDigits(isha),
                nextPrayerName = nextName,
                remainingText = remText
            )
        } catch (_: Exception) {
            PalestineData.PrayerDisplay()
        }
    }

    suspend fun fetchCityWeather(lat: Double, lon: Double, timeZoneId: String): Triple<String, String, String>? = withContext(Dispatchers.IO) {
        try {
            val encodedTz = URLEncoder.encode(timeZoneId, "UTF-8")
            val raw = httpGet("https://open-meteo.com")
                ?: return@withContext null
            val json = JSONObject(raw)
            val current = json.getJSONObject("current_weather").getDouble("temperature").toInt().toString()
            val daily = json.getJSONObject("daily")
            val max = daily.getJSONArray("temperature_2m_max").getDouble(0).toInt().toString()
            val min = daily.getJSONArray("temperature_2m_min").getDouble(0).toInt().toString()
            Triple(current, max, min)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchGitHubFeed(): LiveFeedState = withContext(Dispatchers.IO) {
        try {
            val raw = httpGet("https://githubusercontent.com")
                ?: return@withContext LiveFeedState()
            val json = JSONObject(raw)
            val newsArr = json.optJSONArray("news")
            val newsList = mutableListOf<String>()
            if (newsArr != null) {
                for (i in 0 until newsArr.length()) newsList.add(newsArr.getString(i))
            }
            val matchesArr = json.optJSONArray("matches")
            val matchesList = mutableListOf<MatchItem>()
            if (matchesArr != null) {
                for (i in 0 until matchesArr.length()) {
                    val m = matchesArr.getJSONObject(i)
                    matchesList.add(MatchItem(m.optString("name"), m.optString("status")))
                }
            }
            LiveFeedState(
                news = newsList.ifEmpty { LiveFeedState().news },
                matches = matchesList.ifEmpty { LiveFeedState().matches }
            )
        } catch (_: Exception) {
            LiveFeedState()
        }
    }

    suspend fun fetchInstantPrices(): Map<String, Pair<String, Boolean>> = withContext(Dispatchers.IO) {
        try {
            val raw = httpGet("https://er-api.com") ?: return@withContext emptyMap()
            val rates = JSONObject(raw).getJSONObject("rates")
            val ils = rates.optDouble("ILS", 3.65)
            val jod = rates.optDouble("JOD", 0.709)
            val eur = rates.optDouble("EUR", 0.92)
            mapOf(
                "USD/ILS" to (String.format(Locale.US, "%.2f", ils) to true),
                "JOD/ILS" to (String.format(Locale.US, "%.2f", ils / jod) to true),
                "EUR/ILS" to (String.format(Locale.US, "%.2f", ils / eur) to false),
                "GOLD" to ("2640$" to true)
            )
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun checkGitHubUpdate(currentVersion: String): GitHubUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val raw = httpGet("https://github.com")
                ?: return@withContext null
            val json = JSONObject(raw)
            val tag = json.optString("tag_name").removePrefix("v")
            val notes = json.optString("body").ifBlank { "تحديث جديد متاح للتطبيق" }
            val assets = json.optJSONArray("assets")
            var apkUrl = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    val url = a.optString("browser_download_url")
                    if (url.endsWith(".apk")) {
                        apkUrl = url
                        break
                    }
                }
            }
            if (tag.isNotEmpty() && tag != currentVersion && apkUrl.isNotEmpty()) {
                GitHubUpdateInfo(true, tag, apkUrl, notes)
            } else null
        } catch (_: Exception) {
            null
        }
    }
}

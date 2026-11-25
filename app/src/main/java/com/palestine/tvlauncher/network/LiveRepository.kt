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
data class Verse(val number: Int, val text: String, val audioUrl: String?)

val staticSurahList: List<SurahHeader> = listOf(
    SurahHeader(1, "الفاتحة", 7, "مكية"),
    SurahHeader(2, "البقرة", 286, "مدنية"),
    SurahHeader(3, "آل عمران", 200, "مدنية"),
    SurahHeader(4, "النساء", 176, "مدنية"),
    SurahHeader(5, "المائدة", 120, "مدنية"),
    SurahHeader(6, "الأنعام", 165, "مكية"),
    SurahHeader(7, "الأعراف", 206, "مكية"),
    SurahHeader(8, "الأنفال", 75, "مدنية"),
    SurahHeader(9, "التوبة", 129, "مدنية"),
    SurahHeader(10, "يونس", 109, "مكية"),
    SurahHeader(11, "هود", 123, "مكية"),
    SurahHeader(12, "يوسف", 111, "مكية"),
    SurahHeader(13, "الرعد", 43, "مدنية"),
    SurahHeader(14, "إبراهيم", 52, "مكية"),
    SurahHeader(15, "الحجر", 99, "مكية"),
    SurahHeader(16, "النحل", 128, "مكية"),
    SurahHeader(17, "الإسراء", 111, "مكية"),
    SurahHeader(18, "الكهف", 110, "مكية"),
    SurahHeader(19, "مريم", 98, "مكية"),
    SurahHeader(20, "طه", 135, "مكية"),
    SurahHeader(21, "الأنبياء", 112, "مكية"),
    SurahHeader(22, "الحج", 78, "مدنية"),
    SurahHeader(23, "المؤمنون", 118, "مكية"),
    SurahHeader(24, "النور", 64, "مدنية"),
    SurahHeader(25, "الفرقان", 77, "مكية"),
    SurahHeader(26, "الشعراء", 227, "مكية"),
    SurahHeader(27, "النمل", 93, "مكية"),
    SurahHeader(28, "القصص", 88, "مكية"),
    SurahHeader(29, "العنكبوت", 69, "مكية"),
    SurahHeader(30, "الروم", 60, "مكية"),
    SurahHeader(31, "لقمان", 34, "مكية"),
    SurahHeader(32, "السجدة", 30, "مكية"),
    SurahHeader(33, "الأحزاب", 73, "مدنية"),
    SurahHeader(34, "سبأ", 54, "مكية"),
    SurahHeader(35, "فاطر", 45, "مكية"),
    SurahHeader(36, "يس", 83, "مكية"),
    SurahHeader(37, "الصافات", 182, "مكية"),
    SurahHeader(38, "ص", 88, "مكية"),
    SurahHeader(39, "الزمر", 75, "مكية"),
    SurahHeader(40, "غافر", 85, "مكية"),
    SurahHeader(41, "فصلت", 54, "مكية"),
    SurahHeader(42, "الشورى", 53, "مكية"),
    SurahHeader(43, "الزخرف", 89, "مكية"),
    SurahHeader(44, "الدخان", 59, "مكية"),
    SurahHeader(45, "الجاثية", 37, "مكية"),
    SurahHeader(46, "الأحقاف", 35, "مكية"),
    SurahHeader(47, "محمد", 38, "مدنية"),
    SurahHeader(48, "الفتح", 29, "مدنية"),
    SurahHeader(49, "الحجرات", 18, "مدنية"),
    SurahHeader(50, "ق", 45, "مكية"),
    SurahHeader(51, "الذاريات", 60, "مكية"),
    SurahHeader(52, "الطور", 49, "مكية"),
    SurahHeader(53, "النجم", 62, "مكية"),
    SurahHeader(54, "القمر", 55, "مكية"),
    SurahHeader(55, "الرحمن", 78, "مكية"),
    SurahHeader(56, "الواقعة", 96, "مكية"),
    SurahHeader(57, "الحديد", 29, "مدنية"),
    SurahHeader(58, "المجادلة", 22, "مدنية"),
    SurahHeader(59, "الحشر", 24, "مدنية"),
    SurahHeader(60, "الممتحنة", 13, "مدنية"),
    SurahHeader(61, "الصف", 14, "مدنية"),
    SurahHeader(62, "الجمعة", 11, "مدنية"),
    SurahHeader(63, "المنافقون", 11, "مدنية"),
    SurahHeader(64, "التغابن", 18, "مدنية"),
    SurahHeader(65, "الطلاق", 12, "مدنية"),
    SurahHeader(66, "التحريم", 12, "مدنية"),
    SurahHeader(67, "الملك", 30, "مكية"),
    SurahHeader(68, "القلم", 52, "مكية"),
    SurahHeader(69, "الحاقة", 52, "مكية"),
    SurahHeader(70, "المعارج", 44, "مكية"),
    SurahHeader(71, "نوح", 28, "مكية"),
    SurahHeader(72, "الجن", 28, "مكية"),
    SurahHeader(73, "المزمل", 20, "مكية"),
    SurahHeader(74, "المدثر", 56, "مكية"),
    SurahHeader(75, "القيامة", 40, "مكية"),
    SurahHeader(76, "الإنسان", 31, "مدنية"),
    SurahHeader(77, "المرسلات", 50, "مكية"),
    SurahHeader(78, "النبأ", 40, "مكية"),
    SurahHeader(79, "النازعات", 46, "مكية"),
    SurahHeader(80, "عبس", 42, "مكية"),
    SurahHeader(81, "التكوير", 29, "مكية"),
    SurahHeader(82, "الانفطار", 19, "مكية"),
    SurahHeader(83, "المطففين", 36, "مكية"),
    SurahHeader(84, "الانشقاق", 25, "مكية"),
    SurahHeader(85, "البروج", 22, "مكية"),
    SurahHeader(86, "الطارق", 17, "مكية"),
    SurahHeader(87, "الأعلى", 19, "مكية"),
    SurahHeader(88, "الغاشية", 26, "مكية"),
    SurahHeader(89, "الفجر", 30, "مكية"),
    SurahHeader(90, "البلد", 20, "مكية"),
    SurahHeader(91, "الشمس", 15, "مكية"),
    SurahHeader(92, "الليل", 21, "مكية"),
    SurahHeader(93, "الضحى", 11, "مكية"),
    SurahHeader(94, "الشرح", 8, "مكية"),
    SurahHeader(95, "التين", 8, "مكية"),
    SurahHeader(96, "العلق", 19, "مكية"),
    SurahHeader(97, "القدر", 5, "مكية"),
    SurahHeader(98, "البينة", 8, "مدنية"),
    SurahHeader(99, "الزلزلة", 8, "مكية"),
    SurahHeader(100, "العاديات", 11, "مكية"),
    SurahHeader(101, "القارعة", 11, "مكية"),
    SurahHeader(102, "التكاثر", 8, "مكية"),
    SurahHeader(103, "العصر", 3, "مكية"),
    SurahHeader(104, "الهمزة", 9, "مكية"),
    SurahHeader(105, "الفيل", 5, "مكية"),
    SurahHeader(106, "قريش", 4, "مكية"),
    SurahHeader(107, "الماعون", 7, "مكية"),
    SurahHeader(108, "الكوثر", 3, "مكية"),
    SurahHeader(109, "الكافرون", 6, "مكية"),
    SurahHeader(110, "النصر", 3, "مدنية"),
    SurahHeader(111, "المسد", 5, "مكية"),
    SurahHeader(112, "الإخلاص", 4, "مكية"),
    SurahHeader(113, "الفلق", 5, "مكية"),
    SurahHeader(114, "الناس", 6, "مكية")
)

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
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("User-Agent", "PalestineTVLauncher/1.2 (Android TV; Quran Reader)")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                System.err.println("HTTP ${conn.responseCode} for $urlStr: ${conn.responseMessage}")
                null
            }
        } catch (e: Exception) {
            System.err.println("Network error for $urlStr: ${e.message}")
            null
        }
    }

    suspend fun fetchAll114Surahs(): List<SurahHeader> = withContext(Dispatchers.IO) {
        cachedSurahList?.let { return@withContext it }
        cachedSurahList = staticSurahList
        staticSurahList
    }

    suspend fun fetchFullSurahText(surahNumber: Int): FullSurahContent? = withContext(Dispatchers.IO) {
        cachedSurahTexts[surahNumber]?.let { return@withContext it }
        try {
            val raw = httpGet("https://api.alquran.cloud/v1/surah/$surahNumber/quran-uthmani") ?: return@withContext null
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

    suspend fun fetchSurahWithAudio(surahNumber: Int): List<Verse>? = withContext(Dispatchers.IO) {
        // Try primary source: alquran.cloud
        try {
            val raw = httpGet("https://api.alquran.cloud/v1/surah/$surahNumber/ar.alafasy")
            if (raw != null) {
                val data = JSONObject(raw).getJSONObject("data")
                val ayahs = data.getJSONArray("ayahs")
                val verses = mutableListOf<Verse>()
                for (i in 0 until ayahs.length()) {
                    val a = ayahs.getJSONObject(i)
                    val num = a.getInt("numberInSurah")
                    var txt = a.getString("text")
                    if (i == 0 && surahNumber != 1 && txt.startsWith("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ")) {
                        txt = txt.removePrefix("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ").trim()
                    }
                    val audioUrl = a.optString("audio", "").takeIf { it.isNotBlank() }
                    verses.add(Verse(num, txt, audioUrl))
                }
                return@withContext verses
            }
        } catch (e: Exception) {
            System.err.println("alquran.cloud fetch failed for surah $surahNumber: ${e.message}")
        }

        // Fallback: quran.com API
        try {
            val raw = httpGet("https://api.quran.com/api/v4/chapters/$surahNumber/verses/ar.alafasy")
            if (raw != null) {
                val data = JSONObject(raw).getJSONObject("data")
                val ayahs = data.getJSONArray("verses")
                val verses = mutableListOf<Verse>()
                for (i in 0 until ayahs.length()) {
                    val a = ayahs.getJSONObject(i)
                    val num = a.getInt("verse_number")
                    var txt = a.getString("text_uthmani")
                    if (i == 0 && surahNumber != 1 && txt.startsWith("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ")) {
                        txt = txt.removePrefix("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ").trim()
                    }
                    val audioUrl = a.optString("audio_url", "").takeIf { it.isNotBlank() }
                    verses.add(Verse(num, txt, audioUrl))
                }
                return@withContext verses
            }
        } catch (e: Exception) {
            System.err.println("quran.com fallback failed for surah $surahNumber: ${e.message}")
        }

        System.err.println("Both sources failed for surah $surahNumber")
        null
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

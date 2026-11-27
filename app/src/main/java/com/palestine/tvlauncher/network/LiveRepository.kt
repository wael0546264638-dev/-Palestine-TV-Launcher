package com.palestine.tvlauncher.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.palestine.tvlauncher.data.PalestineData
import com.palestine.tvlauncher.data.PalestinianCity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

private val Context.liveDataStore: DataStore<Preferences> by preferencesDataStore(name = "live_cache")

data class MatchItem(val name: String, val status: String)
data class NewsSource(val id: String, val name: String, val url: String, val country: String)
data class GitHubUpdateInfo(val hasUpdate: Boolean, val latestVersion: String, val apkDownloadUrl: String, val releaseNotes: String)

data class QuranRadio(val name: String, val url: String)

data class HadithItem(val id: Int, val text: String, val explanation: String, val benefits: String, val source: String = "")

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

data class WeatherHour(val time: String, val temp: String, val precipProb: Int)
data class WeatherDay(val date: String, val max: String, val min: String, val precipProb: Int)
data class WeatherDetail(
    val currentTemp: String,
    val hourly: List<WeatherHour>,
    val daily: List<WeatherDay>,
    val precipitationProbability: Int
)

data class SurahHeader(val number: Int, val name: String, val ayahCount: Int, val revelationType: String)
data class FullSurahContent(val number: Int, val name: String, val versesText: String, val pageRange: String)
data class Verse(val number: Int, val text: String, val audioUrl: String?, val tafsir: String = "")

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

private val sslContext: SSLContext = SSLContext.getInstance("TLS").apply {
    init(null, arrayOf(TrustAllManager), SecureRandom())
}

private object TrustAllManager : X509TrustManager {
    override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
    override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}

object LiveRepository {

    val countryNewsSources: Map<String, List<NewsSource>> = mapOf(
        "Palestine" to listOf(
            NewsSource("pal_wafa", "وفا", "https://www.wafa.ps/rss", "Palestine"),
            NewsSource("pal_maann", "معاً", "https://www.maannews.net/rss", "Palestine"),
            NewsSource("pal_alquds", "القدس", "https://www.alquds.com/rss", "Palestine")
        ),
        "Jordan" to listOf(
            NewsSource("jor_petra", "بترا", "https://petra.gov.jo/rss", "Jordan"),
            NewsSource("jor_roya", "رؤيا", "https://roya.tv/rss", "Jordan")
        ),
        "Egypt" to listOf(
            NewsSource("egy_ahram", "الأهرام", "https://www.ahram.org.eg/rss", "Egypt"),
            NewsSource("egy_youm7", "اليوم السابع", "https://www.youm7.com/rss", "Egypt")
        ),
        "Saudi Arabia" to listOf(
            NewsSource("ksa_spa", "واس", "https://www.spa.gov.sa/rss", "Saudi Arabia"),
            NewsSource("ksa_okaz", "عكاظ", "https://www.okaz.com.sa/rss", "Saudi Arabia")
        )
    )

    val worldNewsSources: List<NewsSource> = listOf(
        NewsSource("world_bbc", "BBC Arabic", "https://www.bbc.com/arabic/rss", "World"),
        NewsSource("world_aljazeera", "Al Jazeera", "https://www.aljazeera.net/xml/rss/all.xml", "World")
    )

    val techNewsSources: List<NewsSource> = listOf(
        NewsSource("tech_aitnews", "aitnews", "https://aitnews.com/feed/", "Tech")
    )

    fun sourcesForCountry(country: String, disabled: Set<String>): List<NewsSource> {
        val all = (countryNewsSources[country] ?: emptyList()) + worldNewsSources + techNewsSources
        return all.filter { it.id !in disabled }
    }

    val natureWallpapers = listOf(
        "غروب هادئ" to "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1920&q=80",
        "جبال الضباب" to "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1920&q=80",
        "غابة خضراء" to "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=1920&q=80",
        "شاطئ ليلي" to "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1920&q=80",
        "صحراء ذهبية" to "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=1920&q=80",
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

    private const val MP3QURAN_RADIOS_URL = "https://mp3quran.net/api/v3/radios"
    private const val CACHED_RADIOS_KEY = "cached_quran_radios"
    private const val CACHED_RADIOS_TIMESTAMP_KEY = "cached_quran_radios_timestamp"
    private const val CACHE_VALIDITY_HOURS = 24

    private const val HADEETH_ENC_API = "https://hadeethenc.com/api/v1/hadith"
    private const val HADEETH_ENC_PAGE = "https://hadeethenc.com"
    private const val CACHED_HADITH_KEY = "cached_hadith"
    private const val CACHED_HADITH_ID_KEY = "cached_hadith_id"
    private const val CACHED_HADITH_TIMESTAMP_KEY = "cached_hadith_timestamp"
    private const val HADITH_CACHE_HOURS = 24

    private val embeddedHadiths = listOf(
        HadithItem(1, "قال رسول الله ﷺ: «خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ»", "فضل تعليم القرآن وتعلمه.", "أفضل الأعمال تعليم كتاب الله وتعليمه للناس.", "صحيح البخاري"),
        HadithItem(2, "قال رسول الله ﷺ: «إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى»", "الأعمال مبنية على النيات.", "نية العامل تحدد قبول عمله عند الله تعالى.", "صحيح البخاري"),
        HadithItem(3, "قال رسول الله ﷺ: «الْكَلِمَةُ الطَّيِّبَةُ صَدَقَةٌ»", "الكلمة الطيبة صدقة.", "محاسن الأخلاق والكلام الطيب عبادة يتقرب بها إلى الله.", "متفق عليه"),
        HadithItem(4, "قال رسول الله ﷺ: «لَا يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ»", "حب الخير للغير شرط في الإيمان.", "الأخوة الإسلامية والتعاطف بين المسلمين.", "متفق عليه"),
        HadithItem(5, "قال رسول الله ﷺ: «اطْعَمُوا الطَّعَامَ، وَأَفْشُوا السَّلَامَ، وَصِلُوا الْأَرْحَامَ، وَصَلُّوا بِاللَّيْلِ وَالنَّاسُ نِيَامٌ، تَدْخُلُوا الْجَنَّةَ بِسَلَامٍ»", "إطعام الطعام وإفشاء السلام وصلة الرحم.", "أعمال البر التي تؤدي إلى دخول الجنة بالسلام.", "مسند الإمام أحمد"),
        HadithItem(6, "قال رسول الله ﷺ: «مَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الْآخِرِ فَلْيَقُلْ خَيْرًا أَوْ لِيَصْمُتْ»", "قول الخير أو السكوت.", "حفظ اللسان وسداد القول من علامات الإيمان.", "متفق عليه"),
        HadithItem(7, "قال رسول الله ﷺ: «الدِّينُ النَّصِيحَةُ»", "الدين النصيحة لله ورسوله ولأئمة المسلمين وعامتهم.", "النصيحة قلب الدين وأساس صلاح الأمة.", "صحيح مسلم"),
        HadithItem(8, "قال رسول الله ﷺ: «مَنْ كَفَّ شَرَّهُ عَنْ النَّاسِ فَهِيَ صَدَقَةٌ مِنْهُ عَلَى نَفْسِهِ»", "كف الأذى عن الناس صدقة.", "الكف عن الأذى صدقة يتقرب بها العبد إلى ربه.", "متفق عليه"),
        HadithItem(9, "قال رسول الله ﷺ: «كُلُّ مَعْرُوفٍ صَدَقَةٌ»", "كل عمل معروف صدقة.", "كل عمل خير يقدمه المسلم يعتبر صدقة.", "متفق عليه"),
        HadithItem(10, "قال رسول الله ﷺ: «اطْلُبُوا الْعِلْمَ مِنَ الْمَهْدِ إِلَى اللَّحْدِ»", "طلب العلم فريضة على كل مسلم.", "طلب العلم من المهد إلى اللذة طريق للنجاة والفلاح.", "مستدرك الحاكم")
    )


    suspend fun fetchQuranRadios(context: Context): List<QuranRadio> = withContext(Dispatchers.IO) {
        val cached = getCachedRadios(context)
        if (cached.isNotEmpty() && isCacheValid(context)) {
            return@withContext cached
        }

        val fetched = try {
            val raw = httpGet(MP3QURAN_RADIOS_URL) ?: ""
            if (raw.isNotEmpty()) {
                val json = JSONObject(raw)
                val radiosArray = json.optJSONArray("radios") ?: json.optJSONArray("data") ?: JSONArray()
                val result = mutableListOf<QuranRadio>()
                for (i in 0 until radiosArray.length()) {
                    val obj = radiosArray.getJSONObject(i)
                    val name = obj.optString("name").ifBlank { null }
                        ?: obj.optString("radio_name").ifBlank { null }
                        ?: obj.optString("title").ifBlank { null }
                    val url = obj.optString("url").ifBlank { null }
                        ?: obj.optString("radio_url").ifBlank { null }
                        ?: obj.optString("stream_url").ifBlank { null }
                    if (name != null && url != null && url.startsWith("http")) {
                        result.add(QuranRadio(name, url))
                    }
                }
                if (result.isNotEmpty()) {
                    cacheRadios(context, result)
                    result
                } else {
                    getCachedRadios(context).ifEmpty { getDefaultRadios() }
                }
            } else {
                getCachedRadios(context).ifEmpty { getDefaultRadios() }
            }
        } catch (e: Exception) {
            System.err.println("Failed to fetch radios from mp3quran.net: ${e.message}")
            getCachedRadios(context).ifEmpty { getDefaultRadios() }
        }
        fetched
    }

    private suspend fun getCachedRadios(context: Context): List<QuranRadio> = withContext(Dispatchers.IO) {
        try {
            val jsonStr = context.liveDataStore.data.first()[stringPreferencesKey(CACHED_RADIOS_KEY)] ?: ""
            if (jsonStr.isNotBlank()) {
                val array = JSONArray(jsonStr)
                val result = mutableListOf<QuranRadio>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    result.add(QuranRadio(obj.getString("name"), obj.getString("url")))
                }
                result
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun isCacheValid(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val timestamp = context.liveDataStore.data.first()[longPreferencesKey(CACHED_RADIOS_TIMESTAMP_KEY)] ?: 0L
            val now = System.currentTimeMillis()
            (now - timestamp) < (CACHE_VALIDITY_HOURS * 60 * 60 * 1000L)
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun cacheRadios(context: Context, radios: List<QuranRadio>) = withContext(Dispatchers.IO) {
        try {
            val array = JSONArray()
            for (radio in radios) {
                array.put(JSONObject().apply {
                    put("name", radio.name)
                    put("url", radio.url)
                })
            }
            context.liveDataStore.edit { prefs ->
                prefs[stringPreferencesKey(CACHED_RADIOS_KEY)] = array.toString()
                prefs[longPreferencesKey(CACHED_RADIOS_TIMESTAMP_KEY)] = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.err.println("Failed to cache radios: ${e.message}")
        }
    }

    private fun getDefaultRadios(): List<QuranRadio> = listOf(
        QuranRadio("إذاعة القرآن الكريم - مكة المكرمة", "https://stream.radiojar.com/8s5u5tpdtwzuv"),
        QuranRadio("إذاعة القرآن الكريم - المدينة المنورة", "https://stream.radiojar.com/medinah"),
        QuranRadio("إذاعة القرآن الكريم - القاهرة", "https://stream.radiojar.com/8s5u5tpdtwzuv"),
        QuranRadio("إذاعة القرآن الكريم - نابلس", "https://nablus.radiojar.com/stream"),
        QuranRadio("إذاعة القرآن الكريم - القدس", "https://quds.radiojar.com/stream"),
        QuranRadio("إذاعة القرآن الكريم - السعودية (إخبارية)", "https://radio.qurango.net/saudi"),
        QuranRadio("تراتيل قصيرة خاشعة (mp3quran)", "https://qurango.net/radio/short.mp3"),
        QuranRadio("الشيخ ماهر المعيقلي (mp3quran)", "https://qurango.net/radio/maher.mp3"),
        QuranRadio("الشيخ عبدالباسط عبدالصمد", "https://qurango.net/radio/abdulbasit.mp3"),
        QuranRadio("الشيخ مشاري العفاسي", "https://qurango.net/radio/afasy.mp3"),
        QuranRadio("الشيخ ياسر الدوسري", "https://qurango.net/radio/dosari.mp3")
    )

    private suspend fun getCachedHadith(context: Context): HadithItem? = withContext(Dispatchers.IO) {
        try {
            val jsonStr = context.liveDataStore.data.first()[stringPreferencesKey(CACHED_HADITH_KEY)] ?: ""
            val id = context.liveDataStore.data.first()[longPreferencesKey(CACHED_HADITH_ID_KEY)] ?: 0L
            if (jsonStr.isNotBlank()) {
                val obj = JSONObject(jsonStr)
                return@withContext HadithItem(
                    id,
                    obj.optString("text"),
                    obj.optString("explanation"),
                    obj.optString("benefits"),
                    obj.optString("source")
                )
            }
        } catch (_: Exception) { }
        null
    }

    private suspend fun isHadithCacheValid(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val timestamp = context.liveDataStore.data.first()[longPreferencesKey(CACHED_HADITH_TIMESTAMP_KEY)] ?: 0L
            val now = System.currentTimeMillis()
            (now - timestamp) < (HADITH_CACHE_HOURS * 60 * 60 * 1000L)
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun cacheHadith(context: Context, hadith: HadithItem) = withContext(Dispatchers.IO) {
        try {
            context.liveDataStore.edit { prefs ->
                prefs[stringPreferencesKey(CACHED_HADITH_KEY)] = JSONObject().apply {
                    put("text", hadith.text)
                    put("explanation", hadith.explanation)
                    put("benefits", hadith.benefits)
                    put("source", hadith.source)
                }.toString()
                prefs[longPreferencesKey(CACHED_HADITH_ID_KEY)] = hadith.id.toLong()
                prefs[longPreferencesKey(CACHED_HADITH_TIMESTAMP_KEY)] = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.err.println("Failed to cache hadith: ${e.message}")
        }
    }

    suspend fun fetchTodaysHadith(context: Context): HadithItem = withContext(Dispatchers.IO) {
        val todayId = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % embeddedHadiths.size + 1
        if (isHadithCacheValid(context)) {
            getCachedHadith(context)?.let { if (it.id == todayId) return@withContext it }
        }
        val fetched = tryFetchHadith(todayId) ?: embeddedHadiths.firstOrNull { it.id == todayId } ?: embeddedHadiths.first()
        try { cacheHadith(context, fetched) } catch (_: Exception) { }
        fetched
    }

    private suspend fun tryFetchHadith(id: Int): HadithItem? = withContext(Dispatchers.IO) {
        val urls = listOf("$HADEETH_ENC_API/$id", "$HADEETH_ENC_PAGE/$id")
        for (url in urls) {
            val raw = httpGet(url) ?: continue
            try {
                val obj = JSONObject(raw)
                val text = obj.optString("hadith_arabic")
                    .ifBlank { obj.optString("arabic") }
                    .ifBlank { obj.optString("text") }
                    .ifBlank { obj.optString("hadith") }
                    .ifBlank { obj.optString("content") }
                val explanation = obj.optString("explanation")
                    .ifBlank { obj.optString("sharh") }
                    .ifBlank { obj.optString("tafsir") }
                    .ifBlank { obj.optString("description") }
                val benefits = obj.optString("benefits")
                    .ifBlank { obj.optString("fawaid") }
                    .ifBlank { obj.optString("reasons") }
                    .ifBlank { obj.optString("lessons") }
                val source = obj.optString("source")
                    .ifBlank { obj.optString("reference") }
                    .ifBlank { obj.optString("book") }
                    .ifBlank { obj.optString("collector") }
                if (text.isNotBlank()) {
                    return@withContext HadithItem(id, text, explanation, benefits, source)
                }
            } catch (_: Exception) { continue }
        }
        null
    }

    private var cachedSurahList: List<SurahHeader>? = null
    private val cachedSurahTexts = mutableMapOf<Int, FullSurahContent>()

    private fun httpGet(urlStr: String): String? {
        return try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpsURLConnection
            conn.apply {
                connectTimeout = 15000
                readTimeout = 15000
                sslSocketFactory = sslContext.socketFactory
                hostnameVerifier = javax.net.ssl.HostnameVerifier { _, _ -> true }
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android TV; PalestineTVLauncher) AppleWebKit/537.36")
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
            val url = "https://cdn.jsdelivr.net/gh/fawazahmed0/quran-api@1/editions/ar/quran-uthmani/$surahNumber.json"
            val raw = httpGet(url) ?: return@withContext null
            val data = JSONObject(raw)
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
                if (i == 0 && surahNumber != 1 && txt.startsWith("بِسْمِ ٱللَّهِ ٱالرَّحْمَٰنِ ٱالرَّحِيمِ")) {
                    txt = txt.removePrefix("بِسْمِ ٱللَّهِ ٱالرَّحْمَٰنِ ٱالرَّحِيمِ").trim()
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

    suspend fun fetchSurahWithAudio(context: Context, surahNumber: Int): List<Verse>? = withContext(Dispatchers.IO) {
        val cacheKey = stringPreferencesKey("surah_${surahNumber}")
        // Offline-first: return cached if present
        try {
            val cachedJson = context.liveDataStore.data.first()[cacheKey] ?: ""
            if (cachedJson.isNotBlank()) {
                val arr = JSONArray(cachedJson)
                val verses = mutableListOf<Verse>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    verses.add(Verse(o.getInt("num"), o.getString("text"), o.optString("audio"), o.optString("tafsir")))
                }
                if (verses.isNotEmpty()) return@withContext verses
            }
        } catch (_: Exception) { }

        try {
            val uthmaniRaw = httpGet("https://alquran.cloud/$surahNumber/quran-uthmani") ?: httpGet("https://api.alquran.cloud/v1/surah/$surahNumber/quran-uthmani")
            val muyassarRaw = httpGet("https://alquran.cloud/$surahNumber/ar.muyassar") ?: httpGet("https://api.alquran.cloud/v1/surah/$surahNumber/ar.muyassar")
            val uthmaniAyahs = if (uthmaniRaw != null) JSONObject(uthmaniRaw).optJSONObject("data")?.optJSONArray("ayahs") else null
            val muyassarAyahs = if (muyassarRaw != null) JSONObject(muyassarRaw).optJSONObject("data")?.optJSONArray("ayahs") else null

            val verses = mutableListOf<Verse>()
            if (uthmaniAyahs != null) {
                for (i in 0 until uthmaniAyahs.length()) {
                    val a = uthmaniAyahs.getJSONObject(i)
                    val num = a.optInt("numberInSurah", i + 1)
                    var txt = a.optString("text")
                    if (i == 0 && surahNumber != 1 && txt.startsWith("بِسْمِ ٱللَّهِ ٱالرَّحْمَٰنِ ٱالرَّحِيمِ")) {
                        txt = txt.removePrefix("بِسْمِ ٱللَّهِ ٱالرَّحْمَٰنِ ٱالرَّحِيمِ").trim()
                    }
                    val tafsir = if (muyassarAyahs != null && i < muyassarAyahs.length()) muyassarAyahs.getJSONObject(i).optString("text") else ""
                    val surah3 = "%03d".format(surahNumber)
                    val ayah3 = "%03d".format(num)
                    val audioUrl = "https://everyayah.com/data/Al_Afasy_128kbps/${surah3}${ayah3}.mp3"
                    verses.add(Verse(num, txt, audioUrl, tafsir))
                }
            }
            if (verses.isNotEmpty()) {
                // cache
                try {
                    val arr = JSONArray()
                    for (v in verses) {
                        arr.put(JSONObject().apply {
                            put("num", v.number)
                            put("text", v.text)
                            put("audio", v.audioUrl ?: "")
                            put("tafsir", v.tafsir)
                        })
                    }
                    context.liveDataStore.edit { prefs -> prefs[cacheKey] = arr.toString() }
                } catch (_: Exception) { }
                return@withContext verses
            }
        } catch (e: Exception) {
            System.err.println("alquran fetch failed for surah $surahNumber: ${e.message}")
        }
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
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&daily=temperature_2m_max,temperature_2m_min&timezone=$encodedTz"
            val raw = httpGet(url)
                ?: return@withContext null
            val json = JSONObject(raw)
            val current = json.optJSONObject("current_weather")
            val currentTemp = current?.optDouble("temperature", 0.0)?.toInt()?.toString() ?: "0"
            val daily = json.optJSONObject("daily") ?: JSONObject()
            val max = daily.optJSONArray("temperature_2m_max")?.optDouble(0, 0.0)?.toInt()?.toString() ?: "0"
            val min = daily.optJSONArray("temperature_2m_min")?.optDouble(0, 0.0)?.toInt()?.toString() ?: "0"
            Triple(currentTemp, max, min)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchCityWeatherDetailed(lat: Double, lon: Double, timeZoneId: String): WeatherDetail? = withContext(Dispatchers.IO) {
        try {
            val encodedTz = URLEncoder.encode(timeZoneId, "UTF-8")
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&hourly=temperature_2m,precipitation_probability&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=$encodedTz"
            val raw = httpGet(url)
                ?: return@withContext null
            val json = JSONObject(raw)

            val current = json.optJSONObject("current_weather")
            val currentTemp = current?.optDouble("temperature", 0.0)?.toInt()?.toString() ?: "0"

            val hourlyObj = json.optJSONObject("hourly") ?: JSONObject()
            val hourlyTimes = hourlyObj.optJSONArray("time") ?: JSONArray()
            val hourlyTemps = hourlyObj.optJSONArray("temperature_2m") ?: JSONArray()
            val hourlyProbs = hourlyObj.optJSONArray("precipitation_probability") ?: JSONArray()
            val hourly = mutableListOf<WeatherHour>()
            val limit = hourlyTimes.length().coerceAtMost(hourlyTemps.length()).coerceAtMost(hourlyProbs.length())
            for (i in 0 until limit) {
                hourly.add(
                    WeatherHour(
                        hourlyTimes.optString(i),
                        hourlyTemps.optDouble(i, 0.0).toInt().toString(),
                        hourlyProbs.optInt(i, 0)
                    )
                )
            }

            val dailyObj = json.optJSONObject("daily") ?: JSONObject()
            val dailyDates = dailyObj.optJSONArray("time") ?: JSONArray()
            val dailyMaxs = dailyObj.optJSONArray("temperature_2m_max") ?: JSONArray()
            val dailyMins = dailyObj.optJSONArray("temperature_2m_min") ?: JSONArray()
            val dailyProbs = dailyObj.optJSONArray("precipitation_probability_max") ?: JSONArray()
            val daily = mutableListOf<WeatherDay>()
            val dLimit = dailyDates.length().coerceAtMost(dailyMaxs.length()).coerceAtMost(dailyMins.length()).coerceAtMost(dailyProbs.length())
            for (i in 0 until dLimit) {
                daily.add(
                    WeatherDay(
                        dailyDates.optString(i),
                        dailyMaxs.optDouble(i, 0.0).toInt().toString(),
                        dailyMins.optDouble(i, 0.0).toInt().toString(),
                        dailyProbs.optInt(i, 0)
                    )
                )
            }

            val precipProb = current?.optInt("precipitation_probability", 0)
                ?: dailyProbs.optInt(0, 0)

            WeatherDetail(currentTemp, hourly, daily, precipProb)
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
    enum class LiveWallpaperCategory(val title: String, val commonsCategory: String?, val key: String) {
        ALL("الكل المتنوع", "Category:Featured_pictures_on_Wikimedia_Commons", "all"),
        NATURE("الطبيعة والمناظر الخلابة", "Category:Nature", "nature"),
        MOSQUES("المساجد والعمارة الإسلامية", "Category:Islamic architecture", "mosques"),
        SPACE("الفضاء والنجوم", "Category:Outer space", "space"),
        BING("صور Bing اليومية", null, "bing"),
        CITIES_NIGHT("المدن والليل", "Category:Cities at night", "cities_night");

        companion object {
            fun fromTitle(title: String): LiveWallpaperCategory =
                values().firstOrNull { it.title == title } ?: ALL
        }
    }

    private const val LIVE_WALLPAPER_CACHE_HOURS = 6

    private fun isValidWallpaperUrl(url: String): Boolean =
        url.startsWith("https", ignoreCase = true) &&
            (url.endsWith(".jpg", ignoreCase = true) || url.endsWith(".png", ignoreCase = true))

    private val defaultLiveWallpapers = listOf(
        "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1920&q=80",
        "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1920&q=80",
        "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=1920&q=80"
    )

    private suspend fun appendBingWallpapers(sink: MutableList<String>) {
        try {
            val raw = httpGet("https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=8&mkt=en-US") ?: return
            val arr = JSONObject(raw).optJSONArray("images") ?: return
            for (i in 0 until arr.length()) {
                val u = arr.getJSONObject(i).optString("url")
                if (u.isNotBlank()) {
                    val full = if (u.startsWith("http")) u else "https://www.bing.com$u"
                    if (isValidWallpaperUrl(full)) sink.add(full)
                }
            }
        } catch (_: Exception) { }
    }

    private suspend fun appendCommonsWallpapers(category: LiveWallpaperCategory, sink: MutableList<String>) {
        val title = category.commonsCategory ?: return
        try {
            val encoded = URLEncoder.encode(title, "UTF-8")
            val api = "https://commons.wikimedia.org/w/api.php?action=query&generator=categorymembers&gcmtitle=$encoded&gcmnamespace=6&gcmlimit=50&prop=imageinfo&iiprop=url&format=json"
            val raw = httpGet(api) ?: return
            val pages = JSONObject(raw).optJSONObject("query")?.optJSONObject("pages")
            pages?.keys()?.forEach { k ->
                val ii = pages.optJSONObject(k)?.optJSONArray("imageinfo")
                val url = ii?.optJSONObject(0)?.optString("url")
                if (!url.isNullOrBlank() && isValidWallpaperUrl(url)) sink.add(url)
            }
        } catch (_: Exception) { }
    }

    suspend fun fetchLiveWallpapers(context: Context, category: LiveWallpaperCategory = LiveWallpaperCategory.ALL): List<String> = withContext(Dispatchers.IO) {
        val cached = getCachedLiveWallpapers(context, category)
        if (cached.isNotEmpty() && isLiveWallpaperCacheValid(context, category)) return@withContext cached

        val result = mutableListOf<String>()
        if (category == LiveWallpaperCategory.BING || category == LiveWallpaperCategory.ALL) {
            appendBingWallpapers(result)
        }
        if (category != LiveWallpaperCategory.BING) {
            appendCommonsWallpapers(category, result)
        }
        if (result.isEmpty()) {
            result += defaultLiveWallpapers
        }
        val filtered = result.distinct()
        if (filtered.isNotEmpty()) cacheLiveWallpapers(context, category, filtered)
        filtered.ifEmpty { cached.ifEmpty { emptyList() } }
    }

    private suspend fun getCachedLiveWallpapers(context: Context, category: LiveWallpaperCategory): List<String> = withContext(Dispatchers.IO) {
        try {
            val jsonStr = context.liveDataStore.data.first()[stringPreferencesKey("cached_live_walls_${category.key}")] ?: ""
            if (jsonStr.isNotBlank()) {
                val arr = JSONArray(jsonStr)
                (0 until arr.length()).map { index -> arr.getString(index) }
            } else emptyList()
        } catch (_: Exception) { emptyList() }
    }

    private suspend fun isLiveWallpaperCacheValid(context: Context, category: LiveWallpaperCategory): Boolean = withContext(Dispatchers.IO) {
        try {
            val ts = context.liveDataStore.data.first()[longPreferencesKey("cached_live_walls_${category.key}_ts")] ?: 0L
            System.currentTimeMillis() - ts < LIVE_WALLPAPER_CACHE_HOURS * 60 * 60 * 1000L
        } catch (_: Exception) { false }
    }

    private suspend fun cacheLiveWallpapers(context: Context, category: LiveWallpaperCategory, list: List<String>) = withContext(Dispatchers.IO) {
        try {
            val arr = JSONArray()
            list.forEach { arr.put(it) }
            context.liveDataStore.edit { prefs ->
                prefs[stringPreferencesKey("cached_live_walls_${category.key}")] = arr.toString()
                prefs[longPreferencesKey("cached_live_walls_${category.key}_ts")] = System.currentTimeMillis()
            }
        } catch (_: Exception) { }
    }
}
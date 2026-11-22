package com.palestine.tvlauncher.data

import android.icu.util.IslamicCalendar
import java.text.SimpleDateFormat
import java.util.*

data class PalestinianCity(
    val name: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val timeZoneId: String = "Asia/Hebron"
)

data class QuranSurah(val number: Int, val name: String, val verses: String)

object PalestineData {

    val palestineCities = listOf(
        PalestinianCity("أريحا (الزبيدات)", "فلسطين", 32.0450, 35.4580, "Asia/Hebron"),
        PalestinianCity("أريحا", "فلسطين", 31.8611, 35.4618, "Asia/Hebron"),
        PalestinianCity("القدس", "فلسطين", 31.7683, 35.2137, "Asia/Jerusalem"),
        PalestinianCity("رام الله", "فلسطين", 31.9038, 35.2034, "Asia/Hebron"),
        PalestinianCity("البيرة", "فلسطين", 31.9110, 35.2160, "Asia/Hebron"),
        PalestinianCity("نابلس", "فلسطين", 32.2211, 35.2544, "Asia/Hebron"),
        PalestinianCity("الخليل", "فلسطين", 31.5326, 35.0998, "Asia/Hebron"),
        PalestinianCity("بيت لحم", "فلسطين", 31.7054, 35.2024, "Asia/Hebron"),
        PalestinianCity("جنين", "فلسطين", 32.4573, 35.2869, "Asia/Hebron"),
        PalestinianCity("طولكرم", "فلسطين", 32.3104, 35.0286, "Asia/Hebron"),
        PalestinianCity("قلقيلية", "فلسطين", 32.1892, 34.9706, "Asia/Hebron"),
        PalestinianCity("طوباس", "فلسطين", 32.3209, 35.3699, "Asia/Hebron"),
        PalestinianCity("سلفيت", "فلسطين", 32.0852, 35.1806, "Asia/Hebron"),
        PalestinianCity("غزة", "فلسطين", 31.5017, 34.4668, "Asia/Gaza"),
        PalestinianCity("خان يونس", "فلسطين", 31.3462, 34.3063, "Asia/Gaza"),
        PalestinianCity("رفح", "فلسطين", 31.2969, 34.2435, "Asia/Gaza"),
        PalestinianCity("دير البلح", "فلسطين", 31.4171, 34.3493, "Asia/Gaza"),
        PalestinianCity("حيفا", "فلسطين", 32.7940, 34.9896, "Asia/Jerusalem"),
        PalestinianCity("يافا", "فلسطين", 32.0504, 34.7522, "Asia/Jerusalem"),
        PalestinianCity("عكا", "فلسطين", 32.9278, 35.0818, "Asia/Jerusalem"),
        PalestinianCity("الناصرة", "فلسطين", 32.6996, 35.3035, "Asia/Jerusalem"),
        PalestinianCity("صفد", "فلسطين", 32.9646, 35.4960, "Asia/Jerusalem"),
        PalestinianCity("طبريا", "فلسطين", 32.7922, 35.5312, "Asia/Jerusalem"),
        PalestinianCity("بئر السبع", "فلسطين", 31.2530, 34.7915, "Asia/Jerusalem"),
        PalestinianCity("اللد", "فلسطين", 31.9514, 34.8881, "Asia/Jerusalem"),
        PalestinianCity("الرملة", "فلسطين", 31.9275, 34.8625, "Asia/Jerusalem"),
        PalestinianCity("أم الفحم", "فلسطين", 32.5173, 35.1535, "Asia/Jerusalem")
    )

    val worldCapitals = listOf(
        PalestinianCity("عمّان", "الأردن", 31.9454, 35.9284, "Asia/Amman"),
        PalestinianCity("القاهرة", "مصر", 30.0444, 31.2357, "Africa/Cairo"),
        PalestinianCity("مكة المكرمة", "السعودية", 21.3891, 39.8579, "Asia/Riyadh"),
        PalestinianCity("المدينة المنورة", "السعودية", 24.5247, 39.5692, "Asia/Riyadh"),
        PalestinianCity("الرياض", "السعودية", 24.7136, 46.6753, "Asia/Riyadh"),
        PalestinianCity("دبي", "الإمارات", 25.2048, 55.2708, "Asia/Dubai"),
        PalestinianCity("أبو ظبي", "الإمارات", 24.4539, 54.3773, "Asia/Dubai"),
        PalestinianCity("الدوحة", "قطر", 25.2854, 51.5310, "Asia/Qatar"),
        PalestinianCity("الكويت", "الكويت", 29.3759, 47.9774, "Asia/Kuwait"),
        PalestinianCity("بيروت", "لبنان", 33.8938, 35.5018, "Asia/Beirut"),
        PalestinianCity("دمشق", "سوريا", 33.5138, 36.2765, "Asia/Damascus"),
        PalestinianCity("بغداد", "العراق", 33.3152, 44.3661, "Asia/Baghdad"),
        PalestinianCity("إسطنبول", "تركيا", 41.0082, 28.9784, "Europe/Istanbul"),
        PalestinianCity("أنقرة", "تركيا", 39.9334, 32.8597, "Europe/Istanbul"),
        PalestinianCity("لندن", "بريطانيا", 51.5074, -0.1278, "Europe/London"),
        PalestinianCity("باريس", "فرنسا", 48.8566, 2.3522, "Europe/Paris"),
        PalestinianCity("برلين", "ألمانيا", 52.5200, 13.4050, "Europe/Berlin"),
        PalestinianCity("مدريد", "إسبانيا", 40.4168, -3.7038, "Europe/Madrid"),
        PalestinianCity("نيويورك", "أمريكا", 40.7128, -74.0060, "America/New_York"),
        PalestinianCity("طوكيو", "اليابان", 35.6762, 139.6503, "Asia/Tokyo")
    )

    val cities = palestineCities + worldCapitals

    val quranRadios = listOf(
        "إذاعة القرآن الكريم - مكة المكرمة" to "https://radiojar.com",
        "إذاعة القرآن الكريم - القاهرة" to "https://radiojar.com"
    )

    val fullQuranSurahs = listOf(
        QuranSurah(1, "سورة الفاتحة", "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ (١) الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ (٢) الرَّحْمَٰنِ الرَّحِيمِ (٣) مَالِكِ يَوْمِ الدِّينِ (٤) إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ (٥) اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ (٦) صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ (٧)"),
        QuranSurah(2, "آية الكرسي (البقرة)", "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَنْ ذَا الَّذِي يَشْفَعُ عِنْدَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ"),
        QuranSurah(18, "أوائل سورة الكهف", "الْحَمْدُ لِلَّهِ الَّذِي أَنْزَلَ عَلَىٰ عَبْدِهِ الْكِتَابَ وَلَمْ يَجْعَلْ لَهُ عِوَجًا (١) قَيِّمًا لِيُنْذِرَ بَأْسًا شَدِيدًا مِنْ لَدُنْهُ وَيُبَشِّرَ الْمُؤْمِنِينَ الَّذِينَ يَعْمَلُونَ الصَّالِحَاتِ أَنَّ لَهُمْ أَجْرًا حَسَنًا (٢) مَاكِثِينَ فِيهِ أَبَدًا (٣)"),
        QuranSurah(36, "أوائل سورة يس", "يس (١) وَالْقُرْآنِ الْحَكِيمِ (٢) إِنَّكَ لَمِنَ الْمُرْسَلِينَ (٣) عَلَىٰ صِرَاطٍ مُسْتَقِيمٍ (٤) تَنْزِيلَ الْعَزِيزِ الرَّحِيمِ (٥) لِتُنْذِرَ قَوْمًا مَا أُنْذِرَ آبَاؤُهُمْ فَهُمْ غَافِلُونَ (٦)"),
        QuranSurah(67, "أوائل سورة الملك", "تَبَارَكَ الَّذِي بِيَدِهِ الْمُلْكُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ (١) الَّذِي خَلَقَ الْمَوْتَ وَالْحَيَاةَ لِيَبْلُوَكُمْ أَيُّكُمْ أَحْسَنُ عَمَلًا ۚ وَهُوَ الْعَلِيُّ الْغَفُورُ (٢)"),
        QuranSurah(87, "سورة الأعلى", "سَبِّحِ اسْمَ رَبِّكَ الْأَعْلَى (١) الَّذِي خَلَقَ فَسَوَّىٰ (٢) وَالَّذِي قَدَّرَ فَهَدَىٰ (٣) وَالَّذِي أَخْرَجَ الْمَرْعَىٰ (٤) فَجَعَلَهُ غُثَاءً أَحْوَىٰ (٥) سَنُقْرِئُكَ فَلَا تَنْسَىٰ (٦) إِلَّا مَا شَاءَ اللَّهُ ۚ إِنَّهُ يَعْلَمُ الْجَهْرَ وَمَا يَخْفَىٰ (٧)"),
        QuranSurah(112, "سورة الإخلاص", "قُلْ هُوَ اللَّهُ أَحَدٌ (١) اللَّهُ الصَّمَدُ (٢) لَمْ يَلِدْ وَلَمْ يُولَدْ (٣) وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ (٤)"),
        QuranSurah(113, "سورة الفلق", "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ (١) مِنْ شَرِّ مَا خَلَقَ (٢) وَمِنْ شَرِّ غَاسِقٍ إِذَا وَقَبَ (٣) وَمِنْ شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ (٤) وَمِنْ شَرِّ حَاسِدٍ إِذَا حَسَدَ (٥)"),
        QuranSurah(114, "سورة الناس", "قُلْ أَعُوذُ بِرَبِّ النَّاسِ (١) مَلِكِ النَّاسِ (٢) إِلَٰهِ النَّاسِ (٣) مِنْ شَرِّ الْوَسْوَاسِ الْخَنَّاسِ (٤) الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ (٥) مِنَ الْجِنَّةِ وَالنَّاسِ (٦)")
    )

    val dailyVerses = fullQuranSurahs.map { "${it.name}: ${it.verses}" }

    val dailyHadiths = listOf(
        "قال رسول الله ﷺ: «خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ»",
        "قال رسول الله ﷺ: «إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى»",
        "قال رسول الله ﷺ: «الْكَلِمَةُ الطَّيِّبَةُ صَدَقَةٌ»"
    )

    val adhkarList = listOf(
        "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ.",
        "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ.",
        "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ.",
        "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ (٣ مرات).",
        "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ (٣ مرات)."
    )

    data class PrayerDisplay(
        val fajr: String = "٠٤:٥٢",
        val sunrise: String = "٠٦:١٧",
        val dhuhr: String = "١١:٣٤",
        val asr: String = "١٤:٢٥",
        val maghrib: String = "١٦:٤٩",
        val isha: String = "١٨:٠٨",
        val nextPrayerName: String = "الفجر",
        val remainingText: String = "مواقيت فلسطين الدقيقة"
    )

    fun toArabicDigits(input: String): String {
        val arabicChars = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        val sb = StringBuilder()
        for (c in input) {
            if (c in '0'..'9') sb.append(arabicChars[c - '0'])
            else sb.append(c)
        }
        return sb.toString()
    }

    fun computeNextPrayer(p: PrayerDisplay, timeZoneId: String = "Asia/Hebron"): PrayerDisplay {
        val now = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId))
        val nowMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        fun parseMins(t: String): Int {
            val clean = t.replace('٠', '0').replace('١', '1').replace('٢', '2')
                .replace('٣', '3').replace('٤', '4').replace('٥', '5')
                .replace('٦', '6').replace('٧', '7').replace('٨', '8').replace('٩', '9')
            val parts = clean.split(":")
            return if (parts.size >= 2) (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0) else 0
        }
        val list = listOf(
            "الفجر" to parseMins(p.fajr),
            "الشروق" to parseMins(p.sunrise),
            "الظهر" to parseMins(p.dhuhr),
            "العصر" to parseMins(p.asr),
            "المغرب" to parseMins(p.maghrib),
            "العشاء" to parseMins(p.isha)
        )
        val next = list.firstOrNull { it.second > nowMins } ?: ("الفجر" to (list.first().second + 1440))
        val diff = next.second - nowMins
        val h = diff / 60
        val m = diff % 60

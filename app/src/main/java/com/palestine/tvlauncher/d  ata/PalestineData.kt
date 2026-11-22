package com.palestine.tvlauncher.data

import android.icu.util.IslamicCalendar
import android.os.Build
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.Prayer
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class PalestinianCity(
    val name: String,
    val lat: Double,
    val lon: Double
)

object PalestineData {
    val cities = listOf(
        PalestinianCity("الزبيدات - أريحا", 32.1642, 35.5311),
        PalestinianCity("أريحا", 31.8611, 35.4618),
        PalestinianCity("القدس الشريف", 31.7683, 35.2137),
        PalestinianCity("نابلس", 32.2211, 35.2544),
        PalestinianCity("رام الله والبيرة", 31.9038, 35.2034),
        PalestinianCity("الخليل", 31.5326, 35.0998),
        PalestinianCity("جنين", 32.4646, 35.2939),
        PalestinianCity("طولكرم", 32.3104, 35.0286),
        PalestinianCity("قلقيلية", 32.1897, 34.9706),
        PalestinianCity("بيت لحم", 31.7054, 35.2024),
        PalestinianCity("طوباس", 32.3209, 35.3686),
        PalestinianCity("سلفيت", 32.0852, 35.1806),
        PalestinianCity("غزة", 31.5017, 34.4668),
        PalestinianCity("خان يونس", 31.3462, 34.3060),
        PalestinianCity("رفح", 31.2969, 34.2455),
        PalestinianCity("الناصرة", 32.6996, 35.3035),
        PalestinianCity("حيفا", 32.7940, 34.9896),
        PalestinianCity("يافا", 32.0532, 34.7515),
        PalestinianCity("عكا", 32.9278, 35.0818)
    )

    data class PrayerDisplay(
        val fajr: String,
        val sunrise: String,
        val dhuhr: String,
        val asr: String,
        val maghrib: String,
        val isha: String,
        val nextPrayerName: String,
        val remainingText: String
    )

    fun calculatePrayers(city: PalestinianCity): PrayerDisplay {
        val coordinates = Coordinates(city.lat, city.lon)
        val dateComponents = DateComponents.from(Date())
        val params = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.apply {
            fajrAngle = 18.0
            ishaAngle = 17.5
            madhab = Madhab.SHAFI
        }
        val prayerTimes = PrayerTimes(coordinates, dateComponents, params)
        val sdf = SimpleDateFormat("hh:mm a", Locale("ar")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Hebron")
        }

        val next = prayerTimes.nextPrayer()
        val nextTime = when (next) {
            Prayer.FAJR -> prayerTimes.fajr
            Prayer.SUNRISE -> prayerTimes.sunrise
            Prayer.DHUHR -> prayerTimes.dhuhr
            Prayer.ASR -> prayerTimes.asr
            Prayer.MAGHRIB -> prayerTimes.maghrib
            Prayer.ISHA -> prayerTimes.isha
            else -> Date(prayerTimes.fajr.time + 86400000L)
        }

        val nextName = when (next) {
            Prayer.FAJR -> "الفجر"
            Prayer.SUNRISE -> "الشروق"
            Prayer.DHUHR -> "الظهر"
            Prayer.ASR -> "العصر"
            Prayer.MAGHRIB -> "المغرب"
            Prayer.ISHA -> "العشاء"
            else -> "الفجر"
        }

        val diffMillis = (nextTime.time - System.currentTimeMillis()).coerceAtLeast(0)
        val hours = diffMillis / (1000 * 60 * 60)
        val minutes = (diffMillis / (1000 * 60)) % 60
        val remaining = if (hours > 0) "$nextName بعد $hours س و $minutes د" else "$nextName بعد $minutes د"

        return PrayerDisplay(
            fajr = sdf.format(prayerTimes.fajr),
            sunrise = sdf.format(prayerTimes.sunrise),
            dhuhr = sdf.format(prayerTimes.dhuhr),
            asr = sdf.format(prayerTimes.asr),
            maghrib = sdf.format(prayerTimes.maghrib),
            isha = sdf.format(prayerTimes.isha),
            nextPrayerName = nextName,
            remainingText = remaining
        )
    }

    fun getFormattedDates(): Pair<String, String> {
        val localeAr = Locale("ar")
        val gregorianFormat = SimpleDateFormat("EEEE d MMMM yyyy", localeAr)
        val gregorian = gregorianFormat.format(Date())

        val hijriMonths = arrayOf(
            "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
            "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
            "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
        )

        val hijri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val cal = IslamicCalendar()
            val day = cal.get(IslamicCalendar.DAY_OF_MONTH)
            val month = hijriMonths[cal.get(IslamicCalendar.MONTH)]
            val year = cal.get(IslamicCalendar.YEAR)
            "$day $month $year هـ"
        } else {
            "التقويم الهجري"
        }

        return Pair(hijri, gregorian)
    }

    fun getDaysUntilRamadan(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val cal = IslamicCalendar()
            val currentMonth = cal.get(IslamicCalendar.MONTH)
            if (currentMonth == IslamicCalendar.RAMADAN) return 0
            val targetYear = if (currentMonth > IslamicCalendar.RAMADAN) {
                cal.get(IslamicCalendar.YEAR) + 1
            } else {
                cal.get(IslamicCalendar.YEAR)
            }
            val ramadanCal = IslamicCalendar(targetYear, IslamicCalendar.RAMADAN, 1)
            val diff = ramadanCal.timeInMillis - cal.timeInMillis
            (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
        } else {
            90
        }
    }

    val dailyVerses = listOf(
        "﴿فَإِنَّ مَعَ الْعُسْرِ يُسْرًا • إِنَّ مَعَ الْعُسْرِ يُسْرًا﴾ [الشرح: 5-6]",
        "﴿وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ﴾ [الطلاق: 3]",
        "﴿أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ﴾ [الرعد: 28]",
        "﴿رَبِّ اشْرَحْ لِي صَدْرِي • وَيَسِّرْ لِي أَمْرِي﴾ [طه: 25-26]"
    )

    val dailyHadiths = listOf(
        "قال ﷺ: «كلمتان خفيفتان على اللسان، ثقيلتان في الميزان، حبيبتان إلى الرحمن: سبحان الله وبحمده، سبحان الله العظيم».",
        "قال ﷺ: «من صلى البردين دخل الجنة» (الفجر والعصر).",
        "قال ﷺ: «أحب الأعمال إلى الله أدومها وإن قل»."
    )

    val adhkarList = listOf(
        "أصبحنا وأصبح الملك لله، والحمد لله، لا إله إلا الله وحده لا شريك له.",
        "اللهم بك أصبحنا، وبك أمسينا، وبك نحيا، وبك نموت، وإليك النشور.",
        "سبحان الله وبحمده (مائة مرة).",
        "أستغفر الله العظيم وأتوب إليه (مائة مرة).",
        "حسبي الله لا إله إلا هو عليه توكلت وهو رب العرش العظيم (سبع مرات)."
    )
}


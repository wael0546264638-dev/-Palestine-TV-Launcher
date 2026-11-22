package com.palestine.tvlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.launcherDataStore by preferencesDataStore(name = "palestine_launcher_prefs")

data class FamilyReminder(
    val id: String,
    val name: String,
    val month: Int,
    val day: Int
)

data class LauncherConfig(
    val cityName: String = "أريحا (الزبيدات)",
    val cityLat: Double = 32.0450,
    val cityLon: Double = 35.4580,
    val cityTimeZone: String = "Asia/Hebron",
    val clockOffsetX: Float = 0f,
    val clockOffsetY: Float = 0f,
    val clockScale: Float = 1.0f,
    val cardOffsetX: Float = 0f,
    val cardOffsetY: Float = 0f,
    val cardScale: Float = 1.0f,
    val cardBgOpacity: Float = 0.72f,
    val dockOffsetX: Float = 0f,
    val dockOffsetY: Float = 0f,
    val dockBgOpacity: Float = 0.55f,
    val appCardOpacity: Float = 0.90f,
    val appBaseScale: Float = 1.0f,
    val appFocusScale: Float = 1.12f,
    val appCornerRadiusPercent: Int = 28,
    val appGlowMode: Int = 0,
    val appCustomGlowHex: String = "#38BDF8",
    val showTopWifi: Boolean = true,
    val showTopQuranRadio: Boolean = true,
    val showTopQuranBook: Boolean = true,
    val showTopScreensaver: Boolean = true,
    val showTopVpn: Boolean = true,
    val showTopSettings: Boolean = true,
    val dateDisplayMode: Int = 0,
    val showBottomBar: Boolean = true,
    val tickerBgOpacity: Float = 0.65f,
    val activeTickerPairs: Set<String> = setOf("USD/ILS", "JOD/ILS", "EUR/ILS", "GOLD", "BTC"),
    val quranPlayMode: Int = 0,
    val quranScheduleStartHour: Int = 6,
    val quranScheduleEndHour: Int = 10,
    val quranStreamUrl: String = "https://radiojar.com",
    val ssClockOffsetX: Float = 0f,
    val ssClockOffsetY: Float = 0f,
    val ssClockScale: Float = 1.0f,
    val wallpaperIndex: Int = 0,
    val autoRotateTabs: Boolean = true,
    val tasks: List<String> = emptyList(),
    val reminders: List<FamilyReminder> = emptyList()
)

class LauncherPreferences(private val context: Context) {

    companion object {
        val CITY_NAME = stringPreferencesKey("city_name")
        val CITY_LAT = doublePreferencesKey("city_lat")
        val CITY_LON = doublePreferencesKey("city_lon")
        val CITY_TZ = stringPreferencesKey("city_tz")
        val CLOCK_X = floatPreferencesKey("clock_x")
        val CLOCK_Y = floatPreferencesKey("clock_y")
        val CLOCK_SCALE = floatPreferencesKey("clock_scale")
        val CARD_X = floatPreferencesKey("card_x")
        val CARD_Y = floatPreferencesKey("card_y")
        val CARD_SCALE = floatPreferencesKey("card_scale")
        val CARD_OPACITY = floatPreferencesKey("card_opacity")
        val DOCK_X = floatPreferencesKey("dock_x")
        val DOCK_Y = floatPreferencesKey("dock_y")
        val DOCK_OPACITY = floatPreferencesKey("dock_opacity")
        val APP_CARD_OPACITY = floatPreferencesKey("app_card_opacity")
        val APP_SCALE = floatPreferencesKey("app_scale")
        val APP_FOCUS_SCALE = floatPreferencesKey("app_focus_scale")
        val APP_CORNER = intPreferencesKey("app_corner")
        val APP_GLOW_MODE = intPreferencesKey("app_glow_mode")
        val APP_CUSTOM_GLOW = stringPreferencesKey("app_custom_glow")
        val SHOW_WIFI = booleanPreferencesKey("show_wifi")
        val SHOW_QURAN = booleanPreferencesKey("show_quran")
        val SHOW_BOOK = booleanPreferencesKey("show_book")
        val SHOW_SS = booleanPreferencesKey("show_ss")
        val SHOW_VPN = booleanPreferencesKey("show_vpn")
        val SHOW_SETTINGS = booleanPreferencesKey("show_settings")
        val DATE_MODE = intPreferencesKey("date_mode")
        val SHOW_BOTTOM = booleanPreferencesKey("show_bottom")
        val TICKER_OPACITY = floatPreferencesKey("ticker_opacity")
        val TICKER_PAIRS = stringSetPreferencesKey("ticker_pairs")
        val QURAN_PLAY_MODE = intPreferencesKey("quran_play_mode")
        val QURAN_SCHED_START = intPreferencesKey("quran_sched_start")
        val QURAN_SCHED_END = intPreferencesKey("quran_sched_end")
        val QURAN_URL = stringPreferencesKey("quran_url")
        val SS_CLOCK_X = floatPreferencesKey("ss_clock_x")
        val SS_CLOCK_Y = floatPreferencesKey("ss_clock_y")
        val SS_CLOCK_SCALE = floatPreferencesKey("ss_clock_scale")
        val WALLPAPER_IDX = intPreferencesKey("wallpaper_idx")
        val AUTO_ROTATE = booleanPreferencesKey("auto_rotate")
        val TASKS_JSON = stringPreferencesKey("tasks_json")
        val REMINDERS_JSON = stringPreferencesKey("reminders_json")
    }

    val configFlow: Flow<LauncherConfig> = context.launcherDataStore.data.map { p ->
        LauncherConfig(
            cityName = p[CITY_NAME] ?: "أريحا (الزبيدات)",
            cityLat = p[CITY_LAT] ?: 32.0450,
            cityLon = p[CITY_LON] ?: 35.4580,
            cityTimeZone = p[CITY_TZ] ?: "Asia/Hebron",
            clockOffsetX = p[CLOCK_X] ?: 0f,
            clockOffsetY = p[CLOCK_Y] ?: 0f,
            clockScale = p[CLOCK_SCALE] ?: 1.0f,
            cardOffsetX = p[CARD_X] ?: 0f,
            cardOffsetY = p[CARD_Y] ?: 0f,
            cardScale = p[CARD_SCALE] ?: 1.0f,
            cardBgOpacity = p[CARD_OPACITY] ?: 0.72f,
            dockOffsetX = p[DOCK_X] ?: 0f,
            dockOffsetY = p[DOCK_Y] ?: 0f,
            dockBgOpacity = p[DOCK_OPACITY] ?: 0.55f,
            appCardOpacity = p[APP_CARD_OPACITY] ?: 0.90f,
            appBaseScale = p[APP_SCALE] ?: 1.0f,
            appFocusScale = p[APP_FOCUS_SCALE] ?: 1.12f,
            appCornerRadiusPercent = p[APP_CORNER] ?: 28,
            appGlowMode = p[APP_GLOW_MODE] ?: 0,
            appCustomGlowHex = p[APP_CUSTOM_GLOW] ?: "#38BDF8",
            showTopWifi = p[SHOW_WIFI] ?: true,
            showTopQuranRadio = p[SHOW_QURAN] ?: true,
            showTopQuranBook = p[SHOW_BOOK] ?: true,
            showTopScreensaver = p[SHOW_SS] ?: true,
            showTopVpn = p[SHOW_VPN] ?: true,
            showTopSettings = p[SHOW_SETTINGS] ?: true,
            dateDisplayMode = p[DATE_MODE] ?: 0,
            showBottomBar = p[SHOW_BOTTOM] ?: true,
            tickerBgOpacity = p[TICKER_OPACITY] ?: 0.65f,
            activeTickerPairs = p[TICKER_PAIRS] ?: setOf("USD/ILS", "JOD/ILS", "EUR/ILS", "GOLD", "BTC"),
            quranPlayMode = p[QURAN_PLAY_MODE] ?: 0,
            quranScheduleStartHour = p[QURAN_SCHED_START] ?: 6,
            quranScheduleEndHour = p[QURAN_SCHED_END] ?: 10,
            quranStreamUrl = p[QURAN_URL] ?: "https://radiojar.com",
            ssClockOffsetX = p[SS_CLOCK_X] ?: 0f,
            ssClockOffsetY = p[SS_CLOCK_Y] ?: 0f,
            ssClockScale = p[SS_CLOCK_SCALE] ?: 1.0f,
            wallpaperIndex = p[WALLPAPER_IDX] ?: 0,
            autoRotateTabs = p[AUTO_ROTATE] ?: true,
            tasks = parseTasks(p[TASKS_JSON] ?: "[]"),
            reminders = parseReminders(p[REMINDERS_JSON] ?: "[]")
        )
    }

    suspend fun updateConfig(block: (MutablePreferences) -> Unit) {
        context.launcherDataStore.edit { block(it) }
    }

    suspend fun setLocation(city: PalestinianCity) {
        updateConfig {
            it[CITY_NAME] = city.name
            it[CITY_LAT] = city.lat
            it[CITY_LON] = city.lon
            it[CITY_TZ] = city.timeZoneId
        }
    }

    suspend fun saveTasks(list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        updateConfig { it[TASKS_JSON] = arr.toString() }
    }

    suspend fun saveReminders(list: List<FamilyReminder>) {
        val arr = JSONArray()
        list.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("name", it.name)
            obj.put("month", it.month)
            obj.put("day", it.day)
            arr.put(obj)
        }
        updateConfig { it[REMINDERS_JSON] = arr.toString() }
    }

    private fun parseTasks(json: String): List<String> = try {
        val arr = JSONArray(json)
        List(arr.length()) { arr.getString(it) }
    } catch (_: Exception) {
        emptyList()
    }

    private fun parseReminders(json: String): List<FamilyReminder> = try {
        val arr = JSONArray(json)
        List(arr.length()) {
            val o = arr.getJSONObject(it)
            FamilyReminder(o.getString("id"), o.getString("name"), o.getInt("month"), o.getInt("day"))
        }
    } catch (_: Exception) {
        emptyList()
    }
}

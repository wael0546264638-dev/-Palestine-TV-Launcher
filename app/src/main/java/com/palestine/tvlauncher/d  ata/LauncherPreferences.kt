package com.palestine.tvlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore by preferencesDataStore(name = "palestine_tv_prefs")

data class FamilyReminder(
    val id: String,
    val name: String,
    val month: Int,
    val day: Int
)

data class LauncherConfig(
    val cityName: String = "الزبيدات - أريحا",
    val clockOffsetX: Float = 0f,
    val clockOffsetY: Float = 0f,
    val clockScale: Float = 1.0f,
    val dateDisplayMode: Int = 0,
    val cardOffsetX: Float = 0f,
    val cardOffsetY: Float = 0f,
    val cardScale: Float = 1.0f,
    val cardBgOpacity: Float = 0.55f,
    val autoRotateTabs: Boolean = true,
    val wallpaperIndex: Int = 0,
    val dockOffsetX: Float = 0f,
    val dockOffsetY: Float = 0f,
    val dockBgOpacity: Float = 0.45f,
    val appCardOpacity: Float = 0.85f,
    val appCornerRadiusPercent: Int = 24,
    val appBaseScale: Float = 1.0f,
    val appFocusScale: Float = 1.12f,
    val appGlowMode: Int = 0,
    val appCustomGlowHex: String = "#38BDF8",
    val showTopWifi: Boolean = true,
    val showTopQuranRadio: Boolean = true,
    val showTopScreensaver: Boolean = true,
    val showTopVpn: Boolean = true,
    val showTopQuranBook: Boolean = true,
    val quranStreamUrl: String = "https://radiojar.com",
    val quranAutoPlayScreensaver: Boolean = false,
    val showBottomBar: Boolean = true,
    val tickerBgOpacity: Float = 0.65f,
    val activeTickerPairs: Set<String> = setOf(
        "USD/ILS", "JOD/ILS", "GOLD", "BRENT", "BTC", "ETH"
    ),
    val ssClockOffsetX: Float = 0f,
    val ssClockOffsetY: Float = 0f,
    val ssClockScale: Float = 1.0f,
    val tasks: List<String> = emptyList(),
    val reminders: List<FamilyReminder> = emptyList()
)

class LauncherPreferences(private val context: Context) {

    companion object {
        val CITY_NAME = stringPreferencesKey("city_name")
        val CLOCK_X = floatPreferencesKey("clock_x")
        val CLOCK_Y = floatPreferencesKey("clock_y")
        val CLOCK_SCALE = floatPreferencesKey("clock_scale")
        val DATE_MODE = intPreferencesKey("date_mode")
        val CARD_X = floatPreferencesKey("card_x")
        val CARD_Y = floatPreferencesKey("card_y")
        val CARD_SCALE = floatPreferencesKey("card_scale")
        val CARD_OPACITY = floatPreferencesKey("card_opacity")
        val AUTO_ROTATE = booleanPreferencesKey("auto_rotate")
        val WALLPAPER_IDX = intPreferencesKey("wallpaper_idx")
        val DOCK_X = floatPreferencesKey("dock_x")
        val DOCK_Y = floatPreferencesKey("dock_y")
        val DOCK_OPACITY = floatPreferencesKey("dock_opacity")
        val APP_OPACITY = floatPreferencesKey("app_opacity")
        val APP_CORNER = intPreferencesKey("app_corner")
        val APP_SCALE = floatPreferencesKey("app_scale")
        val APP_FOCUS_SCALE = floatPreferencesKey("app_focus_scale")
        val GLOW_MODE = intPreferencesKey("glow_mode")
        val CUSTOM_GLOW = stringPreferencesKey("custom_glow")
        val SHOW_WIFI = booleanPreferencesKey("show_wifi")
        val SHOW_QURAN = booleanPreferencesKey("show_quran")
        val SHOW_SS = booleanPreferencesKey("show_ss")
        val SHOW_VPN = booleanPreferencesKey("show_vpn")
        val SHOW_BOOK = booleanPreferencesKey("show_book")
        val QURAN_URL = stringPreferencesKey("quran_url")
        val QURAN_AUTO_SS = booleanPreferencesKey("quran_auto_ss")
        val SHOW_BOTTOM = booleanPreferencesKey("show_bottom")
        val TICKER_OPACITY = floatPreferencesKey("ticker_opacity")
        val TICKER_PAIRS = stringSetPreferencesKey("ticker_pairs")
        val SS_CLOCK_X = floatPreferencesKey("ss_clock_x")
        val SS_CLOCK_Y = floatPreferencesKey("ss_clock_y")
        val SS_CLOCK_SCALE = floatPreferencesKey("ss_clock_scale")
        val TASKS_JSON = stringPreferencesKey("tasks_json")
        val REMINDERS_JSON = stringPreferencesKey("reminders_json")
    }

    val configFlow: Flow<LauncherConfig> = context.dataStore.data.map { p ->
        LauncherConfig(
            cityName = p[CITY_NAME] ?: "الزبيدات - أريحا",
            clockOffsetX = p[CLOCK_X] ?: 0f,
            clockOffsetY = p[CLOCK_Y] ?: 0f,
            clockScale = p[CLOCK_SCALE] ?: 1.0f,
            dateDisplayMode = p[DATE_MODE] ?: 0,
            cardOffsetX = p[CARD_X] ?: 0f,
            cardOffsetY = p[CARD_Y] ?: 0f,
            cardScale = p[CARD_SCALE] ?: 1.0f,
            cardBgOpacity = p[CARD_OPACITY] ?: 0.55f,
            autoRotateTabs = p[AUTO_ROTATE] ?: true,
            wallpaperIndex = p[WALLPAPER_IDX] ?: 0,
            dockOffsetX = p[DOCK_X] ?: 0f,
            dockOffsetY = p[DOCK_Y] ?: 0f,
            dockBgOpacity = p[DOCK_OPACITY] ?: 0.45f,
            appCardOpacity = p[APP_OPACITY] ?: 0.85f,
            appCornerRadiusPercent = p[APP_CORNER] ?: 24,
            appBaseScale = p[APP_SCALE] ?: 1.0f,
            appFocusScale = p[APP_FOCUS_SCALE] ?: 1.12f,
            appGlowMode = p[GLOW_MODE] ?: 0,
            appCustomGlowHex = p[CUSTOM_GLOW] ?: "#38BDF8",
            showTopWifi = p[SHOW_WIFI] ?: true,
            showTopQuranRadio = p[SHOW_QURAN] ?: true,
            showTopScreensaver = p[SHOW_SS] ?: true,
            showTopVpn = p[SHOW_VPN] ?: true,
            showTopQuranBook = p[SHOW_BOOK] ?: true,
            quranStreamUrl = p[QURAN_URL] ?: "https://radiojar.com",
            quranAutoPlayScreensaver = p[QURAN_AUTO_SS] ?: false,
            showBottomBar = p[SHOW_BOTTOM] ?: true,
            tickerBgOpacity = p[TICKER_OPACITY] ?: 0.65f,
            activeTickerPairs = p[TICKER_PAIRS] ?: setOf(
                "USD/ILS", "JOD/ILS", "GOLD", "BRENT", "BTC", "ETH"
            ),
            ssClockOffsetX = p[SS_CLOCK_X] ?: 0f,
            ssClockOffsetY = p[SS_CLOCK_Y] ?: 0f,
            ssClockScale = p[SS_CLOCK_SCALE] ?: 1.0f,
            tasks = parseTasks(p[TASKS_JSON]),
            reminders = parseReminders(p[REMINDERS_JSON])
        )
    }

    suspend fun updateConfig(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    suspend fun saveTasks(tasks: List<String>) {
        val arr = JSONArray()
        tasks.forEach { arr.put(it) }
        context.dataStore.edit { it[TASKS_JSON] = arr.toString() }
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
        context.dataStore.edit { it[REMINDERS_JSON] = arr.toString() }
    }

    private fun parseTasks(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseReminders(json: String?): List<FamilyReminder> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) {
                val o = arr.getJSONObject(it)
                FamilyReminder(
                    o.getString("id"),
                    o.getString("name"),
                    o.getInt("month"),
                    o.getInt("day")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

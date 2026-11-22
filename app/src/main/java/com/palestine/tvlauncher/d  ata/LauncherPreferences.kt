
package com.palestine.tvlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore by preferencesDataStore(name = "palestine_launcher_prefs")

data class FamilyReminder(
    val id: String,
    val name: String,
    val month: Int,
    val day: Int
)

data class LauncherConfig(
    val cityName: String = "الزبيدات - أريحا",
    // Date display: 0 = Both, 1 = Hijri only, 2 = Gregorian only, 3 = Hidden
    val dateDisplayMode: Int = 0,
    // Top bar visibility
    val showTopWifi: Boolean = true,
    val showTopQuranRadio: Boolean = true,
    val showTopMushaf: Boolean = true,
    val showTopHadith: Boolean = true,
    val showTopScreensaver: Boolean = true,
    val showTopVpn: Boolean = false,
    // App Dock customization
    val appGlowMode: Int = 0, // 0 = App Dominant Color, 1 = Custom Color, 2 = Off
    val appCustomGlowHex: String = "#FFD700",
    val appBaseScale: Float = 1.0f,
    val appFocusScale: Float = 1.15f,
    val appCornerRadiusPercent: Int = 20, // 0 to 100
    val dockBgOpacity: Float = 0.35f,
    val appCardOpacity: Float = 0.85f,
    // Ticker & Bottom bar
    val showBottomBar: Boolean = true,
    val tickerBgOpacity: Float = 0.65f,
    val activeTickerPairs: Set<String> = setOf("USD/ILS", "JOD/ILS", "GOLD", "BRENT", "BTC"),
    // Live Editor Offsets & Scales (Main Screen)
    val clockOffsetX: Float = 0f,
    val clockOffsetY: Float = 0f,
    val clockScale: Float = 1.0f,
    val cardOffsetX: Float = 0f,
    val cardOffsetY: Float = 0f,
    val cardScale: Float = 1.0f,
    val cardBgOpacity: Float = 0.45f,
    val dockOffsetX: Float = 0f,
    val dockOffsetY: Float = 0f,
    // Screensaver Editor Offsets
    val ssClockOffsetX: Float = 0f,
    val ssClockOffsetY: Float = 0f,
    val ssClockScale: Float = 1.0f,
    // Quran Radio
    val quranStreamUrl: String = "https://radiojar.com",
    val quranAutoPlayScreensaver: Boolean = true,
    // Tasks & Family Reminders
    val tasks: List<String> = emptyList(),
    val reminders: List<FamilyReminder> = emptyList()
)

class LauncherPreferences(private val context: Context) {

    companion object {
        val CITY_NAME = stringPreferencesKey("city_name")
        val DATE_MODE = intPreferencesKey("date_mode")
        val SHOW_WIFI = booleanPreferencesKey("show_wifi")
        val SHOW_QURAN = booleanPreferencesKey("show_quran")
        val SHOW_MUSHAF = booleanPreferencesKey("show_mushaf")
        val SHOW_HADITH = booleanPreferencesKey("show_hadith")
        val SHOW_SS = booleanPreferencesKey("show_ss")
        val SHOW_VPN = booleanPreferencesKey("show_vpn")

        val GLOW_MODE = intPreferencesKey("glow_mode")
        val GLOW_COLOR = stringPreferencesKey("glow_color")
        val APP_SCALE = floatPreferencesKey("app_scale")
        val APP_FOCUS_SCALE = floatPreferencesKey("app_focus_scale")
        val APP_CORNER = intPreferencesKey("app_corner")
        val DOCK_OPACITY = floatPreferencesKey("dock_opacity")
        val APP_CARD_OPACITY = floatPreferencesKey("app_card_opacity")

        val SHOW_BOTTOM = booleanPreferencesKey("show_bottom")
        val TICKER_OPACITY = floatPreferencesKey("ticker_opacity")
        val TICKER_PAIRS = stringSetPreferencesKey("ticker_pairs")

        val CLOCK_X = floatPreferencesKey("clock_x")
        val CLOCK_Y = floatPreferencesKey("clock_y")
        val CLOCK_SCALE = floatPreferencesKey("clock_scale")
        val CARD_X = floatPreferencesKey("card_x")
        val CARD_Y = floatPreferencesKey("card_y")
        val CARD_SCALE = floatPreferencesKey("card_scale")
        val CARD_OPACITY = floatPreferencesKey("card_opacity")
        val DOCK_X = floatPreferencesKey("dock_x")
        val DOCK_Y = floatPreferencesKey("dock_y")

        val SS_CLOCK_X = floatPreferencesKey("ss_clock_x")
        val SS_CLOCK_Y = floatPreferencesKey("ss_clock_y")
        val SS_CLOCK_SCALE = floatPreferencesKey("ss_clock_scale")

        val QURAN_URL = stringPreferencesKey("quran_url")
        val QURAN_AUTO_SS = booleanPreferencesKey("quran_auto_ss")

        val TASKS_JSON = stringPreferencesKey("tasks_json")
        val REMINDERS_JSON = stringPreferencesKey("reminders_json")
    }

    val configFlow: Flow<LauncherConfig> = context.dataStore.data.map { prefs ->
        LauncherConfig(
            cityName = prefs[CITY_NAME] ?: "الزبيدات - أريحا",
            dateDisplayMode = prefs[DATE_MODE] ?: 0,
            showTopWifi = prefs[SHOW_WIFI] ?: true,
            showTopQuranRadio = prefs[SHOW_QURAN] ?: true,
            showTopMushaf = prefs[SHOW_MUSHAF] ?: true,
            showTopHadith = prefs[SHOW_HADITH] ?: true,
            showTopScreensaver = prefs[SHOW_SS] ?: true,
            showTopVpn = prefs[SHOW_VPN] ?: false,
            appGlowMode = prefs[GLOW_MODE] ?: 0,
            appCustomGlowHex = prefs[GLOW_COLOR] ?: "#FFD700",
            appBaseScale = prefs[APP_SCALE] ?: 1.0f,
            appFocusScale = prefs[APP_FOCUS_SCALE] ?: 1.15f,
            appCornerRadiusPercent = prefs[APP_CORNER] ?: 20,
            dockBgOpacity = prefs[DOCK_OPACITY] ?: 0.35f,
            appCardOpacity = prefs[APP_CARD_OPACITY] ?: 0.85f,
            showBottomBar = prefs[SHOW_BOTTOM] ?: true,
            tickerBgOpacity = prefs[TICKER_OPACITY] ?: 0.65f,
            activeTickerPairs = prefs[TICKER_PAIRS] ?: setOf("USD/ILS", "JOD/ILS", "GOLD", "BRENT", "BTC"),
            clockOffsetX = prefs[CLOCK_X] ?: 0f,
            clockOffsetY = prefs[CLOCK_Y] ?: 0f,
            clockScale = prefs[CLOCK_SCALE] ?: 1.0f,
            cardOffsetX = prefs[CARD_X] ?: 0f,
            cardOffsetY = prefs[CARD_Y] ?: 0f,
            cardScale = prefs[CARD_SCALE] ?: 1.0f,
            cardBgOpacity = prefs[CARD_OPACITY] ?: 0.45f,
            dockOffsetX = prefs[DOCK_X] ?: 0f,
            dockOffsetY = prefs[DOCK_Y] ?: 0f,
            ssClockOffsetX = prefs[SS_CLOCK_X] ?: 0f,
            ssClockOffsetY = prefs[SS_CLOCK_Y] ?: 0f,
            ssClockScale = prefs[SS_CLOCK_SCALE] ?: 1.0f,
            quranStreamUrl = prefs[QURAN_URL] ?: "https://radiojar.com",
            quranAutoPlayScreensaver = prefs[QURAN_AUTO_SS] ?: true,
            tasks = parseTasks(prefs[TASKS_JSON]),
            reminders = parseReminders(prefs[REMINDERS_JSON])
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

    suspend fun saveReminders(reminders: List<FamilyReminder>) {
        val arr = JSONArray()
        reminders.forEach {
            val obj = JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("month", it.month)
                put("day", it.day)
            }
            arr.put(obj)
        }
        context.dataStore.edit { it[REMINDERS_JSON] = arr.toString() }
    }

    private fun parseTasks(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseReminders(json: String?): List<FamilyReminder> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) {
                val obj = arr.getJSONObject(it)
                FamilyReminder(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    month = obj.getInt("month"),
                    day = obj.getInt("day")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

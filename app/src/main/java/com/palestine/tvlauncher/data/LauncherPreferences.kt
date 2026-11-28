package com.palestine.tvlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "palestine_launcher_prefs")

data class TabItem(val key: String, val title: String)

val cardTabs = listOf(
    TabItem("prayer", "أوقات الصلاة"),
    TabItem("weather", "الطقس"),
    TabItem("matches", "المباريات"),
    TabItem("date", "التاريخ"),
    TabItem("verse", "آية اليوم"),
    TabItem("hadith", "حديث اليوم"),
    TabItem("adhkar", "الأذكار"),
    TabItem("news", "الأخبار"),
    TabItem("prices", "العملات والذهب"),
    TabItem("occasions", "المناسبات"),
    TabItem("device", "حالة الجهاز"),
    TabItem("tasks", "المهام"),
    TabItem("reminders", "التذكيرات"),
    TabItem("all", "الكل")
)

private fun tabOrderFromRaw(raw: String?): List<String> {
    val valid = cardTabs.map { it.key }.toSet()
    return if (raw?.isBlank() == true) cardTabs.map { it.key }
    else (raw ?: "").split("::").filter { it.isNotBlank() && it in valid }
}

private fun appCustomNamesFromRaw(raw: String?): Map<String, String> {
    return if (raw?.isBlank() == true) emptyMap()
    else (raw ?: "").split("||").filter { it.isNotBlank() }.mapNotNull { item ->
        val parts = item.split("::", limit = 2)
        if (parts.size == 2) parts[0] to parts[1] else null
    }.toMap()
}

private fun customEventsFromRaw(raw: String?): List<CustomEvent> {
    return if (raw?.isBlank() == true) emptyList()
    else (raw ?: "").split("||").filter { it.isNotBlank() }.mapNotNull { item ->
        val parts: List<String> = item.split("::").toList()
        if (parts.size >= 4) {
            CustomEvent(
                id = parts[0],
                name = parts[1],
                hijriMonth = parts[2].toIntOrNull() ?: 1,
                hijriDay = parts[3].toIntOrNull() ?: 1,
                isRecurring = parts.getOrNull(4)?.toBoolean() ?: true
            )
        } else null
    }
}

private fun birthdaysFromRaw(raw: String?): List<Birthday> {
    return if (raw?.isBlank() == true) emptyList()
    else (raw ?: "").split("||").filter { it.isNotBlank() }.mapNotNull { item ->
        val parts = item.split("::")
        if (parts.size >= 3) {
            Birthday(
                id = parts[0],
                name = parts[1],
                month = parts[2].toIntOrNull() ?: 1,
                day = parts[3].toIntOrNull() ?: 1
            )
        } else null
    }
}

data class LauncherConfig(
    val customCardAllTabs: Boolean = false,
    val cityName: String = "أريحا (الزبيدات)",
    val cityLat: Double = 32.0450,
    val cityLon: Double = 35.4580,
    val cityTimeZone: String = "Asia/Hebron",
    val clockStyle: Int = 0,
    val dateDisplayMode: Int = 0,
    val quranPlayMode: Int = 0,
    val quranStreamUrl: String = "https://stream.radiojar.com/8s5u5tpdtwzuv",
    val lastSelectedQuranRadioUrl: String = "",
    val quranScheduleStartHour: Int = 6,
    val quranScheduleEndHour: Int = 9,
    val wallpaperIndex: Int = 0,
    val wallpaperIntervalMinutes: Int = 5,
    val customWallpaperUri: String = "",
    val cardBgOpacity: Float = 0.72f,
    val dockBgOpacity: Float = 0.55f,
    val tickerBgOpacity: Float = 0.65f,
    val appBaseScale: Float = 1.0f,
    val appCornerRadiusPercent: Int = 22,

    // Favorite app bar customization
    val appIconOpacity: Float = 1.0f,
    val appIconSize: Int = 1,
    val appIconShape: Int = 0,
    val appFocusGlowIntensity: Float = 0.6f,
    val appFocusGlowColor: Int = 0xFF38BDF8.toInt(),
    val appFocusScaleFactor: Float = 1.12f,
    val showTopWifi: Boolean = true,
    val showTopQuranRadio: Boolean = true,
    val showTopQuranBook: Boolean = true,
    val showTopScreensaver: Boolean = true,
    val showTopVpn: Boolean = true,
    val showBottomBar: Boolean = true,
    val showDeviceGrid: Boolean = true,
    val appCustomNames: Map<String, String> = emptyMap(),
    val favoriteApps: Set<String> = emptySet(),
    val appOrder: List<String> = emptyList(),
    val autoRotateTabs: Boolean = false,
    val cardFlipMode: Int = 0,
    val clockOffsetX: Float = 0f,
    val clockOffsetY: Float = 0f,
    val clockScale: Float = 1.0f,
    val cardOffsetX: Float = 0f,
    val cardOffsetY: Float = 0f,
    val cardScale: Float = 1.0f,
    val dockOffsetX: Float = 0f,
    val dockOffsetY: Float = 0f,
    val ssClockOffsetX: Float = 0f,
    val ssClockOffsetY: Float = 0f,
    val ssClockScale: Float = 1.0f,
    val tasks: List<String> = emptyList(),
    val reminders: List<FamilyReminder> = emptyList(),

    // Adhan settings
    val adhanEnabled: Boolean = true,
    val adhanVolume: Int = 70,
    val adhanMuadhinId: String = "makkah_alimla",
    val adhanLengthMode: Int = 0,
    val adhanReminderMinutes: Int = 5,
    val adhanPlayFajrOnly: Boolean = false,

    // Ticker bar customization
    val tickerBarPosition: Int = 0,
    val tickerBarHeight: Int = 56,
    val tickerBarFontSize: Int = 13,
    val tickerBarOpacity: Float = 0.65f,
    val tickerShowNextPrayer: Boolean = true,
    val tickerShowRamadan: Boolean = true,
    val tickerShowBirthday: Boolean = true,
    val tickerShowRamadanCountdown: Boolean = true,
    val tickerShowBirthdayCountdown: Boolean = true,
    val tickerShowPrices: Boolean = true,
    val tickerShowMatches: Boolean = true,
    val tickerElementOrder: List<String> = listOf("prayer", "ramadan", "birthday", "prices", "matches"),
    val activeTickerPairs: Set<String> = setOf("USD/ILS", "JOD/ILS", "EUR/ILS", "GOLD"),

    // Focus effects
    val focusScaleEnabled: Boolean = true,
    val focusScaleFactor: Float = 1.08f,
    val focusGlowEnabled: Boolean = true,
    val focusGlowColor: Int = 0xFF38BDF8.toInt(),
    val focusBorderEnabled: Boolean = true,
    val focusBorderWidth: Float = 2.5f,
    val focusBorderColor: Int = 0xFFFFFFFF.toInt(),

    // Tab auto-rotate interval in minutes
    val tabAutoRotateIntervalMinutes: Int = 1,

    // Background live wallpapers
    val liveWallpaperEnabled: Boolean = false,
    val liveWallpaperIntervalMinutes: Int = 10,
    val liveWallpaperSource: Int = 0,
    val liveWallpaperType: String = "none",
    val liveWallpaperCategory: String = "الكل المتنوع",

    // Bottom Events Bar customization
    val bottomBarEnabled: Boolean = true,
    val bottomBarHeight: Int = 56,
    val bottomBarFontSize: Int = 13,
    val bottomBarOpacity: Float = 0.65f,
    val bottomBarShowEventCounter: Boolean = true,
    val bottomBarShowBirthdayCounter: Boolean = true,
    val bottomBarShowReligiousEvents: Boolean = true,
    val bottomBarShowNationalEvents: Boolean = true,
    val bottomBarShowNextPrayer: Boolean = true,
    val customEvents: List<CustomEvent> = emptyList(),
    val birthdays: List<Birthday> = emptyList(),

    // Favorite team
    val favoriteTeamId: String = "",
    val favoriteTeamName: String = "",
    val favoriteTeamLeague: String = "",
    val favoriteTeams: List<FavoriteTeam> = emptyList(),
    val disabledNewsSources: Set<String> = emptySet(),

    // إظهار وإخفاء وترتيب التبويبات بشكل مستقل للبطاقة الرئيسية ونافذة التوسعة
    val cardTabOrder: List<String> = cardTabs.map { it.key },
    val cardTabVisible: Set<String> = cardTabs.map { it.key }.toSet(),
    val expansionTabOrder: List<String> = cardTabs.map { it.key },
    val expansionTabVisible: Set<String> = cardTabs.map { it.key }.toSet(),
)

data class FamilyReminder(
    val id: String,
    val name: String,
    val month: Int,
    val day: Int
)

data class FavoriteTeam(
    val id: String,
    val name: String,
    val league: String
)

data class CustomEvent(
    val id: String,
    val name: String,
    val hijriMonth: Int,
    val hijriDay: Int,
    val isRecurring: Boolean = true
)

data class Birthday(
    val id: String,
    val name: String,
    val month: Int,
    val day: Int
)

class LauncherPreferences(private val context: Context) {

    companion object {
        val CUSTOM_CARD_ALL_TABS = booleanPreferencesKey("custom_card_all_tabs")
        val CITY_NAME = stringPreferencesKey("city_name")
        val CITY_LAT = doublePreferencesKey("city_lat")
        val CITY_LON = doublePreferencesKey("city_lon")
        val CITY_TZ = stringPreferencesKey("city_tz")
        val CLOCK_STYLE = intPreferencesKey("clock_style")
        val DATE_MODE = intPreferencesKey("date_mode")
        val QURAN_PLAY_MODE = intPreferencesKey("quran_play_mode")
        val QURAN_URL = stringPreferencesKey("quran_url")
        val LAST_QURAN_RADIO_URL = stringPreferencesKey("last_quran_radio_url")
        val QURAN_SCHED_START = intPreferencesKey("quran_sched_start")
        val QURAN_SCHED_END = intPreferencesKey("quran_sched_end")
        val WALLPAPER_IDX = intPreferencesKey("wallpaper_idx")
        val WALLPAPER_INTERVAL = intPreferencesKey("wallpaper_interval")
        val CUSTOM_WALLPAPER_URI = stringPreferencesKey("custom_wallpaper_uri")
        val CARD_OPACITY = floatPreferencesKey("card_opacity")
        val DOCK_OPACITY = floatPreferencesKey("dock_opacity")
        val TICKER_OPACITY = floatPreferencesKey("ticker_opacity")
        val APP_SCALE = floatPreferencesKey("app_scale")
        val APP_CORNER = intPreferencesKey("app_corner")

        // Favorite app bar
        val APP_ICON_OPACITY = floatPreferencesKey("app_icon_opacity")
        val APP_ICON_SIZE = intPreferencesKey("app_icon_size")
        val APP_ICON_SHAPE = intPreferencesKey("app_icon_shape")
        val APP_FOCUS_GLOW_INTENSITY = floatPreferencesKey("app_focus_glow_intensity")
        val APP_FOCUS_GLOW_COLOR = intPreferencesKey("app_focus_glow_color")
        val APP_FOCUS_SCALE_FACTOR = floatPreferencesKey("app_focus_scale_factor")
        val SHOW_WIFI = booleanPreferencesKey("show_wifi")
        val SHOW_QURAN = booleanPreferencesKey("show_quran")
        val SHOW_BOOK = booleanPreferencesKey("show_book")
        val SHOW_SS = booleanPreferencesKey("show_ss")
        val SHOW_VPN = booleanPreferencesKey("show_vpn")
        val SHOW_BOTTOM = booleanPreferencesKey("show_bottom")
        val SHOW_DEVICE_GRID = booleanPreferencesKey("show_device_grid")
        val APP_CUSTOM_NAMES = stringPreferencesKey("app_custom_names")
        val FAVORITE_APPS_RAW = stringPreferencesKey("favorite_apps_raw")
        val APP_ORDER_RAW = stringPreferencesKey("app_order_raw")
        val AUTO_ROTATE = booleanPreferencesKey("auto_rotate")
        val CARD_FLIP_MODE = intPreferencesKey("card_flip_mode")
        val TICKER_PAIRS = stringSetPreferencesKey("ticker_pairs")
        val CLOCK_X = floatPreferencesKey("clock_x")
        val CLOCK_Y = floatPreferencesKey("clock_y")
        val CLOCK_SCALE = floatPreferencesKey("clock_scale")
        val CARD_X = floatPreferencesKey("card_x")
        val CARD_Y = floatPreferencesKey("card_y")
        val CARD_SCALE = floatPreferencesKey("card_scale")
        val DOCK_X = floatPreferencesKey("dock_x")
        val DOCK_Y = floatPreferencesKey("dock_y")
        val SS_CLOCK_X = floatPreferencesKey("ss_clock_x")
        val SS_CLOCK_Y = floatPreferencesKey("ss_clock_y")
        val SS_CLOCK_SCALE = floatPreferencesKey("ss_clock_scale")
        val TASKS_RAW = stringPreferencesKey("tasks_raw")
        val REMINDERS_RAW = stringPreferencesKey("reminders_raw")

        // Adhan
        val ADHAN_ENABLED = booleanPreferencesKey("adhan_enabled")
        val ADHAN_VOLUME = intPreferencesKey("adhan_volume")
        val ADHAN_MUADHIN_ID = stringPreferencesKey("adhan_muadhin_id")
        val ADHAN_LENGTH_MODE = intPreferencesKey("adhan_length_mode")
        val ADHAN_REMINDER_MINUTES = intPreferencesKey("adhan_reminder_minutes")
        val ADHAN_FAJR_ONLY = booleanPreferencesKey("adhan_fajr_only")

        // Ticker bar
        val TICKER_BAR_POSITION = intPreferencesKey("ticker_bar_position")
        val TICKER_BAR_HEIGHT = intPreferencesKey("ticker_bar_height")
        val TICKER_BAR_FONT_SIZE = intPreferencesKey("ticker_bar_font_size")
        val TICKER_BAR_OPACITY = floatPreferencesKey("ticker_bar_opacity")
        val TICKER_SHOW_NEXT_PRAYER = booleanPreferencesKey("ticker_show_next_prayer")
        val TICKER_SHOW_RAMADAN2 = booleanPreferencesKey("ticker_show_ramadan2")
        val TICKER_SHOW_BIRTHDAY2 = booleanPreferencesKey("ticker_show_birthday2")
        val TICKER_SHOW_RAMADAN = booleanPreferencesKey("ticker_show_ramadan")
        val TICKER_SHOW_BIRTHDAY = booleanPreferencesKey("ticker_show_birthday")
        val TICKER_SHOW_PRICES = booleanPreferencesKey("ticker_show_prices")
        val TICKER_SHOW_MATCHES = booleanPreferencesKey("ticker_show_matches")
        val TICKER_ELEMENT_ORDER = stringPreferencesKey("ticker_element_order")

        // Focus effects
        val FOCUS_SCALE_ENABLED = booleanPreferencesKey("focus_scale_enabled")
        val FOCUS_SCALE_FACTOR = floatPreferencesKey("focus_scale_factor")
        val FOCUS_GLOW_ENABLED = booleanPreferencesKey("focus_glow_enabled")
        val FOCUS_GLOW_COLOR = intPreferencesKey("focus_glow_color")
        val FOCUS_BORDER_ENABLED = booleanPreferencesKey("focus_border_enabled")
        val FOCUS_BORDER_WIDTH = floatPreferencesKey("focus_border_width")
        val FOCUS_BORDER_COLOR = intPreferencesKey("focus_border_color")

        // Tab rotate
        val TAB_ROTATE_INTERVAL_MINUTES = intPreferencesKey("tab_rotate_interval_minutes")

        // Live wallpaper
        val LIVE_WALLPAPER_ENABLED = booleanPreferencesKey("live_wallpaper_enabled")
        val LIVE_WALLPAPER_INTERVAL = intPreferencesKey("live_wallpaper_interval")
        val LIVE_WALLPAPER_SOURCE = intPreferencesKey("live_wallpaper_source")
        val LIVE_WALLPAPER_TYPE = stringPreferencesKey("live_wallpaper_type")
        val LIVE_WALLPAPER_CATEGORY = stringPreferencesKey("live_wallpaper_category")

        // Bottom Events Bar
        val BOTTOM_BAR_ENABLED = booleanPreferencesKey("bottom_bar_enabled")
        val BOTTOM_BAR_HEIGHT = intPreferencesKey("bottom_bar_height")
        val BOTTOM_BAR_FONT_SIZE = intPreferencesKey("bottom_bar_font_size")
        val BOTTOM_BAR_OPACITY = floatPreferencesKey("bottom_bar_opacity")
        val BOTTOM_BAR_SHOW_EVENT_COUNTER = booleanPreferencesKey("bottom_bar_show_event_counter")
        val BOTTOM_BAR_SHOW_BIRTHDAY_COUNTER = booleanPreferencesKey("bottom_bar_show_birthday_counter")
        val BOTTOM_BAR_SHOW_RELIGIOUS_EVENTS = booleanPreferencesKey("bottom_bar_show_religious_events")
        val BOTTOM_BAR_SHOW_NATIONAL_EVENTS = booleanPreferencesKey("bottom_bar_show_national_events")
        val BOTTOM_BAR_SHOW_NEXT_PRAYER = booleanPreferencesKey("bottom_bar_show_next_prayer")
        val CUSTOM_EVENTS_RAW = stringPreferencesKey("custom_events_raw")
        val BIRTHDAYS_RAW = stringPreferencesKey("birthdays_raw")

        // Favorite team
        val FAVORITE_TEAM_ID = stringPreferencesKey("favorite_team_id")
        val FAVORITE_TEAM_NAME = stringPreferencesKey("favorite_team_name")
        val FAVORITE_TEAM_LEAGUE = stringPreferencesKey("favorite_team_league")
        val DISABLED_NEWS_SOURCES = stringSetPreferencesKey("disabled_news_sources")

        // Tab visibility & order (card vs expansion)
        val CARD_TAB_ORDER = stringPreferencesKey("card_tab_order")
        val CARD_TAB_VISIBLE = stringSetPreferencesKey("card_tab_visible")
        val EXPANSION_TAB_ORDER = stringPreferencesKey("expansion_tab_order")
        val EXPANSION_TAB_VISIBLE = stringSetPreferencesKey("expansion_tab_visible")
    }

    val configFlow: Flow<LauncherConfig> = context.dataStore.data.map { p ->
        val rawTasks = p[TASKS_RAW] ?: ""
        val taskList = if (rawTasks.isBlank()) emptyList() else rawTasks.split("||").filter { it.isNotBlank() }

        val rawRem = p[REMINDERS_RAW] ?: ""
        val remList = if (rawRem.isBlank()) emptyList() else rawRem.split("||").mapNotNull { item ->
            val parts = item.split("::")
            if (parts.size == 4) {
                FamilyReminder(parts[0], parts[1], parts[2].toIntOrNull() ?: 1, parts[3].toIntOrNull() ?: 1)
            } else null
        }

        LauncherConfig(
            cityName = p[CITY_NAME] ?: "أريحا (الزبيدات)",
            cityLat = p[CITY_LAT] ?: 32.0450,
            cityLon = p[CITY_LON] ?: 35.4580,
            cityTimeZone = p[CITY_TZ] ?: "Asia/Hebron",
            clockStyle = p[CLOCK_STYLE] ?: 0,
            dateDisplayMode = p[DATE_MODE] ?: 0,
            quranPlayMode = p[QURAN_PLAY_MODE] ?: 0,
            quranStreamUrl = p[QURAN_URL] ?: "https://stream.radiojar.com/8s5u5tpdtwzuv",
            lastSelectedQuranRadioUrl = p[LAST_QURAN_RADIO_URL] ?: "",
            quranScheduleStartHour = p[QURAN_SCHED_START] ?: 6,
            quranScheduleEndHour = p[QURAN_SCHED_END] ?: 9,
            wallpaperIndex = p[WALLPAPER_IDX] ?: 0,
            wallpaperIntervalMinutes = p[WALLPAPER_INTERVAL] ?: 5,
            customWallpaperUri = p[CUSTOM_WALLPAPER_URI] ?: "",
            cardBgOpacity = p[CARD_OPACITY] ?: 0.72f,
            dockBgOpacity = p[DOCK_OPACITY] ?: 0.55f,
            tickerBgOpacity = p[TICKER_OPACITY] ?: 0.65f,
            appBaseScale = p[APP_SCALE] ?: 1.0f,
            appCornerRadiusPercent = p[APP_CORNER] ?: 22,
            appIconOpacity = p[APP_ICON_OPACITY] ?: 1.0f,
            appIconSize = p[APP_ICON_SIZE] ?: 1,
            appIconShape = p[APP_ICON_SHAPE] ?: 0,
            appFocusGlowIntensity = p[APP_FOCUS_GLOW_INTENSITY] ?: 0.6f,
            appFocusGlowColor = p[APP_FOCUS_GLOW_COLOR] ?: 0xFF38BDF8.toInt(),
            appFocusScaleFactor = p[APP_FOCUS_SCALE_FACTOR] ?: 1.12f,
            showTopWifi = p[SHOW_WIFI] ?: true,
            showTopQuranRadio = p[SHOW_QURAN] ?: true,
            showTopQuranBook = p[SHOW_BOOK] ?: true,
            showTopScreensaver = p[SHOW_SS] ?: true,
            showTopVpn = p[SHOW_VPN] ?: true,
            showBottomBar = p[SHOW_BOTTOM] ?: true,
            showDeviceGrid = p[SHOW_DEVICE_GRID] ?: true,
            appCustomNames = appCustomNamesFromRaw(p[APP_CUSTOM_NAMES]),
            favoriteApps = p[FAVORITE_APPS_RAW]?.split("||")?.filter { it.isNotBlank() }?.toSet() ?: emptySet(),
            appOrder = p[APP_ORDER_RAW]?.split("||")?.filter { it.isNotBlank() } ?: emptyList(),
            autoRotateTabs = p[AUTO_ROTATE] ?: false,
            cardFlipMode = p[CARD_FLIP_MODE] ?: 0,
            activeTickerPairs = p[TICKER_PAIRS] ?: setOf("USD/ILS", "JOD/ILS", "EUR/ILS", "GOLD"),
            clockOffsetX = p[CLOCK_X] ?: 0f,
            clockOffsetY = p[CLOCK_Y] ?: 0f,
            clockScale = p[CLOCK_SCALE] ?: 1.0f,
            cardOffsetX = p[CARD_X] ?: 0f,
            cardOffsetY = p[CARD_Y] ?: 0f,
            cardScale = p[CARD_SCALE] ?: 1.0f,
            dockOffsetX = p[DOCK_X] ?: 0f,
            dockOffsetY = p[DOCK_Y] ?: 0f,
            ssClockOffsetX = p[SS_CLOCK_X] ?: 0f,
            ssClockOffsetY = p[SS_CLOCK_Y] ?: 0f,
            ssClockScale = p[SS_CLOCK_SCALE] ?: 1.0f,
            tasks = taskList,
            reminders = remList,

            customCardAllTabs = p[CUSTOM_CARD_ALL_TABS] ?: false,

            adhanEnabled = p[ADHAN_ENABLED] ?: true,
            adhanVolume = p[ADHAN_VOLUME] ?: 70,
            adhanMuadhinId = p[ADHAN_MUADHIN_ID] ?: "makkah_alimla",
            adhanLengthMode = p[ADHAN_LENGTH_MODE] ?: 0,
            adhanReminderMinutes = p[ADHAN_REMINDER_MINUTES] ?: 5,
            adhanPlayFajrOnly = p[ADHAN_FAJR_ONLY] ?: false,

            tickerBarPosition = p[TICKER_BAR_POSITION] ?: 0,
            tickerBarHeight = p[TICKER_BAR_HEIGHT] ?: 56,
            tickerBarFontSize = p[TICKER_BAR_FONT_SIZE] ?: 13,
            tickerBarOpacity = p[TICKER_BAR_OPACITY] ?: 0.65f,
            tickerShowNextPrayer = p[TICKER_SHOW_NEXT_PRAYER] ?: true,
            tickerShowRamadan = p[TICKER_SHOW_RAMADAN] ?: true,
            tickerShowBirthday = p[TICKER_SHOW_BIRTHDAY] ?: true,
            tickerShowPrices = p[TICKER_SHOW_PRICES] ?: true,
            tickerShowMatches = p[TICKER_SHOW_MATCHES] ?: true,
            tickerElementOrder = if (p[TICKER_ELEMENT_ORDER]?.isBlank() == true) listOf("prayer", "ramadan", "birthday", "prices", "matches") else p[TICKER_ELEMENT_ORDER]?.split("::")?.filter { it.isNotBlank() }?.take(5) ?: listOf("prayer", "ramadan", "birthday", "prices", "matches"),

            focusScaleEnabled = p[FOCUS_SCALE_ENABLED] ?: true,
            focusScaleFactor = p[FOCUS_SCALE_FACTOR] ?: 1.08f,
            focusGlowEnabled = p[FOCUS_GLOW_ENABLED] ?: true,
            focusGlowColor = p[FOCUS_GLOW_COLOR] ?: 0xFF38BDF8.toInt(),
            focusBorderEnabled = p[FOCUS_BORDER_ENABLED] ?: true,
            focusBorderWidth = p[FOCUS_BORDER_WIDTH] ?: 2.5f,
            focusBorderColor = p[FOCUS_BORDER_COLOR] ?: 0xFFFFFFFF.toInt(),

            tabAutoRotateIntervalMinutes = p[TAB_ROTATE_INTERVAL_MINUTES] ?: 1,

            liveWallpaperEnabled = p[LIVE_WALLPAPER_ENABLED] ?: false,
            liveWallpaperIntervalMinutes = p[LIVE_WALLPAPER_INTERVAL] ?: 10,
            liveWallpaperSource = p[LIVE_WALLPAPER_SOURCE] ?: 0,
            liveWallpaperType = p[LIVE_WALLPAPER_TYPE] ?: "none",
            liveWallpaperCategory = p[LIVE_WALLPAPER_CATEGORY] ?: "الكل المتنوع",

            bottomBarEnabled = p[BOTTOM_BAR_ENABLED] ?: true,
            bottomBarHeight = p[BOTTOM_BAR_HEIGHT] ?: 56,
            bottomBarFontSize = p[BOTTOM_BAR_FONT_SIZE] ?: 13,
            bottomBarOpacity = p[BOTTOM_BAR_OPACITY] ?: 0.65f,
            bottomBarShowEventCounter = p[BOTTOM_BAR_SHOW_EVENT_COUNTER] ?: true,
            bottomBarShowBirthdayCounter = p[BOTTOM_BAR_SHOW_BIRTHDAY_COUNTER] ?: true,
            bottomBarShowReligiousEvents = p[BOTTOM_BAR_SHOW_RELIGIOUS_EVENTS] ?: true,
            bottomBarShowNationalEvents = p[BOTTOM_BAR_SHOW_NATIONAL_EVENTS] ?: true,
            bottomBarShowNextPrayer = p[BOTTOM_BAR_SHOW_NEXT_PRAYER] ?: true,
            customEvents = customEventsFromRaw(p[CUSTOM_EVENTS_RAW]),
            birthdays = birthdaysFromRaw(p[BIRTHDAYS_RAW]),

            favoriteTeamId = p[FAVORITE_TEAM_ID] ?: "",
            favoriteTeamName = p[FAVORITE_TEAM_NAME] ?: "",
            favoriteTeamLeague = p[FAVORITE_TEAM_LEAGUE] ?: "",
            favoriteTeams = emptyList(),
            disabledNewsSources = p[DISABLED_NEWS_SOURCES] ?: emptySet(),

            cardTabOrder = tabOrderFromRaw(p[CARD_TAB_ORDER]),
            cardTabVisible = p[CARD_TAB_VISIBLE] ?: cardTabs.map { it.key }.toSet(),
            expansionTabOrder = tabOrderFromRaw(p[EXPANSION_TAB_ORDER]),
            expansionTabVisible = p[EXPANSION_TAB_VISIBLE] ?: cardTabs.map { it.key }.toSet(),
        )
    }

    suspend fun updateConfig(transform: (MutablePreferences) -> Unit) {
        context.dataStore.edit(transform)
    }

    suspend fun setLocation(city: PalestinianCity) {
        context.dataStore.edit {
            it[CITY_NAME] = city.name
            it[CITY_LAT] = city.lat
            it[CITY_LON] = city.lon
            it[CITY_TZ] = city.timeZoneId
        }
    }

    suspend fun saveTasks(tasks: List<String>) {
        context.dataStore.edit { it[TASKS_RAW] = tasks.joinToString("||") }
    }

    suspend fun saveReminders(reminders: List<FamilyReminder>) {
        context.dataStore.edit {
            it[REMINDERS_RAW] = reminders.joinToString("||") { r -> "${r.id}::${r.name}::${r.month}::${r.day}" }
        }
    }

    suspend fun saveLastQuranRadioUrl(url: String) {
        context.dataStore.edit { it[LAST_QURAN_RADIO_URL] = url }
    }

    suspend fun toggleNewsSource(sourceId: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[DISABLED_NEWS_SOURCES] ?: emptySet()
            prefs[DISABLED_NEWS_SOURCES] = if (enabled) {
                current - sourceId
            } else {
                current + sourceId
            }
        }
    }

    suspend fun saveLiveWallpaperCategory(category: String) {
        context.dataStore.edit { it[LIVE_WALLPAPER_CATEGORY] = category }
    }

    suspend fun saveAppCustomNames(names: Map<String, String>) {
        context.dataStore.edit { it[APP_CUSTOM_NAMES] = "" }
    }

    suspend fun toggleFavoriteApp(packageName: String, add: Boolean) {
        context.dataStore.edit { prefs ->
            val current = (prefs[FAVORITE_APPS_RAW] ?: "").split("||").filter { it.isNotBlank() }.toMutableList()
            if (add) current += packageName else current -= packageName
            prefs[FAVORITE_APPS_RAW] = current.joinToString("||")
        }
    }

    suspend fun saveAppOrder(order: List<String>) {
        context.dataStore.edit { it[APP_ORDER_RAW] = order.joinToString("||") }
    }

    suspend fun saveCustomEvents(events: List<CustomEvent>) {
        context.dataStore.edit {
            it[CUSTOM_EVENTS_RAW] = events.joinToString("||") { e -> "${e.id}::${e.name}::${e.hijriMonth}::${e.hijriDay}::${e.isRecurring}" }
        }
    }

    suspend fun saveBirthdays(birthdays: List<Birthday>) {
        context.dataStore.edit {
            it[BIRTHDAYS_RAW] = birthdays.joinToString("||") { b -> "${b.id}::${b.name}::${b.month}::${b.day}" }
        }
    }
}

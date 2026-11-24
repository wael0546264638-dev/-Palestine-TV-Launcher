package com.palestine.tvlauncher.ui

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.palestine.tvlauncher.data.*
import com.palestine.tvlauncher.network.GitHubUpdateInfo
import com.palestine.tvlauncher.network.LiveFeedState
import com.palestine.tvlauncher.network.LiveRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

private const val QURAN_DEFAULT_STREAM = "https://stream.radiojar.com/8s5u5tpdtwzuv"

private fun toLatinDigits(input: String): String {
    val arabic = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val sb = StringBuilder()
    for (c in input) {
        val idx = arabic.indexOf(c)
        if (idx >= 0) sb.append(idx) else sb.append(c)
    }
    return sb.toString()
}

private fun isAnyPrayerAtTime(nowMins: Int, prayers: List<Pair<String, String>>): Boolean {
    return prayers.any { (_, timeStr) ->
        val parts = timeStr.split(":")
        parts.size >= 2 && parts[0].toIntOrNull() == null == false && parts[1].toIntOrNull() == null == false
            && parts[0].toIntOrNull()!! * 60 + parts[1].toIntOrNull()!! == nowMins
    }
}

@Composable
fun ArabicClockDisplay(timeZoneId: String, clockStyle: Int) {
    var timeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            timeMillis = System.currentTimeMillis()
            delay(1000L - (timeMillis % 1000L))
        }
    }

    val tz = remember(timeZoneId) { TimeZone.getTimeZone(timeZoneId) }
    val cal = remember(timeMillis, tz) {
        Calendar.getInstance(tz).apply { this.timeInMillis = timeMillis }
    }

    val hhmm = remember(timeMillis / 60000L, tz) {
        val sdf = SimpleDateFormat("hh:mm", Locale.US).apply { timeZone = tz }
        PalestineData.toArabicDigits(sdf.format(Date(timeMillis)))
    }
    val ss = remember(timeMillis / 1000L, tz) {
        val sdf = SimpleDateFormat("ss", Locale.US).apply { timeZone = tz }
        PalestineData.toArabicDigits(sdf.format(Date(timeMillis)))
    }
    val amPm = remember(cal.get(Calendar.AM_PM)) {
        if (cal.get(Calendar.AM_PM) == Calendar.AM) "ص" else "م"
    }

    when (clockStyle) {
        1 -> Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.55f),
            border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(hhmm, fontSize = 66.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.padding(bottom = 10.dp)) {
                    Text(ss, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    Text(amPm, fontSize = 16.sp, color = Color(0xFFFBBF24))
                }
            }
        }
        2 -> {
            val sec = cal.get(Calendar.SECOND)
            val min = cal.get(Calendar.MINUTE)
            val hr = cal.get(Calendar.HOUR)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Canvas(modifier = Modifier.size(116.dp)) {
                    val r = size.minDimension / 2f
                    val center = Offset(r, r)
                    drawCircle(Color.Black.copy(alpha = 0.5f), radius = r)
                    drawCircle(Color(0xFF38BDF8), radius = r, style = Stroke(width = 3.dp.toPx()))
                    val hrAngle = Math.toRadians(((hr + min / 60f) * 30f - 90f).toDouble())
                    val minAngle = Math.toRadians((min * 6f - 90f).toDouble())
                    val secAngle = Math.toRadians((sec * 6f - 90f).toDouble())
                    drawLine(Color.White, center, Offset(center.x + (r * 0.5f * cos(hrAngle)).toFloat(), center.y + (r * 0.5f * sin(hrAngle)).toFloat()), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(Color(0xFFE2E8F0), center, Offset(center.x + (r * 0.72f * cos(minAngle)).toFloat(), center.y + (r * 0.72f * sin(minAngle)).toFloat()), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(Color(0xFFFBBF24), center, Offset(center.x + (r * 0.82f * cos(secAngle)).toFloat(), center.y + (r * 0.82f * sin(secAngle)).toFloat()), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                }
                Text(hhmm, fontSize = 54.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        3 -> Column(horizontalAlignment = Alignment.End) {
            val parts = hhmm.split(":")
            Text(parts.getOrElse(0) { "١٢" }, fontSize = 68.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 66.sp)
            Text(parts.getOrElse(1) { "٠٠" }, fontSize = 68.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF38BDF8), lineHeight = 66.sp)
        }
        else -> Row(verticalAlignment = Alignment.Bottom) {
            Text(hhmm, fontSize = 74.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-1).sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(ss, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                Text(amPm, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFBBF24))
            }
        }
    }
}

@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { LauncherPreferences(context) }
    val scope = rememberCoroutineScope()

    val config by prefs.configFlow.collectAsState(initial = LauncherConfig())
    var installedApps by remember { mutableStateOf<List<TvAppInfo>>(emptyList()) }
    var liveFeed by remember { mutableStateOf(LiveFeedState()) }
    var prayerInfo by remember { mutableStateOf(PalestineData.PrayerDisplay()) }
    var updateInfo by remember { mutableStateOf<GitHubUpdateInfo?>(null) }

    var isWifiConnected by remember { mutableStateOf(true) }
    var isVpnActive by remember { mutableStateOf(false) }
    var isQuranPlaying by remember { mutableStateOf(false) }
    var isQuranBuffering by remember { mutableStateOf(false) }
    var manualQuranOverride by remember { mutableStateOf<Boolean?>(null) }
    var isAppInForeground by remember { mutableStateOf(true) }
    var isScreensaverMode by remember { mutableStateOf(false) }
    var isScreensaverEditMode by remember { mutableStateOf(false) }
    var isLiveEditMode by remember { mutableStateOf(false) }
    var showSettingsDrawer by remember { mutableStateOf(false) }
    var showAdhkarDialog by remember { mutableStateOf(false) }
    var showQuranDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var tabCardExpanded by remember { mutableStateOf(false) }

    // تذكير الأذان المرئي قبل الوقت
    var showAdhanReminder by remember { mutableStateOf(false) }
    var adhanReminderPrayer by remember { mutableStateOf("") }
    var adhanReminderMinsLeft by remember { mutableStateOf(0) }
    var lastReminderKey by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            scope.launch {
                prefs.updateConfig { it[LauncherPreferences.CUSTOM_WALLPAPER_URI] = uri.toString() }
            }
        }
    }

    val mediaPlayer = remember { MediaPlayer() }
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    val wakeLock = remember {
        powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PalestineTV:QuranAudio")
    }

    fun startQuranStream(url: String) {
        val streamUrl = if (url.isBlank() || !url.startsWith("http")) QURAN_DEFAULT_STREAM else url
        try {
            isQuranBuffering = true
            isQuranPlaying = false
            mediaPlayer.reset()
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mediaPlayer.setDataSource(streamUrl)
            mediaPlayer.setOnPreparedListener {
                it.start()
                isQuranBuffering = false
                isQuranPlaying = true
                if (!wakeLock.isHeld) wakeLock.acquire(12 * 60 * 60 * 1000L)
            }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                isQuranBuffering = false
                isQuranPlaying = false
                if (wakeLock.isHeld) wakeLock.release()
                scope.launch {
                    delay(2500)
                    if (isQuranPlaying || isQuranBuffering) return@launch
                    startQuranStream(streamUrl)
                }
                true
            }
            mediaPlayer.setOnCompletionListener {
                isQuranBuffering = false
                isQuranPlaying = false
                if (wakeLock.isHeld) wakeLock.release()
            }
            mediaPlayer.prepareAsync()
        } catch (_: Exception) {
            isQuranBuffering = false
            isQuranPlaying = false
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    fun stopQuranStream() {
        try {
            if (mediaPlayer.isPlaying) mediaPlayer.stop()
            mediaPlayer.reset()
        } catch (_: Exception) {}
        isQuranBuffering = false
        isQuranPlaying = false
        if (wakeLock.isHeld) wakeLock.release()
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isAppInForeground = event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START
            if (event == Lifecycle.Event.ON_STOP && config.quranPlayMode == 1) stopQuranStream()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try { mediaPlayer.release() } catch (_: Exception) {}
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    // مؤقت تغيير الخلفية تلقائياً بالدقائق
    LaunchedEffect(config.wallpaperIntervalMinutes, config.customWallpaperUri) {
        if (config.customWallpaperUri.isEmpty() && config.wallpaperIntervalMinutes > 0) {
            val totalOnline = LiveRepository.natureWallpapers.count { it.second.isNotEmpty() }
            if (totalOnline > 1) {
                while (true) {
                    delay(config.wallpaperIntervalMinutes * 60_000L)
                    val nextIdx = (config.wallpaperIndex + 1) % totalOnline
                    prefs.updateConfig { it[LauncherPreferences.WALLPAPER_IDX] = nextIdx }
                }
            }
        }
    }

    LaunchedEffect(
        config.quranPlayMode,
        config.quranScheduleStartHour,
        config.quranScheduleEndHour,
        config.quranStreamUrl,
        isScreensaverMode,
        isAppInForeground,
        manualQuranOverride
    ) {
        if (manualQuranOverride != null) {
            if (manualQuranOverride == true && !isQuranPlaying && !isQuranBuffering) startQuranStream(config.quranStreamUrl)
            if (manualQuranOverride == false && (isQuranPlaying || isQuranBuffering)) stopQuranStream()
            return@LaunchedEffect
        }
        when (config.quranPlayMode) {
            0 -> {}
            1 -> {
                if (isAppInForeground && !isScreensaverMode) {
                    if (!isQuranPlaying && !isQuranBuffering) startQuranStream(config.quranStreamUrl)
                } else if (isQuranPlaying || isQuranBuffering) stopQuranStream()
            }
            2 -> {
                if (isScreensaverMode) {
                    if (!isQuranPlaying && !isQuranBuffering) startQuranStream(config.quranStreamUrl)
                } else if (isQuranPlaying || isQuranBuffering) stopQuranStream()
            }
            3 -> {
                while (true) {
                    val cal = Calendar.getInstance(TimeZone.getTimeZone(config.cityTimeZone))
                    val hr = cal.get(Calendar.HOUR_OF_DAY)
                    val s = config.quranScheduleStartHour
                    val e = config.quranScheduleEndHour
                    val inRange = if (s <= e) hr in s until e else (hr >= s || hr < e)
                    if (inRange && !isQuranPlaying && !isQuranBuffering) startQuranStream(config.quranStreamUrl)
                    else if (!inRange && (isQuranPlaying || isQuranBuffering)) stopQuranStream()
                    delay(30_000L)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        installedApps = AppManager.loadInstalledApps(context)
        val feed = LiveRepository.fetchGitHubFeed()
        val instantPrices = LiveRepository.fetchInstantPrices()
        liveFeed = feed.copy(prices = feed.prices + instantPrices)
        updateInfo = LiveRepository.checkGitHubUpdate("1.2.0")
        if (updateInfo?.hasUpdate == true) showUpdateDialog = true

        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        isWifiConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        isVpnActive = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    }

    LaunchedEffect(config.cityLat, config.cityLon, config.cityTimeZone) {
        prayerInfo = LiveRepository.fetchExactPrayers(config.cityLat, config.cityLon, config.cityTimeZone)
        val w = LiveRepository.fetchCityWeather(config.cityLat, config.cityLon, config.cityTimeZone)
        if (w != null) liveFeed = liveFeed.copy(currentTemp = w.first, dailyMax = w.second, dailyMin = w.third)
    }

    // تهيئة مدير الأذان وتحديث مواقيت الصلاة
    LaunchedEffect(Unit) {
        AdhanManager.init(context)
    }

    LaunchedEffect(prayerInfo, config.cityTimeZone) {
        AdhanManager.setPrayerTimes(
            mapOf(
                "الفجر" to toLatinDigits(prayerInfo.fajr),
                "الشروق" to toLatinDigits(prayerInfo.sunrise),
                "الظهر" to toLatinDigits(prayerInfo.dhuhr),
                "العصر" to toLatinDigits(prayerInfo.asr),
                "المغرب" to toLatinDigits(prayerInfo.maghrib),
                "العشاء" to toLatinDigits(prayerInfo.isha)
            )
        )
    }

    // مراقبة دخول وقت الصلاة لتشغيل الأذان تلقائياً وإظهار تذكير مرئي قبله
    var lastAdhanKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(config.cityTimeZone, config.adhanEnabled, config.adhanPlayFajrOnly, config.adhanReminderMinutes, config.adhanMuadhinId, config.adhanLengthMode, config.adhanVolume) {
        while (true) {
            if (config.adhanEnabled) {
                val tz = TimeZone.getTimeZone(config.cityTimeZone)
                val now = Calendar.getInstance(tz)
                val nowMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                val dayOfYear = now.get(Calendar.DAY_OF_YEAR)
                val prayers = listOf(
                    "الفجر" to toLatinDigits(prayerInfo.fajr),
                    "الظهر" to toLatinDigits(prayerInfo.dhuhr),
                    "العصر" to toLatinDigits(prayerInfo.asr),
                    "المغرب" to toLatinDigits(prayerInfo.maghrib),
                    "العشاء" to toLatinDigits(prayerInfo.isha)
                )
                var reminderActive = false
                for ((name, timeStr) in prayers) {
                    val parts = timeStr.split(":")
                    if (parts.size >= 2) {
                        val h = parts[0].toIntOrNull() ?: continue
                        val m = parts[1].toIntOrNull() ?: continue
                        val prayerMins = h * 60 + m
                        val minsUntil = (prayerMins - nowMins + 24 * 60) % (24 * 60)
                        val reminder = config.adhanReminderMinutes
                        val isAtTime = minsUntil == 0
                        val isReminder = reminder > 0 && minsUntil > 0 && minsUntil <= reminder
                        val fajrOnly = config.adhanPlayFajrOnly && name != "الفجر"
                        if (isAtTime && !fajrOnly) {
                            val key = "$dayOfYear-$name-a"
                            if (lastAdhanKey != key) {
                                lastAdhanKey = key
                                showAdhanReminder = false
                                scope.launch {
                                    AdhanManager.playAdhan(context, name, config.adhanMuadhinId, config.adhanLengthMode, config.adhanVolume)
                                }
                            }
                        } else if (isReminder && !fajrOnly) {
                            val key = "$dayOfYear-$name-r"
                            if (lastReminderKey != key) {
                                lastReminderKey = key
                                showAdhanReminder = true
                                adhanReminderPrayer = name
                                adhanReminderMinsLeft = minsUntil
                                reminderActive = true
                            }
                        }
                    }
                }
                if (!reminderActive && !isAnyPrayerAtTime(nowMins, prayers)) {
                    showAdhanReminder = false
                }
            }
            delay(20_000L)
        }
    }

    DisposableEffect(Unit) {
        onDispose { AdhanManager.release() }
    }

    val (hijriDate, gregorianDate) = remember(config.cityTimeZone) {
        PalestineData.getFormattedDates(config.cityTimeZone)
    }

    val activeWallpaperModel: Any? = remember(config.customWallpaperUri, config.wallpaperIndex) {
        if (config.customWallpaperUri.isNotEmpty()) {
            Uri.parse(config.customWallpaperUri)
        } else {
            val url = LiveRepository.natureWallpapers
                .getOrElse(config.wallpaperIndex) { LiveRepository.natureWallpapers.first() }
                .second
            url.ifEmpty { null }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF070D19), Color(0xFF0F172A), Color(0xFF081226))
                )
            )
    ) {
        // شريط التذكير المرئي قبل الأذان
        if (showAdhanReminder) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp, vertical = 10.dp)
                    .align(Alignment.Top),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF7C3AED).copy(alpha = 0.92f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "تذكير الأذان - $adhanReminderPrayer",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "يُرفع الأذان بعد $adhanReminderMinsLeft دقيقة${if (adhanReminderMinsLeft > 1) "ات" else ""}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

    if (activeWallpaperModel != null) {
            AsyncImage(
                model = activeWallpaperModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
            )
        }

        if (tabCardExpanded) {
            InteractiveTabsCard(
                modifier = Modifier.fillMaxSize(),
                config = config,
                prayerInfo = prayerInfo,
                liveFeed = liveFeed,
                hijriDate = hijriDate,
                gregorianDate = gregorianDate,
                onAddTask = { task -> scope.launch { prefs.saveTasks(config.tasks + task) } },
                onRemoveTask = { task -> scope.launch { prefs.saveTasks(config.tasks - task) } },
                onAddReminder = { rem -> scope.launch { prefs.saveReminders(config.reminders + rem) } },
                onExpand = { tabCardExpanded = it }
            )
        } else if (isScreensaverMode) {
            ScreensaverView(
                timeZoneId = config.cityTimeZone,
                hijri = hijriDate,
                gregorian = gregorianDate,
                prayerInfo = prayerInfo,
                config = config,
                isEditMode = isScreensaverEditMode,
                onSaveOffsets = { ox, oy, sc ->
                    scope.launch {
                        prefs.updateConfig {
                            it[LauncherPreferences.SS_CLOCK_X] = ox
                            it[LauncherPreferences.SS_CLOCK_Y] = oy
                            it[LauncherPreferences.SS_CLOCK_SCALE] = sc
                        }
                    }
                    isScreensaverEditMode = false
                },
                onExit = { isScreensaverMode = false; isScreensaverEditMode = false }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 26.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                TopControlBar(
                    config = config,
                    isWifiConnected = isWifiConnected,
                    isQuranPlaying = isQuranPlaying,
                    isQuranBuffering = isQuranBuffering,
                    isVpnActive = isVpnActive,
                    updateInfo = updateInfo,
                    onOpenWifi = { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) },
                    onToggleQuran = {
                        if (isQuranPlaying || isQuranBuffering) {
                            manualQuranOverride = false
                            stopQuranStream()
                        } else {
                            manualQuranOverride = true
                            startQuranStream(config.quranStreamUrl)
                        }
                    },
                    onOpenQuranBook = { showQuranDialog = true },
                    onOpenScreensaver = { isScreensaverMode = true },
                    onOpenVpn = { context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS)) },
                    onOpenSettings = { showSettingsDrawer = true },
                    onTriggerUpdate = { showUpdateDialog = true }
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PillActionButton(
                        label = "المهام",
                        icon = Icons.Default.CheckCircle,
                        onClick = { tabCardExpanded = true }
                    )
                    PillActionButton(
                        label = "التذكيرات",
                        icon = Icons.Default.EventNote,
                        onClick = { tabCardExpanded = true }
                    )
                    PillActionButton(
                        label = "البطاقة الشاملة",
                        icon = Icons.Default.ViewCompact,
                        onClick = { tabCardExpanded = !tabCardExpanded }
                    )
                }

                if (config.showBottomBar && config.tickerBarPosition == 1) {
                    BottomTickerBar(config = config, prayerInfo = prayerInfo, liveFeed = liveFeed)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InteractiveTabsCard(
                        modifier = Modifier
                            .weight(0.54f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                translationX = config.cardOffsetX
                                translationY = config.cardOffsetY
                                scaleX = config.cardScale
                                scaleY = config.cardScale
                            },
                        config = config,
                        prayerInfo = prayerInfo,
                        liveFeed = liveFeed,
                        hijriDate = hijriDate,
                        gregorianDate = gregorianDate,
                        onAddTask = { task -> scope.launch { prefs.saveTasks(config.tasks + task) } },
                        onRemoveTask = { task -> scope.launch { prefs.saveTasks(config.tasks - task) } },
                        onAddReminder = { rem -> scope.launch { prefs.saveReminders(config.reminders + rem) } },
                        onExpand = { tabCardExpanded = it }
                    )


                    Spacer(modifier = Modifier.width(18.dp))

                    Column(
                        modifier = Modifier
                            .weight(0.46f)
                            .graphicsLayer {
                                translationX = config.clockOffsetX
                                translationY = config.clockOffsetY
                                scaleX = config.clockScale
                                scaleY = config.clockScale
                            },
                        horizontalAlignment = Alignment.End
                    ) {
                        ArabicClockDisplay(timeZoneId = config.cityTimeZone, clockStyle = config.clockStyle)
                        val dateString = when (config.dateDisplayMode) {
                            0 -> "$hijriDate  |  $gregorianDate"
                            1 -> hijriDate
                            2 -> gregorianDate
                            else -> ""
                        }
                        if (dateString.isNotEmpty()) {
                            Text(
                                text = dateString,
                                fontSize = 18.sp,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            PillActionButton(
                                label = "أذكار الصباح والمساء",
                                icon = Icons.Default.AutoAwesome,
                                onClick = { showAdhkarDialog = true }
                            )
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.50f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = PalestineData.toArabicDigits("${liveFeed.currentTemp}° ${config.cityName}"),
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                AppDockRow(
                    apps = installedApps,
                    config = config,
                    modifier = Modifier.graphicsLayer {
                        translationX = config.dockOffsetX
                        translationY = config.dockOffsetY
                    },
                    onLaunchApp = { app -> AppManager.launchApp(context, app.packageName) }
          )

                Spacer(modifier = Modifier.height(8.dp))

                if (config.showBottomBar && config.tickerBarPosition == 0) {
                    BottomTickerBar(config = config, prayerInfo = prayerInfo, liveFeed = liveFeed)
                }
            }
        }

        if (isLiveEditMode) {
            LiveLayoutEditorOverlay(
                config = config,
                onSave = { cx, cy, cs, kx, ky, ks, ko, dx, dy ->
                    scope.launch {
                        prefs.updateConfig {
                            it[LauncherPreferences.CLOCK_X] = cx
                            it[LauncherPreferences.CLOCK_Y] = cy
                            it[LauncherPreferences.CLOCK_SCALE] = cs
                            it[LauncherPreferences.CARD_X] = kx
                            it[LauncherPreferences.CARD_Y] = ky
                            it[LauncherPreferences.CARD_SCALE] = ks
                            it[LauncherPreferences.CARD_OPACITY] = ko
                            it[LauncherPreferences.DOCK_X] = dx
                            it[LauncherPreferences.DOCK_Y] = dy
                        }
                    }
                    isLiveEditMode = false
                },
                onCancel = { isLiveEditMode = false }
            )
        }

        if (showSettingsDrawer) {
            SettingsModalDrawer(
                config = config,
                prefs = prefs,
                onPickDeviceImage = { imagePickerLauncher.launch(arrayOf("image/*")) },
                onOpenLiveEditor = { showSettingsDrawer = false; isLiveEditMode = true },
                onOpenScreensaverEditor = { showSettingsDrawer = false; isScreensaverEditMode = true; isScreensaverMode = true },
                onDismiss = { showSettingsDrawer = false }
            )
        }

        if (showAdhkarDialog) {
            AlertDialog(
                onDismissRequest = { showAdhkarDialog = false },
                confirmButton = { TextButton(onClick = { showAdhkarDialog = false }) { Text("إغلاق") } },
                title = { Text("أذكار الصباح والمساء", fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PalestineData.adhkarList) { dhikr -> Text("• $dhikr", fontSize = 15.sp) }
                    }
                }
            )
        }

        if (showQuranDialog) {
            FullQuranDialog(onDismiss = { showQuranDialog = false })
        }

        if (showUpdateDialog && updateInfo != null) {
            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                confirmButton = {
                    Button(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo!!.apkDownloadUrl)))
                        showUpdateDialog = false
                    }) { Text("تحميل وتثبيت الآن") }
                },
                dismissButton = { TextButton(onClick = { showUpdateDialog = false }) { Text("لاحقاً") } },
                title = { Text("تحديث جديد متاح (${updateInfo!!.latestVersion})") },
                text = { Text(updateInfo!!.releaseNotes) }
            )
        }
    }
}

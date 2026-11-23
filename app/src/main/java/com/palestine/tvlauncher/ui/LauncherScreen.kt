package com.palestine.tvlauncher.ui

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
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
import androidx.lifecycle.compose.LocalLifecycleOwner
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

    fun startQuranStream(url: String) {
        try {
            isQuranBuffering = true
            isQuranPlaying = false
            mediaPlayer.reset()
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mediaPlayer.setDataSource(url)
            mediaPlayer.setOnPreparedListener {
                it.start()
                isQuranBuffering = false
                isQuranPlaying = true
            }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                isQuranBuffering = false
                isQuranPlaying = false
                true
            }
            mediaPlayer.prepareAsync()
        } catch (_: Exception) {
            isQuranBuffering = false
            isQuranPlaying = false
        }
    }

    fun stopQuranStream() {
        try {
            if (mediaPlayer.isPlaying) mediaPlayer.stop()
            mediaPlayer.reset()
        } catch (_: Exception) {}
        isQuranBuffering = false
        isQuranPlaying = false
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

        if (isScreensaverMode) {
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
                        onAddReminder = { rem -> scope.launch { prefs.saveReminders(config.reminders + rem) } }
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


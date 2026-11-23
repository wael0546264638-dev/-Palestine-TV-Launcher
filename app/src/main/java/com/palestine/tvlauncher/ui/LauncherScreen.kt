package com.palestine.tvlauncher.ui

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.palestine.tvlauncher.data.*
import com.palestine.tvlauncher.network.*
import com.palestine.tvlauncher.data.AppManager
import com.palestine.tvlauncher.data.TvAppInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
private fun ArabicClockDisplay(timeZoneId: String, clockStyle: Int) {
    var hourText by remember { mutableStateOf("") }
    var minuteText by remember { mutableStateOf("") }
    var amPmText by remember { mutableStateOf("") }
    var hourInt by remember { mutableIntStateOf(12) }
    var minInt by remember { mutableIntStateOf(0) }
    var secInt by remember { mutableIntStateOf(0) }

    LaunchedEffect(timeZoneId) {
        val tz = TimeZone.getTimeZone(timeZoneId)
        val sdfHour = SimpleDateFormat("hh", Locale.US).apply { timeZone = tz }
        val sdfMin = SimpleDateFormat("mm", Locale.US).apply { timeZone = tz }
        val sdfAmPm = SimpleDateFormat("a", Locale("ar")).apply { timeZone = tz }
        while (true) {
            val now = Date()
            val cal = Calendar.getInstance(tz).apply { time = now }
            hourText = PalestineData.toArabicDigits(sdfHour.format(now))
            minuteText = PalestineData.toArabicDigits(sdfMin.format(now))
            amPmText = sdfAmPm.format(now)
            hourInt = cal.get(Calendar.HOUR)
            minInt = cal.get(Calendar.MINUTE)
            secInt = cal.get(Calendar.SECOND)
            delay(1000L)
        }
    }

    when (clockStyle) {
        1 -> {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.72f),
                border = BorderStroke(2.dp, Color(0xFF38BDF8).copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = amPmText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "$hourText:$minuteText",
                        fontSize = 74.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 74.sp
                    )
                }
            }
        }
        2 -> {
            Box(
                modifier = Modifier.size(164.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        color = Color(0xFF0F172A).copy(alpha = 0.78f),
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF38BDF8),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    for (i in 0 until 12) {
                        val angle = Math.toRadians((i * 30 - 90).toDouble())
                        val startR = radius * 0.82f
                        val endR = radius * 0.92f
                        drawLine(
                            color = Color.White.copy(alpha = 0.75f),
                            start = Offset(
                                center.x + (startR * cos(angle)).toFloat(),
                                center.y + (startR * sin(angle)).toFloat()
                            ),
                            end = Offset(
                                center.x + (endR * cos(angle)).toFloat(),
                                center.y + (endR * sin(angle)).toFloat()
                            ),
                            strokeWidth = if (i % 3 == 0) 3.dp.toPx() else 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    val hourAngle = Math.toRadians(((hourInt + minInt / 60.0) * 30.0) - 90.0)
                    drawLine(
                        color = Color.White,
                        start = center,
                        end = Offset(
                            center.x + (radius * 0.52f * cos(hourAngle)).toFloat(),
                            center.y + (radius * 0.52f * sin(hourAngle)).toFloat()
                        ),
                        strokeWidth = 5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    val minAngle = Math.toRadians((minInt * 6.0) - 90.0)
                    drawLine(
                        color = Color(0xFF38BDF8),
                        start = center,
                        end = Offset(
                            center.x + (radius * 0.74f * cos(minAngle)).toFloat(),
                            center.y + (radius * 0.74f * sin(minAngle)).toFloat()
                        ),
                        strokeWidth = 3.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    val secAngle = Math.toRadians((secInt * 6.0) - 90.0)
                    drawLine(
                        color = Color(0xFFFBBF24),
                        start = center,
                        end = Offset(
                            center.x + (radius * 0.82f * cos(secAngle)).toFloat(),
                            center.y + (radius * 0.82f * sin(secAngle)).toFloat()
                        ),
                        strokeWidth = 1.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = Color(0xFFFBBF24), radius = 4.dp.toPx(), center = center)
                }
            }
        }
        3 -> {
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = amPmText,
                        fontSize = 24.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = hourText,
                        fontSize = 68.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 68.sp
                    )
                }
                Text(
                    text = minuteText,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBBF24),
                    lineHeight = 68.sp
                )
            }
        }
        else -> {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = amPmText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$hourText:$minuteText",
                    fontSize = 86.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 86.sp
                )
            }
        }
    }
}

@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { LauncherPreferences(context) }
    val config by prefs.configFlow.collectAsState(initial = LauncherConfig())
    val scope = rememberCoroutineScope()

    var installedApps by remember { mutableStateOf<List<TvAppInfo>>(emptyList()) }
    var liveFeed by remember { mutableStateOf(LiveFeedState()) }
    var prayerInfo by remember { mutableStateOf(PalestineData.PrayerDisplay()) }
    var updateInfo by remember { mutableStateOf<GitHubUpdateInfo?>(null) }

    var isWifiConnected by remember { mutableStateOf(true) }
    var isVpnActive by remember { mutableStateOf(false) }
    var isQuranPlaying by remember { mutableStateOf(false) }
    var manualQuranOverride by remember { mutableStateOf<Boolean?>(null) }
    var isAppInForeground by remember { mutableStateOf(true) }
    var isScreensaverMode by remember { mutableStateOf(false) }
    var isScreensaverEditMode by remember { mutableStateOf(false) }
    var isLiveEditMode by remember { mutableStateOf(false) }
    var showSettingsDrawer by remember { mutableStateOf(false) }
    var showAdhkarDialog by remember { mutableStateOf(false) }
    var showQuranDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var selectedSurahIdx by remember { mutableIntStateOf(0) }

    val mediaPlayer = remember { MediaPlayer() }

    fun startQuranStream(url: String) {
        try {
            mediaPlayer.reset()
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mediaPlayer.setDataSource(url)
            mediaPlayer.setOnPreparedListener {
                it.start()
                isQuranPlaying = true
            }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                isQuranPlaying = false
                true
            }
            mediaPlayer.prepareAsync()
        } catch (_: Exception) {
            isQuranPlaying = false
        }
    }

    fun stopQuranStream() {
        try {
            if (mediaPlayer.isPlaying) mediaPlayer.stop()
            mediaPlayer.reset()
        } catch (_: Exception) {}
        isQuranPlaying = false
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isAppInForeground = event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START
            if (event == Lifecycle.Event.ON_STOP && config.quranPlayMode == 1) {
                stopQuranStream()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try { mediaPlayer.release() } catch (_: Exception) {}
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
            if (manualQuranOverride == true && !isQuranPlaying) startQuranStream(config.quranStreamUrl)
            if (manualQuranOverride == false && isQuranPlaying) stopQuranStream()
            return@LaunchedEffect
        }
        when (config.quranPlayMode) {
            0 -> {}
            1 -> {
                if (isAppInForeground && !isScreensaverMode) {
                    if (!isQuranPlaying) startQuranStream(config.quranStreamUrl)
                } else if (isQuranPlaying) stopQuranStream()
            }
            2 -> {
                if (isScreensaverMode) {
                    if (!isQuranPlaying) startQuranStream(config.quranStreamUrl)
                } else if (isQuranPlaying) stopQuranStream()
            }
            3 -> {
                while (true) {
                    val cal = Calendar.getInstance(TimeZone.getTimeZone(config.cityTimeZone))
                    val hr = cal.get(Calendar.HOUR_OF_DAY)
                    val s = config.quranScheduleStartHour
                    val e = config.quranScheduleEndHour
                    val inRange = if (s <= e) hr in s until e else (hr >= s || hr < e)
                    if (inRange && !isQuranPlaying) startQuranStream(config.quranStreamUrl)
                    else if (!inRange && isQuranPlaying) stopQuranStream()
                    delay(30_000L)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
                installedApps = AppManager.getInstalledTvApps(context)
        val feed = LiveRepository.fetchGitHubFeed()
        val instantPrices = LiveRepository.fetchInstantPrices()
        liveFeed = feed.copy(prices = feed.prices + instantPrices)
        updateInfo = LiveRepository.checkGitHubUpdate("1.1.0")
        if (updateInfo?.hasUpdate == true) showUpdateDialog = true

        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        isWifiConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        isVpnActive = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    }

    LaunchedEffect(config.cityLat, config.cityLon, config.cityTimeZone) {
        prayerInfo = LiveRepository.fetchExactPrayers(config.cityLat, config.cityLon, config.cityTimeZone)
        val w = LiveRepository.fetchCityWeather(config.cityLat, config.cityLon, config.cityTimeZone)
        if (w != null) {
            liveFeed = liveFeed.copy(currentTemp = w.first, dailyMax = w.second, dailyMin = w.third)
        }
    }

    val (hijriDate, gregorianDate) = remember(config.cityTimeZone) {
        PalestineData.getFormattedDates(config.cityTimeZone)
    }

    val wallpaperUrl = LiveRepository.natureWallpapers
        .getOrElse(config.wallpaperIndex) { LiveRepository.natureWallpapers.first() }
        .second
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF070D19), Color(0xFF0F172A), Color(0xFF081226))
                )
            )
    ) {
        if (wallpaperUrl.isNotEmpty()) {
            AsyncImage(
                model = wallpaperUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
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
                onExit = {
                    isScreensaverMode = false
                    isScreensaverEditMode = false
                }
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
                    isVpnActive = isVpnActive,
                    updateInfo = updateInfo,
                    onOpenWifi = { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) },
                    onToggleQuran = {
                        if (isQuranPlaying) {
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
                        onAddTask = { task ->
                            scope.launch { prefs.saveTasks(config.tasks + task) }
                        },
                        onRemoveTask = { task ->
                            scope.launch { prefs.saveTasks(config.tasks - task) }
                        },
                        onAddReminder = { rem ->
                            scope.launch { prefs.saveReminders(config.reminders + rem) }
                        }
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
                        ArabicClockDisplay(
                            timeZoneId = config.cityTimeZone,
                            clockStyle = config.clockStyle
                        )

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

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PillActionButton(
                                label = "أذكار الصباح والمساء",
                                icon = Icons.Default.AutoAwesome,
                                onClick = { showAdhkarDialog = true }
                            )

                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.50f),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.18f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WbSunny,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(20.dp)
                                    )
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

                if (config.showBottomBar) {
                    BottomTickerBar(
                        config = config,
                        prayerInfo = prayerInfo,
                        liveFeed = liveFeed
                    )
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
                onOpenLiveEditor = {
                    showSettingsDrawer = false
                    isLiveEditMode = true
                },
                onOpenScreensaverEditor = {
                    showSettingsDrawer = false
                    isScreensaverEditMode = true
                    isScreensaverMode = true
                },
                onDismiss = { showSettingsDrawer = false }
            )
        }

        if (showAdhkarDialog) {
            AlertDialog(
                onDismissRequest = { showAdhkarDialog = false },
                confirmButton = {
                    TextButton(onClick = { showAdhkarDialog = false }) { Text("إغلاق") }
                },
                title = { Text("أذكار الصباح والمساء", fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PalestineData.adhkarList) { dhikr ->
                            Text("• $dhikr", fontSize = 15.sp)
                        }
                    }
                }
            )
        }

        if (showQuranDialog) {
            val currentSurah = PalestineData.fullQuranSurahs.getOrElse(selectedSurahIdx) {
                PalestineData.fullQuranSurahs.first()
            }
            AlertDialog(
                onDismissRequest = { showQuranDialog = false },
                confirmButton = {
                    TextButton(onClick = { showQuranDialog = false }) { Text("إغلاق") }
                },
                title = { Text("المصحف الشريف - ${currentSurah.name}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            PalestineData.fullQuranSurahs.forEachIndexed { idx, surah ->
                                FilterChip(
                                    selected = selectedSurahIdx == idx,
                                    onClick = { selectedSurahIdx = idx },
                                    label = { Text(surah.name) }
                                )
                            }
                        }
                        LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                            item {
                                Text(
                                    text = currentSurah.verses,
                                    fontSize = 19.sp,
                                    lineHeight = 32.sp
                                )
                            }
                        }
                    }
                }
            )
        }

                if (showUpdateDialog && updateInfo != null) {
            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                confirmButton = {
                    Button(onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, android.net.Uri.parse(updateInfo!!.apkDownloadUrl))
                        )
                        showUpdateDialog = false
                    }) {
                        Text("تحميل وتثبيت الآن")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("لاحقاً")
                    }
                },
                title = { Text("تحديث جديد متاح (${updateInfo!!.latestVersion})") },
                text = { Text(updateInfo!!.releaseNotes) }
            )
        }
    }
}

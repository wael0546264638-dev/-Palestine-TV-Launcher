package com.palestine.tvlauncher.ui

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.palestine.tvlauncher.data.*
import com.palestine.tvlauncher.network.GitHubUpdateInfo
import com.palestine.tvlauncher.network.LiveFeedState
import com.palestine.tvlauncher.network.LiveRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LauncherApp(prefs: LauncherPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val config by prefs.configFlow.collectAsState(initial = LauncherConfig())

    var installedApps by remember { mutableStateOf<List<TvAppInfo>>(emptyList()) }
    var liveFeed by remember { mutableStateOf(LiveFeedState()) }
    var updateInfo by remember { mutableStateOf<GitHubUpdateInfo?>(null) }

    var showSettingsDrawer by remember { mutableStateOf(false) }
    var isLiveEditMode by remember { mutableStateOf(false) }
    var isScreensaverMode by remember { mutableStateOf(false) }
    var isScreensaverEditMode by remember { mutableStateOf(false) }
    var showAdhkarDialog by remember { mutableStateOf(false) }
    var showQuranDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    var isRadioPlaying by remember { mutableStateOf(false) }
    val mediaPlayer = remember {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }

    fun toggleQuranRadio() {
        try {
            if (isRadioPlaying) {
                mediaPlayer.pause()
                isRadioPlaying = false
            } else {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(config.quranStreamUrl)
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener {
                    it.start()
                    isRadioPlaying = true
                }
            }
        } catch (_: Exception) {
            isRadioPlaying = false
        }
    }

    LaunchedEffect(Unit) {
        installedApps = AppManager.loadInstalledApps(context)
        liveFeed = LiveRepository.fetchGitHubFeed()
        updateInfo = LiveRepository.checkGitHubUpdate("1.0.0")
    }

    LaunchedEffect(config.cityName) {
        val city = PalestineData.cities.find {
            it.name == config.cityName
        } ?: PalestineData.cities.first()
        LiveRepository.fetchCityWeather(city.lat, city.lon)?.let { res ->
            liveFeed = liveFeed.copy(
                currentTemp = res.first,
                dailyMax = res.second,
                dailyMin = res.third
            )
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val freshPrices = LiveRepository.fetchInstantPrices()
            if (freshPrices.isNotEmpty()) {
                liveFeed = liveFeed.copy(prices = liveFeed.prices + freshPrices)
            }
            delay(20_000L)
        }
    }

    var currentTimeText by remember { mutableStateOf("") }
    var amPmText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTimeText = SimpleDateFormat("hh:mm", Locale.ENGLISH).format(now)
            val ap = SimpleDateFormat("a", Locale.ENGLISH).format(now)
            amPmText = if (ap == "AM") "ص" else "م"
            delay(1000L)
        }
    }

    val selectedCity = PalestineData.cities.find {
        it.name == config.cityName
    } ?: PalestineData.cities.first()
    val prayerInfo = remember(config.cityName, currentTimeText) {
        PalestineData.calculatePrayers(selectedCity)
    }
    val (hijriDate, gregorianDate) = remember(currentTimeText) {
        PalestineData.getFormattedDates()
    }

    val wallpaperUrl = LiveRepository.natureWallpapers
        .getOrNull(config.wallpaperIndex)?.second ?: ""

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF0B1120)
                    )
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
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        }

        if (isScreensaverMode) {
            ScreensaverView(
                timeText = currentTimeText,
                amPm = amPmText,
                hijri = hijriDate,
                gregorian = gregorianDate,
                prayerInfo = prayerInfo,
                config = config,
                isEditMode = isScreensaverEditMode,
                onSaveOffsets = { x: Float, y: Float, scale: Float ->
                    scope.launch {
                        prefs.updateConfig {
                            it[LauncherPreferences.SS_CLOCK_X] = x
                            it[LauncherPreferences.SS_CLOCK_Y] = y
                            it[LauncherPreferences.SS_CLOCK_SCALE] = scale
                        }
                    }
                    isScreensaverEditMode = false
                    isScreensaverMode = false
                },
                onExit = {
                    isScreensaverEditMode = false
                    isScreensaverMode = false
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                TopControlBar(
                    config = config,
                    isRadioPlaying = isRadioPlaying,
                    hasUpdate = updateInfo?.hasUpdate == true,
                    onToggleRadio = { toggleQuranRadio() },
                    onOpenScreensaver = {
                        if (config.quranAutoPlayScreensaver && !isRadioPlaying) {
                            toggleQuranRadio()
                        }
                        isScreensaverMode = true
                    },
                    onOpenQuranBook = { showQuranDialog = true },
                    onOpenSettings = { showSettingsDrawer = true },
                    onOpenWifi = {
                        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    },
                    onOpenUpdate = { showUpdateDialog = true }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // البطاقة التفاعلية في اليمين (أول عنصر في اتجاه RTL)
                    InteractiveTabsCard(
                        modifier = Modifier
                            .weight(0.52f)
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
                        onAddTask = { newTask: String ->
                            scope.launch { prefs.saveTasks(config.tasks + newTask) }
                        },
                        onRemoveTask = { task: String ->
                            scope.launch { prefs.saveTasks(config.tasks - task) }
                        },
                        onAddReminder = { rem: FamilyReminder ->
                            scope.launch {
                                prefs.saveReminders(config.reminders + rem)
                            }
                        }
                    )


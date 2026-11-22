package com.palestine.tvlauncher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.palestine.tvlauncher.data.LauncherConfig
import com.palestine.tvlauncher.data.PalestineData
import com.palestine.tvlauncher.network.GitHubUpdateInfo
import com.palestine.tvlauncher.network.LiveFeedState
import com.palestine.tvlauncher.util.TvAppInfo
import java.util.Calendar

@Composable
fun TopControlBar(
    config: LauncherConfig,
    isWifiConnected: Boolean,
    isQuranPlaying: Boolean,
    isVpnActive: Boolean,
    updateInfo: GitHubUpdateInfo?,
    onOpenWifi: () -> Unit,
    onToggleQuran: () -> Unit,
    onOpenQuranBook: () -> Unit,
    onOpenScreensaver: () -> Unit,
    onOpenVpn: () -> Unit,
    onOpenSettings: () -> Unit,
    onTriggerUpdate: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (config.showTopWifi) {
                PillActionButton(
                    label = if (isWifiConnected) "متصل" else "غير متصل",
                    icon = if (isWifiConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                    tint = if (isWifiConnected) Color(0xFF34D399) else Color(0xFFF87171),
                    onClick = onOpenWifi
                )
            }
            if (config.showTopQuranRadio) {
                PillActionButton(
                    label = if (isQuranPlaying) "إيقاف القرآن" else "إذاعة القرآن",
                    icon = if (isQuranPlaying) Icons.Default.StopCircle else Icons.Default.Radio,
                    tint = if (isQuranPlaying) Color(0xFFFBBF24) else Color.White,
                    onClick = onToggleQuran
                )
            }
            if (config.showTopQuranBook) {
                PillActionButton(
                    label = "المصحف الشريف",
                    icon = Icons.Default.MenuBook,
                    onClick = onOpenQuranBook
                )
            }
            if (config.showTopScreensaver) {
                PillActionButton(
                    label = "شاشة التوقف",
                    icon = Icons.Default.Bedtime,
                    onClick = onOpenScreensaver
                )
            }
            if (config.showTopVpn) {
                PillActionButton(
                    label = if (isVpnActive) "VPN نشط" else "VPN",
                    icon = Icons.Default.VpnLock,
                    tint = if (isVpnActive) Color(0xFF38BDF8) else Color.White,
                    onClick = onOpenVpn
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (updateInfo != null && updateInfo.hasUpdate) {
                PillActionButton(
                    label = "تحديث (${updateInfo.latestVersion})",
                    icon = Icons.Default.SystemUpdate,
                    tint = Color(0xFF34D399),
                    onClick = onTriggerUpdate
                )
            }
            if (config.showTopSettings) {
                PillActionButton(
                    label = "الإعدادات",
                    icon = Icons.Default.Settings,
                    onClick = onOpenSettings
                )
            }
        }
    }
}

@Composable
fun PillActionButton(
    label: String,
    icon: ImageVector,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.07f else 1f,
        animationSpec = tween(70),
        label = "pillScale"
    )

    Surface(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() },
        shape = CircleShape,
        color = if (isFocused) Color.White else Color.Black.copy(alpha = 0.52f),
        border = BorderStroke(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = if (isFocused) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.18f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isFocused) Color.Black else tint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = if (isFocused) Color.Black else Color.White,
                fontSize = 14.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun AppDockRow(
    apps: List<TvAppInfo>,
    config: LauncherConfig,
    modifier: Modifier = Modifier,
    onLaunchApp: (TvAppInfo) -> Unit
) {
    val cornerDp = (config.appCornerRadiusPercent * 0.45f).dp
    val shape = RoundedCornerShape(cornerDp)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.Black.copy(alpha = config.dockBgOpacity)
    ) {
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(
                items = apps,
                key = { it.packageName }
            ) { app ->
                var isFocused by remember { mutableStateOf(false) }
                val targetS = if (isFocused) {
                    config.appBaseScale * config.appFocusScale
                } else {
                    config.appBaseScale
                }
                val scale by animateFloatAsState(
                    targetValue = targetS,
                    animationSpec = tween(durationMillis = 70),
                    label = "appScale"
                )

                val glowColor = remember(config.appGlowMode, config.appCustomGlowHex, app.dominantColor) {
                    when (config.appGlowMode) {
                        0 -> app.dominantColor
                        1 -> try {
                            Color(android.graphics.Color.parseColor(config.appCustomGlowHex))
                        } catch (_: Exception) {
                            Color(0xFF38BDF8)
                        }
                        else -> Color.White
                    }
                }

                val bitmap = remember(app.packageName) {
                    app.bannerOrIcon.toBitmap(150, 86).asImageBitmap()
                }

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .width(136.dp)
                        .height(78.dp)
                        .clip(shape)
                        .background(Color(0xFF1E293B).copy(alpha = config.appCardOpacity))
                        .border(
                            width = if (isFocused) 3.dp else 1.dp,
                            color = if (isFocused) glowColor else Color.White.copy(alpha = 0.15f),
                            shape = shape
                        )
                        .onFocusChanged { isFocused = it.isFocused }
                        .clickable { onLaunchApp(app) },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = app.label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomTickerBar(
    config: LauncherConfig,
    prayerInfo: PalestineData.PrayerDisplay,
    liveFeed: LiveFeedState
) {
    val daysToRamadan = PalestineData.getDaysUntilRamadan()
    val nearestFamily = config.reminders.minByOrNull {
        calculateDaysUntilDate(it.month, it.day)
    }
    val familyText = nearestFamily?.let {
        " • مناسبة ${it.name} بعد ${calculateDaysUntilDate(it.month, it.day)} يوم"
    } ?: ""

    val tickerLine = buildString {
        config.activeTickerPairs.forEach { pair ->
            val info = liveFeed.prices[pair]
                        if (info != null) {
                val arrow = if (info.second) "▲" else "▼"
                append("   |   $pair: ${info.first} $arrow")
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            color = Color.Black.copy(alpha = config.tickerBgOpacity)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(prayerInfo.remainingText, color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    PalestineData.toArabicDigits("رمضان بعد $daysToRamadan يوم$familyText"),
                    color = Color.White,
                    fontSize = 13.sp
                )
                Text(liveFeed.matches.firstOrNull()?.name ?: "", color = Color(0xFFFBBF24), fontSize = 13.sp)
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            color = Color.Black.copy(alpha = config.tickerBgOpacity)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp)) {
                Text(
                    text = "$tickerLine      $tickerLine",
                    color = Color(0xFF34D399),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
            }
        }
    }
}

fun calculateDaysUntilDate(month: Int, day: Int): Int {
    val today = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
        set(Calendar.DAY_OF_MONTH, day.coerceIn(1, 31))
        if (before(today)) add(Calendar.YEAR, 1)
    }
    val diff = target.timeInMillis - today.timeInMillis
    return (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
}


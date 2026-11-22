
package com.palestine.tvlauncher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.palestine.tvlauncher.data.*
import com.palestine.tvlauncher.network.LiveFeedState
import java.util.Calendar

@Composable
fun TopControlBar(
    config: LauncherConfig,
    isRadioPlaying: Boolean,
    hasUpdate: Boolean,
    onToggleRadio: () -> Unit,
    onOpenScreensaver: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWifi: () -> Unit,
    onOpenUpdate: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (config.showTopWifi) {
                PillActionButton(label = "الشبكة", icon = Icons.Default.Wifi, onClick = onOpenWifi)
            }
            if (config.showTopQuranRadio) {
                PillActionButton(
                    label = if (isRadioPlaying) "إيقاف الإذاعة" else "إذاعة القرآن",
                    icon = if (isRadioPlaying) Icons.Default.Stop else Icons.Default.Radio,
                    highlight = isRadioPlaying,
                    onClick = onToggleRadio
                )
            }
            if (config.showTopScreensaver) {
                PillActionButton(label = "شاشة التوقف", icon = Icons.Default.Bedtime, onClick = onOpenScreensaver)
            }
            if (config.showTopVpn) {
                PillActionButton(label = "VPN", icon = Icons.Default.VpnKey, onClick = {})
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (hasUpdate) {
                PillActionButton(
                    label = "تحديث جديد متاح",
                    icon = Icons.Default.SystemUpdate,
                    highlight = true,
                    onClick = onOpenUpdate
                )
            }
            PillActionButton(label = "الإعدادات", icon = Icons.Default.Settings, onClick = onOpenSettings)
        }
    }
}

@Composable
fun PillActionButton(
    label: String,
    icon: ImageVector,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        focused -> Color.White
        highlight -> Color(0xFF10B981).copy(alpha = 0.85f)
        else -> Color.White.copy(alpha = 0.14f)
    }
    val contentColor = if (focused) Color.Black else Color.White

    Surface(
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() },
        shape = CircleShape,
        color = bg,
        border = BorderStroke(1.dp, if (focused) Color.White else Color.White.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
    val cornerDp = (config.appCornerRadiusPercent * 0.5f).dp
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
            items(apps) { app ->
                var isFocused by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(
                    targetValue = if (isFocused) config.appBaseScale * config.appFocusScale else config.appBaseScale,
                    animationSpec = spring(),
                    label = "appScale"
                )

                val glowColor = when (config.appGlowMode) {
                    0 -> app.dominantColor
                    1 -> try {
                        Color(android.graphics.Color.parseColor(config.appCustomGlowHex))
                    } catch (_: Exception) {
                        Color.White
                    }
                    else -> Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .scale(scale)
                        .width(136.dp)
                        .height(78.dp)
                        .shadow(
                            elevation = if (isFocused && config.appGlowMode != 2) 18.dp else 0.dp,
                            shape = shape,
                            ambientColor = glowColor,
                            spotColor = glowColor
                        )
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
                    val bitmap = remember(app.packageName) {
                        app.bannerOrIcon.toBitmap(240, 140).asImageBitmap()
                    }
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

@Composable
fun BottomTickerBar(
    config: LauncherConfig,
    prayerInfo: PalestineData.PrayerDisplay,
    liveFeed: LiveFeedState
) {
    val daysToRamadan = PalestineData.getDaysUntilRamadan()
    val nearestFamily = config.reminders.minByOrNull { calculateDaysUntilDate(it.month, it.day) }
    val familyText = nearestFamily?.let {
        " • مناسبة ${it.name} بعد ${calculateDaysUntilDate(it.month, it.day)} يوم"
    } ?: ""

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            color = Color.Black.copy(alpha = config.tickerBgOpacity)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(prayerInfo.remainingText, color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("رمضان بعد $daysToRamadan يوم$familyText", color = Color.White, fontSize = 13.sp)
                Text(liveFeed.matches.firstOrNull()?.name ?: "", color = Color(0xFFFBBF24), fontSize = 13.sp)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            color = Color.Black.copy(alpha = config.tickerBgOpacity)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                config.activeTickerPairs.forEach { pair ->
                    val rateInfo = liveFeed.prices[pair]
                    if (rateInfo != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$pair: ", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                            Text(
                                text = "${rateInfo.first} ${if (rateInfo.second) "▲" else "▼"}",
                                color = if (rateInfo.second) Color(0xFF34D399) else Color(0xFFF87171),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

fun calculateDaysUntilDate(month: Int, day: Int): Int {
    val today = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.MONTH, (month - 1).coerceIn(0, 11))

package com.palestine.tvlauncher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import com.palestine.tvlauncher.data.TabItem
import com.palestine.tvlauncher.data.cardTabs
import com.palestine.tvlauncher.data.PalestineData
import com.palestine.tvlauncher.data.TvAppInfo
import com.palestine.tvlauncher.network.GitHubUpdateInfo
import com.palestine.tvlauncher.network.LiveFeedState
import java.util.*

@Composable
fun TopControlBar(
    config: LauncherConfig,
    isWifiConnected: Boolean,
    isQuranPlaying: Boolean,
    isQuranBuffering: Boolean,
    updateInfo: GitHubUpdateInfo?,
    onOpenWifi: () -> Unit,
    onToggleQuran: () -> Unit,
    onOpenQuranBook: () -> Unit,
    onOpenScreensaver: () -> Unit,
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
            var toolsExpanded by remember { mutableStateOf(false) }
            Box {
                TopPillButton(
                    icon = Icons.Default.Apps,
                    label = "الأدوات",
                    activeColor = Color(0xFF38BDF8),
                    isHighlighted = false,
                    onClick = { toolsExpanded = true }
                )
                DropdownMenu(expanded = toolsExpanded, onDismissRequest = { toolsExpanded = false }) {
                    if (config.showTopWifi) {
                        DropdownMenuItem(
                            text = { Text(if (isWifiConnected) "متصل" else "غير متصل") },
                            leadingIcon = { Icon(if (isWifiConnected) Icons.Default.Wifi else Icons.Default.WifiOff, contentDescription = null) },
                            onClick = { toolsExpanded = false; onOpenWifi() }
                        )
                    }
                    if (config.showTopQuranRadio) {
                        DropdownMenuItem(
                            text = { Text(when { isQuranBuffering -> "جاري الاتصال..."; isQuranPlaying -> "إذاعة القرآن (يعمل)"; else -> "إذاعة القرآن" }) },
                            leadingIcon = { Icon(if (isQuranPlaying) Icons.Default.GraphicEq else Icons.Default.Radio, contentDescription = null) },
                            onClick = { toolsExpanded = false; onToggleQuran() }
                        )
                    }
                    if (config.showTopQuranBook) {
                        DropdownMenuItem(
                            text = { Text("المصحف الشريف") },
                            leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                            onClick = { toolsExpanded = false; onOpenQuranBook() }
                        )
                    }
                    if (config.showTopScreensaver) {
                        DropdownMenuItem(
                            text = { Text("شاشة التوقف") },
                            leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null) },
                            onClick = { toolsExpanded = false; onOpenScreensaver() }
                        )
                    }
                }
            }
            if (updateInfo?.hasUpdate == true) {
                TopPillButton(
                    icon = Icons.Default.SystemUpdate,
                    label = "تحديث ${updateInfo.latestVersion}",
                    activeColor = Color(0xFFF59E0B),
                    isHighlighted = true,
                    onClick = onTriggerUpdate
                )
            }
        }

        TopPillButton(
            icon = Icons.Default.Settings,
            label = "الإعدادات",
            activeColor = Color.White,
            isHighlighted = false,
            onClick = onOpenSettings
        )
    }
}

@Composable
fun TopPillButton(
    icon: ImageVector,
    label: String,
    activeColor: Color,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = tween(70),
        label = "pill_scale"
    )

    val bgColor = when {
        isFocused -> Color.White
        isHighlighted -> activeColor.copy(alpha = 0.28f)
        else -> Color.Black.copy(alpha = 0.55f)
    }
    val borderColor = when {
        isFocused -> Color.White
        isHighlighted -> activeColor
        else -> Color.White.copy(alpha = 0.22f)
    }

    Surface(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(if (isHighlighted || isFocused) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) Color(0xFF0F172A) else activeColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = if (isFocused) Color(0xFF0F172A) else Color.White,
                fontSize = 13.sp,
                fontWeight = if (isFocused || isHighlighted) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
@Composable
fun PillActionButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = tween(70),
        label = "action_scale"
    )

    Surface(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = CircleShape,
        color = if (isFocused) Color.White else Color.Black.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, if (isFocused) Color.White else Color.White.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) Color(0xFF0F172A) else Color(0xFFFBBF24),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = if (isFocused) Color(0xFF0F172A) else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.Black.copy(alpha = config.dockBgOpacity),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(apps, key = { it.packageName }) { app ->
                TvAppTile(app = app, config = config, onClick = { try { onLaunchApp(app) } catch (_: Exception) {} })
            }
        }
    }
}

@Composable
private fun TvAppTile(app: TvAppInfo, config: LauncherConfig, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) config.appFocusScaleFactor else 1.0f,
        animationSpec = tween(70),
        label = "app_tile_scale"
    )
    val (baseW, baseH) = when (config.appIconSize) {
        0 -> 96f to 60f   // صغير
        2 -> 140f to 90f  // كبير
        else -> 116f to 74f // متوسط
    }
    val tileW = (baseW * config.appBaseScale).dp
    val tileH = (baseH * config.appBaseScale).dp
    val shape = when (config.appIconShape) {
        1 -> CircleShape
        2 -> RectangleShape
        else -> RoundedCornerShape(config.appCornerRadiusPercent.dp)
    }
    val glowAlpha = config.appFocusGlowIntensity.coerceIn(0f, 1f)

    val cachedBitmap: ImageBitmap? = remember(app.packageName) {
        try {
            app.bannerOrIcon.toBitmap(width = 160, height = 100).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(tileW)
            .graphicsLayer {
                scaleX = focusScale
                scaleY = focusScale
                if (isFocused) {
                    shadowElevation = 14.dp.toPx() * glowAlpha
                    spotShadowColor = Color(config.appFocusGlowColor).copy(alpha = glowAlpha)
                }
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(tileW, tileH)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(app.dominantColor.copy(alpha = 0.45f * config.appIconOpacity), Color(0xFF0F172A))
                    )
                )
                .border(
                    width = if (isFocused) 2.5.dp else 1.dp,
                    color = if (isFocused) Color.White else Color.White.copy(alpha = 0.2f),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (cachedBitmap != null) {
                Image(
                    bitmap = cachedBitmap,
                    contentDescription = app.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = config.appIconOpacity }
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = app.label,
            color = if (isFocused) Color.White else Color(0xFFCBD5E1),
            fontSize = 12.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
@OptIn(ExperimentalFoundationApi::class)

@Composable
fun BottomTickerBar(
    config: LauncherConfig,
    prayerInfo: PalestineData.PrayerDisplay,
    liveFeed: LiveFeedState
) {
    val daysToRamadan = remember { PalestineData.getDaysUntilRamadan() }
    val nearestFamily = remember(config.reminders) {
        config.reminders.minByOrNull { calculateDaysUntilDate(it.month, it.day) }
    }
    val familyText = nearestFamily?.let {
        val d = calculateDaysUntilDate(it.month, it.day)
        "   •   مناسبة ${it.name} بعد $d يوم"
    } ?: ""

    val tickerLine = remember(config.activeTickerPairs, liveFeed.prices) {
        buildString {
            config.activeTickerPairs.forEach { pair ->
                val info = liveFeed.prices[pair]
                if (info != null) {
                    val arrow = if (info.second) "▲" else "▼"
                    append("   |   $pair: ${info.first} $arrow")
                }
            }
        }
    }

    val barHeight = config.tickerBarHeight.dp
    val barFontSize = config.tickerBarFontSize.sp
    val barOpacity = config.tickerBgOpacity

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            color = Color.Black.copy(alpha = barOpacity)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(prayerInfo.remainingText, color = Color(0xFF38BDF8), fontSize = barFontSize, fontWeight = FontWeight.Bold)
                Text(
                    PalestineData.toArabicDigits("رمضان بعد $daysToRamadan يوم$familyText"),
                    color = Color.White,
                    fontSize = barFontSize
                )
                Text(liveFeed.matches.firstOrNull()?.name ?: "", color = Color(0xFFFBBF24), fontSize = barFontSize)
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            color = Color.Black.copy(alpha = barOpacity)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp)) {
                Text(
                    text = "$tickerLine      $tickerLine",
                    color = Color(0xFF34D399),
                    fontSize = barFontSize,
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

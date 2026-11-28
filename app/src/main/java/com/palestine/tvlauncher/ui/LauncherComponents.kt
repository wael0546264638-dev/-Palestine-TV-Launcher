package com.palestine.tvlauncher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.palestine.tvlauncher.data.LauncherConfig
import com.palestine.tvlauncher.data.LauncherPreferences
import com.palestine.tvlauncher.data.TabItem
import com.palestine.tvlauncher.data.cardTabs
import com.palestine.tvlauncher.data.PalestineData
import com.palestine.tvlauncher.data.TvAppInfo
import com.palestine.tvlauncher.data.CustomEvent
import com.palestine.tvlauncher.data.Birthday
import com.palestine.tvlauncher.network.GitHubUpdateInfo
import com.palestine.tvlauncher.network.LiveFeedState
import android.content.Intent
import android.net.Uri
import java.util.*
import android.icu.util.IslamicCalendar
import kotlinx.coroutines.launch

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
    onLaunchApp: (TvAppInfo) -> Unit,
    onLongPress: ((TvAppInfo) -> Unit)? = null
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
                TvAppTile(
                    app = app,
                    config = config,
                    displayName = config.appCustomNames[app.packageName] ?: app.label,
onClick = { try { onLaunchApp(app) } catch (_: Exception) {} },
                onLongClick = { onLongPress?.invoke(app) }
                )
            }
        }
    }
}

@Composable
private fun TvAppTile(
    app: TvAppInfo,
    config: LauncherConfig,
    displayName: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
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
            .combinedClickable(interactionSource = interactionSource, indication = null, onClick = onClick, onLongClick = onLongClick)
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
            text = displayName,
            color = if (isFocused) Color.White else Color(0xFFCBD5E1),
            fontSize = 12.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DeviceAppsGrid(
    apps: List<TvAppInfo>,
    config: LauncherConfig,
    onLaunchApp: (TvAppInfo) -> Unit,
    onLongPress: (TvAppInfo) -> Unit,
    columns: Int = 5
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxWidth().height(500.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(apps, key = { it.packageName }) { app ->
            TvAppTile(
                app = app,
                config = config,
                displayName = config.appCustomNames[app.packageName] ?: app.label,
                onClick = { try { onLaunchApp(app) } catch (_: Exception) {} },
                onLongClick = { onLongPress(app) }
            )
        }
    }
}

@Composable
fun AppLongPressMenu(
    app: TvAppInfo,
    config: LauncherConfig,
    prefs: LauncherPreferences,
    scope: kotlinx.coroutines.CoroutineScope,
    favoriteApps: Set<String>,
    currentOrder: List<String>,
    onDismiss: () -> Unit,
    onOrderChanged: (List<String>) -> Unit
) {
    val context = LocalContext.current
    val isFavorite = app.packageName in favoriteApps
    val currentName = config.appCustomNames[app.packageName] ?: app.label
    var editName by remember { mutableStateOf(currentName) }
    var renameFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.72f).height(460.dp).padding(20.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0B1324),
            border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(app.label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(app.packageName, color = Color(0xFF94A3B8), fontSize = 13.sp, modifier = Modifier.padding(bottom = 16.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        SettingOptionPill(
                            text = if (isFavorite) "إزالة من المفضلة" else "إضافة إلى المفضلة",
                            selected = isFavorite,
                            onClick = { scope.launch { prefs.toggleFavoriteApp(app.packageName, !isFavorite) }; onDismiss() }
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingOptionPill("تقديم", false) {
                                onOrderChanged(moveItem(currentOrder, app.packageName, -1)); onDismiss()
                            }
                            SettingOptionPill("تأخير", false) {
                                onOrderChanged(moveItem(currentOrder, app.packageName, 1)); onDismiss()
                            }
                        }
                    }
                    item {
                        SettingOptionPill("تعديل الاسم", false) {
                            editName = currentName
                            renameFocus = true
                            focusRequester.requestFocus()
                        }
                    }
                    if (renameFocus) {
                        item {
                            Column {
                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    label = { Text("الاسم المخصص") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { focusState -> renameFocus = focusState.isFocused },
                                    singleLine = true
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SettingOptionPill("حفظ", false) {
                                        val names = config.appCustomNames.toMutableMap()
                                        if (editName.isBlank() || editName == app.label) names.remove(app.packageName) else names[app.packageName] = editName
                                        scope.launch { prefs.saveAppCustomNames(names) }
                                        renameFocus = false
                                        onDismiss()
                                    }
                                    SettingOptionPill("إلغاء", false) { renameFocus = false }
                                }
                            }
                        }
                    }
                    item {
                        SettingOptionPill(
                            text = "حذف التطبيق (إلغاء التثبيت)",
                            selected = false,
                            onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DELETE)
                                            .setData(Uri.parse("package:${app.packageName}"))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } catch (_: Exception) {}
                                onDismiss()
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                SettingOptionPill("إغلاق", false, onClick = onDismiss)
            }
        }
    }
}

private fun moveItem(order: List<String>, packageName: String, delta: Int): List<String> {
    val index = order.indexOf(packageName)
    if (index < 0) return order
    val target = (index + delta).coerceIn(0, order.lastIndex)
    if (target == index) return order
    val result = order.toMutableList()
    result.removeAt(index)
    result.add(target, packageName)
    return result
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
    val nearestBirthday = remember(config.reminders) {
        config.reminders.minByOrNull { calculateDaysUntilDate(it.month, it.day) }
    }
    val birthdayText = nearestBirthday?.let {
        val d = calculateDaysUntilDate(it.month, it.day)
        "   •   عيد ميلاد ${it.name} بعد $d يوم"
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
                    PalestineData.toArabicDigits("رمضان بعد $daysToRamadan يوم$familyText$birthdayText"),
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

data class IslamicEvent(
    val name: String,
    val hijriMonth: Int,
    val hijriDay: Int,
    val daysUntil: Int
)

data class NextEventInfo(
    val event: IslamicEvent,
    val isCustom: Boolean
)

fun getBuiltInIslamicEvents(): List<IslamicEvent> {
    val today = Calendar.getInstance()
    val hijri = IslamicCalendar().apply { timeZone = android.icu.util.TimeZone.getDefault() }
    val currentHijriYear = hijri.get(IslamicCalendar.YEAR)
    val currentHijriMonth = hijri.get(IslamicCalendar.MONTH)
    val currentHijriDay = hijri.get(IslamicCalendar.DAY_OF_MONTH)

    val builtInEvents = listOf(
        "رأس السنة الهجرية" to Pair(IslamicCalendar.MUHARRAM, 1),
        "الإسراء والمعراج" to Pair(IslamicCalendar.RAJAB, 27),
        "رمضان المبارك" to Pair(IslamicCalendar.RAMADAN, 1),
        "عيد الفطر" to Pair(IslamicCalendar.SHAWWAL, 1),
        "عيد الأضحى" to Pair(IslamicCalendar.DHU_AL_HIJJAH, 10)
    )

    return builtInEvents.map { (name, monthDay) ->
        val (month, day) = monthDay
        val eventCal = IslamicCalendar().apply {
            set(IslamicCalendar.YEAR, currentHijriYear)
            set(IslamicCalendar.MONTH, month)
            set(IslamicCalendar.DAY_OF_MONTH, day)
        }
        val currentCal = IslamicCalendar().apply { timeZone = android.icu.util.TimeZone.getDefault() }
        val daysDiff = calculateHijriDaysDiff(currentCal, eventCal)
        IslamicEvent(name, month, day, if (daysDiff < 0) daysDiff + 354 else daysDiff)
    }
}

private fun calculateHijriDaysDiff(from: IslamicCalendar, to: IslamicCalendar): Int {
    val fromCopy = from.clone() as IslamicCalendar
    var days = 0
    while (fromCopy.before(to) && days < 400) {
        fromCopy.add(IslamicCalendar.DAY_OF_MONTH, 1)
        days++
    }
    if (days >= 400) {
        fromCopy.set(IslamicCalendar.YEAR, fromCopy.get(IslamicCalendar.YEAR) + 1)
        while (fromCopy.before(to) && days < 700) {
            fromCopy.add(IslamicCalendar.DAY_OF_MONTH, 1)
            days++
        }
    }
    return days
}

fun getNextIslamicEvent(customEvents: List<CustomEvent>): NextEventInfo? {
    val builtIn = getBuiltInIslamicEvents()
    val allEvents = mutableListOf<Pair<IslamicEvent, Boolean>>()
    allEvents.addAll(builtIn.map { it to false })
    allEvents.addAll(customEvents.filter { it.isRecurring }.map {
        val today = Calendar.getInstance()
        val hijri = IslamicCalendar().apply { timeZone = android.icu.util.TimeZone.getDefault() }
        val currentHijriYear = hijri.get(IslamicCalendar.YEAR)
        val eventCal = IslamicCalendar().apply {
            set(IslamicCalendar.YEAR, currentHijriYear)
            set(IslamicCalendar.MONTH, it.hijriMonth)
            set(IslamicCalendar.DAY_OF_MONTH, it.hijriDay)
        }
        val currentCal = IslamicCalendar().apply { timeZone = android.icu.util.TimeZone.getDefault() }
        val daysDiff = calculateHijriDaysDiff(currentCal, eventCal)
        IslamicEvent(it.name, it.hijriMonth, it.hijriDay, if (daysDiff < 0) daysDiff + 354 else daysDiff) to true
    })

    return allEvents
        .filter { it.first.daysUntil >= 0 }
        .minByOrNull { it.first.daysUntil }
        ?.let { NextEventInfo(it.first, it.second) }
}

fun getNextBirthday(birthdays: List<Birthday>): Pair<Birthday, Int>? {
    return birthdays
        .mapNotNull { b ->
            val days = calculateDaysUntilDate(b.month, b.day)
            if (days >= 0) b to days else null
        }
        .minByOrNull { it.second }
}

data class NationalEvent(val name: String, val month: Int, val day: Int, val daysUntil: Int)

fun getBuiltInNationalEvents(): List<NationalEvent> {
    val events = listOf(
        "رأس السنة الميلادية" to Pair(1, 1),
        "عيد العمال" to Pair(5, 1),
        "يوم الأرض (فلسطين)" to Pair(3, 30),
        "يوم النكبة" to Pair(5, 15),
        "يوم الاستقلال (فلسطين)" to Pair(11, 15),
        "عيد الميلاد" to Pair(12, 25)
    )
    return events.map { (name, monthDay) ->
        val (m, d) = monthDay
        NationalEvent(name, m, d, calculateDaysUntilDate(m, d))
    }
}

fun getNextNationalEvent(): Pair<NationalEvent, Int>? {
    return getBuiltInNationalEvents()
        .mapNotNull { e -> if (e.daysUntil >= 0) e to e.daysUntil else null }
        .minByOrNull { it.second }
}

@Composable
fun BottomEventsBar(
    config: LauncherConfig,
    prayerInfo: PalestineData.PrayerDisplay
) {
    if (!config.bottomBarEnabled) return

    val nextReligious = remember(config.customEvents) {
        getNextIslamicEvent(config.customEvents)
    }
    val nextNational = remember {
        getNextNationalEvent()
    }
    val nextBirthday = remember(config.birthdays) {
        getNextBirthday(config.birthdays)
    }

    val barHeight = config.bottomBarHeight.dp
    val barFontSize = config.bottomBarFontSize.sp
    val barOpacity = config.bottomBarOpacity

    val religiousText = nextReligious?.let { info ->
        val event = info.event
        val label = if (info.isCustom) "📌 ${event.name}" else "🕌 ${event.name}"
        PalestineData.toArabicDigits("$label: بعد ${event.daysUntil} يوم")
    } ?: PalestineData.toArabicDigits("لا توجد مناسبات دينية قادمة")

    val nationalText = nextNational?.let { (e, days) ->
        PalestineData.toArabicDigits("🇵🇸 ${e.name}: بعد $days يوم")
    } ?: PalestineData.toArabicDigits("لا توجد مناسبات وطنية قادمة")

    val birthdayText = nextBirthday?.let { (b, days) ->
        PalestineData.toArabicDigits("🎂 ${b.name}: بعد $days يوم")
    } ?: PalestineData.toArabicDigits("لا توجد أعياد ميلاد قادمة")

    val prayerText = if (config.bottomBarShowNextPrayer) prayerInfo.remainingText else ""

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight),
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = barOpacity),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (config.bottomBarShowNextPrayer && prayerText.isNotBlank()) {
                    Text(
                        text = prayerText,
                        color = Color(0xFF38BDF8),
                        fontSize = barFontSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (config.bottomBarShowReligiousEvents) {
                    Text(
                        text = religiousText,
                        color = Color.White,
                        fontSize = barFontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 16.dp)
                    )
                }
                if (config.bottomBarShowNationalEvents) {
                    Text(
                        text = nationalText,
                        color = Color(0xFF34D399),
                        fontSize = barFontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 16.dp)
                    )
                }
                if (config.bottomBarShowBirthdayCounter) {
                    Text(
                        text = birthdayText,
                        color = Color(0xFFFBBF24),
                        fontSize = barFontSize,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}

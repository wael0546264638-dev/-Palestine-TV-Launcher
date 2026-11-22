package com.palestine.tvlauncher.ui

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.palestine.tvlauncher.data.*
import com.palestine.tvlauncher.network.LiveFeedState
import com.palestine.tvlauncher.network.LiveRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InteractiveTabsCard(
    modifier: Modifier = Modifier,
    config: LauncherConfig,
    prayerInfo: PalestineData.PrayerDisplay,
    liveFeed: LiveFeedState,
    hijriDate: String,
    gregorianDate: String,
    onAddTask: (String) -> Unit,
    onRemoveTask: (String) -> Unit,
    onAddReminder: (FamilyReminder) -> Unit
) {
    val tabTitles = listOf(
        "مواقيت الصلاة", "طقس الأيام", "المباريات", "التقويم المزدوج",
        "آية اليوم", "حديث اليوم", "الأذكار", "أخبار فلسطين",
        "الأسواق", "المناسبات", "حالة الجهاز", "المهام", "التذكيرات"
    )
    var selectedTab by remember { mutableIntStateOf(0) }
    var expandedTab by remember { mutableStateOf<Int?>(null) }
    var isAnyTabFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(config.autoRotateTabs, isAnyTabFocused, expandedTab) {
        if (config.autoRotateTabs && !isAnyTabFocused && expandedTab == null) {
            while (true) {
                delay(10_000L)
                if (!isAnyTabFocused && expandedTab == null) {
                    selectedTab = (selectedTab + 1) % tabTitles.size
                }
            }
        }
    }

    if (expandedTab != null) {
        AlertDialog(
            onDismissRequest = { expandedTab = null },
            confirmButton = {
                TextButton(onClick = { expandedTab = null }) { Text("إغلاق") }
            },
            title = {
                Text(tabTitles[expandedTab!!], fontWeight = FontWeight.Bold, fontSize = 22.sp)
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (expandedTab) {
                        0 -> items(listOf(
                            "الفجر: ${prayerInfo.fajr}",
                            "الشروق: ${prayerInfo.sunrise}",
                            "الظهر: ${prayerInfo.dhuhr}",
                            "العصر: ${prayerInfo.asr}",
                            "المغرب: ${prayerInfo.maghrib}",
                            "العشاء: ${prayerInfo.isha}",
                            prayerInfo.remainingText
                        )) { Text(it, fontSize = 18.sp) }
                        1 -> items(liveFeed.dailyMax.indices.toList()) { idx ->
                            val mx = liveFeed.dailyMax[idx]
                            val mn = liveFeed.dailyMin.getOrElse(idx) { 15 }
                            Text(
                                PalestineData.toArabicDigits("اليوم +${idx + 1}: العظمى $mx° / الصغرى $mn°"),
                                fontSize = 18.sp
                            )
                        }
                        2 -> items(liveFeed.matches) {
                            Text("${it.name}  (${it.status})", fontSize = 17.sp)
                        }
                        4 -> items(PalestineData.dailyVerses) { Text(it, fontSize = 18.sp) }
                        5 -> items(PalestineData.dailyHadiths) { Text(it, fontSize = 17.sp) }
                        6 -> items(PalestineData.adhkarList) { Text("• $it", fontSize = 16.sp) }
                        7 -> items(liveFeed.news) { Text("▪ $it", fontSize = 17.sp) }
                        8 -> items(liveFeed.prices.toList()) { (k, v) ->
                            Text("$k : ${v.first} ${if (v.second) "▲" else "▼"}", fontSize = 18.sp)
                        }
                        else -> item {
                            Text("$hijriDate  |  $gregorianDate", fontSize = 18.sp)
                        }
                    }
                }
            }
        )
    }

    Surface(
        modifier = modifier.onFocusChanged { isAnyTabFocused = it.hasFocus },
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0F172A).copy(alpha = config.cardBgOpacity),
        border = BorderStroke(
            width = if (isAnyTabFocused) 2.dp else 1.dp,
            color = if (isAnyTabFocused) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.18f)
        )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tabTitles.size) { index ->
                    val isSelected = selectedTab == index
                    var isFocused by remember { mutableStateOf(false) }
                    Surface(
                        modifier = Modifier
                            .onFocusChanged {
                                isFocused = it.isFocused
                                if (it.isFocused) {
                                    isAnyTabFocused = true
                                    selectedTab = index
                                }
                            }
                            .clickable {
                                if (selectedTab == index) expandedTab = index
                                else selectedTab = index
                            },
                        shape = CircleShape,
                        color = when {
                            isFocused -> Color.White
                            isSelected -> Color(0xFF38BDF8).copy(alpha = 0.38f)
                            else -> Color.White.copy(alpha = 0.08f)
                        },
                        border = BorderStroke(
                            width = if (isFocused) 2.5.dp else 1.dp,
                            color = if (isFocused) Color(0xFFFBBF24) else Color.Transparent
                        )
                    ) {
                        Text(
                            text = tabTitles[index],
                            color = if (isFocused) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable { expandedTab = selectedTab }
            ) {
                when (selectedTab) {
                    0 -> {
                        val rows = listOf(
                            "الفجر" to prayerInfo.fajr,
                            "الشروق" to prayerInfo.sunrise,
                            "الظهر" to prayerInfo.dhuhr,
                            "العصر" to prayerInfo.asr,
                            "المغرب" to prayerInfo.maghrib,
                            "العشاء" to prayerInfo.isha
                        )
                        Column(
                            verticalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            rows.forEach { (name, time) ->
                                val isNext = name == prayerInfo.nextPrayerName
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isNext) Color(0xFF38BDF8).copy(alpha = 0.25f)
                                            else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name, color = Color.White)
                                    Text(time, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    1 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("توقعات الطقس في ${config.cityName}:", color = Color(0xFF38BDF8))
                            liveFeed.dailyMax.forEachIndexed { idx, maxT ->
                                val minT = liveFeed.dailyMin.getOrElse(idx) { 15 }
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
@Composable
fun LiveLayoutEditorOverlay(
    config: LauncherConfig,
    onSave: (Float, Float, Float, Float, Float, Float, Float, Float, Float) -> Unit,
    onCancel: () -> Unit
) {
    var selectedElement by remember { mutableIntStateOf(0) }
    val names = listOf("الساعة", "البطاقة", "التطبيقات")
    var cx by remember { mutableFloatStateOf(config.clockOffsetX) }
    var cy by remember { mutableFloatStateOf(config.clockOffsetY) }
    var cs by remember { mutableFloatStateOf(config.clockScale) }
    var kx by remember { mutableFloatStateOf(config.cardOffsetX) }
    var ky by remember { mutableFloatStateOf(config.cardOffsetY) }
    var ks by remember { mutableFloatStateOf(config.cardScale) }
    var ko by remember { mutableFloatStateOf(config.cardBgOpacity) }
    var dx by remember { mutableFloatStateOf(config.dockOffsetX) }
    var dy by remember { mutableFloatStateOf(config.dockOffsetY) }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xEE0F172A),
        border = BorderStroke(2.dp, Color(0xFFFBBF24))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("محرر التخطيط المباشر بالريموت", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                names.forEachIndexed { idx, title ->
                    SettingOptionPill(
                        label = title,
                        selected = selectedElement == idx,
                        onClick = { selectedElement = idx }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingOptionPill("فوق", false) {
                    when (selectedElement) { 0 -> cy -= 15f; 1 -> ky -= 15f; 2 -> dy -= 15f }
                }
                SettingOptionPill("تحت", false) {
                    when (selectedElement) { 0 -> cy += 15f; 1 -> ky += 15f; 2 -> dy += 15f }
                }
                SettingOptionPill("يمين", false) {
                    when (selectedElement) { 0 -> cx += 15f; 1 -> kx += 15f; 2 -> dx += 15f }
                }
                SettingOptionPill("يسار", false) {
                    when (selectedElement) { 0 -> cx -= 15f; 1 -> kx -= 15f; 2 -> dx -= 15f }
                }
                SettingOptionPill("تكبير", false) {
                    if (selectedElement == 0) cs += 0.05f
                    if (selectedElement == 1) ks += 0.05f
                }
                SettingOptionPill("تصغير", false) {
                    if (selectedElement == 0) cs -= 0.05f
                    if (selectedElement == 1) ks -= 0.05f
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingOptionPill("حفظ التخطيط", true) {
                    onSave(cx, cy, cs, kx, ky, ks, ko, dx, dy)
                }
                SettingOptionPill("إلغاء", false, onClick = onCancel)
            }
        }
    }
}

@Composable
fun ScreensaverView(
    timeZoneId: String,
    hijri: String,
    gregorian: String,
    prayerInfo: PalestineData.PrayerDisplay,
    config: LauncherConfig,
    isEditMode: Boolean,
    onSaveOffsets: (Float, Float, Float) -> Unit,
    onExit: () -> Unit
) {
    var ox by remember { mutableFloatStateOf(config.ssClockOffsetX) }
    var oy by remember { mutableFloatStateOf(config.ssClockOffsetY) }
    var sc by remember { mutableFloatStateOf(config.ssClockScale) }
    var ssTime by remember { mutableStateOf("") }

    LaunchedEffect(timeZoneId) {
        val tz = TimeZone.getTimeZone(timeZoneId)
        val sdf = SimpleDateFormat("hh:mm a", Locale("ar")).apply { timeZone = tz }
        while (true) {
            ssTime = PalestineData.toArabicDigits(sdf.format(Date()))
            delay(1000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { if (!isEditMode) onExit() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                translationX = ox
                translationY = oy
                scaleX = sc
                scaleY = sc
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(ssTime, fontSize = 96.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text("$hijri  •  $gregorian", fontSize = 20.sp, color = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.height(12.dp))
            Text(prayerInfo.remainingText, fontSize = 18.sp, color = Color(0xFF38BDF8))
        }
        if (isEditMode) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingOptionPill("فوق", false) { oy -= 20f }
                SettingOptionPill("تحت", false) { oy += 20f }
                SettingOptionPill("تكبير", false) { sc += 0.1f }
                SettingOptionPill("تصغير", false) { sc -= 0.1f }
                SettingOptionPill("حفظ", true) { onSaveOffsets(ox, oy, sc) }
            }
        }
    }
}

@Composable
private fun SettingOptionPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() },
        shape = CircleShape,
        color = when {
            isFocused -> Color.White
            selected -> Color(0xFF38BDF8).copy(alpha = 0.35f)
            else -> Color.White.copy(alpha = 0.10f)
        },
        border = BorderStroke(
            width = if (isFocused) 2.5.dp else if (selected) 1.5.dp else 1.dp,
            color = when {
                isFocused -> Color(0xFFFBBF24)
                selected -> Color(0xFF38BDF8)
                else -> Color.White.copy(alpha = 0.20f)
            }
        )
    ) {
        Text(
            text = label,
            color = if (isFocused) Color.Black else Color.White,
            fontSize = 13.sp,
            fontWeight = if (isFocused || selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun StepAdjustRow(
    title: String,
    valueLabel: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$title: $valueLabel", color = Color.White, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingOptionPill(" - ", false, onClick = onDecrease)
            SettingOptionPill(" + ", false, onClick = onIncrease)
        }
    }
}

@Composable
private fun SettingsCategoryBox(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.72f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, color = Color(0xFFFBBF24), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
fun SettingsModalDrawer(
    config: LauncherConfig,
    prefs: LauncherPreferences,
    onOpenLiveEditor: () -> Unit,
    onOpenScreensaverEditor: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<PalestinianCity>>(emptyList()) }
    var locationStatus by remember { mutableStateOf("") }
    val allPairs = listOf("USD/ILS", "JOD/ILS", "EUR/ILS", "GOLD", "BRENT", "BTC", "ETH")

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.78f)),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            modifier = Modifier.fillMaxHeight().width(540.dp),
            color = Color(0xFF0F172A)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الإعدادات المنظمة", fontSize = 21.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        SettingOptionPill("إغلاق القائمة", false, onClick = onDismiss)
                    }
                }

                // الفئة 1: الموقع الجغرافي ومواقيت الصلاة
                item {
                    SettingsCategoryBox("١. الموقع الجغرافي والتوقيت ومواقيت الصلاة") {
                        Text(
                            "الموقع الحالي: ${config.cityName} (${config.cityTimeZone})",
                            color = Color(0xFF38BDF8),
                            fontSize = 13.sp
                        )
                        if (locationStatus.isNotEmpty()) {
                            Text(locationStatus, color = Color(0xFF34D399), fontSize = 12.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

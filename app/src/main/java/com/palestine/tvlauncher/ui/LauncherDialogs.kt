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
                                                                    ) {
                                    Text(PalestineData.toArabicDigits("اليوم +${idx + 1}"), color = Color.White)
                                    Text(PalestineData.toArabicDigits("العظمى $maxT° / الصغرى $minT°"), color = Color(0xFFFBBF24))
                                }
                            }
                        }
                    }
                    2 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(liveFeed.matches) { m ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(m.name, color = Color.White)
                                Text(m.status, color = Color(0xFF34D399))
                            }
                        }
                    }
                    3 -> Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("التقويم الهجري", color = Color(0xFF38BDF8), fontSize = 14.sp)
                        Text(hijriDate, color = Color.White, fontSize = 22.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("التقويم الميلادي", color = Color(0xFF38BDF8), fontSize = 14.sp)
                        Text(gregorianDate, color = Color.White, fontSize = 20.sp)
                    }
                    4 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(PalestineData.dailyVerses.first(), color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center)
                    }
                    5 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(PalestineData.dailyHadiths.first(), color = Color.White, fontSize = 17.sp, textAlign = TextAlign.Center)
                    }
                    6 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(PalestineData.adhkarList) { item -> Text("• $item", color = Color.White, fontSize = 14.sp) }
                    }
                    7 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(liveFeed.news) { headline ->
                            Text("▪ $headline", color = Color.White, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    8 -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        liveFeed.prices.forEach { (pair, info) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(pair, color = Color.White)
                                Text("${info.first} ${if (info.second) "▲" else "▼"}", color = if (info.second) Color(0xFF34D399) else Color(0xFFF87171))
                            }
                        }
                    }
                    9 -> {
                        val daysToRamadan = PalestineData.getDaysUntilRamadan()
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("المناسبات الإسلامية:", color = Color(0xFF38BDF8))
                            Text(PalestineData.toArabicDigits("• رمضان المبارك: بعد $daysToRamadan يوم"), color = Color.White)
                            Text(PalestineData.toArabicDigits("• عيد الفطر: بعد ${daysToRamadan + 30} يوم"), color = Color(0xFFCBD5E1))
                        }
                    }
                    10 -> {
                        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                        val mi = ActivityManager.MemoryInfo()
                        am.getMemoryInfo(mi)
                        val avail = mi.availMem / (1024 * 1024)
                        val total = mi.totalMem / (1024 * 1024)
                        val stat = StatFs(Environment.getDataDirectory().path)
                        val gb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024 * 1024)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("حالة جهاز TV Box:", color = Color(0xFF38BDF8))
                            Text(PalestineData.toArabicDigits("الذاكرة المتاحة: $avail MB / $total MB"), color = Color.White)
                            Text(PalestineData.toArabicDigits("المساحة المتبقية: $gb GB"), color = Color.White)
                        }
                    }
                    11 -> {
                        var taskInput by remember { mutableStateOf("") }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = taskInput,
                                    onValueChange = { taskInput = it },
                                    placeholder = { Text("أضف مهمة...", color = Color.Gray) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Button(onClick = {
                                    if (taskInput.isNotBlank()) { onAddTask(taskInput.trim()); taskInput = "" }
                                }) { Text("إضافة") }
                            }
                            LazyColumn {
                                items(config.tasks) { t ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("✓ $t", color = Color.White)
                                        TextButton(onClick = { onRemoveTask(t) }) { Text("حذف", color = Color(0xFFF87171)) }
                                    }
                                }
                            }
                        }
                    }
                    12 -> {
                        var nameIn by remember { mutableStateOf("") }
                        var dayIn by remember { mutableStateOf("") }
                        var monthIn by remember { mutableStateOf("") }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(value = nameIn, onValueChange = { nameIn = it }, placeholder = { Text("الاسم", color = Color.Gray) }, modifier = Modifier.weight(1.4f), singleLine = true)
                                OutlinedTextField(value = dayIn, onValueChange = { dayIn = it }, placeholder = { Text("يوم", color = Color.Gray) }, modifier = Modifier.weight(0.6f), singleLine = true)
                                OutlinedTextField(value = monthIn, onValueChange = { monthIn = it }, placeholder = { Text("شهر", color = Color.Gray) }, modifier = Modifier.weight(0.6f), singleLine = true)
                                Button(onClick = {
                                    val d = dayIn.toIntOrNull(); val m = monthIn.toIntOrNull()
                                    if (nameIn.isNotBlank() && d != null && m != null) {
                                        onAddReminder(FamilyReminder(UUID.randomUUID().toString(), nameIn.trim(), m, d))
                                        nameIn = ""; dayIn = ""; monthIn = ""
                                    }
                                }) { Text("حفظ") }
                            }
                            LazyColumn {
                                items(config.reminders) { r ->
                                    val left = calculateDaysUntilDate(r.month, r.day)
                                    Text(PalestineData.toArabicDigits("🎂 ${r.name} (${r.day}/${r.month}) - متبقي $left يوم"), color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Center) {
                repeat(tabTitles.size) { idx ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (idx == selectedTab) 8.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (idx == selectedTab) Color.White else Color.White.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

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
                            SettingOptionPill("تعرف تلقائي على موقعي", false) {
                                locationStatus = "جاري تحديد الموقع..."
                                scope.launch {
                                    val detected = LiveRepository.autoDetectLocation()
                                    if (detected != null) {
                                        prefs.setLocation(detected)
                                        locationStatus = "تم الضبط: ${detected.name} (${detected.country})"
                                    } else {
                                        locationStatus = "تعذر التحديد التلقائي، اختر من القائمة"
                                    }
                                }
                            }
                            SettingOptionPill("أريحا (الزبيدات)", config.cityName == "أريحا (الزبيدات)") {
                                scope.launch {
                                    prefs.setLocation(PalestineData.palestineCities.first())
                                    locationStatus = "تمت الاستعادة إلى أريحا (الزبيدات)"
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("ابحث عن مدينة أو قرية...", color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            SettingOptionPill("بحث", false) {
                                if (searchQuery.isNotBlank()) {
                                    scope.launch {
                                        searchResults = LiveRepository.searchLocationByName(searchQuery)
                                    }
                                }
                            }
                        }

                        if (searchResults.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(searchResults) { res ->
                                    SettingOptionPill("${res.name} (${res.country})", false) {
                                        scope.launch {
                                            prefs.setLocation(res)
                                            locationStatus = "تم اختيار: ${res.name}"
                                            searchResults = emptyList()
                                        }
                                    }
                                }
                            }
                        }

                        Text("مدن وقرى فلسطين:", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(PalestineData.palestineCities) { city ->
                                SettingOptionPill(city.name, config.cityName == city.name) {
                                    scope.launch { prefs.setLocation(city) }
                                }
                            }
                        }

                        Text("البلدان والعواصم العالمية:", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(PalestineData.worldCapitals) { city ->
                                SettingOptionPill("${city.name} (${city.country})", config.cityName == city.name) {
                                    scope.launch { prefs.setLocation(city) }
                                }
                            }
                        }
                    }
                }

                // الفئة 2: أشكال الساعة والتاريخ
                item {
                    SettingsCategoryBox("٢. أشكال الساعة والتاريخ") {
                        val styles = listOf("رقمي عصري", "بطاقة زجاجية", "عقارب دائرية", "عمودي كبير")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(styles.size) { idx ->
                                SettingOptionPill(styles[idx], config.clockStyle == idx) {
                                    scope.launch {
                                        prefs.updateConfig { it[LauncherPreferences.CLOCK_STYLE] = idx }
                                    }
                                }
                            }
                        }
                        val dateModes = listOf("هجري وميلادي", "هجري فقط", "ميلادي فقط", "إخفاء التاريخ")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(dateModes.size) { idx ->
                                SettingOptionPill(dateModes[idx], config.dateDisplayMode == idx) {
                                    scope.launch {
                                        prefs.updateConfig { it[LauncherPreferences.DATE_MODE] = idx }
                                    }
                                }
                            }
                        }
                    }
                }
                // الفئة 3: إذاعة القرآن الكريم
                item {
                    SettingsCategoryBox("٣. إذاعة القرآن الكريم") {
                        val modes = listOf("يدوي / دائم", "الشاشة الرئيسية فقط", "شاشة التوقف فقط", "تشغيل مجدول")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(modes.size) { idx ->
                                SettingOptionPill(modes[idx], config.quranPlayMode == idx) {
                                    scope.launch {
                                        prefs.updateConfig { it[LauncherPreferences.QURAN_PLAY_MODE] = idx }
                                    }
                                }
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(PalestineData.quranRadios) { (title, url) ->
                                SettingOptionPill(title, config.quranStreamUrl == url) {
                                    scope.launch {
                                        prefs.updateConfig { it[LauncherPreferences.QURAN_URL] = url }
                                    }
                                }
                            }
                        }
                        if (config.quranPlayMode == 3) {
                            StepAdjustRow(
                                title = "ساعة البداية",
                                valueLabel = PalestineData.toArabicDigits("${config.quranScheduleStartHour}:00"),
                                onDecrease = {
                                    scope.launch {
                                        prefs.updateConfig {
                                            it[LauncherPreferences.QURAN_SCHED_START] =
                                                (config.quranScheduleStartHour - 1 + 24) % 24
                                        }
                                    }
                                },
                                onIncrease = {
                                    scope.launch {
                                        prefs.updateConfig {
                                            it[LauncherPreferences.QURAN_SCHED_START] =
                                                (config.quranScheduleStartHour + 1) % 24
                                        }
                                    }
                                }
                            )
                            StepAdjustRow(
                                title = "ساعة الإيقاف",
                                valueLabel = PalestineData.toArabicDigits("${config.quranScheduleEndHour}:00"),
                                onDecrease = {
                                    scope.launch {
                                        prefs.updateConfig {
                                            it[LauncherPreferences.QURAN_SCHED_END] =
                                                (config.quranScheduleEndHour - 1 + 24) % 24
                                        }
                                    }
                                },
                                onIncrease = {
                                    scope.launch {
                                        prefs.updateConfig {
                                            it[LauncherPreferences.QURAN_SCHED_END] =
                                                (config.quranScheduleEndHour + 1) % 24
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // الفئة 4: الخلفيات والشفافية والأحجام
                item {
                    SettingsCategoryBox("٤. الخلفيات والشفافية والأحجام") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(LiveRepository.natureWallpapers.size) { idx ->
                                val title = LiveRepository.natureWallpapers[idx].first
                                SettingOptionPill(title, config.wallpaperIndex == idx) {
                                    scope.launch {
                                        prefs.updateConfig { it[LauncherPreferences.WALLPAPER_IDX] = idx }
                                    }
                                }
                            }
                        }
                        StepAdjustRow(
                            title = "شفافية البطاقة",
                            valueLabel = PalestineData.toArabicDigits("${(config.cardBgOpacity * 100).toInt()}%"),
                            onDecrease = {
                                scope.launch {
                                    prefs.updateConfig {
                                        it[LauncherPreferences.CARD_OPACITY] = (config.cardBgOpacity - 0.1f).coerceIn(0.1f, 1f)
                                    }
                                }
                            },
                            onIncrease = {
                                scope.launch {
                                    prefs.updateConfig {
                                        it[LauncherPreferences.CARD_OPACITY] = (config.cardBgOpacity + 0.1f).coerceIn(0.1f, 1f)
                                    }
                                }
                            }
                        )
                        StepAdjustRow(
                            title = "شفافية شريط التطبيقات",
                            valueLabel = PalestineData.toArabicDigits("${(config.dockBgOpacity * 100).toInt()}%"),
                            onDecrease = {
                                scope.launch {
                                    prefs.updateConfig {
                                        it[LauncherPreferences.DOCK_OPACITY] = (config.dockBgOpacity - 0.1f).coerceIn(0f, 1f)
                                    }
                                }
                            },
                            onIncrease = {
                                scope.launch {
                                    prefs.updateConfig {
                                        it[LauncherPreferences.DOCK_OPACITY] = (config.dockBgOpacity + 0.1f).coerceIn(0f, 1f)
                                    }
                                }
                            }
                        )
                        StepAdjustRow(
                            title = "حجم أيقونات التطبيقات",
                            valueLabel = PalestineData.toArabicDigits("${(config.appBaseScale * 100).toInt()}%"),
                            onDecrease = {
                                scope.launch {
                                    prefs.updateConfig {
                                        it[LauncherPreferences.APP_SCALE] = (config.appBaseScale - 0.05f).coerceIn(0.75f, 1.3f)
                                    }
                                }
                            },
                            onIncrease = {
                                scope.launch {
                                    prefs.updateConfig {
                                        it[LauncherPreferences.APP_SCALE] = (config.appBaseScale + 0.05f).coerceIn(0.75f, 1.3f)
                                    }
                                }
                            }
                        )
                        StepAdjustRow(
                            title = "استدارة حواف التطبيقات",
                            valueLabel = PalestineData.toArabicDigits("${config.appCornerRadiusPercent}%"),
                            onDecrease = {
                                scope.launch {
                                    prefs.updateConfig {
                                        it[LauncherPreferences.APP_CORNER] = (config.appCornerRadiusPercent - 10).coerceIn(0, 100)
                                    }
                                }
                            },
                            onIncrease = {
                                scope.launch {
                                    prefs.updateConfig {
                                        it[LauncherPreferences.APP_CORNER] = (config.appCornerRadiusPercent + 10).coerceIn(0, 100)
                                    }
                                }
                            }
                        )
                    }
                }

                // الفئة 5: الشريط العلوي والتبويبات
                item {
                    SettingsCategoryBox("٥. الشريط العلوي والتبويبات") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                SettingOptionPill("الشبكة", config.showTopWifi) {
                                    scope.launch { prefs.updateConfig { it[LauncherPreferences.SHOW_WIFI] = !config.showTopWifi } }
                                }
                            }
                            item {
                                SettingOptionPill("إذاعة القرآن", config.showTopQuranRadio) {
                                    scope.launch { prefs.updateConfig { it[LauncherPreferences.SHOW_QURAN] = !config.showTopQuranRadio } }
                                }
                            }
                            item {
                                SettingOptionPill("المصحف", config.showTopQuranBook) {
                                    scope.launch { prefs.updateConfig { it[LauncherPreferences.SHOW_BOOK] = !config.showTopQuranBook } }
                                }
                            }
                            item {
                                SettingOptionPill("شاشة التوقف", config.showTopScreensaver) {
                                    scope.launch { prefs.updateConfig { it[LauncherPreferences.SHOW_SS] = !config.showTopScreensaver } }
                                                                item {
                                SettingOptionPill("VPN", config.showTopVpn) {
                                    scope.launch { prefs.updateConfig { it[LauncherPreferences.SHOW_VPN] = !config.showTopVpn } }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingOptionPill("تقليب تلقائي للتبويبات", config.autoRotateTabs) {
                                scope.launch { prefs.updateConfig { it[LauncherPreferences.AUTO_ROTATE] = true } }
                            }
                            SettingOptionPill("تقليب يدوي فقط", !config.autoRotateTabs) {
                                scope.launch { prefs.updateConfig { it[LauncherPreferences.AUTO_ROTATE] = false } }
                            }
                        }
                    }
                }

                // الفئة 6: شريط الأسعار والتخطيط المباشر
                item {
                    SettingsCategoryBox("٦. شريط الأسعار والتخطيط المباشر") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(allPairs) { pair ->
                                val active = config.activeTickerPairs.contains(pair)
                                SettingOptionPill(pair, active) {
                                    val newSet = if (active) config.activeTickerPairs - pair else config.activeTickerPairs + pair
                                    scope.launch { prefs.updateConfig { it[LauncherPreferences.TICKER_PAIRS] = newSet } }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingOptionPill("تحريك العناصر بالريموت", false, onClick = onOpenLiveEditor)
                            SettingOptionPill("تعديل شاشة التوقف", false, onClick = onOpenScreensaverEditor)
                        }
                    }
                }
            }
        }
    }
}


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
import java.util.UUID

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
    var isCardFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(config.autoRotateTabs, isCardFocused, expandedTab) {
        if (config.autoRotateTabs && !isCardFocused && expandedTab == null) {
            while (true) {
                delay(10_000L)
                selectedTab = (selectedTab + 1) % tabTitles.size
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
                            Text("اليوم +${idx + 1}: العظمى $mx° / الصغرى $mn°", fontSize = 18.sp)
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
        modifier = modifier.onFocusChanged { isCardFocused = it.isFocused },
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0F172A).copy(alpha = config.cardBgOpacity),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
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
                                if (it.isFocused) selectedTab = index
                            }
                            .clickable {
                                if (selectedTab == index) expandedTab = index
                                else selectedTab = index
                            },
                        shape = CircleShape,
                        color = when {
                            isFocused -> Color.White
                            isSelected -> Color(0xFF38BDF8).copy(alpha = 0.35f)
                            else -> Color.White.copy(alpha = 0.08f)
                        }
                    ) {
                        Text(
                            text = tabTitles[index],
                            color = if (isFocused) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
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
                                    Text(time, color = Color(0xFF38BDF8))
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
                                    Text("اليوم +${idx + 1}", color = Color.White)
                                    Text("العظمى $maxT° / الصغرى $minT°", color = Color(0xFFFBBF24))
                                }
                            }
                        }
                    }
                    2 -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(liveFeed.matches) { m ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(m.name, color = Color.White)
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
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("محرر التخطيط المباشر", color = Color(0xFFFBBF24))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                names.forEachIndexed { idx, title ->
                    Button(onClick = { selectedElement = idx }) { Text(title) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    when (selectedElement) { 0 -> cy -= 15f; 1 -> ky -= 15f; 2 -> dy -= 15f }
                }) { Text("فوق") }
                Button(onClick = {
                    when (selectedElement) { 0 -> cy += 15f; 1 -> ky += 15f; 2 -> dy += 15f }
                }) { Text("تحت") }
                Button(onClick = {
                    when (selectedElement) { 0 -> cx += 15f; 1 -> kx += 15f; 2 -> dx += 15f }
                }) { Text("يمين") }
                Button(onClick = {
                    when (selectedElement) { 0 -> cx -= 15f; 1 -> kx -= 15f; 2 -> dx -= 15f }
                }) { Text("يسار") }
                Button(onClick = {
                    if (selectedElement == 0) cs += 0.05f
                    if (selectedElement == 1) ks += 0.05f
                }) { Text("تكبير") }
                Button(onClick = {
                    if (selectedElement == 0) cs -= 0.05f
                    if (selectedElement == 1) ks -= 0.05f
                }) { Text("تصغير") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onSave(cx, cy, cs, kx, ky, ks, ko, dx, dy) }) { Text("حفظ") }
                OutlinedButton(onClick = onCancel) { Text("إلغاء", color = Color.White) }
            }
        }
    }
}

@Composable
fun ScreensaverView(
    timeText: String,
    amPm: String,
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
            Text("$timeText $amPm", fontSize = 110.sp, color = Color.White)
            Text("$hijri  •  $gregorian", fontSize = 20.sp, color = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.height(12.dp))
            Text(prayerInfo.remainingText, fontSize = 18.sp, color = Color(0xFF38BDF8))
        }
        if (isEditMode) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { oy -= 20f }) { Text("فوق") }
                Button(onClick = { oy += 20f }) { Text("تحت") }
                Button(onClick = { sc += 0.1f }) { Text("تكبير") }
                Button(onClick = { sc -= 0.1f }) { Text("تصغير") }
                Button(onClick = { onSaveOffsets(ox, oy, sc) }) { Text("حفظ") }
            }
        }
    }
}
@Composable
fun ScreensaverView(
    timeText: String,
    amPm: String,
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
            Text("$timeText $amPm", fontSize = 110.sp, color = Color.White)
            Text("$hijri  •  $gregorian", fontSize = 20.sp, color = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.height(12.dp))
            Text(prayerInfo.remainingText, fontSize = 18.sp, color = Color(0xFF38BDF8))
        }
        if (isEditMode) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { oy -= 20f }) { Text("فوق") }
                Button(onClick = { oy += 20f }) { Text("تحت") }
                Button(onClick = { sc += 0.1f }) { Text("تكبير") }
                Button(onClick = { sc -= 0.1f }) { Text("تصغير") }
                Button(onClick = { onSaveOffsets(ox, oy, sc) }) { Text("حفظ") }
            }
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
    val allPairs = listOf("USD/ILS", "JOD/ILS", "EUR/ILS", "GOLD", "BRENT", "BTC", "ETH")

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            modifier = Modifier.fillMaxHeight().width(480.dp),
            color = Color(0xFF0F172A)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إعدادات التخصيص الشاملة", fontSize = 20.sp, color = Color.White)
                        TextButton(onClick = onDismiss) { Text("إغلاق", color = Color(0xFFF87171)) }
                    }
                }
                item {
                    Button(onClick = onOpenLiveEditor, modifier = Modifier.fillMaxWidth()) {
                        Text("تعديل الشاشة الرئيسية بالريموت")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(onClick = onOpenScreensaverEditor, modifier = Modifier.fillMaxWidth()) {
                        Text("تعديل شاشة التوقف بالريموت", color = Color.White)
                    }
                }
                item {
                    Text("خلفيات الطبيعة:", color = Color(0xFF38BDF8))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(LiveRepository.natureWallpapers.size) { idx ->
                            val title = LiveRepository.natureWallpapers[idx].first
                            FilterChip(
                                selected = config.wallpaperIndex == idx,
                                onClick = {
                                    scope.launch {
                                        prefs.updateConfig { it[LauncherPreferences.WALLPAPER_IDX] = idx }
                                    }
                                },
                                label = { Text(title) }
                            )
                        }
                    }
                }
                item {
                    Text("المدينة:", color = Color(0xFF38BDF8))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(PalestineData.cities) { city: PalestinianCity ->
                            FilterChip(
                                selected = config.cityName == city.name,
                                onClick = {
                                    scope.launch {
                                        prefs.updateConfig { it[LauncherPreferences.CITY_NAME] = city.name }
                                    }
                                },
                                label = { Text(city.name) }
                            )
                        }
                    }
                }
                item {
                    Text("تبديل التبويبات التلقائي:", color = Color(0xFF38BDF8))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = config.autoRotateTabs,
                            onClick = {
                                scope.launch {
                                    prefs.updateConfig { it[LauncherPreferences.AUTO_ROTATE] = true }
                                }
                            },
                            label = { Text("تلقائي (كل 10 ثوانٍ)") }
                        )
                        FilterChip(
                            selected = !config.autoRotateTabs,
                            onClick = {
                                scope.launch {
                                    prefs.updateConfig { it[LauncherPreferences.AUTO_ROTATE] = false }
                                }
                            },
                            label = { Text("يدوي فقط") }
                        )
                    }
                }
                item {
                    Text("أزرار الشريط العلوي:", color = Color(0xFF38BDF8))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = config.showTopWifi,
                                onClick = {
                                    scope.launch {
                                        prefs.updateConfig { it[LauncherPreferences.SHOW_WIFI] = !config.showTopWifi }
                                    }
                                },
                                label = { Text("الشبكة") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = config.showTopQuranRadio,
                                onClick = {
                                    scope.launch {
                                        prefs.updateConfig { it[LauncherPreferences.SHOW_QURAN] = !config.showTopQuranRadio }
                                    }
                                },
                                label = { Text("إذاعة القرآن") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = config.showTopQuranBook,
                                onClick = {
                                    scope.launch {
                                        prefs.updateConfig { it[LauncherPreferences.SHOW_BOOK] = !config.showTopQuranBook }
                                    }
                                },
                                label = { Text("المصحف") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = config.showTopScreensaver,
                                onClick = {
                                    scope.launch {
                                        prefs.updateConfig { it[LauncherPreferences.SHOW_SS] = !config.showTopScreensaver }
                                    }
                                },
                                label = { Text("شاشة التوقف") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = config.showTopVpn,
                                onClick = {
                                    scope.launch {
                                        prefs.updateConfig { it[LauncherPreferences.SHOW_VPN] = !config.showTopVpn }
                                    }
                                },
                                label = { Text("VPN") }
                            )
                        }
                    }
                }
                item {
                    Text("شريط التطبيقات (الشكل والحجم والشفافية):", color = Color(0xFF38BDF8))
                    Text("استدارة الحواف: ${config.appCornerRadiusPercent}%", color = Color.White)
                    Slider(
                        value = config.appCornerRadiusPercent.toFloat(),
                        onValueChange = { v ->
                            scope.launch {
                                prefs.updateConfig { it[LauncherPreferences.APP_CORNER] = v.toInt() }
                            }
                        },
                        valueRange = 0f..100f
                    )
                    Text("حجم التطبيقات: ${(config.appBaseScale * 100).toInt()}%", color = Color.White)
                    Slider(
                        value = config.appBaseScale,
                        onValueChange = { v ->
                            scope.launch {
                                prefs.updateConfig { it[LauncherPreferences.APP_SCALE] = v }
                            }
                        },
                        valueRange = 0.7f..1.3f
                    )
                    Text("شفافية خلفية الشريط: ${(config.dockBgOpacity * 100).toInt()}%", color = Color.White)
                    Slider(
                        value = config.dockBgOpacity,
                        onValueChange = { v ->
                            scope.launch {
                                prefs.updateConfig { it[LauncherPreferences.DOCK_OPACITY] = v }
                            }
                        },
                        valueRange = 0f..1f
                    )
                    Text("شفافية البطاقة: ${(config.cardBgOpacity * 100).toInt()}%", color = Color.White)
                    Slider(
                        value = config.cardBgOpacity,
                        onValueChange = { v ->
                            scope.launch {
                                prefs.updateConfig { it[LauncherPreferences.CARD_OPACITY] = v }
                            }
                        },
                        valueRange = 0.1f..1f
                    )
                }
                item {
                    Text("أزواج شريط الأسعار:", color = Color(0xFF38BDF8))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allPairs) { pair: String ->
                            val active = config.activeTickerPairs.contains(pair)
                            FilterChip(
                                selected = active,
                                onClick = {
                                    val newSet = if (active) {
                                        config.activeTickerPairs - pair
                                    } else {
                                        config.activeTickerPairs + pair
                                    }
                                    scope.launch {
                                        prefs.updateConfig { it[LauncherPreferences.TICKER_PAIRS] = newSet }
                                    }
                                },
                                label = { Text(pair) }
                            )
                        }
                    }
                }
            }
        }
    }
}

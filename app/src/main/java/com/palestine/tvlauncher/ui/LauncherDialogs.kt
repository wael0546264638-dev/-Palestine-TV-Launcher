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
    val context = LocalContext.current

    Surface(
        modifier = modifier,
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
                            .clickable { selectedTab = index },
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

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                                    Text(m.status, color = Color(0xFF34D399))
                                }
                            }
                        }
                    }
                    3 -> {
                        Column(
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
                    }
                    4 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            PalestineData.dailyVerses.first(),
                            color = Color.White,
                            fontSize = 19.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    5 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            PalestineData.dailyHadiths.first(),
                            color = Color.White,
                            fontSize = 17.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    6 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(PalestineData.adhkarList) { item: String ->
                            Text("• $item", color = Color.White, fontSize = 14.sp)
                        }
                    }
                    7 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(liveFeed.news) { headline: String ->
                            Text(
                                "▪ $headline",
                                color = Color.White,
                                fontSize = 14.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    8 -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        liveFeed.prices.forEach { (pair, info) ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(pair, color = Color.White)
                                Text(
                                    "${info.first} ${if (info.second) "▲" else "▼"}",

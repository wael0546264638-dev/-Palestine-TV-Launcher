package com.palestine.tvlauncher.ui
import com.palestine.tvlauncher.data.quranRadioStations
import com.palestine.tvlauncher.network.QuranRadio
import com.palestine.tvlauncher.network.*

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.palestine.tvlauncher.data.PalestineData
import com.palestine.tvlauncher.data.AdhanManager
import com.palestine.tvlauncher.data.liveWallpapers
import com.palestine.tvlauncher.data.FamilyReminder
import com.palestine.tvlauncher.data.LauncherConfig
import com.palestine.tvlauncher.data.LauncherPreferences
import com.palestine.tvlauncher.data.PalestinianCity
import com.palestine.tvlauncher.data.TabItem
import com.palestine.tvlauncher.data.cardTabs
import com.palestine.tvlauncher.network.LiveFeedState
import com.palestine.tvlauncher.network.LiveRepository
import com.palestine.tvlauncher.network.SurahHeader
import com.palestine.tvlauncher.network.Verse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    onAddReminder: (FamilyReminder) -> Unit,
    onExpand: (Boolean) -> Unit = {},
    expanded: Boolean = false
) {
    val context = LocalContext.current
    val tabs = cardTabs
    var selectedTab by remember { mutableIntStateOf(0) }

    // التبويبات الفعالة حسب القسم: البطاقة الرئيسية أو نافذة التوسعة، مع الإظهار والإخفاء والترتيب المستقل لكل منهما
    val effectiveTabs = remember(
        config.cardTabOrder, config.cardTabVisible,
        config.expansionTabOrder, config.expansionTabVisible,
        expanded
    ) {
        val order = if (expanded) config.expansionTabOrder else config.cardTabOrder
        val visible = if (expanded) config.expansionTabVisible else config.cardTabVisible
        order.filter { visible.contains(it) }.mapNotNull { key -> tabs.firstOrNull { it.key == key } }
    }
    var isExpanded by remember { mutableStateOf(false) }
    var cardHasFocus by remember { mutableStateOf(false) }
    val cardFocusRequester = remember { FocusRequester() }
    var textFieldFocused by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var todaysHadith by remember { mutableStateOf<HadithItem?>(null) }
    var weatherDetail by remember { mutableStateOf<WeatherDetail?>(null) }
    var newsArticles by remember { mutableStateOf<List<NewsArticle>>(emptyList()) }
    var espnStandings by remember { mutableStateOf<List<EspnStanding>>(emptyList()) }
    var espnMatches by remember { mutableStateOf<List<EspnMatch>>(emptyList()) }

    LaunchedEffect(Unit) {
        scope.launch {
            todaysHadith = LiveRepository.fetchTodaysHadith(context)
        }
        scope.launch {
            weatherDetail = LiveRepository.fetchCityWeatherDetailed(config.cityLat, config.cityLon, config.cityTimeZone)
        }
        scope.launch {
            val cityCountry = (PalestineData.palestineCities + PalestineData.worldCapitals).firstOrNull { it.name == config.cityName }?.country ?: "فلسطين"
            val countryKey = when (cityCountry) {
                "فلسطين" -> "Palestine"
                "الأردن" -> "Jordan"
                "مصر" -> "Egypt"
                "السعودية" -> "Saudi Arabia"
                else -> cityCountry
            }
            val sources = LiveRepository.sourcesForCountry(countryKey, config.disabledNewsSources)
            val allArticles = mutableListOf<NewsArticle>()
            for (source in sources.take(5)) {
                allArticles.addAll(LiveRepository.fetchNewsArticles(source))
            }
            newsArticles = allArticles
        }
        scope.launch {
            espnStandings = LiveRepository.fetchEspnLeagueTable()
            espnMatches = LiveRepository.fetchEspnTeamMatches(config.favoriteTeamId)
        }
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            try { cardFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }
    if (expanded) {
        BackHandler(onBack = { onExpand(false) })
    }

    LaunchedEffect(effectiveTabs.size, config.customCardAllTabs) {
        if (effectiveTabs.isEmpty()) {
            selectedTab = 0
        } else if (config.customCardAllTabs) {
            selectedTab = effectiveTabs.lastIndex
        } else if (selectedTab >= effectiveTabs.size) {
            selectedTab = effectiveTabs.lastIndex
        }
    }

    // التقليب التلقائي: الوضع 0 = تلقائي دائماً، الوضع 1 = يدوي فقط،
    // الوضع 2 = تلقائي ويتوقف فوراً عند دخول التركيز للبطاقة ثم يعود عند الخروج.
    LaunchedEffect(config.cardFlipMode, config.tabAutoRotateIntervalMinutes, cardHasFocus) {
        val autoEnabled = config.cardFlipMode == 0 || config.cardFlipMode == 2
        if (!autoEnabled) return@LaunchedEffect
        if (config.cardFlipMode == 2 && cardHasFocus) return@LaunchedEffect
        if (config.tabAutoRotateIntervalMinutes <= 0) return@LaunchedEffect
        while (true) {
            delay(config.tabAutoRotateIntervalMinutes * 60_000L)
            if (effectiveTabs.isNotEmpty()) {
                selectedTab = (selectedTab + 1) % effectiveTabs.size
            }
        }
    }

    Surface(
        modifier = modifier
            .onFocusChanged { cardHasFocus = it.hasFocus }
            .then(
                if (expanded) Modifier
                    .focusRequester(cardFocusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (expanded && !textFieldFocused && event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.DirectionLeft -> {
                                            if (effectiveTabs.isNotEmpty()) {
                                                selectedTab = (selectedTab + 1) % effectiveTabs.size
                                            }
                                            true
                                        }
                                        Key.DirectionRight -> {
                                            if (effectiveTabs.isNotEmpty()) {
                                                selectedTab = (selectedTab - 1 + effectiveTabs.size) % effectiveTabs.size
                                            }
                                            true
                                        }
                                    else -> false
                                }
                        } else false
                    }
                else Modifier
            )
            .graphicsLayer {
                if (cardHasFocus && config.focusGlowEnabled) {
                    shadowElevation = 12.dp.toPx()
                    spotShadowColor = Color(config.focusGlowColor).copy(alpha = 0.5f)
                }
            },
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = config.cardBgOpacity),
        border = BorderStroke(
            width = if (cardHasFocus) 3.dp else 1.dp,
            color = when {
                cardHasFocus -> Color(config.focusGlowColor)
                else -> Color.White.copy(alpha = 0.16f)
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (expanded) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                ) {
                    Text(
                        text = "◀ ▶ للتبديل  •  ▲ ▼ للتنقل  •  OK للاختيار  •  رجوع للعودة",
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(effectiveTabs.size) { idx ->
                        val active = selectedTab == idx
                        val tab = effectiveTabs[idx]
                        val interaction = remember { MutableInteractionSource() }
                        val focused by interaction.collectIsFocusedAsState()
                        val focusScale = when {
                            focused && config.focusScaleEnabled -> config.focusScaleFactor
                            focused -> 1.12f
                            else -> 1f
                        }
                        val glow = config.focusGlowEnabled && focused
                        // التبويب المحدد: خلفية مميزة (أزرق ساطع) + إطار بارز (3dp) + نص داكن عالي التباين
                        // التبويب المركز: خلفية بيضاء + إطار توهج + نص داكن
                        // التبويبات الأخرى: خلفية شفافة خفيفة
                        val tabBg = when {
                            focused -> Color.White
                            active -> Color(0xFF0EA5E9)  // أزرق ساطع ومميز للتبويب المحدد
                            else -> Color.White.copy(alpha = 0.10f)
                        }
                        val tabBorderColor = when {
                            focused -> Color(config.focusGlowColor)
                            active -> Color(config.focusBorderColor)
                            else -> Color.Transparent
                        }
                        val tabBorderWidth = when {
                            focused -> maxOf(config.focusBorderWidth, 3f)
                            active && config.focusBorderEnabled -> maxOf(config.focusBorderWidth, 3f)  // إطار بارز 3dp للتبويب المحدد
                            else -> 0f
                        }
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .focusable()
                                .onFocusChanged { if (it.isFocused) selectedTab = idx }
                                .clickable(interactionSource = interaction, indication = null) { selectedTab = idx }
                                .graphicsLayer {
                                    scaleX = focusScale
                                    scaleY = focusScale
                                    if (glow) {
                                        shadowElevation = 16.dp.toPx()
                                        spotShadowColor = Color(config.focusGlowColor).copy(alpha = 0.6f)
                                    }
                                },
                            shape = CircleShape,
                            color = tabBg,
                            border = if (tabBorderWidth > 0f) {
                                BorderStroke(tabBorderWidth.dp, tabBorderColor)
                            } else BorderStroke(0.dp, Color.Transparent)
                        ) {
                            Text(
                                text = tab.title,
                                color = if (focused || active) Color(0xFF0F172A) else Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (active || focused) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val currentTab = effectiveTabs.getOrElse(selectedTab) { null }
                when (currentTab?.key) {
                     "prayer" -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                          Text(prayerInfo.remainingText, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                          val rows = listOf(
                              "الفجر" to prayerInfo.fajr,
                              "الشروق" to prayerInfo.sunrise,
                              "الظهر" to prayerInfo.dhuhr,
                              "العصر" to prayerInfo.asr,
                              "المغرب" to prayerInfo.maghrib,
                              "العشاء" to prayerInfo.isha
                          )
 rows.forEach { (name, time) ->
                               val isNext = prayerInfo.nextPrayerName == name
                               Row(
                                   modifier = Modifier
                                       .fillMaxWidth()
                                       .clip(RoundedCornerShape(12.dp))
                                       .then(
                                           if (isNext) Modifier
                                               .background(
                                                   Brush.horizontalGradient(
                                                       listOf(
                                                           Color(0xFFFBBF24).copy(alpha = 0.45f),
                                                           Color(0xFFF59E0B).copy(alpha = 0.25f),
                                                           Color(0xFFEF4444).copy(alpha = 0.15f)
                                                       )
                                                   )
                                               )
                                               .border(2.5.dp, Color(0xFFFBBF24), RoundedCornerShape(12.dp))
                                               .graphicsLayer {
                                                   shadowElevation = 8.dp.toPx()
                                                   spotShadowColor = Color(0xFFFBBF24).copy(alpha = 0.4f)
                                               }
                                           else Modifier
                                       )
                                       .padding(horizontal = 12.dp, vertical = if (isNext) 8.dp else 5.dp),
                                   horizontalArrangement = Arrangement.SpaceBetween,
                                   verticalAlignment = Alignment.CenterVertically
                               ) {
                                   Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                       if (isNext) {
                                           Box(
                                               modifier = Modifier.size(22.dp),
                                               contentAlignment = Alignment.Center
                                           ) {
                                               Icon(
                                                   Icons.Default.NotificationsActive,
                                                   contentDescription = null,
                                                   tint = Color(0xFFFBBF24),
                                                   modifier = Modifier
                                                       .size(18.dp)
                                                       .graphicsLayer {
                                                           shadowElevation = 4.dp.toPx()
                                                           spotShadowColor = Color(0xFFFBBF24).copy(alpha = 0.6f)
                                                       }
                                               )
                                           }
                                       }
                                       Text(
                                           name,
                                           color = if (isNext) Color(0xFFFBBF24) else Color.White,
                                           fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                                           fontSize = if (isNext) 15.sp else 14.sp
                                       )
                                       if (isNext) {
                                           Text(
                                               "القادمة",
                                               color = Color(0xFF0F172A),
                                               fontSize = 11.sp,
                                               fontWeight = FontWeight.ExtraBold,
                                               modifier = Modifier
                                                   .clip(RoundedCornerShape(8.dp))
                                                   .background(
                                                       Brush.horizontalGradient(
                                                           listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))
                                                       )
                                                   )
                                                   .padding(horizontal = 10.dp, vertical = 2.dp)
                                                   .graphicsLayer {
                                                       shadowElevation = 3.dp.toPx()
                                                       spotShadowColor = Color(0xFFF59E0B).copy(alpha = 0.5f)
                                                   }
                                           )
                                       }
                                   }
                                   Text(
                                       time,
                                       color = if (isNext) Color(0xFFFBBF24) else Color(0xFFE2E8F0),
                                       fontWeight = FontWeight.Bold,
                                       fontSize = if (isNext) 15.sp else 14.sp
                                   )
 }
                         }
                     }
                     "weather" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                         val detail = weatherDetail
                         if (detail != null) {
                             Text(
                                 PalestineData.toArabicDigits("الطقس في ${config.cityName}: ${detail.currentTemp}°"),
                                 color = Color(0xFF38BDF8),
                                 fontWeight = FontWeight.Bold,
                                 fontSize = 16.sp
                             )
                             Text(
                                 PalestineData.toArabicDigits("احتمالية المطر: ${detail.precipitationProbability}%"),
                                 color = Color(0xFF90CAF9),
                                 fontSize = 14.sp
                             )
                             Spacer(modifier = Modifier.height(8.dp))
                             Text("توقعات الطقس على مدار الساعات", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                             LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                 items(detail.hourly.take(12)) { hour ->
                                     Column(modifier = Modifier.width(70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                         Text(PalestineData.toArabicDigits(hour.time.substringAfterLast("T").take(5)), color = Color.White, fontSize = 12.sp)
                                         Text(PalestineData.toArabicDigits("${hour.temp}°"), color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                         Text("${hour.precipProb}%", color = Color(0xFF64B5F6), fontSize = 11.sp)
                                     }
                                 }
                             }
                             Spacer(modifier = Modifier.height(10.dp))
                             Text("توقعات الأيام القادمة", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                             LazyColumn {
                                 items(detail.daily) { day ->
                                     Row(
                                         modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                         horizontalArrangement = Arrangement.SpaceBetween,
                                         verticalAlignment = Alignment.CenterVertically
                                     ) {
                                         Text(day.date, color = Color.White, fontSize = 14.sp)
                                         Text(
                                             PalestineData.toArabicDigits("العظمى ${day.max}° / الصغرى ${day.min}°  •  مطر ${day.precipProb}%"),
                                             color = Color(0xFFFBBF24),
                                             fontSize = 14.sp
                                         )
                                     }
                                 }
                             }
                         } else {
                             Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                 Text("جاري تحميل تفاصيل الطقس من المصدر المباشر...", color = Color(0xFF38BDF8), fontSize = 16.sp)
                             }
                         }
                     }
                     "matches" -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                         Text("جدول ترتيب الدوري", color = Color(0xFF38BDF8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                         LazyColumn {
                             items(espnStandings) { standing ->
                                 Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                     Text(PalestineData.toArabicDigits("${standing.position}. ${standing.team}"), color = Color.White)
                                     Text(PalestineData.toArabicDigits("لعب ${standing.played} • رصيد ${standing.points} نقطة"), color = Color(0xFF34D399))
                                 }
                             }
                         }
                         Spacer(modifier = Modifier.height(12.dp))
                         Text("مباريات ${config.favoriteTeamName.ifBlank { "الفريق المفضل" }}", color = Color(0xFF38BDF8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                         if (espnMatches.isNotEmpty()) {
                             LazyColumn {
                                 items(espnMatches) { match ->
                                     Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                         Column {
                                             Text(match.homeTeam, color = Color.White, fontSize = 14.sp)
                                             Text(match.awayTeam, color = Color(0xFF94A3B8), fontSize = 12.sp)
                                         }
                                         Column {
                                             Text("${match.homeScore ?: "-"} - ${match.awayScore ?: "- "}", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                             Text(match.status, color = Color(0xFF64B5F6), fontSize = 12.sp)
                                         }
                                     }
                                 }
                             }
                         } else {
                             Text("لا توجد مباريات متاحة للفريق في الوقت الحالي", color = Color(0xFF94A3B8), fontSize = 14.sp)
                         }
                     }
                     "date" -> Column(
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
                     "verse" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         Text(PalestineData.dailyVerses.first(), color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center)
                     }
                     "hadith" -> val hadith = todaysHadith
                         if (hadith != null) {
                             Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                 Text("حديث اليوم", color = Color(0xFF38BDF8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                 Text(hadith.text, color = Color.White, fontSize = 17.sp, textAlign = TextAlign.Center)
                                 Spacer(modifier = Modifier.height(10.dp))
                                 Text("الشرح: ${hadith.explanation.ifBlank { "غير متوفر" }}", color = Color(0xFFCBD5E1), fontSize = 14.sp)
                                 Text("الفوائد: ${hadith.benefits.ifBlank { "غير متوفرة" }}", color = Color(0xFFE1BEE7), fontSize = 14.sp)
                                 Spacer(modifier = Modifier.height(6.dp))
                                 Text("المصدر: ${hadith.source.ifBlank { "مستنبط" }}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                             }
                         } else {
                             Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                 Text("جاري تحميل حديث اليوم...", color = Color(0xFF38BDF8), fontSize = 16.sp)
                             }
                         }
                     "adhkar" -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                         items(PalestineData.adhkarList) { item -> Text("• $item", color = Color.White, fontSize = 14.sp) }
                     }
                     "news" -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                         items(newsArticles) { article ->
                             Surface(
                                 modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                                 shape = RoundedCornerShape(12.dp),
                                 color = Color.White.copy(alpha = 0.06f)
                             ) {
                                 Column(modifier = Modifier.padding(14.dp)) {
                                     Text("${article.source} - ${article.title}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                     if (article.description.isNotBlank()) {
                                         Spacer(modifier = Modifier.height(8.dp))
                                         Text(article.description, color = Color(0xFFCBD5E1), fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                     }
                                     Spacer(modifier = Modifier.height(8.dp))
                                     Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                         Text("المصدر: ${article.source}", color = Color(0xFF38BDF8), fontSize = 11.sp)
                                         Text(PalestineData.toArabicDigits(article.pubDate.takeIf { it.isNotBlank() } ?: "تاريخ النشر غير محدد"), color = Color(0xFF94A3B8), fontSize = 11.sp)
                                     }
                                 }
                             }
                         }
                         if (newsArticles.isEmpty()) {
                             item { Text("جاري تحميل الأخبار من مصادر الدولة...", color = Color(0xFF94A3B8), fontSize = 14.sp) }
                         }
                     }
                     "prices" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                         liveFeed.prices.forEach { (pair, info) ->
                             Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                 Text(pair, color = Color.White)
                                 Text("${info.first} ${if (info.second) "▲" else "▼"}", color = if (info.second) Color(0xFF34D399) else Color(0xFFF87171))
                             }
                         }
                      }
                      "occasions" -> {
                          val daysToRamadan = PalestineData.getDaysUntilRamadan()
                         Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                             Text("المناسبات الإسلامية:", color = Color(0xFF38BDF8))
                             Text(PalestineData.toArabicDigits("• رمضان المبارك: بعد $daysToRamadan يوم"), color = Color.White)
                             Text(PalestineData.toArabicDigits("• عيد الفطر: بعد ${daysToRamadan + 30} يوم"), color = Color(0xFFCBD5E1))
                         }
                      }
                      "device" -> {
                          var avail = 0L
                         var total = 0L
                         var gb = 0L
                         try {
                             val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                             val mi = ActivityManager.MemoryInfo()
                             am.getMemoryInfo(mi)
                             avail = mi.availMem / (1024 * 1024)
                             total = mi.totalMem / (1024 * 1024)
                         } catch (e: Exception) {
                         }
                         try {
                             val stat = StatFs(Environment.getDataDirectory().path)
                             gb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024 * 1024)
                         } catch (e: Exception) {
                         }
                         Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                             Text("حالة جهاز TV Box:", color = Color(0xFF38BDF8))
                             Text(PalestineData.toArabicDigits("الذاكرة المتاحة: $avail MB / $total MB"), color = Color.White)
                             Text(PalestineData.toArabicDigits("المساحة المتبقية: $gb GB"), color = Color.White)
                         }
                      }
                      "tasks" -> {
                          var taskInput by remember { mutableStateOf("") }
                         Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                             Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                 OutlinedTextField(
                                     value = taskInput,
                                     onValueChange = { taskInput = it },
                                     placeholder = { Text("أضف مهمة...", color = Color.Gray) },
                                     modifier = Modifier
                                         .weight(1f)
                                         .onFocusChanged { textFieldFocused = it.isFocused },
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
                      "reminders" -> {
                          var nameIn by remember { mutableStateOf("") }
                         var dayIn by remember { mutableStateOf("") }
                         var monthIn by remember { mutableStateOf("") }
                         Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                             Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                 OutlinedTextField(value = nameIn, onValueChange = { nameIn = it }, placeholder = { Text("الاسم", color = Color.Gray) }, modifier = Modifier.weight(1.4f).onFocusChanged { textFieldFocused = it.isFocused }, singleLine = true)
                                 OutlinedTextField(value = dayIn, onValueChange = { dayIn = it }, placeholder = { Text("يوم", color = Color.Gray) }, modifier = Modifier.weight(0.6f).onFocusChanged { textFieldFocused = it.isFocused }, singleLine = true)
                                 OutlinedTextField(value = monthIn, onValueChange = { monthIn = it }, placeholder = { Text("شهر", color = Color.Gray) }, modifier = Modifier.weight(0.6f).onFocusChanged { textFieldFocused = it.isFocused }, singleLine = true)
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
         }
     }
 }
}
@Composable
fun FullQuranDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var surahs by remember { mutableStateOf<List<SurahHeader>>(staticSurahList) }
    var selectedSurahNum by remember { mutableIntStateOf(1) }
    var verses by remember { mutableStateOf<List<Verse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var fontSizeSp by remember { mutableFloatStateOf(24f) }
    var showTafsir by remember { mutableStateOf(false) }
    val surahListFocus = remember { FocusRequester() }
    val versesFocus = remember { FocusRequester() }
    var focusedVerseIndex by remember { mutableIntStateOf(-1) }
    var lastPlayedVerseIndex by remember { mutableIntStateOf(-1) }

    val mediaPlayer = remember {
        android.media.MediaPlayer().apply {
            setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }
    }

    fun playVerseAudio(audioUrl: String, verseIndex: Int) {
        if (lastPlayedVerseIndex == verseIndex && mediaPlayer.isPlaying) {
            return
        }
        scope.launch {
            try {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(audioUrl)
                mediaPlayer.setOnPreparedListener {
                    it.start()
                    lastPlayedVerseIndex = verseIndex
                }
                mediaPlayer.setOnErrorListener { _, _, _ ->
                    lastPlayedVerseIndex = -1
                    true
                }
                mediaPlayer.setOnCompletionListener {
                    if (lastPlayedVerseIndex == verseIndex) {
                        lastPlayedVerseIndex = -1
                    }
                }
                mediaPlayer.prepareAsync()
            } catch (_: Exception) {
                lastPlayedVerseIndex = -1
            }
        }
    }

    fun stopAudio() {
        try {
            if (mediaPlayer.isPlaying) mediaPlayer.stop()
            mediaPlayer.reset()
        } catch (_: Exception) {}
        lastPlayedVerseIndex = -1
    }

    LaunchedEffect(Unit) {
        surahListFocus.requestFocus()
    }

    LaunchedEffect(selectedSurahNum) {
        isLoading = true
        focusedVerseIndex = -1
        stopAudio()
                                             val result = LiveRepository.fetchSurahWithAudio(context, selectedSurahNum)
        verses = result ?: emptyList()
        isLoading = false
        if (result != null && result.isNotEmpty()) {
            scope.launch {
                delay(300)
                try {
                    versesFocus.requestFocus()
                } catch (e: Exception) {
                }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            stopAudio()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0B1324),
            border = BorderStroke(width = 1.5.dp, color = Color(0xFFFBBF24).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(28.dp))
                        Text(
                            text = surahs.firstOrNull { it.number == selectedSurahNum }?.let { "${it.name}  •  ${it.ayahCount} آية" } ?: "المصحف الشريف الكامل (١١٤ سورة)",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingOptionPill("تصغير الخط -", false) { fontSizeSp = (fontSizeSp - 2f).coerceAtLeast(18f) }
                        SettingOptionPill("تكبير الخط +", false) { fontSizeSp = (fontSizeSp + 2f).coerceAtMost(36f) }
                        SettingOptionPill(if (showTafsir) "إخفاء التفسير" else "إظهار التفسير", showTafsir) { showTafsir = !showTafsir }
                        SettingOptionPill("إغلاق", false, onClick = { stopAudio(); onDismiss() })
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(0.28f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(8.dp)
                            .focusRequester(surahListFocus),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(surahs, key = { it.number }) { s ->
                            val isSelected = s.number == selectedSurahNum
                            val interaction = remember { MutableInteractionSource() }
                            val focused by interaction.collectIsFocusedAsState()
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(interactionSource = interaction, indication = null) { selectedSurahNum = s.number },
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    focused -> Color.White
                                    isSelected -> Color(0xFFFBBF24).copy(alpha = 0.25f)
                                    else -> Color.Transparent
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = PalestineData.toArabicDigits("${s.number}. ${s.name}"),
                                        color = if (focused) Color(0xFF0F172A) else if (isSelected) Color(0xFFFBBF24) else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected || focused) FontWeight.Bold else FontWeight.Normal
                                    )
                                     Text(
                                         text = PalestineData.toArabicDigits("${s.ayahCount} آية"),
                                         color = if (focused) Color(0xFF334155) else Color(0xFF94A3B8),
                                         fontSize = 11.sp
                                     )
                                 }
                             }
                         }
                         item {
                             Spacer(modifier = Modifier.height(32.dp))
                         }
                    }

                    Surface(
                        modifier = Modifier.weight(0.72f).fillMaxHeight(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF111C33),
                        border = BorderStroke(width = 1.dp, color = Color.White.copy(alpha = 0.1f))
                    ) {
                        if (isLoading) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("جاري تحميل السورة بالرسم العثماني مع الصوت...", color = Color(0xFFFBBF24), fontSize = 18.sp)
                            }
                        } else if (verses.isNotEmpty()) {
                            val scrollState = rememberScrollState()
                            LaunchedEffect(selectedSurahNum) {
                                try {
                                    scrollState.scrollTo(0)
                                } catch (e: Exception) {
                                }
                            }
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(scrollState)
                                    .focusRequester(versesFocus),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(verses, key = { it.number }) { verse ->
                                    val verseIndex = verse.number - 1
                                    val isFocused = focusedVerseIndex == verseIndex
                                    val isPlaying = lastPlayedVerseIndex == verseIndex
                                    val interaction = remember { MutableInteractionSource() }
                                    val focused by interaction.collectIsFocusedAsState()
                                    
                                    LaunchedEffect(focused) {
                                        if (focused) {
                                            focusedVerseIndex = verseIndex
                                            verse.audioUrl?.let { url ->
                                                playVerseAudio(url, verseIndex)
                                            }
                                        }
                                    }
                                    
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .focusable()
                                            .clickable(interactionSource = interaction, indication = null) { 
                                                verse.audioUrl?.let { url ->
                                                    playVerseAudio(url, verseIndex)
                                                }
                                            }
                                            .graphicsLayer {
                                                if (isFocused || isPlaying) {
                                                    shadowElevation = 8.dp.toPx()
                                                    spotShadowColor = Color(0xFFFBBF24).copy(alpha = 0.4f)
                                                }
                                            },
                                        shape = RoundedCornerShape(14.dp),
                                        color = when {
                                            isFocused || isPlaying -> Color(0xFFFBBF24).copy(alpha = 0.15f)
                                            focused -> Color.White.copy(alpha = 0.1f)
                                            else -> Color.Transparent
                                        },
                                        border = BorderStroke(
                                            width = if (isFocused || isPlaying) 3.dp else if (focused) 2.dp else 1.dp,
                                            color = when {
                                                isFocused || isPlaying -> Color(0xFFFBBF24)
                                                focused -> Color.White.copy(alpha = 0.5f)
                                                else -> Color.White.copy(alpha = 0.1f)
                                            }
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = PalestineData.toArabicDigits("﴿${verse.number}﴾"),
                                                    color = if (isFocused || isPlaying) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (isPlaying) {
                                                    Icon(
                                                        imageVector = Icons.Default.VolumeUp,
                                                        contentDescription = "جاري التشغيل",
                                                        tint = Color(0xFFFBBF24),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                              Text(
                                                  text = verse.text,
                                                  color = Color(0xFFF8FAFC),
                                                  fontSize = fontSizeSp.sp,
                                                  lineHeight = (fontSizeSp * 1.9f).sp,
                                                  textAlign = TextAlign.Center,
                                                  fontWeight = FontWeight.Normal
                                              )
                                              if (showTafsir && verse.tafsir.isNotBlank()) {
                                                  Spacer(modifier = Modifier.height(10.dp))
                                                  Text(
                                                      text = "التفسير الميسر: ${verse.tafsir}",
                                                      color = Color(0xFF94A3B8),
                                                      fontSize = (fontSizeSp - 4f).coerceAtLeast(14f).sp,
                                                      textAlign = TextAlign.Center
                                                  )
                                              }
                                         }
                                     }
                                 }
                                 item {
                                     Spacer(modifier = Modifier.height(32.dp))
                                 }
                            }
                           
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectedSurahNum > 1) {
                                    SettingOptionPill("السورة السابقة", false) { selectedSurahNum -= 1 }
                                }
                                if (selectedSurahNum < 114) {
                                    SettingOptionPill("السورة التالية", false) { selectedSurahNum += 1 }
                                }
                            }
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("تعذر جلب السورة، تأكد من اتصال الإنترنت", color = Color(0xFFF87171), fontSize = 16.sp)
                                    SettingOptionPill("إعادة المحاولة", false) {
                                        scope.launch {
                                            isLoading = true
                val result = LiveRepository.fetchSurahWithAudio(context, selectedSurahNum)
                                            verses = result ?: emptyList()
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                        }
                    }
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
    var target by remember { mutableIntStateOf(0) }
    var cx by remember { mutableFloatStateOf(config.clockOffsetX) }
    var cy by remember { mutableFloatStateOf(config.clockOffsetY) }
    var cs by remember { mutableFloatStateOf(config.clockScale) }
    var kx by remember { mutableFloatStateOf(config.cardOffsetX) }
    var ky by remember { mutableFloatStateOf(config.cardOffsetY) }
    var ks by remember { mutableFloatStateOf(config.cardScale) }
    var ko by remember { mutableFloatStateOf(config.cardBgOpacity) }
    var dx by remember { mutableFloatStateOf(config.dockOffsetX) }
    var dy by remember { mutableFloatStateOf(config.dockOffsetY) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> { when (target) { 0 -> cx -= 12f; 1 -> kx -= 12f; 2 -> dx -= 12f }; true }
                        Key.DirectionRight -> { when (target) { 0 -> cx += 12f; 1 -> kx += 12f; 2 -> dx += 12f }; true }
                        Key.DirectionUp -> { when (target) { 0 -> cy -= 12f; 1 -> ky -= 12f; 2 -> dy -= 12f }; true }
                        Key.DirectionDown -> { when (target) { 0 -> cy += 12f; 1 -> ky += 12f; 2 -> dy += 12f }; true }
                        Key.DirectionCenter, Key.Enter -> { target = (target + 1) % 3; true }
                        Key.Back -> { onSave(cx, cy, cs, kx, ky, ks, ko, dx, dy); true }
                        else -> false
                    }
                } else false
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier.padding(top = 18.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.92f),
            border = BorderStroke(1.5.dp, Color(0xFF38BDF8))
        ) {
            val names = listOf("الساعة والتاريخ", "البطاقة التفاعلية", "شريط التطبيقات")
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("العنصر المحدد: ${names[target]}", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                Text("الأسهم: تحريك  |  OK: تبديل العنصر  |  رجوع: حفظ", color = Color.White, fontSize = 13.sp)
                TextButton(onClick = { onSave(cx, cy, cs, kx, ky, ks, ko, dx, dy) }) { Text("حفظ", color = Color(0xFF34D399)) }
                TextButton(onClick = onCancel) { Text("إلغاء", color = Color(0xFFF87171)) }
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
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    if (isEditMode) {
                        when (event.key) {
                            Key.DirectionLeft -> { ox -= 15f; true }
                            Key.DirectionRight -> { ox += 15f; true }
                            Key.DirectionUp -> { oy -= 15f; true }
                            Key.DirectionDown -> { oy += 15f; true }
                            Key.DirectionCenter, Key.Enter, Key.Back -> { onSaveOffsets(ox, oy, sc); true }
                            else -> false
                        }
                    } else { onExit(); true }
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.offset(x = ox.dp, y = oy.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ArabicClockDisplay(timeZoneId = timeZoneId, clockStyle = 0)
            Spacer(modifier = Modifier.height(8.dp))
            Text("$hijri  |  $gregorian", color = Color(0xFFCBD5E1), fontSize = 20.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(prayerInfo.remainingText, color = Color(0xFF38BDF8), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            if (isEditMode) {
                Text("الأسهم: تحريك  |  OK/رجوع: حفظ", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun SettingsCategoryBox(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color(0xFF38BDF8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun SettingOptionPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Surface(
        modifier = Modifier
            .focusable()
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .graphicsLayer {
                if (focused) {
                    scaleX = 1.05f
                    scaleY = 1.05f
                }
            },
        shape = RoundedCornerShape(10.dp),
        color = when {
            focused -> Color.White
            selected -> Color(0xFF38BDF8).copy(alpha = 0.25f)
            else -> Color.White.copy(alpha = 0.06f)
        },
        border = BorderStroke(
            width = if (focused) 2.dp else if (selected) 1.5.dp else 1.dp,
            color = when {
                focused -> Color(config.focusGlowColor)
                selected -> Color(0xFF38BDF8)
                else -> Color.White.copy(alpha = 0.12f)
            }
        )
    ) {
        Text(
            text = text,
            color = if (focused) Color(0xFF0F172A) else if (selected) Color(0xFF38BDF8) else Color.White,
            fontSize = 13.sp,
            fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun StepAdjustRow(
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontSize = 14.sp)
            Text(valueLabel, color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SettingOptionPill("−", false, onClick = onDecrease)
            SettingOptionPill("+", false, onClick = onIncrease)
        }
    }
}

@Composable
fun SettingsModalDrawer(
    config: LauncherConfig,
    prefs: LauncherPreferences,
    onPickDeviceImage: () -> Unit,
    onOpenLiveEditor: () -> Unit,
    onOpenScreensaverEditor: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<PalestinianCity>>(emptyList()) }
    var locationStatus by remember { mutableStateOf("الموقع الحالي: ${config.cityName}") }
    val allPairs = listOf("USD/ILS", "JOD/ILS", "EUR/ILS", "GOLD")
    val closeFocus = remember { FocusRequester() }
    var quranRadios by remember { mutableStateOf<List<QuranRadio>>(emptyList()) }
    var isLoadingRadios by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        closeFocus.requestFocus()
        if (quranRadios.isEmpty() && !isLoadingRadios) {
            isLoadingRadios = true
            scope.launch {
                val radios = LiveRepository.fetchQuranRadios(context)
                quranRadios = radios
                isLoadingRadios = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        BackHandler(onBack = { if (selectedSection != null) selectedSection = null else onDismiss() })
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                modifier = Modifier.fillMaxHeight().fillMaxWidth(0.56f),
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                color = Color(0xFF0B1324),
                border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إعدادات اللانشر", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.focusRequester(closeFocus)) {
                            SettingOptionPill("إغلاق", false, onClick = onDismiss)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedSection == null) {
                        MainSectionsView(onSectionClick = { name -> selectedSection = name })
                    } else {
                        SectionContentView(
                            sectionName = selectedSection,
                            onBackClick = { selectedSection = null },
                            config = config,
                            prefs = prefs,
                            scope = scope,
                            context = context,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            searchResults = searchResults,
                            onSearchResultsChange = { searchResults = it },
                            locationStatus = locationStatus,
                            onLocationStatusChange = { locationStatus = it },
                            allPairs = allPairs,
                            quranRadios = quranRadios,
                            isLoadingRadios = isLoadingRadios,
                            onIsLoadingRadiosChange = { isLoadingRadios = it },
                            onPickDeviceImage = onPickDeviceImage,
                            onOpenLiveEditor = onOpenLiveEditor,
                            onOpenScreensaverEditor = onOpenScreensaverEditor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainSectionsView(onSectionClick: (String) -> Unit) {
    val sections = listOf(
        "عام والموقع" to Icons.Default.Settings,
        "المظهر والخلفيات" to Icons.Default.Palette,
        "البطاقة والتوسعة" to Icons.Default.DashboardCustomize,
        "شريط التطبيقات" to Icons.Default.Apps,
        "الأذان والعبادات" to Icons.Default.MusicNote,
        "مصادر الأخبار والرياضة" to Icons.Default.Newspaper,
        "الشريط السفلي" to Icons.Default.FormatListBulleted,
        "حالة الجهاز" to Icons.Default.Memory
    )
    Column(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("اختر قسم الإعدادات", color = Color(0xFF38BDF8), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(sections) { (title, icon) ->
                val interaction = remember { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()
                val scale = if (focused) 1.08f else 1f
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .focusable()
                        .clickable(interactionSource = interaction, indication = null) { onSectionClick(title) }
                        .graphicsLayer { scaleX = scale; scaleY = scale },
                    shape = RoundedCornerShape(16.dp),
                    color = when {
                        focused -> Color.White.copy(alpha = 0.12f)
                        else -> Color.White.copy(alpha = 0.05f)
                    },
                    border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(28.dp))
                            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SectionContentView(
    sectionName: String,
    onBackClick: () -> Unit,
    config: LauncherConfig,
    prefs: LauncherPreferences,
    scope: kotlinx.coroutines.CoroutineScope,
    context: Context,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<PalestinianCity>,
    onSearchResultsChange: (List<PalestinianCity>) -> Unit,
    locationStatus: String,
    onLocationStatusChange: (String) -> Unit,
    allPairs: List<String>,
    quranRadios: List<QuranRadio>,
    isLoadingRadios: Boolean,
    onIsLoadingRadiosChange: (Boolean) -> Unit,
    onPickDeviceImage: () -> Unit,
    onOpenLiveEditor: () -> Unit,
    onOpenScreensaverEditor: () -> Unit
) {
    Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF38BDF8).copy(alpha = 0.1f),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(sectionName, color = Color(0xFF38BDF8), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                SettingOptionPill("◀ رجوع للأقسام الرئيسية", false, onClick = onBackClick)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (sectionName) {
                "عام والموقع" -> item { GeneralLocationSection(
                    config = config, prefs = prefs, scope = scope, context = context,
                    searchQuery = searchQuery, onSearchQueryChange = onSearchQueryChange,
                    searchResults = searchResults, onSearchResultsChange = onSearchResultsChange,
                    locationStatus = locationStatus, onLocationStatusChange = onLocationStatusChange
                ) }
                "المظهر والخلفيات" -> {
                    item { WallpapersSection(config = config, prefs = prefs, scope = scope, onPickDeviceImage = onPickDeviceImage) }
                    item { LiveWallpaperLibrarySection(config = config, prefs = prefs, scope = scope, context = context) }
                    item { AnimatedWallpapersSection(config = config, prefs = prefs, scope = scope) }
                }
                "البطاقة والتوسعة" -> {
                    item { ClockStylesSection(config = config, prefs = prefs, scope = scope) }
                    item { CardTransparencySection(config = config, prefs = prefs, scope = scope) }
                    item { CardFlipSection(config = config, prefs = prefs, scope = scope) }
                    item { CardTabsSection(config = config, prefs = prefs, scope = scope) }
                    item { ExpansionTabsSection(config = config, prefs = prefs, scope = scope) }
                }
                "شريط التطبيقات" -> item { AppBarSection(config = config, prefs = prefs, scope = scope, allPairs = allPairs, onOpenLiveEditor = onOpenLiveEditor, onOpenScreensaverEditor = onOpenScreensaverEditor) }
                "الأذان والعبادات" -> {
                    item { QuranRadioSection(config = config, prefs = prefs, scope = scope, quranRadios = quranRadios, isLoadingRadios = isLoadingRadios, onIsLoadingRadiosChange = onIsLoadingRadiosChange) }
                    item { AdhanSection(config = config, prefs = prefs, scope = scope, context = context) }
                }
                "مصادر الأخبار والرياضة" -> {
                    item { TickerElementsSection(config = config, prefs = prefs, scope = scope) }
                    item { FavoriteTeamSection(config = config, prefs = prefs, scope = scope) }
                    item { NewsSourcesSection(config = config, prefs = prefs, scope = scope) }
                }
                "الشريط السفلي" -> item { BottomBarSection(config = config, prefs = prefs, scope = scope) }
                "حالة الجهاز" -> item { DeviceStatusSection(config = config, prefs = prefs, scope = scope, context = context) }
            }
        }
    }
}

@Composable
fun GeneralLocationSection(
    config: LauncherConfig,
    prefs: LauncherPreferences,
    scope: kotlinx.coroutines.CoroutineScope,
    context: Context,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<PalestinianCity>,
    onSearchResultsChange: (List<PalestinianCity>) -> Unit,
    locationStatus: String,
    onLocationStatusChange: (String) -> Unit
) {
    SettingsCategoryBox("تعيين اللانشر والموقع") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingOptionPill("تعيين كلانشر افتراضي للتلفاز", false) {
                try { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
            }
            SettingOptionPill("تعرف تلقائي على موقعي", false) {
                onLocationStatusChange("جاري تحديد الموقع...")
                scope.launch {
                    val detected = LiveRepository.autoDetectLocation()
                    if (detected != null) {
                        prefs.setLocation(detected)
                        onLocationStatusChange("تم الضبط: ${detected.name}")
                    } else onLocationStatusChange("تعذر التحديد التلقائي")
                }
            }
        }
        Text(locationStatus, color = Color(0xFFFBBF24), fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            var searchFieldFocused by remember { mutableStateOf(false) }
            var isEditing by remember { mutableStateOf(false) }
            val searchFocusRequester = remember { FocusRequester() }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("ابحث عن مدينة أو قرية...", color = Color.Gray) },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(searchFocusRequester)
                    .onFocusChanged { searchFieldFocused = it.isFocused }
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.DirectionCenter, Key.Enter -> {
                                    if (searchFieldFocused && !isEditing) {
                                        isEditing = true
                                        true
                                    } else if (!searchFieldFocused) {
                                        searchFocusRequester.requestFocus()
                                        searchFieldFocused = true
                                        true
                                    } else {
                                        false
                                    }
                                }
                                Key.Back -> {
                                    if (isEditing) {
                                        isEditing = false
                                        true
                                    } else if (searchFieldFocused) {
                                        searchFieldFocused = false
                                        true
                                    } else {
                                        false
                                    }
                                }
                                else -> false
                            }
                        } else false
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = if (isEditing) ImeAction.Search else ImeAction.None,
                    keyboardType = KeyboardType.Text
                ),
                visualTransformation = VisualTransformation { TextFieldValue(it.text) }
            )
            SettingOptionPill("بحث", false) {
                if (searchQuery.isNotBlank()) {
                    scope.launch { onSearchResultsChange(LiveRepository.searchLocationByName(searchQuery)) }
                }
            }
        }
        if (searchResults.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(searchResults) { res ->
                    SettingOptionPill("${res.name} (${res.country})", false) {
                        scope.launch {
                            prefs.setLocation(res)
                            onLocationStatusChange("تم اختيار: ${res.name}")
                            onSearchResultsChange(emptyList())
                        }
                    }
                }
            }
        }
        Text("مدن وقرى فلسطين:", color = Color(0xFFCBD5E1), fontSize = 13.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(PalestineData.palestineCities) { city ->
                SettingOptionPill(city.name, config.cityName == city.name) {
                    scope.launch { prefs.setLocation(city); onLocationStatusChange("الموقع: ${city.name}") }
                }
            }
        }
        Text("البلدان والعواصم العالمية:", color = Color(0xFFCBD5E1), fontSize = 13.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(PalestineData.worldCapitals) { city ->
                SettingOptionPill("${city.name} (${city.country})", config.cityName == city.name) {
                    scope.launch { prefs.setLocation(city); onLocationStatusChange("الموقع: ${city.name}") }
                }
            }
        }
    }
}

@Composable
fun WallpapersSection(
    config: LauncherConfig,
    prefs: LauncherPreferences,
    scope: kotlinx.coroutines.CoroutineScope,
    onPickDeviceImage: () -> Unit
) {
    SettingsCategoryBox("الخلفيات والمؤقت التلقائي وصور الجهاز") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingOptionPill("اختر صورة من الجهاز", config.customWallpaperUri.isNotEmpty(), onClick = onPickDeviceImage)
            if (config.customWallpaperUri.isNotEmpty()) {
                SettingOptionPill("إلغاء صورة الجهاز", false) {
                    scope.launch { prefs.updateConfig { it[LauncherPreferences.CUSTOM_WALLPAPER_URI] = "" } }
                }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(LiveRepository.natureWallpapers.size) { idx ->
                val title = LiveRepository.natureWallpapers[idx].first
                SettingOptionPill(title, config.customWallpaperUri.isEmpty() && config.wallpaperIndex == idx) {
                    scope.launch {
                        prefs.updateConfig {
                            it[LauncherPreferences.CUSTOM_WALLPAPER_URI] = ""
                            it[LauncherPreferences.WALLPAPER_IDX] = idx
                        }
                    }
                }
            }
        }
        Text("مؤقت تغيير الخلفية تلقائياً:", color = Color(0xFFCBD5E1), fontSize = 13.sp)
        val intervals = listOf(0 to "إيقاف", 1 to "كل ١ دقيقة", 5 to "كل ٥ دقائق", 15 to "كل ١٥ دقيقة", 30 to "كل ٣٠ دقيقة")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(intervals) { (mins, label) ->
                SettingOptionPill(label, config.wallpaperIntervalMinutes == mins) {
                    scope.launch { prefs.updateConfig { it[LauncherPreferences.WALLPAPER_INTERVAL] = mins } }
                }
            }
        }
    }
}

@Composable
fun LiveWallpaperLibrarySection(
    config: LauncherConfig,
    prefs: LauncherPreferences,
    scope: kotlinx.coroutines.CoroutineScope,
    context: Context
) {
    SettingsCategoryBox("مكتبة الخلفيات الحية المتجددة") {
        Text("اختر المكتبة الحية:", color = Color(0xFFCBD5E1), fontSize = 13.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(LiveRepository.LiveWallpaperCategory.values()) { cat ->
                val selected = config.liveWallpaperCategory == cat.title
                SettingOptionPill(cat.title, selected) {
                    scope.launch {
                        prefs.saveLiveWallpaperCategory(cat.title)
                        val list = LiveRepository.fetchLiveWallpapers(context, cat)
                        if (list.isNotEmpty()) {
                            prefs.updateConfig {
                                it[LauncherPreferences.CUSTOM_WALLPAPER_URI] = ""
                                it[LauncherPreferences.WALLPAPER_IDX] = 0
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingOptionPill("تغيير الخلفية الآن", false) {
                scope.launch {
                    val cat = LiveRepository.LiveWallpaperCategory.fromTitle(config.liveWallpaperCategory)
                    val list = LiveRepository.fetchLiveWallpapers(context, cat)
                    if (list.isNotEmpty()) {
                        val next = (config.wallpaperIndex + 1) % list.size
                        prefs.updateConfig {
                            it[LauncherPreferences.CUSTOM_WALLPAPER_URI] = ""
                            it[LauncherPreferences.WALLPAPER_IDX] = next
                        }
                    }
                }
            }
        }
        Text("مدة التقليب التلقائي:", color = Color(0xFFCBD5E1), fontSize = 13.sp)
        val libIntervals = listOf(1 to "كل دقيقة", 5 to "٥ دقائق", 0 to "عند الفتح")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(libIntervals) { (mins, label) ->
                SettingOptionPill(label, config.wallpaperIntervalMinutes == mins || (mins == 0 && config.wallpaperIntervalMinutes == 0)) {
                    scope.launch { prefs.updateConfig { it[LauncherPreferences.WALLPAPER_INTERVAL] = mins } }
                }
            }
        }
    }
}

@Composable
fun AnimatedWallpapersSection(
    config: LauncherConfig,
    prefs: LauncherPreferences,
    scope: kotlinx.coroutines.CoroutineScope
) {
    SettingsCategoryBox("الخلفيات الحية المتحركة") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SettingOptionPill("تفعيل الخلفية الحية", config.liveWallpaperEnabled) {
                scope.launch { prefs.updateConfig { it[LauncherPreferences.LIVE_WALLPAPER_ENABLED] = !config.liveWallpaperEnabled } }
            }
            SettingOptionPill("إيقاف", !config.liveWallpaperEnabled) {
                scope.launch { prefs.updateConfig { it[LauncherPreferences.LIVE_WALLPAPER_ENABLED] = false } }
            }
        }
        Text("نوع الحركة:", color = Color(0xFFCBD5E1), fontSize = 13.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(liveWallpapers.size) { idx ->
                val opt = liveWallpapers[idx]
                SettingOptionPill(opt.title, config.liveWallpaperType == opt.id) {
                    scope.launch { prefs.updateConfig { it[LauncherPreferences.LIVE_WALLPAPER_TYPE] = opt.id } }
                }
            }
        }
        Text("فاصل التجديد (بالدقائق):", color = Color(0xFFCBD5E1), fontSize = 13.sp)
        val intervals = listOf(1 to "كل ١ دقيقة", 5 to "كل ٥ دقائق", 10 to "كل ١٠ دقائق", 30 to "كل ٣٠ دقيقة", 60 to "كل ساعة")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(intervals) { (mins, label) ->
                SettingOptionPill(label, config.liveWallpaperIntervalMinutes == mins) {
                    scope.launch { prefs.updateConfig { it[LauncherPreferences.LIVE_WALLPAPER_INTERVAL] = mins } }
                }
            }
        }
    }
}

@Composable
fun ClockStylesSection(
    config: LauncherConfig,
    prefs: LauncherPreferences,
    scope: kotlinx.coroutines.CoroutineScope
) {
    SettingsCategoryBox("أشكال الساعة والتاريخ") {
        val styles = listOf("رقمي عصري", "بطاقة زجاجية", "عقارب دائرية", "عمودي كبير")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(styles.size) { idx ->
                SettingOptionPill(styles[idx], config.clockStyle == idx) {
                    scope.launch { prefs.updateConfig { it[LauncherPreferences.CLOCK_STYLE] = idx } }
                }
            }
        }
        val dateModes = listOf("هجري وميلادي", "هجري فقط", "ميلادي فقط", "إخفاء التاريخ")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(dateModes.size) { idx ->
                SettingOptionPill(dateModes[idx], config.dateDisplayMode == idx) {
                    scope.launch { prefs.updateConfig { it[LauncherPreferences.DATE_MODE] = idx } }
                }
            }
        }
    }
}

@Composable
fun CardTransparencySection(
    config: LauncherConfig,
    prefs: LauncherPreferences,
    scope: kotlinx.coroutines.CoroutineScope
) {
    SettingsCategoryBox("الشفافية والأحجام") {
        StepAdjustRow(
            title = "شفافية البطاقة",
            valueLabel = PalestineData.toArabicDigits("${(config.cardBgOpacity * 100).toInt()}%"),
            onDecrease = { scope.launch { prefs.updateConfig { it[LauncherPreferences.CARD_OPACITY] = (config.cardBgOpacity - 0.1f).coerceIn(0.1f, 1f) } } },
            onIncrease = { scope.launch { prefs.updateConfig { it[LauncherPreferences.CARD_OPACITY] = (config.cardBgOpacity + 0.1f).coerceIn(0.1f, 1f) } } }
        )
        StepAdjustRow(
            title = "حجم أيقونات التطبيقات",
            valueLabel = PalestineData.toArabicDigits("${(config.appBaseScale * 100).toInt()}%"),
            onDecrease = { scope.launch { prefs.updateConfig { it[LauncherPreferences.APP_SCALE] = (config.appBaseScale - 0.05f).coerceIn(0.75f, 1.3f) } } },
            onIncrease = { scope.launch { prefs.updateConfig { it[LauncherPreferences.APP_SCALE] = (config.appBaseScale + 0.05f).coerceIn(0.75f, 1.3f) } } }
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { SettingOptionPill("الشبكة", config.showTopWifi) { scope.launch { prefs.updateConfig { it[LauncherPreferences.SHOW_WIFI] = !config.showTopWifi } } } }
            item { SettingOptionPill("إذاعة القرآن", config.showTopQuranRadio) { scope.launch { prefs.updateConfig { it[LauncherPreferences.SHOW_QURAN] = !config.showTopQuranRadio } } } }
            item { SettingOptionPill("المصحف", config.showTopQuranBook) { scope.launch { prefs.updateConfig { it[LauncherPreferences.SHOW_BOOK] = !config.showTopQuranBook } } } }
            item { SettingOptionPill("شاشة التوقف", config.showTopScreensaver) { scope.launch { prefs.updateConfig { it[LauncherPreferences.SHOW_SS] = !config.showTopScreensaver } } } }
            item { SettingOptionPill("VPN", config.showTopVpn) { scope.launch { prefs.updateConfig { it[LauncherPreferences.SHOW_VPN] = !config.showTopVpn } } } }
        }
    }
}

@Composable
fun CardFlipSection(
    config: LauncherConfig,
    prefs: LauncherPreferences,
    scope: kotlinx.coroutines.CoroutineScope
) {
    SettingsCategoryBox("تقليب البطاقة التفاعلية") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingOptionPill("تلقائي", config.cardFlipMode == 0) { scope.launch { prefs.updateConfig { it[LauncherPreferences.CARD_FLIP_MODE] = 0 } } }
            SettingOptionPill("يدوي", config.cardFlipMode == 1) { scope.launch { prefs.updateConfig { it[LauncherPreferences.CARD_FLIP_MODE] = 1 } } }
            SettingOptionPill("تلقائي (يدوي عند الوقوف)", config.cardFlipMode == 2) { scope.launch { prefs.updateConfig { it[LauncherPreferences.CARD_FLIP_MODE] = 2 } } }
        }
        if (config.cardFlipMode != 1) {
            Text("فاصل التقليب التلقائي (بالدقائق):", color = Color(0xFFCBD5E1), fontSize = 13.sp)
            val rotateIntervals = listOf(1 to "دقيقة واحدة", 2 to "دقيقتان", 5 to "٥ دقائق", 10 to "١٠ دقائق", 15 to "١٥ دقيقة", 30 to "٣٠ دقيقة")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(rotateIntervals) { (mins, label) ->
                    SettingOptionPill(label, config.tabAutoRotateIntervalMinutes == mins) {
                        scope.launch { prefs.updateConfig { it[LauncherPreferences.TAB_ROTATE_INTERVAL_MINUTES] = mins } }
                    }
                }
            }
        }
    }
}

@Composable
fun TabOrderEditor(
    tabs: List<TabItem>,
    order: List<String>,
    visible: Set<String>,
    onVisibilityToggle: (String, Boolean) -> Unit,
    onOrderChange: (List<String>) -> Unit
) {
    val labels = tabs.associate { it.key to it.title }
    var currentOrder by remember { mutableStateOf(order.toMutableList()) }
    var currentVisible by remember { mutableStateOf(visible.toMutableSet()) }
    LaunchedEffect(order, visible) {
        currentOrder = order.toMutableList()
        currentVisible = visible.toMutableSet()
    }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(currentOrder.size) { i ->
            val key = currentOrder[i]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SettingOptionPill("${i + 1}. ${labels[key] ?: key}", currentVisible.contains(key)) {
                        val next = if (currentVisible.contains(key)) currentVisible - key else currentVisible + key
                        currentVisible = next
                        onVisibilityToggle(key, next.contains(key))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (i > 0) {
                        SettingOptionPill("▲", false) {
                            val n = currentOrder.toMutableList()
                            val t = n.removeAt(i)
                            n.add(i - 1, t)
                            currentOrder = n
                        }
                    }
                    if (i < currentOrder.size - 1) {
                        SettingOptionPill("▼", false) {
                            val n = currentOrder.toMutableList()
                            val t = n.removeAt(i)
                            n.add(i + 1, t)
                            currentOrder = n
                        }
                    }
                }
            }
        }
        Button(onClick = { onOrderChange(currentOrder) }) {
            Text("حفظ الترتيب", color = Color(0xFF34D399))
        }
    }
}

@Composable
fun CardTabsSection(
    config: LauncherConfig,
    prefs: LauncherPreferences,
    scope: kotlinx.coroutines.CoroutineScope
) {
    SettingsCategoryBox("إظهار وإخفاء وترتيب التبويبات — البطاقة الرئيسية") {
        TabOrderEditor(
            tabs = cardTabs,
            order = config.cardTabOrder,
            visible = config.cardTabVisible,
            onVisibilityToggle = { key, enabled ->
                scope.launch {
                    val set = if (enabled) config.cardTabVisible + key else config.cardTabVisible - key
                    prefs.updateConfig { it[LauncherPreferences.CARD_TAB_VISIBLE] = set }
                }
            },
            onOrderChange = { order ->
                scope.launch { prefs.updateConfig { it[LauncherPreferences.CARD_TAB_ORDER] = order.joinToString("::") } }
            }
        )
    }
}

@Composable
fun ExpansionTabsSection(
    config: LauncherConfig,
    prefs: LauncherPreferences,
    scope: kotlinx.coroutines.CoroutineScope
) {
    SettingsCategoryBox("إظهار وإخفاء وترتيب التبويبات — نافذة التوسعة") {
        TabOrderEditor(
            tabs = cardTabs,
            order = config.expansionTabOrder,
            visible = config.expansionTabVisible,
            onVisibilityToggle = { key, enabled ->
                scope.launch {
                    val set = if (enabled) config.expansionTabVisible + key else config.expansionTabVisible - key
                    prefs.updateConfig { it[LauncherPreferences.EXPANSION_TAB_VISIBLE] = set }
                }
            },
            onOrderChange = { order ->
                scope.launch { prefs.updateConfig { it[LauncherPreferences.EXPANSION_TAB_ORDER] = order.joinToString("::") } }
            }
        )
    }
}

@Composable
fun AppBarSection(
    config: LauncherConfig,
    prefs: LauncherPreferences,
    scope: kotlinx.coroutines.CoroutineScope,
    allPairs: List<String>,
    onOpenLiveEditor: () -> Unit,
    onOpenScreensaverEditor: () -> Unit
) {
    SettingsCategoryBox("شريط الأسعار والتخطيط المباشر") {
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

@Composable
fun QuranRadioSection(
    config: LauncherConfig,
    prefs: LauncherPreferences,
    scope: kotlinx.coroutines.CoroutineScope,
    quranRadios: List<QuranRadio>,
    isLoadingRadios: Boolean,
    onIsLoadingRadiosChange: (Boolean) -> Unit
) {
    SettingsCategoryBox("إذاعة القرآن الكريم") {
        val modes = listOf("يدوي / دائم", "الشاشة الرئيسية فقط", "شاشة التوقف فقط", "تشغيل مجدول")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(modes.size) { idx ->
                SettingOptionPill(modes[idx], config.quranPlayMode == idx) {
                    scope.launch { prefs.updateConfig { it[LauncherPreferences.QURAN_PLAY_MODE] = idx } }
                }
            }
        }
        if (isLoadingRadios) {
            Text("جاري تحميل قائمة الإذاعات...", color = Color(0xFFFBBF24), fontSize = 13.sp)
        } else if (quranRadios.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(quranRadios) { radio ->
                    SettingOptionPill(radio.name, config.quranStreamUrl == radio.url) {
                        scope.launch {
                            prefs.updateConfig { it[LauncherPreferences.QURAN_URL] = radio.url }
                            prefs.saveLastQuranRadioUrl(radio.url)
                        }
                    }
                }
            }
        } else {
            Text("تعذر تحميل الإذاعات، يتم استخدام القائمة الافتراضية", color = Color(0xFFF87171), fontSize = 13.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(quranRadioStations) { (title, urls) ->
                    val primaryUrl = urls.first()
                    SettingOptionPill(title, config.quranStreamUrl == primaryUrl) {
                        scope.launch {
                            prefs.updateConfig { it[LauncherPreferences.QURAN_URL] = primaryUrl }
                            prefs.saveLastQuranRadioUrl(primaryUrl)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdhanSection(
    config: LauncherConfig,
    prefs: LauncherPreferences,
    scope: kotlinx.coroutines.CoroutineScope,
    context: Context
) {
    SettingsCategoryBox("إعدادات الأذان") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingOptionPill("تشغيل الأذان", config.adhanEnabled) {
                scope.launch { prefs.updateConfig { it[LauncherPreferences.ADHAN_ENABLED] = !config.adhanEnabled } }
            }
            SettingOptionPill("الفجر فقط", config.adhanPlayFajrOnly) {
                scope.launch { prefs.updateConfig { it[LauncherPreferences.ADHAN_FAJR_ONLY] = !config.adhanPlayFajrOnly } }
            }
        }
        Text("المؤذن:", color = Color(0xFFCBD5E1), fontSize = 13.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(AdhanManager.muadhinList) { (id, label) ->
                SettingOptionPill(label, config.adhanMuadhinId == id) {
                    scope.launch { prefs.updateConfig { it[LauncherPreferences.ADHAN_MUADHIN_ID] = id } }
                }
            }
        }
        StepAdjustRow(
            title = "مستوى صوت الأذان",
            valueLabel = PalestineData.toArabicDigits("${config.adhanVolume}%"),
            onDecrease = { scope.launch { prefs.updateConfig { it[LauncherPreferences.ADHAN_VOLUME] = (config.adhanVolume - 10).coerceIn(10, 100) } } },
            onIncrease = { scope.launch { prefs.updateConfig { it[LauncherPreferences.ADHAN_VOLUME] = (config.adhanVolume + 10).coerceIn(10, 100) } } }
        )
        Text("مدة الأذان:", color = Color(0xFFCBD5E1), fontSize = 13.sp)
        val lengthModes = listOf(0 to "كامل", 1 to "مختصر (١٥ ثانية)")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(lengthModes) { (mode, label) ->
                SettingOptionPill(label, config.adhanLengthMode == mode) {
                    scope.launch { prefs.updateConfig { it[LauncherPreferences.ADHAN_LENGTH_MODE] = mode } }
                }
            }
        }
        Text("التذكير قبل الأذان:", color = Color(0xFFCBD5E1), fontSize = 13.sp)
        val reminderMins = listOf(0 to "إيقاف", 5 to "٥ دقائق", 10 to "١٠ دقائق", 15 to "١٥ دقيقة", 20 to "٢٠ دقيقة")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(reminderMins) { (mins, label) ->
                SettingOptionPill(label, config.adhanReminderMinutes == mins) {
                    scope.launch { prefs.updateConfig { it[LauncherPreferences.ADHAN_REMINDER_MINUTES] = mins } }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingOptionPill("تجربة الأذان", false) {
                scope.launch {
                    val next = AdhanManager.getNextPrayer()
                    val pname = next?.name ?: "الفجر"
                    AdhanManager.playAdhan(context, pname, config.adhanMuadhinId, config.adhanLengthMode, config.adhanVolume)
                }
            }
            if (AdhanManager.isPlaying) {
                SettingOptionPill("إيقاف", false) { AdhanManager.stop() }
            }
        }
    }
}

@Composable
fun TickerElementsSection(
    config: LauncherConfig,
    prefs: LauncherPreferences,
    scope: kotlinx.coroutines.CoroutineScope
) {
    SettingsCategoryBox("عناصر الشريط المتحرك") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SettingOptionPill("الصلاة القادمة", config.tickerShowNextPrayer) {
                scope.launch { prefs.updateConfig { it[LauncherPreferences.TICKER_SHOW_NEXT_PRAYER] = !config.tickerShowNextPrayer } }
            }
            SettingOptionPill("رمضان", config.tickerShowRamadan) {
                scope.launch { prefs.updateConfig { it[LauncherPreferences.TICKER_SHOW_RAMADAN] = !config.tickerShowRamadan } }
            }
            SettingOptionPill("أعياد الميلاد", config.tickerShowBirthday) {
                scope.launch { prefs.updateConfig { it[LauncherPreferences.TICKER_SHOW_BIRTHDAY] = !config.tickerShowBirthday } }
            }
            SettingOptionPill("العملات", config.tickerShowPrices) {
                scope.launch { prefs.updateConfig { it[LauncherPreferences.TICKER_SHOW_PRICES] = !config.tickerShowPrices } }
            }
            SettingOptionPill("المباريات", config.tickerShowMatches) {
                scope.launch { prefs.updateConfig { it[LauncherPreferences.TICKER_SHOW_MATCHES] = !config.tickerShowMatches } }
            }
        }
        Text("ترتيب العناصر:", color = Color(0xFFCBD5E1), fontSize = 13.sp)
        val orderLabels = mapOf(
            "prayer" to "الصلاة القادمة",
            "ramadan" to "رمضان",
            "birthday" to "أعياد الميلاد",
            "prices" to "العملات",
            "matches" to "المباريات"
        )
        var order by remember { mutableStateOf(config.tickerElementOrder.toMutableList()) }
        LaunchedEffect(config.tickerElementOrder) {
            order = config.tickerElementOrder.toMutableList()
        }
        val move = { i: Int, j: Int ->
            val n = order.toMutableList()
            val t = n[i]
            n[i] = n[j]
            n[j] = t
            order = n
        }
        LazyColumn {
            items(order.size) { i ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${i + 1}. ${orderLabels[order[i]] ?: order[i]}", color = Color.White, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (i > 0) {
                            SettingOptionPill("↑", false) { move(i, i - 1) }
                        }
                        if (i < order.size - 1) {
                            SettingOptionPill("↓", false) { move(i, i + 1) }
                        }
                    }
                }
            }
        }
        Button(onClick = {
            scope.launch { prefs.updateConfig { it[LauncherPreferences.TICKER_ELEMENT_ORDER] = order.joinToString("::") } }
        }) {
            Text("حفظ الترتيب", color = Color(0xFF34D399))
        }
    }
}

@Composable
fun FavoriteTeamSection(
    config: LauncherConfig,
    prefs: LauncherPreferences,
    scope: kotlinx.coroutines.CoroutineScope
) {
    SettingsCategoryBox("الفريق الرياضي المفضل") {
        Text("اختر فريقك المفضل لعرضه في شريط المباريات:", color = Color(0xFFCBD5E1), fontSize = 13.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(config.favoriteTeams) { team ->
                val selected = config.favoriteTeamId == team.id
                SettingOptionPill("${team.name}  •  ${team.league}", selected) {
                    scope.launch { prefs.updateConfig {
                        it[LauncherPreferences.FAVORITE_TEAM_ID] = team.id
                        it[LauncherPreferences.FAVORITE_TEAM_NAME] = team.name
                        it[LauncherPreferences.FAVORITE_TEAM_LEAGUE] = team.league
                    } }
                }
            }
            item {
                if (config.favoriteTeamId.isNotBlank()) {
                    Text("المحدد: ${config.favoriteTeamName} (${config.favoriteTeamLeague})", color = Color(0xFF38BDF8), fontSize = 13.sp)
                    SettingOptionPill("إلغاء التحديد", false) {
                        scope.launch { prefs.updateConfig {
                            it[LauncherPreferences.FAVORITE_TEAM_ID] = ""
                            it[LauncherPreferences.FAVORITE_TEAM_NAME] = ""
                            it[LauncherPreferences.FAVORITE_TEAM_LEAGUE] = ""
                        } }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsSourcesSection(
    config: LauncherConfig,
    prefs: LauncherPreferences,
    scope: kotlinx.coroutines.CoroutineScope
) {
    SettingsCategoryBox("إدارة مصادر الأخبار والرياضة") {
        val selectedCountry = (PalestineData.palestineCities + PalestineData.worldCapitals).firstOrNull { it.name == config.cityName }?.country ?: "Palestine"
        val allSources = (LiveRepository.countryNewsSources[selectedCountry] ?: emptyList()) + LiveRepository.worldNewsSources + LiveRepository.techNewsSources
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(allSources) { source ->
                val enabled = source.id !in config.disabledNewsSources
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${source.name}  •  ${source.country}", color = Color.White, fontSize = 14.sp)
                    SettingOptionPill(if (enabled) "تشغيل" else "إيقاف", enabled) {
                        scope.launch { prefs.toggleNewsSource(source.id, !enabled) }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomBarSection(
    config: LauncherConfig,
    prefs: LauncherPreferences,
    scope: kotlinx.coroutines.CoroutineScope
) {
    SettingsCategoryBox("شريط التذكير (الأسفل/الأعلى)") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingOptionPill("أسفل الشاشة", config.tickerBarPosition == 0) {
                scope.launch { prefs.updateConfig { it[LauncherPreferences.TICKER_BAR_POSITION] = 0 } }
            }
            SettingOptionPill("أعلى الشاشة", config.tickerBarPosition == 1) {
                scope.launch { prefs.updateConfig { it[LauncherPreferences.TICKER_BAR_POSITION] = 1 } }
            }
        }
        StepAdjustRow(
            title = "ارتفاع شريط التذكير",
            valueLabel = PalestineData.toArabicDigits("${config.tickerBarHeight} dp"),
            onDecrease = { scope.launch { prefs.updateConfig { it[LauncherPreferences.TICKER_BAR_HEIGHT] = (config.tickerBarHeight - 4).coerceAtLeast(24) } } },
            onIncrease = { scope.launch { prefs.updateConfig { it[LauncherPreferences.TICKER_BAR_HEIGHT] = (config.tickerBarHeight + 4).coerceAtMost(120) } } }
        )
        StepAdjustRow(
            title = "حجم خط شريط التذكير",
            valueLabel = PalestineData.toArabicDigits("${config.tickerBarFontSize} sp"),
            onDecrease = { scope.launch { prefs.updateConfig { it[LauncherPreferences.TICKER_BAR_FONT_SIZE] = (config.tickerBarFontSize - 2).coerceAtLeast(10) } } },
            onIncrease = { scope.launch { prefs.updateConfig { it[LauncherPreferences.TICKER_BAR_FONT_SIZE] = (config.tickerBarFontSize + 2).coerceAtMost(22) } } }
        )
        StepAdjustRow(
            title = "شفافية شريط التذكير",
            valueLabel = PalestineData.toArabicDigits("${(config.tickerBgOpacity * 100).toInt()}%"),
            onDecrease = { scope.launch { prefs.updateConfig { it[LauncherPreferences.TICKER_OPACITY] = (config.tickerBgOpacity - 0.1f).coerceIn(0.1f, 1f) } } },
            onIncrease = { scope.launch { prefs.updateConfig { it[LauncherPreferences.TICKER_OPACITY] = (config.tickerBgOpacity + 0.1f).coerceIn(0.1f, 1f) } } }
        )
    }
}

@Composable
fun DeviceStatusSection(
    config: LauncherConfig,
    prefs: LauncherPreferences,
    scope: kotlinx.coroutines.CoroutineScope,
    context: Context
) {
    SettingsCategoryBox("حالة الجهاز") {
        var avail by remember { mutableStateOf(0L) }
        var total by remember { mutableStateOf(0L) }
        var gb by remember { mutableStateOf(0L) }
        LaunchedEffect(Unit) {
            try {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val mi = ActivityManager.MemoryInfo()
                am.getMemoryInfo(mi)
                avail = mi.availMem / (1024 * 1024)
                total = mi.totalMem / (1024 * 1024)
            } catch (e: Exception) {}
            try {
                val stat = StatFs(Environment.getDataDirectory().path)
                gb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024 * 1024)
            } catch (e: Exception) {}
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("حالة جهاز TV Box:", color = Color(0xFF38BDF8))
            Text(PalestineData.toArabicDigits("الذاكرة المتاحة: $avail MB / $total MB"), color = Color.White)
            Text(PalestineData.toArabicDigits("المساحة المتبقية: $gb GB"), color = Color.White)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("البطاقة المخصصة الشاملة", color = Color(0xFFCBD5E1), fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SettingOptionPill("تفعيل البطاقة الشاملة لجميع التبويبات", config.customCardAllTabs) {
                scope.launch { prefs.updateConfig { it[LauncherPreferences.CUSTOM_CARD_ALL_TABS] = !config.customCardAllTabs } }
            }
            SettingOptionPill("إيقاف", !config.customCardAllTabs) {
                scope.launch { prefs.updateConfig { it[LauncherPreferences.CUSTOM_CARD_ALL_TABS] = false } }
            }
        }
        Text("عند التفعيل، يعرض التبويب الأخير (الكل) جميع المحتويات في بطاقة واحدة قابلة للتمرير.", color = Color(0xFF94A3B8), fontSize = 12.sp)
    }
}
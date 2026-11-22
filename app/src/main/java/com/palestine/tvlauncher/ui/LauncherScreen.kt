package com.palestine.tvlauncher.ui

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.palestine.tvlauncher.data.*
import com.palestine.tvlauncher.network.*
import com.palestine.util.AppManager
import com.palestine.util.TvAppInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
private fun ArabicClockDisplay(timeZoneId: String, clockStyle: Int) {
    var hourText by remember { mutableStateOf("") }
    var minuteText by remember { mutableStateOf("") }
    var amPmText by remember { mutableStateOf("") }
    var hourInt by remember { mutableIntStateOf(12) }
    var minInt by remember { mutableIntStateOf(0) }
    var secInt by remember { mutableIntStateOf(0) }

    LaunchedEffect(timeZoneId) {
        val tz = TimeZone.getTimeZone(timeZoneId)
        val sdfHour = SimpleDateFormat("hh", Locale.US).apply { timeZone = tz }
        val sdfMin = SimpleDateFormat("mm", Locale.US).apply { timeZone = tz }
        val sdfAmPm = SimpleDateFormat("a", Locale("ar")).apply { timeZone = tz }
        while (true) {
            val now = Date()
            val cal = Calendar.getInstance(tz).apply { time = now }
            hourText = PalestineData.toArabicDigits(sdfHour.format(now))
            minuteText = PalestineData.toArabicDigits(sdfMin.format(now))
            amPmText = sdfAmPm.format(now)
            hourInt = cal.get(Calendar.HOUR)
            minInt = cal.get(Calendar.MINUTE)
            secInt = cal.get(Calendar.SECOND)
            delay(1000L)
        }
    }

    when (clockStyle) {
        1 -> {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.72f),
                border = BorderStroke(2.dp, Color(0xFF38BDF8).copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = amPmText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "$hourText:$minuteText",
                        fontSize = 74.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 74.sp
                    )
                }
            }
        }
        2 -> {
            Box(
                modifier = Modifier.size(164.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        color = Color(0xFF0F172A).copy(alpha = 0.78f),
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF38BDF8),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    for (i in 0 until 12) {
                        val angle = Math.toRadians((i * 30 - 90).toDouble())
                        val startR = radius * 0.82f
                        val endR = radius * 0.92f
                        drawLine(
                            color = Color.White.copy(alpha = 0.75f),
                            start = Offset(
                                center.x + (startR * cos(angle)).toFloat(),
                                center.y + (startR * sin(angle)).toFloat()
                            ),
                            end = Offset(
                                center.x + (endR * cos(angle)).toFloat(),
                                center.y + (endR * sin(angle)).toFloat()
                            ),
                            strokeWidth = if (i % 3 == 0) 3.dp.toPx() else 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    val hourAngle = Math.toRadians(((hourInt + minInt / 60.0) * 30.0) - 90.0)
                    drawLine(
                        color = Color.White,
                        start = center,
                        end = Offset(
                            center.x + (radius * 0.52f * cos(hourAngle)).toFloat(),
                            center.y + (radius * 0.52f * sin(hourAngle)).toFloat()
                        ),
                        strokeWidth = 5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    val minAngle = Math.toRadians((minInt * 6.0) - 90.0)
                    drawLine(
                        color = Color(0xFF38BDF8),
                        start = center,
                        end = Offset(
                            center.x + (radius * 0.74f * cos(minAngle)).toFloat(),
                            center.y + (radius * 0.74f * sin(minAngle)).toFloat()
                        ),
                        strokeWidth = 3.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    val secAngle = Math.toRadians((secInt * 6.0) - 90.0)
                    drawLine(
                        color = Color(0xFFFBBF24),
                        start = center,
                        end = Offset(
                            center.x + (radius * 0.82f * cos(secAngle)).toFloat(),
                            center.y + (radius * 0.82f * sin(secAngle)).toFloat()
                        ),
                        strokeWidth = 1.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = Color(0xFFFBBF24), radius = 4.dp.toPx(), center = center)
                }
            }
        }
        3 -> {
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = amPmText,
                        fontSize = 24.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = hourText,
                        fontSize = 68.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 68.sp
                    )
                }
                Text(
                    text = minuteText,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBBF24),
                    lineHeight = 68.sp
                )
            }
        }
        else -> {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = amPmText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$hourText:$minuteText",
                    fontSize = 86.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 86.sp
                )
            }
        }
    }
}

@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { LauncherPreferences(context) }
    val config by prefs.configFlow.collectAsState(initial = LauncherConfig())
    val scope = rememberCoroutineScope()

    var installedApps by remember { mutableStateOf<List<TvAppInfo>>(emptyList()) }
    var liveFeed by remember { mutableStateOf(LiveFeedState()) }
    var prayerInfo by remember { mutableStateOf(PalestineData.PrayerDisplay()) }

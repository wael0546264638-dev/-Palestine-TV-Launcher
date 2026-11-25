package com.palestine.tvlauncher.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.PowerManager
import android.util.Log
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.audio.AudioAttributes as ExoAudioAttributes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.*

object AdhanManager {
    private const val TAG = "AdhanManager"

    private const val FALLBACK_ADHAN_URL = "https://archive.org/download/Adhan-Makkah/Adhan-Makkah.mp3"

    private val MUADHIN_URLS = mapOf(
        "makkah_alimla" to "https://www.islamcan.com/audio/adhan/Adhan-Makkah.mp3",
        "madinah_osamabakhri" to "https://www.islamcan.com/audio/adhan/Adhan-Madinah.mp3",
        "abdulbasit" to "https://www.everyayah.com/data/Abdul_Basit_Murattal_192kbps/001001.mp3",
        "afasy" to "https://www.everyayah.com/data/Alafasy_128kbps/001001.mp3",
        "alaqsa" to "https://www.islamcan.com/audio/adhan/Adhan-Al-Aqsa.mp3"
    )

    private val MUADHIN_NAMES = mapOf(
        "makkah_alimla" to "الحرم المكي - علي ملا",
        "madinah_osamabakhri" to "المسجد النبوي - عصام بخاري",
        "abdulbasit" to "عبدالباسط عبدالصمد",
        "afasy" to "مشاري العفاسي",
        "alaqsa" to "أذان المسجد الأقصى"
    )

    fun getMuadhinName(id: String): String = MUADHIN_NAMES[id] ?: "الحرم المكي - علي ملا"
    fun getMuadhinUrl(id: String): String = MUADHIN_URLS[id] ?: MUADHIN_URLS["makkah_alimla"]!!
    fun getFallbackAdhanUrl(): String = FALLBACK_ADHAN_URL

    val muadhinList: List<Pair<String, String>> = MUADHIN_NAMES.toList()

    var isPlaying by mutableStateOf(false)
        private set

    private var player: ExoPlayer? = null
    private var mediaPlayerFallback: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var adhanCoroutine: kotlinx.coroutines.Job? = null

    fun init(context: Context) {
        if (wakeLock == null) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AdhanManagerWakeLock")
        }
    }

    fun release() {
        stop()
        wakeLock?.release()
        wakeLock = null
        player = null
        mediaPlayerFallback = null
    }

    data class PrayerTime(val name: String, val minutes: Int)
    private val prayerTimes = mutableListOf<PrayerTime>()

    fun setPrayerTimes(prayers: Map<String, String>) {
        val list = mutableListOf<PrayerTime>()
        for ((name, timeStr) in prayers) {
            val parts = timeStr.split(":")
            if (parts.size >= 2) {
                val h = parts[0].toIntOrNull() ?: 0
                val m = parts[1].toIntOrNull() ?: 0
                list.add(PrayerTime(name, h * 60 + m))
            }
        }
        prayerTimes.clear()
        prayerTimes.addAll(list.sortedBy { it.minutes })
    }

    fun getNextPrayer(): PrayerTime? {
        val now = Calendar.getInstance()
        val nowMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return prayerTimes.firstOrNull { it.minutes > nowMins } ?: prayerTimes.firstOrNull()
    }

    private fun cacheFile(context: Context, url: String): File {
        val cacheDir = context.cacheDir
        val name = url.hashCode().toString() + ".mp3"
        return File(cacheDir, name)
    }

    private fun downloadToCache(context: Context, url: String): File? {
        return try {
            val f = cacheFile(context, url)
            if (!f.exists()) {
                URL(url).openStream().use { input ->
                    f.outputStream().use { out -> input.copyTo(out) }
                }
            }
            f
        } catch (e: Exception) {
            Log.e(TAG, "download failed", e)
            null
        }
    }

    fun playAdhan(context: Context, prayerName: String, muadhinId: String, lengthMode: Int, volumePercent: Int) {
        stop()
        isPlaying = true
        val url = getMuadhinUrl(muadhinId)
        val volume = (volumePercent.coerceIn(10, 100) / 100f)

        wakeLock?.acquire()

        try {
            val cached = downloadToCache(context, url) ?: downloadToCache(context, FALLBACK_ADHAN_URL)
            val playUrl = cached?.takeIf { it.exists() }?.absolutePath ?: url
            val audioAttributes = ExoAudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
            player = ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, true)
                .build()
            val item = MediaItem.fromUri(playUrl)
            player?.setMediaItem(item)
            player?.prepare()
            player?.play()
            if (lengthMode == 1) {
                adhanCoroutine = CoroutineScope(Dispatchers.IO).launch {
                    kotlinx.coroutines.delay(15_000L)
                    stop()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "ExoPlayer failed, falling back to MediaPlayer", e)
            playWithMediaPlayer(context, url, volume, lengthMode)
        }
    }

    private fun playWithMediaPlayer(context: Context, url: String, volume: Float, lengthMode: Int) {
        try {
            val cached = downloadToCache(context, url) ?: downloadToCache(context, FALLBACK_ADHAN_URL)
            val playUrl = cached?.takeIf { it.exists() }?.absolutePath ?: url
            mediaPlayerFallback = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build())
                setDataSource(playUrl)
                setVolume(volume, volume)
                setOnPreparedListener { it.start() }
                setOnCompletionListener { stop() }
                setOnErrorListener { _, _, _ ->
                    try {
                        val r = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                        r?.play()
                    } catch (_: Exception) {}
                    stop()
                    true
                }
                prepareAsync()
            }
            if (lengthMode == 1) {
                adhanCoroutine = CoroutineScope(Dispatchers.IO).launch {
                    kotlinx.coroutines.delay(15_000L)
                    stop()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer failed too", e)
            try {
                val r = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                r?.play()
            } catch (_: Exception) {}
            stop()
        }
    }

    fun stop() {
        adhanCoroutine?.cancel()
        adhanCoroutine = null
        player?.stop()
        player?.release()
        player = null
        mediaPlayerFallback?.stop()
        mediaPlayerFallback?.release()
        mediaPlayerFallback = null
        wakeLock?.release()
        isPlaying = false
    }
}

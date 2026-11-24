package com.palestine.tvlauncher.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.PowerManager
import android.util.Log
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.audio.AudioAttributes as ExoAudioAttributes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

object AdhanManager {
    private const val TAG = "AdhanManager"

    private val MUADHIN_URLS = mapOf(
        "makkah_alimla" to "https://mp3quran.net/adhans/Al-Masjid_al-Haram_Ali_Mulla.mp3",
        "madinah_osamabakhri" to "https://mp3quran.net/adhans/Al-Masjid_an-Nabawi_Osama_Bakhit.mp3",
        "abdulbasit" to "https://mp3quran.net/adhans/Abdelbasset_Abdessamad.mp3",
        "afasy" to "https://mp3quran.net/adhans/Mishary_Al_Alfasy.mp3",
        "alaqsa" to "https://mp3quran.net/adhans/Al-Aqsa_Mosque.mp3"
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

    fun playAdhan(context: Context, prayerName: String, muadhinId: String, lengthMode: Int, volumePercent: Int) {
        stop()
        isPlaying = true
        val url = getMuadhinUrl(muadhinId)
        val volume = (volumePercent.coerceIn(10, 100) / 100f)

        // Acquire wake lock to ensure playback continues when screen is off
        wakeLock?.acquire()

        try {
            player = ExoPlayer.Builder(context).build()
            player?.audioAttributes = ExoAudioAttributes.Builder()
                .setContentType(ExoAudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(ExoAudioAttributes.USAGE_MEDIA)
                .setVolume(volume)
                .build()
            val item = MediaItem.fromUri(url)
            player?.setMediaItem(item)
            player?.prepare()
            player?.play()
            if (lengthMode == 1) {
                adhanCoroutine = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
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
            mediaPlayerFallback = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setVolume(volume)
                    .build())
                setDataSource(url)
                setOnPreparedListener { it.start() }
                setOnCompletionListener { stop() }
                setOnErrorListener { _, _, _ -> stop(); true }
                prepareAsync()
            }
            if (lengthMode == 1) {
                adhanCoroutine = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    kotlinx.coroutines.delay(15_000L)
                    stop()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer failed too", e)
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
        // Release wake lock when stopping
        wakeLock?.release()
        isPlaying = false
    }
}
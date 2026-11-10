package com.sas.tv

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log

/**
 * Plays the full adhan after a scheduled prayer alarm.
 * Faruq Hadrawe is used for the regular prayers. Fajr uses a dedicated
 * Mecca Fajr recording so the "الصلاة خير من النوم" wording is part
 * of the recording itself rather than synthesized or spliced.
 */
class PrayerAdhanService : Service() {
    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private val channelId = "sas_tv_adhan_playback"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayer = intent?.getStringExtra("prayer") ?: "الصلاة"
        startForeground(7001, buildNotification(prayer))
        play(prayer, startId)
        return START_NOT_STICKY
    }

    private fun buildNotification(prayer: String): Notification {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "أذان SAS TV", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "تشغيل الأذان عند دخول وقت الصلاة"
                    setSound(null, null)
                    setShowBadge(false)
                }
            )
        }
        return if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("حان وقت $prayer")
                .setContentText("أذان المسجد الحرام • فاروق حضراوي")
                .setCategory(Notification.CATEGORY_ALARM)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("حان وقت $prayer")
                .setContentText("أذان المسجد الحرام")
                .setOngoing(true)
                .build()
        }
    }

    private fun play(prayer: String, startId: Int) {
        releasePlayer()
        val isFajr = prayer == "الفجر"
        val url = if (isFajr) {
            FAJR_ADHAN_URL
        } else {
            FARUQ_ADHAN_URL
        }
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        if (Build.VERSION.SDK_INT >= 26) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener { change ->
                    if (change == AudioManager.AUDIOFOCUS_LOSS) stopSelf()
                }
                .build()
            focusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }

        player = MediaPlayer().apply {
            setAudioAttributes(attributes)
            setDataSource(url)
            setOnPreparedListener { it.start() }
            setOnCompletionListener { stopPlayback(startId) }
            setOnErrorListener { _, what, extra ->
                Log.e("SAS-TV-Adhan", "Playback error what=$what extra=$extra")
                stopPlayback(startId)
                true
            }
            prepareAsync()
        }
    }

    private fun stopPlayback(startId: Int) {
        releasePlayer()
        stopForeground(true)
        stopSelf(startId)
    }

    private fun releasePlayer() {
        runCatching { player?.reset(); player?.release() }
        player = null
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= 26) {
            focusRequest?.let { runCatching { audioManager.abandonAudioFocusRequest(it) } }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }

    companion object {
        // Recording attributed by its public collection to Faruq Abdul Rahman Hadrawe,
        // Al-Haram Al-Maki. Streamed from the source repository; not copied into the APK.
        private const val FARUQ_ADHAN_URL =
            "https://raw.githubusercontent.com/Kiwifu/adhan-mp3/main/Faruq_Abdul_Rahman_Hadrawe_-_Al_Haram_Al_Maki_(%D9%81%D8%A7%D8%B1%D9%88%D9%82_%D8%B9%D8%A8%D8%AF%D8%A7%D9%84%D8%B1%D8%AD%D9%85%D9%86_%D8%AD%D8%B6%D8%B1%D8%A7%D9%88%D9%8A_-_%D8%A7%D9%84%D8%AD%D8%B1%D9%85_%D8%A7%D9%84%D9%85%D9%83%D9%8A).mp3"
        // Dedicated Fajr adhan from Al-Haram Al-Maki; the recording includes the Fajr call.
        private const val FAJR_ADHAN_URL =
            "https://raw.githubusercontent.com/Kiwifu/adhan-mp3/main/Adhan_Fajr_Al_Haram_Al_Maki_(%D8%A3%D8%B0%D8%A7%D9%86_%D8%A7%D9%84%D9%81%D8%AC%D8%B1_%D8%A7%D9%84%D8%AD%D8%B1%D9%85_%D8%A7%D9%84%D9%85%D9%83%D9%8A).mp3"
    }
}

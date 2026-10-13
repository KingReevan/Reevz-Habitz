package com.reevan.reevzhabitz.timer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Rings while the timer is up: the phone's alarm sound on a loop, on the alarm volume (so it
 * sounds even on silent, like the Clock app), and a repeating buzz, until [TimerController] stops
 * it. A foreground service, so it keeps ringing with the app closed or the phone locked.
 */
class TimerRingService : Service() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val preset = TimerController.get(this).state.value.preset ?: 0
        ServiceCompat.startForeground(
            this,
            TimerNotifications.RINGING_ID,
            TimerNotifications.ringing(this, preset),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            },
        )
        if (player == null) player = playAlarmSound()
        if (vibrator == null) vibrator = buzz()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        player?.release()
        player = null
        vibrator?.cancel()
        vibrator = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** The user's alarm sound, falling back to the notification and ringtone sounds. */
    private fun playAlarmSound(): MediaPlayer? {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val sounds = listOf(
            RingtoneManager.TYPE_ALARM,
            RingtoneManager.TYPE_NOTIFICATION,
            RingtoneManager.TYPE_RINGTONE,
        ).mapNotNull(RingtoneManager::getDefaultUri)
        for (sound in sounds) {
            val player = MediaPlayer()
            try {
                player.setAudioAttributes(attributes)
                player.setDataSource(this, sound)
                player.isLooping = true
                player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
                player.prepare()
                player.start()
                return player
            } catch (e: Exception) {
                Log.w(TAG, "Can't play $sound", e)
                player.release()
            }
        }
        return null
    }

    private fun buzz(): Vibrator? {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
        if (vibrator == null || !vibrator.hasVibrator()) return null
        // Half a second on, half a second off, repeating from the start.
        val pattern = VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(pattern, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                pattern,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            )
        }
        return vibrator
    }

    companion object {
        private const val TAG = "TimerRingService"

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, TimerRingService::class.java))
            } catch (e: IllegalStateException) {
                // Android refuses a background start only without the exact alarm that grants it;
                // the screen shows "Time's up" when the app is next opened.
                Log.w(TAG, "Can't start ringing", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TimerRingService::class.java))
        }
    }
}

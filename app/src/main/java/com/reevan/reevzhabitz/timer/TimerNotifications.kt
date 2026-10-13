package com.reevan.reevzhabitz.timer

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.reevan.reevzhabitz.MainActivity
import com.reevan.reevzhabitz.R

/**
 * The timer's two notifications. Tapping either opens the Timer screen.
 * - The countdown, while running or paused: quiet, with Pause/Resume and +1:00 (or Reset).
 * - "Time's up", while ringing: the ringing service's own notification, with Stop and +1:00.
 *   Its channel is silent because the service plays the alarm sound itself, on the alarm volume.
 *   It is a full-screen alarm notification, like the Clock app's: the pop-up stays on screen until
 *   acted on instead of sliding away after a few seconds, and it wakes a sleeping screen.
 */
object TimerNotifications {

    const val COUNTDOWN_ID = 1
    const val RINGING_ID = 2
    private const val COUNTDOWN_CHANNEL = "timer"
    private const val RINGING_CHANNEL = "timer_ringing"

    /** Extra on the intent that opens the app: show the Timer screen. */
    const val EXTRA_OPEN_TIMER = "open_timer"

    fun openTimerIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_TIMER, true)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )

    /** Posts, updates or removes the countdown notification to match [state]. */
    fun showCountdown(context: Context, state: TimerState, now: Long) {
        val manager = NotificationManagerCompat.from(context)
        if (state !is TimerState.Running && state !is TimerState.Paused) {
            manager.cancel(COUNTDOWN_ID)
            return
        }
        if (!allowed(context)) return
        ensureChannels(context)
        val left = state.left(now)
        val builder = base(context, COUNTDOWN_CHANNEL)
            .setSilent(true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
        if (state is TimerState.Running) {
            val endsAt = System.currentTimeMillis() + left
            builder
                .setContentTitle("Timer")
                .setContentText("Ends at ${DateFormat.getTimeFormat(context).format(endsAt)}")
                .setWhen(endsAt)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .addAction(action(context, R.drawable.ic_pause, "Pause", TimerReceiver.ACTION_PAUSE))
                .addAction(action(context, R.drawable.ic_timer, "+1:00", TimerReceiver.ACTION_ADD_MINUTE))
        } else {
            builder
                .setContentTitle("Timer paused")
                .setContentText("${formatTimer(left)} left")
                .setShowWhen(false)
                .addAction(action(context, R.drawable.ic_play, "Resume", TimerReceiver.ACTION_RESUME))
                .addAction(action(context, R.drawable.ic_stop, "Reset", TimerReceiver.ACTION_RESET))
        }
        manager.notifyIfAllowed(context, COUNTDOWN_ID, builder.build())
    }

    /** The ringing service's foreground notification. */
    fun ringing(context: Context, total: Long): Notification {
        ensureChannels(context)
        return base(context, RINGING_CHANNEL)
            .setContentTitle("Time's up")
            .setContentText("${formatTimer(total)} timer")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(openTimer(context), true)
            .addAction(action(context, R.drawable.ic_stop, "Stop", TimerReceiver.ACTION_RESET))
            .addAction(action(context, R.drawable.ic_timer, "+1:00", TimerReceiver.ACTION_ADD_MINUTE))
            .build()
    }

    private fun base(context: Context, channel: String) =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_timer)
            .setOnlyAlertOnce(true)
            .setContentIntent(openTimer(context))

    private fun openTimer(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            openTimerIntent(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun action(context: Context, icon: Int, title: String, receiverAction: String) =
        NotificationCompat.Action(
            icon,
            title,
            PendingIntent.getBroadcast(
                context,
                receiverAction.hashCode(),
                Intent(context, TimerReceiver::class.java).setAction(receiverAction),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )

    private fun ensureChannels(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(COUNTDOWN_CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName("Timer")
                .setDescription("The timer counting down")
                .build(),
        )
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(RINGING_CHANNEL, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("Timer alarm")
                .setDescription("When the timer runs out")
                .setSound(null, null)
                .setVibrationEnabled(false)
                .build(),
        )
    }

    private fun allowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun NotificationManagerCompat.notifyIfAllowed(
        context: Context,
        id: Int,
        notification: Notification,
    ) {
        if (allowed(context)) {
            @Suppress("MissingPermission") // Checked by allowed().
            notify(id, notification)
        }
    }
}

package com.reevan.reevzhabitz.timer

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The one timer, shared by the Timer screen, its notification and the alarm. Every change goes
 * through [update], which keeps four things in step with the state:
 * - **Saved** to a small preferences file, so a timer outlives Android killing the app while it
 *   counts down in the background. A reboot clears it (the since-boot clock starts over).
 * - **The alarm**: an exact alarm at the end time wakes the phone even from deep sleep, where a
 *   coroutine delay would stall, and [TimerReceiver] then calls [settle].
 * - **The notification** showing the countdown (or that it is paused).
 * - **The ring**: [TimerRingService] plays the alarm sound and vibrates while [TimerState.Ringing].
 *
 * It also keeps the saved timers (the cards on the Timer screen), in a file of their own so that
 * clearing a finished timer never touches them. They are a list of lengths, not habit data, so
 * they live here rather than in the database.
 *
 * The rules themselves are the pure functions in TimerState.kt.
 */
class TimerController private constructor(private val app: Context) {

    private val prefs = app.getSharedPreferences("timer", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val savedPrefs = app.getSharedPreferences("saved_timers", Context.MODE_PRIVATE)
    private val _saved = MutableStateFlow(decodeTimers(savedPrefs.getString(KEY_SAVED, null)))

    /** The saved timers' lengths, shortest first. */
    val saved: StateFlow<List<Long>> = _saved.asStateFlow()

    fun now(): Long = SystemClock.elapsedRealtime()

    /** Starts the timer [duration] afresh, replacing any other, and saves it if it is new. */
    fun start(duration: Long) {
        if (duration <= 0) return
        setSaved(saved.value.withTimer(duration))
        update(startTimer(duration, now()))
    }

    /** Forgets the saved timer [duration], stopping it first if it is the one going. */
    fun delete(duration: Long) {
        if (state.value.preset == duration) reset()
        setSaved(saved.value - duration)
    }

    fun pause() = update(state.value.paused(now()))

    fun resume() = update(state.value.resumed(now()))

    fun addMinute() = update(state.value.plusMinute(now()))

    /** Reset, or Stop while ringing: back to the keypad. */
    fun reset() = update(TimerState.Idle)

    /** The time may be up: the alarm went off, or the screen saw the countdown reach zero. */
    fun settle() = update(state.value.settled(now()))

    /** Re-posts the countdown notification, e.g. once the user allows notifications. */
    fun refreshNotification() = TimerNotifications.showCountdown(app, state.value, now())

    @Synchronized
    private fun setSaved(timers: List<Long>) {
        _saved.value = timers
        savedPrefs.edit { putString(KEY_SAVED, encodeTimers(timers)) }
    }

    @Synchronized
    private fun update(next: TimerState) {
        val previous = _state.value
        if (next == previous) return
        _state.value = next
        save(next)
        scheduleAlarm(next)
        TimerNotifications.showCountdown(app, next, now())
        when {
            next is TimerState.Ringing && previous !is TimerState.Ringing -> TimerRingService.start(app)
            next !is TimerState.Ringing && previous is TimerState.Ringing -> TimerRingService.stop(app)
        }
    }

    private fun scheduleAlarm(state: TimerState) {
        val alarms = app.getSystemService(AlarmManager::class.java)
        val timeUp = PendingIntent.getBroadcast(
            app,
            0,
            Intent(app, TimerReceiver::class.java).setAction(TimerReceiver.ACTION_TIME_UP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        if (state !is TimerState.Running) {
            alarms.cancel(timeUp)
            return
        }
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarms.canScheduleExactAlarms()
        if (exactAllowed) {
            alarms.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP, state.endsAt, timeUp,
            )
        } else {
            // Without the exact-alarm permission it may ring a little late; the screen still
            // settles it on time while it is open.
            alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, state.endsAt, timeUp)
        }
    }

    private fun save(state: TimerState) = prefs.edit {
        clear()
        putInt(KEY_BOOT, bootCount())
        when (state) {
            TimerState.Idle -> Unit
            is TimerState.Running -> putString(KEY_KIND, "running")
                .putLong(KEY_AT, state.endsAt).putLong(KEY_TOTAL, state.total)
                .putLong(KEY_PRESET, state.preset)
            is TimerState.Paused -> putString(KEY_KIND, "paused")
                .putLong(KEY_AT, state.left).putLong(KEY_TOTAL, state.total)
                .putLong(KEY_PRESET, state.preset)
            is TimerState.Ringing -> putString(KEY_KIND, "ringing")
                .putLong(KEY_AT, state.since).putLong(KEY_TOTAL, state.total)
                .putLong(KEY_PRESET, state.preset)
        }
    }

    private fun load(): TimerState {
        // Since-boot times mean nothing after a reboot, and the reboot cleared the alarm anyway.
        if (prefs.getInt(KEY_BOOT, -1) != bootCount()) return TimerState.Idle
        val at = prefs.getLong(KEY_AT, 0)
        val total = prefs.getLong(KEY_TOTAL, 0)
        val preset = prefs.getLong(KEY_PRESET, total)
        return when (prefs.getString(KEY_KIND, null)) {
            "running" -> TimerState.Running(at, total, preset)
            "paused" -> TimerState.Paused(at, total, preset)
            "ringing" -> TimerState.Ringing(at, total, preset)
            else -> TimerState.Idle
        }
    }

    private fun bootCount(): Int =
        Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT, 0)

    companion object {
        private const val KEY_KIND = "kind"
        private const val KEY_AT = "at"
        private const val KEY_TOTAL = "total"
        private const val KEY_BOOT = "boot"
        private const val KEY_PRESET = "preset"
        private const val KEY_SAVED = "durations"

        // Holds only the application context, which lives exactly as long as the process.
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: TimerController? = null

        fun get(context: Context): TimerController =
            instance ?: synchronized(this) {
                instance ?: TimerController(context.applicationContext).also { instance = it }
            }
    }
}

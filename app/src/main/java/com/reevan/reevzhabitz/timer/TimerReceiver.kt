package com.reevan.reevzhabitz.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** The timer's alarm going off, and the buttons on its notifications. */
class TimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val timer = TimerController.get(context)
        when (intent.action) {
            ACTION_TIME_UP -> timer.settle()
            ACTION_PAUSE -> timer.pause()
            ACTION_RESUME -> timer.resume()
            ACTION_ADD_MINUTE -> timer.addMinute()
            ACTION_RESET -> timer.reset()
        }
    }

    companion object {
        const val ACTION_TIME_UP = "com.reevan.reevzhabitz.timer.TIME_UP"
        const val ACTION_PAUSE = "com.reevan.reevzhabitz.timer.PAUSE"
        const val ACTION_RESUME = "com.reevan.reevzhabitz.timer.RESUME"
        const val ACTION_ADD_MINUTE = "com.reevan.reevzhabitz.timer.ADD_MINUTE"
        const val ACTION_RESET = "com.reevan.reevzhabitz.timer.RESET"
    }
}

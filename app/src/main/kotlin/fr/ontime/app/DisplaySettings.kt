package fr.ontime.app

import android.content.Context

/** What the home screen and the widgets show; shared by the settings screen and the widget toggle. */
data class DisplayOptions(
    val countdown: Boolean = true,
    val leaveTime: Boolean = true,
    val cancelled: Boolean = false,
)

class DisplaySettings(context: Context) {
    private val preferences = context.getSharedPreferences("ontime_display_settings", Context.MODE_PRIVATE)

    fun read() = DisplayOptions(
        countdown = preferences.getBoolean(COUNTDOWN, true),
        leaveTime = preferences.getBoolean(LEAVE_TIME, true),
        cancelled = preferences.getBoolean(CANCELLED, false),
    )

    fun write(options: DisplayOptions) {
        preferences.edit()
            .putBoolean(COUNTDOWN, options.countdown)
            .putBoolean(LEAVE_TIME, options.leaveTime)
            .putBoolean(CANCELLED, options.cancelled)
            .apply()
    }

    private companion object {
        const val COUNTDOWN = "countdown"
        const val LEAVE_TIME = "leave_time"
        const val CANCELLED = "cancelled"
    }
}

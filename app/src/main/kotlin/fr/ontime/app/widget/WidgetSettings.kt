package fr.ontime.app.widget

import android.content.Context

/** What the home-screen widgets show; changed from the app settings screen. */
data class WidgetOptions(
    val countdown: Boolean = true,
    val leaveTime: Boolean = true,
    val cancelled: Boolean = false,
)

class WidgetSettings(context: Context) {
    private val preferences = context.getSharedPreferences("ontime_widget_settings", Context.MODE_PRIVATE)

    fun read() = WidgetOptions(
        countdown = preferences.getBoolean(COUNTDOWN, true),
        leaveTime = preferences.getBoolean(LEAVE_TIME, true),
        cancelled = preferences.getBoolean(CANCELLED, false),
    )

    fun write(options: WidgetOptions) {
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

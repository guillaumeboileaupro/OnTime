package fr.ontime.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import java.time.Instant

/**
 * One inexact alarm that refreshes every OnTime widget soon after the shown
 * leave time, so a passed countdown is replaced by the next train. No exact
 * alarm permission is needed and repeated scheduling keeps a single alarm.
 */
object WidgetRefreshAlarm {
    fun schedule(context: Context, at: Instant) {
        // Several widgets share this alarm: keep the earliest pending refresh.
        val preferences = context.getSharedPreferences("ontime_widget_refresh", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val pendingAt = preferences.getLong("at", 0L)
        if (pendingAt > now && pendingAt <= at.toEpochMilli()) return
        preferences.edit().putLong("at", at.toEpochMilli()).apply()
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, TripWidgetReceiver::class.java))
        if (ids.isEmpty()) return
        val intent = Intent(context, TripWidgetReceiver::class.java)
            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC, maxOf(at.toEpochMilli(), now), pending)
    }
}

package fr.ontime.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import fr.ontime.domain.TravelProfile
import fr.ontime.domain.nextPlanningAt
import fr.ontime.domain.reminderAt
import java.time.Instant
import java.time.ZoneId

val ParisZone: ZoneId = ZoneId.of("Europe/Paris")

/**
 * One alarm slot per trip and kind, so planning twice never duplicates a
 * reminder. Exact alarms are used when Android allows them; otherwise the
 * reminder is approximate and the app says so.
 */
class ReminderScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun canBeExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    fun schedule(tripId: String, action: String, at: Instant) {
        val pending = pendingIntent(tripId, action)
        val time = maxOf(at.toEpochMilli(), System.currentTimeMillis())
        if (canBeExact()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending)
        }
    }

    fun cancel(tripId: String, action: String) {
        alarms.cancel(pendingIntent(tripId, action))
    }

    /** Recomputes the planning alarm of every trip; trips without a window lose theirs. */
    fun planAll(trips: List<TravelProfile>, store: ReminderStore, now: Instant = Instant.now()) {
        trips.forEach { trip ->
            val next = store.window(trip.id)?.nextPlanningAt(now, ParisZone)
            if (next == null) cancel(trip.id, ACTION_PLAN) else schedule(trip.id, ACTION_PLAN, next)
            store.planned(trip.id)?.let { planned ->
                // Restore a pending warning lost by a reboot or a time change.
                schedule(trip.id, ACTION_NOTIFY, trip.reminderAt(planned))
            }
        }
    }

    fun forget(tripId: String, store: ReminderStore) {
        cancel(tripId, ACTION_PLAN)
        cancel(tripId, ACTION_NOTIFY)
        store.forget(tripId)
    }

    private fun pendingIntent(tripId: String, action: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(action).putExtra(EXTRA_TRIP, tripId)
        val code = tripId.hashCode() * 2 + if (action == ACTION_NOTIFY) 1 else 0
        return PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val ACTION_PLAN = "fr.ontime.app.reminders.PLAN"
        const val ACTION_NOTIFY = "fr.ontime.app.reminders.NOTIFY"
        const val EXTRA_TRIP = "trip"
    }
}

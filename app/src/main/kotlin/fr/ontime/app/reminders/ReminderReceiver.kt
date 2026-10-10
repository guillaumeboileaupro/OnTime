package fr.ontime.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import fr.ontime.app.data.SharedPreferencesProfileRepository
import fr.ontime.app.data.TripLabels
import fr.ontime.app.data.sncf.SncfServices
import fr.ontime.app.shortLabel
import fr.ontime.domain.ProfileSnapshot
import fr.ontime.domain.ReminderDecision
import fr.ontime.domain.Status
import fr.ontime.domain.TravelProfile
import fr.ontime.domain.decideReminder
import fr.ontime.domain.nextPlanningAt
import fr.ontime.domain.pickDeparture
import fr.ontime.domain.reminderAt
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Private receiver for reminder alarms (plan, notify). Each alarm makes at most
 * one SNCF request; there is no polling loop.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handle(context.applicationContext, intent)
            } finally {
                pending.finish()
            }
        }
    }

    private fun handle(context: Context, intent: Intent) {
        val store = ReminderStore(context)
        val scheduler = ReminderScheduler(context)
        val trips = (SharedPreferencesProfileRepository(context).snapshot() as? ProfileSnapshot.Data)?.profiles.orEmpty()
        val trip = trips.firstOrNull { it.id == intent.getStringExtra(ReminderScheduler.EXTRA_TRIP) } ?: return
        when (intent.action) {
            ReminderScheduler.ACTION_PLAN -> plan(context, trip, store, scheduler)
            ReminderScheduler.ACTION_NOTIFY -> notify(context, trip, store, scheduler)
        }
    }

    private fun plan(context: Context, trip: TravelProfile, store: ReminderStore, scheduler: ReminderScheduler) {
        val window = store.window(trip.id) ?: return
        val sncf = SncfServices.shared() ?: return
        val update = sncf.tripUpdate(trip, limit = 5)
        val now = Instant.now()
        val chosen = if (update.selection.status == Status.Available) window.pickDeparture(update.upcoming, trip, ParisZone) else null
        if (chosen != null) {
            store.setPlanned(trip.id, chosen)
            scheduler.schedule(trip.id, ReminderScheduler.ACTION_NOTIFY, trip.reminderAt(chosen))
            return
        }
        val closes = now.atZone(ParisZone).toLocalDate().atTime(window.end).atZone(ParisZone).toInstant()
        val retry = now.plus(RETRY_AFTER_ERROR)
        if (update.selection.status == Status.Error && retry.isBefore(closes)) {
            scheduler.schedule(trip.id, ReminderScheduler.ACTION_PLAN, retry)
        } else {
            scheduleNextWindow(trip, store, scheduler, now)
        }
    }

    private fun notify(context: Context, trip: TravelProfile, store: ReminderStore, scheduler: ReminderScheduler) {
        val planned = store.planned(trip.id) ?: return
        if (planned.departureAt.isBefore(Instant.now())) {
            // Missed while the phone was off: the train has left, warning now would mislead.
            store.setPlanned(trip.id, null)
            scheduleNextWindow(trip, store, scheduler, Instant.now())
            return
        }
        val label = TripLabels(context).get(trip.id)?.let(::shortLabel) ?: "Votre trajet"
        val update = SncfServices.shared()?.tripUpdate(trip, limit = 5)
        val fetched = update?.takeIf { it.selection.status == Status.Available || it.selection.status == Status.Empty }
        val now = Instant.now()
        val notifier = DepartureNotifier(context)
        when (val decision = decideReminder(planned, trip, fetched?.departures, fetched?.upcoming.orEmpty(), now)) {
            is ReminderDecision.Postpone -> {
                scheduler.schedule(trip.id, ReminderScheduler.ACTION_NOTIFY, decision.at)
                return
            }
            is ReminderDecision.Notify ->
                notifier.notifyDeparture(trip, label, decision.departure, decision.confirmed, decision.replacement)
            ReminderDecision.NoTrain -> notifier.notifyNoTrain(trip, label)
        }
        store.setPlanned(trip.id, null)
        scheduleNextWindow(trip, store, scheduler, now)
    }

    /** Plans the next occurrence of the regular window after today's one (none for a one-off reminder). */
    private fun scheduleNextWindow(trip: TravelProfile, store: ReminderStore, scheduler: ReminderScheduler, now: Instant) {
        val window = store.window(trip.id) ?: return
        val closes = now.atZone(ParisZone).toLocalDate().atTime(window.end).atZone(ParisZone).toInstant()
        window.nextPlanningAt(maxOf(now, closes), ParisZone)?.let {
            scheduler.schedule(trip.id, ReminderScheduler.ACTION_PLAN, it)
        }
    }

    private companion object {
        val RETRY_AFTER_ERROR: Duration = Duration.ofMinutes(10)
    }
}

/**
 * Exported receiver for system events only (reboot, app update, time or time-zone
 * change, exact-alarm permission): it reschedules alarms and never notifies.
 */
class ReminderRescheduler : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in SYSTEM_EVENTS) return
        val app = context.applicationContext
        val trips = (SharedPreferencesProfileRepository(app).snapshot() as? ProfileSnapshot.Data)?.profiles.orEmpty()
        ReminderScheduler(app).planAll(trips, ReminderStore(app))
    }

    private companion object {
        val SYSTEM_EVENTS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}

package fr.ontime.domain

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** Warn this long before the time to leave home. */
val REMINDER_ADVANCE: Duration = Duration.ofMinutes(5)

/** Fetch departures this long before a reminder window opens. */
val PLANNING_LEAD: Duration = Duration.ofMinutes(30)

/** A delay smaller than this does not postpone an imminent reminder. */
private val RESCHEDULE_THRESHOLD: Duration = Duration.ofMinutes(2)

/** Recurring reminder slot of a trip: leave home on [days] between [start] and [end] (local time). */
data class ReminderWindow(val days: Set<DayOfWeek>, val start: LocalTime, val end: LocalTime) {
    val isValid: Boolean get() = days.isNotEmpty() && start.isBefore(end)
}

fun TravelProfile.leaveAt(departure: Departure): Instant =
    departure.departureAt.minus(Duration.ofMinutes((walkingMinutes + marginMinutes).toLong()))

/**
 * When to fetch departures for the next occurrence of [window]: [lead] before it
 * opens, or [now] if that moment has passed while the window is still open.
 */
fun ReminderWindow.nextPlanningAt(now: Instant, zone: ZoneId, lead: Duration = PLANNING_LEAD): Instant? {
    if (!isValid) return null
    val today = now.atZone(zone).toLocalDate()
    for (offset in 0L..7L) {
        val date = today.plusDays(offset)
        if (date.dayOfWeek !in days) continue
        val opens = date.atTime(start).atZone(zone).toInstant()
        val closes = date.atTime(end).atZone(zone).toInstant()
        if (!now.isBefore(closes)) continue
        val planning = opens.minus(lead)
        return if (planning.isAfter(now)) planning else now
    }
    return null
}

/** First departure of [upcoming] whose leave-home time falls inside today's [window] occurrence. */
fun ReminderWindow.pickDeparture(upcoming: List<Departure>, trip: TravelProfile, zone: ZoneId): Departure? =
    upcoming.sortedBy { it.departureAt }.firstOrNull { departure ->
        val leave = trip.leaveAt(departure).atZone(zone)
        leave.dayOfWeek in days && !leave.toLocalTime().isBefore(start) && !leave.toLocalTime().isAfter(end)
    }

fun TravelProfile.reminderAt(departure: Departure, advance: Duration = REMINDER_ADVANCE): Instant =
    leaveAt(departure).minus(advance)

sealed interface ReminderDecision {
    /** Warn now about [departure]; [confirmed] is false when fresh data could not be fetched. */
    data class Notify(val departure: Departure, val confirmed: Boolean, val replacement: Boolean) : ReminderDecision

    /** The planned train is late: warn later. */
    data class Postpone(val at: Instant) : ReminderDecision

    /** The planned train vanished and no other reachable train exists. */
    data object NoTrain : ReminderDecision
}

/**
 * Decides what a due reminder does, from the freshly fetched [update] (null on
 * failure). A late train postpones the warning; a vanished one is replaced by
 * the next reachable train; without data the planned train is announced as
 * unconfirmed. No departure is ever invented.
 */
fun decideReminder(
    planned: Departure,
    trip: TravelProfile,
    update: List<Departure>?,
    reachable: List<Departure>,
    now: Instant,
    advance: Duration = REMINDER_ADVANCE,
): ReminderDecision {
    if (update == null) return ReminderDecision.Notify(planned, confirmed = false, replacement = false)
    val current = update.firstOrNull { it.journeyId == planned.journeyId && !it.cancelled }
    if (current != null) {
        val due = trip.reminderAt(current, advance)
        return if (due.isAfter(now.plus(RESCHEDULE_THRESHOLD))) {
            ReminderDecision.Postpone(due)
        } else {
            ReminderDecision.Notify(current, confirmed = true, replacement = false)
        }
    }
    val next = reachable.minByOrNull { it.departureAt } ?: return ReminderDecision.NoTrain
    return ReminderDecision.Notify(next, confirmed = true, replacement = true)
}

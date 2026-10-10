package fr.ontime.domain

import java.time.Instant

enum class Mode { Train, Rer, Metro, Tram, Bus }
enum class Quality { Realtime, Scheduled, Estimated }
enum class Status { Available, Empty, Stale, Error }

data class Departure(
    val provider: String,
    val journeyId: String,
    val stopId: String,
    val lineId: String,
    val directionId: String,
    val destination: String,
    val mode: Mode,
    val departureAt: Instant,
    val fetchedAt: Instant,
    val quality: Quality,
    val cancelled: Boolean,
    /** Arrival at the trip destination, when the provider answers for an origin-destination pair. */
    val arrivalAt: Instant? = null,
    /** Timetabled departure, when the provider publishes it next to the real-time one. */
    val scheduledAt: Instant? = null,
    /** Provider explanation of a delay or cancellation (e.g. infrastructure failure). */
    val disruption: String? = null,
) {
    /** Positive delay against the timetable, rounded to whole minutes; null when on time or unknown. */
    val delayMinutes: Long?
        get() = scheduledAt?.let { java.time.Duration.between(it, departureAt).toMinutes() }?.takeIf { it > 0 }
}

/**
 * Cancelled departures worth announcing before [next]: those scheduled between
 * [now] and the recommended train, earliest first.
 */
fun cancelledBefore(departures: List<Departure>, next: Departure?, now: Instant): List<Departure> =
    departures.filter { it.cancelled && !it.departureAt.isBefore(now) && (next == null || it.departureAt.isBefore(next.departureAt)) }
        .sortedBy { it.departureAt }

data class Selection(
    val status: Status,
    val departure: Departure? = null,
)

interface DepartureRepository {
    fun currentSelection(): Selection
}

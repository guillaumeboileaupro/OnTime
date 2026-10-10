package fr.ontime.domain

import java.time.Duration
import java.time.Instant

fun selectNextDeparture(
    departures: List<Departure>,
    now: Instant,
    walking: Duration,
    margin: Duration,
    maxAge: Duration,
    sourceStatus: Status = Status.Available,
): Selection {
    if (sourceStatus == Status.Error || sourceStatus == Status.Stale) {
        return Selection(sourceStatus)
    }
    if (sourceStatus == Status.Empty) return Selection(Status.Empty)
    if (now.isBefore(Instant.EPOCH) || walking.isNegative || margin.isNegative || maxAge.isNegative) {
        return Selection(Status.Error)
    }

    var sawFresh = false
    var selected: Departure? = null
    departures.forEach { candidate ->
        if (!candidate.isValid() || candidate.fetchedAt.isAfter(now)) {
            return Selection(Status.Error)
        }
        val fresh = Duration.between(candidate.fetchedAt, now) <= maxAge
        if (fresh) sawFresh = true
        if (fresh && candidate.isRecommendable(now, walking, margin) &&
            (selected == null || candidate.departureAt.isBefore(selected!!.departureAt))
        ) {
            selected = candidate
        }
    }

    return when {
        selected != null -> Selection(Status.Available, selected)
        departures.isEmpty() || sawFresh -> Selection(Status.Empty)
        else -> Selection(Status.Stale)
    }
}

/**
 * The next [limit] departures still reachable on foot, earliest first, under the
 * same rules as [selectNextDeparture]. Call it only for an `Available` selection.
 */
fun reachableDepartures(
    departures: List<Departure>,
    now: Instant,
    walking: Duration,
    margin: Duration,
    limit: Int,
): List<Departure> = departures
    .filter { it.isValid() && it.isRecommendable(now, walking, margin) }
    .sortedBy { it.departureAt }
    .take(limit)

private fun Departure.isValid(): Boolean =
    provider.isNotBlank() && journeyId.isNotBlank() && stopId.isNotBlank() &&
        lineId.isNotBlank() && directionId.isNotBlank() && destination.isNotBlank() &&
        !departureAt.isBefore(Instant.EPOCH) && !fetchedAt.isBefore(Instant.EPOCH)

private fun Departure.isRecommendable(
    now: Instant,
    walking: Duration,
    margin: Duration,
): Boolean = !cancelled && quality != Quality.Estimated &&
    !departureAt.isBefore(now.plus(walking).plus(margin))

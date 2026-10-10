package fr.ontime.app.data.sncf

import fr.ontime.app.BuildConfig
import fr.ontime.domain.Departure
import fr.ontime.domain.RequestBudget
import fr.ontime.domain.Selection
import fr.ontime.domain.Status
import fr.ontime.domain.TravelProfile
import fr.ontime.domain.reachableDepartures
import fr.ontime.domain.selectNextDeparture
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Result of one refresh for a trip, shared by the home screen, widgets and
 * reminders: [departures] is everything fetched, [upcoming] what is still reachable.
 */
data class TripUpdate(
    val selection: Selection,
    val upcoming: List<Departure>,
    val checkedAt: Instant,
    val departures: List<Departure> = emptyList(),
)

/**
 * SNCF use cases sharing one client, quota budget and departure cache for the
 * whole process, so the app and its widgets never exceed the daily quota.
 */
class SncfServices private constructor(apiKey: String, private val clock: Clock) {
    val api: SncfApi = SncfClient(apiKey)
    val departures = SncfDepartureSource(api, quota, clock)
    val nearest = NearestStationFinder(api, quota, clock)
    val search = StationSearch(api, quota, clock)

    /** Blocking: call off the main thread. */
    fun tripUpdate(trip: TravelProfile, limit: Int = 3): TripUpdate {
        val fetched = departures.fetchTrip(trip.stopId, trip.destinationId)
        return buildTripUpdate(trip, fetched.status, fetched.departures, clock.instant(), limit)
    }

    companion object {
        private val quota = RequestBudget()

        @Volatile
        private var instance: SncfServices? = null

        /** Process-wide services, or null when the build has no SNCF key. */
        fun shared(): SncfServices? {
            val key = BuildConfig.SNCF_API_KEY.takeIf { it.isNotBlank() } ?: return null
            return instance ?: synchronized(this) {
                instance ?: SncfServices(key, Clock.systemUTC()).also { instance = it }
            }
        }
    }
}

private val MAX_DATA_AGE: Duration = Duration.ofMinutes(3)

/** Selection and reachable departures of [trip] from any provider's answer. */
fun buildTripUpdate(trip: TravelProfile, status: Status, departures: List<Departure>, now: Instant, limit: Int = 3): TripUpdate {
    val walking = Duration.ofMinutes(trip.walkingMinutes.toLong())
    val margin = Duration.ofMinutes(trip.marginMinutes.toLong())
    val selection = selectNextDeparture(departures, now, walking, margin, MAX_DATA_AGE, status)
    val upcoming = if (selection.departure != null) reachableDepartures(departures, now, walking, margin, limit) else emptyList()
    return TripUpdate(selection, upcoming, now, departures)
}

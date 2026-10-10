package fr.ontime.app.data.sncf

import fr.ontime.domain.Departure
import fr.ontime.domain.RequestBudget
import fr.ontime.domain.Status
import java.time.Clock

data class SncfSnapshot(val status: Status, val departures: List<Departure> = emptyList())

/**
 * Fetches departures through the shared [budget]. When the budget refuses a
 * call, the last successful answer for that stop is returned unchanged so its
 * `fetchedAt` lets the selector report it as stale; no departure is invented.
 */
class SncfDepartureSource(
    private val api: SncfApi,
    private val budget: RequestBudget,
    private val clock: Clock,
) {
    private val cache = mutableMapOf<String, List<Departure>>()

    /** Next departures at a stop area, all lines and directions. */
    fun fetch(stopAreaId: String): SncfSnapshot = load(stopAreaId) { api, now ->
        val response = api.get(
            "/stop_areas/${encodeSegment(stopAreaId)}/departures?count=10&data_freshness=realtime&disable_geojson=true",
        )
        (response as? SncfResponse.Body)?.let { parseSncfDepartures(it.json, now) }
    }

    /** Next direct trains from [originId] to [destinationId], with arrival times. */
    fun fetchTrip(originId: String, destinationId: String): SncfSnapshot = load("$originId>$destinationId") { api, now ->
        val response = api.get(
            "/journeys?from=${encodeSegment(originId)}&to=${encodeSegment(destinationId)}" +
                "&max_nb_transfers=0&count=5&data_freshness=realtime&disable_geojson=true",
        )
        (response as? SncfResponse.Body)?.let { parseSncfJourneys(it.json, originId, now) }
    }

    @Synchronized
    private fun load(key: String, request: (SncfApi, java.time.Instant) -> List<Departure>?): SncfSnapshot {
        if (!budget.tryAcquire(clock.instant())) {
            return cache[key]?.let(::available) ?: SncfSnapshot(Status.Stale)
        }
        val departures = request(api, clock.instant()) ?: return SncfSnapshot(Status.Error)
        cache[key] = departures
        return available(departures)
    }

    private fun available(departures: List<Departure>) =
        SncfSnapshot(if (departures.isEmpty()) Status.Empty else Status.Available, departures)
}

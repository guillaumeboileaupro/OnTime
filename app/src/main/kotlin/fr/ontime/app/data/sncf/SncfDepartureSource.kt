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

    /**
     * Next direct trains from [originId] to [destinationId], with arrival times,
     * delays and cancellations. The timetable used to spot removed trains is
     * reused for [TIMETABLE_REUSE] to spare the daily quota.
     */
    fun fetchTrip(originId: String, destinationId: String): SncfSnapshot = load("$originId>$destinationId") { api, now ->
        val realtime = journeys(api, originId, destinationId, "realtime", REALTIME_COUNT)
            ?.let { parseSncfTrains(it, originId, now) } ?: return@load null
        val timetable = timetable(api, originId, destinationId, now)
        if (timetable == null) realtime.map { it.departure } else mergeCancellations(realtime, timetable)
    }

    private val timetables = mutableMapOf<String, Pair<java.time.Instant, List<SncfTrain>>>()

    private fun timetable(api: SncfApi, originId: String, destinationId: String, now: java.time.Instant): List<SncfTrain>? {
        val key = "$originId>$destinationId"
        timetables[key]?.takeIf { java.time.Duration.between(it.first, now) < TIMETABLE_REUSE }?.let { return it.second }
        if (!budget.tryAcquire(now)) return timetables[key]?.second
        val trains = journeys(api, originId, destinationId, "base_schedule", TIMETABLE_COUNT)
            ?.let { parseSncfTrains(it, originId, now) } ?: return null
        timetables[key] = now to trains
        return trains
    }

    private fun journeys(api: SncfApi, originId: String, destinationId: String, freshness: String, count: Int): String? =
        (
            api.get(
                "/journeys?from=${encodeSegment(originId)}&to=${encodeSegment(destinationId)}" +
                    "&max_nb_transfers=0&count=$count&data_freshness=$freshness&disable_geojson=true",
            ) as? SncfResponse.Body
            )?.json

    @Synchronized
    private fun load(key: String, request: (SncfApi, java.time.Instant) -> List<Departure>?): SncfSnapshot {
        if (!budget.tryAcquire(clock.instant())) {
            return cache[key]?.let(::available) ?: SncfSnapshot(Status.Stale)
        }
        val departures = request(api, clock.instant()) ?: return SncfSnapshot(Status.Error)
        cache[key] = departures
        return available(departures)
    }

    private companion object {
        const val REALTIME_COUNT = 6
        const val TIMETABLE_COUNT = 10
        val TIMETABLE_REUSE: java.time.Duration = java.time.Duration.ofMinutes(10)
    }

    private fun available(departures: List<Departure>) =
        SncfSnapshot(if (departures.isEmpty()) Status.Empty else Status.Available, departures)
}

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

    @Synchronized
    fun fetch(stopAreaId: String): SncfSnapshot {
        if (!budget.tryAcquire(clock.instant())) {
            return cache[stopAreaId]?.let(::available) ?: SncfSnapshot(Status.Stale)
        }
        val response = api.departures(stopAreaId)
        if (response !is SncfResponse.Body) return SncfSnapshot(Status.Error)
        val departures = parseSncfDepartures(response.json, clock.instant())
            ?: return SncfSnapshot(Status.Error)
        cache[stopAreaId] = departures
        return available(departures)
    }

    private fun available(departures: List<Departure>) =
        SncfSnapshot(if (departures.isEmpty()) Status.Empty else Status.Available, departures)
}

package fr.ontime.app.data.sncf

import fr.ontime.domain.RequestBudget
import java.time.Clock

sealed interface StationSearchResult {
    data class Found(val stations: List<NearbyStation>) : StationSearchResult
    data object Busy : StationSearchResult
    data object Error : StationSearchResult
}

/** Finds SNCF stop areas by name (one request on the shared budget). */
class StationSearch(
    private val api: SncfApi,
    private val budget: RequestBudget,
    private val clock: Clock,
) {
    fun search(name: String): StationSearchResult {
        val query = name.trim()
        if (query.length < 2) return StationSearchResult.Found(emptyList())
        if (!budget.tryAcquire(clock.instant())) return StationSearchResult.Busy
        val response = api.get("/places?q=${encodeSegment(query)}&type%5B%5D=stop_area&count=8&disable_geojson=true")
        val stations = (response as? SncfResponse.Body)?.let { parseStopAreas(it.json, "places") }
            ?: return StationSearchResult.Error
        return StationSearchResult.Found(stations)
    }
}

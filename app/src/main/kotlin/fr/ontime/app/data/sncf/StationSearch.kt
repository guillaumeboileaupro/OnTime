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

    /** Every terminus served from [stopAreaId], all lines included, sorted by name. */
    fun directionsFrom(stopAreaId: String): StationSearchResult {
        if (!budget.tryAcquire(clock.instant())) return StationSearchResult.Busy
        val response = api.get("/stop_areas/${encodeSegment(stopAreaId)}/routes?count=200&disable_geojson=true")
        val directions = (response as? SncfResponse.Body)?.let { parseRouteDirections(it.json, stopAreaId) }
            ?: return StationSearchResult.Error
        return StationSearchResult.Found(directions)
    }
}

internal fun parseRouteDirections(body: String, originId: String): List<NearbyStation>? = runCatching {
    val routes = org.json.JSONObject(body).optJSONArray("routes") ?: return@runCatching emptyList()
    (0 until routes.length()).map { routes.getJSONObject(it).getJSONObject("direction") }
        .filter { it.optString("embedded_type") == "stop_area" && it.getString("id") != originId }
        .map { NearbyStation(it.getString("id"), it.getString("name")) }
        .distinctBy { it.stopAreaId }
        .sortedBy { it.name }
}.getOrNull()

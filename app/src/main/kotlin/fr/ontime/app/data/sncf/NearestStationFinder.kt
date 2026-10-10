package fr.ontime.app.data.sncf

import fr.ontime.domain.RequestBudget
import fr.ontime.domain.walkingMinutesFrom
import java.time.Clock
import java.util.Locale
import org.json.JSONObject

data class NearbyStation(val stopAreaId: String, val name: String)

sealed interface NearestStationResult {
    data class Found(val station: NearbyStation, val walkingMinutes: Int, val walkingMeters: Int) : NearestStationResult
    data object NoStation : NearestStationResult
    data object TooFar : NearestStationResult
    data object Busy : NearestStationResult
    data object Error : NearestStationResult
}

/**
 * Finds the closest SNCF stop area to a position and its walking time along
 * streets. Costs two requests, reserved together on the shared budget. The
 * position is only sent to the SNCF API, never stored.
 */
class NearestStationFinder(
    private val api: SncfApi,
    private val budget: RequestBudget,
    private val clock: Clock,
    private val searchRadiusMeters: Int = 3_000,
) {
    fun find(latitude: Double, longitude: Double): NearestStationResult {
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return NearestStationResult.Error
        if (!budget.tryAcquire(clock.instant(), requests = 2)) return NearestStationResult.Busy
        val coord = String.format(Locale.ROOT, "%.5f;%.5f", longitude, latitude)

        val nearby = api.get(
            "/coord/$coord/places_nearby?type%5B%5D=stop_area&count=1&distance=$searchRadiusMeters&disable_geojson=true",
        )
        val stations = (nearby as? SncfResponse.Body)?.let { parseStopAreas(it.json, "places_nearby") }
            ?: return NearestStationResult.Error
        val found = stations.firstOrNull() ?: return NearestStationResult.NoStation

        val walk = api.get(
            "/journeys?from=$coord&to=${encodeSegment(found.stopAreaId)}" +
                "&direct_path=only&direct_path_mode%5B%5D=walking&disable_geojson=true",
        )
        val (seconds, meters) = (walk as? SncfResponse.Body)?.let { parseWalking(it.json) }
            ?: return NearestStationResult.Error
        val minutes = walkingMinutesFrom(seconds) ?: return NearestStationResult.TooFar
        return NearestStationResult.Found(found, minutes, meters)
    }
}

/** Stop areas listed under [arrayKey], in API order; null if the body is malformed. */
internal fun parseStopAreas(body: String, arrayKey: String): List<NearbyStation>? = runCatching {
    val places = JSONObject(body).optJSONArray(arrayKey) ?: return@runCatching emptyList()
    (0 until places.length()).map { places.getJSONObject(it) }
        .filter { it.optString("embedded_type") == "stop_area" }
        .map { NearbyStation(it.getString("id"), it.getString("name")) }
}.getOrNull()

/** Walking duration (s) and distance (m) of the direct walking journey, null if absent or malformed. */
internal fun parseWalking(body: String): Pair<Int, Int>? = runCatching {
    val journeys = JSONObject(body).getJSONArray("journeys")
    var best: Pair<Int, Int>? = null
    for (index in 0 until journeys.length()) {
        val journey = journeys.getJSONObject(index)
        val sections = journey.getJSONArray("sections")
        val walkingOnly = (0 until sections.length()).all {
            val section = sections.getJSONObject(it)
            section.getString("type") == "street_network" && section.getString("mode") == "walking"
        }
        if (!walkingOnly || sections.length() == 0) continue
        val candidate = journey.getInt("duration") to journey.getJSONObject("distances").getInt("walking")
        if (best == null || candidate.first < best.first) best = candidate
    }
    best
}.getOrNull()

package fr.ontime.app.data

import fr.ontime.app.data.lda.LignesAzurSource
import fr.ontime.app.data.sncf.NearbyStation
import fr.ontime.app.data.sncf.NearestStationResult
import fr.ontime.app.data.sncf.SncfServices
import fr.ontime.app.data.sncf.StationSearchResult
import fr.ontime.app.data.sncf.walkingBetween
import fr.ontime.domain.walkingMinutesFrom
import kotlin.math.roundToInt

/** Stop lookup of one network, as used by the trip form. Blocking: call off the main thread. */
interface NetworkStops {
    fun search(query: String): StationSearchResult
    fun directionsFrom(stopId: String): StationSearchResult
    fun nearest(latitude: Double, longitude: Double): NearestStationResult
}

class SncfStops(private val sncf: SncfServices) : NetworkStops {
    override fun search(query: String) = sncf.search.search(query)
    override fun directionsFrom(stopId: String) = sncf.search.directionsFrom(stopId)
    override fun nearest(latitude: Double, longitude: Double) = sncf.nearest.find(latitude, longitude)
}

/**
 * Lignes d'Azur stops are searched on the phone (no request). Walking time to
 * the nearest stop uses the SNCF street router when a key exists, otherwise a
 * labelled estimate (straight line x 1.3 at 75 m/min).
 */
class AzurNetworkStops(private val source: LignesAzurSource, private val sncf: SncfServices?) : NetworkStops {
    override fun search(query: String): StationSearchResult {
        val stops = source.stops() ?: return StationSearchResult.Error
        return StationSearchResult.Found(stops.search(query).map { NearbyStation(it.id, it.name) })
    }

    override fun directionsFrom(stopId: String): StationSearchResult {
        val directions = source.directionsFrom(stopId) ?: return StationSearchResult.Error
        return StationSearchResult.Found(directions.map { NearbyStation(it.id, it.name) })
    }

    override fun nearest(latitude: Double, longitude: Double): NearestStationResult {
        val stops = source.stops() ?: return NearestStationResult.Error
        val (place, meters) = stops.nearest(latitude, longitude) ?: return NearestStationResult.NoStation
        val routed = sncf?.nearest?.walkingBetween(latitude, longitude, place.latitude, place.longitude)
        val seconds = routed?.first ?: (meters * DETOUR / WALK_METERS_PER_SECOND).roundToInt()
        val walkedMeters = routed?.second ?: (meters * DETOUR).roundToInt()
        val minutes = walkingMinutesFrom(seconds) ?: return NearestStationResult.TooFar
        return NearestStationResult.Found(NearbyStation(place.id, place.name), minutes, walkedMeters)
    }

    private companion object {
        const val DETOUR = 1.3
        const val WALK_METERS_PER_SECOND = 1.25
    }
}

package fr.ontime.app.data.lda

import fr.ontime.domain.Departure
import fr.ontime.domain.Mode
import fr.ontime.domain.Quality
import fr.ontime.domain.Status
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.zip.ZipInputStream

/** Departures computed for one trip, with the provider status. */
data class AzurSnapshot(val status: Status, val departures: List<Departure> = emptyList())

/**
 * Lignes d'Azur buses and trams from open data, without key: the stop catalogue
 * comes from the static GTFS (refreshed weekly, only `stops.txt` is kept) and
 * departures from the GTFS-Realtime TripUpdates feed (about 5 hours ahead).
 */
class LignesAzurSource(
    private val cacheDir: File,
    private val clock: Clock,
    private val download: (String) -> ByteArray? = ::httpGet,
) {
    private var stops: AzurStops? = null
    private var feed: Pair<Instant, List<RtTrip>>? = null

    @Synchronized
    fun stops(): AzurStops? {
        stops?.let { return it }
        val file = File(cacheDir, STOPS_FILE)
        val stale = !file.exists() || Duration.between(Instant.ofEpochMilli(file.lastModified()), clock.instant()) > STOPS_MAX_AGE
        if (stale) download(GTFS_URL)?.let(::extractStops)?.let { file.writeText(it) }
        stops = file.takeIf { it.exists() }?.readText()?.let(AzurStops::parse)
        return stops
    }

    /** Every terminus reached by a vehicle currently scheduled through [placeId]. */
    fun directionsFrom(placeId: String): List<AzurPlace>? {
        val catalogue = stops() ?: return null
        val trips = trips() ?: return null
        return trips.filter { trip -> trip.stops.any { catalogue.placeOf(it.stopId) == placeId } }
            .mapNotNull { trip -> trip.stops.maxByOrNull { it.sequence }?.let { catalogue.placeOf(it.stopId) } }
            .filter { it != placeId }
            .distinct()
            .mapNotNull { catalogue.places[it] }
            .sortedBy { it.name }
    }

    /** Next vehicles leaving [originId] that later stop at [destinationId]. */
    fun fetchTrip(originId: String, destinationId: String): AzurSnapshot {
        val catalogue = stops() ?: return AzurSnapshot(Status.Error)
        val (fetchedAt, trips) = feedOrNull() ?: return AzurSnapshot(Status.Error)
        val departures = trips.mapNotNull { trip -> departureOf(trip, originId, destinationId, catalogue, fetchedAt) }
            .distinctBy { it.journeyId }
            .sortedBy { it.departureAt }
        return AzurSnapshot(if (departures.isEmpty()) Status.Empty else Status.Available, departures)
    }

    private fun trips(): List<RtTrip>? = feedOrNull()?.second

    @Synchronized
    private fun feedOrNull(): Pair<Instant, List<RtTrip>>? {
        val now = clock.instant()
        feed?.takeIf { Duration.between(it.first, now) < FEED_REUSE }?.let { return it }
        val parsed = download(TRIP_UPDATES_URL)?.let(::parseTripUpdates) ?: return null
        return (now to parsed).also { feed = it }
    }

    private companion object {
        const val GTFS_URL = "https://www.data.gouv.fr/api/1/datasets/r/f5678ab2-c863-4b48-ba1f-9021c7d97634"
        const val TRIP_UPDATES_URL = "https://ara-api.enroute.mobi/rla/gtfs/trip-updates"
        const val STOPS_FILE = "lda_stops.txt"
        val STOPS_MAX_AGE: Duration = Duration.ofDays(7)
        val FEED_REUSE: Duration = Duration.ofSeconds(30)
    }
}

internal fun departureOf(
    trip: RtTrip,
    originId: String,
    destinationId: String,
    stops: AzurStops,
    fetchedAt: Instant,
): Departure? {
    val ordered = trip.stops.filter { !it.skipped }.sortedBy { it.sequence }
    val from = ordered.indexOfFirst { stops.placeOf(it.stopId) == originId }
    if (from < 0) return null
    val to = ordered.drop(from + 1).firstOrNull { stops.placeOf(it.stopId) == destinationId } ?: return null
    val leaving = ordered[from].departure ?: ordered[from].arrival ?: return null
    val arriving = to.arrival ?: to.departure
    val terminus = trip.stops.maxByOrNull { it.sequence }?.let { stops.placeOf(it.stopId) } ?: destinationId
    return Departure(
        provider = "lignes-azur",
        journeyId = trip.tripId.ifBlank { return null },
        stopId = originId,
        lineId = trip.routeId.ifBlank { return null },
        directionId = terminus,
        destination = stops.places[terminus]?.name ?: "Destination",
        mode = if (trip.routeId.matches(Regex("L\\d+"))) Mode.Tram else Mode.Bus,
        departureAt = Instant.ofEpochSecond(leaving),
        fetchedAt = fetchedAt,
        quality = Quality.Realtime,
        cancelled = trip.cancelled,
        arrivalAt = arriving?.let(Instant::ofEpochSecond),
    )
}

/** Keeps only `stops.txt` from the GTFS archive. */
internal fun extractStops(zip: ByteArray): String? = runCatching {
    ZipInputStream(zip.inputStream()).use { input ->
        generateSequence { input.nextEntry }.firstOrNull { it.name == "stops.txt" } ?: return@runCatching null
        input.readBytes().toString(Charsets.UTF_8)
    }
}.getOrNull()

private fun httpGet(url: String): ByteArray? {
    val connection = URL(url).openConnection() as HttpURLConnection
    return try {
        connection.connectTimeout = 10_000
        connection.readTimeout = 30_000
        if (connection.responseCode == HttpURLConnection.HTTP_OK) connection.inputStream.use { it.readBytes() } else null
    } catch (_: IOException) {
        null
    } finally {
        connection.disconnect()
    }
}

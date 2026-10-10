package fr.ontime.app.data.sncf

import fr.ontime.domain.Departure
import fr.ontime.domain.Mode
import fr.ontime.domain.Quality
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.json.JSONArray
import org.json.JSONObject

const val SNCF_PROVIDER = "sncf"

private val NAVITIA_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss")

/**
 * Maps a Navitia `/departures` body to domain departures. Returns null when the
 * body or a supported departure is malformed; unsupported modes (bus, coach...)
 * are skipped. Local times are resolved with `context.timezone`.
 */
fun parseSncfDepartures(body: String, fetchedAt: Instant): List<Departure>? = runCatching {
    val root = JSONObject(body)
    val zone = ZoneId.of(root.getJSONObject("context").getString("timezone"))
    val items = root.getJSONArray("departures")
    buildList {
        for (index in 0 until items.length()) {
            val item = items.getJSONObject(index)
            val links = item.getJSONArray("links")
            val mode = links.linkId("physical_mode")?.let(::toMode) ?: continue
            val stopTime = item.getJSONObject("stop_date_time")
            val route = item.getJSONObject("route")
            add(
                Departure(
                    provider = SNCF_PROVIDER,
                    journeyId = requireNotNull(links.linkId("vehicle_journey")),
                    stopId = item.getJSONObject("stop_point").getJSONObject("stop_area").getString("id"),
                    lineId = route.getJSONObject("line").getString("id"),
                    directionId = route.getString("id"),
                    destination = item.getJSONObject("display_informations").getString("direction"),
                    mode = mode,
                    departureAt = localInstant(stopTime.getString("departure_date_time"), zone),
                    fetchedAt = fetchedAt,
                    quality = if (stopTime.optString("data_freshness") == "realtime") {
                        Quality.Realtime
                    } else {
                        Quality.Scheduled
                    },
                    cancelled = false,
                ),
            )
        }
    }.distinctBy { it.journeyId } // a train listed twice is shown once
}.getOrNull()

/**
 * Maps a Navitia `/journeys` body restricted to direct trains to departures from
 * the origin, with their arrival at the destination. Journeys that are not a
 * single train section, or use an unsupported mode, are skipped.
 */
fun parseSncfJourneys(body: String, originStopAreaId: String, fetchedAt: Instant): List<Departure>? = runCatching {
    val root = JSONObject(body)
    val zone = ZoneId.of(root.getJSONObject("context").getString("timezone"))
    val journeys = root.optJSONArray("journeys") ?: return@runCatching emptyList()
    buildList {
        for (index in 0 until journeys.length()) {
            val sections = journeys.getJSONObject(index).getJSONArray("sections")
            val trains = (0 until sections.length()).map { sections.getJSONObject(it) }
                .filter { it.getString("type") == "public_transport" }
            val train = trains.singleOrNull() ?: continue
            val links = train.getJSONArray("links")
            val mode = links.linkId("physical_mode")?.let(::toMode) ?: continue
            add(
                Departure(
                    provider = SNCF_PROVIDER,
                    journeyId = requireNotNull(links.linkId("vehicle_journey")),
                    stopId = originStopAreaId,
                    lineId = requireNotNull(links.linkId("line")),
                    directionId = requireNotNull(links.linkId("route")),
                    destination = train.getJSONObject("display_informations").getString("direction"),
                    mode = mode,
                    departureAt = localInstant(train.getString("departure_date_time"), zone),
                    fetchedAt = fetchedAt,
                    quality = if (train.optString("data_freshness") == "realtime") Quality.Realtime else Quality.Scheduled,
                    cancelled = false,
                    arrivalAt = localInstant(train.getString("arrival_date_time"), zone),
                ),
            )
        }
    }.distinctBy { it.journeyId } // a train listed twice is shown once
}.getOrNull()

private fun localInstant(value: String, zone: ZoneId): Instant =
    LocalDateTime.parse(value, NAVITIA_DATE_TIME).atZone(zone).toInstant()

private fun JSONArray.linkId(type: String): String? {
    for (index in 0 until length()) {
        val link = getJSONObject(index)
        if (link.optString("type") == type) return link.getString("id")
    }
    return null
}

private fun toMode(physicalModeId: String): Mode? = when (physicalModeId.removePrefix("physical_mode:")) {
    "LocalTrain", "LongDistanceTrain", "Train", "RailShuttle" -> Mode.Train
    "RapidTransit" -> Mode.Rer
    "Metro" -> Mode.Metro
    "Tramway" -> Mode.Tram
    else -> null
}

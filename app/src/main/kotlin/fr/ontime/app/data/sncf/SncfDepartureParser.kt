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

/** A direct train of a `/journeys` answer with its public train number (used to spot cancellations). */
data class SncfTrain(val number: String, val departure: Departure)

/**
 * Maps a Navitia `/journeys` body restricted to direct trains to departures from
 * the origin, with their arrival at the destination, timetabled departure and
 * disruption message. Journeys that are not a single train section, or use an
 * unsupported mode, are skipped.
 */
fun parseSncfJourneys(body: String, originStopAreaId: String, fetchedAt: Instant): List<Departure>? =
    parseSncfTrains(body, originStopAreaId, fetchedAt)?.map { it.departure }

fun parseSncfTrains(body: String, originStopAreaId: String, fetchedAt: Instant): List<SncfTrain>? = runCatching {
    val root = JSONObject(body)
    val zone = ZoneId.of(root.getJSONObject("context").getString("timezone"))
    val journeys = root.optJSONArray("journeys") ?: return@runCatching emptyList()
    val messages = disruptionMessages(root)
    buildList {
        for (index in 0 until journeys.length()) {
            val sections = journeys.getJSONObject(index).getJSONArray("sections")
            val trains = (0 until sections.length()).map { sections.getJSONObject(it) }
                .filter { it.getString("type") == "public_transport" }
            val train = trains.singleOrNull() ?: continue
            val links = train.getJSONArray("links")
            val mode = links.linkId("physical_mode")?.let(::toMode) ?: continue
            val display = train.getJSONObject("display_informations")
            val departure = Departure(
                provider = SNCF_PROVIDER,
                journeyId = requireNotNull(links.linkId("vehicle_journey")),
                stopId = originStopAreaId,
                lineId = requireNotNull(links.linkId("line")),
                directionId = requireNotNull(links.linkId("route")),
                destination = display.getString("direction"),
                mode = mode,
                departureAt = localInstant(train.getString("departure_date_time"), zone),
                fetchedAt = fetchedAt,
                quality = if (train.optString("data_freshness") == "realtime") Quality.Realtime else Quality.Scheduled,
                cancelled = false,
                arrivalAt = localInstant(train.getString("arrival_date_time"), zone),
                scheduledAt = train.optString("base_departure_date_time").takeIf { it.isNotBlank() }
                    ?.let { localInstant(it, zone) },
                disruption = links.ids("disruption").firstNotNullOfOrNull { messages[it] },
            )
            add(SncfTrain(display.optString("headsign"), departure))
        }
    }.distinctBy { it.departure.journeyId } // a train listed twice is shown once
}.getOrNull()

/**
 * Real-time answer completed with cancellations: a timetabled train missing
 * from the real-time answer, inside the period it covers, has been removed by
 * the SNCF and is returned as cancelled at its timetabled time.
 */
fun mergeCancellations(realtime: List<SncfTrain>, timetable: List<SncfTrain>): List<Departure> {
    val lastCovered = realtime.maxOfOrNull { it.departure.scheduledAt ?: it.departure.departureAt }
        ?: return realtime.map { it.departure }
    val running = realtime.map { it.number }.toSet()
    val removed = timetable
        .filter { it.number.isNotBlank() && it.number !in running && !it.departure.departureAt.isAfter(lastCovered) }
        .map { it.departure.copy(cancelled = true, scheduledAt = it.departure.departureAt) }
    return (realtime.map { it.departure } + removed).sortedBy { it.departureAt }
}

private fun disruptionMessages(root: JSONObject): Map<String, String> {
    val disruptions = root.optJSONArray("disruptions") ?: return emptyMap()
    return (0 until disruptions.length()).map { disruptions.getJSONObject(it) }.mapNotNull { disruption ->
        val text = disruption.optJSONArray("messages")?.optJSONObject(0)?.optString("text")?.takeIf { it.isNotBlank() }
        text?.let { disruption.optString("id") to it }
    }.toMap()
}

private fun localInstant(value: String, zone: ZoneId): Instant =
    LocalDateTime.parse(value, NAVITIA_DATE_TIME).atZone(zone).toInstant()

private fun JSONArray.ids(type: String): List<String> =
    (0 until length()).map { getJSONObject(it) }.filter { it.optString("type") == type }.map { it.getString("id") }

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

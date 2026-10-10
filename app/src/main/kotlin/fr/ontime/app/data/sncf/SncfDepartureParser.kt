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
                    departureAt = LocalDateTime.parse(stopTime.getString("departure_date_time"), NAVITIA_DATE_TIME)
                        .atZone(zone).toInstant(),
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
    }
}.getOrNull()

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

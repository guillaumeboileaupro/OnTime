package fr.ontime.app.data.lda

import java.text.Normalizer
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** A Lignes d'Azur stop place (all its platforms, both directions). Ids are prefixed `lda:`. */
data class AzurPlace(val id: String, val name: String, val latitude: Double, val longitude: Double)

/**
 * Stop catalogue built from the GTFS `stops.txt`: platforms (location_type 0)
 * are grouped under their parent place, so a trip "A -> B" works whatever the
 * platform or direction.
 */
class AzurStops private constructor(
    val places: Map<String, AzurPlace>,
    private val placeOfPlatform: Map<String, String>,
) {
    fun placeOf(platformId: String): String? = placeOfPlatform[platformId]

    fun search(query: String, limit: Int = 8): List<AzurPlace> {
        val wanted = normalize(query)
        if (wanted.length < 2) return emptyList()
        return places.values
            .filter { normalize(it.name).contains(wanted) }
            .sortedWith(compareBy({ !normalize(it.name).startsWith(wanted) }, { it.name }))
            .take(limit)
    }

    fun nearest(latitude: Double, longitude: Double, maxMeters: Double = 1_500.0): Pair<AzurPlace, Double>? =
        places.values
            .map { it to distanceMeters(latitude, longitude, it.latitude, it.longitude) }
            .filter { it.second <= maxMeters }
            .minByOrNull { it.second }

    companion object {
        const val PREFIX = "lda:"

        /** Parses `stops.txt`; null when the header lacks the needed columns. */
        fun parse(csv: String): AzurStops? = runCatching {
            val lines = csv.removePrefix("﻿").lineSequence().filter { it.isNotBlank() }.toList()
            val header = splitCsv(lines.first())
            fun column(name: String) = header.indexOf(name).also { require(it >= 0) }
            val id = column("stop_id")
            val name = column("stop_name")
            val lat = column("stop_lat")
            val lon = column("stop_lon")
            val type = column("location_type")
            val parent = column("parent_station")
            val places = mutableMapOf<String, AzurPlace>()
            val platformToPlace = mutableMapOf<String, String>()
            lines.drop(1).map(::splitCsv).forEach { row ->
                val placeId = when (row.getOrNull(type)) {
                    "1" -> row[id]
                    else -> row.getOrNull(parent)?.takeIf { it.isNotBlank() } ?: row[id]
                }
                if (row.getOrNull(type) != "1") platformToPlace[row[id]] = PREFIX + placeId
                if (row.getOrNull(type) == "1" || PREFIX + placeId !in places) {
                    places[PREFIX + placeId] = AzurPlace(PREFIX + placeId, row[name], row[lat].toDouble(), row[lon].toDouble())
                }
            }
            AzurStops(places, platformToPlace)
        }.getOrNull()

        private fun splitCsv(line: String): List<String> {
            val cells = mutableListOf<String>()
            val cell = StringBuilder()
            var quoted = false
            var index = 0
            while (index < line.length) {
                val char = line[index]
                when {
                    char == '"' && quoted && line.getOrNull(index + 1) == '"' -> { cell.append('"'); index++ }
                    char == '"' -> quoted = !quoted
                    char == ',' && !quoted -> { cells += cell.toString(); cell.clear() }
                    else -> cell.append(char)
                }
                index++
            }
            cells += cell.toString()
            return cells
        }

        internal fun normalize(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()
                .replace(Regex("[^a-z0-9]+"), " ").trim()

        internal fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
            return 2 * 6_371_000.0 * asin(sqrt(a))
        }
    }
}

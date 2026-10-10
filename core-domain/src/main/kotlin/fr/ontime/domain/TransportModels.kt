package fr.ontime.domain

import java.time.Instant

enum class Mode { Train, Rer, Metro, Tram }
enum class Quality { Realtime, Scheduled, Estimated }
enum class Status { Available, Empty, Stale, Error }

data class Departure(
    val provider: String,
    val journeyId: String,
    val stopId: String,
    val lineId: String,
    val directionId: String,
    val destination: String,
    val mode: Mode,
    val departureAt: Instant,
    val fetchedAt: Instant,
    val quality: Quality,
    val cancelled: Boolean,
)

data class Selection(
    val status: Status,
    val departure: Departure? = null,
)

interface DepartureRepository {
    fun currentSelection(): Selection
}

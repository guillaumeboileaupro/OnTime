package fr.ontime.app.data

import fr.ontime.domain.Departure
import fr.ontime.domain.DepartureRepository
import fr.ontime.domain.Mode
import fr.ontime.domain.Quality
import fr.ontime.domain.Selection
import fr.ontime.domain.Status
import fr.ontime.domain.selectNextDeparture
import java.time.Clock
import java.time.Duration

enum class DemoScenario(val label: String) {
    Available("Available"),
    Empty("Empty"),
    Stale("Stale"),
    Error("Error"),
}

class FixtureDepartureRepository(
    private val clock: Clock,
    private val scenario: DemoScenario,
) : DepartureRepository {
    override fun currentSelection(): Selection {
        val now = clock.instant()
        return when (scenario) {
            DemoScenario.Available -> selectNextDeparture(
                departures = listOf(
                    Departure(
                        provider = "demo",
                        journeyId = "demo-course-0842",
                        stopId = "demo:stop:central",
                        lineId = "demo:line:a",
                        directionId = "demo:direction:outbound",
                        destination = "Destination de démonstration",
                        mode = Mode.Rer,
                        departureAt = now.plusSeconds(12 * 60),
                        fetchedAt = now.minusSeconds(30),
                        quality = Quality.Scheduled,
                        cancelled = false,
                    ),
                ),
                now = now,
                walking = Duration.ofMinutes(7),
                margin = Duration.ofMinutes(2),
                maxAge = Duration.ofMinutes(2),
            )
            DemoScenario.Empty -> Selection(Status.Empty)
            DemoScenario.Stale -> Selection(Status.Stale)
            DemoScenario.Error -> Selection(Status.Error)
        }
    }
}

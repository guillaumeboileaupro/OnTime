package fr.ontime.app.data

import fr.ontime.domain.Status
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FixtureDepartureRepositoryTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-09T06:30:00Z"), ZoneOffset.UTC)

    @Test
    fun `fixture scenarios expose the four domain states without inventing departures`() {
        DemoScenario.entries.forEach { scenario ->
            val selection = FixtureDepartureRepository(clock, scenario).currentSelection()
            assertEquals(Status.valueOf(scenario.name), selection.status)
            if (scenario == DemoScenario.Available) {
                assertNotNull(selection.departure)
            } else {
                assertNull(selection.departure)
            }
        }
    }
}

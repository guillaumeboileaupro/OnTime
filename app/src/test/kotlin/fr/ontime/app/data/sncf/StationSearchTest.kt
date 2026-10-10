package fr.ontime.app.data.sncf

import fr.ontime.domain.RequestBudget
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StationSearchTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-10T09:55:00Z"), ZoneOffset.UTC)

    @Test
    fun `lists only stop areas and encodes the query`() {
        var path = ""
        val body = """{"places":[
            {"id":"stop_area:DEMO:SA:1","name":"Gare fictive","embedded_type":"stop_area"},
            {"id":"admin:fr:00000","name":"Ville fictive","embedded_type":"administrative_region"}]}"""
        val result = StationSearch({ path = it; SncfResponse.Body(body) }, RequestBudget(), clock).search(" Gare fictive ")

        assertEquals(StationSearchResult.Found(listOf(NearbyStation("stop_area:DEMO:SA:1", "Gare fictive"))), result)
        assertTrue(path.startsWith("/places?q=Gare+fictive&"))
    }

    @Test
    fun `skips short queries and reports busy or errors`() {
        assertEquals(StationSearchResult.Found(emptyList()), StationSearch({ error("no call") }, RequestBudget(), clock).search("N"))
        val budget = RequestBudget(burst = 1)
        budget.tryAcquire(clock.instant())
        assertEquals(StationSearchResult.Busy, StationSearch({ error("no call") }, budget, clock).search("Nice"))
        assertEquals(StationSearchResult.Error, StationSearch({ SncfResponse.Failed }, RequestBudget(), clock).search("Nice"))
        assertEquals(StationSearchResult.Error, StationSearch({ SncfResponse.Body("oops") }, RequestBudget(), clock).search("Nice"))
    }
}

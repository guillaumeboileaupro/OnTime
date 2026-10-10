package fr.ontime.app.data.sncf

import fr.ontime.domain.RequestBudget
import fr.ontime.domain.Status
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class SncfDepartureSourceTest {
    private val fixture = requireNotNull(javaClass.getResource("/sncf/departures.json")).readText()
    private val start = Instant.parse("2026-10-10T09:55:00Z")

    private class MutableClock(var now: Instant) : Clock() {
        override fun instant() = now
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId) = this
    }

    @Test
    fun `serves the cached answer while the budget refuses a new call`() {
        var calls = 0
        val clock = MutableClock(start)
        val source = SncfDepartureSource({ calls++; SncfResponse.Body(fixture) }, RequestBudget(), clock)

        val first = source.fetch("stop_area:DEMO:SA:1")
        clock.now = start.plusSeconds(5)
        val second = source.fetch("stop_area:DEMO:SA:1")

        assertEquals(1, calls)
        assertEquals(Status.Available, second.status)
        assertEquals(first.departures, second.departures)
        assertEquals(start, second.departures.first().fetchedAt)
    }

    @Test
    fun `reports stale without cache and error on provider failures`() {
        val clock = MutableClock(start)
        val budget = RequestBudget()
        budget.tryAcquire(start)
        assertEquals(Status.Stale, SncfDepartureSource({ error("no call") }, budget, clock).fetch("a").status)

        listOf(SncfResponse.Unauthorized, SncfResponse.QuotaExceeded, SncfResponse.Failed, SncfResponse.Body("{}"))
            .forEach { response ->
                val source = SncfDepartureSource({ response }, RequestBudget(), clock)
                assertEquals(Status.Error, source.fetch("a").status, response.toString())
            }
    }

    @Test
    fun `reports empty when the provider publishes no departure`() {
        val body = """{"context":{"timezone":"Europe/Paris"},"departures":[]}"""
        val source = SncfDepartureSource({ SncfResponse.Body(body) }, RequestBudget(), MutableClock(start))
        assertEquals(Status.Empty, source.fetch("a").status)
    }
}

package fr.ontime.domain

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RequestBudgetTest {
    private val start = Instant.parse("2026-10-10T06:00:00Z")

    @Test
    fun `grants first request then waits the full interval`() {
        val budget = RequestBudget()
        assertTrue(budget.tryAcquire(start))
        assertFalse(budget.tryAcquire(start.plusSeconds(19)))
        assertTrue(budget.tryAcquire(start.plusSeconds(20)))
    }

    @Test
    fun `denied attempts do not delay the next grant`() {
        val budget = RequestBudget()
        assertTrue(budget.tryAcquire(start))
        repeat(10) { assertFalse(budget.tryAcquire(start.plusSeconds(it.toLong()))) }
        assertTrue(budget.tryAcquire(start.plusSeconds(20)))
    }

    @Test
    fun `stays under the SNCF daily quota when polled continuously`() {
        val budget = RequestBudget()
        var granted = 0
        var now = start
        val end = start.plus(Duration.ofDays(1))
        while (now.isBefore(end)) {
            if (budget.tryAcquire(now)) granted++
            now = now.plusSeconds(1)
        }
        assertEquals(4320, granted)
    }

    @Test
    fun `recovers when the clock moves backwards`() {
        val budget = RequestBudget()
        assertTrue(budget.tryAcquire(start))
        assertTrue(budget.tryAcquire(start.minusSeconds(3600)))
    }

    @Test
    fun `rejects a non positive interval`() {
        assertFailsWith<IllegalArgumentException> { RequestBudget(Duration.ZERO) }
    }
}

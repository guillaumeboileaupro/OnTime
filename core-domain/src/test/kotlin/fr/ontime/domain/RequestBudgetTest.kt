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
    fun `allows a short burst then one request per interval`() {
        val budget = RequestBudget(Duration.ofSeconds(20), burst = 4)
        repeat(4) { assertTrue(budget.tryAcquire(start)) }
        assertFalse(budget.tryAcquire(start.plusSeconds(19)))
        assertTrue(budget.tryAcquire(start.plusSeconds(20)))
        assertFalse(budget.tryAcquire(start.plusSeconds(21)))
    }

    @Test
    fun `stays under the SNCF daily quota when polled continuously`() {
        val budget = RequestBudget(Duration.ofSeconds(20), burst = 4)
        var granted = 0
        var now = start
        val end = start.plus(Duration.ofDays(1))
        while (now.isBefore(end)) {
            if (budget.tryAcquire(now)) granted++
            now = now.plusSeconds(1)
        }
        assertEquals(4323, granted) // 4 burst + 4319 earned in 86399 s
        assertTrue(granted < 5000)
    }

    @Test
    fun `multi request lookups consume several tokens at once`() {
        val budget = RequestBudget(Duration.ofSeconds(20), burst = 4)
        assertTrue(budget.tryAcquire(start, requests = 2))
        assertTrue(budget.tryAcquire(start, requests = 2))
        assertFalse(budget.tryAcquire(start.plusSeconds(20), requests = 2))
        assertTrue(budget.tryAcquire(start.plusSeconds(40), requests = 2))
        assertFailsWith<IllegalArgumentException> { budget.tryAcquire(start, requests = 0) }
        assertFailsWith<IllegalArgumentException> { budget.tryAcquire(start, requests = 5) }
    }

    @Test
    fun `a clock moving backwards earns no extra request`() {
        val budget = RequestBudget(Duration.ofSeconds(20), burst = 4)
        repeat(4) { assertTrue(budget.tryAcquire(start)) }
        assertFalse(budget.tryAcquire(start.minusSeconds(3600)))
        assertTrue(budget.tryAcquire(start.plusSeconds(20)))
    }

    @Test
    fun `default settings allow a full search flow and stay under the daily quota`() {
        val budget = RequestBudget()
        repeat(20) { assertTrue(budget.tryAcquire(start)) }
        assertFalse(budget.tryAcquire(start))
        var granted = 20
        var now = start
        val end = start.plus(Duration.ofDays(1))
        while (now.isBefore(end)) {
            now = now.plusSeconds(1)
            if (budget.tryAcquire(now)) granted++
        }
        assertTrue(granted <= 4820, "granted $granted")
        assertTrue(granted < 5000)
    }

    @Test
    fun `rejects invalid settings`() {
        assertFailsWith<IllegalArgumentException> { RequestBudget(Duration.ZERO) }
        assertFailsWith<IllegalArgumentException> { RequestBudget(burst = 0) }
    }
}

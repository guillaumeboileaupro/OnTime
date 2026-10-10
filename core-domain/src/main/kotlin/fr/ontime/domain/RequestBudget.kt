package fr.ontime.domain

import java.time.Duration
import java.time.Instant

/**
 * Shared provider quota guard (token bucket). Up to [burst] requests may run
 * back to back, then one more is earned every [refillInterval]. Over any 24 h
 * the 20 s / 4 defaults allow at most 4320 + 4 calls, under the 5000 daily
 * requests granted by the SNCF API.
 */
class RequestBudget(
    private val refillInterval: Duration = Duration.ofSeconds(20),
    private val burst: Int = 4,
) {
    private var tokens = burst.toDouble()
    private var updatedAt: Instant? = null

    init {
        require(!refillInterval.isNegative && !refillInterval.isZero)
        require(burst > 0)
    }

    @Synchronized
    fun tryAcquire(now: Instant, requests: Int = 1): Boolean {
        require(requests in 1..burst)
        val last = updatedAt
        // A clock moving backwards earns nothing until it passes the last update again.
        if (last == null || now.isAfter(last)) {
            if (last != null) {
                val earned = Duration.between(last, now).toMillis().toDouble() / refillInterval.toMillis()
                tokens = minOf(burst.toDouble(), tokens + earned)
            }
            updatedAt = now
        }
        if (tokens + EPSILON < requests) return false
        tokens -= requests
        return true
    }

    private companion object {
        const val EPSILON = 1e-9
    }
}

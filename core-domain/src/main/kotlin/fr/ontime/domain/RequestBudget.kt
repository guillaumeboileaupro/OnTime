package fr.ontime.domain

import java.time.Duration
import java.time.Instant

/**
 * Shared provider quota guard. Each request reserves [minInterval], which
 * bounds any 24-hour window: the 20 s default allows at most 4320 calls,
 * under the 5000 daily requests granted by the SNCF API.
 */
class RequestBudget(private val minInterval: Duration = Duration.ofSeconds(20)) {
    private var reservedAt: Instant? = null
    private var nextAllowedAt: Instant? = null

    init {
        require(!minInterval.isNegative && !minInterval.isZero)
    }

    @Synchronized
    fun tryAcquire(now: Instant, requests: Int = 1): Boolean {
        require(requests > 0)
        val start = reservedAt
        val next = nextAllowedAt
        // A clock moved backwards before the reservation no longer blocks.
        if (start != null && next != null && !now.isBefore(start) && now.isBefore(next)) return false
        reservedAt = now
        nextAllowedAt = now.plus(minInterval.multipliedBy(requests.toLong()))
        return true
    }
}

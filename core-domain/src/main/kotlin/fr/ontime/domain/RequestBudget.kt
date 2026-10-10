package fr.ontime.domain

import java.time.Duration
import java.time.Instant

/**
 * Shared provider quota guard. One request per [minInterval] bounds any
 * 24-hour window: the 20 s default allows at most 4320 calls, under the
 * 5000 daily requests granted by the SNCF API.
 */
class RequestBudget(private val minInterval: Duration = Duration.ofSeconds(20)) {
    private var lastGrantedAt: Instant? = null

    init {
        require(!minInterval.isNegative && !minInterval.isZero)
    }

    @Synchronized
    fun tryAcquire(now: Instant): Boolean {
        val last = lastGrantedAt
        if (last != null && !now.isBefore(last) && now.isBefore(last.plus(minInterval))) {
            return false
        }
        lastGrantedAt = now
        return true
    }
}

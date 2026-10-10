package fr.ontime.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WalkingTimeTest {
    @Test
    fun `rounds up so the traveller never leaves too late`() {
        assertEquals(0, walkingMinutesFrom(0))
        assertEquals(1, walkingMinutesFrom(1))
        assertEquals(21, walkingMinutesFrom(1224))
        assertEquals(MAX_WALKING_MINUTES, walkingMinutesFrom(MAX_WALKING_MINUTES * 60))
    }

    @Test
    fun `rejects negative or out of profile range durations`() {
        assertNull(walkingMinutesFrom(-1))
        assertNull(walkingMinutesFrom(MAX_WALKING_MINUTES * 60 + 1))
    }
}

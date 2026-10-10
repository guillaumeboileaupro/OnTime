package fr.ontime.app

import kotlin.test.Test
import kotlin.test.assertEquals

class NamesTest {
    @Test
    fun `drops only the trailing commune in parentheses`() {
        assertEquals("Gare A", shortName("Gare A (Commune A)"))
        assertEquals("Ville - Gare (Nord)", shortName("Ville - Gare (Nord) (Commune)"))
        assertEquals("Gare B", shortName("Gare B"))
        assertEquals("(Commune)", shortName("(Commune)"))
        assertEquals("Gare A → Ville B", shortLabel("Gare A (Commune) → Ville B (Commune B)"))
    }

    @Test
    fun `formats delays for people`() {
        assertEquals("10 min", formatDelay(10))
        assertEquals("1 h 00", formatDelay(60))
        assertEquals("3 h 40", formatDelay(220))
    }
}

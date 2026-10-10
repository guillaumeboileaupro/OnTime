package fr.ontime.app.data

import fr.ontime.app.data.sncf.SncfResponse
import fr.ontime.domain.ProfileChange
import fr.ontime.domain.ProfileCommandResult
import fr.ontime.domain.ProfileDraft
import fr.ontime.domain.ProfileRepository
import fr.ontime.domain.ProfileSnapshot
import fr.ontime.domain.TravelProfile
import fr.ontime.domain.toProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LegacyTripMigrationTest {
    private class MemoryLegacy(var raw: String?) : LegacyTripStore {
        override fun read() = raw
        override fun clear() {
            raw = null
        }
    }

    private class MemoryLabels : TripLabelStore {
        val values = mutableMapOf<String, String>()
        override fun get(profileId: String) = values[profileId]
        override fun put(profileId: String, label: String) {
            values[profileId] = label
        }
    }

    private class MemoryRepository : ProfileRepository {
        val profiles = mutableListOf<TravelProfile>()
        override fun snapshot() = ProfileSnapshot.Data(profiles.toList(), null)
        override fun create(draft: ProfileDraft): ProfileChange =
            draft.toProfile("new-${profiles.size + 1}").also { if (it is ProfileChange.Success) profiles += it.profile }
        override fun update(id: String, draft: ProfileDraft) = ProfileChange.NotFound
        override fun delete(id: String) = ProfileCommandResult.NotFound
        override fun select(id: String) = ProfileCommandResult.NotFound
    }

    private val legacyJson = """[
        {"id":"old-demo","stopId":"demo:stop:central","lineId":"demo:line:a","direction":"demo:direction:outbound","walkingMinutes":7,"marginMinutes":2},
        {"id":"old-real","stopId":"stop_area:SNCF:1","lineId":"line:SNCF:X","direction":"route:SNCF:R1","walkingMinutes":12,"marginMinutes":3}]"""
    private val route = """{"routes":[{"id":"route:SNCF:R1","direction":{"id":"stop_area:SNCF:9","name":"Terminus (Commune)"}}]}"""

    @Test
    fun `converts real trips to their route terminus and drops demo ones`() {
        val legacy = MemoryLegacy(legacyJson)
        val repository = MemoryRepository()
        val labels = MemoryLabels().apply { put("old-real", "Gare A → Terminus (Commune)") }
        var path = ""

        LegacyTripMigration(legacy, repository, labels) { path = it; SncfResponse.Body(route) }.run()

        val trip = repository.profiles.single()
        assertEquals(TravelProfile("new-1", "stop_area:SNCF:1", "stop_area:SNCF:9", 12, 3), trip)
        assertEquals("Gare A → Terminus (Commune)", labels.get("new-1"))
        assertEquals("/routes/route%3ASNCF%3AR1?disable_geojson=true", path)
        assertNull(legacy.raw)
    }

    @Test
    fun `keeps the old storage for a later retry when the API is unavailable`() {
        val legacy = MemoryLegacy(legacyJson)
        val repository = MemoryRepository()

        LegacyTripMigration(legacy, repository, MemoryLabels()) { SncfResponse.Failed }.run()

        assertEquals(emptyList(), repository.profiles)
        assertNotNull(legacy.raw)
    }

    @Test
    fun `clears unreadable legacy storage`() {
        val legacy = MemoryLegacy("not-json")
        LegacyTripMigration(legacy, MemoryRepository(), MemoryLabels()) { error("no call") }.run()
        assertNull(legacy.raw)
    }
}

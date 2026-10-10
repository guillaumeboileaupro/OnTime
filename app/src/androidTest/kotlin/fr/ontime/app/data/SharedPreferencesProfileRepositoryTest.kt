package fr.ontime.app.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.ontime.domain.ProfileChange
import fr.ontime.domain.ProfileDraft
import fr.ontime.domain.ProfileCommandResult
import fr.ontime.domain.ProfileSnapshot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesProfileRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    @After
    fun clearStorage() {
        context.getSharedPreferences(SharedPreferencesProfileRepository.FILE_NAME, 0)
            .edit().clear().commit()
    }

    @Test
    fun createsUpdatesSelectsDeletesAndSurvivesRepositoryRestart() {
        val ids = listOf("profile-1", "profile-2").iterator()
        val firstRepository = SharedPreferencesProfileRepository(context) { ids.next() }
        assertTrue(firstRepository.create(draft("A")) is ProfileChange.Success)
        assertTrue(firstRepository.create(draft("B")) is ProfileChange.Success)
        assertEquals(ProfileCommandResult.Success, firstRepository.select("profile-2"))

        val restarted = SharedPreferencesProfileRepository(context)
        val restartedSnapshot = restarted.snapshot() as ProfileSnapshot.Data
        assertEquals(listOf("profile-1", "profile-2"), restartedSnapshot.profiles.map { it.id })
        assertEquals("profile-2", restartedSnapshot.selectedProfileId)
        assertTrue(restarted.update("profile-2", draft("C")) is ProfileChange.Success)

        val restartedAgain = SharedPreferencesProfileRepository(context)
        assertEquals("C", (restartedAgain.snapshot() as ProfileSnapshot.Data).profiles.last().lineId)
        assertEquals(ProfileCommandResult.Success, restartedAgain.delete("profile-2"))
        assertEquals("profile-1", (restartedAgain.snapshot() as ProfileSnapshot.Data).selectedProfileId)
        assertEquals(ProfileCommandResult.NotFound, restartedAgain.select("missing"))
    }

    @Test
    fun malformedStorageIsReportedAndNeverOverwrittenAsEmpty() {
        val preferences = context.getSharedPreferences(SharedPreferencesProfileRepository.FILE_NAME, 0)
        assertTrue(preferences.edit().putString("profiles", "not-json").commit())
        val repository = SharedPreferencesProfileRepository(context) { "profile-1" }

        assertEquals(ProfileSnapshot.StorageError, repository.snapshot())
        assertEquals(ProfileChange.StorageError, repository.create(draft("A")))
        assertEquals("not-json", preferences.getString("profiles", null))
    }

    @Test
    fun malformedSelectedIdTypeIsReportedWithoutCrashing() {
        val repository = SharedPreferencesProfileRepository(context) { "profile-1" }
        assertTrue(repository.create(draft("A")) is ProfileChange.Success)
        val preferences = context.getSharedPreferences(SharedPreferencesProfileRepository.FILE_NAME, 0)
        assertTrue(preferences.edit().putInt(SharedPreferencesProfileRepository.KEY_SELECTED, 42).commit())

        assertEquals(ProfileSnapshot.StorageError, repository.snapshot())
        assertEquals(ProfileChange.StorageError, repository.create(draft("B")))
    }

    @Test
    fun validJsonWithInvalidDomainValuesIsReportedAndPreserved() {
        val invalid = """[{"id":"bad","stopId":"demo:stop","lineId":"A","direction":"out","walkingMinutes":-20,"marginMinutes":2}]"""
        val preferences = context.getSharedPreferences(SharedPreferencesProfileRepository.FILE_NAME, 0)
        assertTrue(preferences.edit().putString("profiles", invalid).commit())
        val repository = SharedPreferencesProfileRepository(context) { "profile-1" }

        assertEquals(ProfileSnapshot.StorageError, repository.snapshot())
        assertEquals(ProfileChange.StorageError, repository.create(draft("B")))
        assertEquals(invalid, preferences.getString("profiles", null))
    }

    private fun draft(line: String) = ProfileDraft(
        stopId = "demo:stop",
        lineId = line,
        direction = "Direction fictive",
        walkingMinutes = 7,
        marginMinutes = 2,
    )
}

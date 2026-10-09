package fr.ontime.app.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.ontime.domain.ProfileChange
import fr.ontime.domain.ProfileDraft
import fr.ontime.domain.ProfileCommandResult
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
        assertEquals(listOf("profile-1", "profile-2"), restarted.profiles().map { it.id })
        assertEquals("profile-2", restarted.selectedProfileId())
        assertTrue(restarted.update("profile-2", draft("C")) is ProfileChange.Success)

        val restartedAgain = SharedPreferencesProfileRepository(context)
        assertEquals("C", restartedAgain.profiles().last().lineId)
        assertEquals(ProfileCommandResult.Success, restartedAgain.delete("profile-2"))
        assertEquals("profile-1", restartedAgain.selectedProfileId())
        assertEquals(ProfileCommandResult.NotFound, restartedAgain.select("missing"))
    }

    private fun draft(line: String) = ProfileDraft(
        stopId = "demo:stop",
        lineId = line,
        direction = "Direction fictive",
        walkingMinutes = 7,
        marginMinutes = 2,
    )
}

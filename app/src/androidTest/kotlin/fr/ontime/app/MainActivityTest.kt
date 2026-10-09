package fr.ontime.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.ontime.app.data.SharedPreferencesProfileRepository
import fr.ontime.domain.ProfileDraft
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    @After
    fun clearProfiles() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences(SharedPreferencesProfileRepository.FILE_NAME, 0)
            .edit().clear().commit()
    }

    @Test
    fun launchesAndDisplaysEveryFixtureState() {
        composeRule.onNodeWithText("DÉMONSTRATION — DONNÉES FICTIVES").assertIsDisplayed()
        composeRule.onNodeWithText("PARTIR DANS").assertIsDisplayed()

        assertScenario("Empty", "AUCUN DÉPART")
        assertScenario("Stale", "DONNÉES PÉRIMÉES")
        assertScenario("Error", "ERREUR DE DONNÉES")
        assertScenario("Available", "PARTIR DANS")
    }

    @Test
    fun onlyCalculatesHomeDepartureForMatchingAvailableSelection() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        SharedPreferencesProfileRepository(context) { "profile-ui" }.create(
            ProfileDraft("demo:stop:central", "demo:line:a", "Direction fictive", 7, 2),
        )
        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText("Départ de chez soi : 08:33 (fixture)")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Empty").performScrollTo().performClick()
        composeRule.onNodeWithText("Aucun départ fixture compatible à calculer.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun rejectsFixtureDepartureAlreadyMissedWithProfileDurations() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        SharedPreferencesProfileRepository(context) { "profile-missed" }.create(
            ProfileDraft("demo:stop:central", "demo:line:a", "Direction fictive", 20, 2),
        )
        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText("Aucun départ fixture compatible à calculer.")
            .performScrollTo().assertIsDisplayed()
    }

    private fun assertScenario(button: String, expectedState: String) {
        composeRule.onNodeWithText(button).performClick()
        composeRule.onNodeWithText(expectedState).assertIsDisplayed()
    }
}

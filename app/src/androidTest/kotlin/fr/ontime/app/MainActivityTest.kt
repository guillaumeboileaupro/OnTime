package fr.ontime.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesAndDisplaysEveryFixtureState() {
        composeRule.onNodeWithText("DÉMONSTRATION — DONNÉES FICTIVES").assertIsDisplayed()
        composeRule.onNodeWithText("PARTIR DANS").assertIsDisplayed()

        assertScenario("Empty", "AUCUN DÉPART")
        assertScenario("Stale", "DONNÉES PÉRIMÉES")
        assertScenario("Error", "ERREUR DE DONNÉES")
        assertScenario("Available", "PARTIR DANS")
    }

    private fun assertScenario(button: String, expectedState: String) {
        composeRule.onNodeWithText(button).performClick()
        composeRule.onNodeWithText(expectedState).assertIsDisplayed()
    }
}

package fr.ontime.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.ontime.app.data.SharedPreferencesProfileRepository
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
    fun launchesOnHomeAndNavigatesWithTheMenu() {
        composeRule.onNode(hasText("Prochain départ") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Ouvrir le menu").performClick()
        composeRule.onNodeWithText("Mes trajets").performClick()
        composeRule.onNode(hasText("Mes trajets") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Ouvrir le menu").performClick()
        composeRule.onNodeWithText("À propos").performClick()
        composeRule.onNodeWithText("Votre position n'est jamais enregistrée.").assertIsDisplayed()
    }
}

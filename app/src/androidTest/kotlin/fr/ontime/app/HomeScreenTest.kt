package fr.ontime.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.ontime.domain.Departure
import fr.ontime.domain.Mode
import fr.ontime.domain.Quality
import fr.ontime.domain.Selection
import fr.ontime.domain.Status
import fr.ontime.domain.TravelProfile
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val clock = Clock.fixed(Instant.parse("2026-10-09T06:30:00Z"), ZoneOffset.UTC)
    private val trip = TravelProfile("trip", "stop_area:TEST:1", "stop_area:TEST:2", 7, 2)
    private val departure = Departure(
        provider = "test",
        journeyId = "journey:TEST:1",
        stopId = trip.stopId,
        lineId = "line:TEST:1",
        directionId = "route:TEST:1",
        destination = "Ville d'essai",
        mode = Mode.Train,
        departureAt = Instant.parse("2026-10-09T06:42:00Z"),
        fetchedAt = clock.instant(),
        quality = Quality.Realtime,
        cancelled = false,
        arrivalAt = Instant.parse("2026-10-09T07:09:00Z"),
    )

    @Test
    fun showsWhenToLeaveInPlainFrench() {
        composeRule.setContent {
            OnTimeTheme {
                HomeScreen(
                    HomeState.Ready(Selection(Status.Available, departure), clock.instant()),
                    trip, "Gare d'essai → Ville d'essai", clock, {}, {},
                )
            }
        }
        composeRule.onNodeWithText("Gare d'essai → Ville d'essai").assertIsDisplayed()
        composeRule.onNodeWithText("3 min").assertIsDisplayed()
        composeRule.onNodeWithText("Quittez la maison à 08:33").assertIsDisplayed()
        composeRule.onNodeWithText("Train de 08:42, arrivée 09:09").assertIsDisplayed()
        composeRule.onNodeWithText("Direction Ville d'essai").assertIsDisplayed()
    }

    @Test
    fun explainsEveryOtherStateWithoutTechnicalWords() {
        var state by mutableStateOf<HomeState>(HomeState.NoTrip)
        composeRule.setContent { OnTimeTheme { HomeScreen(state, trip.takeIf { state is HomeState.Ready }, null, clock, {}, {}) } }
        listOf(
            HomeState.NoTrip to "Aucun trajet",
            HomeState.NoKey to "Horaires indisponibles",
            HomeState.Ready(Selection(Status.Empty), clock.instant()) to "Aucun train à prendre",
            HomeState.Ready(Selection(Status.Stale), clock.instant()) to "Horaires pas à jour",
            HomeState.Ready(Selection(Status.Error), clock.instant()) to "Horaires indisponibles",
        ).forEach { (next, title) ->
            state = next
            composeRule.waitForIdle()
            composeRule.onNodeWithText(title).assertIsDisplayed()
        }
    }
}

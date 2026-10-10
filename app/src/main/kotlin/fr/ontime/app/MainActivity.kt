package fr.ontime.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.ontime.app.data.SharedPreferencesProfileRepository
import fr.ontime.app.data.TripLabels
import fr.ontime.app.data.sncf.NearestStationFinder
import fr.ontime.app.data.sncf.SncfClient
import fr.ontime.app.data.sncf.SncfDepartureSource
import fr.ontime.app.data.sncf.StationSearch
import fr.ontime.domain.ProfileSnapshot
import fr.ontime.domain.RequestBudget
import fr.ontime.domain.selectNextDeparture
import java.time.Clock
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One budget for every SNCF call made by this process. */
private val SncfQuota = RequestBudget()
private val RefreshEvery: Duration = Duration.ofSeconds(60)
private val MaxDataAge: Duration = Duration.ofMinutes(3)

/** SNCF use cases sharing one client, clock and quota budget. */
class SncfServices(apiKey: String, clock: Clock) {
    private val client = SncfClient(apiKey)
    val departures = SncfDepartureSource(client, SncfQuota, clock)
    val nearest = NearestStationFinder(client, SncfQuota, clock)
    val search = StationSearch(client, SncfQuota, clock)
}

private enum class Screen(val title: String) {
    Home("Prochain départ"),
    Trips("Mes trajets"),
    About("À propos"),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OnTimeTheme { OnTimeApp() } }
    }
}

@Composable
fun OnTimeApp(clock: Clock = Clock.systemUTC()) {
    val applicationContext = LocalContext.current.applicationContext
    val profileRepository = remember { SharedPreferencesProfileRepository(applicationContext) }
    val labels = remember { TripLabels(applicationContext) }
    val sncf = remember { BuildConfig.SNCF_API_KEY.takeIf { it.isNotBlank() }?.let { SncfServices(it, clock) } }
    var screen by remember { mutableStateOf(Screen.Home) }
    var revision by remember { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }
    val snapshot = remember(revision) { profileRepository.snapshot() }
    val trip = (snapshot as? ProfileSnapshot.Data)?.let { data ->
        data.profiles.firstOrNull { it.id == data.selectedProfileId }
    }
    var homeState by remember { mutableStateOf<HomeState>(HomeState.Loading) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(trip, screen, refresh) {
        when {
            sncf == null -> homeState = HomeState.NoKey
            trip == null -> homeState = HomeState.NoTrip
            screen != Screen.Home -> Unit
            else -> {
                if (homeState !is HomeState.Ready) homeState = HomeState.Loading
                while (true) {
                    val fetched = withContext(Dispatchers.IO) { sncf.departures.fetch(trip.stopId) }
                    val now = clock.instant()
                    homeState = HomeState.Ready(
                        selectNextDeparture(
                            departures = fetched.departures.filter {
                                it.lineId == trip.lineId && it.directionId == trip.direction
                            },
                            now = now,
                            walking = Duration.ofMinutes(trip.walkingMinutes.toLong()),
                            margin = Duration.ofMinutes(trip.marginMinutes.toLong()),
                            maxAge = MaxDataAge,
                            sourceStatus = fetched.status,
                        ),
                        checkedAt = now,
                    )
                    delay(RefreshEvery.toMillis())
                }
            }
        }
    }

    fun open(target: Screen) {
        screen = target
        scope.launch { drawerState.close() }
    }

    BackHandler(enabled = drawerState.isOpen || screen != Screen.Home) {
        if (drawerState.isOpen) scope.launch { drawerState.close() } else screen = Screen.Home
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Paper) {
                Image(
                    painter = painterResource(R.drawable.ontime_logo),
                    contentDescription = "OnTime",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(96.dp).padding(vertical = 12.dp),
                )
                HorizontalDivider(color = Ink)
                Screen.entries.forEach { target ->
                    NavigationDrawerItem(
                        label = { Text(target.title, style = MaterialTheme.typography.labelLarge) },
                        selected = screen == target,
                        onClick = { open(target) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                }
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(Paper).windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Filled.Menu, contentDescription = "Ouvrir le menu", tint = Ink)
                }
                Text(
                    screen.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
            }
            HorizontalDivider(color = Ink)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                when (screen) {
                    Screen.Home -> HomeScreen(
                        state = homeState,
                        trip = trip,
                        tripLabel = trip?.let { labels.get(it.id) },
                        clock = clock,
                        onOpenTrips = { screen = Screen.Trips },
                        onRefresh = { refresh += 1 },
                    )
                    Screen.Trips -> TripsScreen(profileRepository, labels, snapshot, { revision += 1 }, sncf)
                    Screen.About -> AboutScreen(sncf != null)
                }
            }
        }
    }
}

@Composable
private fun AboutScreen(hasKey: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("OnTime ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleLarge)
        Text(
            if (hasKey) "Horaires, gares et temps de marche : API SNCF." else "Clé SNCF absente de cette version.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "Les horaires sont actualisés chaque minute sur l'accueil, dans la limite du quota SNCF.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text("Votre position n'est jamais enregistrée.", style = MaterialTheme.typography.bodyLarge)
    }
}

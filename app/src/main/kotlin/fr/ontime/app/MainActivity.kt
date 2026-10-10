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
import fr.ontime.app.data.Transport
import fr.ontime.app.data.SncfStops
import fr.ontime.app.data.AzurNetworkStops
import fr.ontime.app.data.sncf.SncfServices
import fr.ontime.app.widget.TripWidget
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import fr.ontime.app.reminders.ReminderScheduler
import fr.ontime.app.reminders.ReminderStore
import fr.ontime.domain.Departure
import fr.ontime.domain.cancelledBefore
import fr.ontime.domain.reminderAt
import androidx.glance.appwidget.updateAll
import fr.ontime.app.data.LegacyTripMigration
import fr.ontime.app.data.SharedPreferencesLegacyTripStore
import fr.ontime.domain.ProfileSnapshot
import java.time.Clock
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val RefreshEvery: Duration = Duration.ofSeconds(60)

private enum class Screen(val title: String) {
    Home("Prochain départ"),
    Trips("Mes trajets"),
    Settings("Paramètres"),
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
    val sncf = remember { SncfServices.shared() }
    var screen by remember { mutableStateOf(Screen.Home) }
    var revision by remember { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }
    val snapshot = remember(revision) { profileRepository.snapshot() }
    val trip = (snapshot as? ProfileSnapshot.Data)?.let { data ->
        data.profiles.firstOrNull { it.id == data.selectedProfileId }
    }
    var homeState by remember { mutableStateOf<HomeState>(HomeState.Loading) }
    val reminders = remember { ReminderStore(applicationContext) }
    val scheduler = remember { ReminderScheduler(applicationContext) }
    var reminderNote by remember { mutableStateOf<String?>(null) }
    var pendingReminder by remember { mutableStateOf<Departure?>(null) }

    fun remind(departure: Departure) {
        val current = trip ?: return
        reminders.setPlanned(current.id, departure)
        scheduler.schedule(current.id, ReminderScheduler.ACTION_NOTIFY, current.reminderAt(departure))
        reminderNote = "Rappel prévu à ${TimeFormat.format(current.reminderAt(departure))}" +
            if (scheduler.canBeExact()) "." else " (approximatif : alarmes exactes non autorisées)."
    }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val departure = pendingReminder
        pendingReminder = null
        if (granted && departure != null) remind(departure) else reminderNote = "Notifications refusées : aucun rappel possible."
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (sncf != null) {
            withContext(Dispatchers.IO) {
                LegacyTripMigration(
                    SharedPreferencesLegacyTripStore(applicationContext),
                    profileRepository,
                    labels,
                    sncf.api,
                ).run()
            }
            revision += 1
        }
        val trips = (profileRepository.snapshot() as? ProfileSnapshot.Data)?.profiles.orEmpty()
        scheduler.planAll(trips, reminders)
    }

    LaunchedEffect(revision) {
        // Trips may have been edited or deleted: widgets show them by id.
        if (revision > 0) TripWidget().updateAll(applicationContext)
    }

    LaunchedEffect(trip, screen, refresh) {
        when {
            trip == null -> homeState = HomeState.NoTrip
            screen != Screen.Home -> Unit
            else -> {
                if (homeState !is HomeState.Ready) homeState = HomeState.Loading
                while (true) {
                    val update = withContext(Dispatchers.IO) { Transport.tripUpdate(applicationContext, trip) }
                    homeState = update?.let {
                        HomeState.Ready(it.selection, it.checkedAt, cancelledBefore(it.departures, it.selection.departure, it.checkedAt))
                    } ?: HomeState.NoKey
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
                    modifier = Modifier.fillMaxWidth().height(160.dp).padding(vertical = 16.dp),
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
                        tripLabel = trip?.let { labels.get(it.id) }?.let(::shortLabel),
                        clock = clock,
                        onOpenTrips = { screen = Screen.Trips },
                        onRefresh = { refresh += 1 },
                        reminderNote = reminderNote,
                        options = remember(screen, refresh) { DisplaySettings(applicationContext).read() },
                        onRemind = { departure ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                pendingReminder = departure
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                remind(departure)
                            }
                        },
                    )
                    Screen.Trips -> TripsScreen(
                        profileRepository,
                        labels,
                        snapshot,
                        { revision += 1 },
                        trainStops = sncf?.let(::SncfStops),
                        busStops = remember { AzurNetworkStops(Transport.lignesAzur(applicationContext), sncf) },
                    )
                    Screen.Settings -> SettingsScreen()
                    Screen.About -> AboutScreen()
                }
            }
        }
    }
}

@Composable
private fun AboutScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("OnTime ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleLarge)
        Text("Les horaires sont actualisés chaque minute.", style = MaterialTheme.typography.bodyLarge)
        Text("Votre position n'est jamais enregistrée.", style = MaterialTheme.typography.bodyLarge)
        Text("© 2026 Guillaume Boileau. Tous droits réservés.", style = MaterialTheme.typography.bodyMedium)
    }
}

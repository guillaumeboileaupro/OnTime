package fr.ontime.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.ontime.app.data.DemoScenario
import fr.ontime.app.data.FixtureDepartureRepository
import fr.ontime.app.data.SharedPreferencesProfileRepository
import fr.ontime.domain.Departure
import fr.ontime.domain.DepartureRepository
import fr.ontime.domain.Selection
import fr.ontime.domain.Status
import fr.ontime.app.data.sncf.NearestStationFinder
import fr.ontime.app.data.sncf.SncfClient
import fr.ontime.domain.RequestBudget
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val Paper = Color(0xFFDFDCD3)
private val Ink = Color(0xFF2A2926)
/** One budget for every SNCF call made by this process. */
private val SncfQuota = RequestBudget()

private val DemoClock: Clock = Clock.fixed(
    Instant.parse("2026-10-09T06:30:00Z"),
    ZoneId.of("Europe/Paris"),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OnTimeApp() }
    }
}

@Composable
fun OnTimeApp() {
    val applicationContext = LocalContext.current.applicationContext
    val profileRepository = remember { SharedPreferencesProfileRepository(applicationContext) }
    val colors = lightColorScheme(
        primary = Ink,
        onPrimary = Paper,
        secondary = Ink,
        onSecondary = Paper,
        background = Paper,
        onBackground = Ink,
        surface = Paper,
        onSurface = Ink,
        error = Ink,
        onError = Paper,
        outline = Ink,
    )
    MaterialTheme(colorScheme = colors) {
        DemoDeparturesScreen(profileRepository)
    }
}

@Composable
private fun DemoDeparturesScreen(profileRepository: fr.ontime.domain.ProfileRepository) {
    var scenario by remember { mutableStateOf(DemoScenario.Available) }
    val repository: DepartureRepository = remember(scenario) {
        FixtureDepartureRepository(DemoClock, scenario)
    }
    val selection = repository.currentSelection()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.ontime_logo),
            contentDescription = "Logo OnTime fourni",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().height(72.dp),
        )
        Text(
            text = "DÉMONSTRATION — DONNÉES FICTIVES",
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Ink)
                .padding(10.dp)
                .semantics { contentDescription = "Avertissement : données fictives de démonstration" },
            color = Ink,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "États du domaine",
            color = Ink,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DemoScenario.entries.forEach { candidate ->
                val selected = scenario == candidate
                val onClick = { scenario = candidate }
                if (selected) {
                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Ink,
                            contentColor = Paper,
                        ),
                        modifier = Modifier.weight(1f),
                    ) { Text(candidate.label, fontSize = 11.sp) }
                } else {
                    OutlinedButton(
                        onClick = onClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                        modifier = Modifier.weight(1f),
                    ) { Text(candidate.label, fontSize = 11.sp) }
                }
            }
        }
        SelectionPanel(selection)
        val nearestStation = remember {
            BuildConfig.SNCF_API_KEY.takeIf { it.isNotBlank() }?.let { key ->
                NearestStationFinder(SncfClient(key), SncfQuota, Clock.systemUTC())
            }
        }
        ProfileSection(profileRepository, DemoClock, selection, nearestStation)
        Text(
            text = "Source : fixtures locales • aucune API ni donnée temps réel",
            color = Ink,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SelectionPanel(selection: Selection) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Ink)
            .padding(20.dp)
            .semantics { contentDescription = "État ${selection.status}" },
    ) {
        when (selection.status) {
            Status.Available -> AvailableContent(requireNotNull(selection.departure))
            Status.Empty -> StateMessage("AUCUN DÉPART", "La fixture fraîche ne contient aucun service recommandable.")
            Status.Stale -> StateMessage("DONNÉES PÉRIMÉES", "Aucun horaire n’est affiché comme actuel.")
            Status.Error -> StateMessage("ERREUR DE DONNÉES", "Impossible de proposer un départ depuis cette fixture.")
        }
    }
}

@Composable
private fun AvailableContent(departure: Departure) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Europe/Paris"))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(Ink),
                contentAlignment = Alignment.Center,
            ) {
                Text("A", color = Paper, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text("RER • FIXTURE", color = Ink, fontWeight = FontWeight.Bold)
                Text(departure.destination, color = Ink, fontSize = 19.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("PARTIR DANS", color = Ink, fontWeight = FontWeight.Bold)
        Text("3 min", color = Ink, fontSize = 64.sp, fontWeight = FontWeight.Black)
        Text(
            "Départ transport ${formatter.format(departure.departureAt)} • horaire théorique",
            color = Ink,
            fontSize = 16.sp,
        )
        Text("Fixture collectée il y a 30 s", color = Ink, fontSize = 14.sp)
    }
}

@Composable
private fun StateMessage(title: String, detail: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text(detail, color = Ink, textAlign = TextAlign.Center, fontSize = 17.sp)
    }
}

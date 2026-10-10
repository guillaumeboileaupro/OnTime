package fr.ontime.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Switch
import fr.ontime.app.reminders.ReminderScheduler
import fr.ontime.app.reminders.ReminderStore
import fr.ontime.domain.ReminderWindow
import java.time.DayOfWeek
import java.time.LocalTime
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.ontime.app.data.DeviceLocator
import fr.ontime.app.data.TripLabels
import fr.ontime.app.data.sncf.NearbyStation
import fr.ontime.app.data.NetworkStops
import fr.ontime.app.data.lda.AzurStops
import fr.ontime.app.data.sncf.NearestStationResult
import fr.ontime.app.data.sncf.StationSearchResult
import fr.ontime.domain.ProfileChange
import fr.ontime.domain.ProfileCommandResult
import fr.ontime.domain.ProfileDraft
import fr.ontime.domain.ProfileError
import fr.ontime.domain.ProfileRepository
import fr.ontime.domain.ProfileSnapshot
import fr.ontime.domain.TravelProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ButtonHeight = Modifier.heightIn(min = 48.dp)
private const val LABEL_SEPARATOR = " → "
private const val BUSY_MESSAGE = "Limite de requêtes SNCF atteinte : réessayez dans une minute."
private const val ERROR_MESSAGE = "Recherche impossible pour le moment."
private val WorkingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
private val DayLabels = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")

private fun parseTime(text: String): LocalTime? =
    runCatching { LocalTime.parse(text.trim().replace('h', ':').padStart(5, '0')) }.getOrNull()

@Composable
fun TripsScreen(
    repository: ProfileRepository,
    labels: TripLabels,
    snapshot: ProfileSnapshot,
    onChanged: () -> Unit,
    trainStops: NetworkStops?,
    busStops: NetworkStops,
) {
    val profiles = (snapshot as? ProfileSnapshot.Data)?.profiles.orEmpty()
    val selectedId = (snapshot as? ProfileSnapshot.Data)?.selectedProfileId
    var editingId by remember { mutableStateOf<String?>(null) }
    var busNetwork by remember { mutableStateOf(trainStops == null) }
    val stops = if (busNetwork) busStops else trainStops ?: busStops
    val place = if (busNetwork) "arrêt" else "gare"
    var origin by remember { mutableStateOf<NearbyStation?>(null) }
    var destination by remember { mutableStateOf<NearbyStation?>(null) }
    var originQuery by remember { mutableStateOf("") }
    var originResults by remember { mutableStateOf<List<NearbyStation>>(emptyList()) }
    var destinationQuery by remember { mutableStateOf("") }
    var destinationResults by remember { mutableStateOf<List<NearbyStation>>(emptyList()) }
    var directions by remember { mutableStateOf<List<NearbyStation>>(emptyList()) }
    var walking by remember { mutableStateOf("10") }
    var margin by remember { mutableStateOf("2") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var formOpen by remember { mutableStateOf(false) }
    var remindOn by remember { mutableStateOf(false) }
    var remindDays by remember { mutableStateOf(WorkingDays) }
    var remindFrom by remember { mutableStateOf("07:30") }
    var remindTo by remember { mutableStateOf("09:00") }
    val context = LocalContext.current
    val locator = remember { DeviceLocator(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val reminders = remember { ReminderStore(context.applicationContext) }
    val scheduler = remember { ReminderScheduler(context.applicationContext) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) message = "Notifications refusées : les rappels ne pourront pas s'afficher."
    }

    fun reset() {
        formOpen = false
        editingId = null
        origin = null
        destination = null
        originQuery = ""
        destinationQuery = ""
        originResults = emptyList()
        destinationResults = emptyList()
        directions = emptyList()
        walking = "10"
        margin = "2"
        remindOn = false
        remindDays = WorkingDays
        remindFrom = "07:30"
        remindTo = "09:00"
    }

    fun load(profile: TravelProfile) {
        val label = labels.get(profile.id).orEmpty()
        reset()
        formOpen = true
        editingId = profile.id
        busNetwork = profile.stopId.startsWith(AzurStops.PREFIX) || trainStops == null
        origin = NearbyStation(profile.stopId, label.substringBefore(LABEL_SEPARATOR).ifBlank { "Gare enregistrée" })
        destination = NearbyStation(profile.destinationId, label.substringAfter(LABEL_SEPARATOR, "Destination enregistrée"))
        walking = profile.walkingMinutes.toString()
        margin = profile.marginMinutes.toString()
        reminders.window(profile.id)?.let { window ->
            remindOn = true
            remindDays = window.days
            remindFrom = window.start.toString()
            remindTo = window.end.toString()
        }
    }

    fun runLookup(lookup: () -> StationSearchResult, onFound: (List<NearbyStation>) -> Unit) {
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { lookup() }
            busy = false
            when (result) {
                is StationSearchResult.Found -> onFound(result.stations)
                StationSearchResult.Busy -> message = BUSY_MESSAGE
                StationSearchResult.Error -> message = ERROR_MESSAGE
            }
        }
    }

    fun chooseOrigin(station: NearbyStation) {
        origin = station
        originResults = emptyList()
        destination = null
        directions = emptyList()
        runLookup({ stops.directionsFrom(station.stopAreaId) }) { found ->
            directions = found
            if (found.isEmpty()) message = "Aucun départ connu depuis cet $place pour le moment : recherchez la destination."
        }
    }

    fun locateNearest() {
        busy = true
        message = "Localisation en cours…"
        locator.locate { location ->
            if (location == null) {
                busy = false
                message = "Position indisponible : activez la localisation du téléphone."
                return@locate
            }
            scope.launch {
                val result = withContext(Dispatchers.IO) { stops.nearest(location.latitude, location.longitude) }
                busy = false
                message = when (result) {
                    is NearestStationResult.Found -> {
                        walking = result.walkingMinutes.toString()
                        chooseOrigin(result.station)
                        "${shortName(result.station.name)} : ${result.walkingMinutes} min à pied (${result.walkingMeters} m)."
                    }
                    NearestStationResult.NoStation -> "Aucun $place proche de votre position."
                    NearestStationResult.TooFar -> "Trop éloigné à pied (plus de 3 h)."
                    NearestStationResult.Busy -> BUSY_MESSAGE
                    NearestStationResult.Error -> ERROR_MESSAGE
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) locateNearest() else message = "Localisation refusée : recherchez le départ par son nom."
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (snapshot == ProfileSnapshot.StorageError) {
            Text("Vos trajets n'ont pas pu être lus sur ce téléphone.", style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            "Trajets enregistrés",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        if (profiles.isEmpty()) {
            Text("Aucun trajet enregistré pour le moment.", style = MaterialTheme.typography.bodyLarge)
        }
        profiles.forEach { profile ->
            val shown = profile.id == selectedId
            Column(
                modifier = Modifier.fillMaxWidth().border(if (shown) 2.dp else 1.dp, Ink).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    labels.get(profile.id)?.let(::shortLabel) ?: "Trajet sans nom",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "${profile.walkingMinutes} min de marche + ${profile.marginMinutes} min de marge" +
                        if (shown) " · affiché sur l'accueil" else "",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!shown) {
                        Button(
                            onClick = {
                                message = when (repository.select(profile.id)) {
                                    ProfileCommandResult.Success -> {
                                        onChanged()
                                        "Trajet affiché sur l'accueil."
                                    }
                                    ProfileCommandResult.NotFound -> "Trajet introuvable."
                                    ProfileCommandResult.StorageError -> "Erreur d'enregistrement."
                                }
                            },
                            modifier = Modifier.weight(1f).then(ButtonHeight),
                        ) { Text("Afficher", style = MaterialTheme.typography.labelLarge) }
                    }
                    OutlinedButton(
                        onClick = { load(profile) },
                        modifier = Modifier.weight(1f).then(ButtonHeight),
                    ) { Text("Modifier", style = MaterialTheme.typography.labelLarge) }
                }
            }
        }
        if (!formOpen) {
            Button(
                onClick = {
                    reset()
                    formOpen = true
                    message = null
                },
                modifier = Modifier.fillMaxWidth().then(ButtonHeight),
            ) { Text("+ Nouveau trajet", style = MaterialTheme.typography.labelLarge) }
        }

        if (formOpen) Column(
            modifier = Modifier.fillMaxWidth().border(1.dp, Ink).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (editingId == null) "Nouveau trajet" else "Modifier le trajet",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )

            StepTitle("1. Départ")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(false to "Train", true to "Bus & tram").forEach { (bus, label) ->
                    val selected = busNetwork == bus
                    val choose = {
                        if (!selected) {
                            busNetwork = bus
                            origin = null
                            destination = null
                            directions = emptyList()
                            originResults = emptyList()
                            destinationResults = emptyList()
                        }
                    }
                    if (selected) {
                        Button(onClick = choose, modifier = Modifier.weight(1f).then(ButtonHeight)) {
                            Text(label, style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        OutlinedButton(
                            onClick = choose,
                            enabled = bus || trainStops != null,
                            modifier = Modifier.weight(1f).then(ButtonHeight),
                        ) { Text(label, style = MaterialTheme.typography.labelLarge) }
                    }
                }
            }
            origin?.let { Text("Départ : ${shortName(it.name)}", style = MaterialTheme.typography.bodyLarge) }
            Button(
                enabled = !busy,
                onClick = {
                    focus.clearFocus()
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                    )
                },
                modifier = Modifier.fillMaxWidth().then(ButtonHeight),
            ) { Text(if (busNetwork) "Arrêt le plus proche de moi" else "Gare la plus proche de moi", style = MaterialTheme.typography.labelLarge) }
            StationSearchField("Ou rechercher le départ", originQuery, busy, { originQuery = it }) {
                runLookup({ stops.search(originQuery) }) { found ->
                    originResults = found
                    message = if (found.isEmpty()) "Aucun résultat." else null
                }
            }
            originResults.forEach { StationButton(it.name) { chooseOrigin(it) } }

            StepTitle("2. Destination")
            val start = origin
            if (start == null) {
                Text("Choisissez d'abord le départ.", style = MaterialTheme.typography.bodyMedium)
            } else {
                destination?.let { Text("Arrivée : ${shortName(it.name)}", style = MaterialTheme.typography.bodyLarge) }
                if (directions.isNotEmpty()) {
                    Text("Toutes les directions depuis cette gare :", style = MaterialTheme.typography.bodyMedium)
                    directions.forEach { candidate ->
                        val picked = candidate.stopAreaId == destination?.stopAreaId
                        StationButton("${if (picked) "● " else "○ "}${shortName(candidate.name)}") { destination = candidate }
                    }
                }
                StationSearchField("Ou rechercher l'arrivée", destinationQuery, busy, { destinationQuery = it }) {
                    runLookup({ stops.search(destinationQuery) }) { found ->
                        destinationResults = found.filter { it.stopAreaId != start.stopAreaId }
                        message = if (destinationResults.isEmpty()) "Aucune gare trouvée." else null
                    }
                }
                destinationResults.forEach { candidate ->
                    StationButton(candidate.name) {
                        destination = candidate
                        destinationResults = emptyList()
                    }
                }
            }

            StepTitle("3. Temps")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField("Marche (min)", walking, Modifier.weight(1f)) { walking = it }
                NumberField("Marge (min)", margin, Modifier.weight(1f)) { margin = it }
            }

            StepTitle("4. Rappels")
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    "Me prévenir 5 min avant de partir",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = remindOn,
                    onCheckedChange = { on ->
                        remindOn = on
                        if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }
            if (remindOn) {
                ReminderDays(remindDays) { remindDays = it }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeField("Partir dès", remindFrom, Modifier.weight(1f)) { remindFrom = it }
                    TimeField("Jusqu'à", remindTo, Modifier.weight(1f)) { remindTo = it }
                }
                if (!scheduler.canBeExact() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Text(
                        "Rappels approximatifs (quelques minutes) tant que les alarmes exactes ne sont pas autorisées.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().then(ButtonHeight),
                    ) { Text("Autoriser les alarmes exactes", style = MaterialTheme.typography.labelLarge) }
                }
            }

            Button(
                enabled = origin != null && destination != null && !busy,
                onClick = {
                    focus.clearFocus()
                    val from = requireNotNull(origin)
                    val to = requireNotNull(destination)
                    val window = if (remindOn) {
                        val start = parseTime(remindFrom)
                        val end = parseTime(remindTo)
                        ReminderWindow(remindDays, start ?: LocalTime.MIN, end ?: LocalTime.MIN)
                            .takeIf { start != null && end != null && it.isValid }
                            ?: run {
                                message = "Rappels : choisissez au moins un jour et une plage horaire valide (ex. 07:30 à 09:00)."
                                return@Button
                            }
                    } else {
                        null
                    }
                    val draft = ProfileDraft(
                        stopId = from.stopAreaId,
                        destinationId = to.stopAreaId,
                        walkingMinutes = walking.toIntOrNull() ?: -1,
                        marginMinutes = margin.toIntOrNull() ?: -1,
                    )
                    val result = editingId?.let { repository.update(it, draft) } ?: repository.create(draft)
                    message = when (result) {
                        is ProfileChange.Success -> {
                            labels.put(result.profile.id, "${shortName(from.name)}$LABEL_SEPARATOR${shortName(to.name)}")
                            reminders.setWindow(result.profile.id, window)
                            if (window == null) scheduler.cancel(result.profile.id, ReminderScheduler.ACTION_PLAN)
                            scheduler.planAll(listOf(result.profile), reminders)
                            val selection = repository.select(result.profile.id)
                            onChanged()
                            reset()
                            if (selection == ProfileCommandResult.Success) {
                                "Trajet enregistré et affiché sur l'accueil."
                            } else {
                                "Trajet enregistré, mais pas sélectionné."
                            }
                        }
                        is ProfileChange.Invalid -> result.errors.joinToString(" ") { it.explanation() }
                        ProfileChange.NotFound -> "Trajet introuvable."
                        ProfileChange.StorageError -> "Erreur d'enregistrement."
                    }
                },
                modifier = Modifier.fillMaxWidth().then(ButtonHeight),
            ) { Text("Enregistrer le trajet", style = MaterialTheme.typography.labelLarge) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        reset()
                        message = null
                    },
                    modifier = Modifier.weight(1f).then(ButtonHeight),
                ) { Text("Annuler", style = MaterialTheme.typography.labelLarge) }
                editingId?.let { id ->
                    OutlinedButton(
                        onClick = {
                            message = when (repository.delete(id)) {
                                ProfileCommandResult.Success -> {
                                    labels.remove(id)
                                    scheduler.forget(id, reminders)
                                    reset()
                                    onChanged()
                                    "Trajet supprimé."
                                }
                                ProfileCommandResult.NotFound -> "Trajet introuvable."
                                ProfileCommandResult.StorageError -> "Erreur d'enregistrement."
                            }
                        },
                        modifier = Modifier.weight(1f).then(ButtonHeight),
                    ) { Text("Supprimer", style = MaterialTheme.typography.labelLarge) }
                }
            }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderDays(selected: Set<DayOfWeek>, onChange: (Set<DayOfWeek>) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DayOfWeek.entries.forEachIndexed { index, day ->
            val on = day in selected
            val toggle = { onChange(if (on) selected - day else selected + day) }
            if (on) {
                Button(onClick = toggle, modifier = ButtonHeight) { Text(DayLabels[index], style = MaterialTheme.typography.labelLarge) }
            } else {
                OutlinedButton(onClick = toggle, modifier = ButtonHeight) { Text(DayLabels[index], style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

@Composable
private fun TimeField(label: String, value: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        isError = parseTime(value) == null,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
    )
}

@Composable
private fun StepTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}

@Composable
private fun StationButton(text: String, onClick: () -> Unit) {
    val focus = LocalFocusManager.current
    OutlinedButton(onClick = { focus.clearFocus(); onClick() }, modifier = Modifier.fillMaxWidth().then(ButtonHeight)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun StationSearchField(
    label: String,
    query: String,
    busy: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    val canSearch = !busy && query.trim().length >= 2
    val focus = LocalFocusManager.current
    val search = {
        focus.clearFocus()
        onSearch()
    }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text(label) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { if (canSearch) search() }),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(
        enabled = canSearch,
        onClick = search,
        modifier = Modifier.fillMaxWidth().then(ButtonHeight),
    ) { Text("Rechercher", style = MaterialTheme.typography.labelLarge) }
}

/** Shortens every part of a saved "Departure → Arrival" label. */
internal fun shortLabel(label: String) =
    label.split(LABEL_SEPARATOR).joinToString(LABEL_SEPARATOR) { shortName(it) }

private fun ProfileError.explanation() = when (this) {
    ProfileError.InvalidId -> "Identifiant invalide."
    ProfileError.EmptyStop -> "Choisissez la gare de départ."
    ProfileError.EmptyDestination -> "Choisissez la destination."
    ProfileError.SameStopAndDestination -> "La destination doit être différente du départ."
    ProfileError.InvalidWalking -> "Marche : entre 0 et 180 min."
    ProfileError.InvalidMargin -> "Marge : entre 0 et 60 min."
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
    )
}

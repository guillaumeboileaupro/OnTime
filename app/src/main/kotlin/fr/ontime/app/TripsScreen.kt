package fr.ontime.app

import android.Manifest
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
import fr.ontime.app.data.sncf.SncfServices
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

@Composable
fun TripsScreen(
    repository: ProfileRepository,
    labels: TripLabels,
    snapshot: ProfileSnapshot,
    onChanged: () -> Unit,
    sncf: SncfServices?,
) {
    if (sncf == null) {
        Text(
            "Cette version ne contient pas de clé SNCF : impossible de chercher une gare.",
            style = MaterialTheme.typography.bodyLarge,
        )
        return
    }
    val profiles = (snapshot as? ProfileSnapshot.Data)?.profiles.orEmpty()
    val selectedId = (snapshot as? ProfileSnapshot.Data)?.selectedProfileId
    var editingId by remember { mutableStateOf<String?>(null) }
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
    val context = LocalContext.current
    val locator = remember { DeviceLocator(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

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
    }

    fun load(profile: TravelProfile) {
        val label = labels.get(profile.id).orEmpty()
        reset()
        formOpen = true
        editingId = profile.id
        origin = NearbyStation(profile.stopId, label.substringBefore(LABEL_SEPARATOR).ifBlank { "Gare enregistrée" })
        destination = NearbyStation(profile.destinationId, label.substringAfter(LABEL_SEPARATOR, "Destination enregistrée"))
        walking = profile.walkingMinutes.toString()
        margin = profile.marginMinutes.toString()
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
        runLookup({ sncf.search.directionsFrom(station.stopAreaId) }) { found ->
            directions = found
            if (found.isEmpty()) message = "Aucune ligne SNCF connue depuis cette gare."
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
                val result = withContext(Dispatchers.IO) { sncf.nearest.find(location.latitude, location.longitude) }
                busy = false
                message = when (result) {
                    is NearestStationResult.Found -> {
                        walking = result.walkingMinutes.toString()
                        chooseOrigin(result.station)
                        "${result.walkingMinutes} min à pied (${result.walkingMeters} m) d'après l'itinéraire SNCF."
                    }
                    NearestStationResult.NoStation -> "Aucune gare SNCF à moins de 3 km."
                    NearestStationResult.TooFar -> "Gare trop éloignée à pied (plus de 3 h)."
                    NearestStationResult.Busy -> BUSY_MESSAGE
                    NearestStationResult.Error -> ERROR_MESSAGE
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) locateNearest() else message = "Localisation refusée : recherchez la gare par son nom."
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

            StepTitle("1. Gare de départ")
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
            ) { Text("Gare la plus proche de moi", style = MaterialTheme.typography.labelLarge) }
            StationSearchField("Ou rechercher la gare de départ", originQuery, busy, { originQuery = it }) {
                runLookup({ sncf.search.search(originQuery) }) { found ->
                    originResults = found
                    message = if (found.isEmpty()) "Aucune gare trouvée." else null
                }
            }
            originResults.forEach { StationButton(it.name) { chooseOrigin(it) } }

            StepTitle("2. Destination")
            val start = origin
            if (start == null) {
                Text("Choisissez d'abord la gare de départ.", style = MaterialTheme.typography.bodyMedium)
            } else {
                destination?.let { Text("Arrivée : ${shortName(it.name)}", style = MaterialTheme.typography.bodyLarge) }
                if (directions.isNotEmpty()) {
                    Text("Toutes les directions depuis cette gare :", style = MaterialTheme.typography.bodyMedium)
                    directions.forEach { candidate ->
                        val picked = candidate.stopAreaId == destination?.stopAreaId
                        StationButton("${if (picked) "● " else "○ "}${shortName(candidate.name)}") { destination = candidate }
                    }
                }
                StationSearchField("Ou rechercher la gare d'arrivée", destinationQuery, busy, { destinationQuery = it }) {
                    runLookup({ sncf.search.search(destinationQuery) }) { found ->
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

            Button(
                enabled = origin != null && destination != null && !busy,
                onClick = {
                    focus.clearFocus()
                    val from = requireNotNull(origin)
                    val to = requireNotNull(destination)
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

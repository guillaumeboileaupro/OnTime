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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.ontime.app.data.DeviceLocator
import fr.ontime.app.data.TripLabels
import fr.ontime.app.data.sncf.NearbyStation
import fr.ontime.app.data.sncf.NearestStationResult
import fr.ontime.app.data.sncf.StationSearchResult
import fr.ontime.domain.ProfileChange
import fr.ontime.domain.ProfileCommandResult
import fr.ontime.domain.ProfileDraft
import fr.ontime.domain.ProfileError
import fr.ontime.domain.ProfileRepository
import fr.ontime.domain.ProfileSnapshot
import fr.ontime.domain.Status
import fr.ontime.domain.TravelProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ButtonHeight = Modifier.heightIn(min = 48.dp)
private const val LABEL_SEPARATOR = " → "

private data class TripChoice(val lineId: String, val directionId: String, val label: String)

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
    var station by remember { mutableStateOf<NearbyStation?>(null) }
    var choice by remember { mutableStateOf<TripChoice?>(null) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<NearbyStation>>(emptyList()) }
    var choices by remember { mutableStateOf<List<TripChoice>>(emptyList()) }
    var walking by remember { mutableStateOf("10") }
    var margin by remember { mutableStateOf("2") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val locator = remember { DeviceLocator(context.applicationContext) }
    val scope = rememberCoroutineScope()

    fun reset() {
        editingId = null
        station = null
        choice = null
        choices = emptyList()
        results = emptyList()
        walking = "10"
        margin = "2"
    }

    fun load(profile: TravelProfile) {
        val label = labels.get(profile.id).orEmpty()
        editingId = profile.id
        station = NearbyStation(profile.stopId, label.substringBefore(LABEL_SEPARATOR).ifBlank { "Gare enregistrée" })
        choice = TripChoice(profile.lineId, profile.direction, label.substringAfter(LABEL_SEPARATOR, "Direction enregistrée"))
        choices = emptyList()
        walking = profile.walkingMinutes.toString()
        margin = profile.marginMinutes.toString()
    }

    fun chooseStation(found: NearbyStation) {
        station = found
        choice = null
        choices = emptyList()
        results = emptyList()
    }

    fun loadDirections(stopId: String) {
        busy = true
        scope.launch {
            val snapshotAtStop = withContext(Dispatchers.IO) { sncf.departures.fetch(stopId) }
            busy = false
            choices = snapshotAtStop.departures
                .distinctBy { it.lineId to it.directionId }
                .map { TripChoice(it.lineId, it.directionId, "${modeLabel(it.mode)}$LABEL_SEPARATOR${it.destination}") }
            when {
                snapshotAtStop.status == Status.Error -> message = "Impossible de charger les directions."
                choices.isEmpty() -> message = "Aucun train annoncé dans cette gare pour le moment."
            }
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
                        chooseStation(result.station)
                        walking = result.walkingMinutes.toString()
                        loadDirections(result.station.stopAreaId)
                        "${result.walkingMinutes} min à pied (${result.walkingMeters} m) d'après l'itinéraire SNCF."
                    }
                    NearestStationResult.NoStation -> "Aucune gare SNCF à moins de 3 km."
                    NearestStationResult.TooFar -> "Gare trop éloignée à pied (plus de 3 h)."
                    NearestStationResult.Busy -> "Trop de recherches rapprochées : réessayez dans un instant."
                    NearestStationResult.Error -> "Recherche impossible pour le moment."
                }
            }
        }
    }

    fun searchByName() {
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { sncf.search.search(query) }
            busy = false
            when (result) {
                is StationSearchResult.Found -> {
                    results = result.stations
                    message = if (result.stations.isEmpty()) "Aucune gare trouvée." else null
                }
                StationSearchResult.Busy -> message = "Trop de recherches rapprochées : réessayez dans un instant."
                StationSearchResult.Error -> message = "Recherche impossible pour le moment."
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
        profiles.forEach { profile ->
            val selected = profile.id == selectedId
            OutlinedButton(
                onClick = {
                    message = when (repository.select(profile.id)) {
                        ProfileCommandResult.Success -> {
                            load(profile)
                            onChanged()
                            "Trajet affiché sur l'accueil."
                        }
                        ProfileCommandResult.NotFound -> "Trajet introuvable."
                        ProfileCommandResult.StorageError -> "Erreur d'enregistrement."
                    }
                },
                modifier = Modifier.fillMaxWidth().then(ButtonHeight),
            ) {
                Text(
                    "${if (selected) "● " else "○ "}${labels.get(profile.id) ?: "Trajet sans nom"}",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().border(1.dp, Ink).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (editingId == null) "Nouveau trajet" else "Modifier le trajet",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )

            StepTitle("1. Gare de départ")
            station?.let { Text("Gare : ${it.name}", style = MaterialTheme.typography.bodyLarge) }
            Button(
                enabled = !busy,
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                    )
                },
                modifier = Modifier.fillMaxWidth().then(ButtonHeight),
            ) { Text("Gare la plus proche de moi", style = MaterialTheme.typography.labelLarge) }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Ou rechercher une gare") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { if (!busy) searchByName() }),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                enabled = !busy && query.trim().length >= 2,
                onClick = { searchByName() },
                modifier = Modifier.fillMaxWidth().then(ButtonHeight),
            ) { Text("Rechercher", style = MaterialTheme.typography.labelLarge) }
            results.forEach { found ->
                OutlinedButton(
                    onClick = {
                        chooseStation(found)
                        loadDirections(found.stopAreaId)
                    },
                    modifier = Modifier.fillMaxWidth().then(ButtonHeight),
                ) { Text(found.name, style = MaterialTheme.typography.labelLarge) }
            }

            StepTitle("2. Direction")
            val currentStation = station
            when {
                currentStation == null -> Text("Choisissez d'abord une gare.", style = MaterialTheme.typography.bodyMedium)
                choices.isEmpty() -> {
                    choice?.let { Text("Direction : ${it.label}", style = MaterialTheme.typography.bodyLarge) }
                    OutlinedButton(
                        enabled = !busy,
                        onClick = { loadDirections(currentStation.stopAreaId) },
                        modifier = Modifier.fillMaxWidth().then(ButtonHeight),
                    ) { Text("Voir les directions", style = MaterialTheme.typography.labelLarge) }
                }
                else -> choices.forEach { candidate ->
                    val picked = candidate == choice
                    OutlinedButton(
                        onClick = { choice = candidate },
                        modifier = Modifier.fillMaxWidth().then(ButtonHeight),
                    ) { Text("${if (picked) "● " else "○ "}${candidate.label}", style = MaterialTheme.typography.labelLarge) }
                }
            }

            StepTitle("3. Temps")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField("Marche (min)", walking, Modifier.weight(1f)) { walking = it }
                NumberField("Marge (min)", margin, Modifier.weight(1f)) { margin = it }
            }

            val ready = station != null && choice != null
            Button(
                enabled = ready && !busy,
                onClick = {
                    val chosenStation = requireNotNull(station)
                    val chosenDirection = requireNotNull(choice)
                    val draft = ProfileDraft(
                        stopId = chosenStation.stopAreaId,
                        lineId = chosenDirection.lineId,
                        direction = chosenDirection.directionId,
                        walkingMinutes = walking.toIntOrNull() ?: -1,
                        marginMinutes = margin.toIntOrNull() ?: -1,
                    )
                    val result = editingId?.let { repository.update(it, draft) } ?: repository.create(draft)
                    message = when (result) {
                        is ProfileChange.Success -> {
                            labels.put(result.profile.id, "${chosenStation.name}$LABEL_SEPARATOR${chosenDirection.label.substringAfter(LABEL_SEPARATOR)}")
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
            editingId?.let { id ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            reset()
                            message = null
                        },
                        modifier = Modifier.weight(1f).then(ButtonHeight),
                    ) { Text("Annuler", style = MaterialTheme.typography.labelLarge) }
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

private fun ProfileError.explanation() = when (this) {
    ProfileError.InvalidId -> "Identifiant invalide."
    ProfileError.EmptyStop -> "Choisissez une gare."
    ProfileError.EmptyLine, ProfileError.EmptyDirection -> "Choisissez une direction."
    ProfileError.InvalidWalking -> "Marche : entre 0 et 180 min."
    ProfileError.InvalidMargin -> "Marge : entre 0 et 60 min."
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

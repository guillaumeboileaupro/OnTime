package fr.ontime.app

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.ontime.domain.HomeDepartureCalculator
import fr.ontime.domain.ProfileChange
import fr.ontime.domain.ProfileCommandResult
import fr.ontime.domain.ProfileDraft
import fr.ontime.domain.ProfileRepository
import fr.ontime.domain.Selection
import fr.ontime.domain.Status
import fr.ontime.domain.TravelProfile
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ProfilePaper = Color(0xFFDFDCD3)
private val ProfileInk = Color(0xFF2A2926)

@Composable
fun ProfileSection(repository: ProfileRepository, clock: Clock, selection: Selection) {
    var revision by remember { mutableIntStateOf(0) }
    val profiles = remember(revision) { repository.profiles() }
    val selectedId = remember(revision) { repository.selectedProfileId() }
    var editingId by remember { mutableStateOf<String?>(null) }
    var stop by remember { mutableStateOf("demo:stop:central") }
    var line by remember { mutableStateOf("A") }
    var direction by remember { mutableStateOf("Direction de démonstration") }
    var walking by remember { mutableStateOf("7") }
    var margin by remember { mutableStateOf("2") }
    var message by remember { mutableStateOf<String?>(null) }

    fun load(profile: TravelProfile) {
        editingId = profile.id
        stop = profile.stopId
        line = profile.lineId
        direction = profile.direction
        walking = profile.walkingMinutes.toString()
        margin = profile.marginMinutes.toString()
    }

    fun draft() = ProfileDraft(
        stopId = stop,
        lineId = line,
        direction = direction,
        walkingMinutes = walking.toIntOrNull() ?: -1,
        marginMinutes = margin.toIntOrNull() ?: -1,
    )

    fun refresh() {
        revision += 1
    }

    Column(
        modifier = Modifier.fillMaxWidth().border(2.dp, ProfileInk).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("PROFILS — FIXTURES LOCALES", color = ProfileInk)
        Text("Persistés sur cet appareil, sans API réelle.", color = ProfileInk)
        profiles.forEach { profile ->
            val selected = profile.id == selectedId
            OutlinedButton(
                onClick = {
                    message = when (repository.select(profile.id)) {
                        ProfileCommandResult.Success -> {
                            load(profile)
                            refresh()
                            "Profil sélectionné"
                        }
                        ProfileCommandResult.NotFound -> "Profil introuvable"
                        ProfileCommandResult.StorageError -> "Erreur de stockage"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "${if (selected) "●" else "○"} ${profile.lineId} · ${profile.direction}",
                    color = ProfileInk,
                )
            }
        }
        selectedId?.let { id ->
            profiles.firstOrNull { it.id == id }?.let { profile ->
                val departure = selection.departure?.takeIf {
                    selection.status == Status.Available &&
                        it.stopId == profile.stopId && it.lineId == profile.lineId
                }
                if (departure == null) {
                    Text("Aucun départ fixture compatible à calculer.", color = ProfileInk)
                } else {
                    val leave = HomeDepartureCalculator(clock).calculate(departure.departureAt, profile)
                    val formatted = DateTimeFormatter.ofPattern("HH:mm")
                        .withZone(ZoneId.of("Europe/Paris"))
                        .format(leave.leaveAt)
                    Text("Départ de chez soi : $formatted (fixture)", color = ProfileInk)
                }
            }
        }
        ProfileField("Arrêt", stop) { stop = it }
        ProfileField("Ligne", line) { line = it }
        ProfileField("Direction", direction) { direction = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileField("Marche (min)", walking, Modifier.weight(1f)) { walking = it }
            ProfileField("Marge (min)", margin, Modifier.weight(1f)) { margin = it }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val result = editingId?.let { repository.update(it, draft()) }
                        ?: repository.create(draft())
                    message = when (result) {
                        is ProfileChange.Success -> {
                            editingId = result.profile.id
                            when (repository.select(result.profile.id)) {
                                ProfileCommandResult.Success -> {
                                    refresh()
                                    "Profil enregistré"
                                }
                                ProfileCommandResult.NotFound -> "Profil enregistré mais introuvable"
                                ProfileCommandResult.StorageError -> "Profil enregistré, sélection non persistée"
                            }
                        }
                        is ProfileChange.Invalid -> "Paramètres invalides : ${result.errors.joinToString()}"
                        ProfileChange.NotFound -> "Profil introuvable"
                        ProfileChange.StorageError -> "Erreur de stockage"
                    }
                },
                colors = ButtonDefaults.buttonColors(ProfileInk, ProfilePaper),
            ) { Text(if (editingId == null) "Créer" else "Modifier") }
            OutlinedButton(onClick = {
                editingId = null
                message = "Nouveau profil"
            }) { Text("Nouveau", color = ProfileInk) }
            editingId?.let { id ->
                OutlinedButton(onClick = {
                    message = when (repository.delete(id)) {
                        ProfileCommandResult.Success -> {
                            editingId = null
                            refresh()
                            "Profil supprimé"
                        }
                        ProfileCommandResult.NotFound -> "Profil introuvable"
                        ProfileCommandResult.StorageError -> "Erreur de stockage"
                    }
                }) { Text("Supprimer", color = ProfileInk) }
            }
        }
        message?.let { Text(it, color = ProfileInk) }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
    )
}

package fr.ontime.app

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.ontime.domain.Departure
import fr.ontime.domain.HomeDepartureCalculator
import fr.ontime.domain.Mode
import fr.ontime.domain.Quality
import fr.ontime.domain.Selection
import fr.ontime.domain.Status
import fr.ontime.domain.TravelProfile
import fr.ontime.domain.reminderAt
import androidx.compose.material3.Button
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Europe/Paris"))

sealed interface HomeState {
    data object NoKey : HomeState
    data object NoTrip : HomeState
    data object Loading : HomeState
    data class Ready(
        val selection: Selection,
        val checkedAt: Instant,
        /** Cancelled departures before the recommended one, to warn about. */
        val cancelled: List<Departure> = emptyList(),
    ) : HomeState
}

@Composable
fun HomeScreen(
    state: HomeState,
    trip: TravelProfile?,
    tripLabel: String?,
    clock: Clock,
    onOpenTrips: () -> Unit,
    onRefresh: () -> Unit,
    reminderNote: String? = null,
    onRemind: ((Departure) -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (trip != null) {
            Text(tripLabel ?: "Trajet enregistré", style = MaterialTheme.typography.titleMedium)
        }
        Column(
            modifier = Modifier.fillMaxWidth().border(2.dp, Ink).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (state) {
                HomeState.NoKey -> StateCard(
                    "Horaires indisponibles",
                    "Cette version ne contient pas de clé SNCF : les horaires ne peuvent pas être récupérés.",
                )
                HomeState.NoTrip -> StateCard(
                    "Aucun trajet",
                    "Choisissez votre gare et votre direction pour savoir quand partir de chez vous.",
                )
                HomeState.Loading -> StateCard("Recherche des horaires…", "Interrogation de la SNCF en cours.")
                is HomeState.Ready -> {
                    state.cancelled.forEach { CancelledLine(it) }
                    SelectionContent(state.selection, requireNotNull(trip), clock)
                }
            }
        }
        val next = (state as? HomeState.Ready)?.selection?.departure
        if (next != null && trip != null && onRemind != null) {
            val remindAt = trip.reminderAt(next)
            if (remindAt.isAfter(clock.instant())) {
                Button(onClick = { onRemind(next) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Me prévenir à ${TimeFormat.format(remindAt)}", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        reminderNote?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        if (state is HomeState.Ready) {
            Text("Vérifié à ${TimeFormat.format(state.checkedAt)}", style = MaterialTheme.typography.labelMedium)
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Actualiser", style = MaterialTheme.typography.labelLarge)
            }
        }
        if (state == HomeState.NoTrip) {
            OutlinedButton(onClick = onOpenTrips, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Créer mon trajet", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SelectionContent(selection: Selection, trip: TravelProfile, clock: Clock) {
    when (selection.status) {
        Status.Available -> DepartureCard(requireNotNull(selection.departure), trip, clock)
        Status.Empty -> StateCard(
            "Aucun train à prendre",
            "Aucun train direct atteignable n'est annoncé pour ce trajet avec " +
                "${walkAndMargin(trip)}.",
        )
        Status.Stale -> StateCard(
            "Horaires pas à jour",
            "Les derniers horaires reçus sont trop anciens : aucun départ n'est proposé pour éviter une erreur.",
        )
        Status.Error -> StateCard(
            "Horaires indisponibles",
            "Impossible de joindre la SNCF. Vérifiez la connexion puis actualisez.",
        )
    }
}

@Composable
private fun DepartureCard(departure: Departure, trip: TravelProfile, clock: Clock) {
    val leave = HomeDepartureCalculator(clock).calculate(departure.departureAt, trip)
    Text("Partir dans", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    Text("${leave.timeUntilLeave.toMinutes()} min", style = MaterialTheme.typography.displayLarge)
    Text("Quittez la maison à ${TimeFormat.format(leave.leaveAt)}", style = MaterialTheme.typography.titleMedium)
    Text(
        "${modeLabel(departure.mode)} de ${TimeFormat.format(departure.departureAt)}" +
            (departure.arrivalAt?.let { ", arrivée ${TimeFormat.format(it)}" } ?: ""),
        style = MaterialTheme.typography.bodyLarge,
    )
    departure.delayMinutes?.let { delay ->
        Text(
            "En retard de $delay min (prévu ${TimeFormat.format(requireNotNull(departure.scheduledAt))})",
            style = MaterialTheme.typography.titleMedium,
        )
    }
    departure.disruption?.let { Text("Cause : $it", style = MaterialTheme.typography.bodyMedium) }
    Text("Direction ${shortName(departure.destination)}", style = MaterialTheme.typography.bodyMedium)
    Text(
        "${sourceLabel(departure.provider)} · ${qualityLabel(departure.quality)} · ${walkAndMargin(trip)}",
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun CancelledLine(departure: Departure) {
    Text(
        "⚠ ${modeLabel(departure.mode)} de ${TimeFormat.format(departure.departureAt)} supprimé" +
            (departure.disruption?.let { " ($it)" } ?: ""),
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun StateCard(title: String, detail: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
    Text(detail, style = MaterialTheme.typography.bodyLarge)
}

private fun walkAndMargin(trip: TravelProfile) =
    "${trip.walkingMinutes} min de marche + ${trip.marginMinutes} min de marge"

/** Drops the trailing "(Commune)" the SNCF API appends to stop and destination names. */
internal fun shortName(name: String): String = name.replace(Regex("""\s*\([^()]*\)\s*$"""), "").ifBlank { name }

internal fun modeLabel(mode: Mode) = when (mode) {
    Mode.Train -> "Train"
    Mode.Rer -> "RER"
    Mode.Metro -> "Métro"
    Mode.Tram -> "Tram"
    Mode.Bus -> "Bus"
}

private fun sourceLabel(provider: String) = if (provider == "lignes-azur") "Lignes d'Azur" else "SNCF"

private fun qualityLabel(quality: Quality) = when (quality) {
    Quality.Realtime -> "Horaire en temps réel"
    Quality.Scheduled -> "Horaire prévu (sans temps réel)"
    Quality.Estimated -> "Horaire estimé"
}

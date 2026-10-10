package fr.ontime.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import fr.ontime.app.Ink
import fr.ontime.app.MainActivity
import fr.ontime.app.Paper
import fr.ontime.app.TimeFormat
import fr.ontime.app.data.SharedPreferencesProfileRepository
import fr.ontime.app.data.TripLabels
import fr.ontime.app.data.sncf.SncfServices
import fr.ontime.app.data.sncf.TripUpdate
import fr.ontime.app.shortLabel
import fr.ontime.domain.ProfileSnapshot
import fr.ontime.domain.Status
import fr.ontime.domain.TravelProfile
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface WidgetContent {
    data object NoTrip : WidgetContent
    data object NoKey : WidgetContent
    data class Ready(val label: String, val trip: TravelProfile, val update: TripUpdate) : WidgetContent
}

/**
 * Home-screen widget showing one saved trip per instance. It shows absolute
 * times (leave home, train, arrival) because Android does not refresh widgets
 * every minute in the background; the check time is always visible.
 */
class TripWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val tripId = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[TRIP_KEY]
        val trip = (SharedPreferencesProfileRepository(context).snapshot() as? ProfileSnapshot.Data)
            ?.profiles?.firstOrNull { it.id == tripId }
        val sncf = SncfServices.shared()
        val content = when {
            trip == null -> WidgetContent.NoTrip
            sncf == null -> WidgetContent.NoKey
            else -> WidgetContent.Ready(
                label = TripLabels(context).get(trip.id)?.let(::shortLabel) ?: "Trajet",
                trip = trip,
                update = withContext(Dispatchers.IO) { sncf.tripUpdate(trip) },
            )
        }
        provideContent { WidgetBody(content) }
    }

    companion object {
        val TRIP_KEY = stringPreferencesKey("trip_id")
    }
}

class TripWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TripWidget()
}

class RefreshTripWidget : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        TripWidget().update(context, glanceId)
    }
}

private val InkText = ColorProvider(Ink)

private fun style(size: Int, bold: Boolean = false) =
    TextStyle(color = InkText, fontSize = size.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)

@Composable
private fun WidgetBody(content: WidgetContent) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Paper)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = (content as? WidgetContent.Ready)?.label ?: "OnTime",
                style = style(16, bold = true),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            if (content is WidgetContent.Ready) {
                Text(
                    text = "↻",
                    style = style(22, bold = true),
                    modifier = GlanceModifier.padding(horizontal = 8.dp).clickable(actionRunCallback<RefreshTripWidget>()),
                )
            }
        }
        Spacer(GlanceModifier.height(6.dp))
        when (content) {
            WidgetContent.NoTrip -> Message("Trajet introuvable : appuyez longuement pour choisir un trajet.")
            WidgetContent.NoKey -> Message("Horaires indisponibles dans cette version.")
            is WidgetContent.Ready -> ReadyBody(content)
        }
    }
}

@Composable
private fun ReadyBody(content: WidgetContent.Ready) {
    val update = content.update
    when (update.selection.status) {
        Status.Available -> {
            val pause = Duration.ofMinutes((content.trip.walkingMinutes + content.trip.marginMinutes).toLong())
            update.upcoming.forEachIndexed { index, departure ->
                val leave = TimeFormat.format(departure.departureAt.minus(pause))
                val arrival = departure.arrivalAt?.let { ", arrivée ${TimeFormat.format(it)}" } ?: ""
                if (index == 0) {
                    Text("Quittez la maison à $leave", style = style(20, bold = true))
                    Text("Train ${TimeFormat.format(departure.departureAt)}$arrival", style = style(15))
                    Spacer(GlanceModifier.height(4.dp))
                } else {
                    Text("Puis $leave · train ${TimeFormat.format(departure.departureAt)}", style = style(14))
                }
            }
        }
        Status.Empty -> Message("Aucun train direct à prendre pour le moment.")
        Status.Stale -> Message("Horaires pas à jour : touchez ↻.")
        Status.Error -> Message("Horaires indisponibles : vérifiez la connexion puis touchez ↻.")
    }
    Spacer(GlanceModifier.height(4.dp))
    Text("Vérifié à ${TimeFormat.format(update.checkedAt)}", style = style(14))
}

@Composable
private fun Message(text: String) {
    Text(text, style = style(15))
}

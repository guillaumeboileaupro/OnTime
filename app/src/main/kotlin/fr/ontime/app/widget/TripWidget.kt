package fr.ontime.app.widget

import android.content.Context
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.ui.unit.DpSize
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.SizeMode
import fr.ontime.app.R
import fr.ontime.domain.leaveAt
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
import fr.ontime.app.modeLabel
import fr.ontime.domain.Departure
import fr.ontime.app.data.SharedPreferencesProfileRepository
import fr.ontime.app.data.TripLabels
import fr.ontime.app.data.Transport
import fr.ontime.app.data.sncf.TripUpdate
import fr.ontime.app.shortLabel
import fr.ontime.domain.ProfileSnapshot
import fr.ontime.domain.Status
import fr.ontime.domain.TravelProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface WidgetContent {
    data object NoTrip : WidgetContent
    data object NoKey : WidgetContent
    data class Ready(val label: String, val trip: TravelProfile, val update: TripUpdate) : WidgetContent
}

/**
 * Home-screen widget showing one saved trip per instance, read at a glance: a
 * native countdown to the time to leave home (it ticks without app updates),
 * that time, and on wide sizes the train, arrival and next option.
 */
class TripWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Responsive(setOf(Small, Wide, Large))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val tripId = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[TRIP_KEY]
        // No trip chosen for this widget: follow the trip shown on the home screen.
        val trip = (SharedPreferencesProfileRepository(context).snapshot() as? ProfileSnapshot.Data)
            ?.let { data -> data.profiles.firstOrNull { it.id == (tripId ?: data.selectedProfileId) } }
        val update = trip?.let { withContext(Dispatchers.IO) { Transport.tripUpdate(context, it, limit = 4) } }
        val content = when {
            trip == null -> WidgetContent.NoTrip
            update == null -> WidgetContent.NoKey
            else -> WidgetContent.Ready(
                label = TripLabels(context).get(trip.id)?.let(::shortLabel) ?: "Trajet",
                trip = trip,
                update = update,
            )
        }
        // Refresh just after the shown leave time so the widget moves to the next train.
        (content as? WidgetContent.Ready)?.let { ready ->
            ready.update.upcoming.firstOrNull()?.let { next ->
                WidgetRefreshAlarm.schedule(context, ready.trip.leaveAt(next).plusSeconds(60))
            }
        }
        provideContent { WidgetBody(content) }
    }

    companion object {
        val TRIP_KEY = stringPreferencesKey("trip_id")
    }
}

/** 2x2 widget (resizable). */
class TripWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TripWidget()
}

/** 4x2 widget: same content, wide layout with the train, arrival and next option. */
class TripWidgetWideReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TripWidget()
}

/** 4x3 departures board: countdown plus the next departures with their transport mode. */
class TripWidgetBoardReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TripWidget()
}

class RefreshTripWidget : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        TripWidget().update(context, glanceId)
    }
}

private val InkText = ColorProvider(Ink)
private val Small = DpSize(110.dp, 110.dp)
private val Wide = DpSize(250.dp, 110.dp)
private val Large = DpSize(250.dp, 200.dp)

private fun style(size: Int, bold: Boolean = false) =
    TextStyle(color = InkText, fontSize = size.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)

@Composable
private fun WidgetBody(content: WidgetContent) {
    val wide = LocalSize.current.width >= Wide.width
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = (content as? WidgetContent.Ready)?.label ?: "OnTime",
                style = style(14, bold = true),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            if (content is WidgetContent.Ready) {
                Text(
                    text = "↻",
                    style = style(20, bold = true),
                    modifier = GlanceModifier.padding(start = 8.dp).clickable(actionRunCallback<RefreshTripWidget>()),
                )
            }
        }
        when (content) {
            WidgetContent.NoTrip -> Message("Créez un trajet dans OnTime.")
            WidgetContent.NoKey -> Message("Horaires indisponibles.")
            is WidgetContent.Ready -> ReadyBody(content, wide)
        }
    }
}

@Composable
private fun ReadyBody(content: WidgetContent.Ready, wide: Boolean) {
    val update = content.update
    val next = update.upcoming.firstOrNull()
    if (update.selection.status != Status.Available || next == null) {
        Message(
            when (update.selection.status) {
                Status.Stale -> "Horaires pas à jour : touchez ↻."
                Status.Error -> "Horaires indisponibles : touchez ↻."
                else -> "Aucun départ direct à prendre."
            },
        )
        return
    }
    val leave = content.trip.leaveAt(next)
    val large = LocalSize.current.height >= Large.height
    if (large) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Partir dans ", style = style(14))
            Countdown(leave)
            Text("  à ${TimeFormat.format(leave)}", style = style(16, bold = true))
        }
        Spacer(GlanceModifier.height(6.dp))
        update.upcoming.forEach { departure -> DepartureRow(departure, content.trip) }
        Spacer(GlanceModifier.height(4.dp))
        Text("Vérifié à ${TimeFormat.format(update.checkedAt)}", style = style(14))
        return
    }
    if (wide) {
        // Launcher 4x2 cells can be barely 100 dp high: keep it to three short lines.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Partir dans ", style = style(14))
            Countdown(leave)
            Text("  à ${TimeFormat.format(leave)}", style = style(16, bold = true))
        }
        DepartureRow(next, content.trip)
    } else {
        Countdown(leave)
        Text("partir à ${TimeFormat.format(leave)}", style = style(14, bold = true))
    }
}

@Composable
private fun Countdown(leave: java.time.Instant) {
    val context = LocalContext.current
    AndroidRemoteViews(
        RemoteViews(context.packageName, R.layout.widget_countdown).apply {
            val untilLeave = leave.toEpochMilli() - System.currentTimeMillis()
            setChronometer(R.id.countdown, SystemClock.elapsedRealtime() + untilLeave, null, true)
            setChronometerCountDown(R.id.countdown, true)
        },
    )
}

/** One board line: transport badge, departure -> arrival, and when to leave home. */
@Composable
private fun DepartureRow(departure: Departure, trip: TravelProfile) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = modeBadge(departure),
            style = TextStyle(color = ColorProvider(Paper), fontSize = 13.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
            modifier = GlanceModifier.background(Ink).padding(horizontal = 6.dp, vertical = 2.dp),
        )
        val arrival = departure.arrivalAt?.let { " → ${TimeFormat.format(it)}" } ?: ""
        Text(
            text = "  ${TimeFormat.format(departure.departureAt)}$arrival",
            style = style(16, bold = true),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Text("partir ${TimeFormat.format(trip.leaveAt(departure))}", style = style(14), maxLines = 1)
    }
}

/** "TRAIN", "TRAM L1", "BUS 12": the network line number is shown when it is a short public code. */
internal fun modeBadge(departure: Departure): String {
    val mode = modeLabel(departure.mode).uppercase()
    val line = departure.lineId.takeIf { it.length <= 4 }
    return if (line != null) "$mode $line" else mode
}

@Composable
private fun Message(text: String) {
    Spacer(GlanceModifier.height(8.dp))
    Text(text, style = style(15))
}

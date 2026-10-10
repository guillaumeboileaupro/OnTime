package fr.ontime.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import fr.ontime.app.MainActivity
import fr.ontime.app.OnTimeTheme
import fr.ontime.app.Paper
import fr.ontime.app.data.SharedPreferencesProfileRepository
import fr.ontime.app.data.TripLabels
import fr.ontime.app.shortLabel
import fr.ontime.domain.ProfileSnapshot
import kotlinx.coroutines.launch

/** Lets the user pick the saved trip shown by a newly added (or reconfigured) widget. */
class TripWidgetConfigureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val trips = (SharedPreferencesProfileRepository(this).snapshot() as? ProfileSnapshot.Data)?.profiles.orEmpty()
        val labels = TripLabels(this)

        setContent {
            OnTimeTheme {
                val scope = rememberCoroutineScope()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Paper)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Trajet du widget",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (trips.isEmpty()) {
                        Text("Créez d'abord un trajet dans OnTime.", style = MaterialTheme.typography.bodyLarge)
                        Button(
                            onClick = {
                                startActivity(Intent(this@TripWidgetConfigureActivity, MainActivity::class.java))
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) { Text("Ouvrir OnTime", style = MaterialTheme.typography.labelLarge) }
                    }
                    trips.forEach { trip ->
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val context = this@TripWidgetConfigureActivity
                                    val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
                                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) {
                                        it.toMutablePreferences().apply { this[TripWidget.TRIP_KEY] = trip.id }
                                    }
                                    TripWidget().update(context, glanceId)
                                    setResult(
                                        RESULT_OK,
                                        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                                    )
                                    finish()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text(
                                labels.get(trip.id)?.let(::shortLabel) ?: "Trajet sans nom",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

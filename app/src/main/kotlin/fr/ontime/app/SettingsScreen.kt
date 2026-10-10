package fr.ontime.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import fr.ontime.app.widget.TripWidget
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current.applicationContext
    val settings = remember { DisplaySettings(context) }
    var options by remember { mutableStateOf(settings.read()) }
    val scope = rememberCoroutineScope()

    fun change(updated: DisplayOptions) {
        options = updated
        settings.write(updated)
        scope.launch { TripWidget().updateAll(context) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Accueil et widget", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        SettingSwitch("Afficher le compte à rebours", options.countdown) { change(options.copy(countdown = it)) }
        SettingSwitch("Afficher l'heure de départ de chez soi", options.leaveTime) { change(options.copy(leaveTime = it)) }
        Text("Widget", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        SettingSwitch("Afficher les trains supprimés", options.cancelled) { change(options.copy(cancelled = it)) }
        Text(
            "Le compte à rebours peut aussi être activé ou masqué en touchant ⏱ sur le widget. " +
                "Sans compte à rebours ni heure de départ, l'heure du prochain départ est affichée. " +
                "L'accueil signale toujours les suppressions.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

package fr.ontime.app.reminders

import android.content.Context
import fr.ontime.domain.Departure
import fr.ontime.domain.Mode
import fr.ontime.domain.Quality
import fr.ontime.domain.ReminderWindow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import org.json.JSONArray
import org.json.JSONObject

/** Per-trip reminder windows and the reminder currently planned, kept apart from trip storage. */
class ReminderStore(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun window(tripId: String): ReminderWindow? = runCatching {
        val json = JSONObject(preferences.getString(windowKey(tripId), null) ?: return null)
        val days = json.getJSONArray("days").let { array -> (0 until array.length()).map { DayOfWeek.of(array.getInt(it)) } }
        ReminderWindow(days.toSet(), LocalTime.parse(json.getString("start")), LocalTime.parse(json.getString("end")))
    }.getOrNull()

    fun setWindow(tripId: String, window: ReminderWindow?) {
        val editor = preferences.edit()
        if (window == null) {
            editor.remove(windowKey(tripId))
        } else {
            val json = JSONObject()
                .put("days", JSONArray(window.days.map { it.value }.sorted()))
                .put("start", window.start.toString())
                .put("end", window.end.toString())
            editor.putString(windowKey(tripId), json.toString())
        }
        editor.apply()
    }

    fun planned(tripId: String): Departure? = runCatching {
        val json = JSONObject(preferences.getString(plannedKey(tripId), null) ?: return null)
        Departure(
            provider = json.getString("provider"),
            journeyId = json.getString("journeyId"),
            stopId = json.getString("stopId"),
            lineId = json.getString("lineId"),
            directionId = json.getString("directionId"),
            destination = json.getString("destination"),
            mode = Mode.valueOf(json.getString("mode")),
            departureAt = Instant.ofEpochMilli(json.getLong("departureAt")),
            fetchedAt = Instant.ofEpochMilli(json.getLong("fetchedAt")),
            quality = Quality.valueOf(json.getString("quality")),
            cancelled = false,
            arrivalAt = if (json.has("arrivalAt")) Instant.ofEpochMilli(json.getLong("arrivalAt")) else null,
        )
    }.getOrNull()

    fun setPlanned(tripId: String, departure: Departure?) {
        val editor = preferences.edit()
        if (departure == null) {
            editor.remove(plannedKey(tripId))
        } else {
            val json = JSONObject()
                .put("provider", departure.provider)
                .put("journeyId", departure.journeyId)
                .put("stopId", departure.stopId)
                .put("lineId", departure.lineId)
                .put("directionId", departure.directionId)
                .put("destination", departure.destination)
                .put("mode", departure.mode.name)
                .put("departureAt", departure.departureAt.toEpochMilli())
                .put("fetchedAt", departure.fetchedAt.toEpochMilli())
                .put("quality", departure.quality.name)
            departure.arrivalAt?.let { json.put("arrivalAt", it.toEpochMilli()) }
            editor.putString(plannedKey(tripId), json.toString())
        }
        editor.apply()
    }

    fun forget(tripId: String) {
        preferences.edit().remove(windowKey(tripId)).remove(plannedKey(tripId)).apply()
    }

    private fun windowKey(tripId: String) = "window_$tripId"
    private fun plannedKey(tripId: String) = "planned_$tripId"

    companion object {
        internal const val FILE_NAME = "ontime_reminders"
    }
}

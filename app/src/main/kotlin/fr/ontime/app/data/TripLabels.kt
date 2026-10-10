package fr.ontime.app.data

import android.content.Context

/**
 * Human-readable names of saved trips ("Nice → Menton"), kept apart from the
 * profile payload so the strictly validated profile storage stays unchanged.
 */
class TripLabels(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun get(profileId: String): String? = preferences.getString(profileId, null)

    fun put(profileId: String, label: String) {
        preferences.edit().putString(profileId, label).apply()
    }

    fun remove(profileId: String) {
        preferences.edit().remove(profileId).apply()
    }

    companion object {
        internal const val FILE_NAME = "ontime_trip_labels"
    }
}

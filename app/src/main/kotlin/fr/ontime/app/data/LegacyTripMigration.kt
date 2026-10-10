package fr.ontime.app.data

import android.content.Context
import fr.ontime.app.data.sncf.SncfApi
import fr.ontime.app.data.sncf.SncfResponse
import fr.ontime.app.data.sncf.encodeSegment
import fr.ontime.domain.ProfileChange
import fr.ontime.domain.ProfileDraft
import fr.ontime.domain.ProfileRepository
import org.json.JSONArray
import org.json.JSONObject

/** Former trip storage (line + route direction), read once for migration. */
interface LegacyTripStore {
    fun read(): String?
    fun clear()
}

class SharedPreferencesLegacyTripStore(context: Context) : LegacyTripStore {
    private val preferences = context.getSharedPreferences("ontime_profiles", Context.MODE_PRIVATE)

    override fun read(): String? = preferences.getString("profiles", null)

    override fun clear() {
        preferences.edit().clear().apply()
    }
}

/**
 * Converts trips saved as "stop + line + route" into "stop + destination", the
 * destination being the route terminus returned by the SNCF API. Demo or
 * unreadable entries are dropped. The old storage is cleared only once every
 * real SNCF trip has been converted, so a network failure is retried later.
 */
class LegacyTripMigration(
    private val legacy: LegacyTripStore,
    private val repository: ProfileRepository,
    private val labels: TripLabelStore,
    private val api: SncfApi,
) {
    fun run() {
        val raw = legacy.read() ?: return
        val items = runCatching { JSONArray(raw) }.getOrNull()
        if (items == null) {
            legacy.clear()
            return
        }
        // Resolve every terminus first: a partial failure must not create duplicates on retry.
        val conversions = (0 until items.length()).mapNotNull { items.optJSONObject(it) }
            .filter { it.optString("stopId").startsWith("stop_area:SNCF:") && it.optString("direction").startsWith("route:") }
            .map { item -> item to (terminusOf(item.getString("direction")) ?: return) }
        for ((item, terminus) in conversions) {
            val result = repository.create(
                ProfileDraft(
                    stopId = item.getString("stopId"),
                    destinationId = terminus.first,
                    walkingMinutes = item.optInt("walkingMinutes", -1),
                    marginMinutes = item.optInt("marginMinutes", -1),
                ),
            )
            if (result is ProfileChange.StorageError) return
            if (result is ProfileChange.Success) {
                val origin = labels.get(item.optString("id")).orEmpty().substringBefore(" → ").ifBlank { "Départ" }
                labels.put(result.profile.id, "$origin → ${terminus.second}")
            }
        }
        (0 until items.length()).mapNotNull { items.optJSONObject(it)?.optString("id") }.forEach(labels::remove)
        legacy.clear()
    }

    /** Terminus stop area id and name of [routeId], or null when the API cannot answer now. */
    private fun terminusOf(routeId: String): Pair<String, String>? {
        val response = api.get("/routes/${encodeSegment(routeId)}?disable_geojson=true") as? SncfResponse.Body ?: return null
        return runCatching {
            val direction = JSONObject(response.json).getJSONArray("routes").getJSONObject(0).getJSONObject("direction")
            direction.getString("id") to direction.getString("name")
        }.getOrNull()
    }
}

package fr.ontime.app.data

import android.content.Context
import fr.ontime.domain.ProfileChange
import fr.ontime.domain.ProfileDraft
import fr.ontime.domain.ProfileRepository
import fr.ontime.domain.TravelProfile
import fr.ontime.domain.toProfile
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SharedPreferencesProfileRepository(
    context: Context,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) : ProfileRepository {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    @Synchronized
    override fun profiles(): List<TravelProfile> = readProfiles()

    @Synchronized
    override fun selectedProfileId(): String? {
        val selected = preferences.getString(KEY_SELECTED, null)
        return selected?.takeIf { id -> readProfiles().any { it.id == id } }
    }

    @Synchronized
    override fun create(draft: ProfileDraft): ProfileChange {
        val result = draft.toProfile(idFactory())
        if (result !is ProfileChange.Success) return result
        val updated = readProfiles() + result.profile
        write(updated, selectedProfileId() ?: result.profile.id)
        return result
    }

    @Synchronized
    override fun update(id: String, draft: ProfileDraft): ProfileChange {
        val current = readProfiles()
        if (current.none { it.id == id }) return ProfileChange.NotFound
        val result = draft.toProfile(id)
        if (result !is ProfileChange.Success) return result
        write(current.map { if (it.id == id) result.profile else it }, selectedProfileId())
        return result
    }

    @Synchronized
    override fun delete(id: String): Boolean {
        val current = readProfiles()
        if (current.none { it.id == id }) return false
        val remaining = current.filterNot { it.id == id }
        val selected = selectedProfileId().takeUnless { it == id } ?: remaining.firstOrNull()?.id
        write(remaining, selected)
        return true
    }

    @Synchronized
    override fun select(id: String): Boolean {
        if (readProfiles().none { it.id == id }) return false
        preferences.edit().putString(KEY_SELECTED, id).commit()
        return true
    }

    private fun readProfiles(): List<TravelProfile> = runCatching {
        val array = JSONArray(preferences.getString(KEY_PROFILES, "[]"))
        buildList {
            repeat(array.length()) { index ->
                val item = array.getJSONObject(index)
                add(
                    TravelProfile(
                        id = item.getString("id"),
                        stopId = item.getString("stopId"),
                        lineId = item.getString("lineId"),
                        direction = item.getString("direction"),
                        walkingMinutes = item.getInt("walkingMinutes"),
                        marginMinutes = item.getInt("marginMinutes"),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun write(profiles: List<TravelProfile>, selectedId: String?) {
        val array = JSONArray()
        profiles.forEach { profile ->
            array.put(
                JSONObject()
                    .put("id", profile.id)
                    .put("stopId", profile.stopId)
                    .put("lineId", profile.lineId)
                    .put("direction", profile.direction)
                    .put("walkingMinutes", profile.walkingMinutes)
                    .put("marginMinutes", profile.marginMinutes),
            )
        }
        preferences.edit()
            .putString(KEY_PROFILES, array.toString())
            .apply { if (selectedId == null) remove(KEY_SELECTED) else putString(KEY_SELECTED, selectedId) }
            .commit()
    }

    companion object {
        internal const val FILE_NAME = "ontime_profiles"
        private const val KEY_PROFILES = "profiles"
        private const val KEY_SELECTED = "selected_profile_id"
    }
}

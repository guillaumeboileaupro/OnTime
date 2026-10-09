package fr.ontime.app.data

import android.content.Context
import fr.ontime.domain.ProfileChange
import fr.ontime.domain.ProfileCommandResult
import fr.ontime.domain.ProfileDraft
import fr.ontime.domain.ProfileRepository
import fr.ontime.domain.ProfileSnapshot
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
    override fun snapshot(): ProfileSnapshot {
        val profiles = readProfiles() ?: return ProfileSnapshot.StorageError
        return ProfileSnapshot.Data(profiles, selectedProfileId(profiles))
    }

    @Synchronized
    override fun create(draft: ProfileDraft): ProfileChange {
        val result = draft.toProfile(idFactory())
        if (result !is ProfileChange.Success) return result
        val current = readProfiles() ?: return ProfileChange.StorageError
        val updated = current + result.profile
        return if (write(updated, selectedProfileId(current) ?: result.profile.id)) {
            result
        } else {
            ProfileChange.StorageError
        }
    }

    @Synchronized
    override fun update(id: String, draft: ProfileDraft): ProfileChange {
        val current = readProfiles() ?: return ProfileChange.StorageError
        if (current.none { it.id == id }) return ProfileChange.NotFound
        val result = draft.toProfile(id)
        if (result !is ProfileChange.Success) return result
        return if (write(current.map { if (it.id == id) result.profile else it }, selectedProfileId(current))) {
            result
        } else {
            ProfileChange.StorageError
        }
    }

    @Synchronized
    override fun delete(id: String): ProfileCommandResult {
        val current = readProfiles() ?: return ProfileCommandResult.StorageError
        if (current.none { it.id == id }) return ProfileCommandResult.NotFound
        val remaining = current.filterNot { it.id == id }
        val selected = selectedProfileId(current).takeUnless { it == id } ?: remaining.firstOrNull()?.id
        return if (write(remaining, selected)) {
            ProfileCommandResult.Success
        } else {
            ProfileCommandResult.StorageError
        }
    }

    @Synchronized
    override fun select(id: String): ProfileCommandResult {
        val current = readProfiles() ?: return ProfileCommandResult.StorageError
        if (current.none { it.id == id }) return ProfileCommandResult.NotFound
        return if (preferences.edit().putString(KEY_SELECTED, id).commit()) {
            ProfileCommandResult.Success
        } else {
            ProfileCommandResult.StorageError
        }
    }

    private fun selectedProfileId(profiles: List<TravelProfile>): String? {
        val selected = preferences.getString(KEY_SELECTED, null)
        return selected?.takeIf { id -> profiles.any { it.id == id } }
    }

    private fun readProfiles(): List<TravelProfile>? = runCatching {
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
    }.getOrNull()

    private fun write(profiles: List<TravelProfile>, selectedId: String?): Boolean {
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
        return preferences.edit()
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

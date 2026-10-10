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
    override fun snapshot(): ProfileSnapshot = readData() ?: ProfileSnapshot.StorageError

    @Synchronized
    override fun create(draft: ProfileDraft): ProfileChange {
        val result = draft.toProfile(idFactory())
        if (result !is ProfileChange.Success) return result
        val current = readData() ?: return ProfileChange.StorageError
        if (current.profiles.any { it.id == result.profile.id }) return ProfileChange.StorageError
        val updated = current.profiles + result.profile
        return if (write(updated, current.selectedProfileId ?: result.profile.id)) {
            result
        } else {
            ProfileChange.StorageError
        }
    }

    @Synchronized
    override fun update(id: String, draft: ProfileDraft): ProfileChange {
        val current = readData() ?: return ProfileChange.StorageError
        if (current.profiles.none { it.id == id }) return ProfileChange.NotFound
        val result = draft.toProfile(id)
        if (result !is ProfileChange.Success) return result
        return if (write(current.profiles.map { if (it.id == id) result.profile else it }, current.selectedProfileId)) {
            result
        } else {
            ProfileChange.StorageError
        }
    }

    @Synchronized
    override fun delete(id: String): ProfileCommandResult {
        val current = readData() ?: return ProfileCommandResult.StorageError
        if (current.profiles.none { it.id == id }) return ProfileCommandResult.NotFound
        val remaining = current.profiles.filterNot { it.id == id }
        val selected = current.selectedProfileId.takeUnless { it == id } ?: remaining.firstOrNull()?.id
        return if (write(remaining, selected)) {
            ProfileCommandResult.Success
        } else {
            ProfileCommandResult.StorageError
        }
    }

    @Synchronized
    override fun select(id: String): ProfileCommandResult {
        val current = readData() ?: return ProfileCommandResult.StorageError
        if (current.profiles.none { it.id == id }) return ProfileCommandResult.NotFound
        return if (preferences.edit().putString(KEY_SELECTED, id).commit()) {
            ProfileCommandResult.Success
        } else {
            ProfileCommandResult.StorageError
        }
    }

    private fun readData(): ProfileSnapshot.Data? = runCatching {
        val array = JSONArray(preferences.getString(KEY_PROFILES, "[]"))
        val profiles = buildList {
            repeat(array.length()) { index ->
                val item = array.getJSONObject(index)
                val result = ProfileDraft(
                        stopId = item.exactString("stopId"),
                        lineId = item.exactString("lineId"),
                        direction = item.exactString("direction"),
                        walkingMinutes = item.exactInt("walkingMinutes"),
                        marginMinutes = item.exactInt("marginMinutes"),
                    ).toProfile(item.exactString("id"))
                require(result is ProfileChange.Success)
                add(result.profile)
            }
        }
        val selected = preferences.getString(KEY_SELECTED, null)
        require(selected == null || profiles.any { it.id == selected })
        require(profiles.map { it.id }.distinct().size == profiles.size)
        ProfileSnapshot.Data(profiles, selected)
    }.getOrNull()

    private fun JSONObject.exactInt(key: String): Int {
        val value = get(key)
        require(value is Int)
        return value
    }

    private fun JSONObject.exactString(key: String): String {
        val value = get(key)
        require(value is String)
        return value
    }

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
        internal const val KEY_SELECTED = "selected_profile_id"
    }
}

package fr.ontime.domain

import java.time.Clock
import java.time.Duration
import java.time.Instant

data class TravelProfile(
    val id: String,
    val stopId: String,
    val lineId: String,
    val direction: String,
    val walkingMinutes: Int,
    val marginMinutes: Int,
)

data class ProfileDraft(
    val stopId: String,
    val lineId: String,
    val direction: String,
    val walkingMinutes: Int,
    val marginMinutes: Int,
)

enum class ProfileError { InvalidId, EmptyStop, EmptyLine, EmptyDirection, InvalidWalking, InvalidMargin }

sealed interface ProfileChange {
    data class Success(val profile: TravelProfile) : ProfileChange
    data class Invalid(val errors: Set<ProfileError>) : ProfileChange
    data object NotFound : ProfileChange
    data object StorageError : ProfileChange
}

enum class ProfileCommandResult { Success, NotFound, StorageError }

sealed interface ProfileSnapshot {
    data class Data(val profiles: List<TravelProfile>, val selectedProfileId: String?) : ProfileSnapshot
    data object StorageError : ProfileSnapshot
}

interface ProfileRepository {
    fun snapshot(): ProfileSnapshot
    fun create(draft: ProfileDraft): ProfileChange
    fun update(id: String, draft: ProfileDraft): ProfileChange
    fun delete(id: String): ProfileCommandResult
    fun select(id: String): ProfileCommandResult
}

fun ProfileDraft.validationErrors(): Set<ProfileError> = buildSet {
    if (stopId.isBlank()) add(ProfileError.EmptyStop)
    if (lineId.isBlank()) add(ProfileError.EmptyLine)
    if (direction.isBlank()) add(ProfileError.EmptyDirection)
    if (walkingMinutes !in 0..MAX_WALKING_MINUTES) add(ProfileError.InvalidWalking)
    if (marginMinutes !in 0..60) add(ProfileError.InvalidMargin)
}

fun ProfileDraft.toProfile(id: String): ProfileChange {
    val errors = validationErrors().toMutableSet()
    if (id.isBlank()) errors += ProfileError.InvalidId
    if (errors.isNotEmpty()) return ProfileChange.Invalid(errors)
    return ProfileChange.Success(
        TravelProfile(
            id = id,
            stopId = stopId.trim(),
            lineId = lineId.trim(),
            direction = direction.trim(),
            walkingMinutes = walkingMinutes,
            marginMinutes = marginMinutes,
        ),
    )
}

data class HomeDeparture(
    val leaveAt: Instant,
    val timeUntilLeave: Duration,
)

class HomeDepartureCalculator(private val clock: Clock) {
    fun calculate(departureAt: Instant, profile: TravelProfile): HomeDeparture {
        val leaveAt = departureAt
            .minus(Duration.ofMinutes(profile.walkingMinutes.toLong()))
            .minus(Duration.ofMinutes(profile.marginMinutes.toLong()))
        return HomeDeparture(leaveAt, Duration.between(clock.instant(), leaveAt))
    }
}

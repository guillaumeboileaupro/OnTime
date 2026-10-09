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
}

interface ProfileRepository {
    fun profiles(): List<TravelProfile>
    fun selectedProfileId(): String?
    fun create(draft: ProfileDraft): ProfileChange
    fun update(id: String, draft: ProfileDraft): ProfileChange
    fun delete(id: String): Boolean
    fun select(id: String): Boolean
}

fun ProfileDraft.validationErrors(): Set<ProfileError> = buildSet {
    if (stopId.isBlank()) add(ProfileError.EmptyStop)
    if (lineId.isBlank()) add(ProfileError.EmptyLine)
    if (direction.isBlank()) add(ProfileError.EmptyDirection)
    if (walkingMinutes !in 0..180) add(ProfileError.InvalidWalking)
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

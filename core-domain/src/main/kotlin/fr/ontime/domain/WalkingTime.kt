package fr.ontime.domain

const val MAX_WALKING_MINUTES = 180

/** Rounds a provider walking duration up to whole minutes; null if unusable for a profile. */
fun walkingMinutesFrom(seconds: Int): Int? {
    if (seconds < 0) return null
    val minutes = (seconds + 59) / 60
    return minutes.takeIf { it <= MAX_WALKING_MINUTES }
}

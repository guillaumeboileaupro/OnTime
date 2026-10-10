package fr.ontime.app.data

import android.content.Context
import fr.ontime.app.data.lda.AzurStops
import fr.ontime.app.data.lda.LignesAzurSource
import fr.ontime.app.data.sncf.SncfServices
import fr.ontime.app.data.sncf.TripUpdate
import fr.ontime.app.data.sncf.buildTripUpdate
import fr.ontime.domain.TravelProfile
import java.time.Clock

/** Routes a trip to its network: Lignes d'Azur stops are prefixed `lda:`, the rest is SNCF. */
object Transport {
    @Volatile
    private var azur: LignesAzurSource? = null

    fun lignesAzur(context: Context): LignesAzurSource = azur ?: synchronized(this) {
        azur ?: LignesAzurSource(context.applicationContext.filesDir, Clock.systemUTC()).also { azur = it }
    }

    fun isLignesAzur(trip: TravelProfile) = trip.stopId.startsWith(AzurStops.PREFIX)

    /** Blocking refresh of [trip]; null only when an SNCF trip lacks the SNCF key. */
    fun tripUpdate(context: Context, trip: TravelProfile, limit: Int = 3): TripUpdate? =
        if (isLignesAzur(trip)) {
            val snapshot = lignesAzur(context).fetchTrip(trip.stopId, trip.destinationId)
            buildTripUpdate(trip, snapshot.status, snapshot.departures, Clock.systemUTC().instant(), limit)
        } else {
            SncfServices.shared()?.tripUpdate(trip, limit)
        }
}

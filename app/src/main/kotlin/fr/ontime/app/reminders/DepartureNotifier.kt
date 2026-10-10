package fr.ontime.app.reminders

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import fr.ontime.app.MainActivity
import fr.ontime.app.R
import fr.ontime.app.TimeFormat
import fr.ontime.domain.Departure
import fr.ontime.domain.TravelProfile
import fr.ontime.domain.leaveAt

/** Posts departure warnings on a dedicated, high-importance channel. */
class DepartureNotifier(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun canNotify(): Boolean = manager.areNotificationsEnabled()

    fun notifyDeparture(trip: TravelProfile, label: String, departure: Departure, confirmed: Boolean, replacement: Boolean) {
        val leave = TimeFormat.format(trip.leaveAt(departure))
        val arrival = departure.arrivalAt?.let { ", arrivée ${TimeFormat.format(it)}" } ?: ""
        val title = if (replacement) "Train remplacé : partez à $leave" else "Partez à $leave"
        val text = buildString {
            append("$label · train ${TimeFormat.format(departure.departureAt)}$arrival")
            if (replacement) append(". Le train prévu n'est plus annoncé.")
            if (!confirmed) append(" (horaire non confirmé, connexion indisponible)")
        }
        post(trip.id, title, text)
    }

    fun notifyNoTrain(trip: TravelProfile, label: String) {
        post(trip.id, "Aucun train à prendre", "$label : le train prévu n'est plus annoncé et aucun autre train direct n'est proposé.")
    }

    private fun post(tripId: String, title: String, text: String) {
        if (!canNotify()) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Départs", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Moment de quitter la maison pour vos trajets"
            },
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        // One notification per trip: a newer warning replaces the previous one.
        manager.notify(tripId.hashCode(), notification)
    }

    private companion object {
        const val CHANNEL_ID = "departures"
    }
}

package fr.ontime.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper

/**
 * One-shot, foreground position for the nearest-station lookup. The position is
 * handed to [locate]'s callback only; it is never stored or logged. Callers must
 * hold a location permission.
 */
class DeviceLocator(context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission")
    fun locate(onResult: (Location?) -> Unit) {
        val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstOrNull { manager?.isProviderEnabled(it) == true }
        if (manager == null || provider == null) return onResult(null)

        var done = false
        val cancel = CancellationSignal()
        lateinit var listener: LocationListener
        fun finish(location: Location?) {
            if (done) return
            done = true
            handler.removeCallbacksAndMessages(null)
            cancel.cancel()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) manager.removeUpdates(listener)
            onResult(location)
        }
        listener = LocationListener { finish(it) }
        handler.postDelayed({ finish(null) }, TIMEOUT_MS)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.getCurrentLocation(provider, cancel, { handler.post(it) }) { finish(it) }
        } else {
            @Suppress("DEPRECATION")
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }
    }

    private companion object {
        const val TIMEOUT_MS = 30_000L
    }
}

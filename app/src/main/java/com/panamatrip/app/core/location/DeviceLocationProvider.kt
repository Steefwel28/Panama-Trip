package com.panamatrip.app.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.panamatrip.app.domain.geo.GeoCalculator
import com.panamatrip.app.domain.model.GeoPoint
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Última ubicación conocida del dispositivo. Nunca pide permisos: si la app no
 * los tiene concedidos devuelve `null` y la pantalla usa su respaldo offline.
 */
class DeviceLocationProvider(private val context: Context) {

    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun lastKnownLocation(): GeoPoint? {
        if (!hasPermission()) return null
        return suspendCancellableCoroutine { continuation ->
            runCatching { client.lastLocation }
                .onSuccess { task ->
                    task.addOnSuccessListener { location ->
                        val point = location?.let { GeoPoint(it.latitude, it.longitude) }
                        if (continuation.isActive) {
                            continuation.resume(
                                point?.takeIf(GeoCalculator::isPlausiblePanamaPoint),
                            )
                        }
                    }.addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
                .onFailure {
                    if (continuation.isActive) continuation.resume(null)
                }
        }
    }
}

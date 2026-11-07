package com.example.calculator

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val accuracyMeters: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val provider: String = "GPS"
)

class LocationHelper(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    /**
     * Fetches current location using FusedLocationProviderClient with LocationManager fallback.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): DeviceLocation? = suspendCancellableCoroutine { cont ->
        try {
            // First try current high-accuracy location via Fused Location
            val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()

            fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    if (cont.isActive) {
                        cont.resume(
                            DeviceLocation(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                altitude = loc.altitude,
                                accuracyMeters = loc.accuracy,
                                timestamp = loc.time,
                                provider = loc.provider ?: "Fused"
                            )
                        )
                    }
                } else {
                    // Fallback to getLastLocation
                    fusedClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) {
                            if (cont.isActive) {
                                cont.resume(
                                    DeviceLocation(
                                        latitude = lastLoc.latitude,
                                        longitude = lastLoc.longitude,
                                        altitude = lastLoc.altitude,
                                        accuracyMeters = lastLoc.accuracy,
                                        timestamp = lastLoc.time,
                                        provider = lastLoc.provider ?: "LastLocation"
                                    )
                                )
                            }
                        } else {
                            // Try native LocationManager
                            val fallback = getNativeLastLocation()
                            if (cont.isActive) cont.resume(fallback)
                        }
                    }.addOnFailureListener {
                        val fallback = getNativeLastLocation()
                        if (cont.isActive) cont.resume(fallback)
                    }
                }
            }.addOnFailureListener {
                val fallback = getNativeLastLocation()
                if (cont.isActive) cont.resume(fallback)
            }

            cont.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        } catch (e: Exception) {
            val fallback = getNativeLastLocation()
            if (cont.isActive) cont.resume(fallback)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getNativeLastLocation(): DeviceLocation? {
        val lm = locationManager ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        var bestLoc: Location? = null
        for (provider in providers) {
            try {
                val loc = lm.getLastKnownLocation(provider) ?: continue
                if (bestLoc == null || loc.accuracy < bestLoc.accuracy) {
                    bestLoc = loc
                }
            } catch (_: SecurityException) {
            }
        }
        return bestLoc?.let {
            DeviceLocation(
                latitude = it.latitude,
                longitude = it.longitude,
                altitude = it.altitude,
                accuracyMeters = it.accuracy,
                timestamp = it.time,
                provider = it.provider ?: "Native"
            )
        }
    }

    /**
     * Flow of continuous live location updates for tracking.
     */
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMs: Long = 3000): Flow<DeviceLocation> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setMinUpdateDistanceMeters(1f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    trySend(
                        DeviceLocation(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            altitude = loc.altitude,
                            accuracyMeters = loc.accuracy,
                            timestamp = loc.time,
                            provider = loc.provider ?: "Fused"
                        )
                    )
                }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }
}

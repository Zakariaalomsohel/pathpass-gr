package com.example.calculator

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * State representing live real-time location tracking telemetry.
 */
data class RealtimeTrackingState(
    val isTracking: Boolean = false,
    val latitude: Double = 24.582254,
    val longitude: Double = 90.141237,
    val altitudeMeters: Double = 0.0,
    val speedKmh: Float = 0f,
    val headingDegrees: Float = 0f,
    val accuracyMeters: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val breadcrumbs: List<Pair<Double, Double>> = emptyList(),
    val totalDistanceMeters: Double = 0.0,
    val liveResult: MilitaryGridResult? = null,
    val statusMessage: String = "Tracking Inactive"
)

/**
 * Real-time Location Manager using FusedLocationProviderClient to track
 * user coordinates continuously and stream real-time updates.
 */
class RealtimeLocationManager private constructor(context: Context) {

    private val applicationContext = context.applicationContext
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(applicationContext)

    private val _trackingState = MutableStateFlow(
        RealtimeTrackingState(
            liveResult = MilitaryGridCalculator.calculate(24.582254, 90.141237)
        )
    )
    val trackingState: StateFlow<RealtimeTrackingState> = _trackingState.asStateFlow()

    private var locationCallback: LocationCallback? = null

    /**
     * Start continuous real-time high-accuracy GPS tracking.
     */
    @SuppressLint("MissingPermission")
    fun startRealtimeTracking(intervalMillis: Long = 1500) {
        if (_trackingState.value.isTracking) return

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .setMinUpdateDistanceMeters(0.5f)
            .setWaitForAccurateLocation(false)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc: Location = result.lastLocation ?: return
                onNewLocationReceived(loc)
            }
        }

        fusedClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )

        _trackingState.update {
            it.copy(
                isTracking = true,
                statusMessage = "Real-time Tracking Active"
            )
        }
    }

    /**
     * Stop real-time GPS tracking.
     */
    fun stopRealtimeTracking() {
        locationCallback?.let {
            fusedClient.removeLocationUpdates(it)
        }
        locationCallback = null

        _trackingState.update {
            it.copy(
                isTracking = false,
                statusMessage = "Tracking Paused"
            )
        }
    }

    /**
     * Fetch one-shot current location and update state.
     */
    @SuppressLint("MissingPermission")
    fun requestSingleUpdate(onComplete: ((Boolean) -> Unit)? = null) {
        val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()
        fusedClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { loc: Location? ->
            if (loc != null) {
                onNewLocationReceived(loc)
                onComplete?.invoke(true)
            } else {
                fusedClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                    if (lastLoc != null) {
                        onNewLocationReceived(lastLoc)
                        onComplete?.invoke(true)
                    } else {
                        onComplete?.invoke(false)
                    }
                }.addOnFailureListener {
                    onComplete?.invoke(false)
                }
            }
        }.addOnFailureListener {
            onComplete?.invoke(false)
        }
    }

    /**
     * Process new location sample and update breadcrumbs, speed, and military grid.
     */
    private fun onNewLocationReceived(loc: Location) {
        val lat = loc.latitude
        val lon = loc.longitude
        val alt = loc.altitude
        val speed = if (loc.hasSpeed()) (loc.speed * 3.6f) else 0f
        val heading = if (loc.hasBearing()) loc.bearing else _trackingState.value.headingDegrees
        val accuracy = loc.accuracy

        val currentCrumbs = _trackingState.value.breadcrumbs
        val lastPoint = currentCrumbs.lastOrNull()

        var incrementalDist = 0.0
        val updatedCrumbs = if (lastPoint != null) {
            incrementalDist = GeodeticCalculator.calculateVincentyDistance(
                lastPoint.first, lastPoint.second, lat, lon
            )
            // Add point if moved more than 2 meters
            if (incrementalDist >= 2.0) {
                (currentCrumbs + Pair(lat, lon)).takeLast(250) // keep last 250 points
            } else {
                currentCrumbs
            }
        } else {
            listOf(Pair(lat, lon))
        }

        // Calculate Military GR and Map Sheet in real-time
        val gridResult = MilitaryGridCalculator.calculate(lat, lon)

        _trackingState.update {
            it.copy(
                latitude = lat,
                longitude = lon,
                altitudeMeters = alt,
                speedKmh = speed,
                headingDegrees = heading,
                accuracyMeters = accuracy,
                timestamp = loc.time,
                breadcrumbs = updatedCrumbs,
                totalDistanceMeters = it.totalDistanceMeters + incrementalDist,
                liveResult = gridResult,
                statusMessage = "GPS Lock (±${String.format(java.util.Locale.US, "%.1f", accuracy)}m)"
            )
        }
    }

    /**
     * Clear breadcrumb trail history.
     */
    fun clearTrail() {
        _trackingState.update {
            it.copy(
                breadcrumbs = emptyList(),
                totalDistanceMeters = 0.0
            )
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: RealtimeLocationManager? = null

        fun getInstance(context: Context): RealtimeLocationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RealtimeLocationManager(context).also { INSTANCE = it }
            }
        }
    }
}

package com.example.data.model

import java.text.DecimalFormat
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object NavigationCalculator {

    private const val EARTH_RADIUS_METERS = 6371008.8 // IUGG mean earth radius

    fun calculate(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
        p1Elevation: Double? = null,
        p2Elevation: Double? = null,
        p1Label: String = "Site A [Terminal 1]",
        p2Label: String = "Site B [Terminal 2]"
    ): CalculationResult {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        // Haversine geodesic distance
        val a = sin(deltaPhi / 2.0) * sin(deltaPhi / 2.0) +
                cos(phi1) * cos(phi2) * sin(deltaLambda / 2.0) * sin(deltaLambda / 2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        val distanceMeters = EARTH_RADIUS_METERS * c

        // Forward Bearing (Initial Azimuth)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing % 360.0 + 360.0) % 360.0

        // Back Bearing (Initial Azimuth from Point 2 back to Point 1)
        val yBack = sin(-deltaLambda) * cos(phi1)
        val xBack = cos(phi2) * sin(phi1) - sin(phi2) * cos(phi1) * cos(-deltaLambda)
        var backBearing = Math.toDegrees(atan2(yBack, xBack))
        backBearing = (backBearing % 360.0 + 360.0) % 360.0

        val elevDelta = if (p1Elevation != null && p2Elevation != null) {
            p2Elevation - p1Elevation
        } else null

        return CalculationResult(
            distanceMeters = distanceMeters,
            bearingDegrees = bearing,
            backBearingDegrees = backBearing,
            point1 = GeoPoint(lat1, lon1, label = p1Label, altitude = p1Elevation),
            point2 = GeoPoint(lat2, lon2, label = p2Label, altitude = p2Elevation),
            elevationDeltaMeters = elevDelta
        )
    }

    /**
     * Calculates slope / grade percentage and angle
     */
    fun calculateSlope(distanceMeters: Double, elevDeltaMeters: Double): Pair<Double, Double> {
        if (distanceMeters <= 0.0) return Pair(0.0, 0.0)
        val gradePercent = (elevDeltaMeters / distanceMeters) * 100.0
        val angleRad = atan(elevDeltaMeters / distanceMeters)
        val angleDeg = Math.toDegrees(angleRad)
        return Pair(gradePercent, angleDeg)
    }

    fun formatElevationMetersAndFeet(meters: Double?): String {
        if (meters == null) return "সমুদ্রপৃষ্ঠ থেকে উচ্চতা যাচাই হচ্ছে..."
        val feet = meters * 3.28084
        val df = DecimalFormat("#,##0.0")
        return "${df.format(meters)} মি (${df.format(feet)} ফুট)"
    }
}

package com.example.calculator

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geodetic Calculator implementing distance and compass bearing calculations.
 * Supports:
 * 1. Haversine formula (fast and robust).
 * 2. Vincenty's inverse formula on WGS84 ellipsoid (geodetic accuracy in millimeters).
 * 3. Initial compass bearing / azimuth (0° to 360°).
 * 4. Back azimuth.
 */
object GeodeticCalculator {

    private const val EARTH_RADIUS_METERS = 6371000.0

    // WGS84 ellipsoid constants
    private const val A = 6378137.0
    private const val B = 6356752.314245
    private const val F = 1.0 / 298.257223563

    /**
     * Calculates great-circle distance between two points in meters using Haversine formula.
     */
    fun calculateHaversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val sinDphi = sin(deltaPhi / 2.0)
        val sinDlam = sin(deltaLambda / 2.0)

        val a = sinDphi * sinDphi + cos(phi1) * cos(phi2) * sinDlam * sinDlam
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * Calculates geodetic distance in meters using Vincenty's inverse method on WGS84.
     * Falls back to Haversine if points are antipodal or nearly coincident.
     */
    fun calculateVincentyDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        if (abs(lat1 - lat2) < 1e-9 && abs(lon1 - lon2) < 1e-9) return 0.0

        val u1 = atan2((1.0 - F) * sin(Math.toRadians(lat1)), cos(Math.toRadians(lat1)))
        val u2 = atan2((1.0 - F) * sin(Math.toRadians(lat2)), cos(Math.toRadians(lat2)))
        val l = Math.toRadians(lon2 - lon1)

        val sinU1 = sin(u1)
        val cosU1 = cos(u1)
        val sinU2 = sin(u2)
        val cosU2 = cos(u2)

        var lambda = l
        var lambdaPrev: Double
        var iterLimit = 100
        var cosSqAlpha: Double
        var sinSigma: Double
        var cos2SigmaM: Double
        var cosSigma: Double
        var sigma: Double

        do {
            val sinLambda = sin(lambda)
            val cosLambda = cos(lambda)
            val term1 = cosU2 * sinLambda
            val term2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda
            sinSigma = sqrt(term1 * term1 + term2 * term2)
            if (sinSigma == 0.0) return 0.0 // coincident

            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda
            sigma = atan2(sinSigma, cosSigma)
            val sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma
            cosSqAlpha = 1.0 - sinAlpha * sinAlpha
            cos2SigmaM = if (cosSqAlpha != 0.0) {
                cosSigma - 2.0 * sinU1 * sinU2 / cosSqAlpha
            } else 0.0

            val c = F / 16.0 * cosSqAlpha * (4.0 + F * (4.0 - 3.0 * cosSqAlpha))
            lambdaPrev = lambda
            lambda = l + (1.0 - c) * F * sinAlpha * (
                sigma + c * sinSigma * (cos2SigmaM + c * cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM))
            )
        } while (abs(lambda - lambdaPrev) > 1e-12 && --iterLimit > 0)

        if (iterLimit == 0) {
            // Did not converge, fallback to Haversine
            return calculateHaversineDistance(lat1, lon1, lat2, lon2)
        }

        val uSq = cosSqAlpha * (A * A - B * B) / (B * B)
        val aConst = 1.0 + uSq / 16384.0 * (4096.0 + uSq * (-768.0 + uSq * (320.0 - 175.0 * uSq)))
        val bConst = uSq / 1024.0 * (256.0 + uSq * (-128.0 + uSq * (74.0 - 47.0 * uSq)))
        val deltaSigma = bConst * sinSigma * (
            cos2SigmaM + bConst / 4.0 * (
                cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM) -
                    bConst / 6.0 * cos2SigmaM * (-3.0 + 4.0 * sinSigma * sinSigma) * (-3.0 + 4.0 * cos2SigmaM * cos2SigmaM)
            )
        )

        return B * aConst * (sigma - deltaSigma)
    }

    /**
     * Calculates the forward compass bearing (azimuth) from Point 1 to Point 2 in degrees (0° to 360°).
     */
    fun calculateBearing(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)

        val initialBearing = atan2(y, x)
        val degrees = Math.toDegrees(initialBearing)
        return (degrees + 360.0) % 360.0
    }

    /**
     * Calculates back azimuth (opposite direction, +180° mod 360°).
     */
    fun calculateBackAzimuth(bearing: Double): Double {
        return (bearing + 180.0) % 360.0
    }

    /**
     * Formats distance nicely in meters or kilometers.
     */
    fun formatDistance(meters: Double): String {
        return if (meters >= 1000.0) {
            String.format(java.util.Locale.US, "%.2f km", meters / 1000.0)
        } else {
            String.format(java.util.Locale.US, "%.1f m", meters)
        }
    }
}

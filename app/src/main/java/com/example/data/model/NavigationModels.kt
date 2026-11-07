package com.example.data.model

import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.roundToInt

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val label: String = "",
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toFormattedCoordinates(): String {
        val latDir = if (latitude >= 0) "N" else "S"
        val lonDir = if (longitude >= 0) "E" else "W"
        val df = DecimalFormat("#.000000")
        return "${df.format(kotlin.math.abs(latitude))}°$latDir, ${df.format(kotlin.math.abs(longitude))}°$lonDir"
    }

    fun toDecimalString(): Pair<String, String> {
        val df = DecimalFormat("#.000000")
        return Pair(df.format(latitude), df.format(longitude))
    }
}

enum class DistanceUnit(val displayName: String, val toMetersRatio: Double, val suffix: String) {
    KILOMETERS("Kilometers", 1000.0, "KM"),
    METERS("Meters", 1.0, "m"),
    MILES("Miles", 1609.344, "mi"),
    NAUTICAL_MILES("Nautical Miles", 1852.0, "NM");

    fun convertFromMeters(meters: Double): Double = meters / toMetersRatio
}

data class CalculationResult(
    val distanceMeters: Double,
    val bearingDegrees: Double,
    val backBearingDegrees: Double,
    val point1: GeoPoint,
    val point2: GeoPoint,
    val elevationDeltaMeters: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val degreeSymbol = '°'

    fun formatDistance(unit: DistanceUnit): String {
        val converted = unit.convertFromMeters(distanceMeters)
        val df = DecimalFormat("#,##0.00")
        return "${df.format(converted)} ${unit.suffix}"
    }

    val bearingRounded: Int
        get() = (bearingDegrees.roundToInt() % 360 + 360) % 360

    val backBearingRounded: Int
        get() = (backBearingDegrees.roundToInt() % 360 + 360) % 360

    val bearingCardinal: String
        get() = getCardinalDirection(bearingDegrees)

    val backBearingCardinal: String
        get() = getCardinalDirection(backBearingDegrees)

    // NATO Mils (6400 mils in 360 degrees)
    val bearingMils: Int
        get() = ((bearingDegrees / 360.0) * 6400).roundToInt()

    val backBearingMils: Int
        get() = ((backBearingDegrees / 360.0) * 6400).roundToInt()

    fun toOriginalSummary(): String {
        val df = DecimalFormat("##.00")
        val distanceKm = distanceMeters / 1000.0
        return """
            Distance : ${df.format(distanceKm)} KM
            Bearing : $bearingRounded$degreeSymbol
            Back Bearing : $backBearingRounded$degreeSymbol
        """.trimIndent()
    }

    fun toSmsBody(): String {
        val df = DecimalFormat("##.00")
        val distanceKm = distanceMeters / 1000.0
        val p1Name = point1.label.ifBlank { "Point 1" }
        val p2Name = point2.label.ifBlank { "Point 2" }
        return "$p1Name: ${point1.latitude}, ${point1.longitude}\n" +
                "$p2Name: ${point2.latitude}, ${point2.longitude}\n" +
                "Distance: ${df.format(distanceKm)} KM | Bearing: $bearingRounded° ($bearingCardinal)"
    }

    fun toDetailedReport(): String {
        val df = DecimalFormat("##.00")
        val distanceKm = distanceMeters / 1000.0
        val distanceNm = distanceMeters / 1852.0
        val distanceMi = distanceMeters / 1609.344
        val p1Name = point1.label.ifBlank { "Terminal 1 (Site A)" }
        val p2Name = point2.label.ifBlank { "Terminal 2 (Site B)" }
        return """
            === PATHPASS & GR (ZAS) NAVIGATION REPORT ===
            $p1Name: ${point1.latitude}, ${point1.longitude}
            $p2Name: ${point2.latitude}, ${point2.longitude}
            -----------------------------------------
            Distance: ${df.format(distanceKm)} KM (${df.format(distanceNm)} NM / ${df.format(distanceMi)} mi)
            Forward Bearing / Azimuth: $bearingRounded° ($bearingCardinal) [${bearingMils} Mils]
            Back Bearing / Reciprocal: $backBearingRounded° ($backBearingCardinal) [${backBearingMils} Mils]
            -----------------------------------------
            Author Credit: 1618101 Cpl (Tech) Md Zakaria Alom, 3 Signal Battalion
        """.trimIndent()
    }

    companion object {
        fun getCardinalDirection(degrees: Double): String {
            val normalized = (degrees % 360 + 360) % 360
            val directions = arrayOf(
                "N", "NNE", "NE", "ENE",
                "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW",
                "W", "WNW", "NW", "NNW"
            )
            val index = ((normalized + 11.25) / 22.5).toInt() % 16
            return directions[index]
        }
    }
}

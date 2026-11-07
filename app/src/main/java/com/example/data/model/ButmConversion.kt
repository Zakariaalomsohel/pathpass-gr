package com.example.data.model

import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Bangladesh UTM (BUTM) Grid ↔ Lat/Long Converter
 * Based on Everest 1830 Datum
 *
 * Original logic from GR Converter by Capt Tahsin Abdullah
 * Developed By : 1618101 Cpl (Tech) Md Zakaria Alom, 3 Signal Battalion
 */
object ButmConversion {

    // ---- Constants (Everest 1830 + BUTM parameters) ----
    private const val FE_U = 500000.0 // False Easting (meters)
    private const val FN_U = 0.0      // False Northing (meters)

    private const val ae = 6377276.345          // Semi-major axis (meters) - Everest 1830
    private const val fe = 0.003324449296662885 // Flattening (1 / 300.8017)
    private const val be = ae * (1.0 - fe)
    private const val pi = 3.1415926535898
    private const val cdr = pi / 180.0
    private const val crd = 180.0 / pi
    private const val e2e = fe * (2.0 - fe)
    private const val ko_U = 0.9996             // Scale factor
    private const val lnod_U = 90               // Central Meridian: 90° E
    private const val lnor_U = lnod_U * cdr
    private const val ltod_U = 0                // Latitude of Origin: 0° (Equator)
    private const val ltor_U = ltod_U * cdr
    private const val ne = fe / (2.0 - fe)
    private val re = (ae * ((ne.pow(2.0) / 4.0) + 1.0)) / (ne + 1.0)
    private val se2e = e2e / (1.0 - fe).pow(2.0)

    // Data structures for results
    data class ButmResult(
        val easting: Double,
        val northing: Double,
        val mapSheetNo: String,
        val degreeSheet: String,
        val militaryGr6: String,
        val militaryGr8: String,
        val militaryGr10: String,
        val latDms: String,
        val lonDms: String
    ) {
        val eastingFormatted: String get() = "%.2f".format(Locale.US, easting)
        val northingFormatted: String get() = "%.2f".format(Locale.US, northing)
    }

    data class LatLonResult(
        val latitude: Double,
        val longitude: Double,
        val latDms: String,
        val lonDms: String,
        val mapSheetNo: String,
        val degreeSheet: String,
        val militaryGr6: String,
        val militaryGr8: String,
        val militaryGr10: String
    ) {
        val latFormatted: String get() = "%.5f".format(Locale.US, latitude)
        val lonFormatted: String get() = "%.5f".format(Locale.US, longitude)
    }

    data class MapSheetInfo(
        val sheetNo: String,
        val millionSheet: Int,
        val degreeLetter: Char,
        val subNumber: Int?,
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double
    ) {
        val degreeSheet: String get() = "$millionSheet $degreeLetter"
    }

    data class GrReverseResult(
        val latitude: Double,
        val longitude: Double,
        val latDms: String,
        val lonDms: String,
        val easting: Double,
        val northing: Double,
        val confirmedMapSheet: String,
        val militaryGr6: String,
        val militaryGr8: String,
        val militaryGr10: String,
        val sheetInfo: MapSheetInfo
    ) {
        val latFormatted: String get() = "%.5f".format(Locale.US, latitude)
        val lonFormatted: String get() = "%.5f".format(Locale.US, longitude)
        val eastingFormatted: String get() = "%.2f".format(Locale.US, easting)
        val northingFormatted: String get() = "%.2f".format(Locale.US, northing)
    }

    data class DmsParts(
        val degrees: Int,
        val minutes: Int,
        val seconds: Double,
        val isNegative: Boolean
    ) {
        fun format(isLatitude: Boolean): String {
            val dir = if (isLatitude) {
                if (isNegative) "S" else "N"
            } else {
                if (isNegative) "W" else "E"
            }
            return "%d° %02d' %05.2f\" %s".format(Locale.US, degrees, minutes, seconds, dir)
        }

        fun toColonString(): String {
            return "%d:%02d:%05.2f".format(Locale.US, degrees, minutes, seconds)
        }
    }

    /**
     * Converts Decimal degrees to DMS parts
     */
    fun decimalToDmsParts(decimal: Double): DmsParts {
        val isNeg = decimal < 0
        val absVal = kotlin.math.abs(decimal)
        val deg = absVal.toInt()
        val remainder = (absVal - deg) * 60.0
        val min = remainder.toInt()
        val sec = (remainder - min) * 60.0
        return DmsParts(degrees = deg, minutes = min, seconds = roundTwoDecimals(sec), isNegative = isNeg)
    }

    /**
     * Converts DMS (degree, minute, second) to Decimal degrees
     */
    fun dmsToDecimal(degree: Double, min: Double, sec: Double, isNegative: Boolean = false): Double {
        val dec = degree + (min * 60.0 + sec) / 3600.0
        val res = roundFiveDecimals(dec)
        return if (isNegative) -res else res
    }

    /**
     * Lat/Long (decimal degrees, Everest 1830) → BUTM Grid (Easting, Northing)
     */
    fun latLonToButmGrid(latDeg: Double, lonDeg: Double): ButmResult {
        val lat = latDeg * cdr
        val lon = lonDeg * cdr

        val d1 = lon - lnor_U
        val d2 = sin(lat)
        val d3 = cos(lat)
        val d4 = tan(lat)
        val d5 = se2e * d3 * d3

        val nVal = ae / sqrt(1.0 - e2e * d2 * d2)
        val tVal = d4 * d4
        val cVal = d5
        val aVal = d3 * d1
        val mVal = meridDistForward(lat)

        val easting = ko_U * nVal * (aVal + (aVal.pow(3) / 6.0) * (1.0 - tVal + cVal)
                + (aVal.pow(5) / 120.0) * (5.0 - 18.0 * tVal + tVal * tVal + 72.0 * cVal - 58.0 * se2e)) + FE_U

        val northing = ko_U * (mVal + nVal * d4 * ((aVal * aVal / 2.0) + (aVal.pow(4) / 24.0) * (5.0 - tVal + 9.0 * cVal + 4.0 * cVal * cVal)
                + (aVal.pow(6) / 720.0) * (61.0 - 58.0 * tVal + tVal * tVal + 600.0 * cVal - 330.0 * se2e))) + FN_U

        val roundedEasting = roundTwoDecimals(easting)
        val roundedNorthing = roundTwoDecimals(northing)

        val sheetNo = calculateMapSheet(latDeg, lonDeg)
        val degSheet = sheetNo.substringBefore("/")

        // Calculate Military Grid References inside 100km block
        val eInt = ((roundedEasting % 100000).toLong() + 100000) % 100000
        val nInt = ((roundedNorthing % 100000).toLong() + 100000) % 100000

        val e3 = "%03d".format(Locale.US, (eInt / 100) % 1000)
        val n3 = "%03d".format(Locale.US, (nInt / 100) % 1000)
        val gr6 = "$e3 $n3"

        val e4 = "%04d".format(Locale.US, (eInt / 10) % 10000)
        val n4 = "%04d".format(Locale.US, (nInt / 10) % 10000)
        val gr8 = "$e4 $n4"

        val e5 = "%05d".format(Locale.US, eInt % 100000)
        val n5 = "%05d".format(Locale.US, nInt % 100000)
        val gr10 = "$e5 $n5"

        val dmsLat = decimalToDmsParts(latDeg)
        val dmsLon = decimalToDmsParts(lonDeg)

        return ButmResult(
            easting = roundedEasting,
            northing = roundedNorthing,
            mapSheetNo = sheetNo,
            degreeSheet = degSheet,
            militaryGr6 = gr6,
            militaryGr8 = gr8,
            militaryGr10 = gr10,
            latDms = dmsLat.format(isLatitude = true),
            lonDms = dmsLon.format(isLatitude = false)
        )
    }

    /**
     * BUTM Grid (Easting, Northing) → Lat/Long (Everest 1830)
     */
    fun butmGridToLatLon(easting: Double, northing: Double): LatLonResult {
        val d1 = easting - FE_U
        val d2 = northing - FN_U

        val sMd = meridDistForward(ltor_U)
        val mDist = (sMd + d2) / ko_U
        val ltFp = meridDistInverse(mDist)

        val d3 = ltFp
        val d4 = (ko_U * ae) / sqrt(1.0 - (e2e * sin(d3).pow(2)))
        val d5 = d1 / d4

        val d6 = tan(d3).pow(2)
        val d7 = se2e * cos(d3).pow(2)
        val d8 = sqrt(d6) * (1.0 + d7)

        val lat = d3 - ((d5 * d5) / 2.0) * d8 * (1.0 - (d5 * d5 / 24.0) * (5.0 + 3.0 * d6 + d7 * (1.0 - 9.0 * d6) - 4.0 * d7)
                + (d5.pow(4) / 720.0) * (61.0 + 90.0 * d6 + 45.0 * d6 * d6))

        val lon = lnor_U + (d5 / cos(d3)) * (1.0 - (d5 * d5 / 6.0) * (1.0 + 2.0 * d6 + d7)
                + (d5.pow(4) / 120.0) * (5.0 + 28.0 * d6 + 24.0 * d6 * d6))

        val latDeg = roundFiveDecimals(lat * crd)
        val lonDeg = roundFiveDecimals(lon * crd)

        val sheetNo = calculateMapSheet(latDeg, lonDeg)
        val degSheet = sheetNo.substringBefore("/")

        val eInt = ((roundTwoDecimals(easting) % 100000).toLong() + 100000) % 100000
        val nInt = ((roundTwoDecimals(northing) % 100000).toLong() + 100000) % 100000

        val gr6 = "%03d %03d".format(Locale.US, (eInt / 100) % 1000, (nInt / 100) % 1000)
        val gr8 = "%04d %04d".format(Locale.US, (eInt / 10) % 10000, (nInt / 10) % 10000)
        val gr10 = "%05d %05d".format(Locale.US, eInt % 100000, nInt % 100000)

        val dmsLat = decimalToDmsParts(latDeg)
        val dmsLon = decimalToDmsParts(lonDeg)

        return LatLonResult(
            latitude = latDeg,
            longitude = lonDeg,
            latDms = dmsLat.format(isLatitude = true),
            lonDms = dmsLon.format(isLatitude = false),
            mapSheetNo = sheetNo,
            degreeSheet = degSheet,
            militaryGr6 = gr6,
            militaryGr8 = gr8,
            militaryGr10 = gr10
        )
    }

    /**
     * Official Bangladesh Topographical Map Sheet Number calculation (1:50,000 sheet)
     * Follows Survey of India / Survey of Bangladesh column-wise 1:250,000 degree sheet layout
     * and column-wise 1:50,000 15' x 15' quadrangle layout.
     */
    fun calculateMapSheet(latDeg: Double, lonDeg: Double): String {
        // Determine Million Sheet and base origin
        val (million, baseLat, baseLon) = when {
            lonDeg < 92.0 -> {
                if (latDeg >= 24.0) Triple(78, 24.0, 88.0) else Triple(79, 20.0, 88.0)
            }
            else -> {
                if (latDeg >= 24.0) Triple(83, 24.0, 92.0) else Triple(84, 20.0, 92.0)
            }
        }

        // Degree sheet col (W->E) and row (N->S) inside 4° x 4°
        val degCol = (lonDeg - baseLon).toInt().coerceIn(0, 3)
        val degRowFromBottom = (latDeg - baseLat).toInt().coerceIn(0, 3)
        val degRowFromTop = (3 - degRowFromBottom).coerceIn(0, 3)

        val letterIndex = degCol * 4 + degRowFromTop
        val letter = ('A'.code + letterIndex).toChar()

        // 15' x 15' quadrangle (1 to 16) inside 1° x 1°
        val latMin = ((latDeg - latDeg.toInt()) * 60.0).coerceIn(0.0, 59.9999)
        val lonMin = ((lonDeg - lonDeg.toInt()) * 60.0).coerceIn(0.0, 59.9999)

        val subCol = (lonMin / 15.0).toInt().coerceIn(0, 3)
        val subRowFromTop = (3 - (latMin / 15.0).toInt()).coerceIn(0, 3)
        val subNumber = subCol * 4 + subRowFromTop + 1

        return "$million $letter/$subNumber"
    }

    /**
     * Parses Map Sheet string (e.g., "79 I/5", "79I/5", "79-I-5", "78 G/16", "84 C/2")
     * and computes its geographic bounding box.
     */
    fun parseMapSheet(sheetInput: String): MapSheetInfo? {
        val normalized = normalizeDigits(sheetInput.trim().uppercase(Locale.US))
        val regex = Regex("""^([0-9]{2})\s*[-/]?\s*([A-P])(?:\s*[-/]?\s*([0-9]{1,2}))?""")
        val match = regex.find(normalized) ?: return null

        val million = match.groupValues[1].toIntOrNull() ?: return null
        val letter = match.groupValues[2][0]
        val subNumber = match.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }?.toIntOrNull()

        val (baseLat, baseLon) = when (million) {
            78 -> Pair(24.0, 88.0)
            79 -> Pair(20.0, 88.0)
            83 -> Pair(24.0, 92.0)
            84 -> Pair(20.0, 92.0)
            else -> return null
        }

        val letterIdx = letter - 'A'
        val degCol = letterIdx / 4
        val degRowFromTop = letterIdx % 4

        val degLatMin = baseLat + (3 - degRowFromTop)
        val degLatMax = degLatMin + 1.0
        val degLonMin = baseLon + degCol
        val degLonMax = degLonMin + 1.0

        return if (subNumber != null && subNumber in 1..16) {
            val subIdx = subNumber - 1
            val subCol = subIdx / 4
            val subRowFromTop = subIdx % 4

            val sheetLatMin = degLatMin + (3 - subRowFromTop) * 0.25
            val sheetLatMax = sheetLatMin + 0.25
            val sheetLonMin = degLonMin + subCol * 0.25
            val sheetLonMax = sheetLonMin + 0.25

            MapSheetInfo(
                sheetNo = "$million $letter/$subNumber",
                millionSheet = million,
                degreeLetter = letter,
                subNumber = subNumber,
                minLat = sheetLatMin,
                maxLat = sheetLatMax,
                minLon = sheetLonMin,
                maxLon = sheetLonMax
            )
        } else {
            MapSheetInfo(
                sheetNo = "$million $letter",
                millionSheet = million,
                degreeLetter = letter,
                subNumber = null,
                minLat = degLatMin,
                maxLat = degLatMax,
                minLon = degLonMin,
                maxLon = degLonMax
            )
        }
    }

    /**
     * Converts Military Grid Reference (GR) and Bangladesh Map Sheet Number
     * back to exact Lat/Long (Everest 1830) and BUTM Easting & Northing.
     *
     * Supports:
     * - 6-digit GR (e.g., "420 338" or "420338") -> 100m accuracy
     * - 8-digit GR (e.g., "4202 3381" or "42023381") -> 10m accuracy
     * - 10-digit GR (e.g., "42018 33814") -> 1m accuracy
     */
    fun grAndMapSheetToLatLon(mapSheetStr: String, grStr: String): GrReverseResult {
        val sheetInfo = parseMapSheet(mapSheetStr.trim())
            ?: throw IllegalArgumentException("অবৈধ ম্যাপ শীট নম্বর! উদাহরণ: 79 I/5, 78 G/16, 84 C/2")

        val cleanGr = normalizeDigits(grStr).replace(Regex("[^0-9]"), "")
        val (eMeters, nMeters) = when (cleanGr.length) {
            6 -> {
                val e = cleanGr.substring(0, 3).toDouble() * 100.0 + 50.0 // cell midpoint
                val n = cleanGr.substring(3, 6).toDouble() * 100.0 + 50.0
                Pair(e, n)
            }
            8 -> {
                val e = cleanGr.substring(0, 4).toDouble() * 10.0 + 5.0 // cell midpoint
                val n = cleanGr.substring(4, 8).toDouble() * 10.0 + 5.0
                Pair(e, n)
            }
            10 -> {
                val e = cleanGr.substring(0, 5).toDouble()
                val n = cleanGr.substring(5, 10).toDouble()
                Pair(e, n)
            }
            else -> {
                // Try parsing space/dash/slash separated pair
                val parts = normalizeDigits(grStr).trim().split(Regex("[\\s,/-]+")).filter { it.isNotBlank() }
                if (parts.size == 2 && parts[0].length == parts[1].length && parts[0].all { it.isDigit() }) {
                    when (parts[0].length) {
                        3 -> Pair(parts[0].toDouble() * 100.0 + 50.0, parts[1].toDouble() * 100.0 + 50.0)
                        4 -> Pair(parts[0].toDouble() * 10.0 + 5.0, parts[1].toDouble() * 10.0 + 5.0)
                        5 -> Pair(parts[0].toDouble(), parts[1].toDouble())
                        else -> throw IllegalArgumentException("GR অবশ্যই ৬, ৮ বা ১০ ডিজিটের হতে হবে (যেমন: 420 338 বা 4202 3381)")
                    }
                } else {
                    throw IllegalArgumentException("GR অবশ্যই ৬, ৮ বা ১০ ডিজিটের হতে হবে (যেমন: 420 338 বা 4202 3381)")
                }
            }
        }

        // Center of the sheet
        val centerLat = (sheetInfo.minLat + sheetInfo.maxLat) / 2.0
        val centerLon = (sheetInfo.minLon + sheetInfo.maxLon) / 2.0
        val centerButm = latLonToButmGrid(centerLat, centerLon)

        // Find the integer 100,000m grid multiple
        val kE = kotlin.math.round((centerButm.easting - eMeters) / 100000.0).toLong()
        val kN = kotlin.math.round((centerButm.northing - nMeters) / 100000.0).toLong()

        val fullEasting = kE * 100000.0 + eMeters
        val fullNorthing = kN * 100000.0 + nMeters

        val latLon = butmGridToLatLon(fullEasting, fullNorthing)
        val butm = latLonToButmGrid(latLon.latitude, latLon.longitude)

        val dmsLat = decimalToDmsParts(latLon.latitude)
        val dmsLon = decimalToDmsParts(latLon.longitude)

        return GrReverseResult(
            latitude = latLon.latitude,
            longitude = latLon.longitude,
            latDms = dmsLat.format(isLatitude = true),
            lonDms = dmsLon.format(isLatitude = false),
            easting = roundTwoDecimals(fullEasting),
            northing = roundTwoDecimals(fullNorthing),
            confirmedMapSheet = butm.mapSheetNo,
            militaryGr6 = butm.militaryGr6,
            militaryGr8 = butm.militaryGr8,
            militaryGr10 = butm.militaryGr10,
            sheetInfo = sheetInfo
        )
    }

    // ====================== HELPER METHODS ======================

    private fun meridDistForward(lat: Double): Double {
        val e2 = e2e
        val e4 = e2 * e2
        val e6 = e4 * e2
        val a0 = 1.0 - e2 / 4.0 - 3.0 * e4 / 64.0 - 5.0 * e6 / 256.0
        val a2 = 3.0 / 8.0 * (e2 + e4 / 4.0 + 15.0 * e6 / 128.0)
        val a4 = 15.0 / 256.0 * (e4 + 3.0 * e6 / 4.0)
        val a6 = 35.0 * e6 / 3072.0
        return ae * (a0 * lat - a2 * sin(2 * lat) + a4 * sin(4 * lat) - a6 * sin(6 * lat))
    }

    private fun meridDistInverse(m: Double): Double {
        val mu = m / (ae * (1.0 - e2e / 4.0 - 3.0 * e2e * e2e / 64.0))
        val e1 = (1.0 - sqrt(1.0 - e2e)) / (1.0 + sqrt(1.0 - e2e))
        return mu + (3.0 * e1 / 2.0 - 27.0 * e1.pow(3) / 32.0) * sin(2 * mu) +
                (21.0 * e1 * e1 / 16.0 - 55.0 * e1.pow(4) / 32.0) * sin(4 * mu) +
                (151.0 * e1.pow(3) / 96.0) * sin(6 * mu)
    }

    fun roundTwoDecimals(v: Double): Double {
        return try {
            DecimalFormat("#.##").format(v).toDouble()
        } catch (_: Exception) {
            (v * 100.0).roundToLong() / 100.0
        }
    }

    fun roundFiveDecimals(v: Double): Double {
        return try {
            DecimalFormat("#.#####").format(v).toDouble()
        } catch (_: Exception) {
            (v * 100000.0).roundToLong() / 100000.0
        }
    }

    private fun Double.roundToLong(): Long = kotlin.math.round(this).toLong()

    fun normalizeDigits(input: String): String {
        val bnDigits = "০১২৩৪৫৬৭৮৯"
        var res = input
        for (i in 0..9) {
            res = res.replace(bnDigits[i], ('0' + i))
        }
        return res
    }

    /** Lat/Long (decimal) → DMS string matching user's format: "lat/lon" e.g. "23:05:32.96/90:52:29.98" */
    fun decimalToDMS(lat: Double, lon: Double): String {
        val latParts = decimalToDmsParts(lat)
        val lonParts = decimalToDmsParts(lon)
        return "${latParts.toColonString()}/${lonParts.toColonString()}"
    }

    /**
     * BUTM Grid (Easting, Northing) → Lat/Long string "lat,lon" in decimal degrees
     */
    fun butmGridToLatLonString(easting: Double, northing: Double): String {
        val res = butmGridToLatLon(easting, northing)
        return "${res.latFormatted},${res.lonFormatted}"
    }

    /**
     * Lat/Long (decimal degrees) → BUTM Grid string "easting,northing"
     */
    fun latLonToButmGridString(latDeg: Double, lonDeg: Double): String {
        val res = latLonToButmGrid(latDeg, lonDeg)
        return "${res.eastingFormatted},${res.northingFormatted}"
    }

    /** Map Sheet Number from DMS string e.g. "23:05:32.96/90:52:29.98" */
    fun getMapSheetNo(dmsString: String): String {
        return try {
            val normalized = normalizeDigits(dmsString)
            val parts = normalized.split("/")
            val latParts = parts[0].trim().split(":")
            val lonParts = parts[1].trim().split(":")
            val latDeg = latParts[0].toDouble()
            val latMin = latParts[1].toDouble()
            val latSec = if (latParts.size > 2) latParts[2].toDouble() else 0.0

            val lonDeg = lonParts[0].toDouble()
            val lonMin = lonParts[1].toDouble()
            val lonSec = if (lonParts.size > 2) lonParts[2].toDouble() else 0.0

            val lat = latDeg + (latMin * 60.0 + latSec) / 3600.0
            val lon = lonDeg + (lonMin * 60.0 + lonSec) / 3600.0
            calculateMapSheet(lat, lon)
        } catch (_: Exception) {
            "79 I/5"
        }
    }
}

/**
 * Compatibility class matching user's exact Java class naming: `ButmConvertion`
 * Developed By : 1618101 Cpl (Tech) Md Zakaria Alom, 3 Signal Battalion
 */
class ButmConvertion {
    fun decimalToDMS(lat: Double, lon: Double): String = ButmConversion.decimalToDMS(lat, lon)
    fun dmsToDecimal(degree: Double, min: Double, sec: Double): Double = ButmConversion.dmsToDecimal(degree, min, sec)
    fun butmGridToLatLon(easting: Double, northing: Double): String = ButmConversion.butmGridToLatLonString(easting, northing)
    fun latLonToButmGrid(latDeg: Double, lonDeg: Double): String = ButmConversion.latLonToButmGridString(latDeg, lonDeg)
    fun getMapSheetNo(dmsString: String): String = ButmConversion.getMapSheetNo(dmsString)
}

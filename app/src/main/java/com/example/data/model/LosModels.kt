package com.example.data.model

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class AntennaConfig(
    val p1HeightMeters: Double = 24.0, // Site A Mast Height (default 24m)
    val p2HeightMeters: Double = 24.0, // Site B Mast Height (default 24m)
    val frequencyMhz: Double = 1350.0,  // Military Tactical Radio Relay Band IV (default 1350 MHz)
    val txPowerDbm: Double = 30.0,     // Power Output default 30 dBm (1.0 W)
    val rxSensitivityDbm: Double = -87.0 // Receiver Sensitivity default -87 dBm (-47 to -87 dBm)
)

data class TerrainPoint(
    val distanceFraction: Float, // 0.0 (Site A) to 1.0 (Site B)
    val groundElevationMeters: Double,
    val earthBulgeMeters: Double,
    val losBeamHeightMeters: Double,
    val fresnelRadiusMeters: Double
)

data class RfLinkBudget(
    val txPowerDbm: Double = 30.0,
    val rxSensitivityDbm: Double = -87.0,
    val frequencyMhz: Double = 1350.0,
    val distanceKm: Double,
    val freeSpacePathLossDb: Double, // FSPL(dB) = 20*log10(d_km) + 20*log10(f_MHz) + 32.44
    val txAntennaGainDbi: Double = 15.0,
    val rxAntennaGainDbi: Double = 15.0,
    val cableLossDb: Double = 2.0,
    val obstacleLossDb: Double = 0.0, // Diffraction / clutter loss
    val receivedSignalLevelDbm: Double, // RSL = P_tx + G_tx + G_rx - CableLoss - FSPL - ObstacleLoss
    val fadeMarginDb: Double, // Fade Margin = RSL - RxSensitivity
    val linkReliabilityPercent: Double,
    val linkStatusText: String,
    val linkStatusColor: LosAnalysisResult.StatusColor
)

data class LosAnalysisResult(
    val isLosClear: Boolean,
    val clearanceMarginMeters: Double,
    val earthCurvatureBulgeMeters: Double,
    val fresnelZoneRadiusMeters: Double,
    val statusText: String,
    val statusColorType: StatusColor,
    val recommendation: String,
    val points: List<TerrainPoint>,
    val linkBudget: RfLinkBudget
) {
    enum class StatusColor {
        CLEAR,
        MARGINAL,
        OBSTRUCTED
    }
}

object LosCalculator {

    // K-factor for standard atmospheric refraction = 4/3
    private const val K_FACTOR = 4.0 / 3.0
    private const val EARTH_RADIUS_KM = 6371.0

    fun calculateLineOfSight(
        distanceKm: Double,
        p1Elevation: Double,
        p1AntennaHeight: Double,
        p2Elevation: Double,
        p2AntennaHeight: Double,
        frequencyMhz: Double = 1350.0,
        txPowerDbm: Double = 30.0,
        rxSensitivityDbm: Double = -87.0,
        antennaGainDbi: Double = 15.0
    ): LosAnalysisResult {
        val totalHeight1 = p1Elevation + p1AntennaHeight
        val totalHeight2 = p2Elevation + p2AntennaHeight

        val dKmSafe = max(distanceKm, 0.001)
        val fMhzSafe = max(frequencyMhz, 1.0)

        // Free Space Path Loss (FSPL) Formula:
        // FSPL(dB) = 20 * log10(d_km) + 20 * log10(f_MHz) + 32.44
        val fsplDb = 20.0 * log10(dKmSafe) + 20.0 * log10(fMhzSafe) + 32.44
        val txGain = antennaGainDbi // dBi
        val rxGain = antennaGainDbi // dBi
        val cableLoss = 2.0 // dB

        if (distanceKm <= 0.0) {
            val rsl = txPowerDbm + txGain + rxGain - cableLoss - 32.44
            val margin = rsl - rxSensitivityDbm
            val fallbackBudget = RfLinkBudget(
                txPowerDbm = txPowerDbm,
                rxSensitivityDbm = rxSensitivityDbm,
                frequencyMhz = frequencyMhz,
                distanceKm = 0.0,
                freeSpacePathLossDb = 32.44,
                txAntennaGainDbi = txGain,
                rxAntennaGainDbi = rxGain,
                cableLossDb = cableLoss,
                obstacleLossDb = 0.0,
                receivedSignalLevelDbm = rsl,
                fadeMarginDb = margin,
                linkReliabilityPercent = 99.99,
                linkStatusText = "LOS Clear (একই স্থান)",
                linkStatusColor = LosAnalysisResult.StatusColor.CLEAR
            )
            return LosAnalysisResult(
                isLosClear = true,
                clearanceMarginMeters = p1AntennaHeight,
                earthCurvatureBulgeMeters = 0.0,
                fresnelZoneRadiusMeters = 0.0,
                statusText = "LOS Clear (একই স্থান)",
                statusColorType = LosAnalysisResult.StatusColor.CLEAR,
                recommendation = "উভয় এন্টেনা একই স্থানে অবস্থিত।",
                points = emptyList(),
                linkBudget = fallbackBudget
            )
        }

        // Maximum Earth curvature bulge at midpoint: h = d1*d2 / (2 * k * R)
        val midEarthBulge = (distanceKm * distanceKm) / (8.0 * K_FACTOR * (EARTH_RADIUS_KM / 1000.0))

        // 1st Fresnel zone radius at midpoint: F1 = 17.32 * sqrt( (d1 * d2) / (f_GHz * D) )
        val fGhz = fMhzSafe / 1000.0
        val d1Km = distanceKm / 2.0
        val d2Km = distanceKm / 2.0
        val fresnelMid = 17.32 * sqrt((d1Km * d2Km) / (fGhz * distanceKm))

        // LOS straight line height at midpoint
        val losMidHeight = (totalHeight1 + totalHeight2) / 2.0

        // Terrain baseline at midpoint (interpolated ground elevation + earth curvature bulge)
        val interpolatedGroundMid = (p1Elevation + p2Elevation) / 2.0
        val obstacleMidHeight = interpolatedGroundMid + midEarthBulge

        // Clearance margin between LOS line and Earth/Terrain bulge
        val clearanceToEarth = losMidHeight - obstacleMidHeight

        val isClear = clearanceToEarth > fresnelMid
        val isMarginal = clearanceToEarth in 0.0..fresnelMid
        val isObstructed = clearanceToEarth < 0.0

        // Obstacle / Diffraction loss
        val obstacleLossDb = when {
            clearanceToEarth >= fresnelMid -> 0.0
            clearanceToEarth in (0.6 * fresnelMid)..fresnelMid -> 1.0
            clearanceToEarth in 0.0..(0.6 * fresnelMid) -> {
                val ratio = clearanceToEarth / (0.6 * fresnelMid.coerceAtLeast(0.1))
                1.0 + 5.0 * (1.0 - ratio)
            }
            else -> {
                val blockedRatio = kotlin.math.abs(clearanceToEarth) / fresnelMid.coerceAtLeast(1.0)
                6.0 + 16.0 * min(blockedRatio, 2.5)
            }
        }

        // Received Signal Level (RSL) and Fade Margin
        val rslDbm = txPowerDbm + txGain + rxGain - cableLoss - fsplDb - obstacleLossDb
        val fadeMarginDb = rslDbm - rxSensitivityDbm

        val (linkStatusText, linkReliabilityPercent, linkColor) = when {
            fadeMarginDb >= 20.0 && !isObstructed -> Triple(
                "উৎকৃষ্ট লিঙ্ক (৯৯.৯৯% রিলাইবিলিটি)",
                99.99,
                LosAnalysisResult.StatusColor.CLEAR
            )
            fadeMarginDb in 10.0..20.0 && !isObstructed -> Triple(
                "স্থিতিশীল ও কার্যকরী লিঙ্ক (STABLE)",
                99.0,
                LosAnalysisResult.StatusColor.CLEAR
            )
            fadeMarginDb in 0.0..10.0 || isMarginal -> Triple(
                "মার্জিনাল লিঙ্ক (ঝুঁকিপূর্ণ / WEATHER SENSITIVE)",
                90.0,
                LosAnalysisResult.StatusColor.MARGINAL
            )
            else -> Triple(
                "সিগন্যাল লস / লিঙ্ক ড্রপ (DISCONNECTED)",
                0.0,
                LosAnalysisResult.StatusColor.OBSTRUCTED
            )
        }

        val (statusText, colorType, advice) = when {
            isClear && fadeMarginDb >= 15.0 -> Triple(
                "LINE OF SIGHT: ক্লিয়ার (CLEAR)",
                LosAnalysisResult.StatusColor.CLEAR,
                "উভয় এন্টেনার মাঝে সরাসরি দৃষ্টিরেখা (LOS) ও ৬০% ফ্রেনেল জোন সম্পূর্ণ ক্লিয়ার আছে। ফেড মার্জিন +${"%.1f".format(fadeMarginDb)} dB সহ রেডিও সিগন্যাল সর্বোচ্চ দক্ষতায় প্রবাহিত হবে।"
            )
            isMarginal || (isClear && fadeMarginDb < 15.0) -> Triple(
                "LINE OF SIGHT: মার্জিনাল (FRESNEL TOUCH / LOW MARGIN)",
                LosAnalysisResult.StatusColor.MARGINAL,
                "দৃষ্টিরেখা রয়েছে তবে ফ্রেনেল জোন বা ফেড মার্জিন (${"%.1f".format(fadeMarginDb)} dB) সংবেদনশীল। এন্টেনার উচ্চতা ${"%.1f".format(fresnelMid - clearanceToEarth).coerceAtLeast("2.0")} মি বা ট্রান্সমিট পাওয়ার বাড়ালে লিঙ্ক আরও নির্ভরযোগ্য হবে।"
            )
            else -> Triple(
                "LINE OF SIGHT: অবস্ট্রাকটেড / বাধাপ্রাপ্ত (BLOCKED)",
                LosAnalysisResult.StatusColor.OBSTRUCTED,
                "পৃথিবীর বক্রতা বা উচ্চতা পার্থক্যের কারণে সরাসরি লাইন অফ সাইড বাধাপ্রাপ্ত হচ্ছে (ফেড মার্জিন: ${"%.1f".format(fadeMarginDb)} dB)! সংযোগ নিশ্চিত করতে অন্তত ${"%.1f".format(-clearanceToEarth + fresnelMid)} মিটার এন্টেনা উচ্চতা বাড়ানো প্রয়োজন।"
            )
        }

        // Generate 10-point cross-section profile for digital plotting
        val profilePoints = mutableListOf<TerrainPoint>()
        val steps = 10
        for (i in 0..steps) {
            val frac = i.toFloat() / steps
            val d1 = distanceKm * frac
            val d2 = distanceKm * (1.0 - frac)
            val bulge = if (frac > 0 && frac < 1) (d1 * d2) / (2.0 * K_FACTOR * (EARTH_RADIUS_KM / 1000.0)) else 0.0
            val ground = p1Elevation + (p2Elevation - p1Elevation) * frac
            val losRay = totalHeight1 + (totalHeight2 - totalHeight1) * frac
            val fresnel = if (frac > 0 && frac < 1) 17.32 * sqrt((d1 * d2) / (fGhz * distanceKm)) else 0.0

            profilePoints.add(
                TerrainPoint(
                    distanceFraction = frac,
                    groundElevationMeters = ground,
                    earthBulgeMeters = bulge,
                    losBeamHeightMeters = losRay,
                    fresnelRadiusMeters = fresnel
                )
            )
        }

        val linkBudget = RfLinkBudget(
            txPowerDbm = txPowerDbm,
            rxSensitivityDbm = rxSensitivityDbm,
            frequencyMhz = frequencyMhz,
            distanceKm = distanceKm,
            freeSpacePathLossDb = fsplDb,
            txAntennaGainDbi = txGain,
            rxAntennaGainDbi = rxGain,
            cableLossDb = cableLoss,
            obstacleLossDb = obstacleLossDb,
            receivedSignalLevelDbm = rslDbm,
            fadeMarginDb = fadeMarginDb,
            linkReliabilityPercent = linkReliabilityPercent,
            linkStatusText = linkStatusText,
            linkStatusColor = linkColor
        )

        return LosAnalysisResult(
            isLosClear = isClear,
            clearanceMarginMeters = clearanceToEarth,
            earthCurvatureBulgeMeters = midEarthBulge,
            fresnelZoneRadiusMeters = fresnelMid,
            statusText = statusText,
            statusColorType = colorType,
            recommendation = advice,
            points = profilePoints,
            linkBudget = linkBudget
        )
    }
}

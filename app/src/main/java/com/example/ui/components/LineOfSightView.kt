package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CalculationResult
import com.example.data.model.KmlExporter
import com.example.data.model.LosAnalysisResult
import com.example.data.model.LosCalculator
import com.example.ui.theme.CyberGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonGreen
import java.util.Locale

/**
 * Single Terrain Point Sample for realistic DEM Profile & Earth Curvature Bulge.
 */
data class TerrainSample(
    val fraction: Double,
    val distKm: Double,
    val rawTerrainMsl: Double,
    val bulgeMeters: Double,
    val effectiveGroundMsl: Double,
    val losRayMsl: Double,
    val fresnelRadius: Double,
    val clearanceMeters: Double
)

/**
 * Item definition for the 13-parameter Digital Link Budget Table (CR 4).
 */
data class RfBudgetParam(
    val sl: String,
    val nameEn: String,
    val nameBn: String,
    val valueStr: String,
    val unitStr: String
)

@Composable
fun LineOfSightView(
    calcResult: CalculationResult?,
    p1Elevation: Double?,
    p2Elevation: Double?,
    p1AntennaHeight: String,
    onP1AntennaHeightChange: (String) -> Unit,
    p2AntennaHeight: String,
    onP2AntennaHeightChange: (String) -> Unit,
    frequencyMhz: Double,
    onFrequencyChange: (Double) -> Unit,
    txPowerDbm: Double = 30.0,
    onTxPowerChange: (Double) -> Unit = {},
    rxSensitivityDbm: Double = -87.0,
    onRxSensitivityChange: (Double) -> Unit = {},
    frequency2Mhz: Double = 2400.0,
    onFrequency2Change: (Double) -> Unit = {},
    antennaGainDbi: Double = 15.0,
    onAntennaGainChange: (Double) -> Unit = {},
    modifier: Modifier = Modifier,
    onScanObstaclesGemini: () -> Unit = {}
) {
    val context = LocalContext.current

    // Antenna heights (default 24m)
    val h1 = p1AntennaHeight.toDoubleOrNull() ?: 24.0
    val h2 = p2AntennaHeight.toDoubleOrNull() ?: 24.0
    val elev1 = p1Elevation ?: 15.0
    val elev2 = p2Elevation ?: 15.0

    val p1Lat = calcResult?.point1?.latitude ?: 23.8103
    val p1Lon = calcResult?.point1?.longitude ?: 90.4125
    val p2Lat = calcResult?.point2?.latitude ?: 24.3636
    val p2Lon = calcResult?.point2?.longitude ?: 88.6241

    // Geodesic distance
    val activeDistKm = if (calcResult != null) maxOf(0.01, calcResult.distanceMeters / 1000.0) else 10.0

    // Frequency 1 & Frequency 2 (side-by-side inputs)
    var customFreq1Text by remember(frequencyMhz) { mutableStateOf(frequencyMhz.toInt().toString()) }
    var customFreq2Text by remember(frequency2Mhz) { mutableStateOf(frequency2Mhz.toInt().toString()) }

    // Power output (dBm) & Antenna Gain (dBi)
    var customTxPowerText by remember(txPowerDbm) { mutableStateOf(txPowerDbm.toInt().toString()) }
    var customAntennaGainText by remember(antennaGainDbi) { mutableStateOf(antennaGainDbi.toInt().toString()) }

    // Receiver sensitivity (-47 to -130 dBm)
    var customRxSensText by remember(rxSensitivityDbm) { mutableStateOf(rxSensitivityDbm.toInt().toString()) }

    val activeFreq1Mhz = customFreq1Text.toDoubleOrNull() ?: frequencyMhz
    val activeFreq2Mhz = customFreq2Text.toDoubleOrNull() ?: frequency2Mhz
    val activeTxPowerDbm = customTxPowerText.toDoubleOrNull() ?: txPowerDbm
    val activeAntennaGainDbi = customAntennaGainText.toDoubleOrNull() ?: antennaGainDbi
    val activeRxSensDbm = customRxSensText.toDoubleOrNull() ?: rxSensitivityDbm

    // Compute Line of Sight & Link Budget
    val losResult = LosCalculator.calculateLineOfSight(
        distanceKm = activeDistKm,
        p1Elevation = elev1,
        p1AntennaHeight = h1,
        p2Elevation = elev2,
        p2AntennaHeight = h2,
        frequencyMhz = activeFreq1Mhz,
        txPowerDbm = activeTxPowerDbm,
        rxSensitivityDbm = activeRxSensDbm,
        antennaGainDbi = activeAntennaGainDbi
    )
    val budget = losResult.linkBudget

    // =========================================================================
    // CR 6.1: Multi-point Terrain Elevation Profile using Physiographic Model & Bulge
    // =========================================================================
    val numSamples = 25
    val terrainSamples = remember(p1Lat, p1Lon, p2Lat, p2Lon, elev1, elev2, h1, h2, activeDistKm, activeFreq1Mhz) {
        (0 until numSamples).map { i ->
            val frac = i.toDouble() / (numSamples - 1).toDouble()
            val lat = p1Lat + (p2Lat - p1Lat) * frac
            val lon = p1Lon + (p2Lon - p1Lon) * frac
            val rawTerrain = if (frac == 0.0) elev1 else if (frac == 1.0) elev2
            else com.example.calculator.ElevationService.calculateBangladeshPhysiographicElevation(lat, lon)

            val d1 = activeDistKm * frac
            val d2 = activeDistKm * (1.0 - frac)
            val bulge = if (frac > 0.0 && frac < 1.0) (d1 * d2) / 12.75 else 0.0
            val effectiveGround = rawTerrain + bulge
            val ray = (elev1 + h1) + ((elev2 + h2) - (elev1 + h1)) * frac
            val f1 = if (activeDistKm > 0.05 && frac > 0.0 && frac < 1.0) {
                17.32 * kotlin.math.sqrt((d1 * d2) / (activeFreq1Mhz * activeDistKm))
            } else 0.0
            val clearance = ray - effectiveGround

            TerrainSample(
                fraction = frac,
                distKm = d1,
                rawTerrainMsl = rawTerrain,
                bulgeMeters = bulge,
                effectiveGroundMsl = effectiveGround,
                losRayMsl = ray,
                fresnelRadius = f1,
                clearanceMeters = clearance
            )
        }
    }

    // Critical Clearance Point (Minimum Clearance along the Path)
    val criticalSample = remember(terrainSamples) {
        terrainSamples.minByOrNull { it.clearanceMeters } ?: terrainSamples[numSamples / 2]
    }

    // =========================================================================
    // CR 4: Complete 13 RF Link Budget Parameters (Radio Mobile / RMW Methodology)
    // =========================================================================
    val rfBudgetParams = remember(calcResult, elev1, elev2, h1, h2, activeDistKm, activeFreq1Mhz, activeTxPowerDbm, activeRxSensDbm, criticalSample, budget) {
        val az1 = calcResult?.bearingDegrees ?: 45.0
        val az2 = (az1 + 180.0) % 360.0

        val dMeters = activeDistKm * 1000.0
        val kFactor = 4.0 / 3.0
        val earthRadiusMeters = 6371000.0 * kFactor // ~8,494,667m

        // Antenna Tilt Angles (Elevation Angles)
        val deltaH1 = ((elev2 + h2) - (elev1 + h1)) - ((dMeters * dMeters) / (2.0 * earthRadiusMeters))
        val elevAngle1 = kotlin.math.atan2(deltaH1, dMeters) * (180.0 / Math.PI)

        val deltaH2 = ((elev1 + h1) - (elev2 + h2)) - ((dMeters * dMeters) / (2.0 * earthRadiusMeters))
        val elevAngle2 = kotlin.math.atan2(deltaH2, dMeters) * (180.0 / Math.PI)

        // Path Loss
        val fspl = 32.44 + (20.0 * kotlin.math.log10(maxOf(0.01, activeDistKm))) + (20.0 * kotlin.math.log10(maxOf(1.0, activeFreq1Mhz)))
        val obsLoss = budget.obstacleLossDb
        val totalPathLoss = fspl + obsLoss

        // Gains and Losses
        val txGain = budget.txAntennaGainDbi // 6.0 dBi
        val rxGain = budget.rxAntennaGainDbi // 6.0 dBi
        val cableLoss = budget.cableLossDb // 2.0 dB

        // E-Field Strength (dBµV/m) using RMW standard formula: E = 104.8 + P_tx + G_tx - Path_Loss + 20*log10(f_MHz)
        val eField = 104.8 + activeTxPowerDbm + txGain - totalPathLoss + (20.0 * kotlin.math.log10(maxOf(1.0, activeFreq1Mhz)))

        // Clearance at Critical Point
        val critClearanceM = criticalSample.clearanceMeters
        val critClearanceKm = critClearanceM / 1000.0

        // Receive Level (Rx Level) in dBm
        val rxLevelDbm = activeTxPowerDbm + txGain + rxGain - totalPathLoss - cableLoss

        // Worst Fresnel Zone (Percentage & F1 ratio)
        val worstFresnelRatio = if (criticalSample.fresnelRadius > 0.0) critClearanceM / criticalSample.fresnelRadius else 1.0
        val worstFresnelPct = worstFresnelRatio * 100.0

        // Receive Voltage (Rx Voltage) in µV for 50Ω System: V_rx = 10^((Rx_dBm + 107) / 20)
        val rxVoltageUv = Math.pow(10.0, (rxLevelDbm + 107.0) / 20.0)

        // Transmit Power in Watts: P_watts = 10^((P_dBm - 30) / 10)
        val pWatts = Math.pow(10.0, (activeTxPowerDbm - 30.0) / 10.0)

        listOf(
            RfBudgetParam("01", "1st Azimuth", "পয়েন্ট ১ দিক / Azimuth", String.format(Locale.US, "%.2f", az1), "Degree (°)"),
            RfBudgetParam("02", "1st Elevation Angle", "পয়েন্ট ১ অ্যান্টেনা টিল্ট কোণ", String.format(Locale.US, "%+.2f", elevAngle1), "Degree (°)"),
            RfBudgetParam("03", "2nd Azimuth / Back Azimuth", "পয়েন্ট ২ দিক / Back Azimuth", String.format(Locale.US, "%.2f", az2), "Degree (°)"),
            RfBudgetParam("04", "2nd Elevation Angle", "পয়েন্ট ২ অ্যান্টেনা টিল্ট কোণ", String.format(Locale.US, "%+.2f", elevAngle2), "Degree (°)"),
            RfBudgetParam("05", "Path Loss (FSPL + Obs)", "মোট পাথ লস ও ডিফ্র্যাকশন", String.format(Locale.US, "%.2f", totalPathLoss), "dB"),
            RfBudgetParam("06", "Electric Field Strength (E-Field)", "টার্মিনালে ইলেকট্রিক ফিল্ড", String.format(Locale.US, "%.2f", eField), "dBµV/m"),
            RfBudgetParam("07", "Clearance at Critical Point", "সংকীর্ণ পয়েন্টে ক্লিয়ারেন্স", String.format(Locale.US, "%+.2f m (%+.4f km)", critClearanceM, critClearanceKm), "m / km"),
            RfBudgetParam("08", "Receive Level (Rx Level)", "রিসিভার সিগন্যাল পাওয়ার", String.format(Locale.US, "%.2f", rxLevelDbm), "dBm"),
            RfBudgetParam("09", "Worst Fresnel Zone", "সবচেয়ে কম ফ্রেনেল ক্লিয়ারেন্স", String.format(Locale.US, "%.1f%% (%.2f F1)", worstFresnelPct, worstFresnelRatio), "F1 / %"),
            RfBudgetParam("10", "Receive Voltage (Rx Level)", "টার্মিনালে প্রাপ্ত ভোল্টেজ", String.format(Locale.US, "%.2f", rxVoltageUv), "µV"),
            RfBudgetParam("11", "Distance", "টার্মিনাল দুটির দূরত্ব", String.format(Locale.US, "%.3f", activeDistKm), "km"),
            RfBudgetParam("12", "Transmit Power (TX Power)", "ট্রান্সমিটার আউটপুট পাওয়ার", String.format(Locale.US, "%.1f dBm (%.3f W)", activeTxPowerDbm, pWatts), "dBm / W"),
            RfBudgetParam("13", "Antenna Gain", "পয়েন্ট ১ ও ২ অ্যান্টেনা গেইন", String.format(Locale.US, "%.1f dBi / %.1f dBi", txGain, rxGain), "dBi")
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Digital Path Profile & Radio Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CellTower,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ডিজিটাল পাথ প্রোফাইল ও লিঙ্ক বাজেট",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Line of Sight (LOS) • ১ম ফ্রেনেল জোন • লিঙ্ক মার্জিন (${String.format(Locale.US, "%.2f km", activeDistKm)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status chip
                Surface(
                    color = when (losResult.statusColorType) {
                        LosAnalysisResult.StatusColor.CLEAR -> Color(0x3300E676)
                        LosAnalysisResult.StatusColor.MARGINAL -> Color(0x33FFD600)
                        LosAnalysisResult.StatusColor.OBSTRUCTED -> Color(0x33EF4444)
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when (losResult.statusColorType) {
                            LosAnalysisResult.StatusColor.CLEAR -> NeonGreen
                            LosAnalysisResult.StatusColor.MARGINAL -> CyberGold
                            LosAnalysisResult.StatusColor.OBSTRUCTED -> Color(0xFFEF4444)
                        }
                    )
                ) {
                    Text(
                        text = losResult.statusText,
                        color = when (losResult.statusColorType) {
                            LosAnalysisResult.StatusColor.CLEAR -> NeonGreen
                            LosAnalysisResult.StatusColor.MARGINAL -> CyberGold
                            LosAnalysisResult.StatusColor.OBSTRUCTED -> Color(0xFFEF4444)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // RF PARAMETERS CONTROL BOX
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Network Interface (নেটওয়ার্ক ইন্টারফেস):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 1: Side-by-Side Frequency 1 & Frequency 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customFreq1Text,
                            onValueChange = {
                                customFreq1Text = it
                                it.toDoubleOrNull()?.let { f -> onFrequencyChange(f) }
                            },
                            label = { Text("১ম ফ্রিকোয়েন্সি (MHz)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).testTag("frequency_input_1"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customFreq2Text,
                            onValueChange = {
                                customFreq2Text = it
                                it.toDoubleOrNull()?.let { f -> onFrequency2Change(f) }
                            },
                            label = { Text("২য় ফ্রিকোয়েন্সি (MHz)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).testTag("frequency_input_2"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 2: Power Output (dBm) & Antenna Gain (dBi)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customTxPowerText,
                            onValueChange = {
                                customTxPowerText = it
                                it.toDoubleOrNull()?.let { p -> onTxPowerChange(p) }
                            },
                            label = { Text("Power Output (dBm)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).testTag("tx_power_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customAntennaGainText,
                            onValueChange = {
                                customAntennaGainText = it
                                it.toDoubleOrNull()?.let { g -> onAntennaGainChange(g) }
                            },
                            label = { Text("Antenna Gain (dBi)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).testTag("antenna_gain_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 3: Rx Sensitivity & Antenna Mast Heights (Site A & Site B)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = customRxSensText,
                            onValueChange = {
                                customRxSensText = it
                                it.toDoubleOrNull()?.let { s -> onRxSensitivityChange(s) }
                            },
                            label = { Text("Rx Sens (dBm)", fontSize = 9.sp) },
                            modifier = Modifier.weight(1f).testTag("rx_sensitivity_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = p1AntennaHeight,
                            onValueChange = onP1AntennaHeightChange,
                            label = { Text("Site 1 Ant (m)", fontSize = 9.sp) },
                            modifier = Modifier.weight(1f).testTag("p1_antenna_height"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = p2AntennaHeight,
                            onValueChange = onP2AntennaHeightChange,
                            label = { Text("Site 2 Ant (m)", fontSize = 9.sp) },
                            modifier = Modifier.weight(1f).testTag("p2_antenna_height"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // CR 6: VISUAL CROSS-SECTION DIGITAL PATH PROFILE CANVAS (Radio Mobile Style)
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ডিজিটাল পাথ প্রোফাইল ভিজ্যুয়ালাইজার (RADIO MOBILE STYLE)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "বাস্তব DEM ভূপ্রাকৃতিক প্রোফাইল",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = Color(0xFF00E5FF)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(Color(0xFF040A14), RoundedCornerShape(12.dp))
                    .border(1.2.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .testTag("digital_path_profile_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
                    val w = size.width
                    val h = size.height

                    val paddingBottom = 28f
                    val paddingTop = 26f
                    val paddingLeft = 36f
                    val paddingRight = 36f

                    val drawW = w - paddingLeft - paddingRight
                    val drawH = h - paddingBottom - paddingTop

                    val siteAX = paddingLeft
                    val siteBX = w - paddingRight

                    // Determine Min & Max Altitude for Dynamic Y-Scale
                    val maxEffectiveGround = terrainSamples.maxOf { it.effectiveGroundMsl }
                    val maxRay = maxOf(elev1 + h1, elev2 + h2)
                    val maxFresnelTop = terrainSamples.maxOf { it.losRayMsl + it.fresnelRadius }

                    val maxY = maxOf(maxEffectiveGround, maxRay, maxFresnelTop) + 12.0
                    val minY = maxOf(0.0, minOf(elev1, elev2, terrainSamples.minOf { it.rawTerrainMsl }) - 8.0)
                    val rangeY = maxOf(10.0, maxY - minY)

                    fun getY(altMeters: Double): Float {
                        val norm = ((altMeters - minY) / rangeY).toFloat()
                        return h - paddingBottom - (norm * drawH)
                    }

                    // Background Grid Lines (Horizontal Altitudes)
                    val gridSteps = 4
                    for (step in 0..gridSteps) {
                        val altLevel = minY + (rangeY * (step.toDouble() / gridSteps))
                        val yPos = getY(altLevel)
                        drawLine(
                            color = Color(0x1F38BDF8),
                            start = Offset(paddingLeft - 8f, yPos),
                            end = Offset(siteBX + 8f, yPos),
                            strokeWidth = 1f
                        )
                        // Left Axis Label
                        drawContext.canvas.nativeCanvas.apply {
                            val p = android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 18f
                                textAlign = android.graphics.Paint.Align.RIGHT
                            }
                            drawText("${altLevel.toInt()}m", paddingLeft - 10f, yPos + 6f, p)
                        }
                    }

                    // 1. CR 6.1 & 6.3: Realistic Ground Profile Polygon with Gradient Fill (Radio Mobile style)
                    val terrainPolygon = Path().apply {
                        moveTo(siteAX, getY(terrainSamples.first().effectiveGroundMsl))
                        for (sample in terrainSamples) {
                            val xPos = siteAX + (sample.fraction.toFloat() * drawW)
                            val yPos = getY(sample.effectiveGroundMsl)
                            lineTo(xPos, yPos)
                        }
                        lineTo(siteBX, h - paddingBottom)
                        lineTo(siteAX, h - paddingBottom)
                        close()
                    }

                    drawPath(
                        path = terrainPolygon,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF334155),
                                Color(0xFF1E293B),
                                Color(0xFF0F172A)
                            ),
                            startY = getY(maxEffectiveGround),
                            endY = h - paddingBottom
                        )
                    )

                    // Stroke Top Terrain Line
                    val terrainContour = Path().apply {
                        moveTo(siteAX, getY(terrainSamples.first().effectiveGroundMsl))
                        for (sample in terrainSamples) {
                            val xPos = siteAX + (sample.fraction.toFloat() * drawW)
                            val yPos = getY(sample.effectiveGroundMsl)
                            lineTo(xPos, yPos)
                        }
                    }
                    drawPath(
                        path = terrainContour,
                        color = Color(0xFF94A3B8),
                        style = Stroke(width = 2.5f)
                    )

                    // 2. 1st Fresnel Zone Envelope (Top & Bottom curves)
                    val fresnelEnvelope = Path().apply {
                        // Top Curve
                        val firstSample = terrainSamples.first()
                        moveTo(siteAX, getY(firstSample.losRayMsl + firstSample.fresnelRadius))
                        for (sample in terrainSamples) {
                            val xPos = siteAX + (sample.fraction.toFloat() * drawW)
                            val yPos = getY(sample.losRayMsl + sample.fresnelRadius)
                            lineTo(xPos, yPos)
                        }
                        // Bottom Curve (reversed)
                        for (sample in terrainSamples.reversed()) {
                            val xPos = siteAX + (sample.fraction.toFloat() * drawW)
                            val yPos = getY(sample.losRayMsl - sample.fresnelRadius)
                            lineTo(xPos, yPos)
                        }
                        close()
                    }
                    val fresnelGlowColor = if (losResult.isLosClear) Color(0x2800E5FF) else Color(0x33EF4444)
                    drawPath(path = fresnelEnvelope, color = fresnelGlowColor)
                    drawPath(
                        path = fresnelEnvelope,
                        color = if (losResult.isLosClear) Color(0x5500E5FF) else Color(0x66EF4444),
                        style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
                    )

                    // 3. Direct Line of Sight (LOS) Ray
                    val mastA_Y = getY(elev1 + h1)
                    val mastB_Y = getY(elev2 + h2)
                    val rayColor = when (losResult.statusColorType) {
                        LosAnalysisResult.StatusColor.CLEAR -> Color(0xFF10B981)
                        LosAnalysisResult.StatusColor.MARGINAL -> Color(0xFFF59E0B)
                        LosAnalysisResult.StatusColor.OBSTRUCTED -> Color(0xFFEF4444)
                    }
                    drawLine(
                        color = rayColor,
                        start = Offset(siteAX, mastA_Y),
                        end = Offset(siteBX, mastB_Y),
                        strokeWidth = 3.5f,
                        cap = StrokeCap.Round
                    )

                    // 4. Antenna Towers (Point 1 and Point 2)
                    val groundA_Y = getY(elev1)
                    val groundB_Y = getY(elev2)

                    // Site A Tower
                    drawLine(color = Color.White, start = Offset(siteAX, groundA_Y), end = Offset(siteAX, mastA_Y), strokeWidth = 3.5f)
                    drawCircle(color = Color(0xFF10B981), radius = 6f, center = Offset(siteAX, mastA_Y))
                    drawCircle(color = Color.White, radius = 2.5f, center = Offset(siteAX, mastA_Y))

                    // Site B Tower
                    drawLine(color = Color.White, start = Offset(siteBX, groundB_Y), end = Offset(siteBX, mastB_Y), strokeWidth = 3.5f)
                    drawCircle(color = Color(0xFFEF4444), radius = 6f, center = Offset(siteBX, mastB_Y))
                    drawCircle(color = Color.White, radius = 2.5f, center = Offset(siteBX, mastB_Y))

                    // 5. Critical Clearance Point Marker (CR 6.2 & 6.3)
                    val critX = siteAX + (criticalSample.fraction.toFloat() * drawW)
                    val critGroundY = getY(criticalSample.effectiveGroundMsl)
                    val critRayY = getY(criticalSample.losRayMsl)

                    // Vertical dashed clearance line
                    drawLine(
                        color = Color(0xFFFFD600),
                        start = Offset(critX, critGroundY),
                        end = Offset(critX, critRayY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                    // Diamond marker at critical ground obstacle
                    drawCircle(color = Color(0xFFFFD600), radius = 5f, center = Offset(critX, critGroundY))

                    // Labels & Annotations
                    drawContext.canvas.nativeCanvas.apply {
                        val p = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 20f
                            isFakeBoldText = true
                            setShadowLayer(4f, 1f, 1f, android.graphics.Color.BLACK)
                        }

                        p.textAlign = android.graphics.Paint.Align.LEFT
                        drawText("${calcResult?.point1?.label?.ifBlank { "Site 1" }} (${h1.toInt()}m)", siteAX, mastA_Y - 10f, p)

                        p.textAlign = android.graphics.Paint.Align.RIGHT
                        drawText("${calcResult?.point2?.label?.ifBlank { "Site 2" }} (${h2.toInt()}m)", siteBX, mastB_Y - 10f, p)

                        // Critical Clearance Tag
                        p.textAlign = android.graphics.Paint.Align.CENTER
                        p.color = if (criticalSample.clearanceMeters >= 0) android.graphics.Color.GREEN else android.graphics.Color.RED
                        val critSign = if (criticalSample.clearanceMeters >= 0) "+" else ""
                        drawText(
                            "Critical: $critSign${String.format(Locale.US, "%.1fm", criticalSample.clearanceMeters)}",
                            critX,
                            minOf(critGroundY, critRayY) - 10f,
                            p
                        )

                        // Bottom Distance Scale
                        p.color = android.graphics.Color.LTGRAY
                        p.textSize = 18f
                        drawText("0 km", siteAX, h - 6f, p)
                        drawText("${String.format(Locale.US, "%.1f km", activeDistKm / 2.0)}", (siteAX + siteBX) / 2f, h - 6f, p)
                        drawText("${String.format(Locale.US, "%.1f km", activeDistKm)}", siteBX, h - 6f, p)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // CR 4: DEDICATED DIGITAL LINK BUDGET CALCULATION TABLE (13 PARAMETERS)
            // =========================================================================
            Card(
                modifier = Modifier.fillMaxWidth().testTag("digital_link_budget_table"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, ElectricCyan.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DIGITAL LINK BUDGET CALCULATION",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = ElectricCyan,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "RMW / Radio Mobile Software RF Formula Engine",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Surface(
                            color = when (budget.linkStatusColor) {
                                LosAnalysisResult.StatusColor.CLEAR -> Color(0x3300E676)
                                LosAnalysisResult.StatusColor.MARGINAL -> Color(0x33FFD600)
                                LosAnalysisResult.StatusColor.OBSTRUCTED -> Color(0x33EF4444)
                            },
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when (budget.linkStatusColor) {
                                    LosAnalysisResult.StatusColor.CLEAR -> NeonGreen
                                    LosAnalysisResult.StatusColor.MARGINAL -> CyberGold
                                    LosAnalysisResult.StatusColor.OBSTRUCTED -> Color(0xFFEF4444)
                                }
                            )
                        ) {
                            Text(
                                text = "MARGIN: ${if (budget.fadeMarginDb >= 0) "+" else ""}${String.format(Locale.US, "%.1f dB", budget.fadeMarginDb)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = when (budget.linkStatusColor) {
                                    LosAnalysisResult.StatusColor.CLEAR -> NeonGreen
                                    LosAnalysisResult.StatusColor.MARGINAL -> CyberGold
                                    LosAnalysisResult.StatusColor.OBSTRUCTED -> Color(0xFFEF4444)
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 13 Parameters Table
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SL", fontSize = 10.sp, fontWeight = FontWeight.Black, color = ElectricCyan, modifier = Modifier.width(24.dp))
                            Text("PARAMETER & DESCRIPTION", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.weight(1.3f))
                            Text("CALCULATED VALUE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeonGreen, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                        }

                        HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))

                        // Render each of the 13 parameters
                        rfBudgetParams.forEachIndexed { index, param ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (index % 2 == 0) Color.Transparent else Color(0xFF0F172A).copy(alpha = 0.4f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = param.sl,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.width(24.dp)
                                )
                                Column(modifier = Modifier.weight(1.3f)) {
                                    Text(
                                        text = param.nameEn,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF1F5F9)
                                    )
                                    Text(
                                        text = param.nameBn,
                                        fontSize = 9.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1.1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = param.valueStr,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = when (param.sl) {
                                            "08" -> if (budget.receivedSignalLevelDbm >= activeRxSensDbm) NeonGreen else Color(0xFFEF4444)
                                            "05" -> ElectricCyan
                                            "06" -> CyberGold
                                            else -> Color.White
                                        },
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = param.unitStr,
                                        fontSize = 9.sp,
                                        color = Color(0xFF64748B),
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Link Quality Assessment Banner
                    Surface(
                        color = when (budget.linkStatusColor) {
                            LosAnalysisResult.StatusColor.CLEAR -> Color(0xFF064E3B)
                            LosAnalysisResult.StatusColor.MARGINAL -> Color(0xFF78350F)
                            LosAnalysisResult.StatusColor.OBSTRUCTED -> Color(0xFF7F1D1D)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (budget.fadeMarginDb >= 15.0) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = budget.linkStatusText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // 3D GOOGLE EARTH & ARCGIS EARTH EXTRACT BUTTONS
            Text(
                text = "গুগল আর্থ ও ARCGIS EARTH এক্সট্র্যাক্ট",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "দুইটি টার্মিনালের ৩ডি লাইন অফ সাইড সরাসরি এক্সট্র্যাক্ট করে দেখুন",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Extract Button for Google Earth & ArcGIS Earth (3D KML)
            Button(
                onClick = {
                    KmlExporter.exportAndLaunchKml(
                        context = context,
                        calcResult = calcResult,
                        p1Elevation = elev1,
                        p1AntennaHeight = h1,
                        p2Elevation = elev2,
                        p2AntennaHeight = h2,
                        frequencyMhz = activeFreq1Mhz,
                        losResult = losResult
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag("export_kml_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Google Earth ও ArcGIS এ 3D Extract করুন (KML)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary Quick 3D Web Launchers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val lat1 = calcResult?.point1?.latitude ?: 23.8103
                        val lon1 = calcResult?.point1?.longitude ?: 90.4125
                        openGoogleEarthWeb(context, lat1, lon1)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Earth 3D Web", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val lat1 = calcResult?.point1?.latitude ?: 23.8103
                        val lon1 = calcResult?.point1?.longitude ?: 90.4125
                        openArcGisEarthWeb(context, lat1, lon1)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Terrain, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ArcGIS 3D Web", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        KmlExporter.shareKmlFile(
                            context = context,
                            calcResult = calcResult,
                            p1Elevation = elev1,
                            p1AntennaHeight = h1,
                            p2Elevation = elev2,
                            p2AntennaHeight = h2,
                            frequencyMhz = activeFreq1Mhz,
                            losResult = losResult
                        )
                    },
                    modifier = Modifier.weight(0.7f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share KML", modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Gemini Maps Obstacle Scanner (Mountains, Buildings & Dense Jungle)
            Button(
                onClick = onScanObstaclesGemini,
                modifier = Modifier.fillMaxWidth().testTag("scan_obstacles_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Forest, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("পাহাড়, বিল্ডিং ও ঘন জঙ্গল স্ক্যান রিপোর্ট (AI Scan)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

private fun openGoogleEarthWeb(context: Context, lat: Double, lon: Double) {
    try {
        val uri = Uri.parse("https://earth.google.com/web/@$lat,$lon,600a,35y,0h,60t,0r")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (_: Exception) {
        val webUri = Uri.parse("https://earth.google.com/web/@$lat,$lon,600a")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

private fun openArcGisEarthWeb(context: Context, lat: Double, lon: Double) {
    try {
        val uri = Uri.parse("https://www.arcgis.com/home/webscene/viewer.html?viewpoint=cam:$lon,$lat,1500;0;45")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: Exception) {
        val webUri = Uri.parse("https://www.arcgis.com/home/webscene/viewer.html")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

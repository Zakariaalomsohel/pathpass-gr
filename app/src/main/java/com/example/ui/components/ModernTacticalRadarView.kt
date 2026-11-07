package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.data.model.CalculationResult
import com.example.data.model.DistanceUnit
import kotlin.math.cos
import kotlin.math.sin

/**
 * Modern Military Pulse-Doppler Tactical PPI (Plan Position Indicator) Radar Scope.
 * Features:
 * 1. True 360° calibrated outer compass/azimuth bezel scale graduated in degrees (000° to 350°).
 * 2. High-contrast Cardinal (N, S, E, W) and Intercardinal (NE, SE, SW, NW) degree markings.
 * 3. Exact Target Bearing Cursor & Laser Line of Bearing (LOB) showing the measured azimuth on the scale.
 * 4. Concentric calibrated tactical range rings with distances in km.
 * 5. High-speed 360° sweeping beam with glowing phosphorescent decay trail.
 * 6. Target acquisition blip with crosshair brackets and on-screen RF telemetry.
 * 7. 4-Corner Digital Tactical Radar HUD status overlays.
 */
@Composable
fun ModernTacticalRadarView(
    calcResult: CalculationResult?,
    p1Elevation: Double?,
    p2Elevation: Double?,
    sweepAngle: Float,
    pulseRadius: Float,
    selectedRangeKm: Double?,
    modifier: Modifier = Modifier
) {
    val distKm = if (calcResult != null) calcResult.distanceMeters / 1000.0 else 15.0
    val rssiEstimate = (-32 - (20.0 * kotlin.math.log10(maxOf(1.0, distKm)))).toInt().coerceIn(-110, -28)
    val dopplerHz = (calcResult?.bearingDegrees?.times(1.6) ?: 120.0).toInt()

    Canvas(modifier = modifier.fillMaxSize().padding(6.dp)) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        // Radar Radii geometry
        val scopeMaxR = minOf(w, h) / 2f - 4f
        val outerBezelR = scopeMaxR
        val scaleRingR = outerBezelR - 10f
        val innerScaleR = scaleRingR - 22f
        val radarScreenR = innerScaleR - 4f

        // 1. Dark CRT Deep Phosphor Radar Scope Background
        drawCircle(
            color = Color(0xFF030C14),
            radius = outerBezelR,
            center = center
        )
        // Subtle radial phosphor green glow
        drawCircle(
            color = Color(0xFF00E676).copy(alpha = 0.05f),
            radius = radarScreenR,
            center = center
        )

        // Heavy Tactical Outer Bezel Rings
        drawCircle(
            color = Color(0xFF1E293B),
            radius = outerBezelR,
            center = center,
            style = Stroke(width = 4f)
        )
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = 0.5f),
            radius = scaleRingR,
            center = center,
            style = Stroke(width = 1.8f)
        )
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = 0.28f),
            radius = innerScaleR,
            center = center,
            style = Stroke(width = 1.2f)
        )

        // 2. FULL 360° CALIBRATED COMPASS & BEARING DEGREE DIAL (স্কেল দ্বারা ডিগ্রি)
        drawContext.canvas.nativeCanvas.apply {
            val degPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(210, 0, 229, 255)
                textSize = 15f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }

            for (deg in 0 until 360 step 2) {
                val degRad = Math.toRadians(deg.toDouble())
                val s = sin(degRad).toFloat()
                val c = cos(degRad).toFloat()

                when {
                    deg % 30 == 0 -> {
                        // Major 30-degree tick
                        val startX = center.x + scaleRingR * s
                        val startY = center.y - scaleRingR * c
                        val endX = center.x + (scaleRingR - 11f) * s
                        val endY = center.y - (scaleRingR - 11f) * c
                        drawLine(
                            color = if (deg == 0) Color(0xFFFF3D00) else Color(0xFF00E5FF),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 2.2f
                        )

                        // Degree Text (e.g. 000°, 030°, 060°...)
                        val textR = scaleRingR - 20f
                        val textX = center.x + textR * s
                        val textY = center.y - textR * c + 5f
                        degPaint.color = when (deg) {
                            0 -> android.graphics.Color.RED
                            90, 180, 270 -> android.graphics.Color.YELLOW
                            else -> android.graphics.Color.argb(230, 0, 229, 255)
                        }
                        degPaint.textSize = if (deg in listOf(0, 90, 180, 270)) 16f else 13f
                        drawText(String.format("%03d°", deg), textX, textY, degPaint)
                    }
                    deg % 10 == 0 -> {
                        // 10-degree intermediate tick
                        val startX = center.x + scaleRingR * s
                        val startY = center.y - scaleRingR * c
                        val endX = center.x + (scaleRingR - 8f) * s
                        val endY = center.y - (scaleRingR - 8f) * c
                        drawLine(
                            color = Color(0xFF00E5FF).copy(alpha = 0.75f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 1.4f
                        )
                    }
                    deg % 5 == 0 -> {
                        // 5-degree tick
                        val startX = center.x + scaleRingR * s
                        val startY = center.y - scaleRingR * c
                        val endX = center.x + (scaleRingR - 5.5f) * s
                        val endY = center.y - (scaleRingR - 5.5f) * c
                        drawLine(
                            color = Color(0xFF00E5FF).copy(alpha = 0.45f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 1.0f
                        )
                    }
                    else -> {
                        // 2-degree fine tick
                        val startX = center.x + scaleRingR * s
                        val startY = center.y - scaleRingR * c
                        val endX = center.x + (scaleRingR - 3f) * s
                        val endY = center.y - (scaleRingR - 3f) * c
                        drawLine(
                            color = Color(0xFF00E5FF).copy(alpha = 0.22f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 0.7f
                        )
                    }
                }
            }
        }

        // Prominent North Red Arrow Indicator
        val nTrianglePath = Path().apply {
            moveTo(center.x, center.y - outerBezelR - 2f)
            lineTo(center.x - 6f, center.y - scaleRingR + 2f)
            lineTo(center.x + 6f, center.y - scaleRingR + 2f)
            close()
        }
        drawPath(nTrianglePath, Color(0xFFFF3D00))

        // Cardinal & Intercardinal Text Markers
        drawContext.canvas.nativeCanvas.apply {
            val cardinalPaint = android.graphics.Paint().apply {
                textSize = 21f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
                setShadowLayer(4f, 1f, 1f, android.graphics.Color.BLACK)
            }
            cardinalPaint.color = android.graphics.Color.RED
            drawText("▲ N", center.x, center.y - radarScreenR + 18f, cardinalPaint)

            cardinalPaint.color = android.graphics.Color.YELLOW
            drawText("S", center.x, center.y + radarScreenR - 8f, cardinalPaint)
            drawText("E", center.x + radarScreenR - 16f, center.y + 7f, cardinalPaint)
            drawText("W", center.x - radarScreenR + 16f, center.y + 7f, cardinalPaint)

            cardinalPaint.textSize = 14f
            cardinalPaint.color = android.graphics.Color.argb(160, 255, 214, 0)
            val diagR = radarScreenR * 0.78f
            val d45 = Math.toRadians(45.0)
            val d135 = Math.toRadians(135.0)
            val d225 = Math.toRadians(225.0)
            val d315 = Math.toRadians(315.0)
            drawText("NE", center.x + (diagR * sin(d45)).toFloat(), center.y - (diagR * cos(d45)).toFloat() + 5f, cardinalPaint)
            drawText("SE", center.x + (diagR * sin(d135)).toFloat(), center.y - (diagR * cos(d135)).toFloat() + 5f, cardinalPaint)
            drawText("SW", center.x + (diagR * sin(d225)).toFloat(), center.y - (diagR * cos(d225)).toFloat() + 5f, cardinalPaint)
            drawText("NW", center.x + (diagR * sin(d315)).toFloat(), center.y - (diagR * cos(d315)).toFloat() + 5f, cardinalPaint)
        }

        // 3. CONCENTRIC RANGE RINGS (দূরত্ব স্কেল)
        val autoKm = when {
            distKm <= 10.0 -> 10.0
            distKm <= 25.0 -> 25.0
            distKm <= 50.0 -> 50.0
            distKm <= 100.0 -> 100.0
            else -> 200.0
        }
        val maxKmRange = selectedRangeKm ?: autoKm
        val ringCount = 4
        val kmPerStep = maxKmRange / ringCount

        for (i in 1..ringCount) {
            val r = radarScreenR * (i.toFloat() / ringCount)
            drawCircle(
                color = Color(0xFF00E676).copy(alpha = if (i == ringCount) 0.35f else 0.18f),
                radius = r,
                center = center,
                style = Stroke(width = if (i == ringCount) 1.5f else 1.0f)
            )
            // Distance tag on North and East axes
            val rangeStr = "${(i * kmPerStep).toInt()}km"
            drawContext.canvas.nativeCanvas.apply {
                val rangePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(170, 0, 230, 118)
                    textSize = 15f
                    isFakeBoldText = true
                }
                drawText(rangeStr, center.x + 5f, center.y - r + 13f, rangePaint)
                drawText(rangeStr, center.x + r - 26f, center.y - 4f, rangePaint)
            }
        }

        // Crosshairs Graticule Axes (000°-180° and 090°-270°)
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = 0.20f),
            start = Offset(center.x, center.y - radarScreenR),
            end = Offset(center.x, center.y + radarScreenR),
            strokeWidth = 1f
        )
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = 0.20f),
            start = Offset(center.x - radarScreenR, center.y),
            end = Offset(center.x + radarScreenR, center.y),
            strokeWidth = 1f
        )

        // Subtle 30° radial index spokes
        for (deg in 0 until 360 step 30) {
            val rad = Math.toRadians(deg.toDouble())
            val spokeX = center.x + radarScreenR * sin(rad).toFloat()
            val spokeY = center.y - radarScreenR * cos(rad).toFloat()
            drawLine(
                color = Color(0xFF00E5FF).copy(alpha = 0.10f),
                start = center,
                end = Offset(spokeX, spokeY),
                strokeWidth = 0.8f
            )
        }

        // 4. ROTATING RADAR SWEEP BEAM with fading phosphor persistence sector
        val sweepRad = Math.toRadians(sweepAngle.toDouble())
        val sweepEnd = Offset(
            center.x + radarScreenR * sin(sweepRad).toFloat(),
            center.y - radarScreenR * cos(sweepRad).toFloat()
        )

        // 40° phosphor trail sector
        val sectorPath = Path().apply {
            moveTo(center.x, center.y)
            val steps = 14
            for (s in 0..steps) {
                val a = sweepAngle - (40f * (1f - s.toFloat() / steps))
                val rRad = Math.toRadians(a.toDouble())
                val px = center.x + radarScreenR * sin(rRad).toFloat()
                val py = center.y - radarScreenR * cos(rRad).toFloat()
                lineTo(px, py)
            }
            close()
        }
        drawPath(sectorPath, Color(0xFF00E676).copy(alpha = 0.12f))

        // Sharp bright leading sweep line
        drawLine(
            color = Color(0xFF00E676),
            start = center,
            end = sweepEnd,
            strokeWidth = 2.4f,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = Color(0xFF00E676),
            radius = 3.5f,
            center = sweepEnd
        )

        // 5. TARGET BEARING SCALE, CURSOR, AND VECTOR LINE (স্কেল দ্বারা বেয়ারিং)
        if (calcResult != null) {
            val bearingDeg = calcResult.bearingDegrees
            val bearingRad = Math.toRadians(bearingDeg)
            val targetFraction = (distKm / maxKmRange).coerceIn(0.12, 0.95).toFloat()
            val targetR = radarScreenR * targetFraction

            val p2X = center.x + targetR * sin(bearingRad).toFloat()
            val p2Y = center.y - targetR * cos(bearingRad).toFloat()
            val targetPos = Offset(p2X, p2Y)

            // Direct Line of Bearing (LOB) laser beam extending all the way out to scaleRingR
            val lobEndX = center.x + scaleRingR * sin(bearingRad).toFloat()
            val lobEndY = center.y - scaleRingR * cos(bearingRad).toFloat()
            drawLine(
                color = Color(0xFFFFD600),
                start = center,
                end = Offset(lobEndX, lobEndY),
                strokeWidth = 2.6f,
                cap = StrokeCap.Round
            )

            // Highlighted Bearing Angle Arc (from North 0° to target bearing)
            val arcRadius = radarScreenR * 0.38f
            val arcRect = Rect(
                center.x - arcRadius,
                center.y - arcRadius,
                center.x + arcRadius,
                center.y + arcRadius
            )
            drawArc(
                color = Color(0xFFFFD600).copy(alpha = 0.45f),
                startAngle = -90f,
                sweepAngle = bearingDeg.toFloat(),
                useCenter = false,
                style = Stroke(width = 2.2f)
            )

            // Antenna Radiation Cone (±12° 3dB beamwidth)
            val beamHalfAngle = Math.toRadians(12.0)
            val beamConePath = Path().apply {
                moveTo(center.x, center.y)
                val coneLen = radarScreenR * 0.96f
                val xLeft = center.x + coneLen * sin(bearingRad - beamHalfAngle).toFloat()
                val yLeft = center.y - coneLen * cos(bearingRad - beamHalfAngle).toFloat()
                val xRight = center.x + coneLen * sin(bearingRad + beamHalfAngle).toFloat()
                val yRight = center.y - coneLen * cos(bearingRad + beamHalfAngle).toFloat()
                lineTo(xLeft, yLeft)
                lineTo(xRight, yRight)
                close()
            }
            drawPath(beamConePath, Color(0xFFFFD600).copy(alpha = 0.08f))

            // Prominent Bearing Index Marker on Outer Scale Ring
            val cursorTip = Offset(
                center.x + (scaleRingR - 1f) * sin(bearingRad).toFloat(),
                center.y - (scaleRingR - 1f) * cos(bearingRad).toFloat()
            )
            val cursorBackRad1 = bearingRad + Math.toRadians(12.0)
            val cursorBackRad2 = bearingRad - Math.toRadians(12.0)
            val cursorP1 = Offset(
                center.x + (scaleRingR + 8f) * sin(cursorBackRad1).toFloat(),
                center.y - (scaleRingR + 8f) * cos(cursorBackRad1).toFloat()
            )
            val cursorP2 = Offset(
                center.x + (scaleRingR + 8f) * sin(cursorBackRad2).toFloat(),
                center.y - (scaleRingR + 8f) * cos(cursorBackRad2).toFloat()
            )
            val cursorPath = Path().apply {
                moveTo(cursorTip.x, cursorTip.y)
                lineTo(cursorP1.x, cursorP1.y)
                lineTo(cursorP2.x, cursorP2.y)
                close()
            }
            drawPath(cursorPath, Color(0xFFFFD600))

            // Reciprocal / Back-bearing dashed reciprocal pointer
            val backBearingRad = Math.toRadians(calcResult.backBearingDegrees)
            val backLobX = center.x + scaleRingR * sin(backBearingRad).toFloat()
            val backLobY = center.y - scaleRingR * cos(backBearingRad).toFloat()
            drawLine(
                color = Color(0xFF00E5FF).copy(alpha = 0.5f),
                start = center,
                end = Offset(backLobX, backLobY),
                strokeWidth = 1.6f
            )

            // Pulsing Observer Wave (P1 Transmitter)
            drawCircle(
                color = Color(0xFF00E676).copy(alpha = 0.4f),
                radius = pulseRadius,
                center = center,
                style = Stroke(width = 2f)
            )
            drawCircle(color = Color(0xFF00E676), radius = 8f, center = center)
            drawCircle(color = Color.Black, radius = 4f, center = center)

            // Tactical Target Blip (P2 Receiver)
            val blipSize = 16f
            drawRect(
                color = Color(0xFFFF3D00),
                topLeft = Offset(targetPos.x - blipSize / 2f, targetPos.y - blipSize / 2f),
                size = Size(blipSize, blipSize),
                style = Stroke(width = 2.2f)
            )
            drawCircle(color = Color(0xFFFF3D00), radius = 6f, center = targetPos)
            drawCircle(color = Color.White, radius = 2.5f, center = targetPos)

            // On-Canvas Telemetry Text Annotations
            drawContext.canvas.nativeCanvas.apply {
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 20f
                    isFakeBoldText = true
                    setShadowLayer(5f, 1f, 1f, android.graphics.Color.BLACK)
                }
                val p1Text = calcResult.point1.label.ifBlank { "TX_OBS" }
                val p2Text = calcResult.point2.label.ifBlank { "RX_TGT" }

                textPaint.color = android.graphics.Color.GREEN
                drawText("[$p1Text]", center.x + 10f, center.y + 22f, textPaint)

                textPaint.color = android.graphics.Color.RED
                drawText("[$p2Text] RSSI: $rssiEstimate dBm", targetPos.x + 12f, targetPos.y - 12f, textPaint)

                // Midpoint bearing & distance readouts
                textPaint.color = android.graphics.Color.YELLOW
                textPaint.textSize = 19f
                val midX = (center.x + targetPos.x) / 2f
                val midY = (center.y + targetPos.y) / 2f
                drawText(
                    "${calcResult.formatDistance(DistanceUnit.KILOMETERS)} @ ${calcResult.bearingRounded}° (${calcResult.bearingCardinal})",
                    midX + 8f,
                    midY - 8f,
                    textPaint
                )
            }
        } else {
            drawCircle(color = Color(0xFF00E5FF), radius = 7f, center = center)
        }

        // 6. DIGITAL TACTICAL HUD PANELS (4 CORNERS)
        drawContext.canvas.nativeCanvas.apply {
            val hudPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(235, 0, 229, 255)
                textSize = 16f
                isFakeBoldText = true
                setShadowLayer(4f, 1f, 1f, android.graphics.Color.BLACK)
            }

            // Top Left: Radar System Spec
            hudPaint.color = android.graphics.Color.argb(240, 0, 229, 255)
            drawText("RADAR: MILITARY 3D PULSE DOPPLER PPI", 12f, 22f, hudPaint)
            hudPaint.textSize = 14f
            hudPaint.color = android.graphics.Color.argb(190, 0, 229, 255)
            drawText("SWEEP: ${sweepAngle.toInt()}° | PRF: 1200Hz | MAX R: ${maxKmRange.toInt()}km", 12f, 40f, hudPaint)

            // Top Right: Bearing & Azimuth Scale Summary
            hudPaint.textAlign = android.graphics.Paint.Align.RIGHT
            hudPaint.textSize = 16f
            hudPaint.color = android.graphics.Color.argb(255, 255, 214, 0)
            val bStr = calcResult?.let { "BEARING: ${it.bearingRounded}° (${it.bearingCardinal})" } ?: "BEARING: ---°"
            drawText(bStr, w - 12f, 22f, hudPaint)
            hudPaint.textSize = 14f
            hudPaint.color = android.graphics.Color.argb(200, 255, 214, 0)
            val milStr = calcResult?.let { "${it.bearingMils} MILS | BACK: ${it.backBearingRounded}°" } ?: "-- MILS"
            drawText(milStr, w - 12f, 40f, hudPaint)

            // Bottom Left: Range & LOS
            hudPaint.textAlign = android.graphics.Paint.Align.LEFT
            hudPaint.textSize = 15f
            hudPaint.color = android.graphics.Color.argb(230, 0, 230, 118)
            val rangeStr = calcResult?.let { "RANGE: ${it.formatDistance(DistanceUnit.KILOMETERS)}" } ?: "RANGE: -- km"
            drawText(rangeStr, 12f, h - 28f, hudPaint)
            hudPaint.textSize = 13f
            hudPaint.color = android.graphics.Color.argb(180, 203, 213, 225)
            val elevStr = "TX: ${p1Elevation?.let { "${it.toInt()}m" } ?: "N/A"} | RX: ${p2Elevation?.let { "${it.toInt()}m" } ?: "N/A"}"
            drawText(elevStr, 12f, h - 10f, hudPaint)

            // Bottom Right: Doppler Shift & Status
            hudPaint.textAlign = android.graphics.Paint.Align.RIGHT
            hudPaint.textSize = 15f
            hudPaint.color = android.graphics.Color.argb(220, 0, 229, 255)
            drawText("Δf: +${dopplerHz}Hz | RSSI: ${rssiEstimate}dBm", w - 12f, h - 28f, hudPaint)
            hudPaint.textSize = 13f
            hudPaint.color = android.graphics.Color.argb(180, 203, 213, 225)
            drawText("360° BEARING SCALE CALIBRATED", w - 12f, h - 10f, hudPaint)
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.MaterialTheme

/**
 * Draws a colorful modern tactical military map background with glowing
 * topographical contour lines, latitude/longitude graticules, and compass bearings.
 */
@Composable
fun ParchmentMapBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bgColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Dynamic Tactical Background
            drawRect(
                color = bgColor,
                size = size
            )

            // 2. Vibrant colored landmass / tactical zone polygons
            val zone1Color = primaryColor.copy(alpha = 0.12f)
            val zonePath1 = Path().apply {
                moveTo(width * 0.15f, 0f)
                cubicTo(width * 0.35f, height * 0.05f, width * 0.25f, height * 0.18f, width * 0.45f, height * 0.15f)
                cubicTo(width * 0.65f, height * 0.12f, width * 0.85f, height * 0.22f, width * 0.95f, height * 0.18f)
                lineTo(width, height * 0.18f)
                lineTo(width, 0f)
                close()

                moveTo(0f, height * 0.65f)
                cubicTo(width * 0.3f, height * 0.60f, width * 0.2f, height * 0.80f, width * 0.55f, height * 0.75f)
                cubicTo(width * 0.8f, height * 0.72f, width * 0.7f, height * 0.95f, width * 0.9f, height * 0.90f)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            drawPath(zonePath1, zone1Color)

            val zone2Color = secondaryColor.copy(alpha = 0.10f)
            val zonePath2 = Path().apply {
                moveTo(width * 0.35f, height * 0.25f)
                cubicTo(width * 0.6f, height * 0.22f, width * 0.75f, height * 0.35f, width * 0.85f, height * 0.45f)
                cubicTo(width * 0.7f, height * 0.55f, width * 0.4f, height * 0.48f, width * 0.35f, height * 0.25f)
                close()
            }
            drawPath(zonePath2, zone2Color)

            // 3. Glowing Topographical Contour Curves
            val contourColor = primaryColor.copy(alpha = 0.18f)
            val stroke1 = Stroke(width = 1.2f)
            for (i in 1..7) {
                val yOffset = height * (0.11f * i)
                val curvePath = Path().apply {
                    moveTo(0f, yOffset)
                    cubicTo(
                        width * 0.25f, yOffset - 35f + (i * 10f),
                        width * 0.65f, yOffset + 45f - (i * 8f),
                        width, yOffset - 12f
                    )
                }
                drawPath(curvePath, contourColor, style = stroke1)
            }

            // 4. Graticule Grid Lines (Latitude & Longitude)
            val gridLineColor = secondaryColor.copy(alpha = 0.15f)

            // Longitude parallels
            val xSteps = 6
            for (i in 0..xSteps) {
                val x = width * (i.toFloat() / xSteps)
                drawLine(
                    color = gridLineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
            }

            // Latitude parallels
            val ySteps = 10
            for (i in 0..ySteps) {
                val y = height * (i.toFloat() / ySteps)
                drawLine(
                    color = gridLineColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            // 5. Golden Rhumb Bearing Lines (Compass Rose Center)
            val rhumbColor = tertiaryColor.copy(alpha = 0.15f)
            val center1 = Offset(width * 0.82f, height * 0.32f)
            for (deg in 0 until 360 step 30) {
                val rad = Math.toRadians(deg.toDouble())
                val end = Offset(
                    center1.x + (width * 0.4f * kotlin.math.cos(rad)).toFloat(),
                    center1.y + (width * 0.4f * kotlin.math.sin(rad)).toFloat()
                )
                drawLine(rhumbColor, center1, end, strokeWidth = 1f)
            }

            // 6. Tactical Border Frame
            val borderMargin = 6f
            drawRect(
                color = secondaryColor.copy(alpha = 0.35f),
                topLeft = Offset(borderMargin, borderMargin),
                size = Size(width - borderMargin * 2, height - borderMargin * 2),
                style = Stroke(width = 1.5f)
            )
            drawRect(
                color = tertiaryColor.copy(alpha = 0.25f),
                topLeft = Offset(borderMargin + 4f, borderMargin + 4f),
                size = Size(width - (borderMargin + 4f) * 2, height - (borderMargin + 4f) * 2),
                style = Stroke(width = 0.8f)
            )
        }

        // Foreground content on top of tactical map canvas
        content()
    }
}

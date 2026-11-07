package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassRoseView(
    bearingDegrees: Float,
    backBearingDegrees: Float,
    modifier: Modifier = Modifier,
    showBackBearing: Boolean = true
) {
    val animatedBearing by animateFloatAsState(
        targetValue = bearingDegrees,
        animationSpec = tween(durationMillis = 600),
        label = "bearingAnim"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val errorColor = MaterialTheme.colorScheme.error

    Box(
        modifier = modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 12f

            // Outer dial circle
            drawCircle(
                color = surfaceColor,
                radius = radius,
                center = center
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.5f),
                radius = radius,
                center = center,
                style = Stroke(width = 3f)
            )

            // Inner reference circle
            drawCircle(
                color = primaryColor.copy(alpha = 0.2f),
                radius = radius * 0.7f,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // Degree tick marks (every 10 deg, major at 30 deg)
            for (angle in 0 until 360 step 10) {
                val isMajor = angle % 30 == 0
                val tickLength = if (isMajor) radius * 0.14f else radius * 0.07f
                val strokeW = if (isMajor) 2.5f else 1.2f
                val tickColor = if (isMajor) primaryColor else primaryColor.copy(alpha = 0.4f)

                val rad = Math.toRadians(angle.toDouble())
                val startX = center.x + (radius - tickLength) * sin(rad).toFloat()
                val startY = center.y - (radius - tickLength) * cos(rad).toFloat()
                val endX = center.x + radius * sin(rad).toFloat()
                val endY = center.y - radius * cos(rad).toFloat()

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }

            // Cardinal Labels (N, E, S, W)
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = radius * 0.18f
                    isFakeBoldText = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                // N (Red)
                paint.color = android.graphics.Color.RED
                drawText("N", center.x, center.y - radius * 0.72f, paint)

                paint.color = android.graphics.Color.LTGRAY
                // E
                drawText("E", center.x + radius * 0.78f, center.y + radius * 0.06f, paint)
                // S
                drawText("S", center.x, center.y + radius * 0.85f, paint)
                // W
                drawText("W", center.x - radius * 0.78f, center.y + radius * 0.06f, paint)
            }

            // Draw Forward Bearing Arrow (Rotating)
            rotate(degrees = animatedBearing, pivot = center) {
                // Forward pointer (Emerald / Green arrow pointing up to bearing)
                val arrowPath = Path().apply {
                    moveTo(center.x, center.y - radius * 0.88f)
                    lineTo(center.x - radius * 0.12f, center.y - radius * 0.45f)
                    lineTo(center.x + radius * 0.12f, center.y - radius * 0.45f)
                    close()
                }
                drawPath(path = arrowPath, color = primaryColor)

                // Forward needle stem
                drawLine(
                    color = primaryColor,
                    start = center,
                    end = Offset(center.x, center.y - radius * 0.88f),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )

                // Back bearing reciprocal needle stem (Amber / Tertiary)
                if (showBackBearing) {
                    val backPath = Path().apply {
                        moveTo(center.x, center.y + radius * 0.88f)
                        lineTo(center.x - radius * 0.10f, center.y + radius * 0.50f)
                        lineTo(center.x + radius * 0.10f, center.y + radius * 0.50f)
                        close()
                    }
                    drawPath(path = backPath, color = tertiaryColor)
                    drawLine(
                        color = tertiaryColor,
                        start = center,
                        end = Offset(center.x, center.y + radius * 0.88f),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Center pivot pin
            drawCircle(
                color = onSurface,
                radius = radius * 0.08f,
                center = center
            )
            drawCircle(
                color = surfaceColor,
                radius = radius * 0.04f,
                center = center
            )
        }
    }
}

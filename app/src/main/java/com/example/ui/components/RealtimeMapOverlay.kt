package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculator.RealtimeTrackingState
import com.example.ui.theme.CyberGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonGreen
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws the real-time user location overlay onto a map canvas:
 * - Breadcrumbs path trail
 * - Pulsing GPS radar beacon
 * - Directional heading arrow
 */
fun DrawScope.drawRealtimeLocationOverlay(
    trackingState: RealtimeTrackingState,
    pulseAnim: Float,
    lonToX: (Double) -> Float,
    latToY: (Double) -> Float
) {
    val crumbs = trackingState.breadcrumbs
    if (crumbs.size > 1) {
        val trailPath = Path()
        trailPath.moveTo(lonToX(crumbs[0].second), latToY(crumbs[0].first))
        for (i in 1 until crumbs.size) {
            trailPath.lineTo(lonToX(crumbs[i].second), latToY(crumbs[i].first))
        }

        // Draw glowing neon trail
        drawPath(
            path = trailPath,
            color = Color(0x6600E5FF),
            style = Stroke(width = 6f)
        )
        drawPath(
            path = trailPath,
            color = Color(0xFF00E5FF),
            style = Stroke(width = 2.5f)
        )
    }

    // User's current location on map
    val userX = lonToX(trackingState.longitude)
    val userY = latToY(trackingState.latitude)

    // Pulsing radar ripple halo
    drawCircle(
        color = Color(0xFF00E676).copy(alpha = (1f - pulseAnim) * 0.45f),
        radius = 16f + (pulseAnim * 24f),
        center = Offset(userX, userY)
    )

    // Outer beacon ring
    drawCircle(
        color = Color(0xFF00E676),
        radius = 12f,
        center = Offset(userX, userY),
        style = Stroke(width = 2.5f)
    )

    // Inner bright core
    drawCircle(
        color = Color(0xFF00E5FF),
        radius = 6f,
        center = Offset(userX, userY)
    )

    // Heading arrow (if moving or has bearing)
    if (trackingState.speedKmh > 1.0f || trackingState.headingDegrees != 0f) {
        val rad = Math.toRadians((trackingState.headingDegrees - 90.0))
        val arrowLen = 22f
        val tipX = userX + (arrowLen * cos(rad)).toFloat()
        val tipY = userY + (arrowLen * sin(rad)).toFloat()

        drawLine(
            color = Color(0xFFFFD600),
            start = Offset(userX, userY),
            end = Offset(tipX, tipY),
            strokeWidth = 3f
        )
        drawCircle(
            color = Color(0xFFFFD600),
            radius = 3.5f,
            center = Offset(tipX, tipY)
        )
    }
}

/**
 * Tactical Real-time Telemetry HUD Card
 */
@Composable
fun RealtimeTelemetryHud(
    trackingState: RealtimeTrackingState,
    onToggleTracking: (Boolean) -> Unit,
    onCenterOnUser: () -> Unit,
    onClearTrail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "beacon_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Surface(
        color = Color(0xE60F172A),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(14.dp))
            .testTag("realtime_telemetry_hud")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Tracking toggle and live status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsing LED indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (trackingState.isTracking) NeonGreen else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (trackingState.isTracking) "LIVE GPS TRACKING" else "GPS STANDBY",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = if (trackingState.isTracking) NeonGreen else Color(0xFF94A3B8)
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (trackingState.isTracking) "ON" else "OFF",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (trackingState.isTracking) ElectricCyan else Color.Gray,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Switch(
                        checked = trackingState.isTracking,
                        onCheckedChange = onToggleTracking,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonGreen,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.testTag("tracking_toggle_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Real-time Coordinates & Live Military Grid Display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LIVE GR (6-FIG)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                    Text(
                        text = trackingState.liveResult?.gr6 ?: "------",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "MAP SHEET (1:50k)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberGold
                    )
                    Text(
                        text = trackingState.liveResult?.mapSheet50k ?: "-- --/--",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ACCURACY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                    Text(
                        text = "±${String.format(Locale.US, "%.1f", trackingState.accuracyMeters)}m",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Telemetry Gauges Row: Speed, Altitude, Heading, Traveled Distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryItem(
                    label = "SPEED",
                    value = String.format(Locale.US, "%.1f km/h", trackingState.speedKmh),
                    icon = Icons.Default.Speed,
                    tint = ElectricCyan
                )
                TelemetryItem(
                    label = "HEADING",
                    value = String.format(Locale.US, "%.0f°", trackingState.headingDegrees),
                    icon = Icons.AutoMirrored.Filled.AltRoute,
                    tint = NeonGreen
                )
                TelemetryItem(
                    label = "DISTANCE",
                    value = String.format(Locale.US, "%.2f km", trackingState.totalDistanceMeters / 1000.0),
                    icon = Icons.Default.GpsFixed,
                    tint = Color(0xFFFF6D00)
                )
            }

            // Action Buttons Row: Center & Clear Trail
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    color = Color(0xFF334155),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clickable { onCenterOnUser() }
                        .padding(end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "Center",
                            tint = ElectricCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Center View",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Surface(
                    color = Color(0x33EF4444),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFEF4444)),
                    modifier = Modifier.clickable { onClearTrail() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clear Trail",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
            )
        }
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
    }
}

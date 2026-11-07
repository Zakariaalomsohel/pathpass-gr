package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CompassCalibration
import com.example.ui.components.CameraAntennaAlignmentView
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.sensor.CompassData
import com.example.data.sensor.CompassSensorManager
import com.example.ui.MainViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun StandaloneCompassScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var isCameraAlignmentMode by remember { mutableStateOf(false) }

    if (isCameraAlignmentMode) {
        val calcResult by viewModel.calculationResult.collectAsState()
        val targetAz = calcResult?.bearingDegrees?.toFloat()
        CameraAntennaAlignmentView(
            onClose = { isCameraAlignmentMode = false },
            targetAzimuth = targetAz,
            targetElevation = 2.0f
        )
        return
    }

    var compassData by remember {
        mutableStateOf(
            CompassData(
                azimuthDegrees = 0f,
                cardinalDirection = "N",
                mils = 0,
                pitchDegrees = 0f,
                rollDegrees = 0f,
                accuracy = 3
            )
        )
    }

    // Target Heading Lock state
    var lockedHeading by remember { mutableStateOf<Float?>(null) }

    // Register sensor listener
    DisposableEffect(Unit) {
        val sensorManager = CompassSensorManager(context)
        val job = sensorManager.getCompassDataFlow().onEach { data ->
            compassData = data
        }.launchIn(scope)

        onDispose {
            job.cancel()
        }
    }

    val currentGps by viewModel.currentGps.collectAsState()
    val p1Elevation by viewModel.p1Elevation.collectAsState()

    val animatedAzimuth by animateFloatAsState(
        targetValue = compassData.azimuthDegrees,
        animationSpec = tween(durationMillis = 150),
        label = "compassRotation"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Explore,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TACTICAL PRISMATIC COMPASS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
                Text(
                    text = "High-Precision Azimuth & NATO Mils Readout",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Accuracy badge
            Box(
                modifier = Modifier
                    .background(
                        if (compassData.accuracy >= 2) primaryColor.copy(alpha = 0.2f) else tertiaryColor.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (compassData.accuracy >= 2) "SENSOR HIGH" else "CALIBRATE 8",
                    color = if (compassData.accuracy >= 2) primaryColor else tertiaryColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // CAMERA ALIGNMENT & ANTENNA MEASUREMENT BUTTON (Section 7)
        Button(
            onClick = { isCameraAlignmentMode = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("camera_alignment_mode_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CAMERA ALIGNMENT / অ্যান্টেনা অ্যালাইনমেন্ট",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Big Digital Degrees Display Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Digital Degree
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "CURRENT HEADING",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "%03d".format(compassData.azimuthDegrees.roundToInt() % 360),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Black,
                                color = primaryColor
                            )
                            Text(
                                text = "° ${compassData.cardinalDirection}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = tertiaryColor,
                                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                            )
                        }
                    }

                    // NATO Mils & Tilt
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "NATO MILS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "%04d Mils".format(compassData.mils),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = secondaryColor
                        )

                        Text(
                            text = "Tilt: P ${compassData.pitchDegrees.roundToInt()}° / R ${compassData.rollDegrees.roundToInt()}°",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // If locked heading active
                if (lockedHeading != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val diff = (compassData.azimuthDegrees - lockedHeading!! + 540) % 360 - 180
                    val dirText = if (diff > 1) "Turn Left ${diff.roundToInt()}°" else if (diff < -1) "Turn Right ${(-diff).roundToInt()}°" else "ON COURSE (সঠিক ট্র্যাকে আছেন)"
                    val dirColor = if (kotlin.math.abs(diff) <= 2) primaryColor else tertiaryColor

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(dirColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, dirColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Locked Target: ${lockedHeading!!.toInt()}° | $dirText",
                            color = dirColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Large Graphical Prismatic Compass Dial
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617))
                    ),
                    shape = CircleShape
                )
                .border(3.dp, primaryColor.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - 16f

                // Fixed top sighting index mark (Red indicator at top center)
                drawLine(
                    color = Color.Red,
                    start = Offset(center.x, center.y - radius - 12f),
                    end = Offset(center.x, center.y - radius + 14f),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )

                // Rotating dial under compass needle/sensor heading
                rotate(degrees = -animatedAzimuth, pivot = center) {
                    // Outer graduation ring
                    drawCircle(
                        color = Color(0xFF334155),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2f)
                    )

                    // Inner circle
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.15f),
                        radius = radius * 0.72f,
                        center = center,
                        style = Stroke(width = 1.5f)
                    )

                    // Degree tick marks (every 2 degrees, major every 10 and 30)
                    for (angle in 0 until 360 step 2) {
                        val is30 = angle % 30 == 0
                        val is10 = angle % 10 == 0
                        val tickLen = when {
                            is30 -> radius * 0.16f
                            is10 -> radius * 0.10f
                            else -> radius * 0.05f
                        }
                        val strokeW = if (is30) 3f else if (is10) 1.8f else 1f
                        val tickColor = if (is30) primaryColor else if (is10) Color.White.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.4f)

                        val rad = Math.toRadians(angle.toDouble())
                        val sX = center.x + (radius - tickLen) * sin(rad).toFloat()
                        val sY = center.y - (radius - tickLen) * cos(rad).toFloat()
                        val eX = center.x + radius * sin(rad).toFloat()
                        val eY = center.y - radius * cos(rad).toFloat()

                        drawLine(
                            color = tickColor,
                            start = Offset(sX, sY),
                            end = Offset(eX, eY),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }

                    // Cardinal labels & Numbers
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 34f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }

                        // N (Red)
                        paint.color = android.graphics.Color.RED
                        paint.textSize = 42f
                        drawText("N", center.x, center.y - radius * 0.75f, paint)

                        paint.color = android.graphics.Color.WHITE
                        paint.textSize = 36f
                        // E
                        drawText("E", center.x + radius * 0.78f, center.y + 12f, paint)
                        // S
                        drawText("S", center.x, center.y + radius * 0.82f, paint)
                        // W
                        drawText("W", center.x - radius * 0.78f, center.y + 12f, paint)

                        // 30 degree numbers
                        paint.textSize = 20f
                        paint.color = android.graphics.Color.LTGRAY
                        listOf(30, 60, 120, 150, 210, 240, 300, 330).forEach { deg ->
                            val rNum = radius * 0.58f
                            val rad = Math.toRadians(deg.toDouble())
                            val x = center.x + rNum * sin(rad).toFloat()
                            val y = center.y - rNum * cos(rad).toFloat() + 7f
                            drawText("$deg", x, y, paint)
                        }
                    }

                    // North Needle Arrow inside dial
                    val needlePath = Path().apply {
                        moveTo(center.x, center.y - radius * 0.70f)
                        lineTo(center.x - 14f, center.y)
                        lineTo(center.x + 14f, center.y)
                        close()
                    }
                    drawPath(path = needlePath, color = Color.Red)

                    val southNeedlePath = Path().apply {
                        moveTo(center.x, center.y + radius * 0.70f)
                        lineTo(center.x - 14f, center.y)
                        lineTo(center.x + 14f, center.y)
                        close()
                    }
                    drawPath(path = southNeedlePath, color = Color(0xFF64748B))

                    // Locked Heading Bug (Yellow index)
                    if (lockedHeading != null) {
                        val bugRad = Math.toRadians(lockedHeading!!.toDouble())
                        val bugX = center.x + (radius - 6f) * sin(bugRad).toFloat()
                        val bugY = center.y - (radius - 6f) * cos(bugRad).toFloat()
                        drawCircle(color = Color.Yellow, radius = 7f, center = Offset(bugX, bugY))
                    }
                }

                // Stationary Center crosshair & Pivot
                drawCircle(color = primaryColor, radius = 10f, center = center)
                drawCircle(color = Color.Black, radius = 5f, center = center)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Actions: Lock Heading, Copy Bearing, Live GPS info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    lockedHeading = if (lockedHeading == null) compassData.azimuthDegrees else null
                },
                modifier = Modifier.weight(1f).testTag("lock_heading_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (lockedHeading == null) primaryColor else tertiaryColor
                )
            ) {
                Icon(
                    imageVector = if (lockedHeading == null) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (lockedHeading == null) "হেডিং লক (Lock)" else "আনলক (Unlock)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            OutlinedButton(
                onClick = {
                    val report = "Compass Azimuth: ${compassData.azimuthDegrees.roundToInt()}° (${compassData.cardinalDirection}) | NATO Mils: ${compassData.mils} Mils"
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Heading", report))
                    Toast.makeText(context, "ডিগ্রী ক্লিপবোর্ডে কপি হয়েছে", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f).testTag("copy_bearing_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Degree", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Position and Sea Level Elevation at current observer position
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Terrain,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "লাইভ জিপিএস অবস্থান ও সমুদ্রপৃষ্ঠের উচ্চতা",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Text(
                        text = if (currentGps != null) {
                            "Lat: ${currentGps!!.latitude}, Lon: ${currentGps!!.longitude}"
                        } else "জিপিএস অবস্থান লোড হচ্ছে...",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = if (p1Elevation != null) "সমুদ্রপৃষ্ঠ থেকে উচ্চতা: ${"%.1f".format(p1Elevation)} মিটার (${"%.1f".format(p1Elevation!! * 3.28084)} ফুট)" else "উচ্চতা সক্রিয় হচ্ছে...",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = tertiaryColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

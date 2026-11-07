package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.sensor.CompassData
import com.example.data.sensor.CompassSensorManager
import com.example.ui.theme.CyberGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonGreen
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.Locale
import kotlin.math.abs

/**
 * Camera-based Antenna Alignment and Angular Measurement AR HUD.
 * Integrates Mobile Camera with Compass, Gyroscope & Accelerometer sensors to measure
 * real-time Horizontal Azimuth and Vertical Elevation angles with optical crosshair.
 */
@Composable
fun CameraAntennaAlignmentView(
    onClose: () -> Unit,
    targetAzimuth: Float? = null,
    targetElevation: Float? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera permission check & request
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Compass Sensor integration
    val sensorManager = remember { CompassSensorManager(context) }
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

    DisposableEffect(sensorManager) {
        val job = sensorManager.getCompassDataFlow()
            .onEach { data -> compassData = data }
            .launchIn(kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main))

        onDispose {
            job.cancel()
        }
    }

    // Target configuration states
    var customTargetAzimuth by remember { mutableFloatStateOf(targetAzimuth ?: 45.0f) }
    var customTargetElevation by remember { mutableFloatStateOf(targetElevation ?: 2.5f) }
    var showTargetDialog by remember { mutableStateOf(false) }
    var isAngleLocked by remember { mutableStateOf(false) }
    var lockedAzimuth by remember { mutableFloatStateOf(0f) }
    var lockedElevation by remember { mutableFloatStateOf(0f) }

    val currentAzimuth = if (isAngleLocked) lockedAzimuth else compassData.azimuthDegrees
    // Pitch: when holding phone upright facing the horizon, pitch is 0°. Upward tilt is positive, downward is negative.
    val currentPitch = if (isAngleLocked) lockedElevation else -compassData.pitchDegrees

    val deltaAzimuth = ((currentAzimuth - customTargetAzimuth + 540) % 360) - 180
    val deltaPitch = currentPitch - customTargetElevation

    val isAzimuthAligned = abs(deltaAzimuth) < 2.5f
    val isElevationAligned = abs(deltaPitch) < 2.5f
    val isOnTarget = isAzimuthAligned && isElevationAligned

    val infiniteTransition = rememberInfiniteTransition(label = "target_lock")
    val lockPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_antenna_alignment_view")
    ) {
        if (hasCameraPermission) {
            // CameraX Live Preview View
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission missing prompt
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xF0131E33)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, ElectricCyan)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ক্যামেরা পারমিশন প্রয়োজন",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "অ্যান্টেনা অ্যালাইনমেন্ট ও কোণ পরিমাপের জন্য ক্যামেরা অ্যাক্সেস চালু করুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                        ) {
                            Text("পারমিশন দিন", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // TACTICAL AR HUD OVERLAY
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // 1. Center Tactical Crosshair
            val crosshairColor = if (isOnTarget) NeonGreen else ElectricCyan
            val reticleRadius = 40f * (if (isOnTarget) lockPulse else 1f)

            // Center circle
            drawCircle(
                color = crosshairColor.copy(alpha = 0.85f),
                radius = reticleRadius,
                center = Offset(cx, cy),
                style = Stroke(width = if (isOnTarget) 3.5f else 2f)
            )

            // Inner dot
            drawCircle(
                color = if (isOnTarget) NeonGreen else CyberGold,
                radius = 4f,
                center = Offset(cx, cy)
            )

            // Crosshair tick marks
            drawLine(
                color = crosshairColor,
                start = Offset(cx - reticleRadius - 30f, cy),
                end = Offset(cx - reticleRadius - 6f, cy),
                strokeWidth = 2.5f
            )
            drawLine(
                color = crosshairColor,
                start = Offset(cx + reticleRadius + 6f, cy),
                end = Offset(cx + reticleRadius + 30f, cy),
                strokeWidth = 2.5f
            )
            drawLine(
                color = crosshairColor,
                start = Offset(cx, cy - reticleRadius - 30f),
                end = Offset(cx, cy - reticleRadius - 6f),
                strokeWidth = 2.5f
            )
            drawLine(
                color = crosshairColor,
                start = Offset(cx, cy + reticleRadius + 6f),
                end = Offset(cx, cy + reticleRadius + 30f),
                strokeWidth = 2.5f
            )

            // 2. Artificial Horizon / Elevation Ladder Line
            val pitchOffset = currentPitch * 10f
            val horizonY = cy + pitchOffset
            if (horizonY in 50f..(h - 100f)) {
                drawLine(
                    color = CyberGold.copy(alpha = 0.7f),
                    start = Offset(cx - 120f, horizonY),
                    end = Offset(cx + 120f, horizonY),
                    strokeWidth = 2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f))
                )
            }

            // 3. Top Azimuth Tape Ribbon
            val tapeY = 85f
            drawLine(
                color = Color(0xFF00E5FF).copy(alpha = 0.6f),
                start = Offset(cx - 160f, tapeY),
                end = Offset(cx + 160f, tapeY),
                strokeWidth = 2f
            )
            // Center indicator caret
            val caretPath = Path().apply {
                moveTo(cx, tapeY + 12f)
                lineTo(cx - 8f, tapeY + 2f)
                lineTo(cx + 8f, tapeY + 2f)
                close()
            }
            drawPath(path = caretPath, color = NeonGreen)
        }

        // TOP CONTROLS & REAL-TIME ANGLE READOUTS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            // Header Bar: Close & Mode title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xCC0F172A),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CAMERA ANTENNA ALIGNMENT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = ElectricCyan
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Target setup button
                    Surface(
                        color = Color(0xCC0F172A),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberGold),
                        modifier = Modifier.clickable { showTargetDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Target", tint = CyberGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("TARGET", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberGold)
                        }
                    }

                    // Lock angle toggle
                    Surface(
                        color = if (isAngleLocked) Color(0xFFEF4444).copy(alpha = 0.85f) else Color(0xCC0F172A),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAngleLocked) Color.Red else Color.Gray),
                        modifier = Modifier.clickable {
                            if (!isAngleLocked) {
                                lockedAzimuth = compassData.azimuthDegrees
                                lockedElevation = -compassData.pitchDegrees
                                isAngleLocked = true
                            } else {
                                isAngleLocked = false
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isAngleLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Lock",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // PRIMARY DIGITAL READOUT HUD CARDS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Azimuth Card (Horizontal Angle)
                Surface(
                    color = Color(0xEE0A1120),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, if (isAzimuthAligned) NeonGreen else ElectricCyan),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("AZIMUTH (অনুভূমিক)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                            Text(compassData.cardinalDirection, fontSize = 10.sp, fontWeight = FontWeight.Black, color = CyberGold)
                        }
                        Text(
                            text = String.format(Locale.US, "%.1f°", currentAzimuth),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            text = "Target: ${"%.1f°".format(customTargetAzimuth)} (Δ: ${if (deltaAzimuth > 0) "+" else ""}${"%.1f°".format(deltaAzimuth)})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isAzimuthAligned) NeonGreen else Color(0xFF94A3B8)
                        )
                    }
                }

                // Elevation Card (Vertical Angle / Pitch)
                Surface(
                    color = Color(0xEE0A1120),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, if (isElevationAligned) NeonGreen else CyberGold),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ELEVATION (উল্লম্ব)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberGold)
                            Text(if (currentPitch >= 0) "UP" else "DOWN", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeonGreen)
                        }
                        Text(
                            text = String.format(Locale.US, "%+.1f°", currentPitch),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            text = "Target: %+.1f° (Δ: %+.1f°)".format(customTargetElevation, deltaPitch),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isElevationAligned) NeonGreen else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // BOTTOM ALIGNMENT GUIDANCE BANNER
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "ON TARGET" OR DIRECTIONAL ARROW GUIDANCE
            Surface(
                color = if (isOnTarget) Color(0xF0064E3B) else Color(0xF01E293B),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (isOnTarget) NeonGreen else Color(0xFF38BDF8)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isOnTarget) Icons.Default.CheckCircle else Icons.Default.NearMe,
                            contentDescription = null,
                            tint = if (isOnTarget) NeonGreen else ElectricCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isOnTarget) "🎯 ON TARGET (লক / সঠিক অ্যালাইনমেন্ট)"
                                else if (!isAzimuthAligned && deltaAzimuth < 0) "◄ বামে ঘুরুন (Turn Left ${"%.1f°".format(abs(deltaAzimuth))})"
                                else if (!isAzimuthAligned && deltaAzimuth > 0) "ডানে ঘুরুন (Turn Right ${"%.1f°".format(abs(deltaAzimuth))}) ►"
                                else if (!isElevationAligned && deltaPitch < 0) "▲ উপরে তুলুন (Tilt Up ${"%.1f°".format(abs(deltaPitch))})"
                                else "▼ নিচে নামান (Tilt Down ${"%.1f°".format(abs(deltaPitch))})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isOnTarget) NeonGreen else Color.White
                            )
                            Text(
                                text = if (isOnTarget) "অ্যান্টেনা এবং ক্রসহেয়ার লক্ষ্যবিন্দুর সাথে সম্পূর্ণ অ্যালাইন্ড।"
                                else "নির্দিষ্ট Azimuth ও Elevation কোণে অ্যান্টেনা সমন্বয় করুন।",
                                fontSize = 10.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }

                    // Tolerance Pill
                    Surface(
                        color = Color(0x3300E5FF),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, ElectricCyan)
                    ) {
                        Text(
                            text = "±2.5° TOL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // TARGET INPUT DIALOG
        if (showTargetDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { showTargetDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131E33)),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, CyberGold)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "টার্গেট অ্যালাইনমেন্ট কোণ নির্ধারণ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyberGold
                            )
                            IconButton(onClick = { showTargetDialog = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        var azText by remember { mutableStateOf(customTargetAzimuth.toString()) }
                        var elText by remember { mutableStateOf(customTargetElevation.toString()) }

                        OutlinedTextField(
                            value = azText,
                            onValueChange = {
                                azText = it
                                it.toFloatOrNull()?.let { v -> customTargetAzimuth = (v % 360f + 360f) % 360f }
                            },
                            label = { Text("Target Azimuth (ডিগ্রী °: ০-৩৬০°)", fontSize = 11.sp, color = ElectricCyan) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = elText,
                            onValueChange = {
                                elText = it
                                it.toFloatOrNull()?.let { v -> customTargetElevation = v.coerceIn(-90f, 90f) }
                            },
                            label = { Text("Target Elevation Angle (উল্লম্ব কোণ °: -৯০° থেকে +৯০°)", fontSize = 11.sp, color = CyberGold) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showTargetDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("নিশ্চিত করুন (Set Target)", color = Color(0xFF0F172A), fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

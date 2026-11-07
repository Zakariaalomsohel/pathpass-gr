package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculator.BangladeshMapData
import com.example.calculator.GeodeticCalculator
import com.example.calculator.MilitaryGridCalculator
import com.example.calculator.RealtimeLocationManager
import com.example.ui.components.ParchmentMapBackground
import com.example.ui.components.drawRealtimeLocationOverlay
import com.example.ui.theme.CyberGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonGreen
import java.util.Locale

@Composable
fun MilitaryMapViewerScreen(
    currentLat: Double,
    currentLon: Double,
    onCoordinateSelected: (Double, Double) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val locationManager = remember { RealtimeLocationManager.getInstance(context) }
    val trackingState by locationManager.trackingState.collectAsState()

    // Selected Pin Coordinates (default to current active lat/lon)
    var selectedLat by remember { mutableDoubleStateOf(currentLat) }
    var selectedLon by remember { mutableDoubleStateOf(currentLon) }
    var showBreadcrumbs by remember { mutableStateOf(true) }

    // Pulsing radar animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_pulse"
    )

    // Computed Military Grid Result for selected location
    val militaryResult = remember(selectedLat, selectedLon) {
        MilitaryGridCalculator.calculate(selectedLat, selectedLon)
    }

    // Distance and bearing from user's live position to selected pin
    val distanceMeters = remember(selectedLat, selectedLon, trackingState.latitude, trackingState.longitude) {
        GeodeticCalculator.calculateVincentyDistance(
            trackingState.latitude, trackingState.longitude, selectedLat, selectedLon
        )
    }
    val bearingDegrees = remember(selectedLat, selectedLon, trackingState.latitude, trackingState.longitude) {
        GeodeticCalculator.calculateBearing(
            trackingState.latitude, trackingState.longitude, selectedLat, selectedLon
        )
    }

    // Map bounds in degrees
    val minMapLon = 87.8
    val maxMapLon = 93.0
    val minMapLat = 20.3
    val maxMapLat = 26.9

    ParchmentMapBackground {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("map_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "BANGLADESH MILITARY MAP",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFFF8FAFC)
                        )
                    )
                    Text(
                        text = "1:250k & 1:50k Topo Grid with Real-time GPS",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = ElectricCyan
                        )
                    )
                }

                // Snap to User Live Location
                Surface(
                    color = Color(0xFF0284C7),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable {
                            selectedLat = trackingState.latitude
                            selectedLon = trackingState.longitude
                        }
                        .padding(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "My Pos",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }

            // Quick Cantonment selector bar with vibrant colors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BangladeshMapData.KEY_LOCATIONS.forEach { loc ->
                    val isSelected = kotlin.math.abs(selectedLat - loc.lat) < 0.01 && kotlin.math.abs(selectedLon - loc.lon) < 0.01
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) Color(0xFF10B981) else Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) NeonGreen else Color(0xFF334155)
                        ),
                        modifier = Modifier.clickable {
                            selectedLat = loc.lat
                            selectedLon = loc.lon
                        }
                    ) {
                        Text(
                            text = loc.name.substringBefore(" "),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Interactive Tactical Map Canvas with Real-time GPS Overlay
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .background(Color(0xFF0E1726), RoundedCornerShape(12.dp))
                    .testTag("bangladesh_map_canvas")
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                val w = size.width
                                val h = size.height

                                val lonNorm = (tapOffset.x / w).coerceIn(0f, 1f)
                                val latNorm = 1f - (tapOffset.y / h).coerceIn(0f, 1f)

                                val clickedLon = minMapLon + lonNorm * (maxMapLon - minMapLon)
                                val clickedLat = minMapLat + latNorm * (maxMapLat - minMapLat)

                                selectedLat = Math.round(clickedLat * 1000000.0) / 1000000.0
                                selectedLon = Math.round(clickedLon * 1000000.0) / 1000000.0
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    fun lonToX(lon: Double): Float =
                        ((lon - minMapLon) / (maxMapLon - minMapLon) * w).toFloat()

                    fun latToY(lat: Double): Float =
                        ((1.0 - (lat - minMapLat) / (maxMapLat - minMapLat)) * h).toFloat()

                    if (false) {
                        // SATELLITE MODE RENDERING
                        // 1. Deep Space Cosmic Background
                        drawRect(Color(0xFF060D1A), size = size)

                        // 2. Bay of Bengal Oceanic Depths
                        val bayPath = Path().apply {
                            moveTo(lonToX(88.0), latToY(21.9))
                            lineTo(lonToX(92.4), latToY(20.5))
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(bayPath, Color(0xFF03223F))

                        // 3. Bangladesh Satellite Landmass with Lush Forest Tint
                        val borderPath = Path()
                        val borderPts = BangladeshMapData.BANGLADESH_BORDER_COORDS
                        if (borderPts.isNotEmpty()) {
                            borderPath.moveTo(lonToX(borderPts[0].first), latToY(borderPts[0].second))
                            for (i in 1 until borderPts.size) {
                                borderPath.lineTo(lonToX(borderPts[i].first), latToY(borderPts[i].second))
                            }
                            borderPath.close()

                            // Satellite terrain fill: deep emerald-olive
                            drawPath(borderPath, Color(0xFF0F261C))
                            // International border line
                            drawPath(borderPath, Color(0x9900E5FF), style = Stroke(width = 1.8f))
                        }

                        // 4. Sundarbans Mangrove Wetland Canopy (South-West)
                        val sundarbansPath = Path().apply {
                            moveTo(lonToX(88.95), latToY(22.45))
                            cubicTo(lonToX(89.4), latToY(22.3), lonToX(89.9), latToY(22.25), lonToX(90.1), latToY(22.15))
                            lineTo(lonToX(89.8), latToY(21.65))
                            lineTo(lonToX(89.05), latToY(21.75))
                            close()
                        }
                        drawPath(sundarbansPath, Color(0x44059669))

                        // 5. Chittagong Hill Tracts Relief (South-East Ridges)
                        val chtPath = Path().apply {
                            moveTo(lonToX(91.85), latToY(23.7))
                            cubicTo(lonToX(92.1), latToY(23.2), lonToX(92.3), latToY(22.2), lonToX(92.2), latToY(21.4))
                            lineTo(lonToX(92.6), latToY(21.4))
                            cubicTo(lonToX(92.7), latToY(22.4), lonToX(92.5), latToY(23.4), lonToX(92.3), latToY(23.8))
                            close()
                        }
                        drawPath(chtPath, Color(0x33A16207))

                        // 6. Major Accurate River Network (Blue luminous water)
                        // Jamuna / Brahmaputra
                        val jamuna = Path().apply {
                            moveTo(lonToX(89.75), latToY(25.9))
                            cubicTo(lonToX(89.65), latToY(25.1), lonToX(89.70), latToY(24.4), lonToX(89.80), latToY(23.85))
                        }
                        drawPath(jamuna, Color(0xFF38BDF8), style = Stroke(width = 3.6f, cap = StrokeCap.Round))

                        // Padma / Ganges
                        val padma = Path().apply {
                            moveTo(lonToX(88.25), latToY(24.45))
                            cubicTo(lonToX(89.1), latToY(24.0), lonToX(89.80), latToY(23.85), lonToX(90.55), latToY(23.25))
                        }
                        drawPath(padma, Color(0xFF38BDF8), style = Stroke(width = 3.8f, cap = StrokeCap.Round))

                        // Lower Meghna Estuary to Bay of Bengal
                        val meghna = Path().apply {
                            moveTo(lonToX(90.95), latToY(24.3))
                            cubicTo(lonToX(90.7), latToY(23.8), lonToX(90.55), latToY(23.25), lonToX(90.65), latToY(22.05))
                        }
                        drawPath(meghna, Color(0xFF0284C7), style = Stroke(width = 4.4f, cap = StrokeCap.Round))

                        // Karnaphuli River (Chittagong)
                        val karnaphuli = Path().apply {
                            moveTo(lonToX(92.2), latToY(22.55))
                            lineTo(lonToX(91.8), latToY(22.25))
                        }
                        drawPath(karnaphuli, Color(0xFF38BDF8), style = Stroke(width = 2.4f, cap = StrokeCap.Round))

                        // 7. Satellite Graticule Grid (faint orbital coordinates)
                        for (lonDeg in 88..93) {
                            val x = lonToX(lonDeg.toDouble())
                            drawLine(color = Color(0x2200E5FF), start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 0.8f)
                        }
                        for (latDeg in 21..27) {
                            val y = latToY(latDeg.toDouble())
                            drawLine(color = Color(0x2200E5FF), start = Offset(0f, y), end = Offset(w, y), strokeWidth = 0.8f)
                        }

                        // 8. Cantonment Satellite Target Markers
                        BangladeshMapData.KEY_LOCATIONS.forEach { loc ->
                            val px = lonToX(loc.lon)
                            val py = latToY(loc.lat)
                            drawCircle(color = Color(0xFFFFD600), radius = 4f, center = Offset(px, py))
                            drawCircle(color = Color(0x88FFD600), radius = 8f, center = Offset(px, py), style = Stroke(width = 1f))
                        }
                    } else {
                        // TOPO OFFLINE MAP RENDERING
                        // 1. Draw 1:250k Degree Sheet Grids (1° x 1° boundaries)
                        for (lonDeg in 88..93) {
                            val x = lonToX(lonDeg.toDouble())
                            drawLine(
                                color = Color(0x3300E5FF),
                                start = Offset(x, 0f),
                                end = Offset(x, h),
                                strokeWidth = 1.0f
                            )
                        }
                        for (latDeg in 21..27) {
                            val y = latToY(latDeg.toDouble())
                            drawLine(
                                color = Color(0x3300E5FF),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1.0f
                            )
                        }

                        // 2. Draw 1:50k Sub-sheet guidelines (every 15' = 0.25°)
                        for (step in 0..20) {
                            val subLat = 21.0 + step * 0.25
                            if (subLat <= 26.75) {
                                val y = latToY(subLat)
                                drawLine(
                                    color = Color(0x1800E5FF),
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 0.5f
                                )
                            }
                        }
                        for (step in 0..18) {
                            val subLon = 88.0 + step * 0.25
                            if (subLon <= 92.75) {
                                val x = lonToX(subLon)
                                drawLine(
                                    color = Color(0x1800E5FF),
                                    start = Offset(x, 0f),
                                    end = Offset(x, h),
                                    strokeWidth = 0.5f
                                )
                            }
                        }

                        // 3. Draw Bangladesh Border Contour
                        val borderPath = Path()
                        val borderPts = BangladeshMapData.BANGLADESH_BORDER_COORDS
                        if (borderPts.isNotEmpty()) {
                            borderPath.moveTo(lonToX(borderPts[0].first), latToY(borderPts[0].second))
                            for (i in 1 until borderPts.size) {
                                borderPath.lineTo(lonToX(borderPts[i].first), latToY(borderPts[i].second))
                            }
                            borderPath.close()

                            // Tactical fill with dark emerald tint
                            drawPath(borderPath, Color(0x2800E676))
                            // Vibrant glowing border line
                            drawPath(borderPath, Color(0xCC00E5FF), style = Stroke(width = 2.2f))
                        }

                        // 4. Draw 1:250k Sheet Label Markers
                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(140, 255, 215, 0)
                            textSize = 28f
                            isFakeBoldText = true
                        }
                        BangladeshMapData.BANGLADESH_SHEETS_250K.forEach { sheet ->
                            val centerX = lonToX((sheet.minLon + sheet.maxLon) / 2.0)
                            val centerY = latToY((sheet.minLat + sheet.maxLat) / 2.0)
                            drawContext.canvas.nativeCanvas.drawText(
                                sheet.sheetCode,
                                centerX - 24f,
                                centerY + 10f,
                                textPaint
                            )
                        }

                        // 5. Draw Key Reference Cantonment Dots
                        BangladeshMapData.KEY_LOCATIONS.forEach { loc ->
                            val px = lonToX(loc.lon)
                            val py = latToY(loc.lat)
                            drawCircle(
                                color = Color(0xFFFFD600),
                                radius = 3.5f,
                                center = Offset(px, py)
                            )
                        }
                    }

                    // 6. REAL-TIME USER LOCATION OVERLAY (Pulsing radar, breadcrumbs, heading)
                    if (showBreadcrumbs) {
                        drawRealtimeLocationOverlay(
                            trackingState = trackingState,
                            pulseAnim = pulseAnim,
                            lonToX = ::lonToX,
                            latToY = ::latToY
                        )
                    }

                    // 7. Draw Selected Tactical Crosshair Pin (Crimson / Coral)
                    val selX = lonToX(selectedLon)
                    val selY = latToY(selectedLat)

                    drawCircle(
                        color = Color(0xFFFF5252),
                        radius = 14f,
                        center = Offset(selX, selY),
                        style = Stroke(width = 2.5f)
                    )
                    drawCircle(
                        color = Color(0xFFFF5252),
                        radius = 4f,
                        center = Offset(selX, selY)
                    )
                    drawLine(
                        color = Color(0xFFFF5252),
                        start = Offset(selX - 22f, selY),
                        end = Offset(selX + 22f, selY),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color(0xFFFF5252),
                        start = Offset(selX, selY - 22f),
                        end = Offset(selX, selY + 22f),
                        strokeWidth = 2f
                    )
                }

                // Top Left Overlay Badge
                Surface(
                    color = Color(0xCC0B132B),
                    shape = RoundedCornerShape(bottomEnd = 10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LIVE GPS | TOPO 1:50k",
                            color = if (trackingState.isTracking) NeonGreen else Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tactical Result HUD Card for Selected Pin
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xF2141E33)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, ElectricCyan.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "GR: ",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonGreen
                                )
                                Text(
                                    text = militaryResult.gr6,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "Map Sheet: ${militaryResult.mapSheet50k}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberGold
                            )
                        }

                        // Use in Calculator Button
                        Button(
                            onClick = {
                                onCoordinateSelected(selectedLat, selectedLon)
                                onBack()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("apply_coords_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "USE IN CALC",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Secondary Info Row: Lat/Lon and Geodetic Distance & Bearing from Live Location
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Lat: ${String.format(Locale.US, "%.5f", selectedLat)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE2E8F0)
                        )
                        Text(
                            text = "Lon: ${String.format(Locale.US, "%.5f", selectedLon)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE2E8F0)
                        )
                        Text(
                            text = "উচ্চতা: ${militaryResult.elevationInfo.formattedMeters}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                        Text(
                            text = "Range: ${GeodeticCalculator.formatDistance(distanceMeters)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                        Text(
                            text = "Az: ${String.format(Locale.US, "%.0f°", bearingDegrees)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberGold
                        )
                    }
                }
            }
        }
    }
}

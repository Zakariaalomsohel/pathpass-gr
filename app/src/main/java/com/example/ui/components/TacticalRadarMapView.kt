package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.CachedMapAreaEntity
import com.example.data.model.CalculationResult
import com.example.data.model.DistanceUnit
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.MyLocation
import com.example.data.model.OfflineCacheProgress
import com.example.data.model.OfflineMapManager
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class RadarMapMode(val labelBn: String, val shortName: String) {
    GOOGLE_EARTH("Google Earth (গুগল আর্থ)", "Google Earth"),
    MODERN_RADAR("Modern Radar View (আধুনিক রেডার)", "Modern Radar"),
    ARCGIS_EARTH("ArcGIS Earth (আর্কজিআইএস আর্থ)", "ArcGIS Earth"),
    OFFLINE_MAP("Offline Map 25km (অফলাইন ম্যাপ)", "Offline Map")
}

@Composable
fun TacticalRadarMapView(
    calcResult: CalculationResult?,
    p1Elevation: Double?,
    p2Elevation: Double?,
    isOfflineCached: Boolean = false,
    isCachingMap: Boolean = false,
    cachingProgress: OfflineCacheProgress? = null,
    activeCachedArea: CachedMapAreaEntity? = null,
    onToggleOfflineCache: (Boolean) -> Unit = {},
    onCacheVisibleArea: () -> Unit = {},
    onClearOfflineCache: () -> Unit = {},
    modifier: Modifier = Modifier,
    onOpenInGoogleMaps: () -> Unit = {}
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val sweepTransition = rememberInfiniteTransition(label = "sweep")
    val sweepAngle by sweepTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    var currentMode by remember { mutableStateOf(RadarMapMode.GOOGLE_EARTH) }
    var selectedRadarRangeKm by remember { mutableStateOf<Double?>(null) }
    var offlineZoom by remember { mutableIntStateOf(12) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TACTICAL RADAR & MAP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Text(
                        text = "Geodetic Line-of-Sight & Offline GIS Cache",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status chip
                Box(
                    modifier = Modifier
                        .background(
                            if (isOfflineCached) Color(0xFF10B981).copy(alpha = 0.2f) else primaryColor.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isOfflineCached) "OFFLINE READY" else "ONLINE / GPS",
                        color = if (isOfflineCached) Color(0xFF10B981) else primaryColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // VIEW MODE BUTTON DISPLAY STYLE (Stylized Tactical Switcher)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VIEW MODE DISPLAY STYLE:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Google Earth 3D View
                    FilterChip(
                        selected = currentMode == RadarMapMode.GOOGLE_EARTH,
                        onClick = { currentMode = RadarMapMode.GOOGLE_EARTH },
                        leadingIcon = {
                            Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(13.dp))
                        },
                        label = { Text("Google Earth", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0D9488),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )

                    // 2. Modern Radar View (Radio Link Path Analysis)
                    FilterChip(
                        selected = currentMode == RadarMapMode.MODERN_RADAR,
                        onClick = { currentMode = RadarMapMode.MODERN_RADAR },
                        leadingIcon = {
                            Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(13.dp))
                        },
                        label = { Text("Modern Radar", fontSize = 10.sp, fontWeight = FontWeight.Black) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryColor,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )

                    // 3. ArcGIS Earth 3D View
                    FilterChip(
                        selected = currentMode == RadarMapMode.ARCGIS_EARTH,
                        onClick = { currentMode = RadarMapMode.ARCGIS_EARTH },
                        leadingIcon = {
                            Icon(Icons.Default.Apartment, contentDescription = null, modifier = Modifier.size(13.dp))
                        },
                        label = { Text("ArcGIS Earth", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF7C3AED),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )

                    // 4. Offline Map (25 km Area)
                    FilterChip(
                        selected = currentMode == RadarMapMode.OFFLINE_MAP,
                        onClick = { currentMode = RadarMapMode.OFFLINE_MAP },
                        leadingIcon = {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(13.dp))
                        },
                        label = { Text("Offline Map", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF059669),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }

                // Range Scale Bar - displayed when Modern Radar mode is selected
                if (currentMode == RadarMapMode.MODERN_RADAR) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RADAR RANGE SCALE (দূরত্ব স্কেল):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val rangeOptions = listOf(
                                null to "AUTO",
                                10.0 to "10km",
                                25.0 to "25km",
                                50.0 to "50km",
                                100.0 to "100km"
                            )
                            for ((rangeVal, label) in rangeOptions) {
                                val isSelected = selectedRadarRangeKm == rangeVal
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) primaryColor else Color(0xFF1E293B))
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF00E5FF) else Color(0xFF334155),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { selectedRadarRangeKm = rangeVal }
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // MAIN MAXIMIZED VISUALIZER CANVAS (Doppler Radar, Satellite View, or Offline 25km Map)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .background(Color(0xFF040B14), RoundedCornerShape(14.dp))
                    .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                when (currentMode) {
                    RadarMapMode.GOOGLE_EARTH,
                    RadarMapMode.ARCGIS_EARTH -> {
                        TacticalSatelliteMapView(
                            calcResult = calcResult,
                            p1Elevation = p1Elevation,
                            p2Elevation = p2Elevation,
                            mode = currentMode,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    RadarMapMode.MODERN_RADAR -> {
                        ModernTacticalRadarView(
                            calcResult = calcResult,
                            p1Elevation = p1Elevation,
                            p2Elevation = p2Elevation,
                            sweepAngle = sweepAngle,
                            pulseRadius = pulseRadius,
                            selectedRangeKm = selectedRadarRangeKm,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    RadarMapMode.OFFLINE_MAP -> {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            val p1Lat = calcResult?.point1?.latitude ?: 23.8103
                            val p1Lon = calcResult?.point1?.longitude ?: 90.4125
                            val p2Lat = calcResult?.point2?.latitude ?: p1Lat
                            val p2Lon = calcResult?.point2?.longitude ?: p1Lon

                            val centerLat = (p1Lat + p2Lat) / 2.0
                            val centerLon = (p1Lon + p2Lon) / 2.0

                            val centerTileX = OfflineMapManager.lonToTileX(centerLon, offlineZoom)
                            val centerTileY = OfflineMapManager.latToTileY(centerLat, offlineZoom)

                            val tileSize = 256
                            val canvasCenterX = w / 2f
                            val canvasCenterY = h / 2f

                            for (dx in -1..1) {
                                for (dy in -1..1) {
                                    val tx = centerTileX + dx
                                    val ty = centerTileY + dy

                                    val bmp = OfflineMapManager.getCachedTileBitmap(context, offlineZoom, tx, ty)
                                    val posX = canvasCenterX + (dx * tileSize) - (tileSize / 2f)
                                    val posY = canvasCenterY + (dy * tileSize) - (tileSize / 2f)

                                    if (bmp != null) {
                                        val imageBitmap = bmp.asImageBitmap()
                                        drawImage(
                                            image = imageBitmap,
                                            dstOffset = IntOffset(posX.toInt(), posY.toInt()),
                                            dstSize = IntSize(tileSize, tileSize)
                                        )
                                    } else {
                                        drawRect(
                                            color = Color(0xFF0F172A),
                                            topLeft = Offset(posX, posY),
                                            size = androidx.compose.ui.geometry.Size(tileSize.toFloat(), tileSize.toFloat())
                                        )
                                        drawRect(
                                            color = Color(0xFF334155),
                                            topLeft = Offset(posX, posY),
                                            size = androidx.compose.ui.geometry.Size(tileSize.toFloat(), tileSize.toFloat()),
                                            style = Stroke(1f)
                                        )
                                    }
                                }
                            }

                            // Tactical Grid
                            val gridStep = 75f
                            var gx = 0f
                            while (gx < w) {
                                drawLine(color = Color(0x3338BDF8), start = Offset(gx, 0f), end = Offset(gx, h), strokeWidth = 1f)
                                gx += gridStep
                            }
                            var gy = 0f
                            while (gy < h) {
                                drawLine(color = Color(0x3338BDF8), start = Offset(0f, gy), end = Offset(w, gy), strokeWidth = 1f)
                                gy += gridStep
                            }

                            if (calcResult != null) {
                                val p1Pos = Offset(w * 0.3f, h * 0.7f)
                                val p2Pos = Offset(w * 0.7f, h * 0.3f)

                                drawLine(
                                    color = tertiaryColor,
                                    start = p1Pos,
                                    end = p2Pos,
                                    strokeWidth = 3.5f,
                                    cap = StrokeCap.Round
                                )

                                drawCircle(color = primaryColor, radius = 10f, center = p1Pos)
                                drawCircle(color = Color.Black, radius = 5f, center = p1Pos)

                                drawCircle(color = errorColor, radius = 10f, center = p2Pos)
                                drawCircle(color = Color.White, radius = 5f, center = p2Pos)

                                drawContext.canvas.nativeCanvas.apply {
                                    val p = android.graphics.Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = 22f
                                        isFakeBoldText = true
                                        setShadowLayer(4f, 2f, 2f, android.graphics.Color.BLACK)
                                    }
                                    val p1MapText = calcResult.point1.label.ifBlank { "SITE A" }
                                    val p2MapText = calcResult.point2.label.ifBlank { "SITE B" }
                                    p.color = android.graphics.Color.GREEN
                                    drawText("$p1MapText (${"%.3f".format(p1Lat)}, ${"%.3f".format(p1Lon)})", p1Pos.x + 14f, p1Pos.y + 8f, p)

                                    p.color = android.graphics.Color.RED
                                    drawText("$p2MapText (${"%.3f".format(p2Lat)}, ${"%.3f".format(p2Lon)})", p2Pos.x - 120f, p2Pos.y - 14f, p)

                                    p.color = android.graphics.Color.YELLOW
                                    p.textSize = 20f
                                    val midX = (p1Pos.x + p2Pos.x) / 2f
                                    val midY = (p1Pos.y + p2Pos.y) / 2f
                                    drawText("${calcResult.formatDistance(DistanceUnit.KILOMETERS)} @ ${calcResult.bearingRounded}°", midX + 8f, midY - 8f, p)
                                }
                            }

                            drawContext.canvas.nativeCanvas.apply {
                                val statusPaint = android.graphics.Paint().apply {
                                    color = if (isOfflineCached) android.graphics.Color.GREEN else android.graphics.Color.CYAN
                                    textSize = 20f
                                    isFakeBoldText = true
                                    setShadowLayer(3f, 1f, 1f, android.graphics.Color.BLACK)
                                }
                                val statusText = if (isOfflineCached) "⚡ OFFLINE 25KM MAP ACTIVE" else "📡 25KM OFFLINE TOPO MAP"
                                drawText(statusText, 14f, 26f, statusPaint)

                                statusPaint.textSize = 16f
                                statusPaint.color = android.graphics.Color.LTGRAY
                                drawText("Center: ${"%.3f".format(centerLat)}°N, ${"%.3f".format(centerLon)}°E | 25km Coverage", 14f, h - 10f, statusPaint)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Elevation and Coordinates stats bar
            if (calcResult != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceColor, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${calcResult.point1.label.ifBlank { "P1" }} Elevation",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (p1Elevation != null) "${"%.1f".format(p1Elevation)} m" else "N/A",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Δ Elevation",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val delta = if (p1Elevation != null && p2Elevation != null) p2Elevation - p1Elevation else null
                        val deltaPrefix = if (delta != null && delta > 0) "+" else ""
                        Text(
                            text = if (delta != null) "$deltaPrefix${"%.1f".format(delta)} m" else "--",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = tertiaryColor
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${calcResult.point2.label.ifBlank { "P2" }} Elevation",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (p2Elevation != null) "${"%.1f".format(p2Elevation)} m" else "N/A",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = secondaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Google Maps Launcher Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (calcResult != null) {
                            openGoogleMapsRoute(
                                context,
                                calcResult.point1.latitude,
                                calcResult.point1.longitude,
                                calcResult.point2.latitude,
                                calcResult.point2.longitude
                            )
                        } else {
                            Toast.makeText(context, "দয়া করে প্রথমে হিসাব করুন", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("map_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = "Navigate in Google Maps", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Google Maps", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = {
                        if (calcResult != null) {
                            openSatelliteView(context, calcResult.point2.latitude, calcResult.point2.longitude)
                        } else {
                            Toast.makeText(context, "লোকেশন দিন", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Map, contentDescription = "Satellite View", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Satellite Target", fontSize = 13.sp)
                }
            }
        }
    }
}

private fun openGoogleMapsRoute(context: Context, lat1: Double, lon1: Double, lat2: Double, lon2: Double) {
    try {
        val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$lat1,$lon1&destination=$lat2,$lon2&travelmode=driving")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback to browser or any map app
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    } catch (_: Exception) {
        val webUri = Uri.parse("https://www.google.com/maps/dir/$lat1,$lon1/$lat2,$lon2")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

private fun openSatelliteView(context: Context, lat: Double, lon: Double) {
    try {
        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(Target)&z=16")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: Exception) {
        val webUri = Uri.parse("https://maps.google.com/?q=$lat,$lon&t=k")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

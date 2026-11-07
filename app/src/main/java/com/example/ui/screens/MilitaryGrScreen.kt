package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.calculator.ElevationInfo
import com.example.calculator.ElevationService
import com.example.calculator.MilitaryGridCalculator
import com.example.calculator.MilitaryGridResult
import com.example.calculator.RealtimeLocationManager
import com.example.calculator.ReverseGridResult
import com.example.ui.components.ColorfulCoordinateField
import com.example.ui.components.ColorfulResultDisplay
import com.example.ui.components.ColorfulTacticalButton
import com.example.ui.components.ConversionModeTab
import com.example.ui.components.ParchmentMapBackground
import com.example.ui.components.RealtimeTelemetryHud
import com.example.ui.components.ReverseResultDisplay
import com.example.ui.theme.CyberGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonGreen
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun MilitaryGrScreen(
    initialLat: Double = 24.582254,
    initialLon: Double = 90.141237,
    onNavigateToMapViewer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Mode: Forward (Lat/Lon -> GR) vs Reverse (GR & Sheet -> Lat/Lon)
    var isReverseMode by remember { mutableStateOf(false) }

    // Real-time Location Manager Service instance
    val locationManager = remember { RealtimeLocationManager.getInstance(context) }
    val trackingState by locationManager.trackingState.collectAsState()

    // Forward Mode Inputs: Latitude & Longitude
    var latitudeText by remember { mutableStateOf(String.format(Locale.US, "%.6f", initialLat)) }
    var longitudeText by remember { mutableStateOf(String.format(Locale.US, "%.6f", initialLon)) }

    // Forward Mode Result
    var calculationResult by remember {
        mutableStateOf<MilitaryGridResult?>(
            MilitaryGridCalculator.calculate(initialLat, initialLon)
        )
    }

    // Reverse Mode Inputs: Map Sheet & GR
    var sheetInputText by remember { mutableStateOf("78 L/2") }
    var grInputText by remember { mutableStateOf("146184") }

    // Reverse Mode Result
    var reverseResult by remember {
        mutableStateOf<ReverseGridResult?>(
            MilitaryGridCalculator.reverseCalculate("78 L/2", "146184")
        )
    }

    var locationStatusMessage by remember { mutableStateOf<String?>(null) }
    var showTelemetryHud by remember { mutableStateOf(true) }
    var customElevationText by remember { mutableStateOf("") }
    var isManualElevationOverride by remember { mutableStateOf(false) }

    // Fetch high-res elevation when coordinates change
    fun updateHighResElevation(lat: Double, lon: Double, isReverse: Boolean) {
        coroutineScope.launch {
            val highResEle = ElevationService.getElevation(lat, lon)
            if (!isReverse) {
                if (!isManualElevationOverride) {
                    calculationResult = calculationResult?.copy(elevationInfo = highResEle)
                    customElevationText = String.format(Locale.US, "%.2f", highResEle.elevationMeters)
                }
            } else {
                reverseResult = reverseResult?.copy(elevationInfo = highResEle)
            }
        }
    }

    // Sync when initialLat/Lon changes externally (e.g. from Map Viewer)
    LaunchedEffect(initialLat, initialLon) {
        latitudeText = String.format(Locale.US, "%.6f", initialLat)
        longitudeText = String.format(Locale.US, "%.6f", initialLon)
        isManualElevationOverride = false
        val result = MilitaryGridCalculator.calculate(initialLat, initialLon)
        calculationResult = result
        customElevationText = String.format(Locale.US, "%.2f", result.elevationInfo.elevationMeters)
        updateHighResElevation(initialLat, initialLon, isReverse = false)
    }

    // When real-time tracking is active, auto-update forward AND reverse inputs & results
    LaunchedEffect(trackingState.latitude, trackingState.longitude, trackingState.isTracking) {
        if (trackingState.isTracking) {
            latitudeText = String.format(Locale.US, "%.6f", trackingState.latitude)
            longitudeText = String.format(Locale.US, "%.6f", trackingState.longitude)
            isManualElevationOverride = false
            val gpsEle = if (trackingState.altitudeMeters > 0) {
                ElevationService.fromGpsAltitude(trackingState.altitudeMeters)
            } else null
            val result = MilitaryGridCalculator.calculate(trackingState.latitude, trackingState.longitude, gpsEle)
            calculationResult = result
            customElevationText = String.format(Locale.US, "%.2f", result.elevationInfo.elevationMeters)
            updateHighResElevation(trackingState.latitude, trackingState.longitude, isReverse = false)

            // Auto-update GR & Sheet for Reverse Mode during Live GPS Tracking
            sheetInputText = result.mapSheet50k
            grInputText = result.gr6
            reverseResult = MilitaryGridCalculator.reverseCalculate(result.mapSheet50k, result.gr6)
            updateHighResElevation(trackingState.latitude, trackingState.longitude, isReverse = true)
        }
    }

    // Calculate Forward (Lat/Lon -> GR & Sheet & Elevation)
    fun performCalculation(forceAutoElevation: Boolean = false) {
        val lat = latitudeText.toDoubleOrNull()
        val lon = longitudeText.toDoubleOrNull()
        if (lat == null || lon == null) {
            return
        }
        val customOverride: ElevationInfo? = if (!forceAutoElevation && isManualElevationOverride) {
            val customElev = customElevationText.trim().toDoubleOrNull()
            customElev?.let { elevMeters ->
                ElevationInfo(
                    elevationMeters = elevMeters,
                    elevationFeet = elevMeters * 3.28084,
                    formattedMeters = String.format(Locale.US, "%.2f m", elevMeters),
                    formattedFeet = String.format(Locale.US, "%.1f ft", elevMeters * 3.28084),
                    source = "Custom Entry"
                )
            }
        } else {
            null
        }

        val result = MilitaryGridCalculator.calculate(lat, lon, elevationOverride = customOverride)
        calculationResult = result
        if (customOverride == null) {
            customElevationText = String.format(Locale.US, "%.2f", result.elevationInfo.elevationMeters)
            updateHighResElevation(lat, lon, isReverse = false)
        }

        if (!result.isWithinBangladesh) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Note: Coordinates are outside Bangladesh border.")
            }
        }
    }

    // Calculate Reverse (Map Sheet & GR -> Lat/Lon & Elevation)
    fun performReverseCalculation() {
        val sheet = sheetInputText.trim()
        val gr = grInputText.trim().filter { it.isDigit() }
        if (sheet.isEmpty() || gr.isEmpty()) {
            Toast.makeText(context, "অনুগ্রহ করে Map Sheet (যেমন 78 L/2) ও GR (যেমন 146184) দিন", Toast.LENGTH_SHORT).show()
            return
        }
        val result = MilitaryGridCalculator.reverseCalculate(sheet, gr)
        if (result != null) {
            reverseResult = result
            updateHighResElevation(result.wgsLat, result.wgsLon, isReverse = true)
            Toast.makeText(context, "ল্যাটিটিউড ও লংগিটিউড সফলভাবে বের হয়েছে", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "সঠিক Map Sheet (যেমন: 78 L/2) ও 6-ডিজিট GR প্রদান করুন", Toast.LENGTH_LONG).show()
        }
    }

    // Runtime Permission Launcher for Location
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            locationManager.startRealtimeTracking()
            Toast.makeText(context, "Real-time GPS Tracking Started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Location permission required for GPS tracking", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleRealtimeTracking(enable: Boolean) {
        if (enable) {
            val fineCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            val coarseCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (fineCheck == PackageManager.PERMISSION_GRANTED || coarseCheck == PackageManager.PERMISSION_GRANTED) {
                locationManager.startRealtimeTracking()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        } else {
            locationManager.stopRealtimeTracking()
        }
    }

    fun fetchSingleOwnLocation() {
        val fineCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fineCheck == PackageManager.PERMISSION_GRANTED || coarseCheck == PackageManager.PERMISSION_GRANTED) {
            locationStatusMessage = "Acquiring high-accuracy GPS fix..."
            locationManager.requestSingleUpdate { success ->
                if (success) {
                    val lat = locationManager.trackingState.value.latitude
                    val lon = locationManager.trackingState.value.longitude
                    latitudeText = String.format(Locale.US, "%.6f", lat)
                    longitudeText = String.format(Locale.US, "%.6f", lon)
                    isManualElevationOverride = false
                    performCalculation(forceAutoElevation = true)
                    locationStatusMessage = "Location updated (±${String.format(Locale.US, "%.1f", locationManager.trackingState.value.accuracyMeters)}m)"
                    Toast.makeText(context, "Own GPS Location Acquired", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Unable to get GPS fix. Ensure GPS is on.", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Function to Send / Share Location (works for both Forward and Reverse modes)
    fun onSendLocationClicked() {
        val lat: String
        val lon: String
        val gr: String
        val sheet: String
        val elevationStr: String
        val easting: Double
        val northing: Double

        if (!isReverseMode) {
            val res = calculationResult ?: MilitaryGridCalculator.calculate(
                latitudeText.toDoubleOrNull() ?: 24.582254,
                longitudeText.toDoubleOrNull() ?: 90.141237
            )
            lat = latitudeText
            lon = longitudeText
            gr = res.gr6
            sheet = res.mapSheet50k
            elevationStr = "${res.elevationInfo.formattedMeters} (${res.elevationInfo.formattedFeet}) MSL"
            easting = res.eastingMeters
            northing = res.northingMeters
        } else {
            val res = reverseResult ?: MilitaryGridCalculator.reverseCalculate(sheetInputText, grInputText)
            lat = String.format(Locale.US, "%.6f", res?.wgsLat ?: 24.582254)
            lon = String.format(Locale.US, "%.6f", res?.wgsLon ?: 90.141237)
            gr = res?.grInput ?: grInputText
            sheet = res?.mapSheet50k ?: sheetInputText
            elevationStr = res?.elevationInfo?.let { "${it.formattedMeters} (${it.formattedFeet}) MSL" } ?: "N/A"
            easting = res?.eastingMeters ?: 0.0
            northing = res?.northingMeters ?: 0.0
        }

        val shareText = """
            [MILITARY SITREP - COORDINATE REPORT]
            Mode: ${if (!isReverseMode) "Lat/Lon to GR" else "GR/Sheet to Lat/Lon"}
            Latitude: $lat
            Longitude: $lon
            Elevation above Sea Level: $elevationStr
            Grid Reference (GR): $gr (6-Figure)
            Map Sheet: $sheet (1:50,000 Topo)
            BTM Coordinates: E: ${String.format(Locale.US, "%.1f", easting)}m, N: ${String.format(Locale.US, "%.1f", northing)}m
            Google Map: https://maps.google.com/?q=$lat,$lon
            
            Developed & Reported by:
            1618101 Cpl (Tech) Md. Zakaria Alom
            3 Signal Battalion • Bangladesh Army
        """.trimIndent()

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Military Location: GR $gr ($sheet)")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Send Military Location via"))
    }

    // Function to View in Google Map
    fun onViewInGoogleMapClicked() {
        val lat: Double
        val lon: Double
        val gr: String
        val sheet: String

        if (!isReverseMode) {
            lat = latitudeText.toDoubleOrNull() ?: 24.582254
            lon = longitudeText.toDoubleOrNull() ?: 90.141237
            gr = calculationResult?.gr6 ?: ""
            sheet = calculationResult?.mapSheet50k ?: ""
        } else {
            lat = reverseResult?.wgsLat ?: 24.582254
            lon = reverseResult?.wgsLon ?: 90.141237
            gr = reverseResult?.grInput ?: grInputText
            sheet = reverseResult?.mapSheet50k ?: sheetInputText
        }

        val label = Uri.encode("GR: $gr ($sheet)")
        val geoUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($label)")
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$lat,$lon"))
            context.startActivity(webIntent)
        }
    }

    // Copy helper
    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Military Coordinate", text)
        clipboard.setPrimaryClip(clip)
        val msg = if (text.contains(",")) {
            "Latitude & Longitude Copied Successfully!"
        } else {
            "$text copied to clipboard"
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    // Paste helper
    fun pasteCoordinate(isLat: Boolean) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text.toString().trim()
            if (isLat) latitudeText = text else longitudeText = text
            isManualElevationOverride = false
            performCalculation(forceAutoElevation = true)
        }
    }

    fun pasteReverseField(isSheet: Boolean) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text.toString().trim()
            if (isSheet) sheetInputText = text else grInputText = text
            performReverseCalculation()
        }
    }

    ParchmentMapBackground {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title & Map Button Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (!isReverseMode) "LAT & LON ➔ GR CONVERTER" else "GR & SHEET ➔ LAT/LON CONVERTER",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFFF8FAFC)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Button to open Military Map Sheet Viewer
                    Surface(
                        color = Color(0xFF7C3AED),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .clickable { onNavigateToMapViewer() }
                            .testTag("map_viewer_nav_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Military Map",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "MAP",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // 2. CONVERSION MODE SELECTOR TAB
                ConversionModeTab(
                    isReverseMode = isReverseMode,
                    onModeSelected = { isReverseMode = it },
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                // Real-time GPS Tracking Telemetry HUD
                if (showTelemetryHud) {
                    RealtimeTelemetryHud(
                        trackingState = trackingState,
                        onToggleTracking = { toggleRealtimeTracking(it) },
                        onCenterOnUser = {
                            latitudeText = String.format(Locale.US, "%.6f", trackingState.latitude)
                            longitudeText = String.format(Locale.US, "%.6f", trackingState.longitude)
                            isManualElevationOverride = false
                            if (!isReverseMode) {
                                performCalculation(forceAutoElevation = true)
                            }
                        },
                        onClearTrail = { locationManager.clearTrail() },
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                // ==========================================
                // FORWARD MODE: Latitude/Longitude -> GR & Sheet & Elevation
                // ==========================================
                if (!isReverseMode) {
                    ColorfulCoordinateField(
                        label = "Latitude:",
                        value = latitudeText,
                        onValueChange = {
                            latitudeText = it
                            isManualElevationOverride = false
                            performCalculation(forceAutoElevation = true)
                        },
                        placeholder = "24.582254",
                        onClear = { latitudeText = "" },
                        onPaste = { pasteCoordinate(isLat = true) },
                        testTag = "latitude_input"
                    )

                    ColorfulCoordinateField(
                        label = "Longitude:",
                        value = longitudeText,
                        onValueChange = {
                            longitudeText = it
                            isManualElevationOverride = false
                            performCalculation(forceAutoElevation = true)
                        },
                        placeholder = "90.141237",
                        labelColor = CyberGold,
                        onClear = { longitudeText = "" },
                        onPaste = { pasteCoordinate(isLat = false) },
                        onDone = { performCalculation() },
                        testTag = "longitude_input"
                    )

                    ColorfulCoordinateField(
                        label = "Elevation (উচ্চতা মি. - ঐচ্ছিক টাইপ করে পরিবর্তন করুন):",
                        value = customElevationText,
                        onValueChange = {
                            customElevationText = it
                            val parsed = it.trim().toDoubleOrNull()
                            if (parsed != null && calculationResult != null) {
                                isManualElevationOverride = true
                                val customInfo = ElevationInfo(
                                    elevationMeters = parsed,
                                    elevationFeet = parsed * 3.28084,
                                    formattedMeters = String.format(Locale.US, "%.2f m", parsed),
                                    formattedFeet = String.format(Locale.US, "%.1f ft", parsed * 3.28084),
                                    source = "Custom Entry"
                                )
                                calculationResult = calculationResult?.copy(elevationInfo = customInfo)
                            }
                        },
                        placeholder = "Google Earth দ্বারা স্বয়ংক্রিয় বা পরিবর্তন করুন (যেমন 23.3)",
                        labelColor = ElectricCyan,
                        onClear = {
                            customElevationText = ""
                            isManualElevationOverride = false
                            performCalculation(forceAutoElevation = true)
                        },
                        testTag = "elevation_input"
                    )

                    // Primary Output Display: GR, Map Sheet, Elevation above sea level
                    ColorfulResultDisplay(
                        result = calculationResult,
                        onCopy = { copyToClipboard(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary Action Button: SHOW GR & MAP SHEET
                    ColorfulTacticalButton(
                        text = "SHOW GR & MAP SHEET",
                        onClick = { performCalculation() },
                        gradient = Brush.horizontalGradient(
                            listOf(Color(0xFF059669), Color(0xFF10B981))
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        testTag = "show_gr_button"
                    )
                }

                // ==========================================
                // REVERSE MODE: Map Sheet & GR -> Latitude, Longitude & Elevation
                // ==========================================
                if (isReverseMode) {
                    // Map Sheet Input
                    ColorfulCoordinateField(
                        label = "Map Sheet:",
                        value = sheetInputText,
                        onValueChange = {
                            sheetInputText = it
                            if (it.length >= 5 && grInputText.length >= 6) {
                                performReverseCalculation()
                            }
                        },
                        placeholder = "78 L/2",
                        labelColor = CyberGold,
                        onClear = { sheetInputText = "" },
                        onPaste = { pasteReverseField(isSheet = true) },
                        testTag = "sheet_reverse_input"
                    )

                    // Quick Sheet Suggestions Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("78 L/2", "78 L/3", "78 H/14", "78 L/6", "78 H/13", "79 I/1", "79 N/7", "83 D/13").forEach { s ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (sheetInputText == s) Color(0xFF0284C7) else Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF334155)),
                                modifier = Modifier.clickable {
                                    sheetInputText = s
                                    performReverseCalculation()
                                }
                            ) {
                                Text(
                                    text = s,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sheetInputText == s) Color.White else Color(0xFFCBD5E1),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Grid Reference (GR) Input
                    ColorfulCoordinateField(
                        label = "Grid Ref (GR):",
                        value = grInputText,
                        onValueChange = {
                            grInputText = it
                            if (it.length >= 6) {
                                performReverseCalculation()
                            }
                        },
                        placeholder = "146184",
                        labelColor = NeonGreen,
                        onClear = { grInputText = "" },
                        onPaste = { pasteReverseField(isSheet = false) },
                        onDone = { performReverseCalculation() },
                        testTag = "gr_reverse_input"
                    )

                    // Reverse Result Display: Latitude, Longitude, Elevation, DMS
                    ReverseResultDisplay(
                        result = reverseResult,
                        onCopy = { copyToClipboard(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Button: SHOW LAT & LONGITUDE
                    ColorfulTacticalButton(
                        text = "SHOW LAT & LONGITUDE",
                        onClick = { performReverseCalculation() },
                        icon = Icons.Default.Visibility,
                        gradient = Brush.horizontalGradient(
                            listOf(Color(0xFF059669), Color(0xFF10B981))
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        testTag = "show_latlon_button"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Shared Action Buttons Row: OWN LOCATION (Cyan) & SEND LOCATION (Amber/Orange)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ColorfulTacticalButton(
                        text = if (trackingState.isTracking) "TRACKING ACTIVE" else "OWN LOCATION",
                        onClick = {
                            if (!trackingState.isTracking) {
                                fetchSingleOwnLocation()
                            } else {
                                latitudeText = String.format(Locale.US, "%.6f", trackingState.latitude)
                                longitudeText = String.format(Locale.US, "%.6f", trackingState.longitude)
                                performCalculation()
                            }
                        },
                        icon = Icons.Default.GpsFixed,
                        gradient = Brush.horizontalGradient(
                            listOf(Color(0xFF0284C7), Color(0xFF06B6D4))
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        testTag = "own_location_button"
                    )

                    ColorfulTacticalButton(
                        text = "SEND LOCATION",
                        onClick = { onSendLocationClicked() },
                        icon = Icons.AutoMirrored.Filled.Send,
                        gradient = Brush.horizontalGradient(
                            listOf(Color(0xFFD97706), Color(0xFFF59E0B))
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        testTag = "send_location_button"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // VIEW IN GOOGLE MAP Button (Purple / Violet Gradient)
                ColorfulTacticalButton(
                    text = "VIEW IN GOOGLE MAP",
                    onClick = { onViewInGoogleMapClicked() },
                    icon = Icons.Default.Place,
                    gradient = Brush.horizontalGradient(
                        listOf(Color(0xFF7C3AED), Color(0xFF8B5CF6))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    testTag = "view_google_map_button"
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

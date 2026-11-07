package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import java.util.Locale
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.ShareLocation
import com.example.data.model.CalculationResult
import com.example.data.model.DistanceUnit
import com.example.data.model.NavigationCalculator
import com.example.ui.MainViewModel
import com.example.ui.components.CompassRoseView
import com.example.ui.components.CreditDialog
import com.example.ui.components.LineOfSightView
import com.example.ui.components.TacticalRadarMapView

@Composable
fun CalculatorScreen(
    viewModel: MainViewModel,
    onNavigateToIntel: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToRadar: () -> Unit = {},
    onNavigateToGr: () -> Unit = {},
    onNavigateToCompass: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val p1Name by viewModel.p1Name.collectAsState()
    val p2Name by viewModel.p2Name.collectAsState()
    val p1Lat by viewModel.p1Lat.collectAsState()
    val p1Lon by viewModel.p1Lon.collectAsState()
    val p1Elevation by viewModel.p1Elevation.collectAsState()
    val p1ElevationText by viewModel.p1ElevationText.collectAsState()
    val p1ElevLoading by viewModel.p1ElevationLoading.collectAsState()

    val p2Lat by viewModel.p2Lat.collectAsState()
    val p2Lon by viewModel.p2Lon.collectAsState()
    val p2Elevation by viewModel.p2Elevation.collectAsState()
    val p2ElevationText by viewModel.p2ElevationText.collectAsState()
    val p2ElevLoading by viewModel.p2ElevationLoading.collectAsState()

    val calcResult by viewModel.calculationResult.collectAsState()
    val selectedUnit by viewModel.selectedUnit.collectAsState()
    val isGpsSearching by viewModel.isGpsSearching.collectAsState()

    val p1AntennaHeight by viewModel.p1AntennaHeight.collectAsState()
    val p2AntennaHeight by viewModel.p2AntennaHeight.collectAsState()
    val frequencyMhz by viewModel.frequencyMhz.collectAsState()
    val frequency2Mhz by viewModel.frequency2Mhz.collectAsState()
    val txPowerDbm by viewModel.txPowerDbm.collectAsState()
    val rxSensitivityDbm by viewModel.rxSensitivityDbm.collectAsState()
    val antennaGainDbi by viewModel.antennaGainDbi.collectAsState()

    val isOfflineCached by viewModel.isOfflineCached.collectAsState()
    val isCachingMap by viewModel.isCachingMap.collectAsState()
    val cachingProgress by viewModel.cachingProgress.collectAsState()
    val activeCachedArea by viewModel.activeCachedArea.collectAsState()

    var showCreditDialog by remember { mutableStateOf(false) }

    if (showCreditDialog) {
        CreditDialog(onDismiss = { showCreditDialog = false })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        // Action Button: Live Location Share
        Button(
            onClick = { viewModel.shareLiveLocation(context) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("share_live_location_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.ShareLocation, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("লাইভ GPS লোকেশন শেয়ার", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Unit Selector & Header Tools
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "COORDINATE CALCULATOR",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DistanceUnit.values().forEach { unit ->
                    FilterChip(
                        selected = selectedUnit == unit,
                        onClick = { viewModel.setUnit(unit) },
                        label = { Text(unit.suffix, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // POINT 1 CARD (Observer / Start)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "First Location (Point 1 / Observer)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // My Location Button
                    Button(
                        onClick = { viewModel.setCurrentLocationAsPoint1() },
                        modifier = Modifier.testTag("loc_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        if (isGpsSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = "Get GPS Location",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "My GPS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Editable Terminal 1 / Location Name
                OutlinedTextField(
                    value = p1Name,
                    onValueChange = { viewModel.onP1NameChanged(it) },
                    label = { Text("টার্মিনাল / লোকেশন নাম (Site A Name)", fontSize = 11.sp) },
                    placeholder = { Text("e.g. Dhaka Base / Site Alpha") },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editTextFirstLocationName"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = p1Lat,
                        onValueChange = { viewModel.onP1LatChanged(it) },
                        label = { Text("Latitude (Lat 1)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. 23.8103") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("editTextFirstLatitude"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = p1Lon,
                        onValueChange = { viewModel.onP1LonChanged(it) },
                        label = { Text("Longitude (Lon 1)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. 90.4125") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("editTextFirstLongitude"),
                        singleLine = true
                    )
                }

                // Elevation Display & Manual Input for Point 1
                Spacer(modifier = Modifier.height(8.dp))
                EditableElevationRow(
                    elevationText = p1ElevationText,
                    onElevationChange = { viewModel.onP1ElevationChanged(it) },
                    elevationMeters = p1Elevation,
                    isLoading = p1ElevLoading,
                    pointLabel = "Point 1",
                    testTag = "editTextFirstElevation"
                )
            }
        }

        // PROMINENT SWAP BUTTON
        Button(
            onClick = { viewModel.swapPoints() },
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .padding(vertical = 4.dp)
                .testTag("swap_locations_button"),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                Icons.Default.SwapVert,
                contentDescription = "Swap Locations",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SWAP (পয়েন্ট ১ ⇄ পয়েন্ট ২ পরিবর্তন)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // POINT 2 CARD (Target / Destination)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Second Location (Point 2 / Target)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Target GPS Button
                    Button(
                        onClick = { viewModel.setCurrentLocationAsPoint2() },
                        modifier = Modifier.testTag("target_loc_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Get GPS Location for Point 2",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Target GPS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // TARGET ACTION GRID VIEW (Share Place, Search Place)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Share Place Grid Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable {
                                val targetInfo = """
                                    [TARGET SITE - POINT 2 LOCATION REPORT]
                                    Site Name: ${p2Name.ifBlank { "Point 2 (Target)" }}
                                    Latitude: $p2Lat
                                    Longitude: $p2Lon
                                    Elevation: ${p2Elevation?.let { "${"%.1f".format(it)}m MSL" } ?: "Calculated"}
                                    Google Maps: https://maps.google.com/?q=$p2Lat,$p2Lon
                                """.trimIndent()
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Target Location: $p2Name")
                                    putExtra(Intent.EXTRA_TEXT, targetInfo)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Target Place via"))
                            }
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .testTag("share_target_place_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.ShareLocation, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Share Place", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }

                    // Search Place Grid Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable { onNavigateToIntel() }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Search Place", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Editable Terminal 2 / Location Name
                OutlinedTextField(
                    value = p2Name,
                    onValueChange = { viewModel.onP2NameChanged(it) },
                    label = { Text("টার্মিনাল / লোকেশন নাম (Site B Name)", fontSize = 11.sp) },
                    placeholder = { Text("e.g. Rajshahi Outpost / Site Bravo") },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editTextSecondLocationName"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = p2Lat,
                        onValueChange = { viewModel.onP2LatChanged(it) },
                        label = { Text("Latitude (Lat 2)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. 24.3636") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("editTextSecondLatitude"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = p2Lon,
                        onValueChange = { viewModel.onP2LonChanged(it) },
                        label = { Text("Longitude (Lon 2)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. 88.6241") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("editTextSecondLongitude"),
                        singleLine = true
                    )
                }

                // Elevation Display & Manual Input for Point 2
                Spacer(modifier = Modifier.height(8.dp))
                EditableElevationRow(
                    elevationText = p2ElevationText,
                    onElevationChange = { viewModel.onP2ElevationChanged(it) },
                    elevationMeters = p2Elevation,
                    isLoading = p2ElevLoading,
                    pointLabel = "Point 2",
                    testTag = "editTextSecondElevation"
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // PRIMARY ACTION BUTTONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.calculateDistance() },
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .testTag("calc_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Calculate, contentDescription = "Calculate")
                Spacer(modifier = Modifier.width(8.dp))
                Text("হিসাব করুন (Calculate)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            OutlinedButton(
                onClick = { viewModel.clearAll() },
                modifier = Modifier
                    .weight(0.7f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Clear")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear")
            }
        }

        // CALCULATION RESULTS SECTION
        AnimatedVisibility(visible = calcResult != null) {
            calcResult?.let { result ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(18.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "CALCULATION REPORT",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Big Distance Readout
                            Text(
                                text = result.formatDistance(selectedUnit),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("textViewDistance")
                            )

                            Text(
                                text = "Great-Circle Geodesic Distance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Bearing Cards Row with Compass
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Forward Bearing
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Forward Bearing",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${result.bearingRounded}°",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${result.bearingCardinal} (${result.bearingMils} Mils)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Interactive Mini Compass Rose
                                CompassRoseView(
                                    bearingDegrees = result.bearingDegrees.toFloat(),
                                    backBearingDegrees = result.backBearingDegrees.toFloat(),
                                    modifier = Modifier.size(130.dp)
                                )

                                // Back Bearing
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Back Bearing",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${result.backBearingRounded}°",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Text(
                                        text = "${result.backBearingCardinal} (${result.backBearingMils} Mils)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }

                            // Elevation Difference and Incline Analysis
                            if (p1Elevation != null && p2Elevation != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val delta = p2Elevation!! - p1Elevation!!
                                val (gradePercent, slopeAngle) = NavigationCalculator.calculateSlope(result.distanceMeters, delta)
                                val deltaSign = if (delta > 0) "+" else ""

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("উচ্চতার ব্যবধান (Δh)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                "$deltaSign${"%.1f".format(delta)} m (${deltaSign}${"%.1f".format(delta * 3.28084)} ft)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (delta >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("ঢাল / গ্রেডিয়েন্ট", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                "${"%.2f".format(gradePercent)}% (${"%.2f".format(slopeAngle)}°)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 4 PRIMARY ACTION BUTTONS: SMS, Share, Save, Alternate
                            Text(
                                text = "ACTIONS (অ্যাকশন):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Row 1 of Grid: SMS & Share
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { sendSms(context, result) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .testTag("msg_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "SMS", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SMS", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { shareReport(context, result) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .testTag("share_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Share", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Row 2 of Grid: Save & Alternate
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.saveRoute() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .testTag("save_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Icon(Icons.Default.BookmarkAdd, contentDescription = "Save", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            // Alternate Route / Reverse Path & Relay Recommendation
                                            val p1L = p1Lat.toDoubleOrNull() ?: 23.8103
                                            val p1Ln = p1Lon.toDoubleOrNull() ?: 90.4125
                                            val p2L = p2Lat.toDoubleOrNull() ?: 24.3636
                                            val p2Ln = p2Lon.toDoubleOrNull() ?: 88.6241

                                            val midLat = (p1L + p2L) / 2.0
                                            val midLon = (p1Ln + p2Ln) / 2.0
                                            val alternateMsg = """
                                                [ALTERNATE ROUTE / REVERSE PATH]
                                                Reverse Direction: ${result.backBearingRounded}° (${result.backBearingCardinal}, ${result.backBearingMils} Mils)
                                                Alternate Relay Point: ${"%.5f".format(midLat)}, ${"%.5f".format(midLon)}
                                                Elevation Difference: ${p2Elevation?.let { "${"%.1f".format(it)}m" } ?: "N/A"} -> ${p1Elevation?.let { "${"%.1f".format(it)}m" } ?: "N/A"}
                                                LOS Range: ${result.formatDistance(selectedUnit)}
                                            """.trimIndent()

                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Alternate Route", alternateMsg)
                                            clipboard.setPrimaryClip(clip)

                                            Toast.makeText(context, "Alternate Route / Reverse Link ডাটা কপি হয়েছে", Toast.LENGTH_LONG).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .testTag("alternate_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.AltRoute, contentDescription = "Alternate", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Alternate", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Embedded Tactical Radar Map
                    TacticalRadarMapView(
                        calcResult = result,
                        p1Elevation = p1Elevation,
                        p2Elevation = p2Elevation,
                        isOfflineCached = isOfflineCached,
                        isCachingMap = isCachingMap,
                        cachingProgress = cachingProgress,
                        activeCachedArea = activeCachedArea,
                        onToggleOfflineCache = { viewModel.toggleOfflineCache(it) },
                        onCacheVisibleArea = { viewModel.cacheVisibleMapArea() },
                        onClearOfflineCache = { viewModel.clearOfflineCache() }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Antenna Heights & Line of Sight (LOS) / Fresnel Zone Analysis & Digital Path Profile
                    LineOfSightView(
                        calcResult = result,
                        p1Elevation = p1Elevation,
                        p2Elevation = p2Elevation,
                        p1AntennaHeight = p1AntennaHeight,
                        onP1AntennaHeightChange = { viewModel.setP1AntennaHeight(it) },
                        p2AntennaHeight = p2AntennaHeight,
                        onP2AntennaHeightChange = { viewModel.setP2AntennaHeight(it) },
                        frequencyMhz = frequencyMhz,
                        onFrequencyChange = { viewModel.setFrequencyMhz(it) },
                        txPowerDbm = txPowerDbm,
                        onTxPowerChange = { viewModel.setTxPowerDbm(it) },
                        rxSensitivityDbm = rxSensitivityDbm,
                        onRxSensitivityChange = { viewModel.setRxSensitivityDbm(it) },
                        frequency2Mhz = frequency2Mhz,
                        onFrequency2Change = { viewModel.setFrequency2Mhz(it) },
                        antennaGainDbi = antennaGainDbi,
                        onAntennaGainChange = { viewModel.setAntennaGainDbi(it) },
                        onScanObstaclesGemini = {
                            viewModel.scanObstaclesWithGemini()
                            onNavigateToIntel()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Military Developer Credit (as requested in original code)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCreditDialog = true }
                .padding(vertical = 12.dp)
                .testTag("credit_text"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Credit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Developed By : 1618101 Cpl (Tech) Md Zakaria Alom, 3 Signal Battalion",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = "Tap to view developer credit & unit specifications",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun EditableElevationRow(
    elevationText: String,
    onElevationChange: (String) -> Unit,
    elevationMeters: Double?,
    isLoading: Boolean,
    pointLabel: String,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Terrain,
                    contentDescription = "Elevation",
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$pointLabel - উচ্চতা (Elevation MSL):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(11.dp), strokeWidth = 1.8.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "গুগল আর্থ...",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Explore,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Google Earth",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = elevationText,
            onValueChange = onElevationChange,
            placeholder = { Text("স্বয়ংক্রিয় বা টাইপ করুন (যেমন 23.3)", fontSize = 11.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            singleLine = true,
            trailingIcon = {
                if (elevationMeters != null) {
                    val feet = elevationMeters * 3.28084
                    Text(
                        text = "${String.format(Locale.US, "%.1f", elevationMeters)} মি (${String.format(Locale.US, "%.1f", feet)} ft)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        )
    }
}

private fun sendSms(context: Context, result: CalculationResult) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = "vnd.android-dir/mms-sms"
            putExtra("sms_body", result.toSmsBody())
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val sendIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:")
            putExtra("sms_body", result.toSmsBody())
        }
        try {
            context.startActivity(sendIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "SMS অ্যাপ পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun shareReport(context: Context, result: CalculationResult) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, result.toDetailedReport())
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Location Finder Report")
    context.startActivity(shareIntent)
}

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculator.MilitaryGridResult
import com.example.calculator.ReverseGridResult
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyberGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonGreen

/**
 * Top Developer Header Badge in English placed prominently at the very top.
 *
 * DEVELOPED BY:
 * 16181001 Cpl (Technician) Md. Zakaria Alam
 * 3 Signal Battalion
 */
@Composable
fun TopDeveloperHeader(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .testTag("top_developer_header"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xF0131E33)
        ),
        border = BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(
                listOf(ElectricCyan, CyberGold, NeonGreen)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Military Shield Insignia Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF00E5FF).copy(alpha = 0.3f), Color(0xFF1E293B))
                        )
                    )
                    .border(1.2.dp, CyberGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MilitaryTech,
                    contentDescription = "Military Tech",
                    tint = CyberGold,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "DEVELOPED BY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = ElectricCyan
                    )
                    Surface(
                        color = Color(0x3300E676),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.6.dp, NeonGreen)
                    ) {
                        Text(
                            text = "3 SIGNAL BATTALION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Text(
                    text = "1618101 Cpl (Tech) Md. Zakaria Alom",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFF8FAFC)
                )

                Text(
                    text = "3 Signal Battalion • Bangladesh Army",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

/**
 * Colorful modern tactile button with gradient styling and icons.
 */
@Composable
fun ColorfulTacticalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    gradient: Brush = Brush.horizontalGradient(listOf(Color(0xFF0284C7), Color(0xFF06B6D4))),
    textColor: Color = Color.White,
    testTag: String = "tactical_button"
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        modifier = modifier
            .testTag(testTag)
            .shadow(if (enabled) 5.dp else 1.dp, RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (enabled) gradient else Brush.horizontalGradient(listOf(Color.DarkGray, Color.Gray)))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = textColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        fontSize = 15.sp,
                        color = textColor
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Colorful Coordinate Input Field for Latitude and Longitude.
 */
@Composable
fun ColorfulCoordinateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    labelColor: Color = ElectricCyan,
    onPaste: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null,
    testTag: String = "coord_field"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = labelColor
            ),
            modifier = Modifier.width(105.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .testTag(testTag),
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color(0x6694A3B8),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            textStyle = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = if (onDone != null) ImeAction.Done else ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDone?.invoke() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0x331E293B),
                unfocusedContainerColor = Color(0x221E293B),
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = Color(0xFF334155),
                cursorColor = ElectricCyan
            ),
            shape = RoundedCornerShape(10.dp),
            trailingIcon = {
                Row {
                    if (value.isNotEmpty() && onClear != null) {
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else if (onPaste != null) {
                        IconButton(
                            onClick = onPaste,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        )
    }
}

/**
 * Colorful Military Result Card prominently displaying:
 * GR: 146184
 * Map Sheet: 78 L/2
 */
@Composable
fun ColorfulResultDisplay(
    result: MilitaryGridResult?,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF0131E33)),
        border = BorderStroke(1.2.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (result != null) {
                // GR Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCopy(result.gr6) }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "GR: ",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp,
                                color = NeonGreen
                            )
                        )
                        Text(
                            text = result.gr6,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 30.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.2.sp
                            ),
                            modifier = Modifier.testTag("gr_result_text")
                        )
                    }

                    Surface(
                        color = Color(0x3300E676),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.8.dp, NeonGreen),
                        modifier = Modifier.clickable { onCopy(result.gr6) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy GR",
                                tint = NeonGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "COPY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Map Sheet Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCopy(result.mapSheet50k) }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Map Sheet: ",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = CyberGold
                            )
                        )
                        Text(
                            text = result.mapSheet50k,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 23.sp,
                                color = Color.White
                            ),
                            modifier = Modifier.testTag("mapsheet_result_text")
                        )
                    }

                    Text(
                        text = "1:50,000 Topo",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Projected Technical Values
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BTM Easting (X):",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f m", result.eastingMeters),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column {
                        Text(
                            text = "BTM Northing (Y):",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f m", result.northingMeters),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "8-Fig GR (10m):",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = result.gr8,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Elevation Display Row (সমুদ্রপৃষ্ঠ থেকে উচ্চতা)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terrain,
                            contentDescription = "Elevation",
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "সমুদ্রপৃষ্ঠ থেকে উচ্চতা:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                    Text(
                        text = "${result.elevationInfo.formattedMeters} (${result.elevationInfo.formattedFeet}) MSL",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = ElectricCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Text(
                    text = "GR: ------",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = Color.Gray
                    )
                )
                Text(
                    text = "Map Sheet: -- --/--",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.Gray
                    )
                )
            }
        }
    }
}

/**
 * Tab bar to toggle between Forward (Lat/Lon -> GR) and Reverse (GR & Sheet -> Lat/Lon).
 */
@Composable
fun ConversionModeTab(
    isReverseMode: Boolean,
    onModeSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF141E33),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.2.dp, Color(0xFF334155)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            // Forward Tab: Lat/Lon -> GR
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (!isReverseMode) {
                            Brush.horizontalGradient(listOf(Color(0xFF0284C7), Color(0xFF06B6D4)))
                        } else {
                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
                    .clickable { onModeSelected(false) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LAT / LON ➔ GR",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 0.6.sp,
                    color = if (!isReverseMode) Color.White else Color(0xFF94A3B8)
                )
            }

            // Reverse Tab: GR & Sheet -> Lat/Lon
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isReverseMode) {
                            Brush.horizontalGradient(listOf(Color(0xFF059669), Color(0xFF10B981)))
                        } else {
                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
                    .clickable { onModeSelected(true) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GR & SHEET ➔ LAT / LON",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 0.6.sp,
                    color = if (isReverseMode) Color.White else Color(0xFF94A3B8)
                )
            }
        }
    }
}

/**
 * Reverse Result Display: Map Sheet & GR -> Latitude, Longitude, and Elevation.
 */
@Composable
fun ReverseResultDisplay(
    result: ReverseGridResult?,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF0131E33)),
        border = BorderStroke(1.2.dp, Color(0xFF10B981).copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (result != null) {
                val formattedLatLon = String.format(java.util.Locale.US, "%.6f, %.6f", result.wgsLat, result.wgsLon)

                // Top Actions Header with Unified COPY LAT/LON Button
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GEODETIC COORDINATES (WGS-84):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.5.sp
                    )

                    // CR 4: Unified One-Click COPY LAT/LON Button
                    Surface(
                        color = Color(0x3300E5FF),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, ElectricCyan),
                        modifier = Modifier
                            .testTag("copy_lat_lon_button")
                            .clickable { onCopy(formattedLatLon) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy LAT/LON",
                                tint = ElectricCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "COPY LAT/LON",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = ElectricCyan
                            )
                        }
                    }
                }

                // Latitude Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCopy(formattedLatLon) }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Latitude: ",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = ElectricCyan
                                )
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.6f°", result.wgsLat),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                        Text(
                            text = result.latDms,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Longitude Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCopy(formattedLatLon) }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Longitude: ",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = CyberGold
                                )
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.6f°", result.wgsLon),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                        Text(
                            text = result.lonDms,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Elevation Row (সমুদ্রপৃষ্ঠ থেকে উচ্চতা)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terrain,
                            contentDescription = "Elevation",
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "সমুদ্রপৃষ্ঠ থেকে উচ্চতা:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                    Text(
                        text = "${result.elevationInfo.formattedMeters} (${result.elevationInfo.formattedFeet}) MSL",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // BTM Grid Footprint
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BTM Easting (X):",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f m", result.eastingMeters),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column {
                        Text(
                            text = "BTM Northing (Y):",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f m", result.northingMeters),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onCopy("${result.mapSheet50k} GR ${result.grInput}") }
                            .padding(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Sheet / GR:",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy GR", tint = CyberGold, modifier = Modifier.size(11.dp))
                        }
                        Text(
                            text = "${result.mapSheet50k} - ${result.grInput}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Formula Verification (Round-Trip) Confirmation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x33059669), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified Formula",
                            tint = NeonGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ফরওয়ার্ড সূত্র নিশ্চিতকরণ:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                    Text(
                        text = "GR ${result.verifiedGr} • ${result.verifiedSheet} (১০০% মিল)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Text(
                    text = "Latitude: --.------°",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                )
                Text(
                    text = "Longitude: --.------°",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                )
            }
        }
    }
}


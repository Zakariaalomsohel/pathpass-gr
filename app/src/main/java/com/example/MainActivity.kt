package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.data.notification.NotificationHelper
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.MainViewModel
import com.example.ui.components.CreditDialog
import com.example.ui.components.LineOfSightView
import com.example.ui.components.TacticalRadarMapView
import com.example.ui.components.ThemeSelectorDialog
import com.example.ui.components.TopDeveloperBanner
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.GeminiIntelScreen
import com.example.ui.screens.MilitaryGrScreen
import com.example.ui.screens.MilitaryMapViewerScreen
import com.example.ui.screens.SavedRoutesScreen
import com.example.ui.screens.StandaloneCompassScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

enum class AppScreen(val title: String) {
    CALCULATOR("Calculator"),
    RADAR("Radar & LOS"),
    MILITARY_GR("Army GR"),
    MILITARY_MAP("Military Map"),
    COMPASS("Compass"),
    INTEL("Maps Intel"),
    SAVED("Saved Routes")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val colorTheme by viewModel.selectedColorTheme.collectAsState()
            val darkOption by viewModel.selectedDarkOption.collectAsState()

            MyApplicationTheme(
                colorTheme = colorTheme,
                darkThemeOption = darkOption
            ) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.CALCULATOR) }
    var militaryLat by remember { androidx.compose.runtime.mutableDoubleStateOf(24.582254) }
    var militaryLon by remember { androidx.compose.runtime.mutableDoubleStateOf(90.141237) }
    var showCreditDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val userMessage by viewModel.userMessage.collectAsState()

    // GPS and Notification permissions array
    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    var hasLocationPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPerm by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }

    // Permission launcher for Location and Notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else {
            NotificationHelper.hasNotificationPermission(context)
        }

        hasLocationPerm = fineGranted || coarseGranted
        hasNotificationPerm = notifGranted

        if (hasLocationPerm) {
            viewModel.fetchGpsFix(silent = false)
        } else {
            Toast.makeText(context, "সঠিক কোঅর্ডিনেট গণনার জন্য GPS পারমিশন প্রয়োজন", Toast.LENGTH_SHORT).show()
        }

        if (hasNotificationPerm) {
            NotificationHelper.showTacticalNotification(
                context = context,
                notificationId = 1001,
                title = "PathPass & GR (ZAS)",
                message = "GPS ও নোটিফিকেশন সিস্টেম সফলভাবে অনুমোদিত হয়েছে।"
            )
        }
    }

    LaunchedEffect(Unit) {
        NotificationHelper.initNotificationChannel(context)
        if (!hasLocationPerm || !hasNotificationPerm) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    // Handle user messages
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearMessage()
            }
        }
    }

    // Back handling for custom navigation
    BackHandler(enabled = currentScreen != AppScreen.CALCULATOR) {
        if (currentScreen == AppScreen.MILITARY_MAP) {
            currentScreen = AppScreen.MILITARY_GR
        } else {
            currentScreen = AppScreen.CALCULATOR
        }
    }

    if (showCreditDialog) {
        CreditDialog(onDismiss = { showCreditDialog = false })
    }

    if (showThemeDialog) {
        val colorTheme by viewModel.selectedColorTheme.collectAsState()
        val darkOption by viewModel.selectedDarkOption.collectAsState()
        ThemeSelectorDialog(
            currentColorTheme = colorTheme,
            currentDarkOption = darkOption,
            onSelectColorTheme = { viewModel.setColorTheme(it) },
            onSelectDarkOption = { viewModel.setDarkOption(it) },
            onDismiss = { showThemeDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PATHPASS & GR (ZAS)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    // Quick Colorful Theme Palette Selector in TopAppBar
                    IconButton(
                        onClick = { showThemeDialog = true },
                        modifier = Modifier.testTag("top_theme_button")
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "Color Theme (কালার ফুল থিম)",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Quick Notification Test & Status in top bar
                    IconButton(
                        onClick = {
                            if (hasNotificationPerm) {
                                NotificationHelper.showTacticalNotification(
                                    context = context,
                                    notificationId = 102,
                                    title = "PathPass & GR (ZAS)",
                                    message = "ট্যাকটিক্যাল নোটিফিকেশন সিস্টেম সম্পূর্ণ কার্যকর আছে।"
                                )
                                Toast.makeText(context, "টেস্ট নোটিফিকেশন পাঠানো হয়েছে", Toast.LENGTH_SHORT).show()
                            } else {
                                permissionLauncher.launch(permissionsToRequest)
                            }
                        },
                        modifier = Modifier.testTag("top_notification_button")
                    ) {
                        Icon(
                            if (hasNotificationPerm) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                            contentDescription = "Notification Status",
                            tint = if (hasNotificationPerm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }

                    // Quick 1-click location share button in top bar
                    IconButton(
                        onClick = { viewModel.shareLiveLocation(context) },
                        modifier = Modifier.testTag("top_share_location_button")
                    ) {
                        Icon(
                            Icons.Default.ShareLocation,
                            contentDescription = "Share Live Location",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.fetchGpsFix(silent = false) },
                        modifier = Modifier.testTag("gps_refresh_button")
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Refresh GPS",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { showCreditDialog = true },
                        modifier = Modifier.testTag("credit_button")
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Developer Credit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.CALCULATOR,
                    onClick = { currentScreen = AppScreen.CALCULATOR },
                    icon = { Icon(Icons.Default.Calculate, contentDescription = "Calculator") },
                    label = { Text("Calc", fontSize = 9.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.RADAR,
                    onClick = { currentScreen = AppScreen.RADAR },
                    icon = { Icon(Icons.Default.Radar, contentDescription = "Radar & LOS") },
                    label = { Text("Radar/LOS", fontSize = 9.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.MILITARY_GR || currentScreen == AppScreen.MILITARY_MAP,
                    onClick = { currentScreen = AppScreen.MILITARY_GR },
                    icon = { Icon(Icons.Default.MilitaryTech, contentDescription = "BD Army GR (ZAS)") },
                    label = { Text("Army GR", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.COMPASS,
                    onClick = { currentScreen = AppScreen.COMPASS },
                    icon = { Icon(Icons.Default.Explore, contentDescription = "Compass") },
                    label = { Text("Compass", fontSize = 9.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.INTEL,
                    onClick = { currentScreen = AppScreen.INTEL },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Maps Intel") },
                    label = { Text("AI Intel", fontSize = 9.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.SAVED,
                    onClick = { currentScreen = AppScreen.SAVED },
                    icon = { Icon(Icons.Default.Bookmarks, contentDescription = "Saved") },
                    label = { Text("Saved", fontSize = 9.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // PROMINENT TOP DEVELOPER BANNER (Slides up from bottom to top as requested)
            TopDeveloperBanner(
                onClick = { showCreditDialog = true }
            )

            // Tactical Permission Status & Request Card
            if (!hasLocationPerm || !hasNotificationPerm) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (!hasLocationPerm) Icons.Default.MyLocation else Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (!hasLocationPerm && !hasNotificationPerm) "GPS ও নোটিফিকেশন পারমিশন প্রয়োজন"
                                    else if (!hasLocationPerm) "GPS লোকেশন পারমিশন প্রয়োজন"
                                    else "নোটিফিকেশন পারমিশন প্রয়োজন",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "লাইভ কোঅর্ডিনেট, GR ও অ্যালার্ট সক্রিয় করতে অনুমতি দিন",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { permissionLauncher.launch(permissionsToRequest) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("অনুমোদন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (currentScreen) {
                    AppScreen.CALCULATOR -> {
                        CalculatorScreen(
                            viewModel = viewModel,
                            onNavigateToIntel = { currentScreen = AppScreen.INTEL },
                            onNavigateToHistory = { currentScreen = AppScreen.SAVED },
                            onNavigateToRadar = { currentScreen = AppScreen.RADAR },
                            onNavigateToGr = { currentScreen = AppScreen.MILITARY_GR },
                            onNavigateToCompass = { currentScreen = AppScreen.COMPASS }
                        )
                    }
                    AppScreen.RADAR -> {
                        val calcResult by viewModel.calculationResult.collectAsState()
                        val p1Elev by viewModel.p1Elevation.collectAsState()
                        val p2Elev by viewModel.p2Elevation.collectAsState()
                        val p1AntennaHeight by viewModel.p1AntennaHeight.collectAsState()
                        val p2AntennaHeight by viewModel.p2AntennaHeight.collectAsState()
                        val frequencyMhz by viewModel.frequencyMhz.collectAsState()
                        val frequency2Mhz by viewModel.frequency2Mhz.collectAsState()
                        val txPowerDbm by viewModel.txPowerDbm.collectAsState()
                        val rxSensitivityDbm by viewModel.rxSensitivityDbm.collectAsState()
                        val isOfflineCached by viewModel.isOfflineCached.collectAsState()
                        val isCachingMap by viewModel.isCachingMap.collectAsState()
                        val cachingProgress by viewModel.cachingProgress.collectAsState()
                        val activeCachedArea by viewModel.activeCachedArea.collectAsState()
                        val radarScrollState = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(radarScrollState)
                                .padding(16.dp)
                        ) {
                            TacticalRadarMapView(
                                calcResult = calcResult,
                                p1Elevation = p1Elev,
                                p2Elevation = p2Elev,
                                isOfflineCached = isOfflineCached,
                                isCachingMap = isCachingMap,
                                cachingProgress = cachingProgress,
                                activeCachedArea = activeCachedArea,
                                onToggleOfflineCache = { viewModel.toggleOfflineCache(it) },
                                onCacheVisibleArea = { viewModel.cacheVisibleMapArea() },
                                onClearOfflineCache = { viewModel.clearOfflineCache() }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            LineOfSightView(
                                calcResult = calcResult,
                                p1Elevation = p1Elev,
                                p2Elevation = p2Elev,
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
                                onScanObstaclesGemini = {
                                    viewModel.scanObstaclesWithGemini()
                                    currentScreen = AppScreen.INTEL
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    AppScreen.MILITARY_GR -> {
                        MilitaryGrScreen(
                            initialLat = militaryLat,
                            initialLon = militaryLon,
                            onNavigateToMapViewer = {
                                currentScreen = AppScreen.MILITARY_MAP
                            }
                        )
                    }
                    AppScreen.MILITARY_MAP -> {
                        MilitaryMapViewerScreen(
                            currentLat = militaryLat,
                            currentLon = militaryLon,
                            onCoordinateSelected = { lat, lon ->
                                militaryLat = lat
                                militaryLon = lon
                                currentScreen = AppScreen.MILITARY_GR
                            },
                            onBack = {
                                currentScreen = AppScreen.MILITARY_GR
                            }
                        )
                    }
                    AppScreen.COMPASS -> {
                        StandaloneCompassScreen(viewModel = viewModel)
                    }
                    AppScreen.INTEL -> {
                        GeminiIntelScreen(
                            viewModel = viewModel,
                            onNavigateBackToCalc = { currentScreen = AppScreen.CALCULATOR }
                        )
                    }
                    AppScreen.SAVED -> {
                        SavedRoutesScreen(
                            viewModel = viewModel,
                            onNavigateBackToCalc = { currentScreen = AppScreen.CALCULATOR }
                        )
                    }
                }
            }
        }
    }
}

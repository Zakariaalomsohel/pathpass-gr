package com.example.ui

import android.app.Application
import android.content.Context
import java.util.Locale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.theme.AppColorTheme
import com.example.ui.theme.DarkThemeOption
import com.example.data.api.ElevationService
import com.example.data.db.AppDatabase
import com.example.data.db.CachedMapAreaEntity
import com.example.data.db.SavedRouteEntity
import com.example.data.gemini.GeminiMapsClient
import com.example.data.gemini.PlaceSearchResult
import com.example.data.gemini.RouteAnalysisResult
import com.example.data.location.LocationHelper
import com.example.data.model.CalculationResult
import com.example.data.model.DistanceUnit
import com.example.data.model.GeoPoint
import com.example.data.model.MapBoundingBox
import com.example.data.model.NavigationCalculator
import com.example.data.model.OfflineCacheProgress
import com.example.data.model.OfflineMapManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val locationHelper = LocationHelper(application)
    private val realtimeLocationManager = com.example.calculator.RealtimeLocationManager.getInstance(application)

    // Input States
    private val _p1Name = MutableStateFlow("Site A [Terminal 1]")
    val p1Name: StateFlow<String> = _p1Name.asStateFlow()

    private val _p2Name = MutableStateFlow("Site B [Terminal 2]")
    val p2Name: StateFlow<String> = _p2Name.asStateFlow()

    private val _p1Lat = MutableStateFlow("")
    val p1Lat: StateFlow<String> = _p1Lat.asStateFlow()

    private val _p1Lon = MutableStateFlow("")
    val p1Lon: StateFlow<String> = _p1Lon.asStateFlow()

    private val _p1Elevation = MutableStateFlow<Double?>(null)
    val p1Elevation: StateFlow<Double?> = _p1Elevation.asStateFlow()

    private val _p1ElevationText = MutableStateFlow("")
    val p1ElevationText: StateFlow<String> = _p1ElevationText.asStateFlow()

    private val _p1ElevationLoading = MutableStateFlow(false)
    val p1ElevationLoading: StateFlow<Boolean> = _p1ElevationLoading.asStateFlow()

    private val _p2Lat = MutableStateFlow("")
    val p2Lat: StateFlow<String> = _p2Lat.asStateFlow()

    private val _p2Lon = MutableStateFlow("")
    val p2Lon: StateFlow<String> = _p2Lon.asStateFlow()

    private val _p2Elevation = MutableStateFlow<Double?>(null)
    val p2Elevation: StateFlow<Double?> = _p2Elevation.asStateFlow()

    private val _p2ElevationText = MutableStateFlow("")
    val p2ElevationText: StateFlow<String> = _p2ElevationText.asStateFlow()

    private val _p2ElevationLoading = MutableStateFlow(false)
    val p2ElevationLoading: StateFlow<Boolean> = _p2ElevationLoading.asStateFlow()

    // Calculation result
    private val _calculationResult = MutableStateFlow<CalculationResult?>(null)
    val calculationResult: StateFlow<CalculationResult?> = _calculationResult.asStateFlow()

    // Distance unit
    private val _selectedUnit = MutableStateFlow(DistanceUnit.KILOMETERS)
    val selectedUnit: StateFlow<DistanceUnit> = _selectedUnit.asStateFlow()

    // Antenna & LOS Parameters
    private val _p1AntennaHeight = MutableStateFlow("24")
    val p1AntennaHeight: StateFlow<String> = _p1AntennaHeight.asStateFlow()

    private val _p2AntennaHeight = MutableStateFlow("24")
    val p2AntennaHeight: StateFlow<String> = _p2AntennaHeight.asStateFlow()

    private val _frequencyMhz = MutableStateFlow(1350.0)
    val frequencyMhz: StateFlow<Double> = _frequencyMhz.asStateFlow()

    private val _frequency2Mhz = MutableStateFlow(2400.0)
    val frequency2Mhz: StateFlow<Double> = _frequency2Mhz.asStateFlow()

    // Power output (dBm) default 30 dBm, and Rx sensitivity (-47 to -87 dBm) default -87 dBm
    private val _txPowerDbm = MutableStateFlow(30.0)
    val txPowerDbm: StateFlow<Double> = _txPowerDbm.asStateFlow()

    private val _antennaGainDbi = MutableStateFlow(15.0)
    val antennaGainDbi: StateFlow<Double> = _antennaGainDbi.asStateFlow()

    private val _rxSensitivityDbm = MutableStateFlow(-87.0)
    val rxSensitivityDbm: StateFlow<Double> = _rxSensitivityDbm.asStateFlow()

    // GPS State
    private val _currentGps = MutableStateFlow<GeoPoint?>(null)
    val currentGps: StateFlow<GeoPoint?> = _currentGps.asStateFlow()

    private val _isGpsSearching = MutableStateFlow(false)
    val isGpsSearching: StateFlow<Boolean> = _isGpsSearching.asStateFlow()

    // Notifications & Messages
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Theme preferences & Colorful Theme state
    private val themePrefs = application.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)

    private val _selectedColorTheme = MutableStateFlow(
        run {
            val savedName = themePrefs.getString("color_theme", AppColorTheme.CYBER_EMERALD.name)
            try {
                AppColorTheme.valueOf(savedName ?: AppColorTheme.CYBER_EMERALD.name)
            } catch (e: Exception) {
                AppColorTheme.CYBER_EMERALD
            }
        }
    )
    val selectedColorTheme: StateFlow<AppColorTheme> = _selectedColorTheme.asStateFlow()

    private val _selectedDarkOption = MutableStateFlow(
        run {
            val savedMode = themePrefs.getString("dark_option", DarkThemeOption.DARK.name)
            try {
                DarkThemeOption.valueOf(savedMode ?: DarkThemeOption.DARK.name)
            } catch (e: Exception) {
                DarkThemeOption.DARK
            }
        }
    )
    val selectedDarkOption: StateFlow<DarkThemeOption> = _selectedDarkOption.asStateFlow()

    fun setColorTheme(theme: AppColorTheme) {
        _selectedColorTheme.value = theme
        themePrefs.edit().putString("color_theme", theme.name).apply()
        _userMessage.value = "থিম পরিবর্তন: ${theme.titleBn}"
    }

    fun setDarkOption(option: DarkThemeOption) {
        _selectedDarkOption.value = option
        themePrefs.edit().putString("dark_option", option.name).apply()
    }

    // Gemini Place Search and Route Intelligence State
    private val _geminiSearchLoading = MutableStateFlow(false)
    val geminiSearchLoading: StateFlow<Boolean> = _geminiSearchLoading.asStateFlow()

    private val _geminiSearchResult = MutableStateFlow<PlaceSearchResult?>(null)
    val geminiSearchResult: StateFlow<PlaceSearchResult?> = _geminiSearchResult.asStateFlow()

    private val _geminiRouteAnalysis = MutableStateFlow<RouteAnalysisResult?>(null)
    val geminiRouteAnalysis: StateFlow<RouteAnalysisResult?> = _geminiRouteAnalysis.asStateFlow()

    private val _geminiAnalysisLoading = MutableStateFlow(false)
    val geminiAnalysisLoading: StateFlow<Boolean> = _geminiAnalysisLoading.asStateFlow()

    // Saved Routes from Room
    val savedRoutes: StateFlow<List<SavedRouteEntity>> = db.routeDao().getAllRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Offline Map Area Caching State
    private val _isOfflineCached = MutableStateFlow(false)
    val isOfflineCached: StateFlow<Boolean> = _isOfflineCached.asStateFlow()

    private val _isCachingMap = MutableStateFlow(false)
    val isCachingMap: StateFlow<Boolean> = _isCachingMap.asStateFlow()

    private val _cachingProgress = MutableStateFlow<OfflineCacheProgress?>(null)
    val cachingProgress: StateFlow<OfflineCacheProgress?> = _cachingProgress.asStateFlow()

    private val _activeCachedArea = MutableStateFlow<CachedMapAreaEntity?>(null)
    val activeCachedArea: StateFlow<CachedMapAreaEntity?> = _activeCachedArea.asStateFlow()

    val cachedMapAreas: StateFlow<List<CachedMapAreaEntity>> = db.cachedMapAreaDao().getAllAreas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Coroutine Jobs for debounced elevation fetching
    private var p1ElevationJob: Job? = null
    private var p2ElevationJob: Job? = null

    init {
        // Synchronize GPS location with Army GR Menu from the identical source
        viewModelScope.launch {
            realtimeLocationManager.trackingState.collect { state ->
                if (state.latitude != 0.0 && state.longitude != 0.0) {
                    val geoPoint = GeoPoint(latitude = state.latitude, longitude = state.longitude, altitude = state.altitudeMeters)
                    _currentGps.value = geoPoint
                    if (state.isTracking || _p1Lat.value.isEmpty()) {
                        _p1Lat.value = String.format(Locale.US, "%.6f", state.latitude)
                        _p1Lon.value = String.format(Locale.US, "%.6f", state.longitude)
                        scheduleP1ElevationCheck()
                        if (_p2Lat.value.isNotEmpty() && _p2Lon.value.isNotEmpty()) {
                            calculateDistance()
                        }
                    }
                }
            }
        }

        // Preload GPS fix if permitted
        if (locationHelper.hasLocationPermission()) {
            fetchGpsFix(silent = true)
        }
    }

    fun onP1NameChanged(name: String) {
        _p1Name.value = name
        updateCalculationMetadata()
    }

    fun onP2NameChanged(name: String) {
        _p2Name.value = name
        updateCalculationMetadata()
    }

    private fun updateCalculationMetadata() {
        val current = _calculationResult.value ?: return
        _calculationResult.value = current.copy(
            point1 = current.point1.copy(label = _p1Name.value),
            point2 = current.point2.copy(label = _p2Name.value)
        )
    }

    fun onP1LatChanged(value: String) {
        _p1Lat.value = value
        scheduleP1ElevationCheck()
    }

    fun onP1LonChanged(value: String) {
        _p1Lon.value = value
        scheduleP1ElevationCheck()
    }

    fun onP2LatChanged(value: String) {
        _p2Lat.value = value
        scheduleP2ElevationCheck()
    }

    fun onP2LonChanged(value: String) {
        _p2Lon.value = value
        scheduleP2ElevationCheck()
    }

    fun onP1ElevationChanged(value: String) {
        p1ElevationJob?.cancel()
        _p1ElevationText.value = value
        val parsed = value.trim().toDoubleOrNull()
        _p1Elevation.value = parsed
        updateCalculationElevation()
    }

    fun onP2ElevationChanged(value: String) {
        p2ElevationJob?.cancel()
        _p2ElevationText.value = value
        val parsed = value.trim().toDoubleOrNull()
        _p2Elevation.value = parsed
        updateCalculationElevation()
    }

    fun setUnit(unit: DistanceUnit) {
        _selectedUnit.value = unit
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    private fun scheduleP1ElevationCheck() {
        p1ElevationJob?.cancel()
        val lat = _p1Lat.value.trim().toDoubleOrNull()
        val lon = _p1Lon.value.trim().toDoubleOrNull()

        if (lat == null || lon == null || lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
            _p1Elevation.value = null
            _p1ElevationText.value = ""
            _p1ElevationLoading.value = false
            return
        }

        checkOfflineStatusForCurrentPoints()

        p1ElevationJob = viewModelScope.launch {
            _p1ElevationLoading.value = true
            val result = ElevationService.getElevation(lat, lon, getApplication())
            _p1ElevationLoading.value = false
            result.onSuccess { elev ->
                _p1Elevation.value = elev
                _p1ElevationText.value = if (kotlin.math.abs(elev - kotlin.math.round(elev * 10.0) / 10.0) < 0.005) {
                    String.format(Locale.US, "%.1f", elev)
                } else {
                    String.format(Locale.US, "%.2f", elev)
                }
                updateCalculationElevation()
            }.onFailure {
                _p1Elevation.value = null
            }
        }
    }

    private fun scheduleP2ElevationCheck() {
        p2ElevationJob?.cancel()
        val lat = _p2Lat.value.trim().toDoubleOrNull()
        val lon = _p2Lon.value.trim().toDoubleOrNull()

        if (lat == null || lon == null || lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
            _p2Elevation.value = null
            _p2ElevationText.value = ""
            _p2ElevationLoading.value = false
            return
        }

        checkOfflineStatusForCurrentPoints()

        p2ElevationJob = viewModelScope.launch {
            _p2ElevationLoading.value = true
            val result = ElevationService.getElevation(lat, lon, getApplication())
            _p2ElevationLoading.value = false
            result.onSuccess { elev ->
                _p2Elevation.value = elev
                _p2ElevationText.value = if (kotlin.math.abs(elev - kotlin.math.round(elev * 10.0) / 10.0) < 0.005) {
                    String.format(Locale.US, "%.1f", elev)
                } else {
                    String.format(Locale.US, "%.2f", elev)
                }
                updateCalculationElevation()
            }.onFailure {
                _p2Elevation.value = null
            }
        }
    }

    private fun updateCalculationElevation() {
        val current = _calculationResult.value ?: return
        val elev1 = _p1Elevation.value
        val elev2 = _p2Elevation.value
        val elevDelta = if (elev1 != null && elev2 != null) elev2 - elev1 else null
        _calculationResult.value = current.copy(
            point1 = current.point1.copy(altitude = elev1),
            point2 = current.point2.copy(altitude = elev2),
            elevationDeltaMeters = elevDelta
        )
    }

    fun fetchGpsFix(silent: Boolean = false) {
        if (!locationHelper.hasLocationPermission()) {
            if (!silent) _userMessage.value = "লোকেশন পারমিশন প্রয়োজন"
            return
        }

        if (!locationHelper.isGpsEnabled()) {
            if (!silent) _userMessage.value = "GPS বন্ধ আছে। চালু করুন।"
            return
        }

        _isGpsSearching.value = true
        realtimeLocationManager.requestSingleUpdate { success ->
            _isGpsSearching.value = false
            if (success) {
                val state = realtimeLocationManager.trackingState.value
                val loc = GeoPoint(latitude = state.latitude, longitude = state.longitude, altitude = state.altitudeMeters)
                _currentGps.value = loc
                if (!silent) {
                    _p1Lat.value = String.format(Locale.US, "%.6f", state.latitude)
                    _p1Lon.value = String.format(Locale.US, "%.6f", state.longitude)
                    scheduleP1ElevationCheck()
                    _userMessage.value = "Location পাওয়া গেছে (Army GR GPS এর সাথে সিংক্রোনাইজড)"
                    if (_p2Lat.value.isNotEmpty() && _p2Lon.value.isNotEmpty()) {
                        calculateDistance()
                    }
                }
            } else {
                viewModelScope.launch {
                    val loc = locationHelper.requestSingleFreshLocation() ?: locationHelper.getLastKnownLocation()
                    if (loc != null) {
                        _currentGps.value = loc
                        if (!silent) {
                            _p1Lat.value = String.format(Locale.US, "%.6f", loc.latitude)
                            _p1Lon.value = String.format(Locale.US, "%.6f", loc.longitude)
                            scheduleP1ElevationCheck()
                            _userMessage.value = "Location পাওয়া গেছে"
                            if (_p2Lat.value.isNotEmpty() && _p2Lon.value.isNotEmpty()) {
                                calculateDistance()
                            }
                        }
                    } else if (!silent) {
                        _userMessage.value = "Location খুঁজছে... একটু অপেক্ষা করুন"
                    }
                }
            }
        }
    }

    fun setCurrentLocationAsPoint1() {
        if (!locationHelper.hasLocationPermission()) {
            _userMessage.value = "লোকেশন পারমিশন প্রয়োজন"
            return
        }

        if (!locationHelper.isGpsEnabled()) {
            _userMessage.value = "GPS বন্ধ আছে। চালু করুন।"
            return
        }

        _isGpsSearching.value = true
        realtimeLocationManager.requestSingleUpdate { success ->
            _isGpsSearching.value = false
            if (success) {
                val state = realtimeLocationManager.trackingState.value
                val newLoc = GeoPoint(latitude = state.latitude, longitude = state.longitude, altitude = state.altitudeMeters)
                _currentGps.value = newLoc
                _p1Lat.value = String.format(Locale.US, "%.6f", state.latitude)
                _p1Lon.value = String.format(Locale.US, "%.6f", state.longitude)
                scheduleP1ElevationCheck()
                _userMessage.value = "Point 1 (Observer) লোকেশন GPS আপডেট হয়েছে"
                if (_p2Lat.value.isNotEmpty() && _p2Lon.value.isNotEmpty()) {
                    calculateDistance()
                }
            } else {
                viewModelScope.launch {
                    val loc = locationHelper.requestSingleFreshLocation() ?: locationHelper.getLastKnownLocation()
                    if (loc != null) {
                        _currentGps.value = loc
                        _p1Lat.value = String.format(Locale.US, "%.6f", loc.latitude)
                        _p1Lon.value = String.format(Locale.US, "%.6f", loc.longitude)
                        scheduleP1ElevationCheck()
                        _userMessage.value = "Point 1 (Observer) লোকেশন GPS আপডেট হয়েছে"
                        if (_p2Lat.value.isNotEmpty() && _p2Lon.value.isNotEmpty()) {
                            calculateDistance()
                        }
                    } else {
                        _userMessage.value = "GPS সিগন্যাল খুঁজছে... খোলা আকাশের নিচে যান"
                    }
                }
            }
        }
    }

    fun setCurrentLocationAsPoint2() {
        if (!locationHelper.hasLocationPermission()) {
            _userMessage.value = "লোকেশন পারমিশন প্রয়োজন"
            return
        }

        if (!locationHelper.isGpsEnabled()) {
            _userMessage.value = "GPS বন্ধ আছে। চালু করুন।"
            return
        }

        _isGpsSearching.value = true
        realtimeLocationManager.requestSingleUpdate { success ->
            _isGpsSearching.value = false
            if (success) {
                val state = realtimeLocationManager.trackingState.value
                val newLoc = GeoPoint(latitude = state.latitude, longitude = state.longitude, altitude = state.altitudeMeters)
                _currentGps.value = newLoc
                _p2Lat.value = String.format(Locale.US, "%.6f", state.latitude)
                _p2Lon.value = String.format(Locale.US, "%.6f", state.longitude)
                scheduleP2ElevationCheck()
                _userMessage.value = "Point 2 (Target) এর GPS লোকেশন আপডেট হয়েছে"
                if (_p1Lat.value.isNotEmpty() && _p1Lon.value.isNotEmpty()) {
                    calculateDistance()
                }
            } else {
                viewModelScope.launch {
                    val loc = locationHelper.requestSingleFreshLocation() ?: locationHelper.getLastKnownLocation()
                    if (loc != null) {
                        _currentGps.value = loc
                        _p2Lat.value = String.format(Locale.US, "%.6f", loc.latitude)
                        _p2Lon.value = String.format(Locale.US, "%.6f", loc.longitude)
                        scheduleP2ElevationCheck()
                        _userMessage.value = "Point 2 (Target) এর GPS লোকেশন আপডেট হয়েছে"
                        if (_p1Lat.value.isNotEmpty() && _p1Lon.value.isNotEmpty()) {
                            calculateDistance()
                        }
                    } else {
                        _userMessage.value = "GPS সিগন্যাল খুঁজছে... খোলা আকাশের নিচে যান"
                    }
                }
            }
        }
    }

    fun swapPoints() {
        val tempName = _p1Name.value
        val tempLat = _p1Lat.value
        val tempLon = _p1Lon.value
        val tempElev = _p1Elevation.value
        val tempElevText = _p1ElevationText.value

        _p1Name.value = _p2Name.value
        _p1Lat.value = _p2Lat.value
        _p1Lon.value = _p2Lon.value
        _p1Elevation.value = _p2Elevation.value
        _p1ElevationText.value = _p2ElevationText.value

        _p2Name.value = tempName
        _p2Lat.value = tempLat
        _p2Lon.value = tempLon
        _p2Elevation.value = tempElev
        _p2ElevationText.value = tempElevText

        if (_p1Lat.value.isNotEmpty() && _p2Lat.value.isNotEmpty()) {
            calculateDistance()
        }
    }

    fun clearAll() {
        _p1Name.value = "Site A [Terminal 1]"
        _p1Lat.value = ""
        _p1Lon.value = ""
        _p1Elevation.value = null
        _p1ElevationText.value = ""
        _p2Name.value = "Site B [Terminal 2]"
        _p2Lat.value = ""
        _p2Lon.value = ""
        _p2Elevation.value = null
        _p2ElevationText.value = ""
        _calculationResult.value = null
    }

    fun calculateDistance(): Boolean {
        val lat1Str = _p1Lat.value.trim()
        val lon1Str = _p1Lon.value.trim()
        val lat2Str = _p2Lat.value.trim()
        val lon2Str = _p2Lon.value.trim()

        if (lat1Str.isEmpty() || lon1Str.isEmpty() || lat2Str.isEmpty() || lon2Str.isEmpty()) {
            _userMessage.value = "সব ফিল্ড পূরণ করুন"
            return false
        }

        val lat1 = lat1Str.toDoubleOrNull()
        val lon1 = lon1Str.toDoubleOrNull()
        val lat2 = lat2Str.toDoubleOrNull()
        val lon2 = lon2Str.toDoubleOrNull()

        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            _userMessage.value = "সঠিক সংখ্যা লিখুন"
            return false
        }

        if (lat1 < -90.0 || lat1 > 90.0 || lat2 < -90.0 || lat2 > 90.0 ||
            lon1 < -180.0 || lon1 > 180.0 || lon2 < -180.0 || lon2 > 180.0) {
            _userMessage.value = "অক্ষাংশ (-90 to 90) ও দ্রাঘিমাংশ (-180 to 180) সঠিক রেঞ্জে দিন"
            return false
        }

        val result = NavigationCalculator.calculate(
            lat1 = lat1,
            lon1 = lon1,
            lat2 = lat2,
            lon2 = lon2,
            p1Elevation = _p1Elevation.value,
            p2Elevation = _p2Elevation.value,
            p1Label = _p1Name.value,
            p2Label = _p2Name.value
        )
        _calculationResult.value = result
        return true
    }

    fun saveRoute(title: String = "") {
        val calc = _calculationResult.value ?: return
        viewModelScope.launch {
            val routeTitle = if (title.isNotBlank()) title else "${_p1Name.value} ➔ ${_p2Name.value} (${calc.bearingRounded}°)"
            val entity = SavedRouteEntity(
                title = routeTitle,
                p1Lat = calc.point1.latitude,
                p1Lon = calc.point1.longitude,
                p1Label = _p1Name.value,
                p1Elevation = _p1Elevation.value,
                p2Lat = calc.point2.latitude,
                p2Lon = calc.point2.longitude,
                p2Label = _p2Name.value,
                p2Elevation = _p2Elevation.value,
                distanceKm = calc.distanceMeters / 1000.0,
                bearingDegrees = calc.bearingDegrees,
                backBearingDegrees = calc.backBearingDegrees
            )
            db.routeDao().insertRoute(entity)
            _userMessage.value = "রুট সফলভাবে সেভ করা হয়েছে"
        }
    }

    fun deleteRoute(route: SavedRouteEntity) {
        viewModelScope.launch {
            db.routeDao().deleteRoute(route)
            _userMessage.value = "রুট মুছে ফেলা হয়েছে"
        }
    }

    fun loadRoute(route: SavedRouteEntity) {
        _p1Name.value = if (route.p1Label.isNotBlank()) route.p1Label else "Terminal 1"
        _p1Lat.value = route.p1Lat.toString()
        _p1Lon.value = route.p1Lon.toString()
        _p1Elevation.value = route.p1Elevation
        _p1ElevationText.value = route.p1Elevation?.let {
            if (kotlin.math.abs(it - kotlin.math.round(it * 10.0) / 10.0) < 0.005) String.format(Locale.US, "%.1f", it) else String.format(Locale.US, "%.2f", it)
        } ?: ""
        _p2Name.value = if (route.p2Label.isNotBlank()) route.p2Label else "Terminal 2"
        _p2Lat.value = route.p2Lat.toString()
        _p2Lon.value = route.p2Lon.toString()
        _p2Elevation.value = route.p2Elevation
        _p2ElevationText.value = route.p2Elevation?.let {
            if (kotlin.math.abs(it - kotlin.math.round(it * 10.0) / 10.0) < 0.005) String.format(Locale.US, "%.1f", it) else String.format(Locale.US, "%.2f", it)
        } ?: ""

        calculateDistance()
        _userMessage.value = "রুট লোড হয়েছে: ${route.title}"
    }

    fun searchPlaceGemini(query: String) {
        if (query.isBlank()) return
        _geminiSearchLoading.value = true
        _geminiSearchResult.value = null

        val currentLoc = _currentGps.value?.let { Pair(it.latitude, it.longitude) }

        viewModelScope.launch {
            val result = GeminiMapsClient.searchPlaceWithGoogleMaps(query, currentLoc)
            _geminiSearchLoading.value = false
            result.onSuccess { searchResult ->
                _geminiSearchResult.value = searchResult
            }.onFailure { e ->
                _userMessage.value = "অনুসন্ধান ব্যর্থ: ${e.message}"
            }
        }
    }

    fun setSearchResultAsPoint1(result: PlaceSearchResult) {
        _p1Name.value = result.name
        _p1Lat.value = result.latitude.toString()
        _p1Lon.value = result.longitude.toString()
        scheduleP1ElevationCheck()
        _userMessage.value = "${result.name} কে Point 1 হিসেবে সেট করা হয়েছে"
    }

    fun setSearchResultAsPoint2(result: PlaceSearchResult) {
        _p2Name.value = result.name
        _p2Lat.value = result.latitude.toString()
        _p2Lon.value = result.longitude.toString()
        scheduleP2ElevationCheck()
        _userMessage.value = "${result.name} কে Point 2 হিসেবে সেট করা হয়েছে"
    }

    fun analyzeRouteGemini() {
        val calc = _calculationResult.value
        if (calc == null) {
            _userMessage.value = "দয়া করে প্রথমে হিসাব করুন"
            return
        }

        _geminiAnalysisLoading.value = true
        _geminiRouteAnalysis.value = null

        viewModelScope.launch {
            val result = GeminiMapsClient.analyzeRouteWithMaps(
                p1 = calc.point1,
                p2 = calc.point2,
                distanceKm = calc.distanceMeters / 1000.0,
                bearingDeg = calc.bearingDegrees,
                p1Elevation = _p1Elevation.value,
                p2Elevation = _p2Elevation.value
            )
            _geminiAnalysisLoading.value = false
            result.onSuccess { analysis ->
                _geminiRouteAnalysis.value = analysis
            }.onFailure { e ->
                _userMessage.value = "রুট বিশ্লেষণ ত্রুটি: ${e.message}"
            }
        }
    }

    fun setP1AntennaHeight(h: String) {
        _p1AntennaHeight.value = h
    }

    fun setP2AntennaHeight(h: String) {
        _p2AntennaHeight.value = h
    }

    fun setFrequencyMhz(freq: Double) {
        _frequencyMhz.value = freq
    }

    fun setFrequency2Mhz(freq: Double) {
        _frequency2Mhz.value = freq
    }

    fun setTxPowerDbm(power: Double) {
        _txPowerDbm.value = power
    }

    fun setRxSensitivityDbm(sens: Double) {
        _rxSensitivityDbm.value = sens
    }

    fun setAntennaGainDbi(gain: Double) {
        _antennaGainDbi.value = gain
    }

    fun setCoordinatesAsPoint1(lat: Double, lon: Double) {
        _p1Lat.value = "%.5f".format(java.util.Locale.US, lat)
        _p1Lon.value = "%.5f".format(java.util.Locale.US, lon)
        scheduleP1ElevationCheck()
        _userMessage.value = "Point 1 সেট করা হয়েছে: ${_p1Lat.value}, ${_p1Lon.value}"
    }

    fun setCoordinatesAsPoint2(lat: Double, lon: Double) {
        _p2Lat.value = "%.5f".format(java.util.Locale.US, lat)
        _p2Lon.value = "%.5f".format(java.util.Locale.US, lon)
        scheduleP2ElevationCheck()
        _userMessage.value = "Point 2 সেট করা হয়েছে: ${_p2Lat.value}, ${_p2Lon.value}"
    }

    fun shareLiveLocation(context: android.content.Context) {
        val gps = _currentGps.value
        val elev = _p1Elevation.value

        val lat: Double
        val lon: Double

        if (gps != null) {
            lat = gps.latitude
            lon = gps.longitude
        } else {
            val p1LatVal = _p1Lat.value.toDoubleOrNull()
            val p1LonVal = _p1Lon.value.toDoubleOrNull()
            if (p1LatVal != null && p1LonVal != null) {
                lat = p1LatVal
                lon = p1LonVal
            } else {
                fetchGpsFix(silent = false)
                _userMessage.value = "GPS লোকেশন অনুসন্ধান করা হচ্ছে, একটু পর চেষ্টা করুন"
                return
            }
        }

        val elevStr = if (elev != null) "${"%.1f".format(elev)} m (${"%.1f".format(elev * 3.28084)} ft)" else "N/A"
        val mapsLink = "https://maps.google.com/?q=$lat,$lon"
        val dateStr = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

        val shareText = """
            📍 LIVE GPS LOCATION & ELEVATION REPORT
            ---------------------------------------
            Latitude: $lat
            Longitude: $lon
            Elevation above Sea Level: $elevStr
            Google Maps Link: $mapsLink
            Timestamp: $dateStr
            ---------------------------------------
            Author: 1618101 Cpl (Tech) Md Zakaria Alom, 3 Signal Battalion
        """.trimIndent()

        val sendIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val chooser = android.content.Intent.createChooser(sendIntent, "Share Live Location via")
        context.startActivity(chooser)
    }

    fun scanObstaclesWithGemini() {
        val calc = _calculationResult.value
        if (calc == null) {
            _userMessage.value = "পাহাড় ও বিল্ডিং স্ক্যানের জন্য প্রথমে হিসাব সম্পন্ন করুন"
            return
        }

        _geminiAnalysisLoading.value = true
        _geminiRouteAnalysis.value = null

        viewModelScope.launch {
            val result = GeminiMapsClient.analyzeRouteWithMaps(
                p1 = calc.point1,
                p2 = calc.point2,
                distanceKm = calc.distanceMeters / 1000.0,
                bearingDeg = calc.bearingDegrees,
                p1Elevation = _p1Elevation.value,
                p2Elevation = _p2Elevation.value
            )
            _geminiAnalysisLoading.value = false
            result.onSuccess { analysis ->
                _geminiRouteAnalysis.value = analysis
                _userMessage.value = "পাহাড়, বিল্ডিং ও বনভূমির স্ক্যান রিপোর্ট প্রস্তুত"
            }.onFailure { e ->
                _userMessage.value = "স্ক্যান ত্রুটি: ${e.message}"
            }
        }
    }

    // OFFLINE MAP AREA CACHING IMPLEMENTATION
    fun checkOfflineStatusForCurrentPoints() {
        val lat1 = _p1Lat.value.trim().toDoubleOrNull()
        val lon1 = _p1Lon.value.trim().toDoubleOrNull()
        if (lat1 == null || lon1 == null) return

        viewModelScope.launch {
            val db = AppDatabase.getInstance(getApplication())
            val area1 = db.cachedMapAreaDao().findAreaContaining(lat1, lon1)
            val lat2 = _p2Lat.value.trim().toDoubleOrNull()
            val lon2 = _p2Lon.value.trim().toDoubleOrNull()

            if (area1 != null && (lat2 == null || lon2 == null || area1.minLat <= lat2 && area1.maxLat >= lat2 && area1.minLon <= lon2 && area1.maxLon >= lon2)) {
                _isOfflineCached.value = true
                _activeCachedArea.value = area1
            } else {
                _isOfflineCached.value = false
                _activeCachedArea.value = null
            }
        }
    }

    fun toggleOfflineCache(enable: Boolean) {
        if (enable) {
            cacheVisibleMapArea()
        } else {
            _isOfflineCached.value = false
            _userMessage.value = "অফলাইন ম্যাপ ক্যাশ মোড বন্ধ করা হয়েছে"
        }
    }

    fun cacheVisibleMapArea(customName: String? = null) {
        val lat1 = _p1Lat.value.trim().toDoubleOrNull()
        val lon1 = _p1Lon.value.trim().toDoubleOrNull()
        val lat2 = _p2Lat.value.trim().toDoubleOrNull()
        val lon2 = _p2Lon.value.trim().toDoubleOrNull()

        val gps = _currentGps.value

        val startLat = lat1 ?: gps?.latitude
        val startLon = lon1 ?: gps?.longitude

        if (startLat == null || startLon == null) {
            _userMessage.value = "ম্যাপ এরিয়া ক্যাশ করতে Point 1 অথবা GPS লোকেশন প্রয়োজন"
            return
        }

        val bbox = OfflineMapManager.calculateBoundingBox(
            p1Lat = startLat,
            p1Lon = startLon,
            p2Lat = lat2,
            p2Lon = lon2
        )

        val name = customName ?: if (lat2 != null && lon2 != null) {
            "Route Area (${"%.2f".format(startLat)},${"%.2f".format(startLon)} -> ${"%.2f".format(lat2)},${"%.2f".format(lon2)})"
        } else {
            "Tactical Area (${"%.2f".format(startLat)},${"%.2f".format(startLon)})"
        }

        _isCachingMap.value = true
        viewModelScope.launch {
            val result = OfflineMapManager.cacheVisibleArea(
                context = getApplication(),
                bbox = bbox,
                areaName = name,
                onProgress = { progress ->
                    _cachingProgress.value = progress
                }
            )

            _isCachingMap.value = false
            result.onSuccess { area ->
                _isOfflineCached.value = true
                _activeCachedArea.value = area
                _userMessage.value = "🟢 ${area.name} ক্যাশ সম্পন্ন! (${area.tileCount} টাইল, ${area.elevationPointCount} এলিভেশন পয়েন্ট)"
            }.onFailure { err ->
                _isOfflineCached.value = false
                _userMessage.value = "ক্যাশিং ত্রুটি: ${err.localizedMessage}"
            }
        }
    }

    fun clearOfflineCache() {
        viewModelScope.launch {
            OfflineMapManager.clearAllOfflineCache(getApplication())
            _isOfflineCached.value = false
            _activeCachedArea.value = null
            _cachingProgress.value = null
            _userMessage.value = "সকল অফলাইন ম্যাপ ও এলিভেশন ক্যাশ মুছে ফেলা হয়েছে"
        }
    }

    fun deleteCachedArea(area: CachedMapAreaEntity) {
        viewModelScope.launch {
            val db = AppDatabase.getInstance(getApplication())
            db.cachedElevationPointDao().deletePointsForArea(area.id)
            db.cachedMapAreaDao().deleteArea(area)
            if (_activeCachedArea.value?.id == area.id) {
                _activeCachedArea.value = null
                _isOfflineCached.value = false
            }
            _userMessage.value = "${area.name} এর ক্যাশ মুছে ফেলা হয়েছে"
        }
    }
}

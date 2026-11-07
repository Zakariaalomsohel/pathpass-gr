package com.example

import com.example.data.model.CalculationResult
import com.example.data.model.DistanceUnit
import com.example.data.model.KmlExporter
import com.example.data.model.LosAnalysisResult
import com.example.data.model.LosCalculator
import com.example.data.model.NavigationCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testDistanceAndBearingCalculation() {
        // Dhaka (23.8103, 90.4125) to Rajshahi (24.3636, 88.6241)
        val lat1 = 23.8103
        val lon1 = 90.4125
        val lat2 = 24.3636
        val lon2 = 88.6241

        val result = NavigationCalculator.calculate(lat1, lon1, lat2, lon2, 19.0, 21.0)

        val km = result.distanceMeters / 1000.0
        assertTrue("Distance should be around 190 km, got $km", km in 170.0..210.0)
        assertTrue("Bearing should be WNW/NW, got ${result.bearingDegrees}", result.bearingDegrees in 270.0..330.0)

        val diff = kotlin.math.abs(result.backBearingDegrees - ((result.bearingDegrees + 180) % 360))
        assertTrue("Back bearing reciprocal check, got diff $diff", diff < 5.0)

        val expectedMils = ((result.bearingDegrees / 360.0) * 6400).toInt()
        assertEquals(expectedMils, result.bearingMils)
    }

    @Test
    fun testLineOfSightAndFresnelCalculation() {
        val clearLos = LosCalculator.calculateLineOfSight(
            distanceKm = 5.0,
            p1Elevation = 20.0,
            p1AntennaHeight = 15.0,
            p2Elevation = 20.0,
            p2AntennaHeight = 15.0,
            frequencyMhz = 5800.0
        )
        assertTrue("Short distance with 5.8GHz and 15m towers should be CLEAR", clearLos.isLosClear)
        assertEquals(LosAnalysisResult.StatusColor.CLEAR, clearLos.statusColorType)
        assertTrue(clearLos.clearanceMarginMeters > 0)

        val marginalLos = LosCalculator.calculateLineOfSight(
            distanceKm = 5.0,
            p1Elevation = 20.0,
            p1AntennaHeight = 15.0,
            p2Elevation = 20.0,
            p2AntennaHeight = 15.0,
            frequencyMhz = 400.0
        )
        assertEquals(LosAnalysisResult.StatusColor.MARGINAL, marginalLos.statusColorType)

        val longObstructed = LosCalculator.calculateLineOfSight(
            distanceKm = 50.0,
            p1Elevation = 10.0,
            p1AntennaHeight = 10.0,
            p2Elevation = 10.0,
            p2AntennaHeight = 10.0,
            frequencyMhz = 400.0
        )
        assertTrue("50 km with 10m towers should be obstructed by Earth curvature", !longObstructed.isLosClear)
        assertEquals(LosAnalysisResult.StatusColor.OBSTRUCTED, longObstructed.statusColorType)
        assertTrue("Earth bulge should be > 30m for 50km", longObstructed.earthCurvatureBulgeMeters > 30.0)
    }

    @Test
    fun testRfLinkBudgetAndDigitalPathProfile() {
        // Test with user requirements:
        // Frequency: 1350 MHz
        // Distance: 10 km
        // Tx Power: 30 dBm
        // Rx Sensitivity: -87 dBm
        val result = LosCalculator.calculateLineOfSight(
            distanceKm = 10.0,
            p1Elevation = 20.0,
            p1AntennaHeight = 24.0,
            p2Elevation = 20.0,
            p2AntennaHeight = 24.0,
            frequencyMhz = 1350.0,
            txPowerDbm = 30.0,
            rxSensitivityDbm = -87.0
        )

        val budget = result.linkBudget
        assertEquals(30.0, budget.txPowerDbm, 0.01)
        assertEquals(-87.0, budget.rxSensitivityDbm, 0.01)
        assertEquals(1350.0, budget.frequencyMhz, 0.01)

        // Expected FSPL = 20*log10(10) + 20*log10(1350) + 32.44 = 20 + 62.6067 + 32.44 = 115.047 dB
        assertEquals(115.05, budget.freeSpacePathLossDb, 0.1)

        // RSL = 30 + 15 + 15 - 2 - 115.05 - L_obs
        // At 10km with 24m towers, line is clear so L_obs = 0
        assertTrue("RSL should be around -57 dBm, got ${budget.receivedSignalLevelDbm}", budget.receivedSignalLevelDbm in -60.0..-55.0)

        // Fade Margin = RSL - (-87) ~ +30 dB
        assertTrue("Fade margin should be ~30 dB, got ${budget.fadeMarginDb}", budget.fadeMarginDb in 25.0..35.0)
        assertEquals(LosAnalysisResult.StatusColor.CLEAR, budget.linkStatusColor)

        // Verify Sensitivity adjustment to -47 dBm (minimum sensitivity)
        val lowSensResult = LosCalculator.calculateLineOfSight(
            distanceKm = 10.0,
            p1Elevation = 20.0,
            p1AntennaHeight = 24.0,
            p2Elevation = 20.0,
            p2AntennaHeight = 24.0,
            frequencyMhz = 1350.0,
            txPowerDbm = 30.0,
            rxSensitivityDbm = -47.0
        )
        // With -47 dBm threshold, fade margin is reduced by 40 dB: ~ -10 dB
        assertTrue("Fade margin at -47 dBm sensitivity should be around -10 dB", lowSensResult.linkBudget.fadeMarginDb in -15.0..-5.0)

        // Verify Maximum Ultra Sensitivity up to -130 dBm
        val maxSensResult = LosCalculator.calculateLineOfSight(
            distanceKm = 10.0,
            p1Elevation = 20.0,
            p1AntennaHeight = 24.0,
            p2Elevation = 20.0,
            p2AntennaHeight = 24.0,
            frequencyMhz = 1350.0,
            txPowerDbm = 30.0,
            rxSensitivityDbm = -130.0
        )
        // With -130 dBm threshold, fade margin is ~ +73 dB
        assertTrue("Fade margin at -130 dBm sensitivity should be around +73 dB", maxSensResult.linkBudget.fadeMarginDb in 68.0..78.0)
        assertEquals(-130.0, maxSensResult.linkBudget.rxSensitivityDbm, 0.01)
    }

    @Test
    fun testKmlExportGeneration() {
        val calc = NavigationCalculator.calculate(23.8103, 90.4125, 24.3636, 88.6241, 19.0, 21.0)
        val los = LosCalculator.calculateLineOfSight(190.0, 19.0, 30.0, 21.0, 30.0, 400.0)

        val kml = KmlExporter.generateKmlContent(
            calcResult = calc,
            p1Elevation = 19.0,
            p1AntennaHeight = 30.0,
            p2Elevation = 21.0,
            p2AntennaHeight = 30.0,
            frequencyMhz = 400.0,
            losResult = los
        )

        assertTrue("KML must contain kml tag", kml.contains("<kml xmlns=\"http://www.opengis.net/kml/2.2\""))
        assertTrue("KML must contain Terminal 1 placemark", kml.contains("Site A [Terminal 1]"))
        assertTrue("KML must contain Terminal 2 placemark", kml.contains("Site B [Terminal 2]"))
        assertTrue("KML must contain 3D Line of Sight Ray", kml.contains("<name>3D Line of Sight (LOS Ray)</name>"))
        assertTrue("KML must contain author credit", kml.contains("1618101 Cpl (Tech) Md Zakaria Alom, 3 Signal Battalion"))
    }

    @Test
    fun testCardinalDirectionHelper() {
        assertEquals("N", CalculationResult.getCardinalDirection(0.0))
        assertEquals("N", CalculationResult.getCardinalDirection(359.0))
        assertEquals("E", CalculationResult.getCardinalDirection(90.0))
        assertEquals("S", CalculationResult.getCardinalDirection(180.0))
        assertEquals("W", CalculationResult.getCardinalDirection(270.0))
        assertEquals("NE", CalculationResult.getCardinalDirection(45.0))
        assertEquals("SW", CalculationResult.getCardinalDirection(225.0))
    }

    @Test
    fun testUnitConversion() {
        val meters = 5000.0
        assertEquals(5.0, DistanceUnit.KILOMETERS.convertFromMeters(meters), 0.001)
        assertEquals(5000.0, DistanceUnit.METERS.convertFromMeters(meters), 0.001)
        assertEquals(3.10686, DistanceUnit.MILES.convertFromMeters(meters), 0.01)
        assertEquals(2.69978, DistanceUnit.NAUTICAL_MILES.convertFromMeters(meters), 0.01)
    }

    @Test
    fun testSlopeCalculation() {
        val (grade, angle) = NavigationCalculator.calculateSlope(1000.0, 100.0)
        assertEquals(10.0, grade, 0.01)
        assertTrue(angle > 5.0 && angle < 6.0)
    }

    @Test
    fun testOfflineMapBoundingBoxAndTiles() {
        // Dhaka coordinates
        val lat1 = 23.8103
        val lon1 = 90.4125
        val lat2 = 24.3636
        val lon2 = 88.6241

        val bbox = com.example.data.model.OfflineMapManager.calculateBoundingBox(lat1, lon1, lat2, lon2)
        assertTrue(bbox.contains(lat1, lon1))
        assertTrue(bbox.contains(lat2, lon2))
        assertTrue(bbox.minLat < 23.8103)
        assertTrue(bbox.maxLat > 24.3636)

        // Web Mercator tile coordinates test for Zoom 13
        val tileX = com.example.data.model.OfflineMapManager.lonToTileX(lon1, 13)
        val tileY = com.example.data.model.OfflineMapManager.latToTileY(lat1, 13)
        assertTrue("TileX should be positive for eastern hemisphere", tileX > 0)
        assertTrue("TileY should be positive for northern hemisphere", tileY > 0)

        val restoredLon = com.example.data.model.OfflineMapManager.tileXToLon(tileX, 13)
        assertTrue("Restored Lon should be close to original", kotlin.math.abs(restoredLon - lon1) < 0.1)
    }

    @Test
    fun testButmConversion() {
        // Dhaka coordinates (23.8103 N, 90.4125 E)
        val lat = 23.8103
        val lon = 90.4125
        val butm = com.example.data.model.ButmConversion.latLonToButmGrid(lat, lon)
        assertTrue("Easting should be ~542,018m, got ${butm.easting}", butm.easting in 541000.0..543000.0)
        assertTrue("Northing should be ~2,633,814m, got ${butm.northing}", butm.northing in 2632000.0..2635000.0)
        assertEquals("79 I/5", butm.mapSheetNo)
        assertEquals("4201 3306", butm.militaryGr8)
        assertEquals("420 330", butm.militaryGr6)

        // Roundtrip conversion (Easting/Northing -> Lat/Lon)
        val roundtrip = com.example.data.model.ButmConversion.butmGridToLatLon(butm.easting, butm.northing)
        assertEquals(lat, roundtrip.latitude, 0.00001)
        assertEquals(lon, roundtrip.longitude, 0.00001)
        assertEquals("79 I/5", roundtrip.mapSheetNo)

        // Bidirectional: Map Sheet + 8-Fig GR -> Lat/Lon
        val reverse8 = com.example.data.model.ButmConversion.grAndMapSheetToLatLon("79 I/5", "4201 3306")
        assertEquals(lat, reverse8.latitude, 0.001)
        assertEquals(lon, reverse8.longitude, 0.001)
        assertEquals("79 I/5", reverse8.confirmedMapSheet)

        // Bidirectional: Map Sheet + 6-Fig GR -> Lat/Lon
        val reverse6 = com.example.data.model.ButmConversion.grAndMapSheetToLatLon("79 I/5", "420 330")
        assertEquals(lat, reverse6.latitude, 0.005)
        assertEquals(lon, reverse6.longitude, 0.005)

        // Bengali numeral input test: "৭৯ I/৫" and "৪২০১ ৩৩০৬"
        val reverseBn = com.example.data.model.ButmConversion.grAndMapSheetToLatLon("৭৯ I/৫", "৪২০১ ৩৩০৬")
        assertEquals(lat, reverseBn.latitude, 0.001)
        assertEquals(lon, reverseBn.longitude, 0.001)

        // Test other regional sheets: Rangpur (25.7439, 89.2752) -> 78 G
        val rangpurSheet = com.example.data.model.ButmConversion.calculateMapSheet(25.7439, 89.2752)
        assertTrue("Rangpur should be in 78 G, got $rangpurSheet", rangpurSheet.startsWith("78 G"))

        // Test Cox's Bazar (21.4272, 92.0058) -> 84 C
        val coxSheet = com.example.data.model.ButmConversion.calculateMapSheet(21.4272, 92.0058)
        assertTrue("Cox's Bazar should be in 84 C, got $coxSheet", coxSheet.startsWith("84 C"))

        // Test compatibility class ButmConvertion
        val comp = com.example.data.model.ButmConvertion()
        val gridStr = comp.latLonToButmGrid(lat, lon)
        assertTrue("Grid string format easting,northing", gridStr.contains(","))
        val latLonStr = comp.butmGridToLatLon(butm.easting, butm.northing)
        assertTrue("LatLon string format lat,lon", latLonStr.contains(","))
        val dmsStr = comp.decimalToDMS(lat, lon)
        val sheetFromDms = comp.getMapSheetNo(dmsStr)
        assertEquals("79 I/5", sheetFromDms)
    }

    @Test
    fun testElevationCalibrationAndGoogleEarthAccuracy() {
        // User Point 1: Lat 24.5013889, Lon 90.0029556 -> Google Earth Elevation: 23.30 m
        val pt1Info = com.example.calculator.ElevationService.getLocalTopographicElevation(24.5013889, 90.0029556)
        assertEquals(23.30, pt1Info.elevationMeters, 0.05)
        assertTrue("Formatted meters should show 23.3 m, got ${pt1Info.formattedMeters}", pt1Info.formattedMeters.startsWith("23.3"))

        // User Point 2: Lat 24.4832333, Lon 89.9964389 -> Google Earth Elevation: 21.82 m
        val pt2Info = com.example.calculator.ElevationService.getLocalTopographicElevation(24.4832333, 89.9964389)
        assertEquals(21.82, pt2Info.elevationMeters, 0.05)
        assertEquals("21.82 m", pt2Info.formattedMeters)

        // User Point 3: Lat 24.4009667, Lon 89.8085056 -> Google Earth Elevation: 15.24 m
        val pt3Info = com.example.calculator.ElevationService.getLocalTopographicElevation(24.4009667, 89.8085056)
        assertEquals(15.24, pt3Info.elevationMeters, 0.05)
        assertEquals("15.24 m", pt3Info.formattedMeters)

        // Benchmark: Dhaka Center (23.8103, 90.4125) -> Google Earth Bare-Earth is 15.87m
        val dhakaInfo = com.example.calculator.ElevationService.getLocalTopographicElevation(23.8103, 90.4125)
        assertEquals(15.87, dhakaInfo.elevationMeters, 0.05)

        // Benchmark: Madhupur North (24.582254, 90.141237) -> Google Earth Bare-Earth is 22.85m
        val madhupurInfo = com.example.calculator.ElevationService.getLocalTopographicElevation(24.582254, 90.141237)
        assertEquals(22.85, madhupurInfo.elevationMeters, 0.05)

        // Benchmark: Chattogram Coast (22.3569, 91.7832) -> Google Earth is 8.99m
        val ctgInfo = com.example.calculator.ElevationService.getLocalTopographicElevation(22.3569, 91.7832)
        assertEquals(8.99, ctgInfo.elevationMeters, 0.05)

        // Benchmark: GPS altitude calibration removes ~2.5m handheld antenna/geoid elevation bias
        val rawGpsAltitude = 18.5
        val calibratedGps = com.example.calculator.ElevationService.fromGpsAltitude(rawGpsAltitude)
        assertEquals(16.0, calibratedGps.elevationMeters, 0.01)

        // MilitaryGridCalculator forward calculation integrates Google Earth elevation
        val forwardResult = com.example.calculator.MilitaryGridCalculator.calculate(24.5013889, 90.0029556)
        assertEquals(23.30, forwardResult.elevationInfo.elevationMeters, 0.05)

        // Test manual elevation typing / override
        val customElevation = com.example.calculator.ElevationInfo(
            elevationMeters = 30.5,
            elevationFeet = 30.5 * 3.28084,
            formattedMeters = "30.5 m",
            formattedFeet = "100.1 ft",
            source = "Custom Entry"
        )
        val customResult = com.example.calculator.MilitaryGridCalculator.calculate(24.5013889, 90.0029556, customElevation)
        assertEquals(30.5, customResult.elevationInfo.elevationMeters, 0.01)
        assertEquals("Custom Entry", customResult.elevationInfo.source)
    }
}


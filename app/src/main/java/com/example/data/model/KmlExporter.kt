package com.example.data.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter

object KmlExporter {

    fun generateKmlContent(
        calcResult: CalculationResult?,
        p1Elevation: Double,
        p1AntennaHeight: Double,
        p2Elevation: Double,
        p2AntennaHeight: Double,
        frequencyMhz: Double,
        losResult: LosAnalysisResult
    ): String {
        val lat1 = calcResult?.point1?.latitude ?: 23.8103
        val lon1 = calcResult?.point1?.longitude ?: 90.4125
        val lat2 = calcResult?.point2?.latitude ?: 24.3636
        val lon2 = calcResult?.point2?.longitude ?: 88.6241
        val distKm = if (calcResult != null) calcResult.distanceMeters / 1000.0 else 10.0
        val bearing = calcResult?.bearingRounded ?: 0

        val totalHeight1 = p1Elevation + p1AntennaHeight
        val totalHeight2 = p2Elevation + p2AntennaHeight

        // KML Color: aabbggrr (Alpha, Blue, Green, Red)
        // Green for clear = ff00ff00; Red for blocked = ff0000ff; Amber for marginal = ff00a5ff
        val rayColor = when (losResult.statusColorType) {
            LosAnalysisResult.StatusColor.CLEAR -> "ff00d000"
            LosAnalysisResult.StatusColor.MARGINAL -> "ff00a5ff"
            LosAnalysisResult.StatusColor.OBSTRUCTED -> "ff0000ff"
        }

        val statusTitle = when (losResult.statusColorType) {
            LosAnalysisResult.StatusColor.CLEAR -> "CLEAR (দৃষ্টিরেখা সম্পূর্ণ মুক্ত)"
            LosAnalysisResult.StatusColor.MARGINAL -> "MARGINAL (ফ্রেনেল জোন স্পর্শ করেছে)"
            LosAnalysisResult.StatusColor.OBSTRUCTED -> "OBSTRUCTED (দৃষ্টিরেখা বাধাপ্রাপ্ত)"
        }

        val p1Label = calcResult?.point1?.label?.ifBlank { "Site A [Terminal 1]" } ?: "Site A [Terminal 1]"
        val p2Label = calcResult?.point2?.label?.ifBlank { "Site B [Terminal 2]" } ?: "Site B [Terminal 2]"

        return """
<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2" xmlns:gx="http://www.google.com/kml/ext/2.2">
  <Document>
    <name>LOS Terminal Path: $p1Label to $p2Label</name>
    <description><![CDATA[
      <h2>Location GR & Path: RF Line of Sight Analysis</h2>
      <p><b>Status:</b> $statusTitle</p>
      <p><b>Frequency:</b> ${frequencyMhz.toInt()} MHz</p>
      <p><b>Distance:</b> ${"%.2f".format(distKm)} KM | <b>Bearing:</b> $bearing°</p>
      <p><b>1st Fresnel Zone Radius:</b> ${"%.1f".format(losResult.fresnelZoneRadiusMeters)} m</p>
      <p><b>Clearance Margin:</b> ${"%.1f".format(losResult.clearanceMarginMeters)} m</p>
      <p><b>Earth Curvature Bulge:</b> ${"%.1f".format(losResult.earthCurvatureBulgeMeters)} m</p>
      <p><b>Power Output:</b> ${losResult.linkBudget.txPowerDbm} dBm | <b>Rx Sensitivity:</b> ${losResult.linkBudget.rxSensitivityDbm.toInt()} dBm</p>
      <p><b>FSPL:</b> ${"%.1f".format(losResult.linkBudget.freeSpacePathLossDb)} dB | <b>RSL:</b> ${"%.1f".format(losResult.linkBudget.receivedSignalLevelDbm)} dBm | <b>Fade Margin:</b> ${"%.1f".format(losResult.linkBudget.fadeMarginDb)} dB</p>
      <hr/>
      <p><b>Author:</b> 1618101 Cpl (Tech) Md Zakaria Alom, 3 Signal Battalion</p>
    ]]></description>

    <!-- Styles -->
    <Style id="siteAPin">
      <IconStyle>
        <scale>1.3</scale>
        <Icon><href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href></Icon>
        <color>ff00ff00</color>
      </IconStyle>
      <LabelStyle><scale>1.1</scale><color>ffffffff</color></LabelStyle>
    </Style>

    <Style id="siteBPin">
      <IconStyle>
        <scale>1.3</scale>
        <Icon><href>http://maps.google.com/mapfiles/kml/shapes/target.png</href></Icon>
        <color>ff0000ff</color>
      </IconStyle>
      <LabelStyle><scale>1.1</scale><color>ffffffff</color></LabelStyle>
    </Style>

    <Style id="losRayStyle">
      <LineStyle>
        <color>$rayColor</color>
        <width>5</width>
      </LineStyle>
      <PolyStyle>
        <color>3300ff00</color>
      </PolyStyle>
    </Style>

    <!-- Placemark Site A (Terminal 1) -->
    <Placemark>
      <name>$p1Label</name>
      <description><![CDATA[
        <b>$p1Label</b><br/>
        Ground Elevation: ${"%.1f".format(p1Elevation)} m<br/>
        Antenna Mast Height: ${"%.1f".format(p1AntennaHeight)} m<br/>
        Total Altitude AMSL: ${"%.1f".format(totalHeight1)} m
      ]]></description>
      <styleUrl>#siteAPin</styleUrl>
      <Point>
        <altitudeMode>absolute</altitudeMode>
        <coordinates>$lon1,$lat1,$totalHeight1</coordinates>
      </Point>
    </Placemark>

    <!-- Placemark Site B (Terminal 2) -->
    <Placemark>
      <name>$p2Label</name>
      <description><![CDATA[
        <b>$p2Label</b><br/>
        Ground Elevation: ${"%.1f".format(p2Elevation)} m<br/>
        Antenna Mast Height: ${"%.1f".format(p2AntennaHeight)} m<br/>
        Total Altitude AMSL: ${"%.1f".format(totalHeight2)} m
      ]]></description>
      <styleUrl>#siteBPin</styleUrl>
      <Point>
        <altitudeMode>absolute</altitudeMode>
        <coordinates>$lon2,$lat2,$totalHeight2</coordinates>
      </Point>
    </Placemark>

    <!-- 3D Line of Sight Beam Path -->
    <Placemark>
      <name>3D Line of Sight (LOS Ray)</name>
      <description><![CDATA[
        <b>RF Line of Sight Beam</b><br/>
        Status: $statusTitle<br/>
        Distance: ${"%.2f".format(distKm)} km<br/>
        Clearance Margin: ${"%.1f".format(losResult.clearanceMarginMeters)} m
      ]]></description>
      <styleUrl>#losRayStyle</styleUrl>
      <LineString>
        <extrude>1</extrude>
        <tessellate>1</tessellate>
        <altitudeMode>absolute</altitudeMode>
        <coordinates>
          $lon1,$lat1,$totalHeight1
          $lon2,$lat2,$totalHeight2
        </coordinates>
      </LineString>
    </Placemark>

  </Document>
</kml>
        """.trimIndent()
    }

    fun exportAndLaunchKml(
        context: Context,
        calcResult: CalculationResult?,
        p1Elevation: Double,
        p1AntennaHeight: Double,
        p2Elevation: Double,
        p2AntennaHeight: Double,
        frequencyMhz: Double,
        losResult: LosAnalysisResult
    ) {
        try {
            val kmlString = generateKmlContent(
                calcResult, p1Elevation, p1AntennaHeight, p2Elevation, p2AntennaHeight, frequencyMhz, losResult
            )

            val cacheDir = context.cacheDir
            val kmlFile = File(cacheDir, "los_terminal_path.kml")
            FileWriter(kmlFile).use { it.write(kmlString) }

            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, kmlFile)

            // Intent to view in Google Earth or ArcGIS Earth
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.google-earth.kml+xml")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Create chooser allowing user to select Google Earth, ArcGIS Earth, or Drive
            val chooser = Intent.createChooser(intent, "Open in Google Earth / ArcGIS Earth")
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback: share file via general intent
            shareKmlFile(context, calcResult, p1Elevation, p1AntennaHeight, p2Elevation, p2AntennaHeight, frequencyMhz, losResult)
        }
    }

    fun shareKmlFile(
        context: Context,
        calcResult: CalculationResult?,
        p1Elevation: Double,
        p1AntennaHeight: Double,
        p2Elevation: Double,
        p2AntennaHeight: Double,
        frequencyMhz: Double,
        losResult: LosAnalysisResult
    ) {
        try {
            val kmlString = generateKmlContent(
                calcResult, p1Elevation, p1AntennaHeight, p2Elevation, p2AntennaHeight, frequencyMhz, losResult
            )
            val cacheDir = context.cacheDir
            val kmlFile = File(cacheDir, "los_terminal_path.kml")
            FileWriter(kmlFile).use { it.write(kmlString) }

            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, kmlFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.google-earth.kml+xml"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "LOS Terminal Path KML - Location GR & Path")
                putExtra(Intent.EXTRA_TEXT, "Line of Sight 3D KML for Google Earth and ArcGIS Earth. Generated by Location GR & Path [1618101 Cpl (Tech) Md Zakaria Alom, 3 Signal Battalion]")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share KML File to Google Earth / ArcGIS"))
        } catch (e: Exception) {
            Toast.makeText(context, "KML এক্সপোর্ট ত্রুটি: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

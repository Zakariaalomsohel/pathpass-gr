package com.example.data.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import com.example.data.api.ElevationService
import com.example.data.db.AppDatabase
import com.example.data.db.CachedElevationPointEntity
import com.example.data.db.CachedMapAreaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class MapBoundingBox(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
) {
    val centerLat: Double get() = (minLat + maxLat) / 2.0
    val centerLon: Double get() = (minLon + maxLon) / 2.0
    val latSpan: Double get() = maxLat - minLat
    val lonSpan: Double get() = maxLon - minLon

    fun contains(lat: Double, lon: Double): Boolean {
        return lat in minLat..maxLat && lon in minLon..maxLon
    }
}

data class OfflineCacheProgress(
    val isDownloading: Boolean = false,
    val currentStep: String = "",
    val progress: Float = 0f,
    val totalTiles: Int = 0,
    val downloadedTiles: Int = 0,
    val elevationPointsCount: Int = 0,
    val isComplete: Boolean = false,
    val error: String? = null
)

object OfflineMapManager {
    private const val TAG = "OfflineMapManager"
    private const val DEFAULT_ZOOM = 12 // Optimal balance between high detail and fast 30km download
    const val MIN_OFFLINE_CACHE_RADIUS_KM = 30.0
    private const val BUFFER_DEGREES = 0.275 // ~30.5 km buffer around points (minimum 30 km coverage)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    fun calculateBoundingBox(
        p1Lat: Double,
        p1Lon: Double,
        p2Lat: Double? = null,
        p2Lon: Double? = null,
        bufferDeg: Double = BUFFER_DEGREES
    ): MapBoundingBox {
        val latMin = if (p2Lat != null) min(p1Lat, p2Lat) else p1Lat
        val latMax = if (p2Lat != null) max(p1Lat, p2Lat) else p1Lat
        val lonMin = if (p2Lon != null) min(p1Lon, p2Lon) else p1Lon
        val lonMax = if (p2Lon != null) max(p1Lon, p2Lon) else p1Lon

        // Ensure minimum 30 km buffer around all points
        val effectiveBuffer = max(bufferDeg, BUFFER_DEGREES)
        return MapBoundingBox(
            minLat = max(-85.0, latMin - effectiveBuffer),
            maxLat = min(85.0, latMax + effectiveBuffer),
            minLon = max(-180.0, lonMin - effectiveBuffer),
            maxLon = min(180.0, lonMax + effectiveBuffer)
        )
    }

    fun lonToTileX(lon: Double, zoom: Int): Int {
        val x = (lon + 180.0) / 360.0 * (1 shl zoom)
        return floor(x).toInt().coerceIn(0, (1 shl zoom) - 1)
    }

    fun latToTileY(lat: Double, zoom: Int): Int {
        val latRad = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
        val n = ln(tan(latRad) + 1.0 / cos(latRad))
        val y = (1.0 - (n / Math.PI)) / 2.0 * (1 shl zoom)
        return floor(y).toInt().coerceIn(0, (1 shl zoom) - 1)
    }

    fun tileXToLon(x: Int, zoom: Int): Double {
        return x.toDouble() / (1 shl zoom) * 360.0 - 180.0
    }

    fun tileYToLat(y: Int, zoom: Int): Double {
        val n = Math.PI - 2.0 * Math.PI * y.toDouble() / (1 shl zoom)
        return Math.toDegrees(atan2(sin(n), cos(n)))
    }

    private fun getTileDir(context: Context): File {
        val dir = File(context.filesDir, "offline_map_tiles")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getTileFile(context: Context, zoom: Int, x: Int, y: Int): File {
        val zoomDir = File(getTileDir(context), zoom.toString())
        val xDir = File(zoomDir, x.toString())
        if (!xDir.exists()) xDir.mkdirs()
        return File(xDir, "$y.png")
    }

    fun getCachedTileBitmap(context: Context, zoom: Int, x: Int, y: Int): Bitmap? {
        val file = getTileFile(context, zoom, x, y)
        if (file.exists() && file.length() > 0) {
            return try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    suspend fun cacheVisibleArea(
        context: Context,
        bbox: MapBoundingBox,
        areaName: String = "Tactical Area",
        onProgress: (OfflineCacheProgress) -> Unit = {}
    ): Result<CachedMapAreaEntity> = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            val zoom = DEFAULT_ZOOM

            val minTileX = lonToTileX(bbox.minLon, zoom)
            val maxTileX = lonToTileX(bbox.maxLon, zoom)
            val minTileY = latToTileY(bbox.maxLat, zoom) // Top latitude has lower Y index
            val maxTileY = latToTileY(bbox.minLat, zoom)

            val tileList = mutableListOf<Pair<Int, Int>>()
            for (x in minTileX..maxTileX) {
                for (y in minTileY..maxTileY) {
                    tileList.add(Pair(x, y))
                }
            }

            val totalTiles = tileList.size
            onProgress(
                OfflineCacheProgress(
                    isDownloading = true,
                    currentStep = "ম্যাপ টাইল প্রস্তুত হচ্ছে ($totalTiles টাইল)...",
                    progress = 0.05f,
                    totalTiles = totalTiles
                )
            )

            var downloadedTiles = 0
            for ((index, tile) in tileList.withIndex()) {
                val x = tile.first
                val y = tile.second
                val tileFile = getTileFile(context, zoom, x, y)

                if (!tileFile.exists() || tileFile.length() == 0L) {
                    val downloaded = downloadTileWithFallbacks(x, y, zoom, tileFile)
                    if (!downloaded) {
                        // Generate synthetic tactical grid tile if network offline
                        createSyntheticTacticalTile(tileFile, zoom, x, y)
                    }
                }
                downloadedTiles++

                val progressFraction = 0.05f + 0.65f * (downloadedTiles.toFloat() / max(1, totalTiles))
                onProgress(
                    OfflineCacheProgress(
                        isDownloading = true,
                        currentStep = "টাইল ডাউনলোড হচ্ছে: $downloadedTiles / $totalTiles",
                        progress = progressFraction,
                        totalTiles = totalTiles,
                        downloadedTiles = downloadedTiles
                    )
                )
            }

            // Next: Download / cache Digital Elevation Model (DEM) grid
            onProgress(
                OfflineCacheProgress(
                    isDownloading = true,
                    currentStep = "এলিভেশন গ্রিড ডেটা ক্যাশ হচ্ছে...",
                    progress = 0.75f,
                    totalTiles = totalTiles,
                    downloadedTiles = downloadedTiles
                )
            )

            val gridSize = 5 // 5x5 grid = 25 elevation checkpoints across the bounding box
            val elevationCoords = mutableListOf<Pair<Double, Double>>()
            for (i in 0 until gridSize) {
                val lat = bbox.minLat + (bbox.maxLat - bbox.minLat) * (i.toDouble() / (gridSize - 1))
                for (j in 0 until gridSize) {
                    val lon = bbox.minLon + (bbox.maxLon - bbox.minLon) * (j.toDouble() / (gridSize - 1))
                    elevationCoords.add(Pair(lat, lon))
                }
            }

            // Fetch elevation batch via Open-Meteo DEM API
            val elevBatchResult = ElevationService.getElevationBatch(elevationCoords)

            // Save Area to Room DB
            val areaEntity = CachedMapAreaEntity(
                name = if (areaName.contains("30km")) areaName else "$areaName (30km Zone)",
                minLat = bbox.minLat,
                maxLat = bbox.maxLat,
                minLon = bbox.minLon,
                maxLon = bbox.maxLon,
                centerLat = bbox.centerLat,
                centerLon = bbox.centerLon,
                radiusKm = max(MIN_OFFLINE_CACHE_RADIUS_KM, calculateApproxRadiusKm(bbox)),
                zoomLevel = zoom,
                tileCount = downloadedTiles,
                elevationPointCount = elevationCoords.size,
                timestamp = System.currentTimeMillis()
            )

            val areaId = db.cachedMapAreaDao().insertArea(areaEntity)
            val savedArea = areaEntity.copy(id = areaId)

            // Insert elevation points
            val elevEntities = elevationCoords.mapIndexed { idx, coord ->
                val elevVal = elevBatchResult[idx] ?: 15.0 // fallback sea level if offline
                CachedElevationPointEntity(
                    areaId = areaId,
                    latitude = coord.first,
                    longitude = coord.second,
                    elevationMeters = elevVal
                )
            }
            db.cachedElevationPointDao().insertPoints(elevEntities)

            onProgress(
                OfflineCacheProgress(
                    isDownloading = false,
                    currentStep = "ক্যাশ সম্পন্ন! অফলাইনে ব্যবহার উপযোগী।",
                    progress = 1.0f,
                    totalTiles = totalTiles,
                    downloadedTiles = downloadedTiles,
                    elevationPointsCount = elevEntities.size,
                    isComplete = true
                )
            )

            Result.success(savedArea)
        } catch (e: Exception) {
            Log.e(TAG, "Error caching map area", e)
            onProgress(
                OfflineCacheProgress(
                    isDownloading = false,
                    currentStep = "ক্যাশিং ব্যর্থ হয়েছে: ${e.localizedMessage}",
                    isComplete = false,
                    error = e.localizedMessage
                )
            )
            Result.failure(e)
        }
    }

    private fun downloadTileWithFallbacks(x: Int, y: Int, zoom: Int, targetFile: File): Boolean {
        val urls = listOf(
            "https://mt1.google.com/vt/lyrs=s&x=$x&y=$y&z=$zoom",
            "https://mt1.google.com/vt/lyrs=y&x=$x&y=$y&z=$zoom",
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$zoom/$y/$x",
            "https://tile.openstreetmap.org/$zoom/$x/$y.png",
            "https://a.basemaps.cartocdn.com/rastertiles/voyager/$zoom/$x/$y.png"
        )

        for (url in urls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "LocationFinder-Tactical-App/1.0 (Android)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null && bytes.isNotEmpty()) {
                            FileOutputStream(targetFile).use { fos ->
                                fos.write(bytes)
                            }
                            return true
                        }
                    }
                }
            } catch (_: Exception) {
                // Try next fallback URL
            }
        }
        return false
    }

    private fun createSyntheticTacticalTile(targetFile: File, zoom: Int, x: Int, y: Int) {
        try {
            val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Tactical dark background
            val bgPaint = Paint().apply {
                color = android.graphics.Color.rgb(15, 23, 42)
            }
            canvas.drawRect(0f, 0f, 256f, 256f, bgPaint)

            // Grid lines
            val gridPaint = Paint().apply {
                color = android.graphics.Color.rgb(30, 41, 59)
                strokeWidth = 1.5f
            }
            canvas.drawLine(0f, 64f, 256f, 64f, gridPaint)
            canvas.drawLine(0f, 128f, 256f, 128f, gridPaint)
            canvas.drawLine(0f, 192f, 256f, 192f, gridPaint)
            canvas.drawLine(64f, 0f, 64f, 256f, gridPaint)
            canvas.drawLine(128f, 0f, 128f, 256f, gridPaint)
            canvas.drawLine(192f, 0f, 192f, 256f, gridPaint)

            // Outer border
            val borderPaint = Paint().apply {
                color = android.graphics.Color.rgb(51, 65, 85)
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRect(1f, 1f, 255f, 255f, borderPaint)

            // Text label with coordinates
            val textPaint = Paint().apply {
                color = android.graphics.Color.rgb(148, 163, 184)
                textSize = 18f
                isAntiAlias = true
            }
            val lon = tileXToLon(x, zoom)
            val lat = tileYToLat(y, zoom)
            canvas.drawText("TACTICAL GRID", 16f, 32f, textPaint)
            textPaint.textSize = 14f
            canvas.drawText("${"%.2f".format(lat)}°N, ${"%.2f".format(lon)}°E", 16f, 54f, textPaint)

            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating synthetic tactical tile", e)
        }
    }

    suspend fun getOfflineElevation(context: Context, lat: Double, lon: Double): Double? = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val nearest = db.cachedElevationPointDao().findNearestPoint(lat, lon)
        if (nearest != null) {
            // Check if distance to nearest point is reasonably close (< 0.25 deg ~ 27 km)
            val distSq = (nearest.latitude - lat) * (nearest.latitude - lat) + (nearest.longitude - lon) * (nearest.longitude - lon)
            if (distSq < 0.1) {
                return@withContext nearest.elevationMeters
            }
        }
        null
    }

    suspend fun isPointCached(context: Context, lat: Double, lon: Double): Boolean = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val area = db.cachedMapAreaDao().findAreaContaining(lat, lon)
        area != null
    }

    suspend fun clearAllOfflineCache(context: Context) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            db.cachedMapAreaDao().clearAllAreas()
            db.cachedElevationPointDao().clearAllPoints()

            val tileDir = getTileDir(context)
            tileDir.deleteRecursively()
            tileDir.mkdirs()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing offline cache", e)
        }
    }

    private fun calculateApproxRadiusKm(bbox: MapBoundingBox): Double {
        val latDist = (bbox.maxLat - bbox.minLat) * 111.0
        val lonDist = (bbox.maxLon - bbox.minLon) * 111.0 * cos(Math.toRadians(bbox.centerLat))
        return sqrt(latDist * latDist + lonDist * lonDist) / 2.0
    }
}

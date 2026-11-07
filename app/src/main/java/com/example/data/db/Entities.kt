package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_routes")
data class SavedRouteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val p1Lat: Double,
    val p1Lon: Double,
    val p1Label: String = "",
    val p1Elevation: Double? = null,
    val p2Lat: Double,
    val p2Lon: Double,
    val p2Label: String = "",
    val p2Elevation: Double? = null,
    val distanceKm: Double,
    val bearingDegrees: Double,
    val backBearingDegrees: Double,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_waypoints")
data class SavedWaypointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double? = null,
    val category: String = "General", // Tactical, Base, Landmark, Observer
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_map_areas")
data class CachedMapAreaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val centerLat: Double,
    val centerLon: Double,
    val radiusKm: Double,
    val zoomLevel: Int = 13,
    val tileCount: Int = 0,
    val elevationPointCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_elevation_points")
data class CachedElevationPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val areaId: Long,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double
)

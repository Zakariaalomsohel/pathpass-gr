package com.example.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Query("SELECT * FROM saved_routes ORDER BY timestamp DESC")
    fun getAllRoutes(): Flow<List<SavedRouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: SavedRouteEntity): Long

    @Delete
    suspend fun deleteRoute(route: SavedRouteEntity)

    @Query("DELETE FROM saved_routes")
    suspend fun clearAllRoutes()
}

@Dao
interface WaypointDao {
    @Query("SELECT * FROM saved_waypoints ORDER BY timestamp DESC")
    fun getAllWaypoints(): Flow<List<SavedWaypointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoint(waypoint: SavedWaypointEntity): Long

    @Delete
    suspend fun deleteWaypoint(waypoint: SavedWaypointEntity)

    @Query("DELETE FROM saved_waypoints")
    suspend fun clearAllWaypoints()
}

@Dao
interface CachedMapAreaDao {
    @Query("SELECT * FROM cached_map_areas ORDER BY timestamp DESC")
    fun getAllAreas(): Flow<List<CachedMapAreaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArea(area: CachedMapAreaEntity): Long

    @Delete
    suspend fun deleteArea(area: CachedMapAreaEntity)

    @Query("DELETE FROM cached_map_areas WHERE id = :id")
    suspend fun deleteAreaById(id: Long)

    @Query("SELECT * FROM cached_map_areas WHERE minLat <= :lat AND maxLat >= :lat AND minLon <= :lon AND maxLon >= :lon LIMIT 1")
    suspend fun findAreaContaining(lat: Double, lon: Double): CachedMapAreaEntity?

    @Query("DELETE FROM cached_map_areas")
    suspend fun clearAllAreas()
}

@Dao
interface CachedElevationPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<CachedElevationPointEntity>)

    @Query("SELECT * FROM cached_elevation_points WHERE areaId = :areaId")
    suspend fun getPointsForArea(areaId: Long): List<CachedElevationPointEntity>

    @Query("SELECT * FROM cached_elevation_points ORDER BY ((latitude - :lat) * (latitude - :lat) + (longitude - :lon) * (longitude - :lon)) ASC LIMIT 1")
    suspend fun findNearestPoint(lat: Double, lon: Double): CachedElevationPointEntity?

    @Query("DELETE FROM cached_elevation_points WHERE areaId = :areaId")
    suspend fun deletePointsForArea(areaId: Long)

    @Query("DELETE FROM cached_elevation_points")
    suspend fun clearAllPoints()
}

@Database(
    entities = [
        SavedRouteEntity::class,
        SavedWaypointEntity::class,
        CachedMapAreaEntity::class,
        CachedElevationPointEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun waypointDao(): WaypointDao
    abstract fun cachedMapAreaDao(): CachedMapAreaDao
    abstract fun cachedElevationPointDao(): CachedElevationPointDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "location_finder.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

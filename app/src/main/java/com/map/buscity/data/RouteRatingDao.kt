package com.map.buscity.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteRatingDao {
    @Query("SELECT * FROM route_ratings WHERE route_number = :routeNumber ORDER BY timestamp DESC")
    fun getRatingsByRouteNumber(routeNumber: String): Flow<List<RouteRating>>

    @Query("SELECT * FROM route_ratings WHERE id = :id")
    suspend fun getRatingById(id: String): RouteRating?

    @Query("SELECT * FROM route_ratings WHERE user_id = :userId AND route_number = :routeNumber LIMIT 1")
    suspend fun getUserRatingForRoute(userId: String, routeNumber: String): RouteRating?

    @Insert
    suspend fun insertRating(rating: RouteRating)

    @Delete
    suspend fun deleteRating(rating: RouteRating)

    @Query("DELETE FROM route_ratings WHERE id = :id")
    suspend fun deleteRatingById(id: String)

    @Query("SELECT AVG(rating) FROM route_ratings WHERE route_number = :routeNumber")
    suspend fun getAverageRating(routeNumber: String): Float?

    @Query("SELECT COUNT(*) FROM route_ratings WHERE route_number = :routeNumber")
    suspend fun getRatingCount(routeNumber: String): Int
}

package com.map.buscity.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RouteCacheDao {
    @Query("SELECT * FROM route_cache WHERE route_number = :routeNumber LIMIT 1")
    suspend fun getByRouteNumber(routeNumber: String): RouteCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: RouteCache)

    @Query("DELETE FROM route_cache WHERE route_number = :routeNumber")
    suspend fun delete(routeNumber: String)
}

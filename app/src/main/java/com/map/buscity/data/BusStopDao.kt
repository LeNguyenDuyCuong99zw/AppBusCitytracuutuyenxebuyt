package com.map.buscity.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BusStopDao {
    @Query("SELECT * FROM bus_stops WHERE route_number = :routeNumber ORDER BY stop_order")
    fun getStopsForRoute(routeNumber: String): Flow<List<BusStop>>

    @Insert
    suspend fun insertStop(stop: BusStop)

    @Insert
    suspend fun insertStops(stops: List<BusStop>)

    @Query("SELECT COUNT(*) FROM bus_stops WHERE route_number = :routeNumber")
    suspend fun countForRoute(routeNumber: String): Int
}

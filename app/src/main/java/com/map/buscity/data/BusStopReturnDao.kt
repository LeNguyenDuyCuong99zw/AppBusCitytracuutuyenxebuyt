package com.map.buscity.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BusStopReturnDao {
    @Query("SELECT * FROM bus_stops_return WHERE route_number = :routeNumber ORDER BY stop_order")
    fun getReturnStopsForRoute(routeNumber: String): Flow<List<BusStopReturn>>

    @Insert
    suspend fun insertStop(stop: BusStopReturn)

    @Insert
    suspend fun insertStops(stops: List<BusStopReturn>)

    @Query("SELECT COUNT(*) FROM bus_stops_return WHERE route_number = :routeNumber")
    suspend fun countForRoute(routeNumber: String): Int
}

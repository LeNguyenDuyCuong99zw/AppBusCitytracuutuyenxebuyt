package com.map.buscity.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BusRouteDao {
    @Query("SELECT * FROM bus_routes ORDER BY CAST(route_number AS INTEGER)")
    fun getAllRoutes(): Flow<List<BusRoute>>

    @Insert
    suspend fun insertRoute(route: BusRoute)

    @Delete
    suspend fun deleteRoute(route: BusRoute)

    @Query("SELECT * FROM bus_routes WHERE id = :id")
    fun getRouteById(id: Int): kotlinx.coroutines.flow.Flow<BusRoute?>

    @Query("SELECT * FROM bus_routes WHERE route_number = :number LIMIT 1")
    suspend fun getRouteByNumber(number: String): BusRoute?
}

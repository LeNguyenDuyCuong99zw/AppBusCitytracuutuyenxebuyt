package com.map.buscity.repository

import com.map.buscity.data.BusRoute
import com.map.buscity.data.BusRouteDao
import kotlinx.coroutines.flow.Flow

class BusRepository(private val dao: BusRouteDao) {
    fun getAllRoutes(): Flow<List<BusRoute>> = dao.getAllRoutes()

    fun getRouteById(id: Int): Flow<BusRoute?> = dao.getRouteById(id)

    suspend fun insertIfNotExists(route: BusRoute) {
        val existing = dao.getRouteByNumber(route.routeNumber)
        if (existing == null) {
            dao.insertRoute(route)
        }
    }

    suspend fun insert(route: BusRoute) = dao.insertRoute(route)

    suspend fun delete(route: BusRoute) = dao.deleteRoute(route)
}

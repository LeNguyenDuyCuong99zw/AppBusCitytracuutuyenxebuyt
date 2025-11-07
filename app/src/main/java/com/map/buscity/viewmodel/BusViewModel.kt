package com.map.buscity.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.map.buscity.data.BusDatabase
import com.map.buscity.data.BusRoute
import com.map.buscity.data.BusStop
import com.map.buscity.repository.BusRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BusViewModel(application: Application) : AndroidViewModel(application) {
    private val db = BusDatabase.getDatabase(application)
    private val repo = BusRepository(db.busRouteDao())

    private val stopDao = db.busStopDao()

    val routes = repo.getAllRoutes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun insertSampleData() {
        viewModelScope.launch {
            val sampleRoutes = listOf(
                BusRoute(
                    routeNumber = "01",
                    routeName = "Bến Thành - Bến xe buýt Chợ Lớn",
                    startTime = "05:00",
                    endTime = "20:15",
                    price = 5000,
                    rating = 4.8f
                ),
                BusRoute(
                    routeNumber = "03",
                    routeName = "Bến Thành - Thạnh Xuân",
                    startTime = "04:00",
                    endTime = "21:00",
                    price = 6000,
                    rating = 2.2f
                ),
                BusRoute(
                    routeNumber = "04",
                    routeName = "Bến Thành - Cộng Hòa - Bến xe An Sương",
                    startTime = "05:00",
                    endTime = "20:15",
                    price = 6000,
                    rating = 3.4f
                ),
                BusRoute(
                    routeNumber = "05",
                    routeName = "Bến xe buýt Chợ Lớn - Bến xe Biên Hòa",
                    startTime = "04:50",
                    endTime = "17:50",
                    price = 10000,
                    rating = 2.8f
                ),
                BusRoute(
                    routeNumber = "06",
                    routeName = "Bến xe buýt Chợ Lớn - Đại học Nông Lâm",
                    startTime = "04:55",
                    endTime = "21:00",
                    price = 7000,
                    rating = 3.0f
                ),
                BusRoute(
                    routeNumber = "07",
                    routeName = "Bến xe buýt Chợ Lớn - Gò Vấp",
                    startTime = "04:00",
                    endTime = "20:00",
                    price = 6000,
                    rating = 4.2f
                ),
                BusRoute(
                    routeNumber = "08",
                    routeName = "Bến xe buýt Quận 8 - Đại học Quốc gia",
                    startTime = "04:40",
                    endTime = "20:30",
                    price = 7000,
                    rating = 4.2f
                ),
                BusRoute(
                    routeNumber = "48",
                    routeName = "Bến xe buýt Tân Phú - Chợ Hiệp Thành",
                    startTime = "04:30",
                    endTime = "20:00",
                    price = 6000,
                    rating = 4.3f
                ),
                BusRoute(
                    routeNumber = "145",
                    routeName = "Bến xe buýt Chợ Lớn - Chợ Hiệp Thành",
                    startTime = "04:30",
                    endTime = "20:30",
                    price = 6000,
                    rating = 3.2f
                ),
                BusRoute(
                    routeNumber = "150",
                    routeName = "Bến xe buýt Chợ Lớn - Bến xe Miền Đông mới",
                    startTime = "04:00",
                    endTime = "22:00",
                    price = 7000,
                    rating = 2.7f
                )
            )
            
            // Insert only when route with same routeNumber doesn't exist to avoid duplicates
            sampleRoutes.forEach { route ->
                repo.insertIfNotExists(route)
            }

            // Insert sample stops for all routes if none exist
            val totalStops = stopDao.countForRoute("01")
            if (totalStops == 0) {
                val sampleStops = com.map.buscity.data.sample.SampleBusStopData.getSampleStops()
                stopDao.insertStops(sampleStops)
            }
        }
    }

    fun delete(route: BusRoute) {
        viewModelScope.launch {
            repo.delete(route)
        }
    }

    fun getRouteById(id: Int) = repo.getRouteById(id)

    fun getStopsForRoute(routeNumber: String) = stopDao.getStopsForRoute(routeNumber)
}

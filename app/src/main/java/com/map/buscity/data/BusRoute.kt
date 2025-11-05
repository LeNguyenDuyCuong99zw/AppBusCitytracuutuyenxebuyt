package com.map.buscity.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bus_routes")
data class BusRoute(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "route_number") val routeNumber: String,
    @ColumnInfo(name = "route_name") val routeName: String,
    @ColumnInfo(name = "start_time") val startTime: String,
    @ColumnInfo(name = "end_time") val endTime: String,
    @ColumnInfo(name = "price") val price: Int,
    @ColumnInfo(name = "rating") val rating: Float
)

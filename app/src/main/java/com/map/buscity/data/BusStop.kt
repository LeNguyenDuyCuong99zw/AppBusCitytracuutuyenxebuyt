package com.map.buscity.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bus_stops")
data class BusStop(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "route_number") val routeNumber: String,
    @ColumnInfo(name = "stop_name") val stopName: String,
    @ColumnInfo(name = "lat") val lat: Double,
    @ColumnInfo(name = "lng") val lng: Double,
    @ColumnInfo(name = "stop_order") val stopOrder: Int = 0
)

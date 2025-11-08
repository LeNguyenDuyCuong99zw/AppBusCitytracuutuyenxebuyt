package com.map.buscity.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "route_cache")
data class RouteCache(
    @PrimaryKey @ColumnInfo(name = "route_number") val routeNumber: String,
    @ColumnInfo(name = "geojson") val geoJson: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

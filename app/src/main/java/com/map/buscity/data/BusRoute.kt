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
    @ColumnInfo(name = "rating") val rating: Float,
    @ColumnInfo(name = "student_price") val studentPrice: Int = 0,
    @ColumnInfo(name = "monthly_pass_30_price") val monthlyPass30Price: Int = 0,
    @ColumnInfo(name = "student_monthly_pass") val studentMonthlyPass: Int = 0,
    @ColumnInfo(name = "route_type") val routeType: String = "Phổ thông",
    @ColumnInfo(name = "run_time") val runTime: String = "0",
    @ColumnInfo(name = "spacing") val spacing: String = "0",
    @ColumnInfo(name = "stops") val stops: Int = 0
)

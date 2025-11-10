package com.map.buscity.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BusRoute::class, BusStop::class, RouteCache::class, BusStopReturn::class], version = 4, exportSchema = false)
abstract class BusDatabase : RoomDatabase() {
    abstract fun busRouteDao(): BusRouteDao
    abstract fun busStopDao(): BusStopDao
    abstract fun busStopReturnDao(): BusStopReturnDao
    abstract fun routeCacheDao(): RouteCacheDao

    companion object {
        @Volatile
        private var INSTANCE: BusDatabase? = null

        fun getDatabase(context: Context): BusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BusDatabase::class.java,
                    "bus_database"
                )
                    // For development: do destructive migration to avoid migration scripts
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

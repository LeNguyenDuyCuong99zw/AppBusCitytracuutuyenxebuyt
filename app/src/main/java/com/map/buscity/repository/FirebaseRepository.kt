package com.map.buscity.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.map.buscity.data.BusRoute
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Firebase Repository - Fetches bus routes from Firebase Realtime Database
 * 
 * This class handles:
 * 1. Real-time listening to /routes path in Firebase
 * 2. Converting Firebase data to BusRoute objects
 * 3. Providing a Flow so UI automatically updates when Firebase data changes
 */
class FirebaseRepository {
    private val database = FirebaseDatabase.getInstance()
    private val routesRef = database.getReference("routes")

    companion object {
        private const val TAG = "FirebaseRepository"
    }

    /**
     * Get all routes from Firebase as a Flow (updates in real-time)
     * 
     * @return Flow<List<BusRoute>> that emits updated list whenever Firebase data changes
     */
    fun getAllRoutesFlow(): Flow<List<BusRoute>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val routes = mutableListOf<BusRoute>()
                    
                    snapshot.children.forEach { routeSnapshot ->
                        val routeData = routeSnapshot.value as? Map<*, *>
                        
                        if (routeData != null) {
                            try {
                                val route = parseFirebaseRoute(routeData)
                                routes.add(route)
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to parse route: ${routeSnapshot.key}", e)
                            }
                        }
                    }
                    
                    Log.d(TAG, "Fetched ${routes.size} routes from Firebase")
                    trySend(routes)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching routes", e)
                    trySend(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase error: ${error.message}")
                // Send empty list on error so Flow continues
                trySend(emptyList())
            }
        }

        // Attach listener
        routesRef.addValueEventListener(listener)

        // Cleanup when Flow is closed
        awaitClose {
            routesRef.removeEventListener(listener)
        }
    }

    /**
     * Parse Firebase data map into BusRoute object
     * 
     * Firebase data format:
     * {
     *   "routeNumber": "01",
     *   "routeName": "Bến Thành - Chợ Lớn",
     *   "startTime": "05:00",
     *   "endTime": "20:15",
     *   "price": 5000,
     *   "rating": 4.8,
     *   ... other fields
     * }
     */
    private fun parseFirebaseRoute(data: Map<*, *>): BusRoute {
        return BusRoute(
            routeNumber = data["routeNumber"]?.toString() ?: "Unknown",
            routeName = data["routeName"]?.toString() ?: "Unknown",
            startTime = data["startTime"]?.toString() ?: "00:00",
            endTime = data["endTime"]?.toString() ?: "23:59",
            price = (data["price"] as? Number)?.toInt() ?: 0,
            rating = (data["rating"] as? Number)?.toFloat() ?: 0f,
            studentPrice = (data["studentPrice"] as? Number)?.toInt() ?: 0,
            monthlyPass30Price = (data["monthlyPass30Price"] as? Number)?.toInt() ?: 0,
            studentMonthlyPass = (data["studentMonthlyPass"] as? Number)?.toInt() ?: 0,
            routeType = data["routeType"]?.toString() ?: "Phổ thông",
            runTime = data["runTime"]?.toString() ?: "0",
            spacing = data["spacing"]?.toString() ?: "0",
            stops = (data["stops"] as? Number)?.toInt() ?: 0
        )
    }
}

package com.map.buscity.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.map.buscity.data.BusRoute
import com.map.buscity.data.RouteRating
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Repository - Fetches bus routes from Firebase Realtime Database
 * 
 * This class handles:
 * 1. Real-time listening to /routes path in Firebase
 * 2. Converting Firebase data to BusRoute objects
 * 3. Providing a Flow so UI automatically updates when Firebase data changes
 * 4. Managing route ratings (save, get, delete, update average)
 */
class FirebaseRepository {
    private val database = FirebaseDatabase.getInstance()
    private val routesRef = database.getReference("routes")
    private val ratingsRef = database.getReference("route_ratings")

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
            stops = (data["stops"] as? Number)?.toInt() ?: 0,
            ratingCount = (data["ratingCount"] as? Number)?.toInt() ?: 0
        )
    }

    // ====================================
    // ROUTE RATINGS MANAGEMENT
    // ====================================

    /**
     * Get all ratings for a specific route as a Flow
     */
    fun getRatingsForRouteFlow(routeNumber: String): Flow<List<RouteRating>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val ratings = mutableListOf<RouteRating>()
                    snapshot.children.forEach { ratingSnapshot ->
                        val ratingData = ratingSnapshot.value as? Map<*, *>
                        if (ratingData != null) {
                            try {
                                val rating = parseFirebaseRating(ratingData, ratingSnapshot.key ?: "")
                                if (rating.routeNumber == routeNumber) {
                                    ratings.add(rating)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to parse rating", e)
                            }
                        }
                    }
                    // Sort by timestamp descending (newest first)
                    ratings.sortByDescending { it.timestamp }
                    trySend(ratings)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching ratings for route $routeNumber", e)
                    trySend(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase error: ${error.message}")
                trySend(emptyList())
            }
        }

        ratingsRef.addValueEventListener(listener)
        awaitClose {
            ratingsRef.removeEventListener(listener)
        }
    }

    /**
     * Save a new rating to Firebase
     */
    suspend fun saveRouteRating(rating: RouteRating): Boolean {
        return try {
            val ratingId = rating.id.ifBlank { ratingsRef.push().key ?: return false }
            val ratingWithId = rating.copy(id = ratingId)
            
            ratingsRef.child(ratingId).setValue(ratingWithId).await()
            Log.d(TAG, "Rating saved successfully: $ratingId")
            
            // Update route average rating
            updateRouteAverageRating(rating.routeNumber)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save rating: ${e.message}")
            false
        }
    }

    /**
     * Delete a rating from Firebase
     */
    suspend fun deleteRouteRating(ratingId: String, routeNumber: String): Boolean {
        return try {
            ratingsRef.child(ratingId).removeValue().await()
            Log.d(TAG, "Rating deleted successfully: $ratingId")
            
            // Update route average rating
            updateRouteAverageRating(routeNumber)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete rating: ${e.message}")
            false
        }
    }

    /**
     * Update the average rating for a route in the routes table
     */
    private fun updateRouteAverageRating(routeNumber: String) {
        ratingsRef.orderByChild("routeNumber").equalTo(routeNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalRating = 0.0
                    var count = 0
                    
                    snapshot.children.forEach { ratingSnapshot ->
                        val ratingData = ratingSnapshot.value as? Map<*, *>
                        if (ratingData != null) {
                            val rating = (ratingData["rating"] as? Number)?.toDouble() ?: 0.0
                            totalRating += rating
                            count++
                        }
                    }

                    val averageRating = if (count > 0) totalRating / count else 0.0

                    // Update the route's average rating
                    routesRef.child(routeNumber).child("rating").setValue(averageRating)
                        .addOnSuccessListener {
                            Log.d(TAG, "Updated average rating for route $routeNumber: $averageRating")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to update route rating: ${e.message}")
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Firebase error: ${error.message}")
                }
            })
    }

    /**
     * Parse Firebase rating data into RouteRating object
     */
    private fun parseFirebaseRating(data: Map<*, *>, id: String): RouteRating {
        return RouteRating(
            id = id,
            routeNumber = data["routeNumber"]?.toString() ?: "",
            userId = data["userId"]?.toString() ?: "",
            userName = data["userName"]?.toString() ?: "Ẩn danh",
            userPhotoUrl = data["userPhotoUrl"]?.toString() ?: "",
            rating = (data["rating"] as? Number)?.toInt() ?: 0,
            feedback = data["feedback"]?.toString() ?: "",
            timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            isVerified = (data["isVerified"] as? Boolean) ?: false
        )
    }
}

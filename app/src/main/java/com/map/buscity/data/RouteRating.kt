package com.map.buscity.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * RouteRating
 * 
 * Lớp này lưu thông tin đánh giá của người dùng cho một tuyến xe.
 * Được lưu trong Firestore dưới collection "route_ratings"
 * 
 * Fields:
 * - id: unique ID của rating (Firebase document ID hoặc UUID)
 * - routeNumber: số tuyến xe (VD: "01", "02")
 * - userId: ID của người dùng (Firebase Auth UID)
 * - userName: tên người dùng (từ Profile hoặc Firebase displayName)
 * - userPhotoUrl: URL ảnh avatar của người dùng
 * - rating: điểm đánh giá từ 1-5 sao
 * - feedback: nội dung nhận xét (tuỳ chọn)
 * - timestamp: thời gian đánh giá (milliseconds)
 * - isVerified: có xác minh (admin đã duyệt) hay chưa
 */
@Entity(tableName = "route_ratings")
data class RouteRating(
    @PrimaryKey val id: String = "",  // Firebase document ID hoặc UUID
    @ColumnInfo(name = "route_number") val routeNumber: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "user_name") val userName: String,
    @ColumnInfo(name = "user_photo_url") val userPhotoUrl: String = "",
    @ColumnInfo(name = "rating") val rating: Int,  // 1-5
    @ColumnInfo(name = "feedback") val feedback: String = "",
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_verified") val isVerified: Boolean = false  // Admin duyệt hay chưa
) : Serializable {
    // Dùng cho Firestore serialization
    constructor() : this(
        id = "",
        routeNumber = "",
        userId = "",
        userName = "",
        userPhotoUrl = "",
        rating = 0,
        feedback = "",
        timestamp = System.currentTimeMillis(),
        isVerified = false
    )
}

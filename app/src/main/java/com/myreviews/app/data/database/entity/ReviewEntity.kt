package com.myreviews.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val restaurantId: Long,
    val restaurantName: String,
    val restaurantLat: Double,
    val restaurantLon: Double,
    val restaurantAddress: String,
    val rating: Float, // 1.0 bis 5.0
    val comment: String,
    val visitDate: Date,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val userId: String = "",     // User UUID
    val userName: String = "Anonym" // Username zum Zeitpunkt der Review
)
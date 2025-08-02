package com.myreviews.app.domain.model

import java.util.Date

data class Review(
    val id: String = java.util.UUID.randomUUID().toString(),
    val restaurantId: Long,
    val restaurantName: String,
    val restaurantLat: Double,
    val restaurantLon: Double,
    val restaurantAddress: String,
    val rating: Float,
    val comment: String,
    val visitDate: Date,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val userId: String = "",     // User UUID
    val userName: String = "Anonym", // Username zum Zeitpunkt der Review
    val reactionCounts: Map<String, Int> = emptyMap(), // Emoji -> Count
    val syncedAt: Date? = null,   // Zeitpunkt der letzten Synchronisation
    val isDeleted: Boolean = false // Markiert gelöschte Reviews für Sync
) {
    companion object {
        fun isValidRating(rating: Float): Boolean {
            return rating in 1f..5f
        }
    }
}
package com.myreviews.app.domain.model

import java.util.Date

data class Review(
    val id: Long,
    val restaurantId: Long,
    val rating: Float,
    val comment: String,
    val createdAt: Date,
    val updatedAt: Date? = null
) {
    companion object {
        fun isValidRating(rating: Float): Boolean {
            return rating in 0f..5f
        }
    }
}
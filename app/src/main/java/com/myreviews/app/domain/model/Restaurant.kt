package com.myreviews.app.domain.model

data class Restaurant(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val averageRating: Float? = null,
    val reviewCount: Int = 0
) {
    fun calculateAverageRating(ratings: List<Float>): Float {
        return if (ratings.isNotEmpty()) {
            ratings.sum() / ratings.size
        } else {
            0f
        }
    }
}
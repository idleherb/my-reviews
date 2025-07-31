package com.myreviews.app.data.repository

import com.myreviews.app.data.database.dao.ReviewDao
import com.myreviews.app.data.database.entity.ReviewEntity
import com.myreviews.app.domain.model.Review
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

class ReviewRepository(
    private val reviewDao: ReviewDao
) {
    fun getAllReviews(): Flow<List<Review>> {
        return reviewDao.getAllReviews().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    fun getReviewsForRestaurant(restaurantId: Long): Flow<List<Review>> {
        return reviewDao.getReviewsForRestaurant(restaurantId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    suspend fun getReviewById(reviewId: Long): Review? {
        return reviewDao.getReviewById(reviewId)?.toDomainModel()
    }
    
    suspend fun saveReview(
        restaurantId: Long,
        restaurantName: String,
        restaurantLat: Double,
        restaurantLon: Double,
        restaurantAddress: String,
        rating: Float,
        comment: String,
        visitDate: Date
    ): Long {
        val entity = ReviewEntity(
            restaurantId = restaurantId,
            restaurantName = restaurantName,
            restaurantLat = restaurantLat,
            restaurantLon = restaurantLon,
            restaurantAddress = restaurantAddress,
            rating = rating,
            comment = comment,
            visitDate = visitDate
        )
        return reviewDao.insertReview(entity)
    }
    
    suspend fun updateReview(review: Review) {
        reviewDao.updateReview(review.toEntity())
    }
    
    suspend fun deleteReview(review: Review) {
        reviewDao.deleteReview(review.toEntity())
    }
    
    suspend fun getRestaurantStats(restaurantId: Long): RestaurantStats {
        val avgRating = reviewDao.getAverageRatingForRestaurant(restaurantId) ?: 0f
        val reviewCount = reviewDao.getReviewCountForRestaurant(restaurantId)
        return RestaurantStats(avgRating, reviewCount)
    }
}

data class RestaurantStats(
    val averageRating: Float,
    val reviewCount: Int
)

// Extension functions für Konvertierung
private fun ReviewEntity.toDomainModel() = Review(
    id = id,
    restaurantId = restaurantId,
    restaurantName = restaurantName,
    restaurantLat = restaurantLat,
    restaurantLon = restaurantLon,
    restaurantAddress = restaurantAddress,
    rating = rating,
    comment = comment,
    visitDate = visitDate,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun Review.toEntity() = ReviewEntity(
    id = id,
    restaurantId = restaurantId,
    restaurantName = restaurantName,
    restaurantLat = restaurantLat,
    restaurantLon = restaurantLon,
    restaurantAddress = restaurantAddress,
    rating = rating,
    comment = comment,
    visitDate = visitDate,
    createdAt = createdAt,
    updatedAt = updatedAt
)
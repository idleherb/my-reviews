package com.myreviews.app.data.repository

import com.myreviews.app.data.database.dao.ReviewDao
import com.myreviews.app.data.database.entity.ReviewEntity
import com.myreviews.app.domain.model.Review
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

class ReviewRepository(
    private val reviewDao: ReviewDao,
    private val userRepository: UserRepository
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
    
    suspend fun getReviewForRestaurant(restaurantId: Long): Review? {
        // Hole die neueste Bewertung für dieses Restaurant
        return reviewDao.getLatestReviewForRestaurant(restaurantId)?.toDomainModel()
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
        // Hole aktuellen User
        val currentUser = userRepository.ensureDefaultUser()
        
        val entity = ReviewEntity(
            restaurantId = restaurantId,
            restaurantName = restaurantName,
            restaurantLat = restaurantLat,
            restaurantLon = restaurantLon,
            restaurantAddress = restaurantAddress,
            rating = rating,
            comment = comment,
            visitDate = visitDate,
            userId = currentUser.userId,
            userName = currentUser.userName
        )
        return reviewDao.insertReview(entity)
    }
    
    suspend fun updateReview(review: Review) {
        // Beim Update syncedAt auf null setzen, damit es beim nächsten Sync hochgeladen wird
        val entity = review.toEntity().copy(
            syncedAt = null,
            updatedAt = Date()
        )
        reviewDao.updateReview(entity)
    }
    
    suspend fun deleteReview(review: Review) {
        // Markiere als gelöscht und setze updatedAt auf jetzt
        reviewDao.markAsDeleted(review.id, Date())
    }
    
    suspend fun updateUserNameInReviews(userId: String, newUserName: String) {
        // Aktualisiere alle Reviews eines Users mit dem neuen Namen
        reviewDao.updateUserNameInReviews(userId, newUserName)
    }
    
    suspend fun getRestaurantStats(restaurantId: Long): RestaurantStats {
        val avgRating = reviewDao.getAverageRatingForRestaurant(restaurantId) ?: 0f
        val reviewCount = reviewDao.getReviewCountForRestaurant(restaurantId)
        return RestaurantStats(avgRating, reviewCount)
    }
    
    suspend fun getUnsyncedReviews(): List<Review> {
        return reviewDao.getUnsyncedReviews().map { it.toDomainModel() }
    }
    
    suspend fun markAsSynced(reviewId: Long) {
        reviewDao.updateSyncedAt(reviewId, Date())
    }
    
    suspend fun cleanupDeletedReviews() {
        reviewDao.deleteAllSyncedDeletedReviews()
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
    updatedAt = updatedAt,
    userId = userId,
    userName = userName,
    syncedAt = syncedAt,
    isDeleted = isDeleted
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
    updatedAt = updatedAt,
    userId = userId,
    userName = userName,
    syncedAt = syncedAt,
    isDeleted = isDeleted
)
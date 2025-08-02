package com.myreviews.app.data.database.dao

import androidx.room.*
import com.myreviews.app.data.database.entity.ReviewEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE isDeleted = 0 ORDER BY visitDate DESC")
    fun getAllReviews(): Flow<List<ReviewEntity>>
    
    @Query("SELECT * FROM reviews WHERE restaurantId = :restaurantId AND isDeleted = 0 ORDER BY visitDate DESC")
    fun getReviewsForRestaurant(restaurantId: Long): Flow<List<ReviewEntity>>
    
    @Query("SELECT * FROM reviews WHERE id = :reviewId")
    suspend fun getReviewById(reviewId: Long): ReviewEntity?
    
    @Query("SELECT * FROM reviews WHERE restaurantId = :restaurantId ORDER BY visitDate DESC LIMIT 1")
    suspend fun getLatestReviewForRestaurant(restaurantId: Long): ReviewEntity?
    
    @Insert
    suspend fun insertReview(review: ReviewEntity): Long
    
    @Update
    suspend fun updateReview(review: ReviewEntity)
    
    @Delete
    suspend fun deleteReview(review: ReviewEntity)
    
    @Query("SELECT AVG(rating) FROM reviews WHERE restaurantId = :restaurantId")
    suspend fun getAverageRatingForRestaurant(restaurantId: Long): Float?
    
    @Query("SELECT COUNT(*) FROM reviews WHERE restaurantId = :restaurantId")
    suspend fun getReviewCountForRestaurant(restaurantId: Long): Int
    
    @Query("UPDATE reviews SET userName = :newUserName WHERE userId = :userId")
    suspend fun updateUserNameInReviews(userId: String, newUserName: String)
    
    @Query("SELECT * FROM reviews WHERE (syncedAt IS NULL OR updatedAt > syncedAt) OR isDeleted = 1")
    suspend fun getUnsyncedReviews(): List<ReviewEntity>
    
    @Query("UPDATE reviews SET syncedAt = :syncedAt WHERE id = :reviewId")
    suspend fun updateSyncedAt(reviewId: Long, syncedAt: Date)
    
    @Query("UPDATE reviews SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :reviewId")
    suspend fun markAsDeleted(reviewId: Long, updatedAt: Date)
    
    @Query("DELETE FROM reviews WHERE isDeleted = 1 AND syncedAt IS NOT NULL")
    suspend fun deleteAllSyncedDeletedReviews()
    
    @Query("DELETE FROM reviews WHERE isDeleted = 0")
    suspend fun deleteAllNonTombstoneReviews()
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateReview(review: ReviewEntity)
}
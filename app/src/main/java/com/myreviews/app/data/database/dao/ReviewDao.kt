package com.myreviews.app.data.database.dao

import androidx.room.*
import com.myreviews.app.data.database.entity.ReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews ORDER BY visitDate DESC")
    fun getAllReviews(): Flow<List<ReviewEntity>>
    
    @Query("SELECT * FROM reviews WHERE restaurantId = :restaurantId ORDER BY visitDate DESC")
    fun getReviewsForRestaurant(restaurantId: Long): Flow<List<ReviewEntity>>
    
    @Query("SELECT * FROM reviews WHERE id = :reviewId")
    suspend fun getReviewById(reviewId: Long): ReviewEntity?
    
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
}
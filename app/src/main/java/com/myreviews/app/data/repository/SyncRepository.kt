package com.myreviews.app.data.repository

import android.content.Context
import com.myreviews.app.data.api.SyncService
import com.myreviews.app.data.api.SyncResult
import com.myreviews.app.data.database.dao.ReviewDao
import com.myreviews.app.data.database.entity.ReviewEntity
import com.myreviews.app.domain.model.Review
import com.myreviews.app.domain.model.User
import com.myreviews.app.ui.settings.SettingsActivity
import kotlinx.coroutines.flow.first
import java.util.Date

class SyncRepository(
    private val context: Context,
    private val reviewDao: ReviewDao,
    private val userRepository: UserRepository
) {
    private val prefs = context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
    
    private fun getSyncService(): SyncService? {
        if (!isSyncEnabled()) return null
        
        val serverUrl = prefs.getString(SettingsActivity.KEY_SERVER_URL, "") ?: ""
        val serverPort = prefs.getString(SettingsActivity.KEY_SERVER_PORT, "3000") ?: "3000"
        
        if (serverUrl.isEmpty()) return null
        
        return SyncService("http://$serverUrl:$serverPort")
    }
    
    fun isSyncEnabled(): Boolean {
        return prefs.getBoolean(SettingsActivity.KEY_CLOUD_SYNC_ENABLED, false)
    }
    
    suspend fun testConnection(): Boolean {
        val service = getSyncService() ?: return false
        return service.testConnection()
    }
    
    suspend fun performSync(): SyncResult {
        val service = getSyncService() ?: return SyncResult.Error("Sync not enabled")
        
        try {
            // 1. Get current user
            val currentUser = userRepository.getCurrentUser() 
                ?: return SyncResult.Error("No current user")
            
            // 2. Sync user data
            if (!service.syncUser(currentUser)) {
                return SyncResult.Error("Failed to sync user")
            }
            
            // 3. Upload local reviews
            val localReviews = reviewDao.getAllReviews().first()
            val reviewModels = localReviews.map { entity ->
                Review(
                    id = entity.id,
                    restaurantId = entity.restaurantId,
                    restaurantName = entity.restaurantName,
                    restaurantLat = entity.restaurantLat,
                    restaurantLon = entity.restaurantLon,
                    restaurantAddress = entity.restaurantAddress,
                    rating = entity.rating,
                    comment = entity.comment,
                    visitDate = entity.visitDate,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    userId = entity.userId,
                    userName = entity.userName
                )
            }
            
            val uploadResult = service.syncReviews(currentUser.userId, reviewModels)
            if (uploadResult is SyncResult.Error) {
                return uploadResult
            }
            
            // 4. Download remote reviews (optional - for multi-device support)
            // This could be implemented later
            
            return uploadResult
        } catch (e: Exception) {
            return SyncResult.Error(e.message ?: "Unknown error")
        }
    }
    
    suspend fun downloadReviews(): Int {
        val service = getSyncService() ?: return 0
        val currentUser = userRepository.getCurrentUser() ?: return 0
        
        try {
            val remoteReviews = service.fetchUserReviews(currentUser.userId)
            var newCount = 0
            
            for (remote in remoteReviews) {
                // Check if review already exists locally
                val existing = reviewDao.getReviewsForRestaurant(remote.restaurantId).first()
                    .find { it.userId == remote.userId }
                
                if (existing == null) {
                    // Insert new review
                    reviewDao.insertReview(ReviewEntity(
                        restaurantId = remote.restaurantId,
                        restaurantName = remote.restaurantName,
                        restaurantLat = remote.restaurantLat,
                        restaurantLon = remote.restaurantLon,
                        restaurantAddress = remote.restaurantAddress,
                        rating = remote.rating,
                        comment = remote.comment,
                        visitDate = remote.visitDate,
                        userId = remote.userId,
                        userName = remote.userName,
                        createdAt = Date(),
                        updatedAt = Date()
                    ))
                    newCount++
                }
            }
            
            return newCount
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }
}
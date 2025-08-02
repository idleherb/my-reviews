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
            
            // 3. Get all unsynced reviews (where updatedAt > syncedAt or syncedAt is null)
            val unsyncedReviews = reviewDao.getUnsyncedReviews()
            android.util.Log.d("SyncRepository", "Found ${unsyncedReviews.size} unsynced reviews")
            
            // Convert to domain models
            val reviewsToSync = unsyncedReviews.map { entity ->
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
                    userName = entity.userName,
                    syncedAt = entity.syncedAt,
                    isDeleted = entity.isDeleted
                )
            }
            
            // 4. Send all unsynced reviews to server
            val syncResponse = service.performBulkSync(currentUser.userId, reviewsToSync)
                ?: return SyncResult.Error("Sync failed - no response from server")
            
            android.util.Log.d("SyncRepository", "Server processed ${syncResponse.processed} reviews")
            
            // 5. Clear local database and replace with server data
            // First, delete all non-tombstone reviews
            reviewDao.deleteAllNonTombstoneReviews()
            
            // 6. Insert all reviews from server
            var insertedCount = 0
            for (remote in syncResponse.allReviews) {
                val entity = ReviewEntity(
                    id = remote.id, // Use the UUID from server
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
                    createdAt = remote.createdAt,
                    updatedAt = remote.updatedAt,
                    syncedAt = Date(), // Mark as synced
                    isDeleted = false  // Server only sends non-deleted
                )
                reviewDao.insertOrUpdateReview(entity)
                insertedCount++
            }
            
            // 7. Clean up local tombstones that have been synced
            reviewDao.deleteAllSyncedDeletedReviews()
            
            android.util.Log.d("SyncRepository", "Sync complete: ${syncResponse.processed} sent, $insertedCount received")
            
            return SyncResult.Success(
                syncedCount = syncResponse.processed,
                message = "${syncResponse.processed} Reviews synchronisiert, ${syncResponse.allReviews.size} Reviews empfangen"
            )
        } catch (e: Exception) {
            android.util.Log.e("SyncRepository", "Sync error", e)
            return SyncResult.Error(e.message ?: "Unknown error")
        }
    }
    
    suspend fun downloadReviews(): Int {
        val service = getSyncService() ?: return 0
        val currentUser = userRepository.getCurrentUser() ?: return 0
        
        try {
            // Fetch ALL reviews, not just current user's reviews
            val remoteReviews = service.fetchAllReviews()
            android.util.Log.d("SyncRepository", "Fetched ${remoteReviews.size} reviews from server")
            
            var newCount = 0
            var skippedCount = 0
            
            for (remote in remoteReviews) {
                // Check if review already exists locally (by restaurant + user)
                val existing = reviewDao.getReviewsForRestaurant(remote.restaurantId).first()
                    .find { it.userId == remote.userId }
                
                if (existing == null) {
                    // Insert new review
                    android.util.Log.d("SyncRepository", "Inserting review for ${remote.restaurantName} by ${remote.userName}")
                    val newId = reviewDao.insertReview(ReviewEntity(
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
                        createdAt = remote.createdAt,
                        updatedAt = remote.updatedAt,
                        syncedAt = Date(), // Markiere als synchronisiert
                        isDeleted = false
                    ))
                    newCount++
                } else if (existing.isDeleted) {
                    // Lokale Review ist als gelöscht markiert - nicht überschreiben!
                    android.util.Log.d("SyncRepository", "Skipping review for ${remote.restaurantName} - local is deleted")
                    skippedCount++
                } else if (existing.updatedAt.time < remote.updatedAt.time) {
                    // Remote ist neuer - update lokal
                    android.util.Log.d("SyncRepository", "Updating review for ${remote.restaurantName} - remote is newer")
                    reviewDao.updateReview(ReviewEntity(
                        id = existing.id,
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
                        createdAt = existing.createdAt, // Behalte original createdAt
                        updatedAt = remote.updatedAt,
                        syncedAt = Date(), // Markiere als synchronisiert
                        isDeleted = false
                    ))
                    skippedCount++
                } else {
                    android.util.Log.d("SyncRepository", "Skipping review for ${remote.restaurantName} - local is newer or same")
                    skippedCount++
                }
            }
            
            android.util.Log.d("SyncRepository", "Download complete: $newCount new, $skippedCount skipped")
            return newCount
        } catch (e: Exception) {
            android.util.Log.e("SyncRepository", "Error downloading reviews", e)
            e.printStackTrace()
            return 0
        }
    }
}
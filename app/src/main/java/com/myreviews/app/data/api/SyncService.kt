package com.myreviews.app.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import com.myreviews.app.domain.model.Review
import com.myreviews.app.domain.model.User

class SyncService(
    private val baseUrl: String
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }
    
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/health")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            responseCode == 200
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun syncUser(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/users/${user.userId}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            
            val jsonBody = JSONObject().apply {
                put("userName", user.userName)
                // Avatar is handled separately via AvatarService
            }
            
            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
            }
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            responseCode == 200
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun syncReviews(userId: String, reviews: List<Review>): SyncResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/reviews/sync")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            
            val reviewsArray = JSONArray()
            reviews.forEach { review ->
                reviewsArray.put(JSONObject().apply {
                    put("restaurantId", review.restaurantId)
                    put("restaurantName", review.restaurantName)
                    put("restaurantLat", review.restaurantLat)
                    put("restaurantLon", review.restaurantLon)
                    put("restaurantAddress", review.restaurantAddress)
                    put("rating", review.rating)
                    put("comment", review.comment)
                    put("visitDate", dateFormat.format(review.visitDate))
                    put("userName", review.userName ?: "Anonym")
                })
            }
            
            val jsonBody = JSONObject().apply {
                put("userId", userId)
                put("reviews", reviewsArray)
            }
            
            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
            }
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(response)
                val syncedCount = responseJson.getInt("synced")
                connection.disconnect()
                SyncResult.Success(syncedCount)
            } else {
                connection.disconnect()
                SyncResult.Error("Server returned code: $responseCode")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }
    
    suspend fun fetchUserReviews(userId: String, since: Date? = null): List<RemoteReview> = withContext(Dispatchers.IO) {
        try {
            val urlString = if (since != null) {
                "$baseUrl/api/reviews/user/$userId?since=${since.time}"
            } else {
                "$baseUrl/api/reviews/user/$userId"
            }
            
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)
                val reviews = mutableListOf<RemoteReview>()
                
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    reviews.add(RemoteReview(
                        id = obj.getLong("id"),
                        restaurantId = obj.getLong("restaurant_id"),
                        restaurantName = obj.getString("restaurant_name"),
                        restaurantLat = obj.getDouble("restaurant_lat"),
                        restaurantLon = obj.getDouble("restaurant_lon"),
                        restaurantAddress = obj.getString("restaurant_address"),
                        rating = obj.getDouble("rating").toFloat(),
                        comment = obj.getString("comment"),
                        visitDate = dateFormat.parse(obj.getString("visit_date"))!!,
                        userId = obj.getString("user_id"),
                        userName = obj.getString("user_name"),
                        createdAt = timestampFormat.parse(obj.getString("created_at"))!!,
                        updatedAt = timestampFormat.parse(obj.getString("updated_at"))!!
                    ))
                }
                
                connection.disconnect()
                reviews
            } else {
                connection.disconnect()
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun fetchAllUsers(): List<RemoteUser> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/users")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)
                val users = mutableListOf<RemoteUser>()
                
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    users.add(RemoteUser(
                        userId = obj.getString("user_id"),
                        userName = obj.getString("user_name"),
                        avatarUrl = if (obj.has("avatar_url") && !obj.isNull("avatar_url")) {
                            obj.getString("avatar_url")
                        } else null,
                        createdAt = timestampFormat.parse(obj.getString("created_at"))!!,
                        updatedAt = timestampFormat.parse(obj.getString("updated_at"))!!
                    ))
                }
                
                connection.disconnect()
                users
            } else {
                connection.disconnect()
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun createReview(review: Review): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/reviews")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            
            val jsonBody = JSONObject().apply {
                put("restaurantId", review.restaurantId)
                put("restaurantName", review.restaurantName)
                put("restaurantLat", review.restaurantLat)
                put("restaurantLon", review.restaurantLon)
                put("restaurantAddress", review.restaurantAddress)
                put("rating", review.rating)
                put("comment", review.comment)
                put("visitDate", dateFormat.format(review.visitDate))
                put("userId", review.userId)
                put("userName", review.userName)
                put("updatedAt", timestampFormat.format(review.updatedAt))
                put("createdAt", timestampFormat.format(review.createdAt))
            }
            
            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
            }
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            responseCode == 201
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun updateReview(review: Review): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/reviews/${review.id}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            
            val jsonBody = JSONObject().apply {
                put("rating", review.rating)
                put("comment", review.comment)
                put("visitDate", dateFormat.format(review.visitDate))
                put("userId", review.userId)
                put("updatedAt", timestampFormat.format(review.updatedAt))
            }
            
            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
            }
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            responseCode == 200
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun deleteReview(reviewId: Long, userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/reviews/$reviewId?userId=$userId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "DELETE"
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            responseCode == 204
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun performBulkSync(userId: String, reviews: List<Review>): SyncResponse? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/reviews/sync")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            
            val reviewsArray = JSONArray()
            for (review in reviews) {
                val reviewJson = JSONObject().apply {
                    put("id", review.id)
                    put("restaurantId", review.restaurantId)
                    put("restaurantName", review.restaurantName)
                    put("restaurantLat", review.restaurantLat)
                    put("restaurantLon", review.restaurantLon)
                    put("restaurantAddress", review.restaurantAddress)
                    put("rating", review.rating)
                    put("comment", review.comment)
                    put("visitDate", dateFormat.format(review.visitDate))
                    put("userId", review.userId)
                    put("userName", review.userName)
                    put("createdAt", timestampFormat.format(review.createdAt))
                    put("updatedAt", timestampFormat.format(review.updatedAt))
                    put("isDeleted", review.isDeleted)
                }
                reviewsArray.put(reviewJson)
            }
            
            val jsonBody = JSONObject().apply {
                put("userId", userId)
                put("reviews", reviewsArray)
            }
            
            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
            }
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                
                val processed = jsonResponse.getInt("processed")
                val allReviewsArray = jsonResponse.getJSONArray("allReviews")
                val allReviews = mutableListOf<RemoteReview>()
                
                for (i in 0 until allReviewsArray.length()) {
                    val obj = allReviewsArray.getJSONObject(i)
                    
                    // Parse reaction counts if present
                    val reactionCounts = mutableMapOf<String, Int>()
                    if (obj.has("reaction_counts") && !obj.isNull("reaction_counts")) {
                        val countsObj = obj.getJSONObject("reaction_counts")
                        countsObj.keys().forEach { emoji ->
                            reactionCounts[emoji] = countsObj.getInt(emoji)
                        }
                    }
                    
                    allReviews.add(RemoteReview(
                        id = obj.getLong("id"),
                        restaurantId = obj.getLong("restaurant_id"),
                        restaurantName = obj.getString("restaurant_name"),
                        restaurantLat = obj.getDouble("restaurant_lat"),
                        restaurantLon = obj.getDouble("restaurant_lon"),
                        restaurantAddress = obj.getString("restaurant_address"),
                        rating = obj.getDouble("rating").toFloat(),
                        comment = obj.getString("comment"),
                        visitDate = dateFormat.parse(obj.getString("visit_date"))!!,
                        userId = obj.getString("user_id"),
                        userName = obj.getString("user_name"),
                        createdAt = timestampFormat.parse(obj.getString("created_at"))!!,
                        updatedAt = timestampFormat.parse(obj.getString("updated_at"))!!,
                        reactionCounts = reactionCounts
                    ))
                }
                
                connection.disconnect()
                SyncResponse(processed, allReviews)
            } else {
                connection.disconnect()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun fetchAllReviews(since: Date? = null): List<RemoteReview> = withContext(Dispatchers.IO) {
        try {
            val urlString = if (since != null) {
                "$baseUrl/api/reviews?since=${since.time}"
            } else {
                "$baseUrl/api/reviews"
            }
            
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)
                val reviews = mutableListOf<RemoteReview>()
                
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    reviews.add(RemoteReview(
                        id = obj.getLong("id"),
                        restaurantId = obj.getLong("restaurant_id"),
                        restaurantName = obj.getString("restaurant_name"),
                        restaurantLat = obj.getDouble("restaurant_lat"),
                        restaurantLon = obj.getDouble("restaurant_lon"),
                        restaurantAddress = obj.getString("restaurant_address"),
                        rating = obj.getDouble("rating").toFloat(),
                        comment = obj.getString("comment"),
                        visitDate = dateFormat.parse(obj.getString("visit_date"))!!,
                        userId = obj.getString("user_id"),
                        userName = obj.getString("user_name"),
                        createdAt = timestampFormat.parse(obj.getString("created_at"))!!,
                        updatedAt = timestampFormat.parse(obj.getString("updated_at"))!!
                    ))
                }
                
                connection.disconnect()
                reviews
            } else {
                connection.disconnect()
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

sealed class SyncResult {
    data class Success(val syncedCount: Int, val message: String? = null) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

data class RemoteReview(
    val id: Long,
    val restaurantId: Long,
    val restaurantName: String,
    val restaurantLat: Double,
    val restaurantLon: Double,
    val restaurantAddress: String,
    val rating: Float,
    val comment: String,
    val visitDate: Date,
    val userId: String,
    val userName: String,
    val createdAt: Date,
    val updatedAt: Date,
    val reactionCounts: Map<String, Int> = emptyMap()
)

data class SyncResponse(
    val processed: Int,
    val allReviews: List<RemoteReview>
)

data class RemoteUser(
    val userId: String,
    val userName: String,
    val avatarUrl: String?,
    val createdAt: Date,
    val updatedAt: Date
)
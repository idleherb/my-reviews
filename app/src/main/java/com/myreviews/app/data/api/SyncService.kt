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
                        userName = obj.getString("user_name")
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
    data class Success(val syncedCount: Int) : SyncResult()
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
    val userName: String
)
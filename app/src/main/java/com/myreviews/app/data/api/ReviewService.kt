package com.myreviews.app.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.myreviews.app.domain.model.Review

class ReviewService(
    private val baseUrl: String
) {
    suspend fun updateReview(reviewId: String, rating: Float, comment: String, visitDate: String, userId: String): Boolean = 
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/api/reviews/$reviewId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "PUT"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                
                val jsonBody = JSONObject().apply {
                    put("rating", rating)
                    put("comment", comment)
                    put("visitDate", visitDate)
                    put("userId", userId)
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
    
    suspend fun deleteReview(reviewId: String, userId: String): Boolean = 
        withContext(Dispatchers.IO) {
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
}
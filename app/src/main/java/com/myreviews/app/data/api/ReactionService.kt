package com.myreviews.app.data.api

import com.myreviews.app.domain.model.Reaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ReactionService(
    private val baseUrl: String
) {
    data class ReactionResponse(
        val reactions: List<Reaction>,
        val counts: Map<String, Int>
    )
    
    suspend fun getReactions(reviewId: String): ReactionResponse = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/reactions/review/$reviewId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                // Parse reactions
                val reactions = mutableListOf<Reaction>()
                val reactionsArray = json.getJSONArray("reactions")
                for (i in 0 until reactionsArray.length()) {
                    val obj = reactionsArray.getJSONObject(i)
                    reactions.add(Reaction(
                        id = obj.getLong("id"),
                        reviewId = obj.getString("review_id"),
                        userId = obj.getString("user_id"),
                        userName = obj.getString("user_name"),
                        emoji = obj.getString("emoji")
                    ))
                }
                
                // Parse counts
                val counts = mutableMapOf<String, Int>()
                val countsObj = json.getJSONObject("counts")
                countsObj.keys().forEach { emoji ->
                    counts[emoji] = countsObj.getInt(emoji)
                }
                
                connection.disconnect()
                ReactionResponse(reactions, counts)
            } else {
                connection.disconnect()
                ReactionResponse(emptyList(), emptyMap())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ReactionResponse(emptyList(), emptyMap())
        }
    }
    
    suspend fun addReaction(reviewId: String, userId: String, emoji: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/reactions/review/$reviewId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            
            val jsonBody = JSONObject().apply {
                put("userId", userId)
                put("emoji", emoji)
            }
            
            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
            }
            
            val success = connection.responseCode == 200
            connection.disconnect()
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun removeReaction(reviewId: String, userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/reactions/review/$reviewId/user/$userId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "DELETE"
            
            val success = connection.responseCode == 200
            connection.disconnect()
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
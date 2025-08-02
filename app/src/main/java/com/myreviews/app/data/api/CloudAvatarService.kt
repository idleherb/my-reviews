package com.myreviews.app.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CloudAvatarService(
    private val baseUrl: String
) {
    // Cache für Avatar-URLs (nur zur Laufzeit)
    private val avatarCache = mutableMapOf<String, String?>()
    
    suspend fun getUserAvatarUrl(userId: String): String? = withContext(Dispatchers.IO) {
        // Check cache first
        if (avatarCache.containsKey(userId)) {
            return@withContext avatarCache[userId]
        }
        
        try {
            val url = URL("$baseUrl/api/users/$userId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                val avatarUrl = if (json.has("avatar_url") && !json.isNull("avatar_url")) {
                    json.getString("avatar_url")
                } else {
                    null
                }
                
                // Cache the result
                avatarCache[userId] = avatarUrl
                
                connection.disconnect()
                avatarUrl
            } else {
                connection.disconnect()
                // Cache negative result too
                avatarCache[userId] = null
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Cache error as null
            avatarCache[userId] = null
            null
        }
    }
    
    fun clearCache() {
        avatarCache.clear()
    }
    
    fun updateCache(userId: String, avatarUrl: String?) {
        avatarCache[userId] = avatarUrl
    }
}
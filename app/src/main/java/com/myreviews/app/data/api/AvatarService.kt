package com.myreviews.app.data.api

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AvatarService(
    private val baseUrl: String
) {
    suspend fun uploadAvatar(context: Context, userId: String, imageUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val boundary = "----WebKitFormBoundary" + System.currentTimeMillis().toString(16)
            val url = URL("$baseUrl/api/avatars/$userId")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                doOutput = true
                doInput = true
                requestMethod = "POST"
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("Accept", "application/json")
            }
            
            val outputStream = DataOutputStream(connection.outputStream)
            
            // Add file part
            outputStream.writeBytes("--$boundary\r\n")
            outputStream.writeBytes("Content-Disposition: form-data; name=\"avatar\"; filename=\"avatar.jpg\"\r\n")
            outputStream.writeBytes("Content-Type: image/jpeg\r\n\r\n")
            
            // Write image data
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
            }
            
            outputStream.writeBytes("\r\n")
            outputStream.writeBytes("--$boundary--\r\n")
            outputStream.flush()
            outputStream.close()
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                // Parse JSON response to get avatar URL
                val avatarUrl = try {
                    val json = org.json.JSONObject(response)
                    json.getString("avatarUrl")
                } catch (e: Exception) {
                    null
                }
                connection.disconnect()
                avatarUrl
            } else {
                connection.disconnect()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun deleteAvatar(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/avatars/$userId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "DELETE"
            
            val success = connection.responseCode == HttpURLConnection.HTTP_OK
            connection.disconnect()
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
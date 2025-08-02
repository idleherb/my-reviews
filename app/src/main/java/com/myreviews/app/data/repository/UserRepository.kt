package com.myreviews.app.data.repository

import android.content.Context
import android.provider.Settings
import com.myreviews.app.data.database.dao.UserDao
import com.myreviews.app.domain.model.User
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class UserRepository(
    private val userDao: UserDao,
    private val context: Context
) {
    suspend fun getCurrentUser(): User? {
        return userDao.getCurrentUser()
    }
    
    fun getCurrentUserFlow(): Flow<User?> {
        return userDao.getCurrentUserFlow()
    }
    
    suspend fun getUserById(userId: String): User? {
        return userDao.getUserById(userId)
    }
    
    fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers()
    }
    
    suspend fun createUser(userName: String = "Anonym"): User {
        // Verwende Android Secure ID als Basis für die User ID
        // Diese ID bleibt gleich, auch nach App-Neuinstallation
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        
        // Erstelle eine deterministische UUID basierend auf der Android ID
        // Dies stellt sicher, dass das gleiche Gerät immer die gleiche UUID bekommt
        val userId = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()
        
        // Prüfe ob User bereits existiert
        val existingUser = getUserById(userId)
        if (existingUser != null) {
            // User existiert bereits, gib ihn zurück
            return existingUser
        }
        
        val user = User(
            userId = userId,
            userName = userName,
            createdAt = System.currentTimeMillis(),
            isCurrentUser = false
        )
        userDao.insertUser(user)
        return user
    }
    
    suspend fun updateUserName(userId: String, newUserName: String) {
        userDao.updateUserName(userId, newUserName)
    }
    
    suspend fun setCurrentUser(userId: String) {
        userDao.setCurrentUser(userId)
    }
    
    suspend fun ensureDefaultUser(): User {
        val currentUser = getCurrentUser()
        if (currentUser != null) {
            return currentUser
        }
        
        // Generiere die geräte-basierte User ID
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val deviceUserId = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()
        
        // Prüfe ob ein User mit dieser ID bereits existiert
        val existingUser = getUserById(deviceUserId)
        if (existingUser != null) {
            // User existiert, aber ist nicht als current markiert
            setCurrentUser(existingUser.userId)
            return existingUser
        }
        
        // Erstelle neuen Default-User
        val newUser = createUser("Anonym")
        setCurrentUser(newUser.userId)
        return newUser
    }
    
    // Avatar is now cloud-only, no local storage
}
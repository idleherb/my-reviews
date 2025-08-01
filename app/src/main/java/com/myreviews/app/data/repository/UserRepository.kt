package com.myreviews.app.data.repository

import com.myreviews.app.data.database.dao.UserDao
import com.myreviews.app.domain.model.User
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class UserRepository(
    private val userDao: UserDao
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
        val user = User(
            userId = UUID.randomUUID().toString(),
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
        
        // Erstelle neuen Default-User
        val newUser = createUser("Anonym")
        setCurrentUser(newUser.userId)
        return newUser
    }
}
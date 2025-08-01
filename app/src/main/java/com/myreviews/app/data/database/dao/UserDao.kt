package com.myreviews.app.data.database.dao

import androidx.room.*
import com.myreviews.app.domain.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUser(): User?
    
    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUserFlow(): Flow<User?>
    
    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String): User?
    
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
    
    @Update
    suspend fun updateUser(user: User)
    
    @Query("UPDATE users SET userName = :userName WHERE userId = :userId")
    suspend fun updateUserName(userId: String, userName: String)
    
    @Query("UPDATE users SET isCurrentUser = 0")
    suspend fun clearCurrentUser()
    
    @Transaction
    suspend fun setCurrentUser(userId: String) {
        clearCurrentUser()
        val user = getUserById(userId)
        if (user != null) {
            insertUser(user.copy(isCurrentUser = true))
        }
    }
    
    @Delete
    suspend fun deleteUser(user: User)
}
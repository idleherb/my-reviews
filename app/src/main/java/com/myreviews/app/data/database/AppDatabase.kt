package com.myreviews.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.myreviews.app.data.database.converter.DateConverter
import com.myreviews.app.data.database.dao.ReviewDao
import com.myreviews.app.data.database.dao.UserDao
import com.myreviews.app.data.database.entity.ReviewEntity
import com.myreviews.app.domain.model.User
import java.util.UUID

@Database(
    entities = [ReviewEntity::class, User::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reviewDao(): ReviewDao
    abstract fun userDao(): UserDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. User-Tabelle erstellen
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS users (
                        userId TEXT NOT NULL PRIMARY KEY,
                        userName TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        isCurrentUser INTEGER NOT NULL
                    )"""
                )
                
                // 2. Default-User erstellen
                val defaultUserId = UUID.randomUUID().toString()
                val currentTime = System.currentTimeMillis()
                database.execSQL(
                    """INSERT INTO users (userId, userName, createdAt, isCurrentUser) 
                       VALUES ('$defaultUserId', 'Anonym', $currentTime, 1)"""
                )
                
                // 3. Reviews-Tabelle erweitern
                database.execSQL("ALTER TABLE reviews ADD COLUMN userId TEXT NOT NULL DEFAULT '$defaultUserId'")
                database.execSQL("ALTER TABLE reviews ADD COLUMN userName TEXT NOT NULL DEFAULT 'Anonym'")
                
                // 4. Existierende Reviews dem Default-User zuordnen
                database.execSQL("UPDATE reviews SET userId = '$defaultUserId', userName = 'Anonym'")
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myreviews_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
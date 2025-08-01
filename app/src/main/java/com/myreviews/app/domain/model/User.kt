package com.myreviews.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val userId: String,      // UUID
    val userName: String,    // Anzeigename (änderbar)
    val createdAt: Long,     // Timestamp der Erstellung
    val isCurrentUser: Boolean = false // Markiert den aktiven User
)
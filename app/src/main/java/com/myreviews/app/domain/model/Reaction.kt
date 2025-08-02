package com.myreviews.app.domain.model

import java.util.Date

data class Reaction(
    val id: Long = 0,
    val reviewId: Long,
    val userId: String,
    val userName: String,
    val emoji: String,
    val createdAt: Date = Date()
) {
    companion object {
        val ALLOWED_EMOJIS = listOf("❤️", "👍", "😂", "🤔", "😮")
        
        fun isValidEmoji(emoji: String): Boolean {
            return emoji in ALLOWED_EMOJIS
        }
    }
}
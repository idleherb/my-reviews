package com.myreviews.app.domain.model

import org.junit.Test
import org.junit.Assert.*
import java.util.Date

class ReviewTest {
    
    @Test
    fun `review creation with valid data should succeed`() {
        val review = Review(
            id = 1,
            restaurantId = 100,
            rating = 4.5f,
            comment = "Great food!",
            createdAt = Date()
        )
        
        assertEquals(1, review.id)
        assertEquals(100, review.restaurantId)
        assertEquals(4.5f, review.rating)
        assertEquals("Great food!", review.comment)
        assertNotNull(review.createdAt)
    }
    
    @Test
    fun `rating validation should work correctly`() {
        assertTrue(Review.isValidRating(0f))
        assertTrue(Review.isValidRating(2.5f))
        assertTrue(Review.isValidRating(5f))
        
        assertFalse(Review.isValidRating(-1f))
        assertFalse(Review.isValidRating(5.1f))
    }
}
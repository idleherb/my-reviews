package com.myreviews.app.domain.model

import org.junit.Test
import org.junit.Assert.*

class RestaurantTest {
    
    @Test
    fun `restaurant creation with valid data should succeed`() {
        val restaurant = Restaurant(
            id = 1,
            name = "Test Restaurant",
            latitude = 52.520008,
            longitude = 13.404954,
            address = "Test Street 123"
        )
        
        assertEquals(1, restaurant.id)
        assertEquals("Test Restaurant", restaurant.name)
        assertEquals(52.520008, restaurant.latitude, 0.000001)
        assertEquals(13.404954, restaurant.longitude, 0.000001)
        assertEquals("Test Street 123", restaurant.address)
    }
    
    @Test
    fun `restaurant average rating calculation should be correct`() {
        val restaurant = Restaurant(
            id = 1,
            name = "Test Restaurant",
            latitude = 52.520008,
            longitude = 13.404954,
            address = "Test Street 123"
        )
        
        val ratings = listOf(5.0f, 4.0f, 3.0f, 5.0f, 4.0f)
        val average = restaurant.calculateAverageRating(ratings)
        
        assertEquals(4.2f, average, 0.01f)
    }
}
package com.myreviews.app.data.api

import com.myreviews.app.domain.model.Restaurant
import org.osmdroid.util.BoundingBox

interface RestaurantSearchService {
    suspend fun searchRestaurants(
        query: String,
        boundingBox: BoundingBox?,
        userLat: Double? = null,
        userLon: Double? = null
    ): List<Restaurant>
    
    suspend fun getNearbyRestaurants(
        lat: Double,
        lon: Double,
        radiusMeters: Int = 1000
    ): List<Restaurant>
    
    suspend fun getRestaurantsInBounds(
        boundingBox: BoundingBox
    ): List<Restaurant>
}
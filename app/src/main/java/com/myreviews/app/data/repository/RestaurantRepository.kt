package com.myreviews.app.data.repository

import com.myreviews.app.data.api.RestaurantSearchService
import com.myreviews.app.domain.model.Restaurant
import com.myreviews.app.ui.map.MapFragment

class RestaurantRepository(
    private val searchService: RestaurantSearchService
) {
    
    suspend fun getRestaurantsNearby(lat: Double, lon: Double, radiusMeters: Int = 1000): List<Restaurant> {
        return searchService.getNearbyRestaurants(lat, lon, radiusMeters)
    }
    
    suspend fun getRestaurantsInBounds(
        latSouth: Double,
        lonWest: Double,
        latNorth: Double,
        lonEast: Double
    ): List<Restaurant> {
        val boundingBox = org.osmdroid.util.BoundingBox(latNorth, lonEast, latSouth, lonWest)
        return searchService.getRestaurantsInBounds(boundingBox)
    }
    
    suspend fun searchRestaurantsByName(name: String, userLat: Double? = null, userLon: Double? = null): List<Restaurant> {
        val mapBounds = MapFragment.currentMapBounds
        val results = searchService.searchRestaurants(name, mapBounds, userLat, userLon)
        
        // Sortiere nach Entfernung, falls Benutzerposition vorhanden
        return if (userLat != null && userLon != null) {
            results.sortedBy { restaurant ->
                calculateDistance(userLat, userLon, restaurant.latitude, restaurant.longitude)
            }
        } else {
            results
        }
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Kilometer
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }
}
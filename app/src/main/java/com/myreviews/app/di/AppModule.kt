package com.myreviews.app.di

import com.myreviews.app.data.api.RestaurantSearchService
import com.myreviews.app.data.api.OverpassSearchService
import com.myreviews.app.data.api.NominatimSearchService
import com.myreviews.app.data.repository.RestaurantRepository

object AppModule {
    // Dynamische Service-Auswahl basierend auf ServiceLocator
    private val searchService: RestaurantSearchService
        get() = when (ServiceLocator.currentSearchService) {
            SearchServiceType.OVERPASS -> OverpassSearchService()
            SearchServiceType.NOMINATIM -> NominatimSearchService()
        }
    
    // Repository wird immer mit dem aktuellen Service erstellt
    val restaurantRepository: RestaurantRepository
        get() = RestaurantRepository(searchService)
}
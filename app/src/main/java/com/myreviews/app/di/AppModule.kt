package com.myreviews.app.di

import android.content.Context
import com.myreviews.app.data.api.RestaurantSearchService
import com.myreviews.app.data.api.OverpassSearchService
import com.myreviews.app.data.api.NominatimSearchService
import com.myreviews.app.data.repository.RestaurantRepository
import com.myreviews.app.data.database.AppDatabase
import com.myreviews.app.data.preferences.AppPreferences

object AppModule {
    private lateinit var applicationContext: Context
    
    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }
    
    // App Preferences
    val appPreferences: AppPreferences by lazy {
        AppPreferences(applicationContext)
    }
    
    // Dynamische Service-Auswahl basierend auf ServiceLocator
    private val searchService: RestaurantSearchService
        get() = when (ServiceLocator.currentSearchService) {
            SearchServiceType.OVERPASS -> OverpassSearchService()
            SearchServiceType.NOMINATIM -> NominatimSearchService()
        }
    
    // Database Singleton
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(applicationContext)
    }
    
    // Repository wird immer mit dem aktuellen Service erstellt
    val restaurantRepository: RestaurantRepository
        get() = RestaurantRepository(searchService)
    
    // Review Repository
    val reviewRepository: com.myreviews.app.data.repository.ReviewRepository by lazy {
        com.myreviews.app.data.repository.ReviewRepository(database.reviewDao())
    }
}
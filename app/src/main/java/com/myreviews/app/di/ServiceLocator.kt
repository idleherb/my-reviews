package com.myreviews.app.di

enum class SearchServiceType {
    OVERPASS,
    NOMINATIM
}

object ServiceLocator {
    var currentSearchService = SearchServiceType.OVERPASS
    
    fun switchToNominatim() {
        currentSearchService = SearchServiceType.NOMINATIM
        // AppModule wird automatisch die neue Implementierung verwenden
    }
    
    fun switchToOverpass() {
        currentSearchService = SearchServiceType.OVERPASS
    }
}
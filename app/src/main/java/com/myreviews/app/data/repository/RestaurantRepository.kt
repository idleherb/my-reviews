package com.myreviews.app.data.repository

import com.myreviews.app.data.api.OverpassApi
import com.myreviews.app.domain.model.Restaurant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RestaurantRepository {
    private val overpassApi: OverpassApi = Retrofit.Builder()
        .baseUrl("https://overpass-api.de/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OverpassApi::class.java)
    
    suspend fun getRestaurantsNearby(lat: Double, lon: Double, radiusMeters: Int = 1000): List<Restaurant> {
        return withContext(Dispatchers.IO) {
            try {
                // Overpass QL Query für Restaurants
                val query = """
                    [out:json][timeout:25];
                    (
                      node["amenity"="restaurant"](around:$radiusMeters,$lat,$lon);
                      way["amenity"="restaurant"](around:$radiusMeters,$lat,$lon);
                    );
                    out body;
                    >;
                    out skel qt;
                """.trimIndent()
                
                val response = overpassApi.getRestaurants(query)
                
                response.elements.mapNotNull { element ->
                    element.tags?.get("name")?.let { name ->
                        Restaurant(
                            id = element.id,
                            name = name,
                            latitude = element.lat,
                            longitude = element.lon,
                            address = element.tags["addr:street"] ?: "",
                            averageRating = null,
                            reviewCount = 0
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    suspend fun searchRestaurantsByName(name: String, userLat: Double? = null, userLon: Double? = null): List<Restaurant> {
        return withContext(Dispatchers.IO) {
            try {
                // Overpass QL Query für Restaurant-Suche nach Namen
                // Sucht deutschlandweit (oder kann angepasst werden)
                val query = """
                    [out:json][timeout:25];
                    area["ISO3166-1"="DE"]->.searchArea;
                    (
                      node["amenity"="restaurant"]["name"~"$name",i](area.searchArea);
                      way["amenity"="restaurant"]["name"~"$name",i](area.searchArea);
                    );
                    out body;
                    >;
                    out skel qt;
                """.trimIndent()
                
                val response = overpassApi.getRestaurants(query)
                
                val restaurants = response.elements.mapNotNull { element ->
                    element.tags?.get("name")?.let { restaurantName ->
                        Restaurant(
                            id = element.id,
                            name = restaurantName,
                            latitude = element.lat,
                            longitude = element.lon,
                            address = buildString {
                                element.tags["addr:street"]?.let { append(it) }
                                element.tags["addr:housenumber"]?.let { append(" $it") }
                                if (isNotEmpty()) append(", ")
                                element.tags["addr:postcode"]?.let { append("$it ") }
                                element.tags["addr:city"]?.let { append(it) }
                            }.ifEmpty { "Keine Adresse verfügbar" },
                            averageRating = null,
                            reviewCount = 0
                        )
                    }
                }
                
                // Sortiere nach Entfernung, falls Benutzerposition vorhanden
                if (userLat != null && userLon != null) {
                    restaurants.sortedBy { restaurant ->
                        calculateDistance(userLat, userLon, restaurant.latitude, restaurant.longitude)
                    }
                } else {
                    restaurants
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
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
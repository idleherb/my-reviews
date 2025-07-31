package com.myreviews.app.data.repository

import com.myreviews.app.data.api.OverpassApi
import com.myreviews.app.domain.model.Restaurant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log

class RestaurantRepository {
    private val overpassApi: OverpassApi = Retrofit.Builder()
        .baseUrl("https://overpass-api.de/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()
        )
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
                    if (element.lat != null && element.lon != null) {
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
                    } else null
                }
            } catch (e: Exception) {
                Log.e("RestaurantRepo", "Error searching restaurants: ${e.message}", e)
                emptyList()
            }
        }
    }
    
    suspend fun searchRestaurantsByName(name: String, userLat: Double? = null, userLon: Double? = null): List<Restaurant> {
        return withContext(Dispatchers.IO) {
            try {
                // Vereinfachte Query - sucht in einem 50km Radius um Heidelberg
                val centerLat = userLat ?: 49.409445  // Heidelberg als Default
                val centerLon = userLon ?: 8.693886
                val radiusKm = 50000  // 50km Radius
                
                val query = """
                    [out:json][timeout:30];
                    (
                      node["amenity"="restaurant"]["name"~"$name",i](around:$radiusKm,$centerLat,$centerLon);
                    );
                    out body;
                """.trimIndent()
                
                Log.d("RestaurantRepo", "Searching for restaurants with name: $name")
                Log.d("RestaurantRepo", "Query: $query")
                
                val response = overpassApi.getRestaurants(query)
                
                Log.d("RestaurantRepo", "Response received with ${response.elements.size} elements")
                
                val restaurants = response.elements.mapNotNull { element ->
                    if (element.lat != null && element.lon != null) {
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
                    } else null
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
                Log.e("RestaurantRepo", "Error searching restaurants: ${e.message}", e)
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
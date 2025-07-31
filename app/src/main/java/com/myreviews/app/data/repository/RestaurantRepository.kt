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
}
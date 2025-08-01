package com.myreviews.app.data.api

import android.util.Log
import com.myreviews.app.domain.model.Restaurant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.BoundingBox
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OverpassSearchService : RestaurantSearchService {
    
    private val httpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "MyReviews Android App")
                .build()
            chain.proceed(request)
        }
        .build()
    
    private val overpassApi: OverpassApi = Retrofit.Builder()
        .baseUrl("https://overpass-api.de/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()
        .create(OverpassApi::class.java)
    
    override suspend fun searchRestaurants(
        query: String,
        boundingBox: BoundingBox?,
        userLat: Double?,
        userLon: Double?
    ): List<Restaurant> = withContext(Dispatchers.IO) {
        try {
            Log.d("OverpassSearch", "Searching for: $query")
            
            val bbox = if (boundingBox != null) {
                "${boundingBox.latSouth},${boundingBox.lonWest},${boundingBox.latNorth},${boundingBox.lonEast}"
            } else {
                // Fallback: Berechne Bounding Box um die aktuelle Position
                val centerLat = userLat ?: 49.409445  // Heidelberg als Default
                val centerLon = userLon ?: 8.693886
                val latDiff = 0.05
                val lonDiff = 0.05
                "${centerLat - latDiff},${centerLon - lonDiff},${centerLat + latDiff},${centerLon + lonDiff}"
            }
            
            val overpassQuery = """
                [out:json][timeout:10];
                (
                  node["amenity"~"restaurant|fast_food|cafe"]["name"~"${query}",i]($bbox);
                  node["amenity"~"restaurant|fast_food|cafe"]["brand"~"${query}",i]($bbox);
                  node["amenity"~"restaurant|fast_food|cafe"]["cuisine"~"${query}",i]($bbox);
                  node["amenity"~"restaurant|fast_food|cafe"]["operator"~"${query}",i]($bbox);
                  way["amenity"~"restaurant|fast_food|cafe"]["name"~"${query}",i]($bbox);
                  way["amenity"~"restaurant|fast_food|cafe"]["brand"~"${query}",i]($bbox);
                  way["amenity"~"restaurant|fast_food|cafe"]["cuisine"~"${query}",i]($bbox);
                );
                out center;
            """.trimIndent()
            
            Log.d("OverpassSearch", "Query: $overpassQuery")
            
            val response = overpassApi.getRestaurants(overpassQuery)
            Log.d("OverpassSearch", "Got ${response.elements.size} results")
            
            response.elements.mapNotNull { element ->
                val lat = element.lat ?: element.center?.lat
                val lon = element.lon ?: element.center?.lon
                
                if (lat != null && lon != null && element.tags != null) {
                    val name = element.tags["name"] 
                        ?: element.tags["brand"] 
                        ?: element.tags["operator"]
                        ?: "Unbenanntes Restaurant"
                        
                    Restaurant(
                        id = element.id,
                        name = name,
                        latitude = lat,
                        longitude = lon,
                        address = buildString {
                            element.tags["addr:street"]?.let { append(it) }
                            element.tags["addr:housenumber"]?.let { append(" $it") }
                            if (isNotEmpty()) append(", ")
                            element.tags["addr:postcode"]?.let { append("$it ") }
                            element.tags["addr:city"]?.let { append(it) }
                        }.ifEmpty { 
                            element.tags["addr:full"] ?: "Keine Adresse verfügbar" 
                        },
                        averageRating = null,
                        reviewCount = 0,
                        amenityType = element.tags["amenity"] ?: "restaurant"
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.e("OverpassSearch", "Error searching restaurants", e)
            emptyList()
        }
    }
    
    override suspend fun getNearbyRestaurants(
        lat: Double,
        lon: Double,
        radiusMeters: Int
    ): List<Restaurant> = withContext(Dispatchers.IO) {
        try {
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
                            reviewCount = 0,
                            amenityType = element.tags["amenity"] ?: "restaurant"
                        )
                    }
                } else null
            }
        } catch (e: Exception) {
            Log.e("OverpassSearch", "Error getting nearby restaurants", e)
            emptyList()
        }
    }
    
    override suspend fun getRestaurantsInBounds(
        boundingBox: BoundingBox
    ): List<Restaurant> = withContext(Dispatchers.IO) {
        try {
            val bbox = "${boundingBox.latSouth},${boundingBox.lonWest},${boundingBox.latNorth},${boundingBox.lonEast}"
            
            val query = """
                [out:json][timeout:25];
                (
                  node["amenity"~"restaurant|fast_food|cafe"]($bbox);
                  way["amenity"~"restaurant|fast_food|cafe"]($bbox);
                );
                out center;
            """.trimIndent()
            
            Log.d("OverpassSearch", "Loading restaurants in bounds: $bbox")
            
            val response = overpassApi.getRestaurants(query)
            
            Log.d("OverpassSearch", "Got ${response.elements.size} restaurants in bounds")
            
            response.elements.mapNotNull { element ->
                val lat = element.lat ?: element.center?.lat
                val lon = element.lon ?: element.center?.lon
                
                if (lat != null && lon != null && element.tags != null) {
                    val name = element.tags["name"] 
                        ?: element.tags["brand"] 
                        ?: element.tags["operator"]
                        ?: "Unbenanntes Restaurant"
                        
                    Restaurant(
                        id = element.id,
                        name = name,
                        latitude = lat,
                        longitude = lon,
                        address = buildString {
                            element.tags["addr:street"]?.let { append(it) }
                            element.tags["addr:housenumber"]?.let { append(" $it") }
                            if (isNotEmpty()) append(", ")
                            element.tags["addr:postcode"]?.let { append("$it ") }
                            element.tags["addr:city"]?.let { append(it) }
                        }.ifEmpty { 
                            element.tags["addr:full"] ?: "Keine Adresse verfügbar" 
                        },
                        averageRating = null,
                        reviewCount = 0,
                        amenityType = element.tags["amenity"] ?: "restaurant"
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.e("OverpassSearch", "Error getting restaurants in bounds", e)
            emptyList()
        }
    }
}
package com.myreviews.app.data.repository

import com.myreviews.app.data.api.OverpassApi
import com.myreviews.app.data.api.NominatimApi
import com.myreviews.app.domain.model.Restaurant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log
import com.myreviews.app.ui.map.MapFragment

class RestaurantRepository {
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
    
    private val nominatimApi: NominatimApi = Retrofit.Builder()
        .baseUrl("https://nominatim.openstreetmap.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()
        .create(NominatimApi::class.java)
    
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
                Log.d("RestaurantRepo", "Searching restaurants with Nominatim for: $name")
                
                // Verwende die aktuellen Kartengrenzen oder falle auf Standard zurück
                val mapBounds = MapFragment.currentMapBounds
                val viewbox = if (mapBounds != null) {
                    "${mapBounds.lonWest},${mapBounds.latSouth},${mapBounds.lonEast},${mapBounds.latNorth}"
                } else {
                    // Fallback: Berechne Bounding Box um die aktuelle Position (ca. 20km Radius)
                    val centerLat = userLat ?: 49.409445  // Heidelberg als Default
                    val centerLon = userLon ?: 8.693886
                    val latDiff = 0.18
                    val lonDiff = 0.18
                    "${centerLon - lonDiff},${centerLat - latDiff},${centerLon + lonDiff},${centerLat + latDiff}"
                }
                
                // Nominatim-Suche für Restaurants
                val searchQuery = "$name restaurant"
                val results = nominatimApi.searchRestaurants(
                    query = searchQuery,
                    viewbox = viewbox
                )
                
                Log.d("RestaurantRepo", "Searching in area: $viewbox")
                Log.d("RestaurantRepo", "Nominatim returned ${results.size} results")
                
                val restaurants = results.mapNotNull { result ->
                    // Restaurants und Fast-Food einschließen
                    Restaurant(
                            id = result.place_id,
                            name = result.name ?: result.display_name.split(",").first(),
                            latitude = result.lat.toDouble(),
                            longitude = result.lon.toDouble(),
                            address = buildString {
                                result.address?.let { addr ->
                                    addr.road?.let { append(it) }
                                    addr.house_number?.let { append(" $it") }
                                    if (isNotEmpty()) append(", ")
                                    addr.postcode?.let { append("$it ") }
                                    (addr.city ?: addr.town ?: addr.village)?.let { append(it) }
                                }
                            }.ifEmpty { result.display_name },
                            averageRating = null,
                            reviewCount = 0
                    )
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
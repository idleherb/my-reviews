package com.myreviews.app.data.api

import android.util.Log
import com.myreviews.app.domain.model.Restaurant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.BoundingBox
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NominatimSearchService : RestaurantSearchService {
    
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
    
    private val nominatimApi: NominatimApi = Retrofit.Builder()
        .baseUrl("https://nominatim.openstreetmap.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()
        .create(NominatimApi::class.java)
    
    override suspend fun searchRestaurants(
        query: String,
        boundingBox: BoundingBox?,
        userLat: Double?,
        userLon: Double?
    ): List<Restaurant> = withContext(Dispatchers.IO) {
        try {
            Log.d("NominatimSearch", "Searching for: $query")
            
            val viewbox = if (boundingBox != null) {
                "${boundingBox.lonWest},${boundingBox.latSouth},${boundingBox.lonEast},${boundingBox.latNorth}"
            } else {
                // Fallback
                val centerLat = userLat ?: 49.409445
                val centerLon = userLon ?: 8.693886
                val latDiff = 0.18
                val lonDiff = 0.18
                "${centerLon - lonDiff},${centerLat - latDiff},${centerLon + lonDiff},${centerLat + latDiff}"
            }
            
            val searchQuery = "$query restaurant"
            val results = nominatimApi.searchRestaurants(
                query = searchQuery,
                viewbox = viewbox
            )
            
            Log.d("NominatimSearch", "Got ${results.size} results")
            
            results.map { result ->
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
        } catch (e: Exception) {
            Log.e("NominatimSearch", "Error searching restaurants", e)
            emptyList()
        }
    }
    
    override suspend fun getNearbyRestaurants(
        lat: Double,
        lon: Double,
        radiusMeters: Int
    ): List<Restaurant> {
        // Nominatim doesn't have a good "nearby" search, so we use a bounding box
        val kmRadius = radiusMeters / 1000.0
        val latDiff = kmRadius / 111.0  // Rough conversion
        val lonDiff = kmRadius / (111.0 * Math.cos(Math.toRadians(lat)))
        
        val boundingBox = BoundingBox(
            lat + latDiff, lon + lonDiff,
            lat - latDiff, lon - lonDiff
        )
        
        return searchRestaurants("", boundingBox, lat, lon)
    }
    
    override suspend fun getRestaurantsInBounds(
        boundingBox: BoundingBox
    ): List<Restaurant> {
        // Nominatim ist nicht ideal für Bounds-Suche, verwende Search mit leerem Query
        return searchRestaurants("", boundingBox)
    }
}
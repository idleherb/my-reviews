package com.myreviews.app.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface OverpassApi {
    @GET("interpreter")
    suspend fun getRestaurants(
        @Query("data") query: String
    ): OverpassResponse
}

data class OverpassResponse(
    val elements: List<Element>
)

data class Element(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val tags: Map<String, String>?
)
package com.myreviews.app.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface OverpassApi {
    @GET("interpreter")
    @retrofit2.http.Headers("Accept: application/json")
    suspend fun getRestaurants(
        @Query("data") query: String
    ): OverpassResponse
}

data class OverpassResponse(
    val version: Double? = null,
    val generator: String? = null,
    val elements: List<Element> = emptyList()
)

data class Element(
    val type: String? = null,
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val tags: Map<String, String>? = null,
    val center: Center? = null
)

data class Center(
    val lat: Double,
    val lon: Double
)
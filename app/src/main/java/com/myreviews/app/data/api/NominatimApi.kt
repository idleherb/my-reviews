package com.myreviews.app.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApi {
    @GET("search")
    suspend fun searchRestaurants(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 50,
        @Query("extratags") extratags: Int = 1,
        @Query("addressdetails") addressdetails: Int = 1,
        @Query("countrycodes") countrycodes: String = "de",
        @Query("bounded") bounded: Int = 1,
        @Query("viewbox") viewbox: String? = null
    ): List<NominatimResult>
}

data class NominatimResult(
    val place_id: Long,
    val lat: String,
    val lon: String,
    val display_name: String,
    val name: String? = null,
    val type: String? = null,
    @com.google.gson.annotations.SerializedName("class")
    val class_: String? = null,
    val address: NominatimAddress? = null
)

data class NominatimAddress(
    val road: String? = null,
    val house_number: String? = null,
    val postcode: String? = null,
    val city: String? = null,
    val town: String? = null,
    val village: String? = null
)
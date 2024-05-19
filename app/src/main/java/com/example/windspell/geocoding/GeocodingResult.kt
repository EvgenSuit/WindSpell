package com.example.windspell.geocoding

import com.google.gson.annotations.SerializedName

data class GeocodingResult(
    @SerializedName("name") val name: String = "",
    @SerializedName("local_names") val localNames: Map<String, String> = mapOf(),
    @SerializedName("lat") val lat: Double = 0.0,
    @SerializedName("lon") val lon: Double = 0.0
)


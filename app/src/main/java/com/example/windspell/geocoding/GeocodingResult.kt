package com.example.windspell.geocoding

import com.google.gson.annotations.SerializedName

data class GeocodingResult(
    @SerializedName("name") val name: String,
    @SerializedName("local_names") val localNames: Map<String, String>,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double
)


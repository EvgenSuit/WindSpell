package com.example.windspell.geocoding

import com.google.gson.annotations.SerializedName

data class GeocodingResult(
    @SerializedName("lat") val lat: String = "",
    @SerializedName("lon") val lon: String = ""
)

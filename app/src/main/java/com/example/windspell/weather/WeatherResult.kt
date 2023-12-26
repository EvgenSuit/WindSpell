package com.example.windspell.weather

import androidx.room.Embedded
import com.google.gson.annotations.SerializedName

data class WeatherResult(
    @SerializedName("main") var main: Main = Main(),
    @SerializedName("weather") var weather: List<Weather> = listOf(),
    @SerializedName("name") var name: String = "",
    @SerializedName("sys") var sys: Sys = Sys(),
    @SerializedName("id") var cityDd: Int = 0
)

data class Weather (
    @SerializedName("main") var main: String = "",
    @SerializedName("description") var description: String = "",
    @SerializedName("icon") var icon: String = ""
)

data class Main (
    @SerializedName("temp") var temp: Double = 0.0,
    @SerializedName("temp_min") var tempMin: Double = 0.0,
    @SerializedName("temp_max") var tempMax: Double = 0.0,
    @SerializedName("feels_like") var feelsLike: Double = 0.0,
    @SerializedName("pressure") var pressure: Double = 0.0,
)

data class Sys (
    @SerializedName("country") var country: String = "",
)
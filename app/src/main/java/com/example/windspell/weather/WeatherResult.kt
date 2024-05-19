package com.example.windspell.weather

import com.example.windspell.data.WeatherItem
import com.example.windspell.data.getTimeInUTC
import com.google.gson.annotations.SerializedName
import java.time.Instant

data class WeatherResult(
    @SerializedName("main") var main: Main = Main(),
    @SerializedName("weather") var weather: List<Weather> = listOf(),
    @SerializedName("name") var name: String = "",
    @SerializedName("sys") var sys: Sys = Sys(),
    @SerializedName("dt") var dt: Long = 0,
    @SerializedName("id") var cityId: Int = -1,
    val lastTimeUpdated: Long = 0,
    val lon: Double = 0.0,
    val lat: Double = 0.0,
    val lang: String = ""
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
    @SerializedName("sunrise") var sunrise: Long = 0,
    @SerializedName("sunset") var sunset: Long = 0
)

fun WeatherResult.weatherResultToItem(lang: String? = null): WeatherItem {
    return WeatherItem(cityName = this.name,
        main = this.main,
        weather = this.weather,
        sys = this.sys,
        cityId = this.cityId,
        forecastUnit = listOf(),
        dt = this.dt,
        lang = lang ?: this.lang,
        lastTimeUpdated = Instant.now().getTimeInUTC(),
        lon = this.lon,
        lat = this.lat)
}

fun WeatherItem.weatherItemToResult(): WeatherResult{
    return WeatherResult(
        this.main,
        this.weather,
        this.cityName,
        this.sys,
        this.dt,
        this.cityId,
        this.lastTimeUpdated,
        this.lon,
        this.lat,
        this.lang
    )
}
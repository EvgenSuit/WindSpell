package com.example.windspell.weather
import com.google.gson.annotations.SerializedName

data class ForecastResult(
    @SerializedName("list") var list: List<ForecastUnit> = listOf()
)

data class ForecastUnit(
    @SerializedName("dt") var timestamp: Long = 0,
    @SerializedName("temp") var temp: TempUnit,
    @SerializedName("weather") var weather: List<Weather>
)

data class TempUnit(
    @SerializedName("day") var day: Double = 0.0,
    @SerializedName("night") var night: Double = 0.0
)
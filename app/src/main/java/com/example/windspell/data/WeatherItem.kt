package com.example.windspell.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.windspell.weather.ForecastUnit
import com.example.windspell.weather.Main
import com.example.windspell.weather.Sys
import com.example.windspell.weather.Weather
import com.example.windspell.weather.WeatherResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity
data class WeatherItem(
    @PrimaryKey
    val cityId: Int = 0,
    val cityName: String = "",
    @Embedded
    val main: Main,
    @TypeConverters(WeatherListConverter::class)
    val weather: List<Weather>,
    val name: String,
    @Embedded
    val sys: Sys,
    @TypeConverters(ForecastListConverter::class)
    val forecastUnit: List<ForecastUnit>,
    val lang: String = "",
    val lastTimeUpdated: Long = 0
)

class WeatherListConverter {
    @TypeConverter
    fun fromJson(json: String): List<Weather> {
        val type = object : TypeToken<List<Weather>>() {}.type
        return Gson().fromJson(json, type)
    }
    @TypeConverter
    fun toJson(list: List<Weather>): String {
        val type = object : TypeToken<List<Weather>>() {}.type
        return Gson().toJson(list, type)
    }
}

class ForecastListConverter {
    @TypeConverter
    fun fromJson(json: String): List<ForecastUnit> {
        val type = object : TypeToken<List<ForecastUnit>>() {}.type
        return Gson().fromJson(json, type)
    }
    @TypeConverter
    fun toJson(list: List<ForecastUnit>): String {
        val type = object : TypeToken<List<ForecastUnit>>() {}.type
        return Gson().toJson(list, type)
    }
}

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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.time.ZoneOffset

@Entity
data class WeatherItem(
    @PrimaryKey
    val cityId: Int = 0,
    val cityName: String = "",
    @Embedded
    val main: Main = Main(),
    @TypeConverters(WeatherListConverter::class)
    val weather: List<Weather> = listOf(),
    val name: String = "",
    @Embedded
    val sys: Sys = Sys(),
    @TypeConverters(ForecastListConverter::class)
    val forecastUnit: List<ForecastUnit> = listOf(),
    val lang: String = "",
    val dt: Long = 0L,
    val lastTimeUpdated: Long = 0,
    val lon: Double = 0.0,
    val lat: Double = 0.0
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

fun Instant.getTimeInUTC(): Long {
    return Instant.ofEpochSecond(this.epochSecond).atZone(ZoneOffset.UTC).toEpochSecond()
}
fun WeatherItem.requiresAnUpdate(lang: String): Boolean = Instant.now().getTimeInUTC() - this.lastTimeUpdated >= 60*60 || this.lang != lang
fun WeatherItem.createQuery(): String = "${this.cityName}, ${this.sys.country}"

object WeatherConfig {
    const val MAX_ITEM_COUNT = 20
}

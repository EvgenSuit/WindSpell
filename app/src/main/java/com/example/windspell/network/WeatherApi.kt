package com.example.windspell.network

import com.example.windspell.Config
import com.example.windspell.geocoding.GeocodingResult
import com.example.windspell.weather.ForecastResult
import com.example.windspell.weather.WeatherResult
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query

private const val BASE_URL = "https://pro.openweathermap.org/data/2.5/"
private val weatherRetrofit = Retrofit.Builder().
        baseUrl(BASE_URL)
    .addConverterFactory(GsonConverterFactory.create())
    .build()


interface WeatherApi {
    @GET("weather")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") metric: String = "metric",
        @Query("lang") lang: String = "",
        @Query("appid") apiKey: String = Config.openWeatherApiKey
    ) : WeatherResult


    @GET("forecast/daily")
    suspend fun getForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("cnt") cnt: Int = 5,
        @Query("units") metric: String = "metric",
        @Query("appid") apiKey: String = Config.openWeatherApiKey
    ) : ForecastResult
}

object WeatherService {
    val weatherService: WeatherApi by lazy {
        weatherRetrofit.create(WeatherApi::class.java)
    }
}

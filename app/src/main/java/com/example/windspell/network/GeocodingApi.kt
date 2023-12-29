package com.example.windspell.network

import com.example.windspell.Config
import com.example.windspell.geocoding.GeocodingResult
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private const val BASE_URL = "https://api.openweathermap.org/geo/1.0/"
private val geocodingRetrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

interface GeocodingApi {
    @GET("direct")
    suspend fun geo(
        @Query("q") cityName: String,
        @Query("limit") limit: Int = 1,
        @Query("appid") apiKey: String = Config.openWeatherApiKey
    ): List<GeocodingResult>
}

object GeocodingService {
    val geocodingService by lazy {
        geocodingRetrofit.create(GeocodingApi::class.java)
    }
}

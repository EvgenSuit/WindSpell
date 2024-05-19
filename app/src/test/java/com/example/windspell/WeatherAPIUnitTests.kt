package com.example.windspell

import com.example.windspell.network.GeocodingApi
import com.example.windspell.network.WeatherService
import com.example.windspell.network.geocodingRetrofit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherAPIUnitTests {
    private var apiKey = BuildConfig.API_KEY
    private var city: String = "London"
    private var lang = "en"
    private val nonExistentCities: List<String> = listOf(
        "NONEXISTENT", "nnfdfk"
    )
    private val geocodingApi = geocodingRetrofit.create(GeocodingApi::class.java)

    @Test
    fun testCityNameMatchesCoordinates() = runTest {
        val geocodingResult = geocodingApi.geo(city, apiKey = apiKey).first()
        assertEquals(geocodingResult.lat, 51.5074456, 0.05)
        assertEquals(geocodingResult.lon, -0.1277653, 0.05)
    }

    @Test
    fun testNonExistentCity() = runTest {
        for (i in nonExistentCities) {
            val geocodingResult = geocodingApi.geo(i, apiKey = apiKey)
            assertTrue(geocodingResult.isEmpty())
        }
    }

    @Test
    fun testWeatherResultIsCorrect() = runTest {
        val geocodingResult = geocodingApi.geo(city, apiKey = apiKey).first()
        var weatherResult = WeatherService.weatherService.getWeather(geocodingResult.lat,
            geocodingResult.lon, apiKey = apiKey, lang = lang)
        assertEquals(weatherResult.name, "London")
        lang = "ru"
        weatherResult = WeatherService.weatherService.getWeather(geocodingResult.lat,
            geocodingResult.lon, apiKey = apiKey, lang = lang)
        assertEquals(weatherResult.name, "Лондон")
    }
}
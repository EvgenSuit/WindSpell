package com.example.windspell

import com.example.windspell.network.GeocodingService
import com.example.windspell.network.WeatherService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherUnitTests {
    private val apiKey = Config.openWeatherApiKey
    private var city: String = "London"
    private var lang = "en"
    private val nonExistentCities: List<String> = listOf(
        "NONEXISTENT", "nnfdfk"
    )

    @Test
    fun testCityNameMatchesCoordinates() = runTest {
        val geocodingResult = GeocodingService.geocodingService.geo(city).first()
        assertEquals(geocodingResult.lat, 51.5074456, 0.05)
        assertEquals(geocodingResult.lon, -0.1277653, 0.05)
    }

    @Test
    fun testNonExistentCity() = runTest {
        for (i in nonExistentCities) {
            val geocodingResult = GeocodingService.geocodingService.geo(i)
            assertTrue(geocodingResult.isEmpty())
        }
    }

    @Test
    fun testWeatherResultIsCorrect() = runTest {
        val geocodingResult = GeocodingService.geocodingService.geo(city).first()
        var weatherResult = WeatherService.weatherService.getWeather(geocodingResult.lat,
            geocodingResult.lon, apiKey = apiKey, lang = lang)
        assertEquals(weatherResult.name, "London")
        lang = "ru"
        weatherResult = WeatherService.weatherService.getWeather(geocodingResult.lat,
            geocodingResult.lon, apiKey = apiKey, lang = lang)
        assertEquals(weatherResult.name, "Лондон")
    }
}
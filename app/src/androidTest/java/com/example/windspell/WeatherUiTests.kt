package com.example.windspell

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.windspell.data.WeatherDao
import com.example.windspell.data.WeatherDatabase
import com.example.windspell.data.WeatherItem
import com.example.windspell.network.GeocodingService
import com.example.windspell.network.WeatherService
import com.example.windspell.weather.WeatherResult
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class WeatherUiTests {
    private val apiKey = Config.openWeatherApiKey
    private lateinit var weatherDao: WeatherDao
    private lateinit var db: WeatherDatabase
    private var city: String = "London"
    private var lang = "en"

    @Before
    fun initDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            WeatherDatabase::class.java
        ).build()
        weatherDao = db.weatherDao()
    }
    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
   fun testReplaceWeatherItem() = runTest {
            val geocodingResult = GeocodingService.geocodingService.geo(city).first()
            var weatherResult = WeatherService.weatherService.getWeather(
                geocodingResult.lat,
                geocodingResult.lon, apiKey = apiKey, lang = lang
            )

            weatherDao.insertWeatherItem(getWeatherItem(weatherResult))
            val cityId = weatherResult.cityDd
            lang = "ru"
            weatherResult = WeatherService.weatherService.getWeather(
                geocodingResult.lat,
                geocodingResult.lon, apiKey = apiKey, lang = lang
            )
            weatherDao.insertWeatherItem(getWeatherItem(weatherResult))
            launch {
                    weatherDao.getWeatherItem(weatherResult.cityDd).collectLatest {
                        assertEquals(cityId, it.cityId)
                        assertEquals("Лондон", it.name)
                        cancel()
                    }
                }
    }

    private fun getWeatherItem(weatherResult: WeatherResult): WeatherItem =
        WeatherItem(cityName = city,
            main = weatherResult.main,
            weather = weatherResult.weather,
            name = weatherResult.name,
            sys = weatherResult.sys,
            cityId = weatherResult.cityDd,
            forecastUnit = listOf(),
            dt = weatherResult.dt,
            lang = lang)
}
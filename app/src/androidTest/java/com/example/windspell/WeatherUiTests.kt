package com.example.windspell

import android.content.Context
import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertValueEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToLog
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.windspell.components.MainScreen
import com.example.windspell.data.WeatherDao
import com.example.windspell.data.WeatherDatabase
import com.example.windspell.data.WeatherItem
import com.example.windspell.network.GeocodingService
import com.example.windspell.network.WeatherService
import com.example.windspell.ui.theme.WindSpellTheme
import com.example.windspell.weather.WeatherResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.createTestCoroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
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
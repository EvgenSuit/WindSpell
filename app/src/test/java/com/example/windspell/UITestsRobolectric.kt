package com.example.windspell

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.windspell.data.DataStoreManager
import com.example.windspell.data.WeatherItem
import com.example.windspell.data.WeatherRepository
import com.example.windspell.data.createQuery
import com.example.windspell.data.getTimeInUTC
import com.example.windspell.domain.CoroutineScopeProvider
import com.example.windspell.domain.DispatchProvider
import com.example.windspell.network.WeatherApi
import com.example.windspell.presentation.MainScreen
import com.example.windspell.presentation.WeatherViewModel
import com.example.windspell.weather.ForecastResult
import com.example.windspell.weather.ForecastUnit
import com.example.windspell.weather.Sys
import com.example.windspell.weather.TempUnit
import com.example.windspell.weather.Weather
import com.example.windspell.weather.weatherItemToResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant


@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class UITestsRobolectric {
    private val timestampToDate = TimestampToDate()
    private val timestampToDayOfWeek = TimestampToDayOfWeek()
    private val dispatcher = StandardTestDispatcher()
    private val cityId = 233
    private val cityName = "London"
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var dataStoreManager: DataStoreManager
    private val weatherItem = WeatherItem(cityId = cityId,
        cityName = cityName,
        sys = Sys(country = "GB"),
        lastTimeUpdated = 0,
        lang = "en",
        weather = listOf(Weather()),
        forecastUnit = listOf(ForecastUnit(temp = TempUnit(),
            weather = listOf(Weather(main = "precipitation"))
        ))
    )

    @get:Rule
    val rule = createComposeRule()
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun mockWeatherRepository(weatherItems: List<WeatherItem>) {
        weatherRepository = mockk<WeatherRepository> {
            every { getAllWeatherItems() } returns flow { emit(weatherItems) }
            coEvery { insertWeatherItem(any<WeatherItem>()) } returns Unit
            for (item in weatherItems) {
                coEvery { deleteWeatherItem(item.cityId) } returns Unit
            }
        }
    }
    private fun mockWeatherApi(weatherItems: List<WeatherItem>): WeatherApi = mockk<WeatherApi> {
        for (i in weatherItems.indices) {
            coEvery { getWeather(any(), any(), any(), any(), any()) } returns weatherItems[i].weatherItemToResult()
            coEvery { getForecast(any(), any(), any(), any(), any()) } returns ForecastResult(weatherItems[i].forecastUnit)
        }
    }
    private fun mockDataStoreManager() {
        dataStoreManager = mockk<DataStoreManager> {
            coEvery { getRecentWeatherItemId() } returns cityId
            coEvery { showSplashScreen(any()) } returns Unit
            coEvery { editRecentWeatherItem(cityId) } returns Unit
        }
    }
    @Before
    fun init() {
        mockDataStoreManager()
    }
    @Test
    fun loadRecentItem_requiresUpdate_itemLoaded() = runTest(dispatcher) {
        val weatherItems = listOf(weatherItem)
        val geocodingApi = mockGeocodingApi(weatherItems)
        mockWeatherRepository(weatherItems)
        val weatherApi = mockWeatherApi(weatherItems)
        val coroutineScopeProvider = CoroutineScopeProvider(this)
        val dispatchProvider = DispatchProvider(dispatcher)
        val viewModel = WeatherViewModel(dataStoreManager, weatherRepository, weatherApi,
            geocodingApi, timestampToDate, timestampToDayOfWeek, coroutineScopeProvider, dispatchProvider)
        advanceUntilIdle()
        with(rule) {
            setContent {
                MainScreen(true, true, null, 0.dp, viewModel, onThemeChanged = {}) {}
            }
            for (item in weatherItems) {
                onNodeWithTag(getString(R.string.as_of)).performScrollTo().assertIsDisplayed()
                assertEquals(viewModel.uiState.value.weatherResult.cityId, item.cityId)
                assertEquals(viewModel.uiState.value.forecastResult, ForecastResult(item.forecastUnit))
                onNodeWithTag("Forecasts").assertIsDisplayed()
            }
        }
    }
    @Test
    fun loadRecentItem_doesNotRequireUpdate_itemLoaded() = runTest(dispatcher) {
        val weatherItems = listOf(weatherItem.copy(lastTimeUpdated = Instant.now().getTimeInUTC()))
        val geocodingApi = mockGeocodingApi(weatherItems)
        mockWeatherRepository(weatherItems)
        val weatherApi = mockWeatherApi(weatherItems)
        val coroutineScopeProvider = CoroutineScopeProvider(this)
        val dispatchProvider = DispatchProvider(dispatcher)
        val viewModel = WeatherViewModel(dataStoreManager, weatherRepository, weatherApi,
            geocodingApi, timestampToDate, timestampToDayOfWeek, coroutineScopeProvider, dispatchProvider)
        advanceUntilIdle()
        with(rule) {
            setContent {
                MainScreen(true, true, null, 0.dp, viewModel, onThemeChanged = {}) {}
            }
            onNodeWithTag("Content").assertIsDisplayed()
            onNodeWithTag("TopBarDrawerButton").performClick()
            for (item in weatherItems) {
                onNodeWithTag("CityDrawerItem: ${item.cityId}").performClick()
                coVerify(exactly = 0) { geocodingApi.geo(cityName = item.createQuery()) }
                assertEquals(viewModel.uiState.value.weatherResult.cityId, item.cityId)
                assertEquals(viewModel.uiState.value.forecastResult, ForecastResult(item.forecastUnit))
                onNodeWithTag("Forecasts").performScrollTo().assertIsDisplayed()
            }
        }
    }
    @Test
    fun onSearchTextInput_weatherLoaded() = runTest(dispatcher) {
        val weatherItems = listOf(weatherItem)
        val geocodingApi = mockGeocodingApi(weatherItems)
        mockWeatherRepository(listOf())
        val weatherApi = mockWeatherApi(weatherItems)
        val coroutineScopeProvider = CoroutineScopeProvider(this)
        val dispatchProvider = DispatchProvider(dispatcher)
        val viewModel = WeatherViewModel(dataStoreManager, weatherRepository, weatherApi,
            geocodingApi, timestampToDate, timestampToDayOfWeek, coroutineScopeProvider, dispatchProvider)
        advanceUntilIdle()
        with(rule) {
            setContent {
                MainScreen(true, true, null, 0.dp, viewModel, onThemeChanged = {}) {}
            }
            for (item in weatherItems) {
                onNodeWithTag("SearchBar").performTextInput(item.createQuery())
                advanceUntilIdle()
                assertEquals(viewModel.uiState.value.weatherResult.cityId, cityId)
            }
        }
    }

    @Test
    fun onSearchTextInput_itemIsNotSavedAddButtonTapped_itemSaved() = runTest(dispatcher) {
        val weatherItems = listOf(weatherItem)
        val geocodingApi = mockGeocodingApi(weatherItems)
        mockWeatherRepository(listOf())
        val weatherApi = mockWeatherApi(weatherItems)
        val coroutineScopeProvider = CoroutineScopeProvider(this)
        val dispatchProvider = DispatchProvider(dispatcher)
        val viewModel = WeatherViewModel(dataStoreManager, weatherRepository, weatherApi,
            geocodingApi, timestampToDate, timestampToDayOfWeek, coroutineScopeProvider, dispatchProvider)
        advanceUntilIdle()
        with(rule) {
            setContent {
                MainScreen(true, true, null, 0.dp, viewModel, onThemeChanged = {}) {}
            }
            for (item in weatherItems) {
                onNodeWithTag("SearchBar").performTextInput(item.createQuery())
                advanceUntilIdle()
                assertEquals(viewModel.uiState.value.weatherResult.cityId, cityId)
                onNodeWithTag("Add item").performClick()
                advanceUntilIdle()
                coVerify { weatherRepository.insertWeatherItem(item.copy(lastTimeUpdated = Instant.now().getTimeInUTC())) }
            }
        }
    }

}
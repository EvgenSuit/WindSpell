package com.example.windspell

import com.example.windspell.data.DataStoreManager
import com.example.windspell.data.WeatherItem
import com.example.windspell.data.WeatherRepository
import com.example.windspell.data.createQuery
import com.example.windspell.data.getTimeInUTC
import com.example.windspell.domain.CoroutineScopeProvider
import com.example.windspell.domain.DispatchProvider
import com.example.windspell.geocoding.GeocodingResult
import com.example.windspell.network.GeocodingApi
import com.example.windspell.network.WeatherApi
import com.example.windspell.presentation.WeatherViewModel
import com.example.windspell.weather.ForecastResult
import com.example.windspell.weather.Sys
import com.example.windspell.weather.weatherItemToResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant


@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelUnitTests {
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
        lang = "en")

    @Before
    fun init() {
        mockWeatherRepository(listOf(weatherItem))
        mockDataStoreManager()
    }
    private fun mockWeatherRepository(weatherItems: List<WeatherItem>) {
        weatherRepository = mockk<WeatherRepository> {
            every { getAllWeatherItems() } returns flow { emit(weatherItems) }
            coEvery { insertWeatherItem(any<WeatherItem>()) } returns Unit
            for (item in weatherItems) {
                coEvery { deleteWeatherItem(item.cityId) } returns Unit
            }
        }
    }
    private fun mockDataStoreManager() {
        dataStoreManager = mockk<DataStoreManager> {
            coEvery { getRecentWeatherItemId() } returns cityId
            coEvery { showSplashScreen(any()) } returns Unit
            coEvery { editRecentWeatherItem(cityId) } returns Unit
        }
    }
    private fun mockWeatherApi(weatherItems: List<WeatherItem>): WeatherApi = mockk<WeatherApi> {
        for (item in weatherItems) {
            coEvery { getWeather(any(), any(), any(), any(), any()) } returns item.weatherItemToResult()
            coEvery { getForecast(any(), any(), any(), any(), any()) } returns ForecastResult()
        }
    }

    @Test
    fun loadRecentItem_isNotEmptyNoUpdateRequired_itemLoaded() = runTest {
        val weatherItems = listOf(WeatherItem(cityId = cityId,
            cityName = cityName,
            lastTimeUpdated = Instant.now().getTimeInUTC(),
            lang = "en"))
        mockWeatherRepository(weatherItems)
        val weatherApi = mockWeatherApi(weatherItems)
        val coroutineScopeProvider = CoroutineScopeProvider(this)
        val dispatchProvider = DispatchProvider(UnconfinedTestDispatcher())
        val geocodingApi = mockGeocodingApi(weatherItems)
        val viewModel = WeatherViewModel(dataStoreManager, weatherRepository, weatherApi, geocodingApi,
            timestampToDate, timestampToDayOfWeek, coroutineScopeProvider, dispatchProvider)
        advanceUntilIdle()
        coVerify { weatherRepository.getAllWeatherItems() }
        coVerify { dataStoreManager.getRecentWeatherItemId() }
        for (item in weatherItems) {
            coVerify(exactly = 0) { geocodingApi.geo(cityName = item.createQuery()) }
        }
        assertEquals(viewModel.uiState.value.weatherResult.cityId, cityId)
    }
    @Test
    fun loadRecentItem_isNotEmptyUpdateRequired_itemLoaded() = runTest(dispatcher) {
        val weatherItems = listOf(weatherItem)
        val query = weatherItem.createQuery()
        val geocodingApi = mockGeocodingApi(weatherItems)
        mockWeatherRepository(weatherItems)
        val weatherApi = mockWeatherApi(weatherItems)
        val coroutineScopeProvider = CoroutineScopeProvider(this)
        val dispatchProvider = DispatchProvider(dispatcher)
        val viewModel = WeatherViewModel(dataStoreManager, weatherRepository, weatherApi,
            geocodingApi, timestampToDate, timestampToDayOfWeek, coroutineScopeProvider, dispatchProvider)
        advanceUntilIdle()
        coVerify { weatherRepository.getAllWeatherItems() }
        coVerify { dataStoreManager.getRecentWeatherItemId() }
        coVerify(exactly = 0) { geocodingApi.geo(cityName = query) }
        coVerify { weatherApi.getWeather(weatherItem.lat, weatherItem.lon, lang = weatherItem.lang) }
        assertTrue(approximatelyEqual(Instant.now().getTimeInUTC(), viewModel.uiState.value.weatherResult.lastTimeUpdated, 5))
        assertEquals(viewModel.uiState.value.weatherResult.cityId, cityId)
    }

    //assume no update is required
    @Test
    fun deleteWeatherItem_isItemNotLast_itemDeleted() = runTest(dispatcher) {
        val weatherItems = List(5) { WeatherItem(cityId = it,
            lang = "en",
            lastTimeUpdated = Instant.now().getTimeInUTC()) }
        mockWeatherRepository(weatherItems)
        dataStoreManager = mockk<DataStoreManager> {
            coEvery { getRecentWeatherItemId() } returns weatherItems.first().cityId
            coEvery { showSplashScreen(any()) } returns Unit
            for (item in weatherItems) {
                coEvery { editRecentWeatherItem(item.cityId) } returns Unit
        }
        }
        val coroutineScopeProvider = CoroutineScopeProvider(this)
        val dispatchProvider = DispatchProvider(dispatcher)
        val viewModel = WeatherViewModel(dataStoreManager, weatherRepository, mockk<WeatherApi>(),
            mockk<GeocodingApi>(), timestampToDate, timestampToDayOfWeek, coroutineScopeProvider, dispatchProvider)
        viewModel.deleteWeatherItem(0)
        advanceUntilIdle()
        // verify that if the current item is deleted the index of new item is the next
        coVerify { weatherRepository.deleteWeatherItem(0) }
        coVerify { dataStoreManager.editRecentWeatherItem(1) }
        assertEquals(viewModel.uiState.value.weatherResult.cityId, weatherItems[1].cityId)

        viewModel.deleteWeatherItem(4)
        advanceUntilIdle()
        // verify that deleting item that is not current doesn't move its index
        coVerify { weatherRepository.deleteWeatherItem(4) }
        coVerify(exactly = 0) { dataStoreManager.editRecentWeatherItem(3) }
        assertEquals(viewModel.uiState.value.weatherResult.cityId, weatherItems[1].cityId)
    }

    @Test
    fun deleteWeatherItem_isItemLast_itemDeletedResultSetToEmpty() = runTest(dispatcher) {
        val weatherItems = List(1) { WeatherItem(cityId = it,
            lang = "en",
            lastTimeUpdated = Instant.now().epochSecond) }
        mockWeatherRepository(weatherItems)
        dataStoreManager = mockk<DataStoreManager> {
            coEvery { getRecentWeatherItemId() } returns weatherItems.first().cityId
            coEvery { showSplashScreen(any()) } returns Unit
            coEvery { editRecentWeatherItem(any()) } returns Unit
        }
        val coroutineScopeProvider = CoroutineScopeProvider(this)
        val dispatchProvider = DispatchProvider(dispatcher)
        val viewModel = WeatherViewModel(dataStoreManager, weatherRepository, mockk<WeatherApi>(),
            mockk<GeocodingApi>(), timestampToDate, timestampToDayOfWeek, coroutineScopeProvider, dispatchProvider)
        viewModel.deleteWeatherItem(0)
        advanceUntilIdle()
        coVerify { weatherRepository.deleteWeatherItem(0) }
        coVerify(exactly = 0) { dataStoreManager.editRecentWeatherItem(1) }
        assertEquals(viewModel.uiState.value.weatherResult.cityId, -1)
    }
}
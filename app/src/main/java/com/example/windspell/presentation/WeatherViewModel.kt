package com.example.windspell.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.windspell.TimestampToDate
import com.example.windspell.TimestampToDayOfWeek
import com.example.windspell.data.DataStoreManager
import com.example.windspell.data.WeatherConfig
import com.example.windspell.data.WeatherItem
import com.example.windspell.data.WeatherRepository
import com.example.windspell.data.getTimeInUTC
import com.example.windspell.data.requiresAnUpdate
import com.example.windspell.domain.CoroutineScopeProvider
import com.example.windspell.domain.DispatchProvider
import com.example.windspell.domain.Result
import com.example.windspell.network.GeocodingApi
import com.example.windspell.network.WeatherApi
import com.example.windspell.weather.ForecastResult
import com.example.windspell.weather.WeatherResult
import com.example.windspell.weather.weatherItemToResult
import com.example.windspell.weather.weatherResultToItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Locale
import javax.inject.Inject


@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val weatherRepository: WeatherRepository,
    private val weatherApi: WeatherApi,
    private val geocodingApi: GeocodingApi,
    private val timestampToDateUseCase: TimestampToDate,
    private val timestampToDayOfWeekUseCase: TimestampToDayOfWeek,
    coroutineScopeProvider: CoroutineScopeProvider,
    dispatcherProvider: DispatchProvider) : ViewModel() {
    private val coroutineScope = coroutineScopeProvider.provideCoroutineScope() ?: viewModelScope
    private val dispatcher = dispatcherProvider.provideDispatcher()
    private var textInputJob: Job = Job()
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    val weatherFetchResultFlow = _uiState.map { it.weatherFetchResult }

    init {
        coroutineScope.launch {
            updateWeatherFetchResult(Result.InProgress)
            withContext(dispatcher) {
                _uiState.update { it.copy(weatherItems = weatherRepository.getAllWeatherItems().first()) }
                loadRecentWeatherItem()
                dataStoreManager.showSplashScreen(false)
                weatherRepository.getAllWeatherItems().collectLatest { result ->
                    val sorted = result.sortedByDescending { it.lastTimeUpdated }
                    if (sorted.size == WeatherConfig.MAX_ITEM_COUNT) {
                        weatherRepository.deleteWeatherItem(sorted.last().cityId)
                    }
                    //append country code if a city with the same name is already present
                    _uiState.update { it.copy(weatherItems = sorted.map {item ->
                        if (sorted.count { it.cityName == item.cityName } > 1)
                            item.copy(cityName = "${item.cityName}, ${item.sys.country}")
                    else item}) }
                }
            }
        }
    }

    fun onSearchTextChanged(city: String) {
        if (textInputJob.isActive && !textInputJob.isCompleted) {
            textInputJob.cancel()
        }
        textInputJob = coroutineScope.launch {
            withContext(dispatcher) {
                delay(300L)
                val items = _uiState.value.weatherItems
                val recentWeatherItemId = dataStoreManager.getRecentWeatherItemId()
                //load recent weather item on blank search text
                if (city.isBlank()) {
                    if (items.any { it.cityId == recentWeatherItemId }) {
                        val item = items.first { it.cityId == recentWeatherItemId }
                        manageWeatherItem(item, lon = item.lon, lat = item.lat)
                    } else updateWeatherFetchResult(Result.Empty)
                    return@withContext
                }
                try {
                    val existentItem = items.first { it.cityName.contains(city, ignoreCase = true) }
                    manageWeatherItem(existentItem, lon = existentItem.lon, lat = existentItem.lat)
                } catch (e: NoSuchElementException) {
                    if (_uiState.value.networkIsOn) {
                        getWeatherByCityName(city, false)
                    }
                }
            }
        }
    }
    fun onCityConfirmed() = coroutineScope.launch {
        insertWeatherItem(_uiState.value.weatherResult,
            _uiState.value.forecastResult)
        updateWeatherResult(_uiState.value.weatherResult)
        updateForecastResult(_uiState.value.forecastResult)
        }

    private suspend fun loadRecentWeatherItem() {
        val recentWeatherItemId = dataStoreManager.getRecentWeatherItemId()
        val allWeatherItems = _uiState.value.weatherItems
        if (allWeatherItems.isEmpty()) updateWeatherFetchResult(Result.Empty)
        for (weatherItem in allWeatherItems){
            val isRecentItem = recentWeatherItemId != null
                    && weatherItem.cityId == recentWeatherItemId
            if (isRecentItem) {
                manageWeatherItem(weatherItem, lon = weatherItem.lon, lat = weatherItem.lat)
            }
        }
    }

    suspend fun manageWeatherItem(weatherItem: WeatherItem = WeatherItem(),
                                  lat: Double = 0.0,
                                  lon: Double = 0.0) {
        val requiresAnUpdate = weatherItem.requiresAnUpdate(_uiState.value.lang)
        /* If the difference between the time of the last weather update and current time is bigger than or equal to an hour
           or the language setting has changed, fetch up-to-date weather data. Otherwise fetch data from the DB */
        if (!requiresAnUpdate) {
            updateWeatherResult(weatherItem.weatherItemToResult(), true)
            updateForecastResult(_uiState.value.forecastResult.copy(weatherItem.forecastUnit))
            updateWeatherFetchResult(Result.Success)
        } else {
            if (_uiState.value.networkIsOn) {
                getWeatherByCoordinates(lat, lon, true)
            }
        }
    }

    private suspend fun getWeatherByCityName(city: String, insert: Boolean) {
        try {
            updateWeatherFetchResult(Result.InProgress)
            val geocodingListResult = geocodingApi.geo(city)
            if (geocodingListResult.isEmpty()) {
                updateWeatherFetchResult(Result.Empty)
                return
            }
            val geocodingResult = geocodingListResult.first()
            val lat = geocodingResult.lat
            val lon = geocodingResult.lon
            getWeatherByCoordinates(lat, lon, insert)
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    updateWeatherFetchResult(Result.Error(e.message!!))
                }
            }
    }

    private suspend fun getWeatherByCoordinates(lat: Double, lon: Double, insert: Boolean) {
        var weatherResult = weatherApi.getWeather(lat, lon, lang = _uiState.value.lang)
        weatherResult = weatherResult.copy(lon = lon, lat = lat,
            lang = _uiState.value.lang,
            lastTimeUpdated = Instant.now().getTimeInUTC())
        val forecastResult = weatherApi.getForecast(lat, lon)
        updateWeatherResult(weatherResult, insert)
        updateForecastResult(forecastResult)
        if (insert) {
            insertWeatherItem(weatherResult, forecastResult)
        }
        updateWeatherFetchResult(Result.Success)
    }

    fun onLocaleChange(locale: Locale) {
        coroutineScope.launch {
            withContext(dispatcher) {
                _uiState.update { it.copy(lang = locale.language) }
                val newItem = _uiState.value.weatherResult.weatherResultToItem()
                manageWeatherItem(newItem, newItem.lat, newItem.lon)
            }
        }
    }

    private suspend fun insertWeatherItem(weatherResult: WeatherResult, forecastResult: ForecastResult){
        val newWeatherItem = weatherResult.weatherResultToItem(_uiState.value.lang).copy(forecastUnit = forecastResult.list)
        weatherRepository.insertWeatherItem(newWeatherItem)
    }
    private suspend fun editRecentWeatherItem(id: Int) {
        dataStoreManager.editRecentWeatherItem(id)
    }
    private suspend fun updateWeatherResult(weatherResult: WeatherResult, editRecentItem: Boolean = true) {
        _uiState.update { it.copy(weatherResult = weatherResult) }
        if (editRecentItem) {
            editRecentWeatherItem(weatherResult.cityId)
        }
    }
    private fun updateForecastResult(forecastResult: ForecastResult) {
        _uiState.update { it.copy(forecastResult = forecastResult) }
    }
    private fun updateWeatherFetchResult(result: Result) {
        _uiState.update { it.copy(weatherFetchResult = result) }
    }

    fun deleteWeatherItem(id: Int) = coroutineScope.launch {
        val items = _uiState.value.weatherItems
        // move index of currently viewing item only if deleting that item
        val currIndex = try {
            items.indexOf(items.first { it.cityId == id && _uiState.value.weatherResult.cityId == id })
        } catch (e: NoSuchElementException) {
            weatherRepository.deleteWeatherItem(id)
            -1
        }
        if (currIndex == -1) return@launch
        var newIndex = -1
        weatherRepository.deleteWeatherItem(id)
        if (currIndex + 1 < items.size) newIndex = currIndex + 1
        else if (currIndex - 1 >= 0) newIndex = currIndex - 1
        if (newIndex != -1) {
            val newItem = items[newIndex]
            manageWeatherItem(newItem)
            editRecentWeatherItem(newItem.cityId)
        } else {
            updateWeatherFetchResult(Result.Empty)
            updateWeatherResult(WeatherResult())
        }
    }
    fun timestampToDate(timestamp: Long): String = timestampToDateUseCase(timestamp)
    fun timestampToDayOfWeek(timestamp: Long): Int = timestampToDayOfWeekUseCase(timestamp)
    fun updateNetworkState(networkIsOn: Boolean) = _uiState.update { it.copy(networkIsOn = networkIsOn) }

    data class UiState(
        val weatherResult: WeatherResult = WeatherResult(),
        val weatherItems: List<WeatherItem> = listOf(),
        val forecastResult: ForecastResult = ForecastResult(),
        val lang: String = Locale.getDefault().language,
        val weatherFetchResult: Result = Result.Idle,
        val networkIsOn: Boolean = true,
        val isCurrentItemSaved: Boolean = true)
}

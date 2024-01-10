package com.example.windspell

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.windspell.components.weatherDataStore
import com.example.windspell.data.WeatherDatabase
import com.example.windspell.data.WeatherItem
import com.example.windspell.data.WeatherRepository
import com.example.windspell.network.GeocodingService
import com.example.windspell.network.WeatherService
import com.example.windspell.weather.ForecastResult
import com.example.windspell.weather.ForecastUnit
import com.example.windspell.weather.WeatherResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import javax.inject.Inject

val recentWeatherItemPrefs = intPreferencesKey(name = "recentWeatherItem")

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val recentWeatherItemDatastore: DataStore<Preferences>,
    private val weatherRepository: WeatherRepository,
) : ViewModel() {

    private val _weatherResult: MutableStateFlow<WeatherResult> = MutableStateFlow(WeatherResult())
    val weatherResult = _weatherResult.asStateFlow()
    private val _forecastResult: MutableStateFlow<ForecastResult> =
        MutableStateFlow(ForecastResult())
    val forecastResult = _forecastResult.asStateFlow()
    private val _weatherItems: MutableStateFlow<List<WeatherItem>> = MutableStateFlow(mutableListOf())
    val weatherItems = _weatherItems.asStateFlow()
    private val defaultLocale = Locale.getDefault()
    val lang: String = defaultLocale.language
    var defaultCityLoaded = mutableStateOf(false)
    private var error: Exception = Exception()

    /*companion object {
        val Factory = viewModelFactory {
            initializer {
                val context = (this[APPLICATION_KEY] as WeatherApplication).context
                val dao = WeatherDatabase.getWeatherDatabase(context).weatherDao()
                WeatherViewModel(context.weatherDataStore, WeatherRepository(dao))
            }
        }
    }*/

    init {
            viewModelScope.launch {
                loadRecentWeatherItem()
                weatherRepository.getAllWeatherItems().collect { result ->
                    _weatherItems.update { result }
                }
            }
    }

    fun timestampToDate(timestamp: Long, includeMMddyy: Boolean = true): String {
        val pattern = if (includeMMddyy) "MM/dd/yyyy HH:mm:ss" else "HH:mm:ss"
        val sdf = SimpleDateFormat(pattern, defaultLocale)
        return sdf.format(Date(timestamp * 1000))
    }

    private suspend fun loadRecentWeatherItem() {
        val recentWeatherItemId = recentWeatherItemDatastore.data.first()[recentWeatherItemPrefs]
        for (weatherItem in weatherRepository.getAllWeatherItems().first()){
           val requiresUpdate = Instant.now().epochSecond - weatherItem.lastTimeUpdated >= 60 * 60
            val isRecentItem = !defaultCityLoaded.value && recentWeatherItemId != null
                    && weatherItem.cityId == recentWeatherItemId
            val requestValue = "${weatherItem.cityName}, ${weatherItem.sys.country}"
            viewModelScope.launch {
                if (weatherItem.lang != lang || requiresUpdate) {
                    if (isRecentItem) {
                        getWeather(requestValue, true, updateState = true, weatherItem.cityId)
                        defaultCityLoaded.value = true
                    } else {
                        getWeather(requestValue, true, updateState = false, weatherItem.cityId)
                    }
                }
                else if(isRecentItem) {
                    _weatherResult.update { res -> res.weatherItemToResult(weatherItem) }
                    _forecastResult.update { res -> res.copy(weatherItem.forecastUnit) }
                    defaultCityLoaded.value = true
                }
            }
        }
    }

    fun insertWeatherItem(city: String, weatherResult: WeatherResult, forecastResult: ForecastResult){
        val newWeatherItem = weatherResult.weatherResultToItem(city).copy(forecastUnit = forecastResult.list)
        viewModelScope.launch {
                weatherRepository.insertWeatherItem(newWeatherItem)
        }
    }

    suspend fun getWeather(city:String, insert: Boolean, updateState: Boolean = true, cityId: Int = -1) {
        try {
                val geocodingResult = GeocodingService.geocodingService.geo(city).first()
                val lat = geocodingResult.lat
                val lon = geocodingResult.lon
                var weatherResult = WeatherService.weatherService.getWeather(
                    lat, lon, lang = lang
                )
                weatherResult = weatherResult.copy(name = geocodingResult.localNames[lang] ?: weatherResult.name)
                val forecastResult = WeatherService.weatherService.getForecast(lat, lon)

                if (cityId != -1) {
                    deleteWeatherItem(cityId)
                }
                if(updateState) {
                    _weatherResult.update {weatherResult}
                    _forecastResult.update {forecastResult}
                }
                if (insert) {
                    insertWeatherItem(weatherResult.name, weatherResult, forecastResult)
                }
                defaultCityLoaded.value = false
            } catch (e: Exception) {
                error = e
            }
    }
    fun updateWeatherItem(weatherItem: WeatherItem, editRecentItem: Boolean = true) {
        _weatherResult.update {
               weatherResult -> weatherResult.weatherItemToResult(weatherItem)
        }
        if (editRecentItem) {
            editRecentWeatherItem(_weatherResult.value.cityDd)
        }
    }

    private fun WeatherResult.weatherResultToItem(city: String): WeatherItem {
        return WeatherItem(cityName = city,
            main = this.main,
            weather = this.weather,
            name = this.name,
            sys = this.sys,
            cityId = this.cityDd,
            forecastUnit = listOf(),
            dt = this.dt,
            lang = lang,
            lastTimeUpdated = Instant.now().epochSecond)
    }

    private fun WeatherResult.weatherItemToResult(weatherItem: WeatherItem) :WeatherResult{
        return this.copy(
            weatherItem.main,
            weatherItem.weather,
            weatherItem.cityName,
            weatherItem.sys,
            weatherItem.dt,
            weatherItem.cityId,
        )
    }

    private fun editRecentWeatherItem(id: Int) {
        viewModelScope.launch {
            recentWeatherItemDatastore.edit {
                it[recentWeatherItemPrefs] = id
            }
        }
    }

    fun updateWeatherResult(weatherResult: WeatherResult) {
        _weatherResult.update { weatherResult }
        editRecentWeatherItem(weatherResult.cityDd)
    }
    fun updateForecastResult(forecastUnit: List<ForecastUnit>) {
        _forecastResult.update {
                forecastResult -> forecastResult.copy(forecastUnit)
        }
    }

    fun deleteWeatherItem(id: Int) = viewModelScope.launch{
        weatherRepository.deleteWeatherItem(id)
    }
}
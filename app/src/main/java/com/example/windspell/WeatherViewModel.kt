package com.example.windspell

import android.app.Application
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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import javax.inject.Inject

val recentWeatherItemPrefs = intPreferencesKey(name = "recentWeatherItem")

class WeatherViewModel(
    private val recentWeatherItemDatastore: DataStore<Preferences>,
    private val weatherRepository: WeatherRepository) : ViewModel() {

    private val _weatherResult: MutableStateFlow<WeatherResult> = MutableStateFlow(WeatherResult())
    val weatherResult = _weatherResult.asStateFlow()
    private val _forecastResult: MutableStateFlow<ForecastResult> =
        MutableStateFlow(ForecastResult())
    val forecastResult = _forecastResult.asStateFlow()
    private val _weatherItems: MutableStateFlow<List<WeatherItem>> = MutableStateFlow(mutableListOf())
    val weatherItems = _weatherItems.asStateFlow()
    val lang: String = Locale.getDefault().language
    var defaultCityLoaded = mutableStateOf(false)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val context = (this[APPLICATION_KEY] as WeatherApplication).context
                val dao = WeatherDatabase.getWeatherDatabase(context).weatherDao()
                WeatherViewModel(context.weatherDataStore, WeatherRepository(dao))
            }
        }
    }

    init {
            viewModelScope.launch {
                loadRecentWeatherItem()
                weatherRepository.getAllWeatherItems().collect { result ->
                    _weatherItems.update { result }
                }
            }
    }

    fun timestampToDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000))
    }

    private suspend fun loadRecentWeatherItem() {
        val recentWeatherItemId = recentWeatherItemDatastore.data.first()[recentWeatherItemPrefs]
        for (weatherItem in weatherRepository.getAllWeatherItems().first()){
            if (!defaultCityLoaded.value && recentWeatherItemId != null
                && weatherItem.cityId == recentWeatherItemId) {
                    _weatherResult.update { item ->
                        item.weatherItemToResult(weatherItem)
                    }
                    _forecastResult.update { forecastResult -> forecastResult.copy(weatherItem.forecastUnit) }
                    defaultCityLoaded.value = true
            }
            if (weatherItem.lang != lang) {
                getWeather(weatherItem.cityName, true)
                updateWeatherItem(weatherItem)
            }
        }
    }

    fun insertWeatherItem(city: String, weatherResult: WeatherResult, forecastResult: ForecastResult){
        val newWeatherItem = WeatherItem(cityName = city,
            main = weatherResult.main,
            weather = weatherResult.weather,
            name = weatherResult.name,
            sys = weatherResult.sys,
            cityId = weatherResult.cityDd,
            forecastUnit = forecastResult.list,
            dt = weatherResult.dt,
            lang = lang,
            lastTimeUpdated = Instant.now().epochSecond)
        viewModelScope.launch {
                weatherRepository.insertWeatherItem(newWeatherItem)
        }
    }

    suspend fun getWeather(city:String, insert: Boolean) {
        defaultCityLoaded.value = false
        try {
                val geocodingResult = GeocodingService.geocodingService.geo(city).first()
                val lat = geocodingResult.lat
                val lon = geocodingResult.lon
                val weatherResult = WeatherService.weatherService.getWeather(
                    lat, lon, lang = lang
                ).copy(name = geocodingResult.localNames[lang] ?: "")
                _weatherResult.update {weatherResult}
                val forecastResult = WeatherService.weatherService.getForecast(lat, lon)
                _forecastResult.update {forecastResult}
                if (insert) {
                    insertWeatherItem(weatherResult.name, weatherResult, forecastResult)
                }
            } catch (e: Exception) {
                Log.d("Error", e.toString())
            }
    }
    fun updateWeatherItem(weatherItem: WeatherItem) {
        _weatherResult.update {
               weatherResult -> weatherResult.weatherItemToResult(weatherItem)
        }
        editRecentWeatherItem(_weatherResult.value.cityDd)
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

    fun deleteWeatherItem(city: String) = viewModelScope.launch{
        weatherRepository.deleteWeatherItem(cityName = city)
        _weatherResult.update {
            WeatherResult()
        }
    }
}
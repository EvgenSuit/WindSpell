package com.example.windspell.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

val Context.weatherDataStore: DataStore<Preferences> by preferencesDataStore("weatherDataStore")
val recentWeatherItemKey = intPreferencesKey(name = "recentWeatherItem")
val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme")
val darkThemeKey = booleanPreferencesKey(name = "useDarkTheme")
val Context.splashScreenDataStore: DataStore<Preferences> by preferencesDataStore("splashScreen")
val showSplashScreenKey = booleanPreferencesKey("show")
class DataStoreManager(
    private val weatherDataStore: DataStore<Preferences>,
    private val themeDataStore: DataStore<Preferences>,
    private val splashScreenDataStore: DataStore<Preferences>
) {
    suspend fun showSplashScreen(show: Boolean) {
        splashScreenDataStore.edit {
            it[showSplashScreenKey] = show
        }
    }
    suspend fun collectShowSplashScreen(onData: (Boolean) -> Unit) {
        splashScreenDataStore.data.collectLatest { onData(it[showSplashScreenKey] ?: true) }
    }
    suspend fun getRecentWeatherItemId(): Int? {
        return weatherDataStore.data.first()[recentWeatherItemKey]
    }
    suspend fun editRecentWeatherItem(id: Int) {
        weatherDataStore.edit {
            it[recentWeatherItemKey] = id
        }
    }
    suspend fun collectTheme(onData: (Boolean) -> Unit) {
        themeDataStore.data.collectLatest {
            onData(it[darkThemeKey] ?: false)
        }
    }
    suspend fun changeTheme(isDark: Boolean) {
        themeDataStore.edit {
            it[darkThemeKey] = isDark
        }
    }
}
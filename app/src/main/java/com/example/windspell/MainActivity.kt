package com.example.windspell

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.windspell.components.MainScreen
import com.example.windspell.network.ConnectionState
import com.example.windspell.network.connectivityState
import com.example.windspell.ui.theme.WindSpellTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme")
val darkThemePrefs = booleanPreferencesKey(name = "useDarkTheme")

enum class SupportedLanguages{
    en,
    ru,
    be,
    pl
}
val supportedLanguages = listOf(
    SupportedLanguages.en.name,
    SupportedLanguages.ru.name,
    SupportedLanguages.be.name,
    SupportedLanguages.pl.name)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private fun Context.setAppLocale(): Context {
        val config = resources.configuration
        var locale = Locale.getDefault()
        if (!supportedLanguages.contains(locale.language)) {
            locale = Locale(SupportedLanguages.en.name)
        }
        Locale.setDefault(locale)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return createConfigurationContext(config)
    }
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.setAppLocale())
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val coroutineScope = rememberCoroutineScope()
            var darkTheme by rememberSaveable {
               mutableStateOf(runBlocking {
                   applicationContext.dataStore.data.first()[darkThemePrefs] ?: true
               })
            }
            val connection by connectivityState()
            WindSpellTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(darkTheme = darkTheme, networkIsOn = connection == ConnectionState.Available) {
                        coroutineScope.launch {
                            applicationContext.dataStore.edit {
                                val currentThemeColor = it[darkThemePrefs] ?: true
                                it[darkThemePrefs] = !currentThemeColor
                                darkTheme = !currentThemeColor
                            }
                        }
                    }
                }
            }
        }
    }
}

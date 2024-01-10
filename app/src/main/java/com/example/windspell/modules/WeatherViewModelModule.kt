package com.example.windspell.modules

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.windspell.data.WeatherDao
import com.example.windspell.data.WeatherDatabase
import com.example.windspell.data.WeatherRepository
import com.example.windspell.dataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext


@Module
@InstallIn(ViewModelComponent::class)
object WeatherViewModelModule {
    @Provides
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    fun provideWeatherDao(@ApplicationContext context: Context): WeatherDao {
        return WeatherDatabase.getWeatherDatabase(context).weatherDao()
    }
    @Provides
    fun provideWeatherRepository(dao: WeatherDao): WeatherRepository {
        return WeatherRepository(dao)
    }
}
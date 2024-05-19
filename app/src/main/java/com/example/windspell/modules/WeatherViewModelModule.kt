package com.example.windspell.modules

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.windspell.TimestampToDate
import com.example.windspell.TimestampToDayOfWeek
import com.example.windspell.data.DataStoreManager
import com.example.windspell.data.WeatherDao
import com.example.windspell.data.WeatherDatabase
import com.example.windspell.data.WeatherRepository
import com.example.windspell.data.splashScreenDataStore
import com.example.windspell.data.themeDataStore
import com.example.windspell.data.weatherDataStore
import com.example.windspell.domain.CoroutineScopeProvider
import com.example.windspell.domain.DispatchProvider
import com.example.windspell.network.GeocodingApi
import com.example.windspell.network.WeatherApi
import com.example.windspell.network.geocodingRetrofit
import com.example.windspell.network.weatherRetrofit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(ActivityRetainedComponent::class)
object WeatherViewModelModule {
    @Provides
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager{
        return DataStoreManager(context.weatherDataStore,
            context.themeDataStore,
            context.splashScreenDataStore)
    }

    @Provides
    fun provideTimestampToDateUseCase(): TimestampToDate = TimestampToDate()
    @Provides
    fun provideTimestampToDayOfWeekUseCase(): TimestampToDayOfWeek = TimestampToDayOfWeek()

    @Provides
    fun provideWeatherDao(@ApplicationContext context: Context): WeatherDao {
        return WeatherDatabase.getWeatherDatabase(context).weatherDao()
    }
    @Provides
    fun provideWeatherRepository(dao: WeatherDao): WeatherRepository {
        return WeatherRepository(dao)
    }
    @Provides
    fun provideCoroutineScopeProvider(): CoroutineScopeProvider = CoroutineScopeProvider()
    @Provides
    fun provideDispatchProvider(): DispatchProvider = DispatchProvider()
    @Provides
    fun provideGeocodingApi(): GeocodingApi = geocodingRetrofit.create(GeocodingApi::class.java)
    @Provides
    fun provideWeatherApi(): WeatherApi = weatherRetrofit.create(WeatherApi::class.java)
}
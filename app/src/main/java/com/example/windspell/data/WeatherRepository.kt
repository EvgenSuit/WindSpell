package com.example.windspell.data

import kotlinx.coroutines.flow.Flow


open class WeatherRepository(private val weatherDao: WeatherDao) {
     suspend fun insertWeatherItem(weatherItem: WeatherItem) = weatherDao.insertWeatherItem(weatherItem)
     suspend fun deleteWeatherItem(id: Int) = weatherDao.deleteWeatherItem(id)
     fun getAllWeatherItems(): Flow<List<WeatherItem>> = weatherDao.getAllWeatherItems()
}
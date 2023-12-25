package com.example.windspell.data

import kotlinx.coroutines.flow.Flow


class WeatherRepository(private val weatherDao: WeatherDao):WeatherDao {
    override suspend fun insertWeatherItem(weatherItem: WeatherItem) = weatherDao.insertWeatherItem(weatherItem)
    override suspend fun deleteWeatherItem(cityName: String) = weatherDao.deleteWeatherItem(cityName)
    override fun getWeatherItem(id: Int): Flow<WeatherItem> = weatherDao.getWeatherItem(id)
    override fun getAllWeatherItems(): Flow<List<WeatherItem>> = weatherDao.getAllWeatherItems()
}
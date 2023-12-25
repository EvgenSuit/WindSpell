package com.example.windspell.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    @Insert
    suspend fun insertWeatherItem(weatherItem: WeatherItem)
    @Query("DELETE FROM WEATHERITEM WHERE cityName = :cityName")
    suspend fun deleteWeatherItem(cityName: String)
    @Query("SELECT * FROM WeatherItem WHERE id = :id")
    fun getWeatherItem(id: Int): Flow<WeatherItem>
    @Query("SELECT * FROM WEATHERITEM")
    fun getAllWeatherItems(): Flow<List<WeatherItem>>
}
package com.example.windspell.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherItem(weatherItem: WeatherItem)
    @Query("DELETE FROM WEATHERITEM WHERE cityId = :id")
    suspend fun deleteWeatherItem(id: Int)
    @Query("SELECT * FROM WEATHERITEM ORDER BY lastTimeUpdated")
    fun getAllWeatherItems(): Flow<List<WeatherItem>>
}
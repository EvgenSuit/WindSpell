package com.example.windspell.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [WeatherItem::class] ,version = 4)
@TypeConverters(WeatherListConverter::class, ForecastListConverter::class)
abstract class WeatherDatabase :RoomDatabase(){
    abstract fun weatherDao(): WeatherDao

    companion object{
        private var INSTANCE: WeatherDatabase? = null

        fun getWeatherDatabase(context: Context): WeatherDatabase {
            return synchronized(this) {
                return Room.databaseBuilder(
                    context,
                    WeatherDatabase::class.java,
                    "WeatherDatabase"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
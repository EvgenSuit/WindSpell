package com.example.windspell

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WeatherApplication: Application() {
    /*lateinit var context: Context
    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }*/
}
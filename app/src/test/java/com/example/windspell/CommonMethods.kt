package com.example.windspell

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.core.app.ApplicationProvider
import com.example.windspell.data.WeatherItem
import com.example.windspell.data.createQuery
import com.example.windspell.geocoding.GeocodingResult
import com.example.windspell.network.GeocodingApi
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.math.abs

fun mockGeocodingApi(weatherItems: List<WeatherItem>): GeocodingApi =
    mockk<GeocodingApi> {
        for (i in weatherItems.indices) {
            coEvery { geo(cityName = weatherItems[i].createQuery()) } returns listOf(GeocodingResult())
        } }

fun ComposeContentTestRule.getString(@StringRes id: Int): String =
    ApplicationProvider.getApplicationContext<Context>().resources.getString(id)

fun approximatelyEqual(value1: Long, value2: Long, deviation: Long): Boolean {
    return abs(value1 - value2) <= deviation
}

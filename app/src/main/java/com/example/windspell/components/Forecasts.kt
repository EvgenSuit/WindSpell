package com.example.windspell.components

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import com.example.windspell.weather.ForecastResult
import com.example.windspell.weather.ForecastUnit
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun Forecasts(forecastResult: ForecastResult,
              modifier: Modifier = Modifier) {
    if (forecastResult.list.isNotEmpty()) {
        Log.d("FORECAST", forecastResult.list[0].temp.day.toString())
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.padding(35.dp)//.background(Color.White)
    ) {
        forecastResult.list.forEach {
            ForecastItem(forecastUnit = it)
        }
    }
}

@Composable
fun ForecastItem(forecastUnit: ForecastUnit) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${timestampToDate(forecastUnit.timestamp)}") //convert to week day
        Spacer(modifier = Modifier.weight(1f))

        Image(painter = painterResource(id = getWeatherIcon(forecastUnit.weather.first().icon)),
            modifier = Modifier.size(50.dp),
            contentDescription = null)

        Spacer(modifier = Modifier.weight(0.1f))
        Text((forecastUnit.temp.day.roundToInt()).toString() + " \u2103")
        Spacer(modifier = Modifier.weight(0.2f))
        Text((forecastUnit.temp.night.roundToInt()).toString()+ " \u2103")
    }
}

fun timestampToDate(timestamp: Long): DayOfWeek {
    val instant = Instant.ofEpochSecond(timestamp)
    return DayOfWeek.from(instant.atZone(ZoneId.systemDefault()))
}
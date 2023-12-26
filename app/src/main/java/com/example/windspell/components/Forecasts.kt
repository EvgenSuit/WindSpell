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
              lang: String,
              modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.padding(35.dp)
    ) {
        forecastResult.list.forEach {
            ForecastItem(forecastUnit = it, lang)
        }
    }
}

@Composable
fun ForecastItem(forecastUnit: ForecastUnit, lang: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(timestampToDate(forecastUnit.timestamp, lang)) //convert to week day
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

fun timestampToDate(timestamp: Long, lang: String): String {
    val instant = Instant.ofEpochSecond(timestamp)
    val dayOfWeek = DayOfWeek.from(instant.atZone(ZoneId.systemDefault()))
    return translatedDayNames(dayOfWeek, lang)
}

fun translatedDayNames(dayOfWeek: DayOfWeek, lang: String):String {
    if (lang == "ru" || lang == "by") {
        return when(dayOfWeek.name) {
            "MONDAY" -> "ПОНЕДЕЛЬНИК"
            "TUESDAY" -> "ВТОРНИК"
            "WEDNESDAY" -> "СРЕДА"
            "THURSDAY" -> "ЧЕТВЕРГ"
            "FRIDAY" -> "ПЯТНИЦА"
            "SATURDAY" -> "СУББОТА"
            else -> "ВОСКРЕСЕНЬЕ"
        }
    }
    if (lang == "pl") {
        return when(dayOfWeek.name) {
            "MONDAY" -> "PONIEDZIAŁEK"
            "TUESDAY" -> "WTOREK"
            "WEDNESDAY" -> "ŚRODA"
            "THURSDAY" -> "CZWARTEK"
            "FRIDAY" -> "PIĄTEK"
            "SATURDAY" -> "SOBOTA"
            else -> "NIEDZIELA"
        }
    }
    else {
        return dayOfWeek.name
    }
}
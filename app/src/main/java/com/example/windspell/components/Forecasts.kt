package com.example.windspell.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.windspell.R
import com.example.windspell.weather.ForecastResult
import com.example.windspell.weather.ForecastUnit
import kotlin.math.roundToInt

@Composable
fun Forecasts(forecastResult: ForecastResult,
              degreeFormat: String,
              timestampToDayOfWeek: (Long) -> Int) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.testTag("Forecasts")
    ) {
        forecastResult.list.forEach {
            ForecastItem(forecastUnit = it, degreeFormat, timestampToDayOfWeek)
        }
    }
}

@Composable
fun ForecastItem(forecastUnit: ForecastUnit,
                 degreeFormat: String,
                 timestampToDayOfWeek: (Long) -> Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = whiteBackground.fillMaxWidth()
    ) {
        val style = MaterialTheme.typography.labelSmall
        Text(stringResource(timestampToDayOfWeek(forecastUnit.timestamp)),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.fillMaxWidth(0.5f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Image(painter = painterResource(id = getWeatherIcon(forecastUnit.weather.first().icon)),
                modifier = Modifier.size(dimensionResource(id = R.dimen.weather_icon_size)),
                contentDescription = null)
            Text("${forecastUnit.temp.day.roundToInt()} $degreeFormat",
                style = style)
            Text("${forecastUnit.temp.night.roundToInt()} $degreeFormat",
                style = style)
        }
    }
}



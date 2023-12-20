package com.example.windspell.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.windspell.R
import com.example.windspell.WeatherViewModel
import com.example.windspell.weather.WeatherResult
import kotlin.math.roundToInt

@Composable
fun MainScreen(viewModel: WeatherViewModel = viewModel()) {
    val weatherResult = viewModel.weatherResult
    var cityConfirmed by rememberSaveable {
        mutableStateOf(false)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SearchBar(
            "${weatherResult.name} ${weatherResult.sys.country}",
            onTextChanged = { viewModel.getCoordinates(it)},
            onSuggestionChanged = {cityConfirmed = it})
        if (cityConfirmed && weatherResult.weather.isNotEmpty()) {
            WeatherDetails(weatherResult)
        }
    }
}

@Composable
fun SearchBar(
    citySuggestion: String,
    onTextChanged: (String) -> Unit,
    onSuggestionChanged: (Boolean) -> Unit
) {
    var city by rememberSaveable {
        mutableStateOf("")
    }
    var cityConfirmed by rememberSaveable {
        mutableStateOf(true)
    }
    val focusRequester = remember {
        FocusRequester()
    }
    val focusManager = LocalFocusManager.current
    //If results are not satisfied, a country code should be added (in uppercase)
    /*If they are still not satisfied, it means that
     the location name was chosen from within a cities'
     territory (e.g as a name of a street, district, bay)
     */
    Column{
        TextField(
            value = city,
            trailingIcon = {
                           IconButton(onClick = {city = ""
                           focusRequester.requestFocus()}) {
                               Icon(imageVector = Icons.Filled.Clear, contentDescription = null)
                           }
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {focusManager.clearFocus()}
            ),
            maxLines = 1,
            onValueChange = {
                    city = it
                    cityConfirmed = false
                    onSuggestionChanged(false)
                    if (it.isNotBlank() && city[0] != ' ') {
                        val pattern = Regex("\\d")
                        val value = pattern.find(it)
                        if (value == null) {
                            onTextChanged(city)
                        }
                    }
            },
            label = {Text("City")},
            modifier = Modifier.focusRequester(focusRequester).fillMaxWidth().testTag("SearchBar"))
        if (!cityConfirmed && city.isNotBlank() && citySuggestion != " ") {
            SuggestedCity(
                onCityConfirmed = {
                    cityConfirmed = true
                    focusManager.clearFocus()
                    onSuggestionChanged(true)
                },
                suggestedCity = citySuggestion)
        }
    }
}

@Composable
fun SuggestedCity(
    onCityConfirmed: () -> Unit,
    suggestedCity: String) {
    Row(
        modifier = Modifier
            .background(Color.Blue)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        TextButton(onClick = onCityConfirmed,
            modifier = Modifier.testTag("SuggestedCity")) {
            Text(
                suggestedCity,
                color = Color.White,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth())
        }
    }
}

@Composable
fun WeatherDetails(
    weatherResult: WeatherResult) {
    val weatherConditionIcon = weatherResult.weather.first().icon
    Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(15.dp).testTag("WeatherDetails")
        ) {
           Row(
               horizontalArrangement = Arrangement.spacedBy(20.dp),
               modifier = Modifier.padding(top = 50.dp)
           ) {
               Column(
                   modifier = Modifier
                       .align(Alignment.CenterVertically)
                       .weight(1f)
               ) {
                   //City(Location) name
                   Text(weatherResult.name,
                       lineHeight = 55.sp,
                       style = MaterialTheme.typography.titleMedium,)

                   //Temperature
                   Text("${weatherResult.main.temp.roundToInt()} \u2103",
                       modifier = Modifier.padding(top = 10.dp),
                       style = MaterialTheme.typography.displayMedium)

                   //Weather condition
                   Text(if (weatherResult.weather.isEmpty()) "" else weatherResult.weather.first().main,
                       modifier = Modifier.padding(top = 10.dp),
                       style = MaterialTheme.typography.displayMedium)
               }
               Image(painter = painterResource(getWeatherIcon(weatherConditionIcon)),
                   modifier = Modifier
                       .size(200.dp)
                       .weight(1f),
                   contentDescription = null)
           }

        }
}

fun getWeatherIcon(sourceIcon: String): Int {
    return when(sourceIcon) {
        "01d" -> R.drawable._01d
        "01n" -> R.drawable._01n
        "02d" -> R.drawable._02d
        "02n" -> R.drawable._02n
        "03d" -> R.drawable._03d
        "03n" -> R.drawable._03n
        "04d" -> R.drawable._04d
        "04n" -> R.drawable._04n
        "09d" -> R.drawable._09d
        "09n" -> R.drawable._09n
        "10d" -> R.drawable._10d
        "10n" -> R.drawable._10n
        "11d" -> R.drawable._11d
        "11n" -> R.drawable._11n
        "13d" -> R.drawable._13d
        "13n" -> R.drawable._13n
        "50d" -> R.drawable._50d
        else -> R.drawable._50n
    }
}
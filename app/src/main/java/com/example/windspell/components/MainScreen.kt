package com.example.windspell.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.List
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.windspell.R
import com.example.windspell.WeatherViewModel
import com.example.windspell.data.WeatherItem
import com.example.windspell.weather.ForecastResult
import com.example.windspell.weather.WeatherResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.roundToInt

@Composable
fun MainScreen(viewModel: WeatherViewModel = viewModel(factory = WeatherViewModel.Factory)) {
    val weatherResult by viewModel.weatherResult.collectAsState()
    val forecastResult by viewModel.forecastResult.collectAsState()
    val weatherItems by viewModel.weatherItems.collectAsState()

    var job by remember { mutableStateOf(Job() as Job?) }

    var cityConfirmed by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val textSearch = weatherResult.name.trim()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(200.dp)
            ) {
                Row{
                    DrawerButton (onDrawerButtonPressed = {
                        coroutineScope.launch {
                            drawerState.apply {
                                if (isClosed) open() else close()
                            }
                        }
                    })
                    Text("Cities",  modifier = Modifier.padding(16.dp))
                }
                Divider()
                weatherItems.forEach {
                    CityDrawer(weatherItem = it,
                        onClick = {
                            cityConfirmed = true
                            coroutineScope.launch {
                                drawerState.apply { close() }
                            }
                            /*
                            If the difference between the time of the last weather update
                            and current time is bigger than or equal to an hour,
                            fetch up-to-date weather data. Otherwise fetch data
                            from the DB
                             */
                            if (it.lastTimeUpdated - Instant.now().toEpochMilli() >= 60 * 60 * 1000L) {
                                coroutineScope.launch {
                                    viewModel.getWeather(it.cityName)
                                }
                            } else {
                                viewModel.updateWeatherResult(it)
                                viewModel.updateForecastResult(it.forecastUnit)
                            }
                            },
                        onDelete = { viewModel.deleteWeatherItem(it.cityName) })
                }
            }
        }) {
            Column {
                MainDrawerContent(
                    onTextChanged = {job?.cancel()
                        job = coroutineScope.launch {
                            delay(200L)
                            viewModel.getWeather(it)
                        } },
                    onDrawerButtonPressed = { coroutineScope.launch {
                        drawerState.apply {
                            if (isClosed) open() else close()
                        }
                    }},
                    onSuggestionChanged = {
                        //Save city info to the database
                        cityConfirmed = it
                        if (cityConfirmed) {
                                viewModel.insertWeatherItem(textSearch,
                                    weatherResult,
                                    forecastResult)
                        }
                    },
                    textSearch = textSearch,
                    weatherResult = weatherResult,
                    forecastResult = forecastResult,
                    cityConfirmed = cityConfirmed)
            }
    }
}
@Composable
fun DrawerButton(onDrawerButtonPressed: () -> Unit) {
    IconButton(onClick = onDrawerButtonPressed) {
        Icon(imageVector = Icons.Rounded.List, contentDescription = null)
    }
}

@Composable
fun MainDrawerContent(
    onTextChanged: (String) -> Unit,
    onSuggestionChanged: (Boolean) -> Unit,
    onDrawerButtonPressed: () -> Unit,
    textSearch: String,
    weatherResult: WeatherResult,
    forecastResult: ForecastResult,
    cityConfirmed: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row{
            DrawerButton (onDrawerButtonPressed = onDrawerButtonPressed)
            SearchBar(
                textSearch + " ${weatherResult.sys.country}",
                onTextChanged = onTextChanged,
                onSuggestionChanged = onSuggestionChanged)
        }
        if (cityConfirmed && weatherResult.weather.isNotEmpty()) {
            WeatherDetails(weatherResult)
            Forecasts(forecastResult)
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
             IconButton(
                  onClick = {
                      city = ""
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
                //check if the city name doesn't contain any digits
                    if (it.isNotBlank() && city[0] != ' ') {
                        val pattern = Regex("\\d")
                        val value = pattern.find(it)
                        if (value == null) {
                            onTextChanged(city)
                        }
                    }
            },
            label = {Text("City")},
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth()
                .testTag("SearchBar"))
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
        modifier = Modifier
            .padding(15.dp)
            .testTag("WeatherDetails")
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
                       Text("${weatherResult.main.tempMin.roundToInt()} \u2103" +
                               "/${weatherResult.main.tempMax.roundToInt()} \u2103",
                           style = MaterialTheme.typography.displayMedium)

                   //Weather condition
                   Text(if (weatherResult.weather.isEmpty()) "" else weatherResult.weather.first().main,
                       modifier = Modifier.padding(top = 10.dp),
                       style = MaterialTheme.typography.displayMedium)
               }
               //Weather icon
               Image(painter = painterResource(getWeatherIcon(weatherConditionIcon)),
                   modifier = Modifier
                       .size(200.dp)
                       .weight(1f),
                   contentDescription = null)
           }

        }
}

@Composable
fun CityDrawer(weatherItem: WeatherItem,
               onClick: () -> Unit,
               onDelete: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }

    var weatherItemLongClicked by remember {
        mutableStateOf(false)
    }
    val viewConfiguration = LocalViewConfiguration.current
    LaunchedEffect(key1 = interactionSource) {
        interactionSource.interactions.collectLatest {interaction ->
            if (interaction is PressInteraction.Press) {
                    weatherItemLongClicked = false
                    delay(viewConfiguration.longPressTimeoutMillis)
                    weatherItemLongClicked = true
                }
        }
    }
    NavigationDrawerItem(
        interactionSource = interactionSource,
        icon = {if (weatherItemLongClicked) {
            IconButton(
                onClick = onDelete) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = null)
            }
        } },
        label = { Text(weatherItem.cityName, overflow = TextOverflow.Ellipsis) },
        selected = false, onClick = {
            if (!weatherItemLongClicked) {
               onClick()
            }
        })
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
package com.example.windspell.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.List
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.windspell.R
import com.example.windspell.WeatherViewModel
import com.example.windspell.data.WeatherItem
import com.example.windspell.network.ShowNoNetwork
import com.example.windspell.weather.ForecastResult
import com.example.windspell.weather.WeatherResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.roundToInt


@Composable
fun MainScreen(viewModel: WeatherViewModel = viewModel(factory = WeatherViewModel.Factory),
               darkTheme: Boolean = false,
               networkIsOn: Boolean = false,
               onThemeChanged: (Boolean) -> Unit,
               ) {

    val weatherResult by viewModel.weatherResult.collectAsState()
    val forecastResult by viewModel.forecastResult.collectAsState()
    val weatherItems by viewModel.weatherItems.collectAsState()

    var job by remember { mutableStateOf(Job() as Job?) }

    var cityConfirmed by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val textSearch = weatherResult.name.trim()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!networkIsOn) {
            ShowNoNetwork()
        }
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
                        Text(stringResource(id = R.string.cities),  modifier = Modifier.padding(16.dp))
                    }
                    Divider()
                    weatherItems.forEach {
                        CityDrawer(weatherItem = it,
                            onClick = {
                                cityConfirmed = true
                                /*
                                If the difference between the time of the last weather update
                                and current time is bigger than or equal to an hour, network is on
                                and the language setting has been changed,
                                fetch up-to-date weather data. Otherwise fetch data
                                from the DB
                                 */
                                if (networkIsOn && (it.lastTimeUpdated - Instant.now().epochSecond >= 60 * 60 * 1000L || it.lang != viewModel.lang)) {
                                    coroutineScope.launch {
                                        drawerState.apply { close() }
                                        viewModel.getWeather(it.cityName, true)
                                    }}
                                 else {
                                    coroutineScope.launch {
                                        drawerState.apply { close() }
                                        viewModel.updateWeatherItem(it)
                                        viewModel.updateForecastResult(it.forecastUnit)
                                    }}
                            },
                            onDelete = { viewModel.deleteWeatherItem(it.cityName) })
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    ChangeTheme(darkTheme, onThemeChanged,
                        modifier = Modifier.align(Alignment.End))
                }
            }) {
            MainDrawerContent(
                onTextChanged = {job?.cancel()
                    if (networkIsOn) {
                        job = coroutineScope.launch {
                            delay(200L)
                            viewModel.getWeather(it, false)
                        }
                    }
                },
                onDrawerButtonPressed = { coroutineScope.launch {
                    drawerState.apply {
                        if (isClosed) open() else close()
                    }
                }},
                onSuggestionChanged = {
                    //Save city info to the database
                    cityConfirmed = it
                    if (cityConfirmed) {
                        viewModel.insertWeatherItem(textSearch, weatherResult, forecastResult)
                    }
                },
                textSearch = textSearch,
                weatherResult = weatherResult,
                forecastResult = forecastResult,
                cityConfirmed = cityConfirmed,
                lang = viewModel.lang)
        }
    }
}

@Composable
fun ChangeTheme(darkTheme: Boolean,
                 onThemeChanged: (Boolean) -> Unit,
                 modifier: Modifier = Modifier) {
    Row(modifier = modifier.testTag("ChangeTheme")) {
        Switch(checked = darkTheme, onCheckedChange = onThemeChanged)
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
    cityConfirmed: Boolean,
    lang: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row{
            DrawerButton (onDrawerButtonPressed = onDrawerButtonPressed)
            SearchBar(
                textSearch + " ${weatherResult.sys.country}",
                onTextChanged = onTextChanged,
                onSuggestionChanged = onSuggestionChanged)
        }
        if (cityConfirmed && weatherResult.weather.isNotEmpty()) {
            WeatherDetails(weatherResult)
            Forecasts(forecastResult, lang)
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
    val interactionSource = remember {
        MutableInteractionSource()
    }
    val focused by interactionSource.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    Column{
        TextField(
            interactionSource = interactionSource,
            value = city,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            trailingIcon = {
             IconButton(
                  onClick = {
                      city = ""
                      focusRequester.requestFocus()}) {
                               if (city.isNotBlank()) {
                                   Icon(imageVector = Icons.Filled.Clear, contentDescription = null)
                               }
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
            label = { if (!focused) Icon(imageVector = Icons.Filled.Search, contentDescription = null)},
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
            .background(MaterialTheme.colorScheme.inversePrimary)
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
                   Text(
                       weatherResult.name,
                       lineHeight = 55.sp,
                       style = MaterialTheme.typography.titleMedium,
                   )

                   //Temperature (\u2103 means Celsius)
                   Text("${weatherResult.main.temp.roundToInt()} \u2103",
                       modifier = Modifier.padding(top = 10.dp),
                       style = MaterialTheme.typography.displayMedium)
                       Text("${weatherResult.main.tempMin.roundToInt()} \u2103" +
                               " / ${weatherResult.main.tempMax.roundToInt()} \u2103",
                           style = MaterialTheme.typography.displayMedium)

                   //Weather condition
                   Text(if (weatherResult.weather.isEmpty()) "" else weatherResult.weather.first().description,
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
                onClick = {
                    onDelete()
                    weatherItemLongClicked = false
                }) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = null)
            }
        } },
        label = { Text(weatherItem.cityName, overflow = TextOverflow.Ellipsis) },
        selected = false, onClick = {
            if (!weatherItemLongClicked) {
               onClick()
                weatherItemLongClicked = false
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

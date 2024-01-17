package com.example.windspell.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.windspell.R
import com.example.windspell.WeatherViewModel
import com.example.windspell.components.ChangeThemeSwitch
import com.example.windspell.components.CityDrawer
import com.example.windspell.components.DrawerButton
import com.example.windspell.components.Forecasts
import com.example.windspell.components.SearchBar
import com.example.windspell.components.SunriseSunsetInfo
import com.example.windspell.components.WeatherDetails
import com.example.windspell.components.colorStopsDark
import com.example.windspell.components.colorStopsLight
import com.example.windspell.network.ShowNoNetwork
import com.example.windspell.weather.ForecastResult
import com.example.windspell.weather.WeatherResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

val Context.weatherDataStore: DataStore<Preferences> by preferencesDataStore(name = "weatherItem")
@Composable
fun MainScreen(darkTheme: Boolean,
               networkIsOn: Boolean = true,
               onThemeChanged: (Boolean) -> Unit){
    val viewModel: WeatherViewModel = hiltViewModel()
    val weatherResult by viewModel.weatherResult.collectAsState()
    val forecastResult by viewModel.forecastResult.collectAsState()
    val weatherItems by viewModel.weatherItems.collectAsState()

    var job by remember { mutableStateOf(Job() as Job?) }

    var cityConfirmed by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val textSearch = weatherResult.name.trim()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.background(MaterialTheme.colorScheme.primary)
    ) {
        if (!networkIsOn) {
            ShowNoNetwork()
        }
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(dimensionResource(id = R.dimen.padding_huge)),
                    drawerContainerColor = MaterialTheme.colorScheme.primary,
                    drawerContentColor = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        DrawerButton (onDrawerButtonPressed = {
                            coroutineScope.launch {
                                drawerState.apply {
                                    if (isClosed) open() else close()
                                }
                            }
                        })
                        Text(stringResource(id = R.string.cities),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.displaySmall,
                            modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_small)))
                    }
                    Divider()
                    Box {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_small)
                            ),
                        ) {
                            items(weatherItems) {
                                CityDrawer(weatherItem = it,
                                    onClick = {
                                        cityConfirmed = true
                                        /*
                                        If the difference between the time of the last weather update
                                        and current time is bigger than or equal to an hour, network is on
                                        or the language setting has been changed,
                                        fetch up-to-date weather data. Otherwise fetch data
                                        from the DB
                                         */
                                        if (networkIsOn && (Instant.now().epochSecond - it.lastTimeUpdated >= 60*60 || it.lang != viewModel.lang)) {
                                            coroutineScope.launch {
                                                drawerState.apply { close() }
                                                viewModel.getWeather("${it.cityName}, ${it.sys.country}", true, cityId =  it.cityId)
                                            }}
                                        else {
                                            coroutineScope.launch {
                                                drawerState.apply { close() }
                                                viewModel.updateWeatherItem(it)
                                                viewModel.updateForecastResult(it.forecastUnit)
                                            }}
                                    },
                                    onDelete = { viewModel.deleteWeatherItem(it.cityId)
                                        viewModel.updateWeatherResult(WeatherResult())})
                            }
                        }
                        Column(modifier = Modifier.align(Alignment.BottomEnd)) {
                            Spacer(modifier = Modifier.weight(1f))
                            ChangeThemeSwitch(darkTheme, onThemeChanged)
                        }
                    }
                }
            }) {
            MainDrawerContent(
                onTextChanged = {job?.cancel()
                    if (networkIsOn) {
                        job = coroutineScope.launch {
                            delay(300L)
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
                        viewModel.updateWeatherResult(weatherResult)
                        viewModel.updateForecastResult(forecastResult.list)
                    }
                },
                textSearch = textSearch,
                weatherResult = weatherResult,
                forecastResult = forecastResult,
                cityConfirmed = cityConfirmed,
                defaultCityLoaded = viewModel.defaultCityLoaded.value,
                timestampToDate = {timestamp, includeMMddyy -> viewModel.timestampToDate(timestamp, includeMMddyy)},
                lang = viewModel.lang)
        }
    }
}

@Composable
fun MainDrawerContent(
    onTextChanged: (String) -> Unit,
    onSuggestionChanged: (Boolean) -> Unit,
    onDrawerButtonPressed: () -> Unit,
    timestampToDate: (Long, Boolean) -> String,
    textSearch: String,
    weatherResult: WeatherResult,
    forecastResult: ForecastResult,
    cityConfirmed: Boolean,
    defaultCityLoaded: Boolean,
    lang: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Row{
            DrawerButton (onDrawerButtonPressed = onDrawerButtonPressed)
            SearchBar(
                textSearch + " ${weatherResult.sys.country}",
                onTextChanged = onTextChanged,
                onSuggestionChanged = onSuggestionChanged)
        }
        if ((cityConfirmed || defaultCityLoaded) && weatherResult.weather.isNotEmpty()) {
            val backgroundColors: Array<Pair<Float, Color>>
            val icon = weatherResult.weather.first().icon
            backgroundColors = if (icon.contains("n")) {
                colorStopsDark
            } else colorStopsLight
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(Brush.verticalGradient(colorStops = backgroundColors))
                    .padding(dimensionResource(id = R.dimen.padding_medium))
            ) {
                WeatherDetails(weatherResult)
                Spacer(modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.padding_medium)))
                Forecasts(forecastResult, lang)
                Spacer(modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.padding_small)))
                SunriseSunsetInfo(weatherResult, timestampToDate)
                Spacer(modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.padding_medium)))
                Text("${stringResource(id = R.string.as_of)} ${timestampToDate(weatherResult.dt, true)}", modifier = Modifier.padding(
                    dimensionResource(id = R.dimen.padding_small)))
            }
        }
    }
}

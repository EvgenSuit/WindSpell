package com.example.windspell.presentation

import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.windspell.R
import com.example.windspell.components.ChangeThemeSwitch
import com.example.windspell.components.CityDrawerItem
import com.example.windspell.components.DrawerButton
import com.example.windspell.components.Forecasts
import com.example.windspell.components.SuggestedCity
import com.example.windspell.components.SunriseSunsetInfo
import com.example.windspell.components.TopBar
import com.example.windspell.components.WeatherDetails
import com.example.windspell.components.colorStopsDark
import com.example.windspell.components.colorStopsLight
import com.example.windspell.components.keyboardAsState
import com.example.windspell.domain.Result
import com.example.windspell.domain.localeReceiver
import com.example.windspell.weather.ForecastResult
import com.example.windspell.weather.WeatherResult
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.launch
import java.util.Locale


@Composable
fun MainScreen(darkTheme: Boolean,
               networkIsOn: Boolean,
               fusedLocationProviderClient: FusedLocationProviderClient? = null,
               maxWidth: Dp,
               viewModel: WeatherViewModel = hiltViewModel(),
               onThemeChanged: (Boolean) -> Unit,
               onSnackbarShow: (String) -> Unit){
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val weatherResult = uiState.weatherResult
    val forecastResult = uiState.forecastResult
    val weatherItems = uiState.weatherItems
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    //city name
    val searchResult by rememberSaveable(weatherResult) {
        mutableStateOf(weatherResult.name.trim())
    }
    //current text field value
    var cityInput by rememberSaveable {
        mutableStateOf("")
    }
    val scrollState = rememberScrollState()
    val cityDrawerItemSpacing = dimensionResource(id = R.dimen.city_drawer_item_spacing)
    DisposableEffect(context) {
        val receiver = localeReceiver(viewModel::onLocaleChange)
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_LOCALE_CHANGED))
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
    LaunchedEffect(networkIsOn) {
        viewModel.updateNetworkState(networkIsOn)
    }
    LaunchedEffect(viewModel) {
        viewModel.weatherFetchResultFlow.collect {res ->
            if (res.error.isNotEmpty()) {
                onSnackbarShow(res.error)
            }
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("Content")
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.fillMaxWidth(if (maxWidth < 500.dp) 0.65f else 0.4f),
                    drawerContainerColor = MaterialTheme.colorScheme.background,
                    drawerContentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ){
                        DrawerButton (
                            modifier = Modifier.testTag("RegularDrawerButton"),
                            onDrawerButtonPressed = {
                            coroutineScope.launch {
                                drawerState.apply {
                                    if (isClosed) open() else close()
                                }
                            }
                        })
                        Text(stringResource(id = R.string.cities),
                            style = MaterialTheme.typography.displaySmall.copy(
                                MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_small)))
                    }
                    Divider()
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(cityDrawerItemSpacing),
                        contentPadding = PaddingValues(3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = cityDrawerItemSpacing)
                    ) {
                        // provide unique key so that 'show' and 'dismissState' of each item
                        // is guaranteed to be unique upon change to other items
                        items(weatherItems, key = {it.cityId}) {item ->
                            CityDrawerItem(weatherItem = item,
                                onClick = {
                                coroutineScope.launch {
                                    drawerState.apply { close() }
                                    viewModel.manageWeatherItem(item, lon = item.lon, lat = item.lat)
                                    scrollState.animateScrollTo(0)
                                }
                            }, onDelete = viewModel::deleteWeatherItem)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        contentAlignment = Alignment.CenterEnd,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ChangeThemeSwitch(darkTheme, onThemeChanged)
                    }
                }
            }) {
            MainDrawerContent(
                cityInput = cityInput,
                scrollState = scrollState,
                isCurrentItemSaved = weatherItems.any { it.cityId == weatherResult.cityId }
                        || weatherResult.cityId == -1,
                onTextChanged = {
                    cityInput = it
                    viewModel.onSearchTextChanged(it)
                },
                onDrawerButtonPressed = { coroutineScope.launch {
                    drawerState.apply { if (isClosed) open() else close() }
                }},
                onCityConfirmed = {
                    //Save city info to the database
                    if (it) { viewModel.onCityConfirmed() }
                },
                searchResult = searchResult,
                weatherResult = weatherResult,
                forecastResult = forecastResult,
                weatherFetchResult = uiState.weatherFetchResult,
                timestampToDate = viewModel::timestampToDate,
                fusedLocationProviderClient = fusedLocationProviderClient,
                onLocation = {lat, lon ->
                         coroutineScope.launch {
                             viewModel.manageWeatherItem(lat = lat, lon = lon)
                         }
                },
                timestampToDayOfWeek = viewModel::timestampToDayOfWeek)
        }
    }
}

@Composable
fun MainDrawerContent(
    cityInput: String,
    scrollState: ScrollState,
    isCurrentItemSaved: Boolean,
    onTextChanged: (String) -> Unit,
    onCityConfirmed: (Boolean) -> Unit,
    onDrawerButtonPressed: () -> Unit,
    timestampToDate: (Long) -> String,
    timestampToDayOfWeek: (Long) -> Int,
    onLocation: (Double, Double) -> Unit,
    fusedLocationProviderClient: FusedLocationProviderClient?,
    searchResult: String,
    weatherResult: WeatherResult,
    forecastResult: ForecastResult,
    weatherFetchResult: Result) {
    val scope = rememberCoroutineScope()
    var cityConfirmed by rememberSaveable {
        mutableStateOf(false)
    }
    val isKeyboardOpen by keyboardAsState()
    val focusManager = LocalFocusManager.current
    val weatherDetailsPadding = dimensionResource(R.dimen.padding_small)
    val topBarHeight = dimensionResource(R.dimen.top_bar_height)
    val topBarPadding = dimensionResource(R.dimen.top_bar_padding)
    // \u2109 means Fahrenheit, \u2103 means Celsius
    val degreeFormat = if (Locale.getDefault().country == "US") "\u2109" else "\u2103"
    //append country code to the city name
    val citySuggestion by rememberSaveable(searchResult, weatherResult) {
        mutableStateOf(searchResult + ", ${weatherResult.sys.country}")
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .testTag("ScrollColumn")
        ) {
            AnimatedVisibility(weatherResult.weather.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (weatherResult.weather.isNotEmpty()) {
                    val icon = weatherResult.weather.first().icon
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colorStops = if (icon.contains("n")) {
                                        colorStopsDark
                                    } else colorStopsLight
                                )
                            )
                            .padding(weatherDetailsPadding)
                            .padding(top = topBarHeight - weatherDetailsPadding + topBarPadding)
                            .pointerInput(Unit) {
                                detectTapGestures(onPress = { focusManager.clearFocus(true) })
                            }
                    ) {
                        WeatherDetails(weatherResult, degreeFormat)
                        Spacer(modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.padding_medium)))
                        Forecasts(forecastResult, degreeFormat, timestampToDayOfWeek)
                        Spacer(modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.padding_small)))
                        SunriseSunsetInfo(weatherResult, timestampToDate)
                        Spacer(modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.padding_medium)))
                        Text("${stringResource(id = R.string.as_of)} ${timestampToDate(weatherResult.lastTimeUpdated)}", modifier = Modifier
                            .padding(dimensionResource(id = R.dimen.padding_small))
                            .testTag(stringResource(R.string.as_of)),
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (weatherFetchResult is Result.Empty) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.no_weather_data),
                    color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        TopBar(cityInput = cityInput,
            fusedLocationProviderClient = fusedLocationProviderClient,
            canScrollBackward = scrollState.canScrollBackward,
            onTextChanged = {
                scope.launch {
                    onTextChanged(it)
                    scrollState.animateScrollTo(0)
                }
            },
            onLocation = { lat, lon ->
                     scope.launch {
                         onLocation(lat, lon)
                         scrollState.animateScrollTo(0)
                     }
            },
            onSuggestionChanged = onCityConfirmed,
            onDrawerButtonPressed = onDrawerButtonPressed)
        // add button
        AnimatedVisibility(!isCurrentItemSaved,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(25.dp)) {
            IconButton(onClick = { onCityConfirmed(true) },
                modifier = Modifier
                    .size(60.dp)
                    .testTag("Add item")) {
                Icon(Icons.Filled.AddCircle,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = null)
            }
        }
        // suggested city
        AnimatedVisibility(cityInput.isNotBlank() && citySuggestion != " "
                && isKeyboardOpen && !scrollState.canScrollBackward && weatherFetchResult is Result.Success,
            enter = fadeIn(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(400)),
            modifier = Modifier
                .padding(top = dimensionResource(R.dimen.top_bar_height)
                + topBarPadding)) {
            SuggestedCity(
                isSaved = isCurrentItemSaved,
                onCityConfirmed = {
                    cityConfirmed = true
                    focusManager.clearFocus(true)
                    onCityConfirmed(true)
                },
                suggestedCity = citySuggestion)
        }
    }
}

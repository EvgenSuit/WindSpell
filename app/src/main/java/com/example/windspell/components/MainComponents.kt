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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.windspell.R
import com.example.windspell.data.WeatherItem
import com.example.windspell.weather.WeatherResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt


@Composable
fun DrawerButton(onDrawerButtonPressed: () -> Unit) {
    IconButton(onClick = onDrawerButtonPressed) {
        Icon(imageVector = Icons.Rounded.Menu,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(80.dp),
            contentDescription = null)
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
    /*
    If search results are not satisfied, it's worth entering a country code
    e.g: New York, US
     */
    Column(modifier = Modifier.background(Color.Transparent)){
        TextField(
            interactionSource = interactionSource,
            value = city,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.primary,
                focusedIndicatorColor = MaterialTheme.colorScheme.inversePrimary,
                cursorColor = MaterialTheme.colorScheme.onPrimary,
                focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
            ),
            trailingIcon = {
                IconButton(
                    onClick = {
                        city = ""
                        focusRequester.requestFocus()}) {
                    if (city.isNotBlank()) {
                        Icon(imageVector = Icons.Filled.Clear,
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = null)
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
            label = { if (!focused) Icon(imageVector = Icons.Filled.Search,
                tint = MaterialTheme.colorScheme.onPrimary,
                contentDescription = null)
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
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
            .background(Color.Transparent)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        TextButton(onClick = onCityConfirmed,
            modifier = Modifier
                .testTag("SuggestedCity")) {
            Text(
                suggestedCity,
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier
                    .padding(dimensionResource(id = R.dimen.padding_small))
                    .fillMaxWidth())
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
        label = { Text(weatherItem.cityName, overflow = TextOverflow.Ellipsis, fontSize = 20.sp) },
        selected = false, onClick = {
            if (!weatherItemLongClicked) {
                onClick()
                weatherItemLongClicked = false
            }
        },
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = MaterialTheme.colorScheme.primary,
            unselectedTextColor = MaterialTheme.colorScheme.onPrimary
        ))
}

@Composable
fun WeatherDetails(
    weatherResult: WeatherResult) {
    val weatherConditionIcon = weatherResult.weather.first().icon
    val paddingSmall = dimensionResource(id = R.dimen.padding_small)
    val minMaxTempStyle = MaterialTheme.typography.displaySmall
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(getWeatherIcon(weatherConditionIcon)),
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.padding_huge)),
            contentDescription = null
        )
        //City + Country name
        Text(
            "${weatherResult.name}, " +
                    weatherResult.sys.country,
            lineHeight = 65.sp,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        //Current temperature (\u2103 means Celsius)
        Text(
            "${weatherResult.main.temp.roundToInt()} \u2103",
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(top = paddingSmall, bottom = paddingSmall)
        )
        Text(weatherResult.weather.first().description,
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(bottom = paddingSmall))
        //Min Max Temp
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = whiteBackground
        ) {
            Text(stringResource(id = R.string.min_temp),
                style = minMaxTempStyle,
                modifier = Modifier.padding(end = paddingSmall))
            Text("${weatherResult.main.tempMin.roundToInt()} \u2103",
                style = minMaxTempStyle,)
            Spacer(modifier = Modifier.weight(1f))
            Text(stringResource(id = R.string.max_temp),
                style = minMaxTempStyle,
                modifier = Modifier.padding(end = paddingSmall))
            Text("${weatherResult.main.tempMax.roundToInt()} \u2103",
                style = minMaxTempStyle,)
        }
    }
}

@Composable
fun SunriseSunsetInfo(weatherResult: WeatherResult,
                      timestampToDate: (Long, Boolean) -> String) {
    val iconSize = dimensionResource(id = R.dimen.sunrise_sunset_icon_size)
    val padding = dimensionResource(id = R.dimen.padding_tiny)
    Row(modifier = whiteBackground) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(padding)
        ) {
            Text(stringResource(id = R.string.sunrise))
            Image(painter = painterResource(id = R.drawable.sunrise),
                modifier = Modifier.size(iconSize),
                contentDescription = null)
            Text(timestampToDate(weatherResult.sys.sunrise, false))
        }
        Spacer(modifier = Modifier.weight(0.7f))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(padding)
        ) {
            Text(stringResource(id = R.string.sunset))
            Image(painter =  painterResource(id = R.drawable.sunset),
                modifier = Modifier.size(iconSize),
                contentDescription = null)
            Text(timestampToDate(weatherResult.sys.sunset, false))
        }
    }
}
@Composable
fun ChangeThemeSwitch(darkTheme: Boolean,
                      onThemeChanged: (Boolean) -> Unit,
                      modifier: Modifier = Modifier) {
    Switch(checked = darkTheme,
        onCheckedChange = onThemeChanged,
        thumbContent = {
            Icon(painter =
            if (darkTheme) painterResource(id = R.drawable.nightlight_24px)
            else painterResource(id = R.drawable.light_mode_24px),
                tint = MaterialTheme.colorScheme.inversePrimary,
                contentDescription = null)
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.Transparent,
            uncheckedThumbColor = Color.Transparent,
            uncheckedTrackColor = MaterialTheme.colorScheme.inverseSurface,
            checkedTrackColor = MaterialTheme.colorScheme.inverseSurface,
        ),
        modifier = modifier
            .testTag("ChangeTheme")
            .scale(1.5f)
            .padding(20.dp))
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
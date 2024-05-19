package com.example.windspell.components

import android.Manifest
import android.location.Location
import android.os.Looper
import android.view.ViewTreeObserver
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.windspell.R
import com.example.windspell.data.WeatherItem
import com.example.windspell.weather.WeatherResult
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.roundToInt


@Composable
fun DrawerButton(
    modifier: Modifier,
    onDrawerButtonPressed: () -> Unit) {
    IconButton(onClick = onDrawerButtonPressed,
        modifier = modifier.size(50.dp)) {
        Icon(imageVector = Icons.Rounded.Menu,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxSize(),
            contentDescription = null)
    }
}

/**
 * onLocation: (lat, lon)
 */
@Composable
fun TopBar(
    cityInput: String,
    fusedLocationProviderClient: FusedLocationProviderClient?,
    canScrollBackward: Boolean,
    onTextChanged: (String) -> Unit,
    onSuggestionChanged: (Boolean) -> Unit,
    onLocation: (Double, Double) -> Unit,
    onDrawerButtonPressed: () -> Unit) {
    //canScrollBackward is true when scroll down is possible
    val backgroundColor by animateColorAsState(if (canScrollBackward) MaterialTheme.colorScheme.background else Color.Transparent,
        animationSpec = tween(500))
    var locationTrigger by remember {
        mutableStateOf("")
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.top_bar_padding)),
        modifier = Modifier
            .background(backgroundColor)
            .padding(dimensionResource(R.dimen.top_bar_padding))
            .height(dimensionResource(R.dimen.top_bar_height))
            .fillMaxSize()
    ) {
        DrawerButton (
            modifier = Modifier.testTag("TopBarDrawerButton"),
            onDrawerButtonPressed = onDrawerButtonPressed)
        SearchBar(
            cityInput,
            modifier = Modifier.weight(1f),
            onTextChanged = {
                onTextChanged(it)
            },
            onSuggestionChanged = onSuggestionChanged)
        IconButton(onClick = { locationTrigger = UUID.randomUUID().toString() },
            modifier = Modifier.testTag("LocationButton")) {
            Icon(Icons.Filled.LocationOn,
                modifier = Modifier.fillMaxSize(),
                contentDescription = null)
        }
    }
    GetLocation(locationTrigger, onLocation = onLocation)
}
@Composable
fun GetLocation(locationTrigger: String,
    onLocation: (Double, Double) -> Unit) {
    var location by remember {
        mutableStateOf<Location?>(null)
    }
    val context = LocalContext.current
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val lastLocation = locationResult.lastLocation
            if (lastLocation != null) {
                location = lastLocation // Store the new location
                onLocation(lastLocation.latitude, lastLocation.longitude) // Use the updated location
            }
            fusedLocationClient.removeLocationUpdates(this) // Stop updates after receiving a location
        }
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {permissions ->
        if (permissions.values.all { it }) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        location = loc
                    } else {
                        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                    }
                }
            } catch (e: SecurityException) { }
        }
    }

    LaunchedEffect(locationTrigger, location) {
        if (locationTrigger.isNotEmpty()) {
            launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            location?.let {
                onLocation(it.latitude, it.longitude)
            }
        }
    }
}

@Composable
fun SearchBar(
    cityInput: String,
    modifier: Modifier,
    onTextChanged: (String) -> Unit,
    onSuggestionChanged: (Boolean) -> Unit
) {
    var cityConfirmed by rememberSaveable {
        mutableStateOf(true)
    }
    val focusManager = LocalFocusManager.current
    /* If search results are not satisfied, it's worth entering a country code
    e.g: New York, US */
    OutlinedTextField(
        value = cityInput,
        colors = OutlinedTextFieldDefaults.colors(
            disabledBorderColor = MaterialTheme.colorScheme.inversePrimary,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedContainerColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
        ),
        trailingIcon = {
            if (cityInput.isNotBlank()) {
                IconButton(
                    onClick = {
                        onTextChanged("")
                    }) {
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
            onDone = {focusManager.clearFocus()
            if (cityInput.isNotBlank()) {
                onTextChanged(cityInput)
            }
            }
        ),
        maxLines = 1,
        onValueChange = {
            cityConfirmed = false
            onSuggestionChanged(false)
            //don't allow numbers and special characters
            if (!Regex("[0-9]").containsMatchIn(it)) {
                onTextChanged(it)
            }
        },
        placeholder = {Text(stringResource(R.string.search))},
        modifier = modifier
            .fillMaxWidth()
            .testTag("SearchBar"))
}

@Composable
fun SuggestedCity(
    isSaved: Boolean,
    onCityConfirmed: () -> Unit,
    suggestedCity: String) {
    val cornerSize = dimensionResource(R.dimen.suggested_city_corner)
    val shape = RoundedCornerShape(bottomStart = cornerSize, bottomEnd = cornerSize)
    ElevatedButton(onClick = onCityConfirmed,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.background.copy(0.5f)
        ),
        shape = shape,
        modifier = Modifier
            .testTag("SuggestedCity")) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                suggestedCity,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape))
            if (!isSaved) {
                Text(stringResource(R.string.tap_to_add_city),
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityDrawerItem(weatherItem: WeatherItem,
                   onClick: () -> Unit,
                   onDelete: (Int) -> Unit) {
    var show by remember {
        mutableStateOf(true)
    }
    val dismissState = rememberDismissState(
        confirmValueChange = {
            if (it == DismissValue.DismissedToEnd) {
                show = false
                true
            } else false
        },
        positionalThreshold = { 100.dp.toPx() }
    )
    AnimatedVisibility(visible = show,
        exit = slideOutHorizontally(animationSpec = tween(300)) { it } ,
        modifier = Modifier.testTag("CityDrawerItem: ${weatherItem.cityId}")) {
        SwipeToDismiss(state = dismissState,
            background = {
                if (dismissState.dismissDirection == DismissDirection.StartToEnd) {
                    Box(
                        modifier = Modifier
                            .background(Color.Red)
                            .fillMaxSize()
                            .padding(start = 5.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Icon(Icons.Filled.Delete,
                            tint = Color.White,
                            contentDescription = stringResource(R.string.delete))
                    }
                }
            },
            dismissContent = {
                NavigationDrawerItem(
                    shape = RoundedCornerShape(dimensionResource(R.dimen.city_drawer_item_corner)),
                    label = { Text(weatherItem.cityName, overflow = TextOverflow.Ellipsis, fontSize = 20.sp) },
                    selected = false, onClick = onClick,
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = MaterialTheme.colorScheme.onPrimary
                    ))
            },
            modifier = Modifier
                .clip(RoundedCornerShape(dimensionResource(R.dimen.city_drawer_item_corner)))
                .height(50.dp))
        LaunchedEffect(show) {
            if (!show) {
                delay(200)
                onDelete(weatherItem.cityId)
            }
        }
    }
}

@Composable
fun WeatherDetails(
    weatherResult: WeatherResult,
    degreeFormat: String) {
    val weatherConditionIcon = weatherResult.weather.first().icon
    val paddingSmall = dimensionResource(id = R.dimen.padding_small)
    val minMaxTempStyle = MaterialTheme.typography.displaySmall
    Column {
        Box(modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(getWeatherIcon(weatherConditionIcon)),
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.padding_huge)),
                contentDescription = null
            )
        }
        //City + Country name
        Text(
            "${weatherResult.name} " +
                   if (!weatherResult.name.contains(weatherResult.sys.country)) weatherResult.sys.country
            else "",
            style = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Start),
            textAlign = TextAlign.Start
        )
        Column(modifier = Modifier.fillMaxWidth()
            .padding(bottom = dimensionResource(R.dimen.padding_medium))) {
            //Current temperature
            Text(
                "${weatherResult.main.temp.roundToInt()} $degreeFormat",
                style = MaterialTheme.typography.displayMedium,
            )
            //Description
            Text(weatherResult.weather.first().description,
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Start)
        }
        //Min Max Temp
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = whiteBackground
        ) {
            Text(stringResource(id = R.string.min_temp),
                style = minMaxTempStyle,
                modifier = Modifier.padding(end = paddingSmall))
            Text("${weatherResult.main.tempMin.roundToInt()} $degreeFormat",
                style = minMaxTempStyle,)
            Spacer(modifier = Modifier.weight(1f))
            Text(stringResource(id = R.string.max_temp),
                style = minMaxTempStyle,
                modifier = Modifier.padding(end = paddingSmall))
            Text("${weatherResult.main.tempMax.roundToInt()} $degreeFormat",
                style = minMaxTempStyle,)
        }
    }
}

@Composable
fun SunriseSunsetInfo(weatherResult: WeatherResult,
                      timestampToDate: (Long) -> String) {
    val iconSize = dimensionResource(id = R.dimen.sunrise_sunset_icon_size)
    val padding = dimensionResource(id = R.dimen.padding_tiny)
    val timestampStyle = MaterialTheme.typography.labelSmall
    val labelStyle = MaterialTheme.typography.titleSmall
    Row(modifier = whiteBackground,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(padding)
        ) {
            Text(stringResource(id = R.string.sunrise),
                style = labelStyle)
            Image(painter = painterResource(id = R.drawable.sunrise),
                modifier = Modifier.size(iconSize),
                contentDescription = null)
            Text(timestampToDate(weatherResult.sys.sunrise),
                style = timestampStyle)
        }
        //Spacer(modifier = Modifier.weight(0.7f))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(padding)
        ) {
            Text(stringResource(id = R.string.sunset),
                style = labelStyle)
            Image(painter =  painterResource(id = R.drawable.sunset),
                modifier = Modifier.size(iconSize),
                contentDescription = null)
            Text(timestampToDate(weatherResult.sys.sunset),
                style = timestampStyle)
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


@Composable
fun keyboardAsState(): State<Boolean> {
    val view = LocalView.current
    var isImeVisible by remember { mutableStateOf(false) }

    DisposableEffect(LocalWindowInfo.current) {
        val listener = ViewTreeObserver.OnPreDrawListener {
            isImeVisible = ViewCompat.getRootWindowInsets(view)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
            true
        }
        view.viewTreeObserver.addOnPreDrawListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnPreDrawListener(listener)
        }
    }
    return rememberUpdatedState(isImeVisible)

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
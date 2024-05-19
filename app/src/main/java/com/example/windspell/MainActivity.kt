package com.example.windspell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DismissState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.windspell.data.DataStoreManager
import com.example.windspell.network.NetworkStatus
import com.example.windspell.presentation.MainScreen
import com.example.windspell.ui.theme.WindSpellTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var isThemeDark = mutableStateOf(false)
    private var showSplashScreen = mutableStateOf(true)

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    private fun collectShowSplashScreen() {
        lifecycleScope.launch {
            dataStoreManager.collectShowSplashScreen {
                showSplashScreen.value = it
            }
        }
    }
    private fun collectTheme() {
        lifecycleScope.launch {
            dataStoreManager.collectTheme {
                isThemeDark.value = it
            }
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().setKeepOnScreenCondition {
            showSplashScreen.value
        }
        collectTheme()
        collectShowSplashScreen()
        setContent {
            val noInternet = stringResource(R.string.no_internet)
            var networkIsOn by remember {
                mutableStateOf(true)
            }
            val snackbarHostState = remember {
                SnackbarHostState()
            }
            val dismissState = rememberDismissState()
            NetworkStatus {
                networkIsOn = it
            }
            LaunchedEffect(networkIsOn) {
                if (!networkIsOn) {
                    val res = snackbarHostState.showSnackbar(noInternet)
                    if (res == SnackbarResult.Dismissed) {
                        snackbarHostState.currentSnackbarData?.dismiss()
                    }
                }
            }
            WindSpellTheme(useDarkTheme = isThemeDark.value) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        MainScreen(darkTheme = isThemeDark.value,
                            networkIsOn = networkIsOn,
                            maxWidth = this.maxWidth,
                            onThemeChanged = {
                                lifecycleScope.launch {
                                    dataStoreManager.changeTheme(!isThemeDark.value)
                                }}) {
                            lifecycleScope.launch { snackbarHostState.showSnackbar(it) }
                        }
                        CustomSnackbar(networkIsOn, snackbarHostState, dismissState)
                    }
                    }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSnackbar(networkIsOn: Boolean,
                   snackbarHostState: SnackbarHostState,
                   dismissState: DismissState) {
    SnackbarHost(hostState = snackbarHostState,
        snackbar = {data ->
            SwipeToDismiss(
                state = dismissState,
                background = {},
                dismissContent = {
                    Snackbar(
                        containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!networkIsOn) {
                                Icon(painter = painterResource(R.drawable.no_wifi),
                                    modifier = Modifier.size(30.dp),
                                    tint = MaterialTheme.colorScheme.error,
                                    contentDescription = null)
                            }
                            Box(modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center) {
                                Text(data.visuals.message,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(Alignment.Top)
            .padding(10.dp)
            .clip(RoundedCornerShape(20.dp)))
}

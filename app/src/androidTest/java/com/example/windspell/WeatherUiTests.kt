package com.example.windspell

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.windspell.components.MainScreen
import com.example.windspell.ui.theme.WindSpellTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class WeatherUiTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun cityEntered_suggestedCityClicked_weatherShown() = runBlocking {
        composeTestRule.setContent {
            WindSpellTheme {
                MainScreen()
            }
        }
        composeTestRule.onNodeWithTag("SearchBar").assertTextContains("")
        composeTestRule.onNodeWithTag("SearchBar").performTextInput("London GB")
        delay(2000L)
        composeTestRule.onNodeWithTag("SuggestedCity").assertIsDisplayed()
        composeTestRule.onNodeWithTag("SuggestedCity").performClick()
        composeTestRule.onNodeWithTag("WeatherDetails").assertIsDisplayed()
        composeTestRule.onRoot().printToLog("TAG")
    }
}
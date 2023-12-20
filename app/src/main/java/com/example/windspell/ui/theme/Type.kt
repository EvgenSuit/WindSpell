package com.example.windspell.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.windspell.R

val antonFamily = FontFamily(Font(R.font.anton_regular))
val Typography = Typography (
    titleMedium = TextStyle(
        fontFamily = antonFamily,
        fontSize = 50.sp
    ),
    displayMedium = TextStyle(
        fontSize = 25.sp
    )
)
package com.example.windspell.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.windspell.R

val antonFamily = FontFamily(Font(R.font.anton_regular))
val robotoFamily = FontFamily(Font(R.font.roboto_condensed_wght))
val dosisFamily = FontFamily(Font(R.font.dosis_wght))
val Typography = Typography (
    titleMedium = TextStyle(
        fontFamily = robotoFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 60.sp,
        color = Color.White,
        lineHeight = 60.sp,
        textAlign = TextAlign.Center,
    ),
    titleSmall = TextStyle(
        fontFamily = dosisFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        color = Color.White,
        textAlign = TextAlign.Center
    ),
    displayMedium = TextStyle(
        fontFamily = robotoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 35.sp,
        color = Color.White,
        textAlign = TextAlign.Center
    ),
    displaySmall = TextStyle(
        fontSize = 25.sp,
        color = Color.White,
        textAlign = TextAlign.Center
    ),
    labelSmall = TextStyle(
        fontFamily = dosisFamily,
        fontSize = 18.sp,
        color = Color.White,
        textAlign = TextAlign.Center
    ),
    labelMedium = TextStyle(
        fontFamily = dosisFamily,
        fontSize = 21.sp,
        color = Color.White,
        textAlign = TextAlign.Center
    ),
)
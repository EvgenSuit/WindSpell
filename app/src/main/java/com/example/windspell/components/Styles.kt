package com.example.windspell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val whiteBackground = Modifier.clip(RoundedCornerShape(10.dp))
    .background(Color.White.copy(alpha = 0.3f))
    .padding(10.dp)
    .fillMaxWidth()

val colorStopsLight = arrayOf(
    0.0f to Color.Blue,
    0.5f to Color.Blue.copy(alpha = 0.4f),
    1f to Color.Blue.copy(alpha = 0.5f)
)
val colorStopsDark = arrayOf(
    0.0f to Color.Black.copy(alpha = 0.5f),
    0.4f to Color.Black.copy(alpha = 0.6f),
    1f to Color.Black.copy(alpha = 0.4f)
)


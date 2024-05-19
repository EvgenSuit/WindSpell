package com.example.windspell

import android.util.Log
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale

class TimestampToDate {
    operator fun invoke(timestamp: Long): String {
        val locale = Locale.getDefault()
        val isLocaleUS = locale == Locale.US
        val sdf = SimpleDateFormat("${if (isLocaleUS) "hh" else "HH"}:mm:ss${if (isLocaleUS) " a" else ""}", locale)
        return sdf.format(Date(timestamp * 1000))
    }
}

class TimestampToDayOfWeek {
    private fun translatedDayNames(dayOfWeek: DayOfWeek): Int =
        when(dayOfWeek.name) {
            "MONDAY" -> R.string.monday
            "TUESDAY" -> R.string.tuesday
            "WEDNESDAY" -> R.string.wednesday
            "THURSDAY" -> R.string.thursday
            "FRIDAY" -> R.string.friday
            "SATURDAY" -> R.string.saturday
            else -> R.string.sunday }
    operator fun invoke(timestamp: Long): Int {
        val instant = Instant.ofEpochSecond(timestamp)
        val dayOfWeek = DayOfWeek.from(instant.atZone(ZoneId.systemDefault()))
        return translatedDayNames(dayOfWeek)
    }
}

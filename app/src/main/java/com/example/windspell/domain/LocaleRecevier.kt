package com.example.windspell.domain

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Locale

fun localeReceiver(onNewLocale: (Locale) -> Unit) = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_LOCALE_CHANGED) {
            onNewLocale(Locale.getDefault())
        }
    }
}

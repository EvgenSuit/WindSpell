package com.example.windspell.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


class CoroutineScopeProvider(private val coroutineScope: CoroutineScope? = null) {
    fun provideCoroutineScope() = coroutineScope
}
class DispatchProvider(private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    fun provideDispatcher() = dispatcher
}
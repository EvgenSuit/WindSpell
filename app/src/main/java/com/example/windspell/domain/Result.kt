package com.example.windspell.domain

sealed class Result(val error: String = "") {
    data object Idle: Result()
    data object InProgress: Result()
    data object Empty: Result()
    data object Success: Result()
    class Error(error: String): Result(error = error)
}


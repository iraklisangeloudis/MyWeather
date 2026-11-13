package com.example.myweather.data.repository

import retrofit2.HttpException
import java.io.IOException

// Exception messages and response bodies can contain request URLs with API keys.
internal fun requestFailureMessage(error: Throwable): String = when (error) {
    is HttpException -> "API request failed: HTTP ${error.code()}"
    is IOException -> "API request failed: network or connection error"
    else -> "API request failed: ${error.javaClass.simpleName}"
}

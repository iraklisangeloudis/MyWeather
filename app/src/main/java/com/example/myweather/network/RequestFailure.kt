package com.example.myweather.network

import java.io.IOException
import retrofit2.HttpException

// Exception messages and response bodies can contain request URLs with API keys.
internal fun requestFailureMessage(error: Throwable): String = when (error) {
    is HttpException -> "API request failed: HTTP ${error.code()}"
    is IOException -> "API request failed: network or connection error"
    else -> "API request failed: ${error.javaClass.simpleName}"
}

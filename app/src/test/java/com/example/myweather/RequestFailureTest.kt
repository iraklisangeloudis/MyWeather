package com.example.myweather

import com.example.myweather.data.repository.requestFailureMessage
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import okhttp3.ResponseBody
import java.io.IOException

class RequestFailureTest {
    @Test
    fun failuresExcludeSensitiveMessagesAndResponseBodies() {
        val secret = "https://example.test/?api_key=secret"
        assertEquals("API request failed: network or connection error", requestFailureMessage(IOException(secret)))
        assertEquals("API request failed: IllegalStateException", requestFailureMessage(IllegalStateException(secret)))
        val httpError = HttpException(Response.error<Any>(401, ResponseBody.create(null, secret)))
        assertEquals("API request failed: HTTP 401", requestFailureMessage(httpError))
    }
}

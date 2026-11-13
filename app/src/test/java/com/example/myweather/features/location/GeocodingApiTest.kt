package com.example.myweather.features.location

import com.example.myweather.features.location.data.api.GeocodingApi
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GeocodingApiTest {
    @Test
    fun reverseGeocodingUsesProviderAuthenticationParameter() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val url = chain.request().url()
            assertEquals("/reverse", url.encodedPath())
            assertEquals("test-key", url.queryParameter("api_key"))
            assertNull(url.queryParameter("key"))
            assertEquals("37.9838", url.queryParameter("lat"))
            assertEquals("23.7275", url.queryParameter("lon"))
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(null, "{\"address\":{\"city\":\"Athens\"}}"))
                .build()
        }.build()
        val geocodingApi = Retrofit.Builder()
            .baseUrl("https://example.test/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeocodingApi::class.java)

        assertEquals("Athens", geocodingApi.fetchAddress(37.9838, 23.7275, "test-key").address.city)
    }
}

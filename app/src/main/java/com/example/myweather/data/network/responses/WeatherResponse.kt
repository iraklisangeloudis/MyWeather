package com.example.myweather.data.network.responses

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("current") val current: Current,
    @SerializedName("hourly") val hourly: Hourly,
    @SerializedName("daily") val daily: Daily
)

data class Current(
    @SerializedName("time") val time: String,
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("relative_humidity_2m") val humidity: Int,
    @SerializedName("apparent_temperature") val apparentTemperature: Double,
    @SerializedName("is_day") val isDay: Int,
    @SerializedName("weather_code") val weatherCode: Int,
    @SerializedName("wind_speed_10m") val windSpeed: Double
)

data class Hourly(
    @SerializedName("time") val time: List<String>,
    @SerializedName("temperature_2m") val temperature: List<Double>,
    @SerializedName("weather_code") val weatherCode: List<Int>,
    @SerializedName("is_day") val isDay: List<Int>
)

data class Daily(
    @SerializedName("time") val time: List<String>,
    @SerializedName("weather_code") val weatherCode: List<Int>,
    @SerializedName("temperature_2m_max") val temperatureMax: List<Double>,
    @SerializedName("temperature_2m_min") val temperatureMin: List<Double>,
    @SerializedName("sunrise") val sunrise: List<String>,
    @SerializedName("sunset") val sunset: List<String>,
    @SerializedName("precipitation_probability_max") val precipitationProbabilityMax: List<Int>
)

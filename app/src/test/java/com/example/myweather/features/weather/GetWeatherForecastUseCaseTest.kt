package com.example.myweather.features.weather

import com.example.myweather.features.weather.domain.GetWeatherForecastUseCase
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GetWeatherForecastUseCaseTest {
    @Test
    fun displaysNext24HoursRatherThanFirst24HoursOfResponse() = runBlocking {
        val forecastDetails = GetWeatherForecastUseCase()(forecast(), LocalDate.parse("2026-11-13"))
        assertEquals(24, forecastDetails.hourly.size)
        assertEquals("2026-11-13T13:00", forecastDetails.hourly.first().time)
        assertEquals("2026-11-14T12:00", forecastDetails.hourly.last().time)
        assertEquals("Today", forecastDetails.daily.single().day)
        assertEquals("07:00", forecastDetails.sunrise)
    }
    @Test
    fun preservesCheckTimeSeparatelyFromTheModelTimestamp() = runBlocking {
        val checkedAt = 1_800_000_000_000L
        val weather = forecast().copy(checkedAtMillis = checkedAt)
        val forecastDetails = GetWeatherForecastUseCase()(weather, LocalDate.parse("2026-11-14"))
        assertEquals(checkedAt, forecastDetails.checkedAtMillis)
        assertEquals(weather.current.time, forecastDetails.current.time)
    }

}

package com.example.myweather.features.weather

import com.example.myweather.features.weather.domain.GetWeatherForecastUseCase
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GetWeatherForecastUseCaseTest {
    @Test
    fun displaysNext24HoursRatherThanFirst24HoursOfResponse() = runBlocking {
        val ui = GetWeatherForecastUseCase()(forecast(), LocalDate.parse("2026-11-13"))
        assertEquals(24, ui.hourly.size)
        assertEquals("2026-11-13T13:00", ui.hourly.first().time)
        assertEquals("2026-11-14T12:00", ui.hourly.last().time)
        assertEquals("Today", ui.daily.single().day)
        assertEquals("07:00", ui.sunrise)
    }
}

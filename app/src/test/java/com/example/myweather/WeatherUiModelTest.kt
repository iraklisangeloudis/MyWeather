package com.example.myweather

import com.example.myweather.presentation.ui.toUiModel
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class WeatherUiModelTest {
    @Test
    fun displaysNext24HoursRatherThanFirst24HoursOfResponse() {
        val ui = forecast().toUiModel(LocalDate.parse("2026-11-13"))
        assertEquals(24, ui.hourly.size)
        assertEquals("2026-11-13T13:00", ui.hourly.first().time)
        assertEquals("2026-11-14T12:00", ui.hourly.last().time)
        assertEquals("Today", ui.daily.single().day)
        assertEquals("07:00", ui.sunrise)
    }
}

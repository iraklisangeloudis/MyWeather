package com.example.myweather.features.weather.domain

import java.time.Clock
import java.util.concurrent.TimeUnit

class GetWeatherCheckAgeUseCase(private val clock: Clock) {
    operator fun invoke(checkedAtMillis: Long?): Age {
        if (checkedAtMillis == null) return Age.Unknown
        val minutes = TimeUnit.MILLISECONDS.toMinutes((clock.millis() - checkedAtMillis).coerceAtLeast(0))
        return when {
            minutes == 0L -> Age.JustNow
            minutes < 60 -> Age.Minutes(minutes.toInt())
            minutes < 1440 -> Age.Hours((minutes / 60).toInt())
            else -> Age.Days((minutes / 1440).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        }
    }

    sealed interface Age {
        data object Unknown : Age
        data object JustNow : Age
        data class Minutes(val count: Int) : Age
        data class Hours(val count: Int) : Age
        data class Days(val count: Int) : Age
    }
}

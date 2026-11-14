package com.example.myweather.features.weather

import android.app.Application
import com.example.myweather.features.weather.ui.WeatherCheckTimeFormatter
import com.example.myweather.features.weather.domain.GetWeatherCheckAgeUseCase
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "en-rUS")
class WeatherCheckTimeTest {
    private val checkedAt = 1_800_000_000_000L

    @Test fun formatsElapsedTimeAtMinuteHourAndDayBoundaries() {
        val context = RuntimeEnvironment.getApplication()
        val cases = listOf(
            0L to "Checked just now",
            59_999L to "Checked just now",
            60_000L to "Checked 1 minute ago",
            5 * 60_000L to "Checked 5 minutes ago",
            59 * 60_000L to "Checked 59 minutes ago",
            60 * 60_000L to "Checked 1 hour ago",
            2 * 60 * 60_000L to "Checked 2 hours ago",
            24 * 60 * 60_000L to "Checked 1 day ago",
            2 * 24 * 60 * 60_000L to "Checked 2 days ago"
        )
        cases.forEach { (age, expected) ->
            assertEquals(expected, formatter(checkedAt + age).format(context, checkedAt))
        }
    }

    @Test fun unknownCheckTimeDoesNotClaimCachedWeatherWasJustChecked() {
        assertEquals("Saved weather · refresh to check",
            formatter(checkedAt).format(RuntimeEnvironment.getApplication(), null))
    }

    @Test fun backwardsClockAdjustmentDoesNotDisplayNegativeAge() {
        assertEquals("Checked just now",
            formatter(checkedAt - 60_000).format(RuntimeEnvironment.getApplication(), checkedAt))
    }

    private fun formatter(nowMillis: Long) = WeatherCheckTimeFormatter(
        GetWeatherCheckAgeUseCase(Clock.fixed(Instant.ofEpochMilli(nowMillis), ZoneOffset.UTC))
    )
}

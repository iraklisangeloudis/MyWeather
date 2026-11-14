package com.example.myweather.features.weather

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.os.Looper
import androidx.core.graphics.ColorUtils
import com.example.myweather.features.weather.ui.background.WeatherBackgroundLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class WeatherBackgroundLayoutTest {
    @Test fun everyAtmosphereDrawsWithReadableDayAndNightPalettes() {
        Robolectric.buildActivity(Activity::class.java).setup().use { controller ->
            val sky = WeatherBackgroundLayout(controller.get())
            controller.get().setContentView(sky)
            controller.get().window.decorView.visibility = View.VISIBLE
            controller.visible()
            shadowOf(Looper.getMainLooper()).idle()
            sky.layout(0, 0, 390, 800)
            sky.setWeatherArea(400f)
            val bitmap = Bitmap.createBitmap(390, 800, Bitmap.Config.ARGB_8888)
            try {
                listOf(0, 1, 3, 45, 61, 71, 97, -1).forEach { code ->
                    sky.setWeather(code, 1)
                    assertTrue(ColorUtils.calculateLuminance(sky.skyColor) > .5)
                    sky.draw(Canvas(bitmap))
                    if (code >= 0) {
                        sky.setWeather(code, 0)
                        assertTrue(ColorUtils.calculateLuminance(sky.skyColor) < .1)
                        sky.draw(Canvas(bitmap))
                    }
                }
            } finally {
                bitmap.recycle()
                sky.dispose()
            }
        }
    }

    @Test fun motionStopsWhenScreenIsHiddenOrWeatherLeavesTheViewport() {
        Robolectric.buildActivity(Activity::class.java).setup().use { controller ->
            val sky = WeatherBackgroundLayout(controller.get())
            controller.get().setContentView(sky)
            controller.get().window.decorView.visibility = View.VISIBLE
            controller.visible()
            shadowOf(Looper.getMainLooper()).idle()
            sky.layout(0, 0, 390, 800)
            sky.setWeather(61, 1)
            sky.setWeatherArea(400f)
            try {
                assertTrue("Sky must be attached", sky.isAttachedToWindow)
                // Robolectric leaves the synthetic window GONE even after Activity.visible().
                val attachInfo = ReflectionHelpers.getField<Any>(sky, "mAttachInfo")
                ReflectionHelpers.setField(attachInfo, "mWindowVisibility", View.VISIBLE)
                sky.setMotionAllowed(true)
                assertTrue("Visible weather should animate: window=${sky.windowVisibility}, enabled=${android.animation.ValueAnimator.areAnimatorsEnabled()}, height=${sky.height}", sky.isMotionRunning)
                sky.setMotionAllowed(false)
                assertFalse(sky.isMotionRunning)
                sky.setMotionAllowed(true)
                assertTrue(sky.isMotionRunning)
                sky.setWeatherArea(0f)
                assertFalse(sky.isMotionRunning)
                sky.setWeatherArea(400f)
                assertTrue(sky.isMotionRunning)
                ReflectionHelpers.setField(attachInfo, "mWindowVisibility", View.GONE)
                sky.setMotionAllowed(true)
                assertFalse(sky.isMotionRunning)
                ReflectionHelpers.setField(attachInfo, "mWindowVisibility", View.VISIBLE)
                sky.setWeather(null, null)
                assertFalse(sky.isMotionRunning)
            } finally { sky.dispose() }
        }
    }
}

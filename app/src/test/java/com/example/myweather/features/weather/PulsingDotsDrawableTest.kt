package com.example.myweather.features.weather

import android.app.Application
import android.graphics.drawable.Drawable
import android.os.Looper
import com.example.myweather.features.weather.ui.PulsingDotsDrawable
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class PulsingDotsDrawableTest {
    @Test fun animationInvalidatesItsOwnDrawableThroughoutLoadingAndStopsCleanly() {
        val dots = PulsingDotsDrawable(RuntimeEnvironment.getApplication())
        var invalidations = 0
        dots.callback = object : Drawable.Callback {
            override fun invalidateDrawable(who: Drawable) { invalidations++ }
            override fun scheduleDrawable(who: Drawable, what: Runnable, whenMillis: Long) = Unit
            override fun unscheduleDrawable(who: Drawable, what: Runnable) = Unit
        }
        try {
            dots.start()
            assertTrue(dots.isRunning)
            shadowOf(Looper.getMainLooper()).idleFor(200, TimeUnit.MILLISECONDS)
            // Robolectric bounds infinite repeats and auto-advances its frame clock.
            // Multiple drawable invalidations catch the cached-frame regression.
            assertTrue(invalidations > 1)
            dots.stop()
            assertFalse(dots.isRunning)
            val stoppedFrames = invalidations
            shadowOf(Looper.getMainLooper()).idleFor(200, TimeUnit.MILLISECONDS)
            assertEquals(stoppedFrames, invalidations)
        } finally {
            dots.stop()
        }
    }
}

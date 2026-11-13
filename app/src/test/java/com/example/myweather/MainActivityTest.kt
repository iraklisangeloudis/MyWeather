package com.example.myweather

import android.widget.EditText
import com.example.myweather.database.WeatherDatabase
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = WeatherApplication::class)
class MainActivityTest {
    @After fun tearDown() {
        GlobalContext.getOrNull()?.get<WeatherDatabase>()?.close()
        stopKoin()
    }

    @Test fun screenStartsWithProductionDependencyInjection() {
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            assertNotNull(controller.get().findViewById<EditText>(R.id.editTextLocation))
        }
    }
}

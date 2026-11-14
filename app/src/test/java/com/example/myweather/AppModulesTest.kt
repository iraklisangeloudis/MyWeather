package com.example.myweather

import android.app.Application
import androidx.lifecycle.ViewModelStore
import com.example.myweather.database.WeatherDatabase
import com.example.myweather.features.location.data.CityNameRepository
import com.example.myweather.features.location.data.LocationRepository
import com.example.myweather.features.location.domain.GetCurrentLocationUseCase
import com.example.myweather.features.weather.data.WeatherRepository
import com.example.myweather.features.weather.data.WeatherCacheNotifier
import com.example.myweather.features.weather.domain.GetWeatherForecastUseCase
import com.example.myweather.features.weather.domain.GetWeatherCheckAgeUseCase
import com.example.myweather.features.weather.ui.WeatherCheckTimeFormatter
import com.example.myweather.features.weather.domain.RefreshSavedWeatherUseCase
import com.example.myweather.features.weather.widget.WeatherWidgetDataSource
import com.example.myweather.features.weather.ui.WeatherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AppModulesTest {
    @Test fun productionModulesResolveTheScreenAndShareRepositories() {
        Dispatchers.setMain(StandardTestDispatcher())
        val app = koinApplication {
            androidContext(RuntimeEnvironment.getApplication())
            modules(appModules)
        }
        val store = ViewModelStore()
        try {
            val koin = app.koin
            assertSame(koin.get<WeatherRepository>(), koin.get<WeatherRepository>())
            assertSame(koin.get<WeatherCacheNotifier>(), koin.get<WeatherCacheNotifier>())
            assertNotNull(koin.get<LocationRepository>())
            assertNotNull(koin.get<CityNameRepository>())
            assertNotNull(koin.get<GetCurrentLocationUseCase>())
            assertNotNull(koin.get<GetWeatherForecastUseCase>())
            assertNotNull(koin.get<GetWeatherCheckAgeUseCase>())
            assertNotNull(koin.get<WeatherCheckTimeFormatter>())
            assertNotNull(koin.get<RefreshSavedWeatherUseCase>())
            assertNotNull(koin.get<WeatherWidgetDataSource>())
            store.put("weather", koin.get<WeatherViewModel>())
        } finally {
            store.clear()
            app.koin.get<WeatherDatabase>().close()
            app.close()
            Dispatchers.resetMain()
        }
    }
}

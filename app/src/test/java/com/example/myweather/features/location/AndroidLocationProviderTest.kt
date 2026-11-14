package com.example.myweather.features.location

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.example.myweather.features.location.data.AndroidLocationProvider
import com.example.myweather.features.location.domain.model.Coordinates
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], application = Application::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AndroidLocationProviderTest {
    private val context = RuntimeEnvironment.getApplication()
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val provider = AndroidLocationProvider(context)

    @Test fun acceptsApproximatePermissionAndUsesNetworkEvenWithGpsEnabled() = runTest {
        shadowOf(context).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(context).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        assertTrue(provider.hasPermission())
        assertTrue(provider.isEnabled())
        val result = async { provider.getLocation() }
        runCurrent()
        shadowOf(manager).simulateLocation(Location(LocationManager.NETWORK_PROVIDER).apply {
            latitude = 37.98
            longitude = 23.72
        })
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(Coordinates(37.98, 23.72), result.await())
    }

    @Test fun approximatePermissionDoesNotTreatGpsAloneAsAnAvailableProvider() {
        shadowOf(context).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(context).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, false)
        assertTrue(provider.hasPermission())
        assertFalse(provider.isEnabled())
    }

    @Test fun acceptsPrecisePermissionAndRejectsMissingPermission() {
        shadowOf(context).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        assertFalse(provider.hasPermission())
        shadowOf(context).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        assertTrue(provider.hasPermission())
        assertTrue(provider.isEnabled())
    }
}

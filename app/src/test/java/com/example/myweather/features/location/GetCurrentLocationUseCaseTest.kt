package com.example.myweather.features.location

import com.example.myweather.features.location.domain.DeviceLocationProvider
import com.example.myweather.features.location.domain.GetCurrentLocationUseCase
import com.example.myweather.features.location.domain.model.Coordinates
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GetCurrentLocationUseCaseTest {
    private class Provider(
        private val permitted: Boolean = true,
        private val enabled: Boolean = true,
        private val lookup: suspend () -> Coordinates? = { Coordinates(37.98, 23.72) }
    ) : DeviceLocationProvider {
        var lookups = 0
        override fun hasPermission() = permitted
        override fun isEnabled() = enabled
        override suspend fun getLocation(): Coordinates? {
            lookups++
            return lookup()
        }
    }

    @Test fun permissionAndProviderChecksAvoidStartingLocationListener() = runTest {
        val denied = Provider(permitted = false)
        assertEquals(GetCurrentLocationUseCase.Result.PermissionRequired, GetCurrentLocationUseCase(denied)())
        assertEquals(0, denied.lookups)
        val disabled = Provider(enabled = false)
        assertEquals(GetCurrentLocationUseCase.Result.Disabled, GetCurrentLocationUseCase(disabled)())
        assertEquals(0, disabled.lookups)
    }

    @Test fun returnsResolvedCoordinates() = runTest {
        assertEquals(GetCurrentLocationUseCase.Result.Located(Coordinates(37.98, 23.72)), GetCurrentLocationUseCase(Provider())())
    }

    @Test fun timeoutCancelsLocationListener() = runTest {
        var cancelled = false
        val provider = Provider {
            try {
                awaitCancellation()
            } finally {
                cancelled = true
            }
        }
        assertEquals(GetCurrentLocationUseCase.Result.Unavailable, GetCurrentLocationUseCase(provider)())
        assertTrue(cancelled)
        assertEquals(15_000, testScheduler.currentTime)
    }
}

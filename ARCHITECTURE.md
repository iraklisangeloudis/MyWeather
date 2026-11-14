# Architecture

MyWeather uses Kotlin, Views/XML, Koin, Retrofit, Room, coroutines, and Flow. Jetpack Glance provides the home-screen widget.

## Structure and responsibilities

- `features/weather/`: weather API/cache, use cases, ViewModel, screen, widget, and Koin bindings.
- `features/location/`: search/reverse geocoding, device-location provider, use case, permission host, and Koin bindings.
- `database/`: Room database and migrations.
- `network/`: API clients and safe failure messages.

`MainActivity` hosts `WeatherUI` and handles Android permission requests through `LocationPermissionHost`. It requests coarse and fine location together using the Activity Result API and accepts either grant.

`WeatherUI` owns views, listeners, rendering, and lifecycle cleanup. Its Activity-scoped `WeatherViewModel` exposes one `StateFlow<WeatherViewState>` and coordinates weather/search requests, cancelling stale work. Koin modules wire dependencies; repositories handle API/cache work on IO, and forecast preparation runs on Default.

## Data and screen behavior

- Launch reads cached weather without requesting location or fetching weather. Resuming rereads the cache to reflect widget refreshes.
- Pull-to-refresh uses device location; selecting a search result fetches that city. Approximate permission uses network location; precise permission permits GPS. Lookup is cancellable with a 15-second timeout.
- Failed refreshes retain the displayed forecast. Location-label updates apply only when coordinates still match the saved forecast.
- `WeatherCacheDao` reads/replaces the complete current/hourly/daily snapshot in `@Transaction` methods. Repositories receive the DAO through DI. Schema migrations preserve existing data.
- Successful fetches persist `checkedAtMillis` atomically with the snapshot. Cache reads and failures never advance it; legacy caches keep an unknown check time until refreshed.
- `GetWeatherCheckAgeUseCase` uses an injected clock; `WeatherCheckTimeFormatter` supplies localized relative labels. Only the app displays check age, recalculated once a minute while visible.

Screen animations stop when inactive and respect reduced-motion/accessibility settings. Weather artwork attribution is in `THIRD_PARTY_NOTICES.md`.

## Widget

All widget instances follow the single saved location. Glance reads cached forecasts; Room Flow updates active sessions. After cache writes, an injected `WeatherCacheNotifier` schedules rendering without making widget failures fail the weather save.

Manual and hourly WorkManager refreshes use saved coordinates through `RefreshSavedWeatherUseCase`, without device-location access. Android may defer periodic work. Removing the last widget cancels widget work. Refresh failures retain cached weather. The refresh control stays in place and shows a spinner while loading; weather artwork is static.

## Verification

Use JDK 17. The app compiles against and targets API 36; minimum API is 26. Gradle files are the source of truth for dependency versions.

```shell
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Use `gradlew.bat` on Windows. Tests use fakes and Robolectric; device/live API checks are separate. GitHub Actions runs the same checks with placeholder API keys.

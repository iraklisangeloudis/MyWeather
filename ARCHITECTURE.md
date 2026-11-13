# Architecture

MyWeather uses Kotlin, Views/XML, Koin, Retrofit, Room, and coroutine Flow. Code is grouped by feature:

```text
features/weather/   # data (API, DAOs, entities), domain, ui, Koin module
features/location/  # data (APIs, Android provider), domain, Koin module
database/           # Room database and migrations
network/            # Retrofit clients and safe failure messages
```

## Responsibilities

- `MainActivity` hosts the screen and handles Android permission dialogs/results.
- `WeatherUI` owns view creation, listeners, rendering, and lifecycle cleanup. It accesses the ViewModel through the Activity's ViewModelStore, preserving state across recreation.
- `WeatherViewModel` exposes one `StateFlow<WeatherViewState>` and coordinates weather, search, and cache updates. Flow operators cancel stale requests; there are no manually managed Job fields.
- Use cases prepare display data and limit device-location lookup to 15 seconds. Repositories handle API/cache work on IO; forecast preparation runs on Default. API methods use `fetch...`; one-time DAO reads use `get...`.

## Screen behavior

Launch restores cached weather without requesting location or refreshing. Pull-to-refresh uses current device coordinates; selecting a search result fetches that city. Refresh failures retain the existing forecast.

Scroll position expands the details, sunrise/sunset, and daily cards. Reduced-motion and screen-reader modes show full cards. Day/night colors follow the current forecast. Long location names wrap to two lines; tapping reveals the full location.

The pull indicator uses animated dots, with explicit drawable invalidation and lifecycle cleanup. Search uses city/region rows and text for loading, empty, and failure states.

Room schema version 2 preserves existing data through migration. Cached location labels are updated only when their coordinates match the stored forecast.

## Verification

Use JDK 17; Gradle's daemon JVM configuration pins it.

```shell
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

On Windows, use `gradlew.bat`. Tests use fakes, intercepted requests, and Robolectric; live API and device checks are separate.

GitHub Actions runs tests, lint, and the debug build on pushes, pull requests, and manual dispatch. CI uses placeholder API keys, so its APK is a build artifact rather than a configured weather app.

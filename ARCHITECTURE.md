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

The pull indicator uses animated dots, with explicit drawable invalidation and lifecycle cleanup. Search expands from its icon inside the header, preserving header height; closing reverses the transition. Reduced-motion mode switches immediately. Results use city/region rows and text for loading, empty, and failure states.

Room schema version 2 preserves existing data through migration. Cached location labels are updated only when their coordinates match the stored forecast.

## Home-screen widget

`features/weather/widget/` uses Jetpack Glance alongside the Views/XML screen. All widget instances follow the app's single saved location. The responsive layout shows current conditions and high/low, adding three hourly readings when widened. Meteocons are rendered to small static bitmaps; widgets do not animate.

The widget reads Room through the existing repository and forecast use case. Room Flow keeps active Glance sessions current. Successful cache writes enqueue a render worker through a callback wired in Koin; widget scheduling failures do not fail weather saves. Tapping the widget opens MainActivity. Refresh actions use WorkManager and `RefreshSavedWeatherUseCase` with saved coordinates, preserving the location label. No background location permission or device-location lookup is used.

While widgets are installed, unique hourly work refreshes on an available network; Android may defer execution. Removing the last widget cancels widget work. Failed refreshes keep cached weather and show a failure label. Empty or migrated caches without coordinates direct users to the app. Glance 1.1.1 and Compose compiler 1.5.3 retain compatibility with Kotlin 1.9.10; the app screen remains Views/XML.

## Verification

Use JDK 17; Gradle's daemon JVM configuration pins it.

```shell
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

On Windows, use `gradlew.bat`. Tests use fakes, intercepted requests, and Robolectric; live API and device checks are separate.

GitHub Actions runs tests, lint, and the debug build on pushes, pull requests, and manual dispatch. CI uses placeholder API keys, so its APK is a build artifact rather than a configured weather app.

Meteocons Flat artwork is bundled as local Lottie resources. Only the current-condition icon animates; forecast/detail icons use static frames. Parsing is asynchronous and cached, and the hero pauses while search is open or the screen is stopped. System animation settings are respected. See `THIRD_PARTY_NOTICES.md` for source and license information.

`WeatherBackgroundLayout` draws a native sky from the displayed weather code and day/night flag, with subtle clouds, precipitation, fog, or clear-night stars. Effects fade above forecast cards and pause off screen, during search, or when the screen stops. Reduced motion keeps a static sky; no additional requests or assets are needed.

# Architecture

MyWeather uses Kotlin, Views/XML, Koin, Retrofit, Room, and coroutine Flow. Code is grouped by feature:

```text
features/weather/   # data (API, DAOs, entities), domain, ui, Koin module
features/location/  # data (APIs, Android provider), domain, Koin module
database/           # Room database and migrations
network/            # Retrofit clients and safe failure messages
```

## Responsibilities

- `MainActivity` hosts the screen and implements `LocationPermissionHost` to handle Android permission dialogs/results. `WeatherUI` requests access through that interface; the activity forwards permission results to the screen.
- `WeatherUI` owns view creation, listeners, rendering, and lifecycle cleanup. It accesses the ViewModel through the Activity's ViewModelStore, preserving state across recreation.
- `WeatherViewModel` exposes one `StateFlow<WeatherViewState>` and coordinates weather, search, and cache updates. Flow operators cancel stale requests; there are no manually managed Job fields.
- Use cases prepare display data and limit device-location lookup to 15 seconds. Repositories handle API/cache work on IO; forecast preparation runs on Default. API methods use `fetch...`; one-time DAO reads use `get...`.

## Screen behavior

Launch restores cached weather without requesting location or refreshing. Pull-to-refresh uses current device coordinates; selecting a search result fetches that city. Refresh failures retain the existing forecast.

Scroll position expands the details, sunrise/sunset, and daily cards. Reduced-motion and screen-reader modes show full cards. Day/night colors follow the current forecast. Long location names wrap to two lines; tapping reveals the full location.

The pull indicator uses animated dots, with explicit drawable invalidation and lifecycle cleanup. Search expands from its icon inside the header, preserving header height; closing reverses the transition. Reduced-motion mode switches immediately. Results use city/region rows and text for loading, empty, and failure states.

Room schema version 2 preserves existing data through migration. Cached location labels are updated only when their coordinates match the stored forecast.

`WeatherCacheDao` groups the three weather tables. Its `@Transaction` methods read and replace complete forecast snapshots atomically. Koin provides the DAO to the repository and widget data source; the repository does not access `WeatherDatabase` directly.

## Home-screen widget

`features/weather/widget/` uses Jetpack Glance alongside the Views/XML screen. All widget instances follow the app's single saved location. Compact layouts show current conditions; taller cards add high/low and hourly readings. Wide layouts place current conditions beside three or four daily forecast rows. Day, cloudy, and night gradients follow the displayed forecast. `WeatherWidgetContent` owns presentation while `WeatherWidget` prepares cached snapshots and small static Meteocons bitmaps; weather artwork does not animate. The refresh control keeps a fixed size and shows a native progress spinner while refreshing.

The widget reads Room through the existing repository and forecast use case. Room Flow keeps active Glance sessions current. The repository notifies an injected `WeatherCacheNotifier` after successful cache writes. Koin binds this interface to `WeatherWidgetCacheNotifier`, which schedules rendering; widget scheduling failures do not fail weather saves. The module only wires dependencies. Tapping the widget opens MainActivity. `RefreshWeatherAction` sets the tapped widget's loading state and requests a redraw before scheduling refresh work, ignoring repeat taps while loading. Scheduling failures restore the failure state. Refresh actions use WorkManager and `RefreshSavedWeatherUseCase` with saved coordinates, preserving the location label. No background location permission or device-location lookup is used.

While widgets are installed, unique hourly work refreshes on an available network; Android may defer execution. Removing the last widget cancels widget work. Failed refreshes keep cached weather and show a failure label. Empty or migrated caches without coordinates direct users to the app. Glance 1.1.1 uses the Compose compiler Gradle plugin, versioned together with Kotlin at 2.3.21; the app screen remains Views/XML.

## Verification

Use JDK 17; Gradle's daemon JVM configuration pins it. Kotlin and its Compose compiler Gradle plugin are both pinned to 2.3.21. Room 2.8.4 processes Kotlin metadata through kapt; the schema remains version 2.

```shell
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

On Windows, use `gradlew.bat`. Tests use fakes, intercepted requests, and Robolectric; live API and device checks are separate.

GitHub Actions runs tests, lint, and the debug build on pushes, pull requests, and manual dispatch. CI uses placeholder API keys, so its APK is a build artifact rather than a configured weather app.

Meteocons Flat artwork is bundled as local Lottie resources. Only the current-condition icon animates; forecast/detail icons use static frames. Parsing is asynchronous and cached, and the hero pauses while search is open or the screen is stopped. System animation settings are respected. See `THIRD_PARTY_NOTICES.md` for source and license information.

`WeatherBackgroundLayout` draws a native sky from the displayed weather code and day/night flag, with subtle clouds, precipitation, fog, or clear-night stars. Effects fade above forecast cards and pause off screen, during search, or when the screen stops. Reduced motion keeps a static sky; no additional requests or assets are needed.

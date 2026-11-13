# Architecture

MyWeather groups related code by feature and separates data access, domain logic, and UI responsibilities. It uses Retrofit, Room, coroutine Flow, and Koin.

```text
features/
  weather/
    data/                 # Repository, cache migration, API, DAOs and persisted models
      api/model/          # Remote DTOs
      dao/
      model/              # Room entities
    domain/
      model/              # Plain forecast values, independent of Android
      GetWeatherForecastUseCase.kt
    ui/                   # WeatherViewModel, WeatherViewState, WeatherUI and adapters
    WeatherModule.kt
  location/
    data/                 # API repositories and AndroidLocationProvider
      api/model/
    domain/
      model/
      DeviceLocationProvider.kt
      GetCurrentLocationUseCase.kt
    LocationModule.kt
database/                 # Shared Room database
network/                  # Retrofit clients and safe failure diagnostics
AppModules.kt             # Application module composition
WeatherApplication.kt     # Koin startup
MainActivity.kt            # Lifecycle, permission prompts and UI event wiring
```

Koin provides shared APIs, repositories, the database and location provider; use cases are factories and the ViewModel uses Koin's lifecycle integration. Feature modules declare their own dependencies. There is no manual application container or ViewModel factory.

The domain contains plain Kotlin values and use cases. `GetWeatherForecastUseCase` prepares the next 24 forecast hours and daily display values on Default. `GetCurrentLocationUseCase` checks provider readiness and bounds location lookup to 15 seconds. Android location callbacks live in the data layer; permission dialogs stay in the Activity.

The ViewModel exposes one `StateFlow<WeatherViewState>`. Search uses `transformLatest` with the debounce inside the transform, so new input immediately cancels previous work. Selections use `flatMapLatest`. `WeatherUI` renders state and forwards selections; it does not own application state. The Activity collects state with `repeatOnLifecycle(STARTED)` and device-location events with `collectLatest`. There are no manually managed Job fields.

Room is the weather cache. Refreshes replace a full snapshot in one transaction. Existing SharedPreferences weather snapshots are migrated once and removed after a successful database write. Cache work runs on IO. API requests use `fetch...`; one-time DAO reads use `get...`.

Use JDK 17. Run `gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`. Tests follow the feature packages and cover API authentication, search/refresh cancellation, forecast preparation, location timeout, cache restoration/migration/rollback, rendering, Koin wiring, and application startup. Android-dependent tests run locally with Robolectric; they do not replace device testing.

## Continuous integration

`.github/workflows/android-ci.yml` runs on pushes, pull requests, and manual dispatch. It uses Temurin 17, the Gradle wrapper, and the Android SDK to run unit tests, lint, and the debug build. Reports are retained for 14 days and the debug APK for 7 days. The Gradle setup action caches dependencies and validates the wrapper.

CI writes dummy API-key properties, so it needs no repository secrets and also works for forked pull requests. Tests use fakes or intercepted requests. The uploaded APK contains placeholder keys; it is a build artifact, not a fully configured weather app. Live-provider checks and emulator/device tests are separate from this workflow.

`gradle/gradle-daemon-jvm.properties` selects Java 17 for Gradle on development machines and CI. The Foojay resolver can provision that JDK if it is missing. Local `.kotlin/` and `.gradle/` files are ignored.

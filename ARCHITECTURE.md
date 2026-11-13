# Architecture

- `data/remote/api`: Retrofit HTTP contracts; remote requests use `fetch…` names.
- `data/remote/dto`: API response models, mapped to plain values in `model`.
- `data/local/db/dao`: Room operations; one-time reads use `get…` names.
- `data/repository`: coordinates remote requests and local persistence. `refreshWeather` replaces the complete Room forecast in one transaction; `getCachedWeather` restores it.
- `presentation/viewmodel`: exposes a single `StateFlow<WeatherScreenState>`. Search uses `transformLatest` with its debounce inside the transform, so new input cancels the previous request immediately. Location selections use `flatMapLatest`.
- `presentation/ui`: `WeatherRenderer` applies prepared screen state to views and reports user selections. It does not own application state.
- `WeatherApplication` / `AppContainer`: creates shared dependencies and keeps API keys out of the UI.

Room is the weather cache. Existing SharedPreferences weather snapshots are migrated once and removed after a successful database write. Cache reads/writes run on IO; forecast preparation runs on Default; view updates run on Main. Device-location requests use a conflated event channel collected with `collectLatest`, the Activity lifecycle, and a 15-second timeout. Screen state is collected with `repeatOnLifecycle(STARTED)`. There are no manually managed `Job` fields.

Use JDK 17 to build this project. Run `gradlew.bat :app:testDebugUnitTest :app:assembleDebug` for the unit tests and debug APK. Tests cover API authentication parameters, search/refresh cancellation, forecast preparation, cache restoration, legacy-cache migration, and transaction rollback. Room tests run locally with Robolectric.

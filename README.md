Overview
WeatherApp is an Android application built using Kotlin that provides real-time weather information. It uses Open-Meteo for weather data and LocationIQ for location services, including autocomplete and reverse geocoding. The app leverages Retrofit for seamless API integration.

Features
Real-time Weather Updates: Get the current weather information for your location or any searched location.
Location Search: Autocomplete feature to search for locations.
Reverse Geocoding: Convert coordinates to a readable address.
User-friendly Interface: Simple and intuitive design for easy navigation and use.
Tech Stack
Kotlin: Programming language used for Android development.
Android Studio: IDE used for developing the app.
Open-Meteo API: Provides weather data.
LocationIQ API: Used for location search (autocomplete) and reverse geocoding.
Retrofit: HTTP client for API requests.
Prerequisites
Minimum SDK = 26
Internet connection for API requests

API Keys: Obtain your API keys from Open-Meteo and LocationIQ!
LOCATIONIQ_API_KEY=your_locationiq_api_key
OPEN_METEO_API_KEY=your_open_meteo_api_key

Project Structure
WeatherApp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/weatherapp/
│   │   │   │   ├── api/
│   │   │   │   ├── model/
│   │   │   │   ├── repository/
│   │   │   │   ├── ui/
│   │   │   │   ├── utils/
│   │   │   │   └── WeatherApp.kt
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── values/
│   │   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── local.properties

Dependencies
dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'androidx.core:core-ktx:1.6.0'
    implementation 'androidx.appcompat:appcompat:1.3.1'
    implementation 'com.google.android.material:material:1.4.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.0'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.3.1'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.3.1'
}

License
This project is licensed under the MIT License - see the LICENSE file for details.

Acknowledgments
Open-Meteo for providing the weather API.
LocationIQ for location services API.
Retrofit for simplifying API integration.
The Android community for their continuous support and resources.

# DriveMetrics

## About

DriveMetrics is an Android app that turns a car's data into a live driving dashboard and a
driving score. It records trips automatically, shows where you lost points, estimates fuel and
money wasted by harsh driving, and compares you with a simulated benchmark driver.

- **Drive**: live speed, speed limit, score and trip status. Read-only.
- **Map**: the current route, or the last trip while parked.
- **Trips**: history with scores, and trip details with events, fuel estimate and tips.
- **Insights**: your score against a simulated benchmark, and fuel and money estimates.
- **Account**: sign in, sign up or try the demo; vehicle settings; crash alert demo.

Trips start and end by themselves from the car's data, so there is no start or end button.
For now the data comes from a built-in car simulator, so the whole app works without a car,
GPS or server. Fuel, money and benchmark values are estimates and are labelled as such.

<p>
  <img src="docs/screenshots/drive.png" width="22%" alt="Drive">
  <img src="docs/screenshots/trips.png" width="22%" alt="Trips">
  <img src="docs/screenshots/trip-detail.png" width="22%" alt="Trip detail">
  <img src="docs/screenshots/settings.png" width="22%" alt="Settings">
</p>

## Tech stack

- Kotlin
- Jetpack Compose and Material 3
- Navigation Compose (type-safe routes)
- ViewModel, StateFlow and coroutines
- Kotlin serialization (for navigation routes)
- Gradle with a version catalog
- JUnit and Compose UI tests

## Project structure

```
gpstracker/
├── domain/                 Pure Kotlin: telemetry models and driving rules
├── app/                    The Android app
│   └── src/main/java/com/example/gpstracker/
│       ├── data/
│       │   ├── estimate/   Trip detection, fuel estimate, benchmark, insights
│       │   ├── auth/       Account interface
│       │   └── fake/       Car simulator, fake accounts, sample data
│       └── ui/
│           ├── theme/      Colors, fonts, shapes
│           ├── model/      Models the screens display
│           ├── components/ Reusable pieces: gauge, charts, cards, banners
│           ├── navigation/ Routes and bottom bar
│           └── auth, drive, map, trips, tripdetail, insights, settings, crash
├── docs/                   Screenshots and font license
└── app/UI_SPEC.md          Full UI specification
```

Each screen has a stateless `Screen` composable, a `Route` that connects it to its
`ViewModel`, and a `ViewModel` that exposes one immutable state.

## Run the tests

Unit tests (no device needed):

```bash
./gradlew :domain:test :app:testDebugUnitTest
```

UI tests (an emulator or a phone must be connected):

```bash
./gradlew connectedDebugAndroidTest
```

## Run the app

Requirements: Android Studio (or JDK 17 and the Android SDK) and an emulator or a phone.

**Emulator**

1. Open the project in Android Studio and let Gradle sync.
2. Create a device in **Tools → Device Manager** and start it.
3. Choose it in the device dropdown and press **Run**.

**Physical phone**

1. On the phone: **Settings → About phone**, tap **Build number** 7 times, then turn on
   **USB debugging** in **Developer options**.
2. Connect it with a USB cable and accept the debugging prompt on the phone.
3. Check that `adb devices` lists it, then choose it in Android Studio and press **Run**.

**From a terminal** (with a device connected or an emulator running):

```bash
./gradlew installDebug
```

Then open **DriveMetrics**. Choose **Try the demo**, or sign in with
`demo@drivemetrics.app` / `demo1234`. A full demo loop takes about three minutes: the car
parks, a trip starts by itself, the car drives and the trip ends by itself.

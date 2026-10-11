# DriveMetrics

An Android app that turns a car's telemetry (speed, position, acceleration) into a
live driving dashboard and a driving score. It is built **UI-first**: the screens run on
a built-in simulator, so the whole app can be demoed without a car, GPS or server.

<p>
  <img src="docs/screenshots/drive.png" width="22%" alt="Drive dashboard">
  <img src="docs/screenshots/trips.png" width="22%" alt="Trips history">
  <img src="docs/screenshots/trip-detail.png" width="22%" alt="Trip detail">
  <img src="docs/screenshots/settings.png" width="22%" alt="Settings">
</p>

## What it does

| Screen | What you see |
|---|---|
| **Drive** | Read-only live dashboard: speed gauge, limit sign, score, distance/time/events, trip status; the last trip while parked |
| **Map** | The route so far with event markers, or the last trip dimmed while parked (drawn on a canvas, no API key) |
| **Trips** | Weekly average score, a sparkline, the trip in progress and the history grouped by day |
| **Trip detail** | Score, stats, route with event markers, "where you lost points", fuel estimate and coaching tips |
| **Insights** | You against a simulated benchmark driver: score trend, events per 100 km, fuel and money estimate |
| **Welcome, Sign in, Sign up** | Account flow with validation, privacy consent and a "Try the demo" guest mode |
| **Vehicle setup** | Fuel type, consumption, fuel price and monthly distance, used for estimates |
| **Settings** | Account, vehicle, data source, speeding tolerance, crash detection, privacy, how estimates work |
| **Crash alert** | Full-screen 10-second countdown with "I'm OK" and "Send alert now" |

## Demo mode

Choose **Try the demo** on the welcome screen, or sign in with the demo account
`demo@drivemetrics.app` / `demo1234` (accounts are in memory only and reset when the app closes).

There is **no start or end trip button**. The app always shows live data from the car, and trips
start and end by themselves from the car's data (ignition and movement). The simulator is a
simulated car that runs one loop of about three minutes, with nothing to press:

1. the car is parked, then the ignition turns on and a trip starts by itself,
2. it drives through Kaunas: speeding, an event banner, a traffic-light stop that does **not**
   split the trip, a reconnecting banner and a GPS-lost banner,
3. it arrives, the ignition turns off, the trip ends by itself and a "Trip saved" banner
   appears; the trip shows up in **Trips** and changes **Insights**.

Demo time runs ten times faster than real time so the detection rules behave realistically.
In debug builds, **Settings → About → Skip to next drive** jumps the simulated car to its next
drive; it controls the car, never the trip.

### How trips are detected

`TripDetector` (`data/estimate/TripDetector.kt`) is a small, unit-tested state machine
(`Parked → Driving ⇄ Stopped → Parked`):

| Rule | Value |
|---|---|
| Trip starts | ignition on and speed above 5 km/h (without ignition data: above 5 km/h for 10 s) |
| Short stops (lights, jams) | do not end the trip; it ends after the car stands still for 2, 5 or 10 min (Settings) |
| Ignition off | ends the trip after a 30 s grace period for quick restarts |
| No data for 10 min | ends the trip at the last sample, marked "ended without signal" |
| Too short | trips under 0.3 km or 60 s are discarded |

### Open decision: when the app is closed

The UI phase only shows live data while the app is open. For the real version choose one, and
ask the teacher whether a server-side database is acceptable:

- **A. Server keeps the data (recommended):** the relay server stores telemetry; the app catches
  up on missed samples when it opens or reconnects and runs `TripDetector` over them.
- **B. Foreground service on the phone:** keeps the connection open with a permanent
  notification; works without a server database but Android restricts it and it drains battery.

### Honest numbers

Fuel, money and CO2 values are **estimates** from a simple, documented model
(`data/estimate/FuelModelConfig.kt`). The benchmark is a **simulated** typical driver built from
fixed assumptions, not data from real drivers, and the UI always says so.

## Project structure

```
gpstracker/
├── domain/   Pure Kotlin. Telemetry models, driving rules, no Android code.
└── app/      Android app: Jetpack Compose UI, ViewModels, fake data.
    └── ui/
        ├── theme/        Colors, fonts, shapes (dark only)
        ├── model/        UI models, already formatted for display
        ├── components/   Reusable pieces: gauge, sign, badges, chips, map
        ├── drive/ map/ trips/ tripdetail/ settings/ crash/
        │                 One folder per screen: Screen, Route and ViewModel
        └── navigation/   Routes, nav graph, bottom bar
```

### How the pieces talk

```
FakeDriveSimulator ──▶ ViewModel ──▶ Route ──▶ Screen
   (fake data)        (state)       (glue)    (pure UI)
```

- A **Screen** is stateless: it takes a `UiState` plus callbacks, and nothing else.
- A **Route** collects the ViewModel's `StateFlow` and passes it to the Screen.
- The **ViewModel** exposes the simulator's state and forwards button presses.
- Later the simulator is swapped for the real domain pipeline and no Screen changes.

## Tech

Kotlin, Jetpack Compose, Material 3, Navigation Compose (type-safe routes),
Kotlin coroutines and Flow. Barlow fonts (SIL OFL, see `docs/FONT_LICENSE.txt`).

## Build and run

Requirements: Android Studio (or JDK 17 and the Android SDK) and an emulator or phone.

```bash
./gradlew assembleDebug        # build
./gradlew installDebug         # install on the connected device or emulator
./gradlew :domain:test :app:testDebugUnitTest   # unit tests
./gradlew connectedDebugAndroidTest             # UI tests (device needed)
```

## Run it on your computer (emulator)

1. Open the project in Android Studio and let Gradle sync.
2. **Tools → Device Manager → Create device** (any phone, API 34 or newer image).
3. Start the emulator, pick it in the device dropdown and press **Run**.

From a terminal, with an emulator already running: `./gradlew installDebug`.

## Run it on a physical phone

The app works fully offline (the data is simulated), so a phone and a USB cable are all you need.
It supports Android 7.0 (API 24) and newer.

**1. Prepare the phone (once)**

1. **Settings → About phone**, tap **Build number** 7 times to enable Developer options.
2. **Settings → System → Developer options**, turn on **USB debugging**.
3. Plug the phone into the computer with a data-capable USB cable.
4. Accept the **Allow USB debugging?** prompt on the phone (tick *Always allow*).

**2. Check the computer sees it**

```bash
adb devices
```

Your phone should be listed with the word `device` next to it.
`adb` lives in `<Android SDK>/platform-tools`
(on Windows usually `%LOCALAPPDATA%\Android\Sdk\platform-tools`).

**3. Install and launch**

- Android Studio: choose the phone in the device dropdown and press **Run**, or
- Terminal: `./gradlew installDebug`, then open **DriveMetrics** from the app drawer.

If an emulator is running too, close it or install to one device only:
`adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk`.

**Wireless (optional, Android 11+):** Developer options → **Wireless debugging**, then pair
using **Pair Devices Using Wi-Fi** in Android Studio.

### Troubleshooting

| Problem | Fix |
|---|---|
| `adb devices` shows `unauthorized` | Unlock the phone and accept the USB debugging prompt |
| Phone not listed at all | Try another cable or port (many cables only charge); install your phone maker's USB driver on Windows |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Uninstall the old copy: `adb uninstall com.example.gpstracker` |
| App crashes on start | `adb logcat -s AndroidRuntime:E` and open an issue with the output |

## Status

- Done: full UI, navigation, simulator, unit and UI tests.
- Next: real telemetry source (WebSocket), local database, real map, crash detection from data.

See `app/UI_SPEC.md` for the full UI specification.

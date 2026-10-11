# DriveMetrics – UI Specification (v2, UI-first)

Implementation spec for Claude Code. Project: `gpstracker`, package `com.example.gpstracker`, Kotlin + Jetpack Compose + Material 3. Android only.

v2 adds: sign in / sign up with vehicle setup, fuel and money estimates, and an Insights tab comparing the driver with a benchmark driver.

v2.1: **no manual trips.** There is no "Start trip" / "End trip" button anywhere. The app always shows live data from the car, and trips start and end automatically from the device data (ignition and movement, section 13.5). The user never opens or closes a trip.

## 0. Goal of this phase

Build the complete, polished UI **with fake data only**, so it can be shown to the teacher. No networking, no database, no real GPS, no real authentication yet.

Hard rules:

1. **Every screen is stateless.** A screen takes a `UiState` + callback lambdas. No ViewModel, repository or fake logic inside a screen composable.
2. **Fake producers drive the app.** `FakeDriveSimulator` and `FakeAuthRepository` (section 13) emit changing state so the app looks alive in a demo. Later they are replaced by real implementations without touching any screen.
3. **Every screen and every state has a `@Preview`** (dark background, phone size; dashboard also landscape).
4. **The project must build and run after every step** in section 15. Run `./gradlew assembleDebug` before declaring a step done.
5. Inspect the existing project first (`gradle/libs.versions.toml`, `app/build.gradle.kts`, `MainActivity.kt`, `ui/theme/`). Reuse the version catalog; replace the template theme rather than adding a second one.
6. All user-visible strings go in `res/values/strings.xml`.
7. **Honest numbers.** Fuel, money and benchmark values are estimates. The UI always labels them as estimates, and the benchmark is labelled "Simulated benchmark". Never present them as measured or official data.

---

## 1. Design direction

The app is used **in a car, at a glance**. It should feel like a road instrument, not a generic dashboard template.

- **Dark only.** Driving UI must not flash white at night. Ignore system light mode (document this decision in a code comment in `Theme.kt`).
- **One memorable element:** the **speed-limit sign** – a white disc with a red ring and black number, exactly like a European (Lithuanian) road sign. It appears on the dashboard, map overlay and in event rows.
- Everything else stays quiet: flat surfaces, no gradients, no drop shadows, no glow, no decorative cards-everywhere layout.
- Big numbers, few words. Readable in under one second.
- **Never use color alone** to carry meaning: every colored state also has an icon and/or text; chart series also differ by line style or label.

### 1.1 Color tokens

Create `ui/theme/Color.kt` with these and nothing else:

| Token | Hex | Use |
|---|---|---|
| `Asphalt` | `#0F1417` | App background |
| `Kerb` | `#182025` | Surfaces (cards, bottom bar, sheets) |
| `KerbHigh` | `#222C32` | Raised surfaces, gauge track, dividers, chart grid |
| `LineWhite` | `#EEF2F3` | Primary text, speed number |
| `Concrete` | `#93A1A8` | Secondary text, units, labels, benchmark series |
| `NightBlue` | `#4DA3FF` | Primary accent: selected nav item, gauge arc (normal), "You" series, links, focus |
| `SignalAmber` | `#FFB020` | Warnings: reconnecting, approaching limit, "fair" score |
| `SignRed` | `#D7263D` | Danger: over limit, speed-limit ring, "risky" score, crash alert, form errors |
| `GoGreen` | `#3DD68C` | Success: "good" score, live connection dot, money saved |

Map them into a Material 3 `darkColorScheme`:
`background = Asphalt, surface = Kerb, surfaceVariant / surfaceContainerHigh = KerbHigh, onBackground = onSurface = LineWhite, onSurfaceVariant = Concrete, primary = NightBlue, onPrimary = Asphalt, error = SignRed, onError = LineWhite, outline = KerbHigh`.

Status colors (`SignalAmber`, `GoGreen`, `SignRed`) are accessed via a small `object DriveColors` – not via Material roles.

Contrast: all text on `Asphalt`/`Kerb` must meet WCAG AA (4.5:1). `Concrete` on `Asphalt` passes; do not use `Concrete` on `KerbHigh` for small text.

### 1.2 Typography

Font: **Barlow** (body/UI) and **Barlow Condensed** (big numerals) – road-signage style, free on Google Fonts (OFL). Bundle TTFs in `res/font/`:
`barlow_regular.ttf, barlow_medium.ttf, barlow_semibold.ttf, barlow_condensed_medium.ttf, barlow_condensed_semibold.ttf`.
If the files are not present, create `FontFamily` definitions that fall back to `FontFamily.Default` and leave a `TODO` – the build must not break.

All numbers use tabular figures (`fontFeatureSettings = "tnum"`) so digits do not jump while changing.

| Style | Font | Size / line | Weight | Use |
|---|---|---|---|---|
| `speedValue` (custom) | Barlow Condensed | 96 / 96 sp | SemiBold | Current speed |
| `displaySmall` | Barlow Condensed | 40 / 44 sp | SemiBold | Score numbers, money values |
| `headlineSmall` | Barlow | 24 / 30 sp | SemiBold | Screen titles |
| `titleMedium` | Barlow | 18 / 24 sp | Medium | Card titles, list row titles |
| `bodyLarge` | Barlow | 16 / 24 sp | Regular | Body text |
| `bodyMedium` | Barlow | 14 / 20 sp | Regular | Secondary text |
| `labelLarge` | Barlow | 14 / 20 sp | Medium | Buttons |
| `labelMedium` | Barlow | 12 / 16 sp | Medium | Units, small labels, chart axes (sentence case, no all-caps) |

### 1.3 Spacing, shape, motion

- Spacing scale (dp): `4, 8, 12, 16, 24, 32`. Screen side padding **16 dp**.
- Shapes: small `8 dp` (chips, buttons, text fields), medium `16 dp` (cards, sheets). Pills only for status chips.
- Touch targets ≥ **48 dp**. Primary trip button and auth primary buttons height **56–64 dp**.
- Motion: speed number & gauge arc animate with `tween(300)`; event banner slides from top (`AnimatedVisibility`, 250 ms) and auto-hides after 3 s; charts draw in once on first show (`animateFloatAsState`, 600 ms, respects reduced motion via `LocalAccessibilityManager`/system animator scale); screen transitions = default fade. No other ambient animation.
- Icons: `androidx.compose.material:material-icons-extended`, outlined/rounded style, always with `contentDescription` (or `null` when purely decorative next to text).

---

## 2. App structure & navigation

```
MainActivity
└── DriveMetricsApp()                 // theme + session check + NavHost
    ├── AuthGraph                     // shown when signed out
    │   ├── Welcome         (start)
    │   ├── SignIn
    │   ├── SignUp
    │   └── VehicleSetup              // after sign up (also reachable from Settings)
    └── MainGraph                     // shown when signed in or in guest demo
        ├── Drive      (start)        // live dashboard          ┐
        ├── Map                       // live map                │ bottom bar
        ├── Trips                     // history list            │ (4 tabs)
        ├── Insights                  // you vs benchmark, fuel  ┘
        ├── TripDetail(tripId)        // pushed, no bottom bar
        └── Settings                  // pushed via avatar button, no bottom bar
    + CrashAlertOverlay               // full-screen overlay above everything
```

- Navigation Compose with **type-safe routes** (`@Serializable` objects/data classes; needs the `kotlin.plugin.serialization` Gradle plugin and `kotlinx-serialization-json`). Use the latest stable `androidx.navigation:navigation-compose` (2.9.x at time of writing). Two nested graphs: `AuthGraph`, `MainGraph`.
- **Start destination:** `MainGraph` if `SessionState` is `SignedIn` or `Guest`, else `AuthGraph`. While the session is loading, show a plain `Asphalt` screen with the app name (no spinner flash under 300 ms).
- Sign in / sign up success → `MainGraph`, clearing `AuthGraph` from the back stack (`popUpTo(AuthGraph) { inclusive = true }`). Sign out → `AuthGraph`, clearing `MainGraph`.
- Bottom `NavigationBar` with 4 items: **Drive** (`Speed` icon), **Map** (`Map`), **Trips** (`History`), **Insights** (`Insights`). Selected = `NightBlue` indicator; container = `Kerb`.
- **Settings** opens from an `AccountAvatar` icon button (initials in a 36 dp circle, `KerbHigh`) in the top bar of Trips and Insights, and from the Drive offline banner action. In guest mode the avatar shows a `Person` icon.
- Bottom bar hidden on `TripDetail`, `Settings` and all auth screens.
- Re-selecting the current tab does nothing; switching tabs keeps each tab's state (`saveState/restoreState`).
- Edge-to-edge (`enableEdgeToEdge()`), insets handled by `Scaffold`; auth forms use `imePadding()` so the keyboard never hides the primary button.
- Keep screen on while the car is driving (`vehicleState != Parked`, via a `KeepScreenOn` composable effect using `FLAG_KEEP_SCREEN_ON`).
- Support portrait and landscape; dashboard has a dedicated landscape layout.

---

## 3. UI models (`ui/model/`)

These are **UI models only**, already formatted where useful. The domain layer will map into them later.

```kotlin
enum class ConnectionStatus { Live, Replay, Connecting, Reconnecting, Offline }
enum class GpsStatus { Good, Weak, Lost }
enum class ScoreBand { Good, Fair, Risky }          // >=85, 60..84, <60
enum class EventType { Speeding, HarshBraking, HarshAcceleration, SharpCorner, CrashSuspected }
enum class FuelType { Petrol, Diesel }              // v1; hybrid users choose Petrol; EVs out of scope
enum class InsightsPeriod { Week, Month }

data class GeoPointUi(val lat: Double, val lon: Double)

data class DrivingEventUi(
    val id: String,
    val type: EventType,
    val title: String,        // "Harsh braking"
    val detail: String,       // "−4.1 m/s²" or "72 in a 50 zone"
    val penalty: Int,         // 3
    val timeLabel: String,    // "14:32"
    val placeLabel: String,   // "Savanorių pr."
    val position: GeoPointUi,
)

/** Comes from the device data, never from the user. */
enum class VehicleState {
    Driving,   // trip in progress, car moving
    Stopped,   // trip in progress, car standing (traffic light, jam) – trip NOT ended yet
    Parked,    // no trip in progress (ignition off / stationary long enough)
}

data class DriveUiState(
    val connection: ConnectionStatus = ConnectionStatus.Connecting,
    val gps: GpsStatus = GpsStatus.Good,
    val vehicleState: VehicleState = VehicleState.Parked,
    val tripStartedLabel: String? = null,     // "Trip started 14:05", null when Parked
    val lastTrip: TripSummaryUi? = null,      // shown on the dashboard while Parked
    val tripJustSaved: TripSummaryUi? = null, // drives the "Trip saved" banner once
    val speedKmh: Int = 0,
    val speedLimitKmh: Int? = null,       // null = unknown limit
    val score: Int = 100,
    val scoreBand: ScoreBand = ScoreBand.Good,
    val distanceKm: Double = 0.0,
    val durationLabel: String = "00:00",
    val eventCount: Int = 0,
    val latestEvent: DrivingEventUi? = null,   // drives the event banner
    val route: List<GeoPointUi> = emptyList(),
    val events: List<DrivingEventUi> = emptyList(),
) {
    val overLimitBy: Int get() = speedLimitKmh?.let { (speedKmh - it).coerceAtLeast(0) } ?: 0
    val isOverLimit: Boolean get() = overLimitBy > 5   // matches the +5 tolerance
    val isTripInProgress: Boolean get() = vehicleState != VehicleState.Parked
}

/** Fuel and money estimate for one trip or one period. All values are estimates. */
data class FuelEstimateUi(
    val fuelUsedL: Double,          // estimated total fuel
    val fuelWastedL: Double,        // extra fuel caused by harsh events and speeding
    val wastedCostLabel: String,    // "€0.19"
    val co2Kg: Double,              // from fuelUsedL
)

data class TripSummaryUi(
    val id: String,
    val dateLabel: String,      // "Today", "Yesterday", "Mon, 5 Oct"
    val timeRange: String,      // "14:05 – 14:41"
    val fromTo: String,         // "Žaliakalnis → Akropolis"
    val distanceKm: Double,
    val durationLabel: String,  // "36 min"
    val score: Int,
    val scoreBand: ScoreBand,
    val eventCount: Int,
)

data class TripDetailUi(
    val summary: TripSummaryUi,
    val avgSpeedKmh: Int,
    val maxSpeedKmh: Int,
    val route: List<GeoPointUi>,
    val events: List<DrivingEventUi>,
    val fuel: FuelEstimateUi,
    val tips: List<String>,      // max 3 coaching tips
)

data class TripsUiState(
    val tripInProgressLabel: String?,        // "Driving now, started 14:05"; null when parked
    val weekAverageScore: Int?,              // null when no trips
    val weekScores: List<Int>,               // last 7 trips, oldest first, for sparkline
    val sections: List<Pair<String, List<TripSummaryUi>>>,  // grouped by date label
)

/** One category in the you-vs-benchmark bar chart. Values are events per 100 km. */
data class ComparisonRowUi(val type: EventType, val label: String, val you: Double, val benchmark: Double)

data class InsightsUiState(
    val period: InsightsPeriod = InsightsPeriod.Week,
    val hasEnoughData: Boolean = true,       // false if < 3 trips in period
    val yourScore: Int = 0,
    val benchmarkScore: Int = 0,
    val verdict: String = "",                // "8 points above the benchmark driver"
    val trendLabels: List<String> = emptyList(),   // x labels, e.g. "W36".."W43" or "May".."Oct"
    val yourTrend: List<Int?> = emptyList(),       // null = no trips that period (gap in line)
    val benchmarkTrend: List<Int> = emptyList(),
    val comparison: List<ComparisonRowUi> = emptyList(),
    val fuel: FuelEstimateUi? = null,              // for the period
    val savedVsPreviousLabel: String? = null,      // "€2.10 less wasted than last month"; null if no previous data
    val projectedYearlySavingLabel: String? = null,// "Up to €64 a year if you match your best week"
    val distanceKm: Double = 0.0,
)

data class VehicleProfileUi(
    val fuelType: FuelType = FuelType.Petrol,
    val consumptionL100: String = "6.5",     // text field value, validated 2.0..25.0
    val fuelPricePerL: String = "",          // user enters local price; empty = money hidden
    val monthlyKm: String = "1000",          // used for yearly projection
)

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data object Guest : SessionState
    data class SignedIn(val name: String, val email: String) : SessionState
}

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val emailError: String? = null,
    val formError: String? = null,       // "Email or password is incorrect."
    val isLoading: Boolean = false,
) { val canSubmit: Boolean get() = email.isNotBlank() && password.isNotBlank() && !isLoading }

data class SignUpUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val acceptedPrivacy: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val formError: String? = null,       // "An account with this email already exists."
    val isLoading: Boolean = false,
)

data class SettingsUiState(
    val accountName: String? = null,      // null = guest
    val accountEmail: String? = null,
    val dataSource: DataSource = DataSource.Replay,   // enum { Live, Replay }
    val serverUrl: String = "ws://10.0.2.2:8080/telemetry",
    val speedTolerance: Int = 5,          // km/h, options 0, 5, 10
    val tripEndAfterMinutes: Int = 5,     // options 2, 5, 10
    val crashDetectionEnabled: Boolean = true,
    val emergencyContactName: String = "",
    val vehicle: VehicleProfileUi = VehicleProfileUi(),
    val appVersion: String = "0.2.0",
)

data class CrashAlertUiState(val visible: Boolean = false, val secondsLeft: Int = 10, val sent: Boolean = false)
```

Helpers: `fun scoreBandOf(score: Int): ScoreBand`, and extension functions returning the color, icon and label for `ScoreBand`, `EventType`, `ConnectionStatus`, `GpsStatus` (in `ui/components/StatusVisuals.kt`). Screens must use these helpers, not hard-coded colors.

Form validation lives in a small pure-Kotlin `AuthValidator` (`ui/auth/AuthValidator.kt`) with unit tests: email format, password ≥ 8 chars with at least one letter and one digit, passwords match, name not blank.

---

## 4. Shared components (`ui/components/`)

Each component gets its own file and at least one `@Preview`.

### 4.1 `SpeedLimitSign(limitKmh: Int?, size: Dp = 64.dp)`
- `Canvas`: white disc, red (`SignRed`) ring with ring width = 12% of diameter, black number centered (Barlow Condensed SemiBold, ~40% of diameter).
- `limitKmh == null` → grey ring (`Concrete`) and "—"; contentDescription "Speed limit unknown".
- contentDescription otherwise: "Speed limit 50 kilometers per hour".

### 4.2 `SpeedGauge(speedKmh: Int, limitKmh: Int?, isOverLimit: Boolean, modifier)`
- Square, default 280 dp (fills available width up to 320 dp).
- Arc from 135° sweeping 270°, range 0–180 km/h.
- Track: `KerbHigh`, stroke 14 dp, round caps.
- Progress arc: `NightBlue` normally; `SignRed` when `isOverLimit`. Animated (`animateFloatAsState`, tween 300).
- Ticks: major every 20 km/h (labels 0, 40, 80, 120, 160 in `labelMedium`, `Concrete`), minor every 10.
- **Limit marker:** a short `SignalAmber` notch on the arc at `limitKmh` (hidden if null).
- Center: speed number (`speedValue`, `LineWhite`, or `SignRed` when over limit), below it "km/h" (`labelMedium`, `Concrete`).
- When over limit, a text line under the unit: "+12 over limit" (`SignRed`, `labelLarge`) – meaning is not color-only.
- Semantics: one merged node, contentDescription "Speed 62 kilometers per hour, limit 50, 12 over limit".

### 4.3 `ScoreBadge(score: Int, band: ScoreBand, size: ScoreBadgeSize = Medium)`
- Ring (stroke 6 dp) filled proportionally to score, band color; score number in the middle (`displaySmall` for Large, `titleMedium` for Small).
- Small (40 dp) for list rows, Medium (72 dp) for dashboard, Large (120 dp) for trip detail.
- Large variant shows band label under the number: "Good" / "Fair" / "Risky".

### 4.4 `ConnectionChip(status: ConnectionStatus)` and `GpsChip(status: GpsStatus)`
Pill, height 32 dp, `Kerb` container, icon + text:

| Status | Icon | Color | Text |
|---|---|---|---|
| Live | filled dot | GoGreen | "Live" |
| Replay | `PlayCircle` | NightBlue | "Demo replay" |
| Connecting | small progress | Concrete | "Connecting…" |
| Reconnecting | `Sync` (rotating) | SignalAmber | "Reconnecting…" |
| Offline | `CloudOff` | SignRed | "Offline" |
| GPS Good | `GpsFixed` | GoGreen | "GPS" |
| GPS Weak | `GpsNotFixed` | SignalAmber | "Weak GPS" |
| GPS Lost | `GpsOff` | SignRed | "No GPS" |

### 4.5 `StatusBanner(kind, message, actionLabel?, onAction?)`
Full-width strip under the top chips, `Kerb` background, 4 dp left accent bar in status color, icon + one line of text. Used for:
- Reconnecting: "Connection lost. Trying again in 4 s." (amber)
- GPS lost: "GPS signal lost. Score paused until it returns." (red)
- Offline + no trip: "Not connected. Check the server address in Settings." + action "Settings" (red)

### 4.6 `EventBanner(event: DrivingEventUi?)`
Slides in from top over the dashboard for 3 s when `latestEvent` changes. `KerbHigh` container, medium shape, event icon in event color, title + detail, penalty on the right ("−3", `SignRed`, `titleMedium`). Announced to TalkBack via `liveRegion = Polite`.

Event visuals:

| EventType | Icon | Color | Title |
|---|---|---|---|
| Speeding | `Speed` | SignRed | "Speeding" |
| HarshBraking | `TrendingDown` | SignalAmber | "Harsh braking" |
| HarshAcceleration | `TrendingUp` | SignalAmber | "Harsh acceleration" |
| SharpCorner | `TurnRight` | SignalAmber | "Sharp corner" |
| CrashSuspected | `CarCrash` | SignRed | "Possible crash" |

### 4.7 `StatTile(value: String, unit: String?, label: String)`
Value in Barlow Condensed 28 sp + unit in `labelMedium`, label below in `bodyMedium` `Concrete`. No card background on the dashboard (quiet layout); used in a row of three separated by 1 dp `KerbHigh` vertical dividers.

### 4.8 `EventRow(event: DrivingEventUi, selected: Boolean, onClick)`
List row, min height 64 dp: event icon in a 40 dp circle (`KerbHigh`), title (`titleMedium`) + time and place (`bodyMedium`, `Concrete`), detail under it, penalty "−3" on the right. Selected → `KerbHigh` background + `NightBlue` 2 dp left bar.

### 4.9 `RouteMap(route, events, currentPosition?, selectedEventId?, followCurrent, modifier)`
**Abstraction for the map.** v1 implementation = `CanvasRouteMap`: no API key needed.
- Background `Kerb` with a subtle 32 dp grid (`KerbHigh`, 1 dp).
- Projects lat/lon into the box (equirectangular, fit route bounds + 10% padding, keep aspect ratio).
- Route polyline 5 dp `NightBlue`, round joins.
- Event markers: 20 dp circles in event color with white 2 dp border; selected marker 28 dp.
- Current position: 18 dp `LineWhite` circle with `NightBlue` 4 dp ring.
- Small text label bottom-left: "Map preview" (`labelMedium`, `Concrete`).

Keep the composable signature stable – a later step swaps the implementation to Google Maps Compose without changing any screen.

### 4.10 `EmptyState(icon, title, message, actionLabel?, onAction?)`
Centered column, icon 48 dp `Concrete`, title `titleMedium`, message `bodyMedium` `Concrete`, optional `OutlinedButton`.

### 4.11 Trip status (no buttons) – `TripStatusBar` and `TripSavedBanner`
There is **no start/end trip control** anywhere in the app. Trips are detected from the car's data.

**`TripStatusBar(vehicleState: VehicleState, tripStartedLabel: String?)`** – one quiet line, height 40 dp, icon + text, no background:

| VehicleState | Icon | Color | Text |
|---|---|---|---|
| Driving | `DirectionsCar` | NightBlue | "Driving. Trip started 14:05" |
| Stopped | `PauseCircle` | Concrete | "Stopped. Trip continues when you drive on" |
| Parked | `LocalParking` | Concrete | "Parked. A new trip starts when the car moves" |

Not clickable. contentDescription = the text.

**`TripSavedBanner(trip: TripSummaryUi?, onView)`** – appears when a trip ends automatically (`tripJustSaved` changes). Same container as `EventBanner`, slides in from the bottom above the bottom bar, stays 6 s: `CheckCircle` icon (GoGreen), "Trip saved", "18.2 km, score 91" + `TextButton` "View" → TripDetail. `liveRegion = Polite`.

**`LastTripCard(trip: TripSummaryUi, onClick)`** – shown on the dashboard while Parked: `Kerb` card, ScoreBadge Small, "Last trip" (`labelMedium`, Concrete), `fromTo` (`titleMedium`), "Today 14:05 – 14:41, 18.2 km" (`bodyMedium`), chevron. Whole card opens TripDetail.

### 4.12 Charts (`ui/components/charts/`) – custom `Canvas`, no chart library

**`TrendLineChart(labels: List<String>, you: List<Int?>, benchmark: List<Int>, modifier)`**
- Height 200 dp. Y axis fixed 0–100 with grid lines at 50 / 70 / 85 (`KerbHigh`, 1 dp) and their labels left (`labelMedium`, `Concrete`).
- Score-band backdrop: very subtle horizontal bands (≤ 8% alpha) of SignRed below 60, SignalAmber 60–84, GoGreen ≥ 85 – optional, keep subtle.
- **You:** `NightBlue` 3 dp solid line with 6 dp dots; a `null` value breaks the line.
- **Benchmark:** `Concrete` 2 dp **dashed** line (`PathEffect.dashPathEffect(8, 6)`), no dots.
- X labels under the axis; show every label when ≤ 8 points.
- Tap on a point → small tooltip above it: "W42 · You 84 · Benchmark 74".
- Legend row above the chart: short solid blue line + "You", short dashed grey line + "Simulated benchmark".
- Semantics: contentDescription summarises the trend, e.g. "Your score rose from 71 to 84 over 8 weeks. Benchmark stays around 74."

**`ComparisonBarChart(rows: List<ComparisonRowUi>, modifier)`**
- One block per category (Speeding, Harsh braking, Harsh acceleration, Sharp corners), each with the category label (`bodyMedium`) and two horizontal bars underneath:
  - "You" bar: `NightBlue`, 12 dp tall, value at the end ("1.8").
  - "Benchmark" bar: `Concrete` outline-only bar (2 dp stroke, no fill) – distinguishable without color.
- Shared x scale = max value across all rows × 1.1. Unit caption once under the chart: "Events per 100 km. Lower is better."
- Per row, a short result on the right: `TrendingDown` + "Better" (GoGreen) when you < benchmark by ≥ 10%, `TrendingUp` + "Worse" (SignalAmber) when > by ≥ 10%, otherwise "Similar" (Concrete).
- Semantics per row: "Harsh braking: you 1.8, benchmark 2.6 per 100 kilometers. Better."

**`Sparkline(values: List<Int>)`** (used in Trips header) – 120×32 dp, `NightBlue` 2 dp line, last point dot in its band color.

### 4.13 `FuelCard(estimate: FuelEstimateUi?, savedLabel: String?, projectionLabel: String?, onSetPrice: () -> Unit)`
`Kerb` card, medium shape, padding 16 dp.
- Title row: `LocalGasStation` icon + "Fuel and money" + an `Info` icon button that opens a bottom sheet explaining the estimate (section 11.3 text).
- Three StatTiles: "Fuel used" `1.4 L`, "Wasted" `0.12 L`, "CO₂" `3.2 kg`.
- Money line (`displaySmall` for the amount): "€0.19 lost to harsh driving".
- `savedLabel` (if present): `Savings` icon + text in GoGreen, e.g. "€2.10 less wasted than last month". If more was wasted, use `SignalAmber` and wording "€1.30 more wasted than last month".
- `projectionLabel` (if present) in `bodyMedium` `Concrete`.
- If fuel price is not set: replace money lines with "Add your fuel price to see costs" + `TextButton` "Add price" → `onSetPrice`.
- Footer caption always: "Estimates based on your vehicle settings." (`labelMedium`, `Concrete`).

### 4.14 Auth components (`ui/auth/components/`)
- `AuthTextField(value, onValueChange, label, error, keyboardType, imeAction, leadingIcon, trailing?)` – `OutlinedTextField`, small shape, error shown as supporting text in `SignRed` **plus** an `Error` trailing icon.
- `PasswordField(...)` – same, trailing `Visibility` / `VisibilityOff` toggle with contentDescription "Show password" / "Hide password"; `KeyboardType.Password`, autofill hint password.
- `PrimaryButton(text, loading, enabled, onClick)` – 56 dp, full width; when `loading`, shows a 20 dp progress indicator instead of the text and ignores clicks.
- `FormErrorBanner(message)` – same visual as `StatusBanner` (red), announced with `liveRegion = Assertive`.

---

## 5. Auth flow (`ui/auth/`)

All auth screens: `Asphalt` background, content max width 480 dp centered, side padding 24 dp, scrollable, `imePadding()`. Email field uses `KeyboardType.Email` + autofill email; IME action Next moves focus, Done on the last field submits.

### 5.1 Welcome – `WelcomeScreen(onCreateAccount, onSignIn, onTryDemo)`

```
┌──────────────────────────────────┐
│                                  │
│            [ 50 ]                │  SpeedLimitSign 96 dp – the app's mark
│                                  │
│          DriveMetrics            │  headlineSmall
│  See how you drive. Drive safer, │  bodyLarge, Concrete, centered
│  spend less on fuel.             │
│                                  │
│                                  │
│ [        Create account        ] │  PrimaryButton (NightBlue)
│ [           Sign in            ] │  OutlinedButton
│          Try the demo            │  TextButton → guest mode
└──────────────────────────────────┘
```
- "Try the demo" sets `SessionState.Guest` and goes to `MainGraph`. Guests see a one-line banner on Trips and Insights: "Demo mode. Create an account to keep your trips." with action "Create account".

### 5.2 Sign in – `SignInScreen(state: SignInUiState, onEmailChange, onPasswordChange, onTogglePassword, onSubmit, onForgotPassword, onGoToSignUp, onBack)`
- Top bar: back arrow, title "Sign in".
- Fields: Email, Password. `TextButton` "Forgot password?" right-aligned under the password field.
- `FormErrorBanner` above the fields when `formError != null`.
- Primary button "Sign in" (disabled unless `canSubmit`; loading state while submitting).
- Footer: "New here?" + `TextButton` "Create account".
- "Forgot password?" in this phase shows a snackbar: "Password reset is not available in the demo yet."
- Previews: empty, filled, loading, wrong password error, invalid email error.

### 5.3 Sign up – `SignUpScreen(state: SignUpUiState, on…Change, onTogglePassword, onAcceptPrivacy, onSubmit, onGoToSignIn, onBack)`
- Top bar: back arrow, title "Create account".
- Fields: Name, Email, Password (supporting text "At least 8 characters, with a letter and a number"), Confirm password.
- Checkbox row: "I agree that my trips are stored and analysed to give me feedback." + `TextButton` "Privacy" (opens a bottom sheet with the privacy text from Settings).
- Primary button "Create account" – enabled only when all fields are non-empty and the checkbox is ticked.
- Validation runs on submit and then live per field after the first submit (don't show errors while the user is still typing for the first time).
- Previews: empty, errors on all fields, email already exists, loading.

### 5.4 Vehicle setup – `VehicleSetupScreen(state: VehicleProfileUi, errors, onChange…, onSave, onSkip)`
Shown right after sign up; also opened from Settings ("Vehicle and fuel").
- Title "Your car", subtitle "Used only to estimate fuel and costs. You can change this later."
- Fuel type: segmented button "Petrol" | "Diesel".
- Average consumption: number field, suffix "L/100 km", supporting text "Check your car's trip computer or manual." Valid 2.0–25.0.
- Fuel price: number field, prefix "€", suffix "per litre", optional – supporting text "Leave empty to hide costs."
- Kilometres per month: number field, suffix "km", used for yearly projections.
- Buttons: "Save" (primary), "Skip for now" (text button; uses defaults, money stays hidden).
- Decimal input accepts both "6.5" and "6,5" (Lithuanian keyboards); `KeyboardType.Decimal`.
- Previews: defaults, validation error (consumption 40).

---

## 6. Screen: Drive (dashboard) – start of MainGraph

`DriveScreen(state: DriveUiState, onOpenTrip(id), onOpenSettings)`

The dashboard is **read-only**: it only shows what the car reports. Nothing on it starts or stops anything.

### Portrait layout – driving

```
┌──────────────────────────────────┐
│ [● Live]  [⌖ GPS]          [50]  │  top row: chips left, SpeedLimitSign 64dp right
│ ┌──────────────────────────────┐ │
│ │ StatusBanner (only if needed)│ │
│ └──────────────────────────────┘ │
│        ╭───────────────╮         │
│      ╱                   ╲       │
│     │        62           │      │  SpeedGauge, centered, max 320dp
│     │       km/h          │      │
│      ╲   +12 over limit  ╱       │
│        ╰───────────────╯         │
│                                  │
│   (74)  Driving score            │  ScoreBadge Medium + "Driving score"
│         Fair, 3 events           │  band label + events
│ ─────────────────────────────────│
│  12.4 km  │  23:41  │    3       │  StatTile ×3: Distance / Time / Events
│  Distance │  Time   │  Events    │
│                                  │
│ 🚗 Driving. Trip started 14:05   │  TripStatusBar, 16dp above bottom bar
└──────────────────────────────────┘
```

### Portrait layout – parked

```
┌──────────────────────────────────┐
│ [● Live]  [⌖ GPS]                │  no speed-limit sign while parked
│        ╭───────────────╮         │
│     │         0           │      │  SpeedGauge at 0 (dimmed: arc track only)
│        ╰───────────────╯         │
│ ┌──────────────────────────────┐ │
│ │ (91) Last trip             › │ │  LastTripCard → TripDetail
│ │      Žaliakalnis → Akropolis │ │
│ │      Today 14:05 – 14:41,    │ │
│ │      18.2 km                 │ │
│ └──────────────────────────────┘ │
│ Ⓟ Parked. A new trip starts when │  TripStatusBar
│   the car moves                  │
└──────────────────────────────────┘
```

- **Driving / Stopped:** score, stats and events are for the trip in progress. Stopped looks the same as Driving except the TripStatusBar text and the speed reads 0.
- **Parked:** no score badge and no stats; the LastTripCard replaces them. If there has never been a trip, show instead: `DirectionsCar` icon + "Waiting for your first drive" + "Trips are recorded automatically when the car moves." (`bodyMedium`, Concrete).
- EventBanner overlays the top area (above chips) when present. TripSavedBanner appears at the bottom when a trip has just ended.
- Offline: gauge shows "—" instead of a number, StatusBanner "Not connected to the car. Check the server address in Settings." with action "Settings". Trip state is shown as last known.
- The dashboard deliberately shows **no fuel or money numbers** – it stays glanceable while driving.

### Landscape layout
Two columns: left 55% = gauge (centered, height-bound) with limit sign at its top-right; right 45% = chips, banner, score + stats (2×2 grid incl. avg speed) or LastTripCard when parked, TripStatusBar at the bottom.

### States to preview
1. Parked, connected (Replay), with last trip
2. Parked, first use (no trips yet)
3. Driving, normal speed (48 in 50)
4. Driving, over limit (72 in 50) – red arc, "+22 over limit"
5. Driving with EventBanner visible (Harsh braking −3)
6. Stopped at a light (trip in progress, speed 0)
7. Trip just ended – Parked with TripSavedBanner
8. Reconnecting banner while driving
9. GPS lost banner while driving
10. Offline (banner with Settings action)
11. Unknown speed limit
12. Landscape, driving

---

## 7. Screen: Map

`MapScreen(state: DriveUiState, onRecenter)`

```
┌──────────────────────────────────┐
│ [● Live] [⌖ GPS]                 │  chips floating, top-left
│                                  │
│          RouteMap (full)         │
│        route + markers + car     │
│                            [◎]   │  SmallFloatingActionButton "Recenter"
│ ┌──────────────────────────────┐ │
│ │ 62 km/h   [50]   (74) Score  │ │  bottom overlay card, Kerb, medium shape
│ │ 12.4 km, 23:41, 3 events     │ │
│ └──────────────────────────────┘ │
└──────────────────────────────────┘
```
- Overlay speed number turns `SignRed` + shows "+12" text when over limit.
- Parked: map shows the **last trip's** route (dimmed, 50% alpha) and the car's parked position; the overlay says "Parked. Your route appears here when the car moves." No trips ever: grid + current position only.
- Previews: active trip with 3 events; empty.

---

## 8. Screen: Trips (history)

`TripsScreen(state: TripsUiState, isGuest: Boolean, onTripClick(id), onOpenSettings, onCreateAccount)`

```
┌──────────────────────────────────┐
│ Trips                       (KA) │  headlineSmall + AccountAvatar → Settings
│ ┌──────────────────────────────┐ │
│ │ (82) This week               │ │  header card: ScoreBadge Medium,
│ │      Average score, 6 trips  │ │  Sparkline of last 7 trip scores
│ │      ▁▃▅▂▆▇▅                 │ │
│ └──────────────────────────────┘ │
│ Today                            │  sticky section header (labelMedium, Concrete)
│ (91) Žaliakalnis → Akropolis   › │  row: ScoreBadge Small, fromTo (titleMedium)
│      14:05 – 14:41, 18.2 km,     │       time range, distance, events
│      1 event                     │
│ Yesterday                        │
│ (58) …                         › │
└──────────────────────────────────┘
```
- `LazyColumn` with `stickyHeader` per section. Row min height 72 dp, ripple, whole row clickable, contentDescription summarising the trip.
- Guest mode: demo banner (section 5.1) above the header card.
- A trip in progress appears as the first row with a "Driving now" label (NightBlue dot + text) instead of the score badge; tapping it opens the Drive tab. It turns into a normal row when the trip ends.
- Empty state: icon `Route`, "No trips yet", "Trips are recorded automatically when your car is driving.", no button.
- Previews: populated (≥ 6 trips across 3 days, mixed bands), empty, guest.

---

## 9. Screen: Trip detail

`TripDetailScreen(state: TripDetailUi, selectedEventId: String?, onEventClick(id), onSetFuelPrice, onBack)`

Top app bar: back arrow, title = `fromTo`, subtitle = `dateLabel`, `timeRange`.

Scrollable column:
1. **Score header:** ScoreBadge Large centered + one-sentence verdict, e.g. "Smooth drive with one speeding event." (`bodyLarge`).
2. **Stats grid** (2×2 StatTiles): Distance, Duration, Avg speed, Max speed.
3. **Route** card (200 dp tall): `RouteMap` with event markers; tapping an event row highlights its marker (`selectedEventId`).
4. **Deductions** section: title "Where you lost points" + total "−11"; list of `EventRow`s sorted by time. If none: a `GoGreen` check icon + "No deductions on this trip."
5. **Fuel and money:** `FuelCard(estimate = state.fuel, savedLabel = null, projectionLabel = null)`.
6. **Tips** section (only if tips non-empty): title "Tips for next time", up to 3 rows with `Lightbulb` icon + text, e.g. "Brake earlier before the Savanorių pr. junction."

Previews: trip with events, perfect trip (100, no events, wasted 0.00 L), fuel price not set.

---

## 10. Screen: Insights – you vs benchmark (new tab)

`InsightsScreen(state: InsightsUiState, isGuest: Boolean, onPeriodChange, onOpenSettings, onSetFuelPrice, onCreateAccount, onAboutBenchmark)`

```
┌──────────────────────────────────┐
│ Insights                    (KA) │  headlineSmall + AccountAvatar
│ [  Week  |  Month  ]             │  SingleChoiceSegmentedButtonRow
│                                  │
│   82          74                 │  two big numbers (displaySmall):
│   You         Benchmark driver ⓘ │  "You" in NightBlue, benchmark in Concrete
│   8 points above the benchmark   │  verdict (bodyLarge)
│                                  │
│ Score over time                  │  titleMedium
│ ── You   - - Simulated benchmark │  legend
│  100┤                            │
│   85┤      ●──●     ●──●         │  TrendLineChart
│   70┤ ●──●- - - - ●- - - - -     │
│   50┤                            │
│      W36 W37 W38 W39 W40 W41 …   │
│                                  │
│ Compared with the benchmark      │  titleMedium
│ Speeding                 Better ↓│
│ ████████ 1.2                     │  You (filled)
│ ▭▭▭▭▭▭▭▭▭▭▭▭ 2.0                 │  Benchmark (outline)
│ Harsh braking            Worse ↑ │
│ …                                │
│ Events per 100 km. Lower is better.
│                                  │
│ ┌──────────────────────────────┐ │
│ │ ⛽ Fuel and money          ⓘ │ │  FuelCard for the period, with
│ │ 42 L used, 2.1 L wasted, …   │ │  savedLabel and projectionLabel
│ │ €3.40 lost to harsh driving  │ │
│ │ ↓ €2.10 less than last month │ │
│ └──────────────────────────────┘ │
└──────────────────────────────────┘
```

- The ⓘ next to "Benchmark driver" opens a bottom sheet: "The benchmark is a simulated typical driver built from fixed assumptions in this app. It is not real data from other drivers. In a future version it could be the anonymous average of all app users." (`onAboutBenchmark`).
- Period switch animates chart values (no full-screen reload).
- `hasEnoughData == false` → `EmptyState`: icon `Insights`, "Not enough trips yet", "After 3 trips this week you can compare your driving. Trips are recorded automatically.", no button.
- Guest mode: demo banner at the top.
- Previews: week (better than benchmark), month (worse on braking), not enough data, fuel price not set, guest.

---

## 11. Screen: Settings (pushed)

`SettingsScreen(state: SettingsUiState, onBack, onDataSourceChange, onServerUrlChange, onToleranceChange, onCrashDetectionChange, onEmergencyContactChange, onEditVehicle, onTriggerTestCrash, onSignOut, onCreateAccount)`

Top bar: back arrow, title "Settings". Grouped list (section titles `labelMedium` `Concrete`):

- **Account**
  - Signed in: avatar (initials), name (`titleMedium`), email (`bodyMedium` `Concrete`); `TextButton` "Sign out" in `SignRed` → confirmation `AlertDialog` "Sign out? Trips stay on this phone." with "Sign out" / "Cancel".
  - Guest: "You're using the demo." + `PrimaryButton` "Create account".
- **Vehicle and fuel**
  - One row summarising: "Petrol, 6.5 L/100 km, €—/L" + chevron → opens `VehicleSetup` (`onEditVehicle`).
- **Data source**
  - Segmented button: "Demo replay" | "Live device".
  - Server address `OutlinedTextField` (enabled only for Live), supporting text "WebSocket address of the relay server", keyboard type Uri, validation error "Address must start with ws:// or wss://".
- **Scoring**
  - "Speeding tolerance": segmented 0 / +5 / +10 km/h, supporting text "Speed above the limit that is not penalised."
- **Trips**
  - "End a trip after the car stands still for": segmented 2 / 5 / 10 min (default 5), supporting text "Trips also end when the engine is turned off. Short stops, like traffic lights, don't split a trip."
  - Read-only note: "Trips start automatically when the car moves."
- **Safety**
  - Switch "Crash detection" + supporting text "Shows a 10-second alert after a sudden stop."
  - Text field "Emergency contact name" (enabled only when crash detection is on).
  - TextButton "Test crash alert" → shows the overlay (demo helper).
- **About**
  - "How estimates work" → bottom sheet with the text in 11.3.
  - "Privacy": "Trips are stored on this phone. Your account is used only to sign in. No data is shared with third parties."
  - "Version 0.2.0".

Previews: signed in, guest, Live selected with invalid URL error.

### 11.3 Text for "How estimates work"
"Fuel use is estimated from distance and your car's average consumption. Extra fuel is added for harsh acceleration, harsh braking and time spent above the speed limit. Costs use the fuel price you entered. CO₂ is calculated from estimated fuel. These are estimates, not measurements."

---

## 12. Crash alert overlay

`CrashAlertOverlay(state: CrashAlertUiState, contactName: String, onImOk, onSendNow)` – rendered above the NavHost when `visible`.

```
┌──────────────────────────────────┐
│                                  │  full screen, SignRed background
│        Possible crash            │  headlineSmall, LineWhite
│                                  │
│            ( 7 )                 │  160 dp countdown ring (LineWhite),
│                                  │  number in speedValue style
│  Alerting Mom in 7 seconds       │  bodyLarge; "your emergency contact" if name empty
│                                  │
│ [        I'm OK        ]         │  64 dp, LineWhite container, SignRed text
│ [   Send alert now     ]         │  OutlinedButton, LineWhite border
└──────────────────────────────────┘
```
- After countdown hits 0 or "Send alert now": content changes to `Check` icon + "Alert sent (demo)" + "Close" button.
- Back press = "I'm OK".
- Semantics: `liveRegion = Assertive` for the countdown text; announce "Possible crash. Alerting in 10 seconds."
- Previews: counting (7 s left), sent.

---

## 13. Fake data, estimates & demo simulator (`data/fake/`, `data/estimate/`)

### 13.1 `FakeData.kt`
- A route of ~80 `GeoPointUi` points through Kaunas (roughly Žaliakalnis → Savanorių pr. → Akropolis). Approximate coordinates are fine.
- 6–8 `TripSummaryUi` across "Today", "Yesterday" and an older date; scores spread over all three bands (e.g. 96, 91, 82, 74, 63, 58, 41).
- 2 full `TripDetailUi` (one with 4 events + 3 tips, one perfect).
- 8 weeks and 6 months of fake score history for Insights, with one gap (null week).
- Preview helpers: `PreviewStates.driveIdle`, `driveOverLimit`, `insightsWeekBetter`, `signInError`, etc. **All previews use these – no inline fake data in screens.**

### 13.2 `FuelEstimator` (pure Kotlin, unit tested, in `data/estimate/`)
Deliberately simple and explainable; every constant lives in one `FuelModelConfig` data class with KDoc saying it is a heuristic to be justified in the report.

```
baseFuelL      = distanceKm × consumptionL100 / 100
excessFuelL    = harshAccelerations × 0.020
               + harshBrakings      × 0.015
               + speedingMinutes    × 0.010
fuelUsedL      = baseFuelL + excessFuelL
fuelWastedL    = excessFuelL
wastedCost     = fuelWastedL × fuelPricePerL          (hidden if price not set)
co2Kg          = fuelUsedL × (2.31 petrol | 2.68 diesel)   // approximate factors; cite a source in the report
```
- "Saved vs previous period" = `(wastedPer100Km(previous) − wastedPer100Km(current)) × currentDistanceKm / 100 × fuelPricePerL`. Positive → "less wasted", negative → "more wasted". Hidden if the previous period has no trips.
- "Projected yearly saving" = `(wastedPer100Km(current) − wastedPer100Km(bestWeek)) × monthlyKm × 12 / 100 × fuelPricePerL`, shown as "Up to €X a year if you match your best week". Hidden if ≤ €1 or no price.
- Money formatting: `NumberFormat.getCurrencyInstance(Locale("lt", "LT"))` style with € and 2 decimals.

### 13.3 `BenchmarkProvider` (pure Kotlin)
Returns a fixed "simulated typical driver" profile from one config object: score 74, events per 100 km — speeding 2.0, harsh braking 2.6, harsh acceleration 1.8, sharp corners 1.2 – plus a flat score trend with ±2 noise. KDoc states these are invented assumptions for demonstration. Interface `BenchmarkSource` so a real backend average can replace it later.

### 13.4 `FakeAuthRepository`
Interface `AuthRepository { val session: StateFlow<SessionState>; suspend fun signIn(email, password): Result<Unit>; suspend fun signUp(name, email, password): Result<Unit>; fun continueAsGuest(); fun signOut() }`.
- In-memory map of accounts, pre-seeded with `demo@drivemetrics.app` / `demo1234`.
- Every call waits 800 ms to show loading states.
- Sign in fails with "Email or password is incorrect." on mismatch; sign up fails with "An account with this email already exists." for existing emails.
- Session starts as `SignedOut` (later: persisted with DataStore).
- **Never log passwords**; store only a hash even in the fake (`MessageDigest` SHA-256 is fine for the fake; real auth replaces this entirely).
- Real implementation later: Firebase Authentication or the project's own backend – decided after the teacher confirms scope.

### 13.5 `TripDetector` (pure Kotlin, unit tested, in `data/estimate/` for now; moves to `:domain` later)
A small state machine that turns the stream of samples into trips. **This replaces any manual start/stop.** Input per sample: timestamp, speed, `ignitionOn: Boolean?` (null = device doesn't report it), GPS valid.

States: `Parked → Driving ⇄ Stopped → Parked`.

| From | Condition | To | Effect |
|---|---|---|---|
| Parked | ignition turns on **and** speed > 5 km/h, or (ignition unknown) speed > 5 km/h for ≥ 10 s | Driving | new trip starts at the first moving sample |
| Driving | speed < 3 km/h for ≥ 5 s | Stopped | trip continues |
| Stopped | speed > 5 km/h | Driving | trip continues |
| Driving/Stopped | ignition turns off | Parked | trip ends (after a 30 s grace period in case of a quick restart) |
| Stopped | standing still for `tripEndAfterMinutes` | Parked | trip ends at the moment the car stopped |
| Driving/Stopped | no data for 10 min | Parked | trip ends at the last received sample, marked "ended without signal" |

- Trips shorter than 0.3 km **or** 60 s are discarded (car moved in a parking lot).
- If the app reconnects after a gap of less than 10 min and the car is still moving, it is the **same trip**.
- Unit tests (`TripDetectorTest`): normal trip with ignition, trip without ignition data, traffic-light stop doesn't split the trip, long stop ends it, ignition off/on within 30 s keeps one trip, too-short trip discarded, signal gap < 10 min continues, gap ≥ 10 min ends.
- Time comes from an injected clock so tests run instantly.

### 13.6 `FakeDriveSimulator`
A plain class exposing `val state: StateFlow<DriveUiState>` and `val trips: StateFlow<TripsUiState>`, with only `start(scope)`. **No `startTrip()` / `stopTrip()`** – it simulates a car, and `TripDetector` decides when trips begin and end. Every 500 ms it emits a sample with speed, limit, position and ignition:
- A loop of a whole "day" in **demo time** (compressed so a full cycle takes ~3 minutes; the compression factor lives in one constant):
  1. Parked, ignition off (~15 s).
  2. Ignition on, drives the Kaunas route; speed follows a smooth curve 0 → 45 → 52 → 30 → 68 (speeding segment, limit 50) → 20 → 0, with one 20 s traffic-light stop in the middle (must **not** split the trip).
  3. Arrives, ignition off → trip ends → `tripJustSaved` set → back to step 1 and next loop drives the route in reverse.
- Changes speed limit per segment (50 / 70 / 30).
- During a trip: accumulates distance/time, emits fake events at scripted points (speeding while > 55 for 3 s, one harsh braking, one sharp corner), sets `latestEvent`, lowers score with the formula from the report (`100 − 10 × penalties / max(km, 1)`, clamped 0..100).
- Once per loop while driving: 6 s of `Reconnecting`; once per loop: 6 s of `GpsStatus.Lost`.
- When a trip ends it builds a `TripSummaryUi` + `TripDetailUi` (fuel via `FuelEstimator`) and prepends it to the in-memory trips list, so Trips and Insights update during a demo.
- Debug-only shortcut for presentations: in Settings → About, a `TextButton` "Skip to next drive" (visible only in debug builds) jumps the simulator to step 2. It controls the **simulated car**, not the trip, so the no-manual-trips rule still holds.

### 13.7 How this works when the app is closed (decision for later, note it in the report)
The UI phase only shows live data while the app is open. For the real version, choose one:
- **A. Server keeps the data (recommended):** the relay server stores telemetry, and the app catches up on missed samples when it opens or reconnects, then runs `TripDetector` over them. This matches how real telematics products work and keeps the phone's battery free.
- **B. Foreground service on the phone:** keeps the connection open with a permanent notification. Works without a server database but Android restricts it and drains battery.
Ask the teacher whether a server-side database is acceptable before picking.

### 13.8 Wiring (temporary)
- Thin ViewModels (`SessionViewModel`, `SignInViewModel`, `SignUpViewModel`, `VehicleSetupViewModel`, `DriveViewModel`, `TripsViewModel`, `TripDetailViewModel`, `InsightsViewModel`, `SettingsViewModel`, `CrashAlertViewModel`) expose `StateFlow`s and forward callbacks. One app-level instance of each fake created in an `AppContainer` object (manual DI for now; Koin comes later).
- Settings and vehicle profile kept in memory for now (DataStore later).
- `CrashAlertViewModel` runs the countdown with a coroutine; "Test crash alert" in Settings triggers it.
- Screens are collected with `collectAsStateWithLifecycle()` in a `…Route` composable (e.g. `DriveRoute` → `DriveScreen`). Screens never see ViewModels.

---

## 14. File layout

```
app/src/main/java/com/example/gpstracker/
├── MainActivity.kt
├── DriveMetricsApp.kt
├── AppContainer.kt
├── data/
│   ├── fake/           FakeData.kt, FakeDriveSimulator.kt, FakeAuthRepository.kt, PreviewStates.kt
│   └── estimate/       FuelEstimator.kt, FuelModelConfig.kt, BenchmarkProvider.kt, TripDetector.kt
└── ui/
    ├── theme/          Color.kt, Type.kt, Shape.kt, Theme.kt
    ├── model/          UiModels.kt, AuthModels.kt, InsightsModels.kt
    ├── navigation/     Routes.kt, DriveMetricsNavHost.kt, AuthGraph.kt, MainGraph.kt, BottomBar.kt
    ├── components/     SpeedGauge.kt, SpeedLimitSign.kt, ScoreBadge.kt, StatusChips.kt,
    │                   StatusBanner.kt, EventBanner.kt, StatTile.kt, EventRow.kt,
    │                   RouteMap.kt, EmptyState.kt, TripStatusBar.kt, TripSavedBanner.kt,
    │                   LastTripCard.kt, FuelCard.kt,
    │                   AccountAvatar.kt, GuestBanner.kt, InfoBottomSheet.kt,
    │                   StatusVisuals.kt, KeepScreenOn.kt
    ├── components/charts/  TrendLineChart.kt, ComparisonBarChart.kt, Sparkline.kt, ChartLegend.kt
    ├── auth/           AuthValidator.kt, SessionViewModel.kt,
    │   ├── components/ AuthTextField.kt, PasswordField.kt, PrimaryButton.kt, FormErrorBanner.kt
    │   ├── welcome/    WelcomeScreen.kt
    │   ├── signin/     SignInRoute.kt, SignInScreen.kt, SignInViewModel.kt
    │   ├── signup/     SignUpRoute.kt, SignUpScreen.kt, SignUpViewModel.kt
    │   └── vehicle/    VehicleSetupRoute.kt, VehicleSetupScreen.kt, VehicleSetupViewModel.kt
    ├── drive/          DriveRoute.kt, DriveScreen.kt, DriveViewModel.kt
    ├── map/            MapRoute.kt, MapScreen.kt
    ├── trips/          TripsRoute.kt, TripsScreen.kt, TripsViewModel.kt
    ├── tripdetail/     TripDetailRoute.kt, TripDetailScreen.kt, TripDetailViewModel.kt
    ├── insights/       InsightsRoute.kt, InsightsScreen.kt, InsightsViewModel.kt
    ├── settings/       SettingsRoute.kt, SettingsScreen.kt, SettingsViewModel.kt
    └── crash/          CrashAlertOverlay.kt, CrashAlertViewModel.kt

app/src/test/java/com/example/gpstracker/
    AuthValidatorTest.kt, FuelEstimatorTest.kt, BenchmarkComparisonTest.kt, TripDetectorTest.kt
```

Dependencies to add (via `libs.versions.toml`, latest stable versions): `navigation-compose`, `material-icons-extended`, `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`, `kotlinx-serialization-json` + `kotlin.plugin.serialization`. Test: `junit`, `ui-test-junit4`, `ui-test-manifest`. **No map SDK, no chart library and no auth SDK in this phase.**

---

## 15. Implementation steps (build & run after each)

1. **Theme:** colors, type (with font fallback), shapes; replace template theme; dark-only. Show a temporary screen with all color tokens and text styles to verify.
2. **UI models + FakeData + PreviewStates.**
3. **Components** 4.1–4.11 and 4.14, each with previews.
4. **Charts + FuelCard** (4.12, 4.13) with previews.
5. **Navigation shell:** AuthGraph + MainGraph, session-based start, bottom bar (4 tabs), avatar → Settings, placeholder screens, TripDetail push.
6. **Auth screens:** Welcome, Sign in, Sign up, Vehicle setup + `AuthValidator` with unit tests + `FakeAuthRepository`.
7. **Drive screen** (portrait driving + parked, landscape) with all 12 preview states. No start/end trip button.
8. **Map screen** with `CanvasRouteMap`.
9. **Trips + Trip detail** screens (with fuel card).
10. **`FuelEstimator` + `BenchmarkProvider`** with unit tests, then the **Insights** screen.
11. **Settings screen** + **Crash alert overlay**.
12. **`TripDetector` with unit tests, then FakeDriveSimulator + ViewModels** – app runs as a live demo end to end, trips start and end on their own.
13. **Polish & checks:** TalkBack labels (including chart descriptions), font scale 1.3 without clipping, landscape, 48 dp targets, keyboard never covers auth buttons, 4–5 Compose UI tests: dashboard shows "+N over limit" when over limit; parked dashboard shows the last trip card; sign-in shows error for wrong password; Insights shows "Not enough trips yet" with < 3 trips; crash overlay "I'm OK" hides it.

## 16. Done when

- App builds with `./gradlew assembleDebug` and runs on emulator **and** a physical phone.
- Welcome → Sign up → Vehicle setup → Drive works; Sign out returns to Welcome; "Try the demo" works without an account; back stack never returns to auth screens after signing in.
- All 4 tabs + Trip detail + Settings + crash overlay reachable; back navigation correct; tab state kept.
- In demo mode, within one simulator cycle (~3 minutes) you can see, **without touching anything**: the car parked, a trip starting by itself, speed changing, a speeding state, an event banner, a traffic-light stop that does not split the trip, a reconnecting banner, a GPS-lost banner, the trip ending by itself with the "Trip saved" banner, and the trip appearing in Trips and changing Insights.
- No start/end trip control exists anywhere in the UI (search the code for "Start trip" / "End trip": no results).
- Fuel and benchmark values are always labelled as estimates / simulated.
- Every screen/state listed above has a preview that renders.
- No hard-coded colors or strings in screen files.
- No ViewModel or fake data referenced inside any `…Screen` composable.
- `AuthValidatorTest`, `FuelEstimatorTest`, `BenchmarkComparisonTest` and `TripDetectorTest` pass.

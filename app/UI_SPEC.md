# DriveMetrics – UI Specification (v1, UI-first)

Implementation spec for Claude Code. Project: `gpstracker`, package `com.example.gpstracker`, Kotlin + Jetpack Compose + Material 3. Android only.

## 0. Goal of this phase

Build the complete, polished UI **with fake data only**, so it can be shown to the teacher. No networking, no database, no real GPS yet.

Hard rules:

1. **Every screen is stateless.** A screen takes a `UiState` + callback lambdas. No ViewModel, repository or fake logic inside a screen composable.
2. **One fake producer drives the app.** `FakeDriveSimulator` (section 9) emits changing state over time so the dashboard looks alive in a demo. Later it will be replaced by the real domain pipeline without touching any screen.
3. **Every screen and every state has a `@Preview`** (dark background, phone size; dashboard also landscape).
4. **The project must build and run after every step** in section 11. Run `./gradlew assembleDebug` before declaring a step done.
5. Inspect the existing project first (`gradle/libs.versions.toml`, `app/build.gradle.kts`, `MainActivity.kt`, `ui/theme/`). Reuse the version catalog; replace the template theme rather than adding a second one.
6. All user-visible strings go in `res/values/strings.xml`.

---

## 1. Design direction

The app is used **in a car, at a glance**. It should feel like a road instrument, not a generic dashboard template.

- **Dark only.** Driving UI must not flash white at night. Ignore system light mode (document this decision in a code comment in `Theme.kt`).
- **One memorable element:** the **speed-limit sign** – a white disc with a red ring and black number, exactly like a European (Lithuanian) road sign. It appears on the dashboard, map overlay and in event rows.
- Everything else stays quiet: flat surfaces, no gradients, no drop shadows, no glow, no decorative cards-everywhere layout.
- Big numbers, few words. Readable in under one second.
- **Never use color alone** to carry meaning: every colored state also has an icon and/or text.

### 1.1 Color tokens

Create `ui/theme/Color.kt` with these and nothing else:

| Token | Hex | Use |
|---|---|---|
| `Asphalt` | `#0F1417` | App background |
| `Kerb` | `#182025` | Surfaces (cards, bottom bar, sheets) |
| `KerbHigh` | `#222C32` | Raised surfaces, gauge track, dividers |
| `LineWhite` | `#EEF2F3` | Primary text, speed number |
| `Concrete` | `#93A1A8` | Secondary text, units, labels |
| `NightBlue` | `#4DA3FF` | Primary accent: selected nav item, gauge arc (normal), links, focus |
| `SignalAmber` | `#FFB020` | Warnings: reconnecting, approaching limit, "fair" score |
| `SignRed` | `#D7263D` | Danger: over limit, speed-limit ring, "risky" score, crash alert |
| `GoGreen` | `#3DD68C` | Success: "good" score, live connection dot |

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
| `displaySmall` | Barlow Condensed | 40 / 44 sp | SemiBold | Score numbers |
| `headlineSmall` | Barlow | 24 / 30 sp | SemiBold | Screen titles |
| `titleMedium` | Barlow | 18 / 24 sp | Medium | Card titles, list row titles |
| `bodyLarge` | Barlow | 16 / 24 sp | Regular | Body text |
| `bodyMedium` | Barlow | 14 / 20 sp | Regular | Secondary text |
| `labelLarge` | Barlow | 14 / 20 sp | Medium | Buttons |
| `labelMedium` | Barlow | 12 / 16 sp | Medium | Units, small labels (sentence case, no all-caps) |

### 1.3 Spacing, shape, motion

- Spacing scale (dp): `4, 8, 12, 16, 24, 32`. Screen side padding **16 dp**.
- Shapes: small `8 dp` (chips, buttons), medium `16 dp` (cards, sheets). Pills only for status chips.
- Touch targets ≥ **48 dp**. Primary trip button height **64 dp**.
- Motion: speed number & gauge arc animate with `tween(300)`; event banner slides from top (`AnimatedVisibility`, 250 ms) and auto-hides after 3 s; screen transitions = default fade. No other ambient animation.
- Icons: `androidx.compose.material:material-icons-extended`, outlined/rounded style, always with `contentDescription` (or `null` when purely decorative next to text).

---

## 2. App structure & navigation

```
MainActivity
└── DriveMetricsApp()            // theme + Scaffold + NavHost + bottom bar
    ├── Drive      (start)       // live dashboard
    ├── Map                      // live map
    ├── Trips                    // history list
    │   └── TripDetail(tripId)   // pushed, no bottom bar
    └── Settings
    + CrashAlertOverlay          // full-screen overlay above everything
```

- Navigation Compose with **type-safe routes** (`@Serializable` objects/data classes; needs the `kotlin.plugin.serialization` Gradle plugin and `kotlinx-serialization-json`). Use the latest stable `androidx.navigation:navigation-compose` (2.9.x at time of writing).
- Bottom `NavigationBar` with 4 items: **Drive** (`Speed` icon), **Map** (`Map`), **Trips** (`History`), **Settings** (`Settings`). Selected = `NightBlue` indicator; container = `Kerb`.
- Bottom bar hidden on `TripDetail`. Back from `TripDetail` → `Trips`.
- Re-selecting the current tab does nothing; switching tabs keeps each tab's state (`saveState/restoreState`).
- Edge-to-edge (`enableEdgeToEdge()`), insets handled by `Scaffold`.
- Keep screen on while a trip is active (`FLAG_KEEP_SCREEN_ON` via a `KeepScreenOn` composable effect).
- Support portrait and landscape; dashboard has a dedicated landscape layout.

---

## 3. UI models (`ui/model/`)

These are **UI models only**, already formatted where useful. The domain layer will map into them later.

```kotlin
enum class ConnectionStatus { Live, Replay, Connecting, Reconnecting, Offline }
enum class GpsStatus { Good, Weak, Lost }
enum class ScoreBand { Good, Fair, Risky }          // >=85, 60..84, <60
enum class EventType { Speeding, HarshBraking, HarshAcceleration, SharpCorner, CrashSuspected }

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

data class DriveUiState(
    val connection: ConnectionStatus = ConnectionStatus.Connecting,
    val gps: GpsStatus = GpsStatus.Good,
    val isTripActive: Boolean = false,
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
}

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
    val tips: List<String>,      // max 3 coaching tips
)

data class TripsUiState(
    val weekAverageScore: Int?,              // null when no trips
    val weekScores: List<Int>,               // last 7 trips, oldest first, for sparkline
    val sections: List<Pair<String, List<TripSummaryUi>>>,  // grouped by date label
)

data class SettingsUiState(
    val dataSource: DataSource = DataSource.Replay,   // enum { Live, Replay }
    val serverUrl: String = "ws://10.0.2.2:8080/telemetry",
    val speedTolerance: Int = 5,          // km/h, options 0, 5, 10
    val crashDetectionEnabled: Boolean = true,
    val emergencyContactName: String = "",
    val appVersion: String = "0.1.0",
)

data class CrashAlertUiState(val visible: Boolean = false, val secondsLeft: Int = 10, val sent: Boolean = false)
```

Helper: `fun scoreBandOf(score: Int): ScoreBand`, and extension functions returning the color, icon and label for `ScoreBand`, `EventType`, `ConnectionStatus`, `GpsStatus` (in `ui/components/StatusVisuals.kt`). Screens must use these helpers, not hard-coded colors.

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
Value in `titleMedium`/Barlow Condensed 28 sp + unit in `labelMedium`, label below in `bodyMedium` `Concrete`. No card background on the dashboard (quiet layout); used in a row of three separated by 1 dp `KerbHigh` vertical dividers.

### 4.8 `EventRow(event: DrivingEventUi, selected: Boolean, onClick)`
List row, min height 64 dp: event icon in a 40 dp circle (`KerbHigh`), title (`titleMedium`) + "14:32 · Savanorių pr." (`bodyMedium`, `Concrete`), detail under it, penalty "−3" on the right. Selected → `KerbHigh` background + `NightBlue` 2 dp left bar.

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

### 4.11 `TripButton(isTripActive, enabled, onStart, onStop)`
Full-width, 64 dp, medium shape.
- Inactive: `FilledButton`, `NightBlue`, icon `PlayArrow`, "Start trip".
- Active: `OutlinedButton` with `SignRed` border and text, icon `Stop`, "End trip". Ending requires a **long press** (hold 1 s, show progress fill in the button) to avoid accidental taps while driving; a short tap shows the hint text "Hold to end trip" below the button for 2 s.
- Disabled while `connection` is `Offline` or `Connecting` (with the hint "Waiting for connection").

---

## 5. Screen: Drive (dashboard) – start destination

`DriveScreen(state: DriveUiState, onStartTrip, onStopTrip, onOpenSettings)`

### Portrait layout

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
│         Fair · 3 events          │  band label + events
│ ─────────────────────────────────│
│  12.4 km  │  23:41  │    3       │  StatTile ×3: Distance / Time / Events
│  Distance │  Time   │  Events    │
│                                  │
│ [ ▶  Start trip               ]  │  TripButton, 16dp from bottom bar
└──────────────────────────────────┘
```

- EventBanner overlays the top area (above chips) when present.
- When no trip is active: gauge still shows live speed; score area shows "Start a trip to get a score" in `Concrete` instead of the badge; stats show "—".

### Landscape layout
Two columns: left 55% = gauge (centered, height-bound) with limit sign at its top-right; right 45% = chips, banner, score, stats (2×2 grid incl. avg speed), TripButton at bottom.

### States to preview
1. Idle, connected (Replay), no trip
2. Active trip, normal speed (48 in 50)
3. Active trip, over limit (72 in 50) – red arc, "+22 over limit"
4. Active trip with EventBanner visible (Harsh braking −3)
5. Reconnecting banner during trip
6. GPS lost banner during trip
7. Offline, no trip (button disabled, banner with Settings action)
8. Unknown speed limit
9. Landscape, active trip

---

## 6. Screen: Map

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
│ │ 12.4 km · 23:41 · 3 events   │ │
│ └──────────────────────────────┘ │
└──────────────────────────────────┘
```
- Overlay speed number turns `SignRed` + shows "+12" text when over limit.
- No trip / empty route: map shows grid + current position only and the overlay says "Start a trip on the Drive tab to record your route."
- Previews: active trip with 3 events; empty.

---

## 7. Screen: Trips (history)

`TripsScreen(state: TripsUiState, onTripClick(id))`

```
┌──────────────────────────────────┐
│ Trips                            │  headlineSmall
│ ┌──────────────────────────────┐ │
│ │ (82) This week               │ │  header card: ScoreBadge Medium,
│ │      Average score · 6 trips │ │  sparkline of last 7 trip scores
│ │      ▁▃▅▂▆▇▅                 │ │  (Canvas, NightBlue line, dots in band colors)
│ └──────────────────────────────┘ │
│ Today                            │  sticky section header (labelMedium, Concrete)
│ (91) Žaliakalnis → Akropolis   › │  row: ScoreBadge Small, fromTo (titleMedium)
│      14:05 – 14:41 · 18.2 km     │       time range · distance · events
│      · 1 event                   │
│ Yesterday                        │
│ (58) …                         › │
└──────────────────────────────────┘
```
- `LazyColumn` with `stickyHeader` per section. Row min height 72 dp, ripple, whole row clickable, contentDescription summarising the trip.
- Empty state: icon `Route`, "No trips yet", "Start a trip on the Drive tab. Finished trips appear here.", button "Go to Drive".
- Previews: populated (≥6 trips across 3 days, mixed bands), empty.

---

## 8. Screen: Trip detail

`TripDetailScreen(state: TripDetailUi, selectedEventId: String?, onEventClick(id), onBack)`

Top app bar: back arrow, title = `fromTo`, subtitle = `dateLabel · timeRange`.

Scrollable column:
1. **Score header:** ScoreBadge Large centered + one-sentence verdict, e.g. "Smooth drive with one speeding event." (`bodyLarge`).
2. **Stats grid** (2×2 StatTiles): Distance, Duration, Avg speed, Max speed.
3. **Route** card (200 dp tall): `RouteMap` with event markers; tapping an event row highlights its marker (`selectedEventId`).
4. **Deductions** section: title "Where you lost points" + total "−11"; list of `EventRow`s sorted by time. If none: a `GoGreen` check icon + "No deductions on this trip."
5. **Tips** section (only if tips non-empty): title "Tips for next time", up to 3 rows with `Lightbulb` icon + text, e.g. "Brake earlier before the Savanorių pr. junction."

Previews: trip with events, perfect trip (100, no events).

---

## 9. Screen: Settings

`SettingsScreen(state: SettingsUiState, onDataSourceChange, onServerUrlChange, onToleranceChange, onCrashDetectionChange, onEmergencyContactChange, onTriggerTestCrash)`

Grouped list (section titles `labelMedium` `Concrete`):

- **Data source**
  - Segmented button: "Demo replay" | "Live device".
  - Server address `OutlinedTextField` (enabled only for Live), supporting text "WebSocket address of the relay server", keyboard type Uri, validation error "Address must start with ws:// or wss://".
- **Scoring**
  - "Speeding tolerance": segmented 0 / +5 / +10 km/h, supporting text "Speed above the limit that is not penalised."
- **Safety**
  - Switch "Crash detection" + supporting text "Shows a 10-second alert after a sudden stop."
  - Text field "Emergency contact name" (enabled only when crash detection is on).
  - TextButton "Test crash alert" → shows the overlay (demo helper).
- **About**
  - "Privacy": short text "Trips are stored only on this phone. No data is shared with third parties."
  - "Version 0.1.0".

Preview: default state, Live selected with invalid URL error.

---

## 10. Crash alert overlay

`CrashAlertOverlay(state: CrashAlertUiState, onImOk, onSendNow)` – rendered above the NavHost when `visible`.

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

## 11. Fake data & demo simulator (`data/fake/`)

### 11.1 `FakeData.kt`
- A route of ~80 `GeoPointUi` points through Kaunas (roughly Žaliakalnis → Savanorių pr. → Akropolis). Approximate coordinates are fine.
- 6–8 `TripSummaryUi` across "Today", "Yesterday" and an older date; scores spread over all three bands (e.g. 96, 91, 82, 74, 63, 58, 41).
- 2 full `TripDetailUi` (one with 4 events + 3 tips, one perfect).
- Preview helpers: `PreviewStates.driveIdle`, `driveOverLimit`, `driveReconnecting`, etc. **All previews use these – no inline fake data in screens.**

### 11.2 `FakeDriveSimulator`
A plain class exposing `val state: StateFlow<DriveUiState>`, with `start(scope)`, `startTrip()`, `stopTrip()`. Every 500 ms it:
- Moves along the fake route, speed follows a smooth curve 0 → 45 → 52 → 30 → 68 (speeding segment, limit 50) → 20 → 0, looping.
- Changes speed limit per segment (50 / 70 / 30).
- When a trip is active: accumulates distance/time, emits fake events at scripted points (speeding while > 55 for 3 s, one harsh braking, one sharp corner), sets `latestEvent`, lowers score using the formula from the report (`100 − 10 × penalties / max(km, 1)`, clamped 0..100).
- Every ~90 s: 6 s of `Reconnecting`; once per loop: 6 s of `GpsStatus.Lost`.
- `stopTrip()` converts the run into a `TripSummaryUi` and prepends it to an in-memory trips list (exposed as `StateFlow<TripsUiState>`), so the Trips screen updates during a demo.

### 11.3 Wiring (temporary)
- Thin ViewModels (`DriveViewModel`, `TripsViewModel`, `TripDetailViewModel`, `SettingsViewModel`, `CrashAlertViewModel`) that expose the simulator's flows as `StateFlow` and forward callbacks. One app-level simulator instance created in an `AppContainer` object (manual DI for now; Koin comes later).
- Settings state kept in memory for now.
- `CrashAlertViewModel` runs the countdown with a coroutine; "Test crash alert" in Settings triggers it.
- Screens are collected with `collectAsStateWithLifecycle()` in a `…Route` composable (e.g. `DriveRoute` → `DriveScreen`). Screens never see ViewModels.

---

## 12. File layout

```
app/src/main/java/com/example/gpstracker/
├── MainActivity.kt
├── DriveMetricsApp.kt
├── AppContainer.kt
├── data/fake/          FakeData.kt, FakeDriveSimulator.kt, PreviewStates.kt
└── ui/
    ├── theme/          Color.kt, Type.kt, Shape.kt, Theme.kt
    ├── model/          UiModels.kt
    ├── navigation/     Routes.kt, DriveMetricsNavHost.kt, BottomBar.kt
    ├── components/     SpeedGauge.kt, SpeedLimitSign.kt, ScoreBadge.kt, StatusChips.kt,
    │                   StatusBanner.kt, EventBanner.kt, StatTile.kt, EventRow.kt,
    │                   RouteMap.kt, EmptyState.kt, TripButton.kt, Sparkline.kt,
    │                   StatusVisuals.kt, KeepScreenOn.kt
    ├── drive/          DriveRoute.kt, DriveScreen.kt, DriveViewModel.kt
    ├── map/            MapRoute.kt, MapScreen.kt
    ├── trips/          TripsRoute.kt, TripsScreen.kt, TripsViewModel.kt
    ├── tripdetail/     TripDetailRoute.kt, TripDetailScreen.kt, TripDetailViewModel.kt
    ├── settings/       SettingsRoute.kt, SettingsScreen.kt, SettingsViewModel.kt
    └── crash/          CrashAlertOverlay.kt, CrashAlertViewModel.kt
```

Dependencies to add (via `libs.versions.toml`, latest stable versions): `navigation-compose`, `material-icons-extended`, `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`, `kotlinx-serialization-json` + `kotlin.plugin.serialization`. Test: `ui-test-junit4`, `ui-test-manifest`. **No map SDK in this phase.**

---

## 13. Implementation steps (build & run after each)

1. **Theme:** colors, type (with font fallback), shapes; replace template theme; dark-only. Show a temporary screen with all color tokens and text styles to verify.
2. **UI models + FakeData + PreviewStates.**
3. **Components** 4.1–4.11, each with previews.
4. **Navigation shell:** routes, NavHost, bottom bar, placeholder screens, TripDetail push.
5. **Drive screen** (portrait + landscape) with all 9 preview states.
6. **Map screen** with `CanvasRouteMap`.
7. **Trips + Trip detail** screens.
8. **Settings screen** + **Crash alert overlay**.
9. **FakeDriveSimulator + ViewModels** – app runs as a live demo.
10. **Polish & checks:** TalkBack labels, font scale 1.3 without clipping, landscape, 48 dp targets, 2–3 Compose UI tests (dashboard shows "+N over limit" when over limit; trips empty state shows button; crash overlay "I'm OK" hides it).

## 14. Done when

- App builds with `./gradlew assembleDebug` and runs on emulator **and** a physical phone.
- All 5 destinations + crash overlay reachable; back navigation correct; tab state kept.
- In demo mode, within 2 minutes you can see: speed changing, a speeding state, an event banner, a reconnecting banner, a GPS-lost banner, and a finished trip appearing in Trips.
- Every screen/state listed above has a preview that renders.
- No hard-coded colors or strings in screen files.
- No ViewModel or fake data referenced inside any `…Screen` composable.

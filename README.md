# Focuss — Native Android (Kotlin + Jetpack Compose)

A from-scratch, **pure native Android** rewrite of the React Native `focuss` app. Same
behaviour, no JavaScript runtime, no bridge — the backend is plain Android and the UI is
Jetpack Compose with a more minimal, ergonomic redesign.

Focuss blocks distracting apps during user-defined focus schedules. When a blocked app is
opened during an active schedule, a full-screen wall takes over until the session ends (or
the user waits out a cooldown to disable protection).

## Build & run

```sh
# Debug APK
./gradlew :app:assembleDebug

# Install on a connected device / emulator
./gradlew :app:installDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Requires: JDK 17+, Android SDK (compileSdk 35), minSdk 24. The `applicationId` is `com.focuss`
— the same as the RN build — so this installs as a drop-in replacement.

## Required permissions

Three special permissions are requested during onboarding (all granted via system settings):

| Permission            | Why                                             |
|-----------------------|-------------------------------------------------|
| Usage Stats           | Detect the foreground app                       |
| Display over apps      | Show the block screen over restricted apps      |
| Accessibility service | Event-driven detection of every app launch      |

Everything stays on-device; nothing is sent anywhere.

## Architecture

```
com.focuss
├── FocussApp                     Application singleton (shares backend objects)
├── data/                         ── core backend, no Android UI ──
│   ├── Models.kt                 Schedule, TimeSlot, InstalledApp
│   ├── ScheduleLogic.kt          Pure scheduling math (active-now, overnight, formatting)
│   ├── ScheduleStore.kt          Persistence: SharedPreferences + org.json
│   ├── Prefs.kt                  Theme + the running session shown by the block screen
│   └── AppRepository.kt          Installed apps, permission checks, service control
├── service/
│   ├── BlockingAccessibilityService.kt   Event-driven foreground-app detector (the engine)
│   └── AppMonitorService.kt              Foreground service: keeps process alive + notification
├── block/
│   └── BlockingActivity.kt       Full-screen "app blocked" wall with live countdown (Compose)
└── ui/                           ── Jetpack Compose ──
    ├── MainActivity.kt           Single activity + NavHost + lifecycle refresh
    ├── FocussViewModel.kt        StateFlow-driven UI state
    ├── theme/                    Material 3, single indigo accent, true-black dark mode
    ├── components/               ScheduleCard, CooldownDialog, shared bits
    └── screens/                  Splash → Permissions → Home → ScheduleEditor → AppSelection
```

### How blocking works

1. `AppMonitorService` runs as a `specialUse` foreground service (just keeps the process
   alive + shows the persistent notification).
2. `BlockingAccessibilityService` listens for `TYPE_WINDOW_STATE_CHANGED` /
   `TYPE_WINDOWS_CHANGED` — every app launch / task switch fires one. **No polling.**
3. When a foreground package is in the active block set, it launches `BlockingActivity`
   over the offending app.
4. The active block set is the union of `blockedApps` across every schedule that is
   "active now" (`ScheduleLogic.activeBlockedApps`), recomputed on every change.

## What changed from the React Native version

- **No RN / Metro / Hermes** — single Kotlin codebase. APK dropped from ~81 MB to ~16 MB.
- **No native bridge** — the old `FocussModule` is now plain `AppRepository` + a ViewModel.
- **Storage** — AsyncStorage → SharedPreferences with a small hand-rolled JSON encoder.
- **UI redesign** — cleaner, more minimal Material 3: one accent colour, larger tap targets,
  bottom-anchored primary actions, an inverted "hero" live-session timer, and true-black
  dark mode for OLED. The native `TimePickerDialog` replaces the JS date picker.
- Same feature set: schedules (time window, days, blocked apps, per-schedule cooldown),
  live session timer, session lock (no editing while a session runs), and theme toggle.
```

# Focuss

**Focuss** is a free, open source app blocker for Android that helps you protect your focus
time. Pick the apps that distract you, set the hours they should be off-limits, and Focuss
keeps them shut during your focus sessions — nothing more.

- 🪶 **Bare-minimum & lightweight** — a single, small Kotlin + Jetpack Compose app. No bloat.
- 🔒 **Private & safe** — everything stays on your device. No accounts, no servers, no tracking.
- 🚫 **No ads, ever** — and nothing to buy.
- 🆓 **Open source** — read the code, build it yourself, or contribute.

## What it does

Focuss blocks distracting apps during the focus schedules you define. When you open a
blocked app during an active schedule, a full-screen wall takes over with a live countdown
until the session ends — so the only easy thing to do is get back to what matters. Turning
protection off mid-session means waiting out a short, per-schedule cooldown.

That's the whole app. It does one thing and stays out of your way.

## Features

- **Focus schedules** — time window, days of the week, and the apps to block.
- **Instant blocking** — every app launch is caught the moment it happens (no battery-draining polling).
- **Live session timer** — see exactly how long is left in the current focus window.
- **Cooldown to disable** — a deliberate pause before protection can be switched off, to stop impulsive quitting.
- **Optional strict mode** — prevents uninstalling Focuss while a session is running.
- **Material 3 design** — one accent colour, large tap targets, and a true-black dark mode for OLED.
- **Multi-language** — available in several languages.

## Build & run

```sh
# Debug APK
./gradlew :app:assembleDebug

# Install on a connected device / emulator
./gradlew :app:installDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Requires JDK 17+, Android SDK (compileSdk 35), minSdk 24.

## Permissions

Three special permissions are requested once during onboarding (all granted through system settings):

| Permission            | Why                                          |
|-----------------------|----------------------------------------------|
| Usage Stats           | Detect the foreground app                    |
| Display over apps     | Show the block screen over restricted apps   |
| Accessibility service | Event-driven detection of every app launch   |

Focuss uses these solely to block apps you choose. Everything stays on-device; nothing is
ever sent anywhere.

## Architecture

```
com.focuss
├── FocussApp                     Application singleton (shares backend objects)
├── data/                         ── core logic, no Android UI ──
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

## Contributing

Issues and pull requests are welcome. Focuss aims to stay small and focused — please keep
contributions in that spirit: minimal, private by default, and free of ads or tracking.

## License

Open source. See the repository for license details.

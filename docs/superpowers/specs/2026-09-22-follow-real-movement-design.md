# Mock My GPS — Follow Real Movement / Anti-stop / Maximum Spoofing

Date: 2026-09-22  
Target branch: `service`  
Implementation branch: `follow-real-movement`

## Goal

Extend the existing Mock My GPS Android app with a relative movement mode that keeps the selected fake location as the anchor, then moves that fake location according to the user's real physical walking direction and detected steps. The same build should also make the mock-location loop more resilient, restore an active session after process recreation, push consistent locations through every mock provider already available in the GMS flavor, and clearly report which location-related channels are spoofed and which remain real without root.

## Existing architecture we will preserve

The app already has:
- `LocationService` as a foreground `START_STICKY` service.
- `LocationThreadManager` as the state/position coordinator.
- `LocationThread` as the periodic injection loop.
- `MockLocationProviderManager` as the flavor-level bridge.
- AOSP mock injection for GPS / Network / Fused.
- GMS mock injection for Google Play Services Fused in the GMS flavor.
- `SharedPrefs` / `SharedPrefsState` for persistent settings.
- `PreferencesActivity` and `activity_preferences.xml` for settings.

The implementation will extend these pieces instead of replacing the whole application.

## 1. FOLLOW REAL MOVEMENT

### Behaviour

When the user enables **Follow real movement** and starts spoofing from a chosen fake location:
1. The selected fake location becomes the fake anchor/current location.
2. The app listens to the accelerometer and magnetometer.
3. Step events are detected from accelerometer motion.
4. Heading is estimated from accelerometer + magnetometer orientation.
5. Each accepted step advances the fake point by `step_length_meters` in the estimated heading.
6. The periodic mock loop continues injecting the most recent fake point even when no step occurs.

Default step length: **0.74 m**.

### Coordinate update

Movement will be applied geodesically rather than by adding a fixed latitude/longitude delta.

For each step:
- distance = configured step length in meters
- bearing = filtered heading in degrees
- current latitude/longitude -> new latitude/longitude using Earth-radius geodesic formulas

Longitude normalization and polar edge cases will be handled.

### Sensor filtering

The movement tracker will:
- use a low-pass gravity estimate from the accelerometer,
- derive linear acceleration for step detection,
- apply threshold + minimum step interval to suppress double-counts,
- calculate azimuth from a rotation matrix built from accelerometer + magnetometer,
- smooth heading to avoid abrupt jumps around 0°/360°,
- ignore magnetometer samples when the field magnitude is obviously invalid.

No gyroscope and no root are required.

### Interaction with current modes

Follow Real Movement is mutually exclusive with Trip/Fly mode.

Joystick remains available only when Follow Real Movement is off.

If Follow Real Movement is enabled while a fixed fake position is active, the current fake position remains the anchor and starts moving from there.

## 2. Anti-stop and watchdog

### Problem in current code

The current `LocationThread` catches an exception around the loop, but after an exception the next callback may never be scheduled. That can silently stop location updates while the foreground service remains visible.

### New behaviour

The periodic loop will be made self-sustaining:
- scheduling of the next tick happens in a `finally`-style path,
- a single provider failure does not end the loop,
- provider exceptions are recorded,
- the manager tracks the time of the last successful mock injection.

A watchdog checks injection health.

States:
- `ACTIVE`
- `RECOVERING`
- `STOPPED`

If no successful injection occurs for the watchdog timeout:
1. state becomes `RECOVERING`,
2. mock providers are stopped defensively,
3. providers are started again,
4. the last fake point is immediately re-injected,
5. state returns to `ACTIVE` after success.

Only an explicit user STOP should put a user-started session into `STOPPED`.

Default watchdog timeout: **15 seconds**.

## 3. Restore after process recreation

The active session state will be persisted separately from normal preferences.

Persisted fields:
- session active flag,
- current fake latitude/longitude,
- Follow Real Movement enabled flag,
- step length,
- aggressive keep-alive flag,
- last selected mode.

When Android recreates `LocationService` with a null intent because of `START_STICKY`:
- the service checks saved session state,
- if the session was active, it restarts the manager at the last fake point,
- sensors and watchdog are restarted if enabled,
- providers are reinitialized.

Explicit STOP clears the active-session flag.

This is recovery after process recreation, not a claim that Android/OEM firmware can never kill or indefinitely suppress the app.

## 4. Maximum location spoofing in the GMS build

The GMS flavor will continue to inject the same fake coordinates through:
- AOSP GPS provider,
- AOSP Network provider,
- AOSP Fused provider where supported,
- UnifiedNlp update path already present,
- Google Play Services Fused mock mode.

The generated `Location` objects will be made internally consistent:
- latitude / longitude,
- timestamp,
- elapsed realtime,
- accuracy,
- altitude,
- speed,
- bearing,
- bearing accuracy where available,
- vertical accuracy where available,
- speed accuracy where available.

For Follow Real Movement:
- speed is derived from accepted step distance / elapsed step time, then smoothed,
- bearing follows the movement heading,
- stationary periods decay speed toward zero.

For fixed position:
- speed remains near zero,
- bearing is stable rather than random.

The implementation will **not** claim to hide Android's mock-location status or to spoof raw GNSS measurements, Wi-Fi scan results, Bluetooth scan results, or cellular tower information without root/system privileges.

## 5. Leak Check screen/status

A lightweight status view will report two categories.

### Mocked by this build
- GPS provider
- Network provider
- Android Fused provider where available
- Google Play Services Fused provider in the GMS flavor

Status wording will distinguish:
- available + active,
- unavailable on this Android version/device,
- provider restart/recovery in progress.

### Not globally spoofed without root/system privileges
- Wi-Fi environment / scan BSSIDs
- cellular tower / `CellInfo`
- Bluetooth scan environment
- raw GNSS / NMEA / satellite measurement APIs
- IP-based geolocation

Leak Check is a capability/status report. It will not claim that every third-party app is guaranteed to see only the fake location.

## 6. Aggressive Keep-Alive

Optional setting, default OFF.

When enabled during an active spoofing session:
- acquire a `PARTIAL_WAKE_LOCK`,
- release it on STOP or service teardown.

This is deliberately optional because it increases battery use.

The existing foreground service remains the primary persistence mechanism.

## 7. Preferences/UI changes

Add a new section in `PreferencesActivity` / `activity_preferences.xml` containing:
- `Follow real movement` checkbox
- `Step length (meters)` numeric field, default `0.74`
- `Aggressive keep-alive` checkbox
- `Watchdog / anti-stop` checkbox, default ON
- `Watchdog timeout (seconds)` numeric field, default `15`
- button or link to `Leak Check`

Settings are stored through the existing `SharedPrefs` / `SharedPrefsState` pattern.

The first implementation will keep UI styling consistent with the existing app instead of redesigning the interface.

## 8. New/modified code areas

Expected new classes:
- `service/motion/RelativeMotionTracker.java`
- `service/recovery/MockSessionState.java`
- `service/recovery/MockWatchdog.java`
- `ui/LeakCheckActivity.java`

Expected modified files:
- `LocationService.java`
- `LocationThread.java`
- `LocationThreadManager.java`
- `MockLocationProvider.java`
- AOSP/GMS provider managers as needed for health reporting
- `SharedPrefs.java`
- `SharedPrefsState.java`
- `PreferencesActivity.java`
- `activity_preferences.xml`
- `AndroidManifest.xml`
- relevant string resources

Exact file count may change slightly after compilation reveals flavor-specific constraints.

## 9. Tests and verification

Before changing behaviour, the existing baseline build remains the regression reference.

Implementation verification:
1. Unit-test geodesic movement:
   - north/east/south/west step cases,
   - heading wraparound.
2. Unit-test step debouncing/filter state where practical without Android hardware.
3. Unit-test session-state serialization.
4. Verify watchdog state transitions.
5. Build the exact already-proven variant:
   `English + WithGooglePlayServicesFusedLocationProvider + WithBackupRestoreSAF + Debug`
6. Build at least the AOSP debug flavor too, to catch shared-source regressions.
7. Inspect the final APK artifacts.
8. Device acceptance test:
   - fixed spoof works,
   - walking changes fake position,
   - standing still keeps injecting,
   - screen off does not immediately stop updates,
   - simulated provider failure/restart recovers,
   - process recreation restores the last fake position,
   - STOP actually stops and does not auto-resume.

## 10. GitHub automation

Because direct connector writes are not available in this chat, implementation will be delivered through one GitHub Actions workflow.

That workflow will:
1. check out `service`,
2. create/reset `follow-real-movement`,
3. apply the prepared source changes,
4. run tests,
5. build the target GMS APK,
6. build the AOSP sanity-check variant,
7. commit the implementation to `follow-real-movement`,
8. push the branch,
9. upload APK artifacts.

The workflow will require `permissions: contents: write`.

It must not modify the original `service` branch.

## Non-goals

This version will not:
- root the device,
- use Magisk/Xposed,
- falsify cell towers, Wi-Fi BSSIDs, Bluetooth surroundings or IP address,
- hide the fact that Android marks locations as mock where the OS/API exposes that,
- guarantee survival against every vendor-specific process killer,
- redesign the whole application.

# Follow Real Movement + Anti-stop + Maximum Spoofing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add sensor-driven relative walking movement, resilient mock injection/recovery, session restoration, consistent location metadata, leak reporting, and optional wake-lock persistence to the existing Mock My GPS Android app without changing the `service` branch directly.

**Architecture:** Keep the existing `LocationService -> LocationThreadManager -> LocationThread -> MockLocationProviderManager` pipeline. Add small pure-Java components for geodesic movement, step/heading filtering, watchdog state, and session snapshots; wrap Android sensors in `RelativeMotionTracker`; extend the existing provider managers to inject one shared `MockLocationFix` and report success/availability. Persist only session/config state through the app's existing SharedPreferences pattern, and expose user controls through the current Preferences screen plus one lightweight Leak Check activity.

**Tech Stack:** Java 8 source compatibility, Android SDK 35 / Build Tools 35.0.0, minSdk 19 for the target GMS+SAF build, Gradle 8.9 wrapper, JUnit 4.13.2 local unit tests, existing Google Play Services Location 21.0.1.

**Spec:** `docs/superpowers/specs/2026-09-22-follow-real-movement-design.md`

## Global Constraints

- Target branch stays untouched: `service`.
- Implementation branch: `follow-real-movement`.
- Target build: `English + WithGooglePlayServicesFusedLocationProvider + WithBackupRestoreSAF + Debug`.
- Target GMS+SAF minSdk: **19**.
- compileSdk / targetSdk: **35**.
- Java source/target compatibility: **Java 8**.
- No root, Magisk, Xposed, system-app privileges, or hidden-API bypasses.
- Follow Real Movement uses accelerometer + magnetometer only; no gyroscope requirement.
- Default step length: **0.74 m**.
- Default watchdog timeout: **15 seconds**.
- Aggressive keep-alive default: **OFF**.
- Watchdog/anti-stop default: **ON**.
- Follow Real Movement and Trip/Fly mode are mutually exclusive.
- Leak Check must never claim that Wi-Fi, cell tower, Bluetooth, raw GNSS/NMEA, IP geolocation, or Android's mock-location marker are hidden/spoofed.
- Only explicit user STOP clears a user-started session.
- The final workflow must build the proven GMS+SAF debug variant and an AOSP+SAF debug sanity variant.

## Review Focus

1. **Heading wraparound (359° -> 1°):** smoothing must choose the 2° short path, not rotate through 180°; pinned by `HeadingFilterTest`.
2. **Fast repeated acceleration peaks:** two peaks inside the minimum step interval must count as one step; pinned by `StepDetectorTest`.
3. **Provider exception during one loop tick:** the loop must still schedule the next tick and enter recovery only after watchdog timeout; pinned by `MockWatchdogTest` plus `LocationThread` callback structure review.
4. **`START_STICKY` null intent after process recreation:** active sessions resume from the persisted fake point; explicit STOP does not resume; pinned by `SessionSnapshotTest` and service integration acceptance test.
5. **Unavailable provider:** one missing AOSP/Fused provider must not make the entire injection report fail when another configured provider succeeds; pinned by provider-manager result aggregation tests/review.

---

## File Structure

### New production files

- `android-studio-project/Mock-my-GPS/src/main/java/com/github/warren_bank/mock_location/service/motion/GeoMover.java`  
  Pure geodesic destination calculation.

- `android-studio-project/Mock-my-GPS/src/main/java/com/github/warren_bank/mock_location/service/motion/StepDetector.java`  
  Pure step peak/debounce state machine.

- `android-studio-project/Mock-my-GPS/src/main/java/com/github/warren_bank/mock_location/service/motion/HeadingFilter.java`  
  Circular heading smoothing.

- `android-studio-project/Mock-my-GPS/src/main/java/com/github/warren_bank/mock_location/service/motion/RelativeMotionTracker.java`  
  Android SensorManager adapter; emits accepted steps with filtered bearing.

- `android-studio-project/Mock-my-GPS/src/main/java/com/github/warren_bank/mock_location/service/looper/MockLocationFix.java`  
  Immutable fake fix shared by AOSP/GMS managers.

- `android-studio-project/Mock-my-GPS/src/main/java/com/github/warren_bank/mock_location/service/recovery/MockWatchdog.java`  
  Pure watchdog state machine and timestamps.

- `android-studio-project/Mock-my-GPS/src/main/java/com/github/warren_bank/mock_location/service/recovery/SessionSnapshot.java`  
  Immutable serializable session DTO.

- `android-studio-project/Mock-my-GPS/src/main/java/com/github/warren_bank/mock_location/service/recovery/MockSessionState.java`  
  SharedPreferences persistence for `SessionSnapshot`.

- `android-studio-project/Mock-my-GPS/src/main/java/com/github/warren_bank/mock_location/ui/LeakCheckActivity.java`  
  Capability/status screen.

### New test files

- `src/test/java/com/github/warren_bank/mock_location/service/motion/GeoMoverTest.java`
- `src/test/java/com/github/warren_bank/mock_location/service/motion/StepDetectorTest.java`
- `src/test/java/com/github/warren_bank/mock_location/service/motion/HeadingFilterTest.java`
- `src/test/java/com/github/warren_bank/mock_location/service/recovery/MockWatchdogTest.java`
- `src/test/java/com/github/warren_bank/mock_location/service/recovery/SessionSnapshotTest.java`

### Modified files

- `Mock-my-GPS/build.gradle`
- `data_model/SharedPrefs.java`
- `data_model/SharedPrefsState.java`
- `service/LocationService.java`
- `service/looper/LocationThread.java`
- `service/looper/LocationThreadManager.java`
- `service/looper/MockLocationProvider.java`
- `service/looper/AospMockLocationProviderManager.java`
- each flavor-specific `MockLocationProviderManager.java`
- GMS `GmsMockLocationProviderManager.java`
- `ui/PreferencesActivity.java`
- `res/layout/activity_preferences.xml`
- new `res/layout/activity_leak_check.xml`
- relevant `res/values/strings.xml` and English resource strings
- `AndroidManifest.xml`
- `.github/workflows/build-follow-real-movement.yml`

---

### Task 1: Pure geodesic movement + heading/step filters

**Files:**
- Create: `.../service/motion/GeoMover.java`
- Create: `.../service/motion/HeadingFilter.java`
- Create: `.../service/motion/StepDetector.java`
- Create tests listed above.
- Modify: `android-studio-project/Mock-my-GPS/build.gradle`

**Interfaces:**
- Produces: `LocPoint GeoMover.move(LocPoint start, double distanceMeters, float bearingDegrees)`
- Produces: `float HeadingFilter.update(float headingDegrees)`
- Produces: `boolean StepDetector.update(float verticalLinearAcceleration, long timestampMs)`

- [ ] **Step 1: Add local JUnit dependency**

Add to `dependencies` in `Mock-my-GPS/build.gradle`:

```gradle
testImplementation 'junit:junit:4.13.2'
```

- [ ] **Step 2: Write failing `GeoMoverTest`**

Use cardinal-direction tests from Warsaw-scale coordinates plus longitude normalization:

```java
@Test
public void movesNorthByAboutOneMeter() {
    LocPoint start = new LocPoint(52.2297, 21.0122);
    LocPoint out = GeoMover.move(start, 1.0, 0f);
    assertEquals(52.22970899, out.getLatitude(), 0.000003);
    assertEquals(21.0122, out.getLongitude(), 0.000003);
}

@Test
public void eastDoesNotMoveLatitudeMaterially() {
    LocPoint start = new LocPoint(52.2297, 21.0122);
    LocPoint out = GeoMover.move(start, 1.0, 90f);
    assertEquals(52.2297, out.getLatitude(), 0.000003);
    assertTrue(out.getLongitude() > start.getLongitude());
}
```

- [ ] **Step 3: Run the focused test and verify RED**

```bash
cd android-studio-project
./gradlew :Mock-my-GPS:testEnglishWithGooglePlayServicesFusedLocationProviderWithBackupRestoreSAFDebugUnitTest --tests '*GeoMoverTest' --stacktrace
```

Expected: compile failure because `GeoMover` does not exist.

- [ ] **Step 4: Implement `GeoMover`**

Use Earth radius `6371008.8d`, convert bearing/distance to radians, calculate destination latitude with `asin`, destination longitude with `atan2`, and normalize longitude to `[-180,180)`.

Core implementation:

```java
public static LocPoint move(LocPoint start, double distanceMeters, float bearingDegrees) {
    final double R = 6371008.8d;
    double lat1 = Math.toRadians(start.getLatitude());
    double lon1 = Math.toRadians(start.getLongitude());
    double brng = Math.toRadians(((bearingDegrees % 360f) + 360f) % 360f);
    double dr = distanceMeters / R;

    double lat2 = Math.asin(
        Math.sin(lat1) * Math.cos(dr)
        + Math.cos(lat1) * Math.sin(dr) * Math.cos(brng)
    );
    double lon2 = lon1 + Math.atan2(
        Math.sin(brng) * Math.sin(dr) * Math.cos(lat1),
        Math.cos(dr) - Math.sin(lat1) * Math.sin(lat2)
    );

    double lonDeg = ((Math.toDegrees(lon2) + 540d) % 360d) - 180d;
    return new LocPoint(Math.toDegrees(lat2), lonDeg);
}
```

Reject non-finite coordinates/distance by returning a copy of the input point; clamp negative distance to zero.

- [ ] **Step 5: Write `HeadingFilterTest` before implementation**

Pin wraparound:

```java
@Test
public void wrapsAcrossNorthUsingShortArc() {
    HeadingFilter filter = new HeadingFilter(0.25f);
    filter.update(359f);
    float out = filter.update(1f);
    assertTrue(out > 350f || out < 10f);
}
```

Also test `-1`, `361`, and repeated constant headings.

- [ ] **Step 6: Implement circular heading smoothing**

Represent heading as unit-vector components, exponentially smooth X/Y, then recover angle with `atan2`; normalize to `[0,360)`.

- [ ] **Step 7: Write `StepDetectorTest` before implementation**

Tests:
- below-threshold samples: no step,
- one positive peak crossing threshold then release: one step,
- second peak < 280 ms later: ignored,
- peak after >= 280 ms: accepted,
- detector rearms only after signal falls below release threshold.

Use constants:
- trigger threshold `1.15f m/s²`
- release threshold `0.35f m/s²`
- minimum interval `280 ms`.

- [ ] **Step 8: Implement `StepDetector`**

State fields:
```java
private boolean armed = true;
private long lastStepMs = Long.MIN_VALUE;
```

A step occurs only on trigger crossing while armed and outside debounce; re-arm on release threshold.

- [ ] **Step 9: Run all three unit test classes**

Expected: PASS.

- [ ] **Step 10: Commit**

```bash
git add android-studio-project/Mock-my-GPS/build.gradle \
  android-studio-project/Mock-my-GPS/src/main/java/com/github/warren_bank/mock_location/service/motion \
  android-studio-project/Mock-my-GPS/src/test
git commit -m "feat: add relative movement math and filters"
```

---

### Task 2: Android sensor adapter and manager integration

**Files:**
- Create: `.../service/motion/RelativeMotionTracker.java`
- Modify: `.../service/looper/LocationThreadManager.java`

**Interfaces:**
- Consumes: `GeoMover`, `StepDetector`, `HeadingFilter`
- Produces:
  - `RelativeMotionTracker.Listener.onStep(float bearingDegrees, long timestampMs)`
  - `start()`, `stop()`, `isRunning()`
  - `LocationThreadManager.setFollowRealMovementEnabled(boolean)`
  - `LocationThreadManager.setStepLengthMeters(double)`

- [ ] **Step 1: Implement the listener contract first**

```java
public interface Listener {
    void onStep(float bearingDegrees, long timestampMs);
}
```

`RelativeMotionTracker` implements `SensorEventListener`, registers:
- `TYPE_ACCELEROMETER`
- `TYPE_MAGNETIC_FIELD`

using `SENSOR_DELAY_GAME`.

- [ ] **Step 2: Implement gravity/linear acceleration calculation**

Use low-pass gravity:
```java
gravity[i] = 0.8f * gravity[i] + 0.2f * accelerometer[i];
linear[i] = accelerometer[i] - gravity[i];
```

Project linear acceleration onto the normalized gravity axis so step detection is orientation-independent enough for pocket/hand use.

- [ ] **Step 3: Implement heading calculation**

Call:

```java
if (SensorManager.getRotationMatrix(rotation, null, gravity, magnetic)) {
    SensorManager.getOrientation(rotation, orientation);
    float azimuth = (float) Math.toDegrees(orientation[0]);
    filteredHeading = headingFilter.update(azimuth);
}
```

Before using magnetometer, reject magnitude outside `15..100 µT`.

- [ ] **Step 4: Emit accepted steps only when a valid heading exists**

On `stepDetector.update(verticalLinearAcceleration, nowMs) == true`, call listener with the most recent filtered heading.

- [ ] **Step 5: Integrate with `LocationThreadManager`**

Add fields:
```java
private RelativeMotionTracker mRelativeMotionTracker;
private boolean mFollowRealMovementEnabled;
private double mStepLengthMeters = 0.74d;
private long mLastAcceptedStepMs = 0L;
private float mCurrentSpeedMps = 0f;
private float mCurrentBearingDeg = 1f;
```

On step:
1. calculate elapsed seconds from last step,
2. update speed = smoothed `(stepLength / elapsed)`,
3. update bearing,
4. replace `mCurrentLocPoint = GeoMover.move(...)`.

Synchronize access to `mCurrentLocPoint` between sensor and location-loop threads.

- [ ] **Step 6: Enforce mode exclusivity**

In `flyToLocation(...)`, stop/disable motion tracking.

When Follow Real Movement is enabled:
- set `mIsFlyMode = false`,
- hide joystick,
- start tracker if session is active.

When disabled while session active:
- stop tracker,
- restore joystick behavior from existing preference.

- [ ] **Step 7: Add stationary speed decay**

In `getUpdateLocPoint` or a new `getCurrentFix()` method, if no accepted step for > 2500 ms, decay speed toward `0f`; after 8 s set `0f`.

- [ ] **Step 8: Build target variant**

```bash
./gradlew :Mock-my-GPS:assembleEnglishWithGooglePlayServicesFusedLocationProviderWithBackupRestoreSAFDebug --stacktrace
```

Expected: SUCCESS.

- [ ] **Step 9: Commit**

```bash
git add .../service/motion/RelativeMotionTracker.java .../service/looper/LocationThreadManager.java
git commit -m "feat: follow real walking movement"
```

---

### Task 3: Shared fix metadata across AOSP + GMS providers

**Files:**
- Create: `.../service/looper/MockLocationFix.java`
- Modify: `MockLocationProvider.java`
- Modify: `AospMockLocationProviderManager.java`
- Modify flavor `MockLocationProviderManager.java` files.
- Modify GMS `GmsMockLocationProviderManager.java`
- Modify `LocationThreadManager.java`
- Modify `LocationThread.java`

**Interfaces:**
- Produces:
```java
new MockLocationFix(double lat, double lon, float speedMps, float bearingDeg,
                    float accuracyMeters, double altitudeMeters)
```
- Provider injection becomes `boolean exec(MockLocationFix fix)`.

- [ ] **Step 1: Add immutable `MockLocationFix`**

Validate:
- lat `[-90,90]`
- normalized lon
- speed `>= 0`
- bearing `[0,360)`
- accuracy `>= 1`
- finite altitude

Use defaults for invalid metadata: speed `0`, bearing `1`, accuracy `3`, altitude `3`.

- [ ] **Step 2: Extend `MockLocationProvider`**

Add:

```java
public void pushLocation(MockLocationFix fix) {
    LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
    lm.setTestProviderLocation(providerName, getLocation(providerName, fix, 0));
}
```

and:

```java
protected static Location getLocation(String providerName, MockLocationFix fix, int advanceTimeMillis)
```

Set:
- lat/lon from fix,
- altitude from fix,
- time = currentTime + advance,
- speed/bearing/accuracy from fix,
- elapsed realtime nanos on API >= 17,
- bearing/vertical/speed accuracy on API >= 26.

Use realistic non-zero uncertainties:
- bearing accuracy `5f`,
- vertical accuracy `8f`,
- speed accuracy `0.35f`.

- [ ] **Step 3: Change provider managers to return success**

AOSP aggregation semantics:
- unavailable provider = neutral,
- configured provider success = true,
- configured provider throws = false for that provider,
- overall result = true if at least one configured location provider accepted the fix,
- UnifiedNlp failure must not crash injection.

GMS manager:
```java
protected static boolean exec(MockLocationFix fix) {
    if (client == null) return false;
    try {
        client.setMockLocation(MockLocationProvider.getLocation(fix, advanceTimeMillis));
        return true;
    } catch (Exception e) {
        return false;
    }
}
```

Top-level GMS flavor manager returns `aospSuccess || gmsSuccess`.

- [ ] **Step 4: Make `LocationThreadManager` expose `MockLocationFix getUpdateFix()`**

Reuse existing fly/fixed position logic to choose coordinates, then wrap current metadata:

```java
return new MockLocationFix(
    point.getLatitude(),
    point.getLongitude(),
    mCurrentSpeedMps,
    mCurrentBearingDeg,
    3f,
    3d
);
```

- [ ] **Step 5: Update `LocationThread` to call boolean injection**

```java
MockLocationFix fix = mLocationThreadManager.getUpdateFix();
boolean success = (fix != null) && MockLocationProviderManager.exec(fix);
mLocationThreadManager.onInjectionResult(success, SystemClock.elapsedRealtime());
```

- [ ] **Step 6: Compile both variants**

```bash
./gradlew \
  :Mock-my-GPS:assembleEnglishWithGooglePlayServicesFusedLocationProviderWithBackupRestoreSAFDebug \
  :Mock-my-GPS:assembleEnglishWithAospLocationProvidersWithBackupRestoreSAFDebug \
  --stacktrace
```

Expected: SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add android-studio-project/Mock-my-GPS/src
git commit -m "feat: inject consistent mock location metadata"
```

---

### Task 4: Watchdog recovery and self-sustaining loop

**Files:**
- Create: `.../service/recovery/MockWatchdog.java`
- Create: `.../service/recovery/MockWatchdogTest.java`
- Modify: `LocationThread.java`
- Modify: `LocationThreadManager.java`
- Modify provider managers only if restart helper is needed.

**Interfaces:**
- Produces enum `State { ACTIVE, RECOVERING, STOPPED }`
- Produces:
  - `markStarted(long nowMs)`
  - `markInjectionSuccess(long nowMs)`
  - `boolean shouldRecover(long nowMs)`
  - `markRecoveryStarted()`
  - `markRecoverySucceeded(long nowMs)`
  - `markStopped()`

- [ ] **Step 1: Write failing watchdog tests**

Cases:
```java
@Test public void noTimeoutBeforeConfiguredWindow() { ... }
@Test public void timeoutAfterFifteenSeconds() { ... }
@Test public void successResetsTimeoutClock() { ... }
@Test public void stoppedNeverRequestsRecovery() { ... }
@Test public void recoveryTransitionsBackToActiveAfterSuccess() { ... }
```

Use timeout `15000L`.

- [ ] **Step 2: Run RED, then implement `MockWatchdog`**

Keep it Android-free and deterministic by passing `nowMs` into methods.

- [ ] **Step 3: Restructure `LocationThread.mUpdateLocation` so rescheduling cannot be skipped**

Pattern:

```java
public void run() {
    try {
        // get fix, inject, record result, recover if required
    }
    catch (Exception e) {
        if (mLocationThreadManager != null)
            mLocationThreadManager.onInjectionResult(false, SystemClock.elapsedRealtime());
    }
    finally {
        if ((mLocationThreadManager != null) && mLocationThreadManager.shouldContinue()) {
            mHandler.postDelayed(this, mTimeInterval);
        }
    }
}
```

Do not recursively spawn new loop threads during recovery.

- [ ] **Step 4: Add controlled provider restart**

When watchdog requests recovery:
1. `MockLocationProviderManager.stopMockingLocation()`
2. `MockLocationProviderManager.startMockingLocation(mContext)`
3. inject last `MockLocationFix`
4. mark success only if the injection result is true.

Throttle recovery attempts to no more than once every 5 seconds.

- [ ] **Step 5: Run watchdog tests and build both variants**

Expected: PASS/SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add .../service/recovery/MockWatchdog.java .../MockWatchdogTest.java \
        .../service/looper/LocationThread.java .../service/looper/LocationThreadManager.java
git commit -m "fix: recover mock location loop after provider stalls"
```

---

### Task 5: Session persistence, START_STICKY restore, optional wake lock

**Files:**
- Create: `.../service/recovery/SessionSnapshot.java`
- Create: `.../service/recovery/MockSessionState.java`
- Create: `.../service/recovery/SessionSnapshotTest.java`
- Modify: `LocationService.java`
- Modify: `AndroidManifest.xml`

**Interfaces:**
- `SessionSnapshot` fields:
  - `boolean active`
  - `double latitude`
  - `double longitude`
  - `boolean followRealMovement`
  - `double stepLengthMeters`
  - `boolean aggressiveKeepAlive`
- `MockSessionState.load(Context)`
- `MockSessionState.save(Context, SessionSnapshot)`
- `MockSessionState.clear(Context)`

- [ ] **Step 1: Write snapshot validation tests**

Test:
- round-trip JSON through `toJson/fromJson`,
- invalid latitude/longitude returns inactive snapshot,
- step length <= 0 falls back to `0.74`,
- explicit inactive snapshot stays inactive.

- [ ] **Step 2: Implement pure `SessionSnapshot` JSON conversion using existing Gson**

No Android dependency in this DTO.

- [ ] **Step 3: Implement `MockSessionState`**

Store one JSON string under a private key such as:
`mock_session_state_v1`.

Use existing app SharedPreferences file so uninstall/clear-data semantics stay consistent.

- [ ] **Step 4: Persist on start and after movement**

`LocationService` saves active snapshot when ACTION_START succeeds.

`LocationThreadManager` persists updated fake coordinates at a throttled interval (maximum once per 2 seconds), not on every 100 ms injection tick.

- [ ] **Step 5: Restore on null intent**

Change:

```java
private void processIntent(Intent intent) {
    if (intent == null) {
        restoreActiveSessionIfPresent();
        return;
    }
    ...
}
```

`restoreActiveSessionIfPresent()`:
- load snapshot,
- return if not active,
- set `running = true`,
- initialize manager settings from snapshot,
- `LTM.start(new LocPoint(snapshot.latitude, snapshot.longitude))`.

- [ ] **Step 6: Explicit STOP clears persistence**

Before `stopSelf()`:
```java
MockSessionState.clear(LocationService.this);
releaseWakeLock();
```

- [ ] **Step 7: Add wake-lock permission and lifecycle**

Manifest:
```xml
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

Service field:
```java
private PowerManager.WakeLock wakeLock;
```

Acquire only if aggressive keep-alive is enabled and session is active:
```java
wakeLock = ((PowerManager)getSystemService(POWER_SERVICE))
    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName() + ":mock_location");
wakeLock.setReferenceCounted(false);
wakeLock.acquire();
```

Release on STOP and `onDestroy()`.

- [ ] **Step 8: Run tests + both builds**

Expected: PASS/SUCCESS.

- [ ] **Step 9: Commit**

```bash
git add .../service/recovery .../service/LocationService.java .../AndroidManifest.xml
git commit -m "feat: restore active mock sessions and optional wake lock"
```

---

### Task 6: Preferences controls

**Files:**
- Modify: `data_model/SharedPrefs.java`
- Modify: `data_model/SharedPrefsState.java`
- Modify: `ui/PreferencesActivity.java`
- Modify: `res/layout/activity_preferences.xml`
- Modify string resources.

**Interfaces / new preference values:**
- `follow_real_movement_enabled` default `false`
- `follow_real_movement_step_length_m` default `0.74`
- `mock_watchdog_enabled` default `true`
- `mock_watchdog_timeout_seconds` default `15`
- `aggressive_keep_alive` default `false`

- [ ] **Step 1: Add resource keys/labels**

English labels:
- `Follow real movement`
- `Step length (meters)`
- `Anti-stop watchdog`
- `Watchdog timeout (seconds)`
- `Aggressive keep-alive`
- `Leak Check`

Add clear helper text stating that aggressive keep-alive increases battery use.

- [ ] **Step 2: Add SharedPrefs getters/setters**

Follow the existing type helpers:
- boolean for toggles,
- double for step length,
- int for timeout.

Clamp:
- step length to `0.20..2.00 m`,
- timeout to `5..120 s`.

- [ ] **Step 3: Extend `SharedPrefsState` constructor/equality/diff**

Allocate new diff bits after current bit 10.

Keep old fields unchanged to avoid breaking backup data semantics.

- [ ] **Step 4: Add controls to existing Preferences layout**

Add one new section after current general/fixed settings:
- checkbox follow movement,
- numeric decimal EditText step length,
- checkbox watchdog,
- integer EditText timeout,
- checkbox aggressive keep-alive,
- Leak Check button.

- [ ] **Step 5: Wire `PreferencesActivity` save/reset**

On save:
- parse and validate,
- show existing `error_number_format` toast on parse failure,
- write only changed fields,
- send `LocationService.doSharedPrefsChange(...)`.

- [ ] **Step 6: Apply live setting changes in `LocationThreadManager`**

`importSharedPrefs()` updates tracker enabled state, step length, watchdog settings while active.

If aggressive keep-alive changes, notify `LocationService` to refresh wake-lock state via the existing preferences action path.

- [ ] **Step 7: Build GMS+SAF**

Expected: SUCCESS.

- [ ] **Step 8: Commit**

```bash
git add .../data_model/SharedPrefs.java .../data_model/SharedPrefsState.java \
        .../ui/PreferencesActivity.java .../res/layout/activity_preferences.xml \
        .../res/values
git commit -m "feat: expose movement and recovery settings"
```

---

### Task 7: Leak Check activity

**Files:**
- Create: `ui/LeakCheckActivity.java`
- Create: `res/layout/activity_leak_check.xml`
- Modify: `AndroidManifest.xml`
- Modify strings.
- Modify provider managers to expose availability status if not already available.

**Interfaces:**
- `MockLocationProviderManager.getStatus(Context)` returns a small `ProviderStatus` DTO or formatted data with:
  - AOSP GPS available/active
  - AOSP Network available/active
  - AOSP Fused available/active/API unsupported
  - GMS Fused available/active in GMS flavor

- [ ] **Step 1: Implement status DTO and provider queries**

Do not infer "spoofed" only from provider existence. "ACTIVE" means the app currently owns/uses that mock path in an active session.

- [ ] **Step 2: Build Leak Check layout**

Two sections:

**Mock location paths**
- GPS
- Network
- Android Fused
- Google Play Services Fused

**Still real / outside app-wide spoofing**
- Wi-Fi scan environment
- cellular tower / CellInfo
- Bluetooth environment
- raw GNSS / NMEA
- IP geolocation
- Android mock-location indication

- [ ] **Step 3: Add explicit explanatory copy**

Use wording:
`This screen reports what this build can control. It does not guarantee that every app sees only the fake location.`

- [ ] **Step 4: Register activity in manifest**

Keep it non-exported:
```xml
<activity
    android:name=".ui.LeakCheckActivity"
    android:exported="false" />
```

- [ ] **Step 5: Launch from Preferences button**

Use explicit Intent.

- [ ] **Step 6: Build both GMS and AOSP variants**

Ensure flavor-specific status implementation compiles for both.

- [ ] **Step 7: Commit**

```bash
git add android-studio-project/Mock-my-GPS/src
git commit -m "feat: add location leak capability report"
```

---

### Task 8: End-to-end CI workflow and regression verification

**Files:**
- Create: `.github/workflows/build-follow-real-movement.yml`
- Add design/spec docs to `docs/superpowers/specs/`
- Add this plan to `docs/superpowers/plans/`

**Interfaces:** GitHub Actions manual workflow.

- [ ] **Step 1: Create workflow with write permission**

Header:

```yaml
name: Build Follow Real Movement

on:
  workflow_dispatch:

permissions:
  contents: write
```

- [ ] **Step 2: Checkout `service` and create/reset implementation branch**

```bash
git fetch origin service
git checkout -B follow-real-movement origin/service
```

The workflow must never push to `service`.

- [ ] **Step 3: Use Java 17 runtime for Gradle and existing runner Android SDK**

Install:
```bash
SDKMANAGER="/usr/local/lib/android/sdk/cmdline-tools/latest/bin/sdkmanager"
yes | "$SDKMANAGER" --licenses >/dev/null || true
"$SDKMANAGER" "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

- [ ] **Step 4: Run unit tests**

```bash
cd android-studio-project
./gradlew \
  :Mock-my-GPS:testEnglishWithGooglePlayServicesFusedLocationProviderWithBackupRestoreSAFDebugUnitTest \
  --stacktrace
```

Expected: PASS.

- [ ] **Step 5: Build target + AOSP sanity variant**

```bash
./gradlew \
  :Mock-my-GPS:assembleEnglishWithGooglePlayServicesFusedLocationProviderWithBackupRestoreSAFDebug \
  :Mock-my-GPS:assembleEnglishWithAospLocationProvidersWithBackupRestoreSAFDebug \
  --stacktrace
```

Expected: SUCCESS.

- [ ] **Step 6: Commit and push only after tests/builds pass**

Configure bot identity, then:

```bash
git add -A
git diff --cached --quiet || git commit -m "feat: add follow real movement and resilient spoofing"
git push --force-with-lease origin follow-real-movement
```

- [ ] **Step 7: Upload APK artifacts**

```yaml
- uses: actions/upload-artifact@v4
  with:
    name: mock-my-gps-follow-real-movement-apks
    path: android-studio-project/Mock-my-GPS/build/outputs/apk/**/*.apk
    if-no-files-found: error
```

- [ ] **Step 8: Manual device acceptance test**

On the user's Android 11 device:
1. install target GMS APK,
2. set it as Developer Options mock-location app,
3. choose a fake anchor and START,
4. verify Maps sees fake anchor,
5. enable Follow Real Movement and walk 20–30 real steps,
6. verify fake point moves rather than jumping to real coordinates,
7. stand still > 30 s and verify updates continue,
8. turn screen off for several minutes and verify foreground service persists,
9. reopen app and confirm session state remains,
10. force-stop app: document that Android force-stop intentionally prevents automatic resurrection until user launches it again,
11. use ordinary process kill/recreation (not force-stop) and verify START_STICKY restore,
12. press STOP and verify no automatic resume,
13. open Leak Check and verify provider statuses/caveats.

- [ ] **Step 9: Final branch review**

Compare:
```bash
git diff origin/service...follow-real-movement
```

Confirm:
- no unrelated files,
- no credentials,
- no code claims that mock detection is hidden,
- no write to `service`,
- both APKs present.

---

## Self-review result

- **Spec coverage:** Follow movement, geodesic stepping, accel+mag heading, watchdog recovery, START_STICKY restore, consistent AOSP/GMS metadata, leak report, optional wake lock, settings, GMS/AOSP builds, and GitHub automation all have owning tasks.
- **Placeholder scan:** no TBD/TODO/“implement later” steps remain.
- **Type consistency:** `MockLocationFix`, `RelativeMotionTracker.Listener`, `MockWatchdog`, and `SessionSnapshot` signatures are defined before downstream use.
- **Review Focus:** wraparound, debounce, provider failure, null-intent restore, and unavailable-provider aggregation are each pinned to a test or explicit acceptance check.

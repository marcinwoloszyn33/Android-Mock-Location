package com.github.warren_bank.mock_location.service.looper;

import com.github.warren_bank.mock_location.data_model.EnhancedPrefs;
import com.github.warren_bank.mock_location.data_model.LocPoint;
import com.github.warren_bank.mock_location.data_model.SharedPrefsState;
import com.github.warren_bank.mock_location.event_hooks.IJoyStickPresenter;
import com.github.warren_bank.mock_location.event_hooks.ISharedPrefsListener;
import com.github.warren_bank.mock_location.security_model.RuntimePermissions;
import com.github.warren_bank.mock_location.service.LocationService;
import com.github.warren_bank.mock_location.service.motion.GeoMover;
import com.github.warren_bank.mock_location.service.motion.MotionControlPolicy;
import com.github.warren_bank.mock_location.service.motion.RelativeMotionTracker;
import com.github.warren_bank.mock_location.service.recovery.MockSessionState;
import com.github.warren_bank.mock_location.service.recovery.MockWatchdog;
import com.github.warren_bank.mock_location.service.recovery.SessionSnapshot;
import com.github.warren_bank.mock_location.service.recovery.TripProgress;
import com.github.warren_bank.mock_location.ui.components.JoyStickView;

import android.content.Context;
import android.os.SystemClock;

public class LocationThreadManager implements IJoyStickPresenter, ISharedPrefsListener, RelativeMotionTracker.Listener {
    private static final long SESSION_PERSIST_INTERVAL_MS = 2000L;
    private static final long WATCHDOG_RETRY_INTERVAL_MS = 5000L;

    private static LocationThreadManager INSTANCE = new LocationThreadManager();

    private final Object mLock = new Object();

    private Context mContext;
    private JoyStickView mJoyStickView;
    private LocationThread mLocationThread;
    private RelativeMotionTracker mRelativeMotionTracker;
    private LocPoint mCurrentLocPoint;
    private LocPoint mOriginLocPoint;
    private LocPoint mTargetLocPoint;
    private int mFlyTime;
    private int mFlyTimeIndex;

    private int mTimeInterval;
    private int mFixedCount;
    private int mFixedCountRemaining;
    private boolean mFixedJoystickEnabled;
    private double mFixedJoystickIncrement;
    private boolean mTripHoldDestination;

    private boolean mFollowRealMovementEnabled;
    private double mStepLengthMeters = EnhancedPrefs.DEFAULT_STEP_LENGTH_METERS;
    private boolean mWatchdogEnabled = true;
    private int mWatchdogTimeoutSeconds = EnhancedPrefs.DEFAULT_WATCHDOG_TIMEOUT_SECONDS;
    private boolean mAggressiveKeepAlive;

    private long mLastAcceptedStepMs;
    private long mLastSessionPersistMs;
    private float mCurrentSpeedMps;
    private float mCurrentBearingDeg = 1f;

    private MockWatchdog mWatchdog = new MockWatchdog(
        true,
        EnhancedPrefs.DEFAULT_WATCHDOG_TIMEOUT_SECONDS * 1000L,
        WATCHDOG_RETRY_INTERVAL_MS
    );

    private boolean mIsStarted = false;
    private boolean mIsFlyMode = false;

    private LocationThreadManager() {
        mContext = null;
    }

    public void init(Context context) {
        mContext = context;
        if (mRelativeMotionTracker == null) {
            mRelativeMotionTracker = new RelativeMotionTracker(context, this);
        }
        importSharedPrefs();
    }

    public static LocationThreadManager get() {
        return INSTANCE;
    }

    public void start(LocPoint locPoint) {
        if (mContext == null || locPoint == null) return;

        synchronized (mLock) {
            mCurrentLocPoint = new LocPoint(locPoint);
            mCurrentSpeedMps = 0f;
            mLastAcceptedStepMs = 0L;
            mFixedCountRemaining = mFollowRealMovementEnabled ? 0 : mFixedCount;
            mIsStarted = true;
            mWatchdog.markStarted(SystemClock.elapsedRealtime());
        }

        if ((mLocationThread == null) || !mLocationThread.isAlive()) {
            mLocationThread = new LocationThread(mContext, this, mTimeInterval);
            mLocationThread.startThread();
        }

        updateMotionAndJoystickState();
        persistSessionMaybe(SystemClock.elapsedRealtime(), true);
    }

    public void stop() {
        synchronized (mLock) {
            mWatchdog.markStopped();
            mIsStarted = false;
            mCurrentSpeedMps = 0f;
        }

        if (mRelativeMotionTracker != null) {
            mRelativeMotionTracker.stop();
        }

        if (mLocationThread != null) {
            mLocationThread.stopThread();
            mLocationThread = null;
        }

        hideJoyStick();
        stopService();
    }

    private void stopService() {
        LocationService.doStop(mContext, true);
    }

    public boolean isStarted() {
        synchronized (mLock) {
            return mIsStarted;
        }
    }

    public void showJoyStick() {
        if (mJoyStickView == null) {
            mJoyStickView = new JoyStickView(mContext);
            mJoyStickView.setJoyStickPresenter(this);
        }

        if (!mJoyStickView.isShowing()) {
            mJoyStickView.addToWindow();
        }
    }

    public void hideJoyStick() {
        if ((mJoyStickView != null) && mJoyStickView.isShowing()) {
            mJoyStickView.removeFromWindow();
        }
    }

    public LocPoint getCurrentLocPoint() {
        synchronized (mLock) {
            return (mCurrentLocPoint == null) ? null : new LocPoint(mCurrentLocPoint);
        }
    }

    public MockLocationFix getUpdateFix() {
        synchronized (mLock) {
            if (!mIsStarted || mCurrentLocPoint == null) return null;

            LocPoint point = getUpdateLocPointLocked();
            if (point == null) return null;

            updateStationarySpeedLocked(SystemClock.elapsedRealtime());

            return new MockLocationFix(
                point.getLatitude(),
                point.getLongitude(),
                mCurrentSpeedMps,
                mCurrentBearingDeg,
                3f,
                3d
            );
        }
    }

    public MockLocationFix getLastFixForRecovery() {
        synchronized (mLock) {
            if (!mIsStarted || mCurrentLocPoint == null) return null;

            updateStationarySpeedLocked(SystemClock.elapsedRealtime());
            return new MockLocationFix(
                mCurrentLocPoint.getLatitude(),
                mCurrentLocPoint.getLongitude(),
                mCurrentSpeedMps,
                mCurrentBearingDeg,
                3f,
                3d
            );
        }
    }

    private LocPoint getUpdateLocPointLocked() {
        if (!mFollowRealMovementEnabled && !mIsFlyMode && (mFixedCountRemaining != 0)) {
            if (mFixedCountRemaining < 0) {
                return null;
            }
            if (mFixedCountRemaining == 1) {
                mFixedCountRemaining = -1;
            }
            else {
                mFixedCountRemaining--;
            }
        }

        if (!mIsFlyMode) {
            return new LocPoint(mCurrentLocPoint);
        }

        if (mFlyTimeIndex >= mFlyTime) {
            jumpToLocationLocked(mTargetLocPoint);
            mFixedCountRemaining = mFollowRealMovementEnabled
                ? 0
                : ((mTripHoldDestination) ? mFixedCount : -1);
            return new LocPoint(mCurrentLocPoint);
        }
        else {
            float factor = (float) mFlyTimeIndex / (float) mFlyTime;
            double lat = mOriginLocPoint.getLatitude() + (factor * (mTargetLocPoint.getLatitude() - mOriginLocPoint.getLatitude()));
            double lon = mOriginLocPoint.getLongitude() + (factor * (mTargetLocPoint.getLongitude() - mOriginLocPoint.getLongitude()));
            mFlyTimeIndex++;
            mCurrentLocPoint.setLatitude(lat);
            mCurrentLocPoint.setLongitude(lon);
            return new LocPoint(mCurrentLocPoint);
        }
    }

    public boolean shouldContinue() {
        boolean done;

        synchronized (mLock) {
            if (!mIsStarted) return false;
            if (mFollowRealMovementEnabled) return true;

            done = (
                    (!mIsFlyMode && (mFixedCountRemaining < 0))
                ||  ( mIsFlyMode && (mFlyTimeIndex > mFlyTime))
            );
        }

        if (done) {
            stop();
        }

        return !done;
    }

    public void jumpToLocation(LocPoint location) {
        if (location == null) return;
        synchronized (mLock) {
            jumpToLocationLocked(location);
            if (mIsStarted && !mFollowRealMovementEnabled) {
                mFixedCountRemaining = mFixedCount;
            }
        }
        updateMotionAndJoystickState();
        persistSessionMaybe(SystemClock.elapsedRealtime(), true);
    }

    private void jumpToLocationLocked(LocPoint location) {
        mIsFlyMode = false;
        mCurrentLocPoint = new LocPoint(location);
        mCurrentSpeedMps = 0f;
    }

    public void flyToLocation(LocPoint location, int trip_duration_seconds) {
        if (location == null) return;

        synchronized (mLock) {
            if (mIsStarted && mFixedJoystickEnabled) {
                hideJoyStick();
            }

            mOriginLocPoint = new LocPoint(mCurrentLocPoint);
            mTargetLocPoint = new LocPoint(location);
            mIsFlyMode = true;
            mFlyTimeIndex = 0;
            mFlyTime = convertFlyTime_secondsToLoopIterations(trip_duration_seconds, mTimeInterval);
            mCurrentSpeedMps = 0f;
        }

        updateMotionAndJoystickState();
        persistSessionMaybe(SystemClock.elapsedRealtime(), true);
    }

    public boolean isFlyMode() {
        synchronized (mLock) {
            return mIsFlyMode;
        }
    }

    public void stopFlyMode() {
        synchronized (mLock) {
            mIsFlyMode = false;
            mCurrentSpeedMps = 0f;
        }
    }

    public void setMoveStep(double moveStep) {
        mFixedJoystickIncrement = moveStep;
    }

    public double getMoveStep() {
        return mFixedJoystickIncrement;
    }

    public boolean isFollowRealMovementEnabled() {
        synchronized (mLock) {
            return mFollowRealMovementEnabled;
        }
    }

    public boolean hasRequiredMotionSensors() {
        return mRelativeMotionTracker != null && mRelativeMotionTracker.hasRequiredSensors();
    }

    @Override
    public void onArrowUpClick() {
        synchronized (mLock) {
            if (mCurrentLocPoint == null) return;
            mCurrentLocPoint.setLatitude(mCurrentLocPoint.getLatitude() + mFixedJoystickIncrement);
            mCurrentSpeedMps = 0f;
        }
        persistSessionMaybe(SystemClock.elapsedRealtime(), true);
    }

    @Override
    public void onArrowDownClick() {
        synchronized (mLock) {
            if (mCurrentLocPoint == null) return;
            mCurrentLocPoint.setLatitude(mCurrentLocPoint.getLatitude() - mFixedJoystickIncrement);
            mCurrentSpeedMps = 0f;
        }
        persistSessionMaybe(SystemClock.elapsedRealtime(), true);
    }

    @Override
    public void onArrowLeftClick() {
        synchronized (mLock) {
            if (mCurrentLocPoint == null) return;
            mCurrentLocPoint.setLongitude(mCurrentLocPoint.getLongitude() - mFixedJoystickIncrement);
            mCurrentSpeedMps = 0f;
        }
        persistSessionMaybe(SystemClock.elapsedRealtime(), true);
    }

    @Override
    public void onArrowRightClick() {
        synchronized (mLock) {
            if (mCurrentLocPoint == null) return;
            mCurrentLocPoint.setLongitude(mCurrentLocPoint.getLongitude() + mFixedJoystickIncrement);
            mCurrentSpeedMps = 0f;
        }
        persistSessionMaybe(SystemClock.elapsedRealtime(), true);
    }

    @Override
    public void onStep(float bearingDegrees, long timestampMs) {
        synchronized (mLock) {
            if (
                !MotionControlPolicy.shouldAcceptRealMovementStep(
                    mIsStarted,
                    mFollowRealMovementEnabled,
                    mIsFlyMode
                )
                || mCurrentLocPoint == null
            )
                return;

            LocPoint moved = GeoMover.move(mCurrentLocPoint, mStepLengthMeters, bearingDegrees);
            if (moved == null) return;

            long elapsedMs = (mLastAcceptedStepMs > 0L) ? (timestampMs - mLastAcceptedStepMs) : 650L;
            if (elapsedMs <= 0L || elapsedMs > 5000L) elapsedMs = 650L;

            float instantaneousSpeed = (float) (mStepLengthMeters / (elapsedMs / 1000d));
            instantaneousSpeed = Math.max(0f, Math.min(4.5f, instantaneousSpeed));

            mCurrentSpeedMps = (mCurrentSpeedMps <= 0f)
                ? instantaneousSpeed
                : ((0.35f * mCurrentSpeedMps) + (0.65f * instantaneousSpeed));

            mCurrentBearingDeg = normalizeBearing(bearingDegrees);
            mCurrentLocPoint = moved;
            mLastAcceptedStepMs = timestampMs;
        }

        persistSessionMaybe(timestampMs, false);
    }

    public void onInjectionResult(boolean success, long nowMs) {
        synchronized (mLock) {
            if (!mIsStarted) return;

            if (success) {
                mWatchdog.markInjectionSuccess(nowMs);
            }
            else {
                mWatchdog.markInjectionFailure(nowMs);
            }
        }

        if (success) {
            persistSessionMaybe(nowMs, false);
        }
    }

    public boolean shouldRecover(long nowMs) {
        synchronized (mLock) {
            return mIsStarted && mWatchdog.shouldRecover(nowMs);
        }
    }

    public void markRecoveryStarted(long nowMs) {
        synchronized (mLock) {
            mWatchdog.markRecoveryStarted(nowMs);
        }
    }

    public void markRecoverySucceeded(long nowMs) {
        synchronized (mLock) {
            mWatchdog.markRecoverySucceeded(nowMs);
        }
        persistSessionMaybe(nowMs, true);
    }

    public MockWatchdog.State getWatchdogState() {
        synchronized (mLock) {
            return mWatchdog.getState();
        }
    }

    @Override
    public void onSharedPrefsChange(short diff_fields) {
        importSharedPrefs();
    }

    private void importSharedPrefs() {
        if (mContext == null) return;

        SharedPrefsState prefsState = new SharedPrefsState(mContext, true);

        if (mFixedCount != prefsState.fixed_count) {
            mFixedCount = prefsState.fixed_count;

            synchronized (mLock) {
                if (mIsStarted && !mIsFlyMode && !mFollowRealMovementEnabled) {
                    mFixedCountRemaining = mFixedCount;
                }
            }
        }

        if (mTimeInterval != prefsState.time_interval) {
            updateFlyTime(prefsState.time_interval);
            mTimeInterval = prefsState.time_interval;

            if ((mLocationThread != null) && mLocationThread.isAlive()) {
                mLocationThread.updateTimeInterval(mTimeInterval);
            }
        }

        if (mFixedJoystickEnabled != prefsState.fixed_joystick_enabled) {
            mFixedJoystickEnabled = prefsState.fixed_joystick_enabled;
        }

        mFixedJoystickIncrement = prefsState.fixed_joystick_increment;
        mTripHoldDestination = prefsState.trip_hold_destination;

        boolean newFollowEnabled = EnhancedPrefs.getFollowRealMovementEnabled(mContext);
        double newStepLength = EnhancedPrefs.getStepLengthMeters(mContext);
        boolean newWatchdogEnabled = EnhancedPrefs.getWatchdogEnabled(mContext);
        int newWatchdogTimeoutSeconds = EnhancedPrefs.getWatchdogTimeoutSeconds(mContext);
        boolean newAggressiveKeepAlive = EnhancedPrefs.getAggressiveKeepAlive(mContext);

        synchronized (mLock) {
            // Keep the configured Follow state even during a simulated trip.
            // onStep() ignores physical steps while fly mode is active, then
            // resumes them automatically as soon as the trip finishes.
            mFollowRealMovementEnabled = newFollowEnabled;
            mStepLengthMeters = newStepLength;
            mWatchdogEnabled = newWatchdogEnabled;
            mWatchdogTimeoutSeconds = newWatchdogTimeoutSeconds;
            mAggressiveKeepAlive = newAggressiveKeepAlive;
            mWatchdog.configure(
                mWatchdogEnabled,
                mWatchdogTimeoutSeconds * 1000L,
                WATCHDOG_RETRY_INTERVAL_MS
            );

        }

        updateMotionAndJoystickState();
    }

    private void updateMotionAndJoystickState() {
        boolean started;
        boolean follow;
        boolean fly;
        boolean joystickEnabled;

        synchronized (mLock) {
            started = mIsStarted;
            follow = mFollowRealMovementEnabled;
            fly = mIsFlyMode;
            joystickEnabled = mFixedJoystickEnabled;
        }

        if (!started) {
            if (mRelativeMotionTracker != null) mRelativeMotionTracker.stop();
            hideJoyStick();
            return;
        }

        if (MotionControlPolicy.shouldRunMotionTracker(started, follow)) {
            if (mRelativeMotionTracker != null) mRelativeMotionTracker.start();
        }
        else {
            if (mRelativeMotionTracker != null) mRelativeMotionTracker.stop();
        }

        boolean showJoystick = MotionControlPolicy.shouldShowJoystick(
            started,
            follow,
            fly,
            joystickEnabled,
            RuntimePermissions.canDrawOverlays(mContext)
        );

        if (showJoystick) {
            showJoyStick();
        }
        else {
            hideJoyStick();
        }
    }

    private void persistSessionMaybe(long nowMs, boolean force) {
        if (mContext == null) return;

        SessionSnapshot snapshot;
        synchronized (mLock) {
            if (!mIsStarted || mCurrentLocPoint == null) return;
            if (!force && mLastSessionPersistMs > 0L && (nowMs - mLastSessionPersistMs) < SESSION_PERSIST_INTERVAL_MS)
                return;

            mLastSessionPersistMs = nowMs;
            String mode = mIsFlyMode
                ? SessionSnapshot.MODE_TRIP
                : (mFollowRealMovementEnabled
                    ? SessionSnapshot.MODE_FOLLOW_REAL_MOVEMENT
                    : SessionSnapshot.MODE_FIXED);

            double tripTargetLatitude = 0d;
            double tripTargetLongitude = 0d;
            long tripRemainingMs = 0L;

            if (mIsFlyMode && mTargetLocPoint != null) {
                tripTargetLatitude = mTargetLocPoint.getLatitude();
                tripTargetLongitude = mTargetLocPoint.getLongitude();
                tripRemainingMs = TripProgress.remainingMillis(
                    mFlyTime,
                    mFlyTimeIndex,
                    mTimeInterval
                );
            }

            snapshot = new SessionSnapshot(
                true,
                mCurrentLocPoint.getLatitude(),
                mCurrentLocPoint.getLongitude(),
                mFollowRealMovementEnabled,
                mStepLengthMeters,
                mAggressiveKeepAlive,
                mode,
                tripTargetLatitude,
                tripTargetLongitude,
                tripRemainingMs
            );
        }

        MockSessionState.save(mContext, snapshot);
    }

    private void updateStationarySpeedLocked(long nowMs) {
        if (!mFollowRealMovementEnabled || mLastAcceptedStepMs <= 0L) return;

        long quietMs = nowMs - mLastAcceptedStepMs;
        if (quietMs >= 8000L) {
            mCurrentSpeedMps = 0f;
        }
        else if (quietMs >= 2500L) {
            mCurrentSpeedMps *= 0.90f;
            if (mCurrentSpeedMps < 0.05f) mCurrentSpeedMps = 0f;
        }
    }

    private static float normalizeBearing(float bearing) {
        if (Float.isNaN(bearing) || Float.isInfinite(bearing)) return 1f;
        float normalized = bearing % 360f;
        if (normalized < 0f) normalized += 360f;
        return normalized;
    }

    private void updateFlyTime(int new_time_interval) {
        synchronized (mLock) {
            if (!mIsStarted || !mIsFlyMode || (mFlyTimeIndex >= mFlyTime))
                return;

            int remaining_trip_duration_seconds =
                convertFlyTime_loopIterationsToSeconds(mFlyTime - mFlyTimeIndex, mTimeInterval);
            int remaining_trip_duration_iterations =
                convertFlyTime_secondsToLoopIterations(remaining_trip_duration_seconds, new_time_interval);

            mOriginLocPoint = new LocPoint(mCurrentLocPoint);
            mFlyTimeIndex = 0;
            mFlyTime = remaining_trip_duration_iterations;
        }
    }

    private static int convertFlyTime_secondsToLoopIterations(int trip_duration_seconds, int time_interval) {
        if (time_interval <= 0) time_interval = 100;
        return (int) Math.ceil((1000f / time_interval) * trip_duration_seconds);
    }

    private static int convertFlyTime_loopIterationsToSeconds(int trip_duration_iterations, int time_interval) {
        if (time_interval <= 0) time_interval = 100;
        return (int) Math.ceil((time_interval / 1000f) * trip_duration_iterations);
    }
}

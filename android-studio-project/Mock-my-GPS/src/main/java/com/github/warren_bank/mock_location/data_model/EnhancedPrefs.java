package com.github.warren_bank.mock_location.data_model;

import android.content.Context;
import android.content.SharedPreferences;

public final class EnhancedPrefs {
    private static final String KEY_FOLLOW_REAL_MOVEMENT = "follow_real_movement_enabled";
    private static final String KEY_STEP_LENGTH_METERS = "follow_real_movement_step_length_m";
    private static final String KEY_WATCHDOG_ENABLED = "mock_watchdog_enabled";
    private static final String KEY_WATCHDOG_TIMEOUT_SECONDS = "mock_watchdog_timeout_seconds";
    private static final String KEY_AGGRESSIVE_KEEP_ALIVE = "aggressive_keep_alive";

    public static final double DEFAULT_STEP_LENGTH_METERS = 0.74d;
    public static final int DEFAULT_WATCHDOG_TIMEOUT_SECONDS = 15;

    private EnhancedPrefs() {}

    public static boolean getFollowRealMovementEnabled(Context context) {
        return prefs(context).getBoolean(KEY_FOLLOW_REAL_MOVEMENT, false);
    }

    public static double getStepLengthMeters(Context context) {
        long bits = prefs(context).getLong(
            KEY_STEP_LENGTH_METERS,
            Double.doubleToLongBits(DEFAULT_STEP_LENGTH_METERS)
        );
        return clampStepLength(Double.longBitsToDouble(bits));
    }

    public static boolean getWatchdogEnabled(Context context) {
        return prefs(context).getBoolean(KEY_WATCHDOG_ENABLED, true);
    }

    public static int getWatchdogTimeoutSeconds(Context context) {
        return clampWatchdogTimeout(
            prefs(context).getInt(KEY_WATCHDOG_TIMEOUT_SECONDS, DEFAULT_WATCHDOG_TIMEOUT_SECONDS)
        );
    }

    public static boolean getAggressiveKeepAlive(Context context) {
        return prefs(context).getBoolean(KEY_AGGRESSIVE_KEEP_ALIVE, false);
    }

    public static void save(
        Context context,
        boolean followRealMovement,
        double stepLengthMeters,
        boolean watchdogEnabled,
        int watchdogTimeoutSeconds,
        boolean aggressiveKeepAlive
    ) {
        SharedPreferences.Editor editor = prefs(context).edit();
        editor.putBoolean(KEY_FOLLOW_REAL_MOVEMENT, followRealMovement);
        editor.putLong(KEY_STEP_LENGTH_METERS, Double.doubleToLongBits(clampStepLength(stepLengthMeters)));
        editor.putBoolean(KEY_WATCHDOG_ENABLED, watchdogEnabled);
        editor.putInt(KEY_WATCHDOG_TIMEOUT_SECONDS, clampWatchdogTimeout(watchdogTimeoutSeconds));
        editor.putBoolean(KEY_AGGRESSIVE_KEEP_ALIVE, aggressiveKeepAlive);
        editor.commit();
    }

    public static void restoreSessionValues(
        Context context,
        boolean followRealMovement,
        double stepLengthMeters,
        boolean aggressiveKeepAlive
    ) {
        SharedPreferences.Editor editor = prefs(context).edit();
        editor.putBoolean(KEY_FOLLOW_REAL_MOVEMENT, followRealMovement);
        editor.putLong(KEY_STEP_LENGTH_METERS, Double.doubleToLongBits(clampStepLength(stepLengthMeters)));
        editor.putBoolean(KEY_AGGRESSIVE_KEEP_ALIVE, aggressiveKeepAlive);
        editor.commit();
    }

    public static double clampStepLength(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0d)
            return DEFAULT_STEP_LENGTH_METERS;
        return Math.max(0.20d, Math.min(2.00d, value));
    }

    public static int clampWatchdogTimeout(int seconds) {
        return Math.max(5, Math.min(120, seconds));
    }

    private static SharedPreferences prefs(Context context) {
        return SharedPrefs.getSharedPreferences(context);
    }
}

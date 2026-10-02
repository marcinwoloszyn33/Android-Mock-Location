package com.github.warren_bank.mock_location.service.trip;

import com.github.warren_bank.mock_location.data_model.LocPoint;
import com.github.warren_bank.mock_location.data_model.SharedPrefs;

import android.content.Context;
import android.content.SharedPreferences;

public final class TripDraftPrefs {
    private static final String KEY_INITIALIZED = "trip_draft_v14_initialized";
    private static final String KEY_DIRTY       = "trip_draft_v14_dirty";
    private static final String KEY_ORIGIN      = "trip_draft_v14_origin";
    private static final String KEY_WAYPOINTS   = "trip_draft_v14_waypoints";
    private static final String KEY_DESTINATION = "trip_draft_v14_destination";
    private static final String KEY_DURATION    = "trip_draft_v14_duration";
    private static final String KEY_TYPE        = "trip_draft_v14_type";
    private static final String KEY_AMPLITUDE   = "trip_draft_v14_amplitude";
    private static final String KEY_CYCLES      = "trip_draft_v14_cycles";
    private static final String KEY_WRAPS       = "trip_draft_v14_wraps";

    private TripDraftPrefs() {}

    public static void ensureInitialized(Context context) {
        SharedPreferences prefs = SharedPrefs.getSharedPreferences(context);
        if (prefs.getBoolean(KEY_INITIALIZED, false)) return;
        LocPoint origin = SharedPrefs.getTripOrigin(context);
        LocPoint destination = SharedPrefs.getTripDestination(context);
        prefs.edit()
            .putBoolean(KEY_INITIALIZED, true)
            .putBoolean(KEY_DIRTY, false)
            .putString(KEY_ORIGIN, origin.toString())
            .putString(KEY_WAYPOINTS, TripPathPrefs.getWaypointsText(context))
            .putString(KEY_DESTINATION, destination.toString())
            .putString(KEY_DURATION, Integer.toString(SharedPrefs.getTripDuration(context)))
            .putInt(KEY_TYPE, TripPathPrefs.getType(context))
            .putString(KEY_AMPLITUDE, Double.toString(TripPathPrefs.getAmplitude(context)))
            .putString(KEY_CYCLES, Integer.toString(TripPathPrefs.getCycles(context)))
            .putString(KEY_WRAPS, Integer.toString(TripPathPrefs.getWraps(context)))
            .commit();
    }

    public static void save(Context context, String origin, String waypoints, String destination,
                            String duration, int type, String amplitude, String cycles,
                            String wraps, boolean dirty) {
        SharedPrefs.getSharedPreferences(context).edit()
            .putBoolean(KEY_INITIALIZED, true)
            .putBoolean(KEY_DIRTY, dirty)
            .putString(KEY_ORIGIN, safe(origin))
            .putString(KEY_WAYPOINTS, safe(waypoints))
            .putString(KEY_DESTINATION, safe(destination))
            .putString(KEY_DURATION, safe(duration))
            .putInt(KEY_TYPE, TripPathGenerator.sanitizeType(type))
            .putString(KEY_AMPLITUDE, safe(amplitude))
            .putString(KEY_CYCLES, safe(cycles))
            .putString(KEY_WRAPS, safe(wraps))
            .commit();
    }

    public static boolean isDirty(Context context) {
        ensureInitialized(context);
        return SharedPrefs.getSharedPreferences(context).getBoolean(KEY_DIRTY, false);
    }
    public static void setDirty(Context context, boolean dirty) {
        ensureInitialized(context);
        SharedPrefs.getSharedPreferences(context).edit().putBoolean(KEY_DIRTY, dirty).commit();
    }
    public static String getOrigin(Context c) { ensureInitialized(c); return SharedPrefs.getSharedPreferences(c).getString(KEY_ORIGIN, ""); }
    public static String getWaypoints(Context c) { ensureInitialized(c); return SharedPrefs.getSharedPreferences(c).getString(KEY_WAYPOINTS, ""); }
    public static String getDestination(Context c) { ensureInitialized(c); return SharedPrefs.getSharedPreferences(c).getString(KEY_DESTINATION, ""); }
    public static String getDuration(Context c) { ensureInitialized(c); return SharedPrefs.getSharedPreferences(c).getString(KEY_DURATION, "60"); }
    public static int getType(Context c) { ensureInitialized(c); return TripPathGenerator.sanitizeType(SharedPrefs.getSharedPreferences(c).getInt(KEY_TYPE, TripPathGenerator.TYPE_STRAIGHT)); }
    public static String getAmplitude(Context c) { ensureInitialized(c); return SharedPrefs.getSharedPreferences(c).getString(KEY_AMPLITUDE, Double.toString(TripPathGenerator.DEFAULT_AMPLITUDE_METERS)); }
    public static String getCycles(Context c) { ensureInitialized(c); return SharedPrefs.getSharedPreferences(c).getString(KEY_CYCLES, Integer.toString(TripPathGenerator.DEFAULT_CYCLES)); }
    public static String getWraps(Context c) { ensureInitialized(c); return SharedPrefs.getSharedPreferences(c).getString(KEY_WRAPS, Integer.toString(TripPathGenerator.DEFAULT_WRAPS)); }
    private static String safe(String value) { return value == null ? "" : value; }
}

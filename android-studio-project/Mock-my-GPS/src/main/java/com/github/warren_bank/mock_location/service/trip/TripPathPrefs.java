package com.github.warren_bank.mock_location.service.trip;

import com.github.warren_bank.mock_location.data_model.SharedPrefs;
import android.content.Context;
import android.content.SharedPreferences;

public final class TripPathPrefs {
    private static final String KEY_TYPE = "trip_shape_type_v9";
    private static final String KEY_AMPLITUDE = "trip_shape_amplitude_v9";
    private static final String KEY_CYCLES = "trip_shape_cycles_v9";
    private static final String KEY_WRAPS = "trip_global_wraps_v12";
    private TripPathPrefs() {}
    public static int getType(Context c) { return TripPathGenerator.sanitizeType(SharedPrefs.getSharedPreferences(c).getInt(KEY_TYPE,TripPathGenerator.TYPE_STRAIGHT)); }
    public static double getAmplitude(Context c) { long b=SharedPrefs.getSharedPreferences(c).getLong(KEY_AMPLITUDE,Double.doubleToLongBits(TripPathGenerator.DEFAULT_AMPLITUDE_METERS)); return TripPathGenerator.sanitizeAmplitude(Double.longBitsToDouble(b)); }
    public static int getCycles(Context c) { return TripPathGenerator.sanitizeCycles(SharedPrefs.getSharedPreferences(c).getInt(KEY_CYCLES,TripPathGenerator.DEFAULT_CYCLES)); }
    public static int getWraps(Context c) { return TripPathGenerator.sanitizeWraps(SharedPrefs.getSharedPreferences(c).getInt(KEY_WRAPS,TripPathGenerator.DEFAULT_WRAPS)); }
    public static void save(Context c,int type,double amplitude,int cycles,int wraps) { SharedPreferences.Editor e=SharedPrefs.getSharedPreferences(c).edit(); e.putInt(KEY_TYPE,TripPathGenerator.sanitizeType(type)); e.putLong(KEY_AMPLITUDE,Double.doubleToLongBits(TripPathGenerator.sanitizeAmplitude(amplitude))); e.putInt(KEY_CYCLES,TripPathGenerator.sanitizeCycles(cycles)); e.putInt(KEY_WRAPS,TripPathGenerator.sanitizeWraps(wraps)); e.commit(); }
}

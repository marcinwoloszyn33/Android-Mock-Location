package com.github.warren_bank.mock_location.service.trip;

import com.github.warren_bank.mock_location.data_model.SharedPrefs;

import android.content.Context;
import android.content.SharedPreferences;

public final class TripPathPrefs {
    private static final String KEY_TYPE = "trip_shape_type_v9";
    private static final String KEY_AMPLITUDE = "trip_shape_amplitude_v9";
    private static final String KEY_CYCLES = "trip_shape_cycles_v9";

    private TripPathPrefs() {}

    public static int getType(Context context) {
        int value = SharedPrefs.getSharedPreferences(context)
            .getInt(KEY_TYPE, TripPathGenerator.TYPE_STRAIGHT);
        return TripPathGenerator.sanitizeType(value);
    }

    public static double getAmplitude(Context context) {
        long bits = SharedPrefs.getSharedPreferences(context).getLong(
            KEY_AMPLITUDE,
            Double.doubleToLongBits(TripPathGenerator.DEFAULT_AMPLITUDE_METERS)
        );
        return TripPathGenerator.sanitizeAmplitude(Double.longBitsToDouble(bits));
    }

    public static int getCycles(Context context) {
        int value = SharedPrefs.getSharedPreferences(context)
            .getInt(KEY_CYCLES, TripPathGenerator.DEFAULT_CYCLES);
        return TripPathGenerator.sanitizeCycles(value);
    }

    public static void save(
        Context context,
        int type,
        double amplitude,
        int cycles
    ) {
        SharedPreferences.Editor editor =
            SharedPrefs.getSharedPreferences(context).edit();
        editor.putInt(KEY_TYPE, TripPathGenerator.sanitizeType(type));
        editor.putLong(
            KEY_AMPLITUDE,
            Double.doubleToLongBits(
                TripPathGenerator.sanitizeAmplitude(amplitude)
            )
        );
        editor.putInt(KEY_CYCLES, TripPathGenerator.sanitizeCycles(cycles));
        editor.commit();
    }
}

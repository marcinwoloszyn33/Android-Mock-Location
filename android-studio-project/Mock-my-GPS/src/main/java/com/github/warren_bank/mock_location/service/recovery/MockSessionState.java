package com.github.warren_bank.mock_location.service.recovery;

import com.github.warren_bank.mock_location.data_model.SharedPrefs;

import android.content.Context;
import android.content.SharedPreferences;

public final class MockSessionState {
    private static final String KEY = "mock_session_state_v1";

    private MockSessionState() {}

    public static SessionSnapshot load(Context context) {
        SharedPreferences prefs = SharedPrefs.getSharedPreferences(context);
        return SessionSnapshot.fromJson(prefs.getString(KEY, null));
    }

    public static void save(Context context, SessionSnapshot snapshot) {
        if (snapshot == null) snapshot = SessionSnapshot.inactive();
        SharedPrefs.getSharedPreferences(context)
            .edit()
            .putString(KEY, snapshot.toJson())
            .commit();
    }

    public static void clear(Context context) {
        SharedPrefs.getSharedPreferences(context)
            .edit()
            .remove(KEY)
            .commit();
    }
}

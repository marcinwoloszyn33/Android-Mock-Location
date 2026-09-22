package com.github.warren_bank.mock_location.ui;

import com.github.warren_bank.mock_location.R;
import com.github.warren_bank.mock_location.data_model.EnhancedPrefs;
import com.github.warren_bank.mock_location.data_model.SharedPrefs;
import com.github.warren_bank.mock_location.data_model.SharedPrefsState;
import com.github.warren_bank.mock_location.service.LocationService;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

public class PreferencesActivity extends Activity {
    private static final int REQUEST_ACTIVITY_RECOGNITION = 4201;

    private SharedPrefsState originalState;

    private boolean originalFollowRealMovement;
    private double originalStepLengthMeters;
    private boolean originalWatchdogEnabled;
    private int originalWatchdogTimeoutSeconds;
    private boolean originalAggressiveKeepAlive;

    private TextView input_time_interval;
    private TextView input_fixed_count;
    private CheckBox input_fixed_joystick_enabled;
    private TextView input_fixed_joystick_increment;
    private CheckBox input_trip_hold_destination;

    private CheckBox input_follow_real_movement;
    private TextView input_step_length_meters;
    private CheckBox input_watchdog_enabled;
    private TextView input_watchdog_timeout_seconds;
    private CheckBox input_aggressive_keep_alive;
    private Button button_leak_check;

    private Button button_cancel;
    private Button button_save;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        originalState = new SharedPrefsState(PreferencesActivity.this, true);

        originalFollowRealMovement = EnhancedPrefs.getFollowRealMovementEnabled(this);
        originalStepLengthMeters = EnhancedPrefs.getStepLengthMeters(this);
        originalWatchdogEnabled = EnhancedPrefs.getWatchdogEnabled(this);
        originalWatchdogTimeoutSeconds = EnhancedPrefs.getWatchdogTimeoutSeconds(this);
        originalAggressiveKeepAlive = EnhancedPrefs.getAggressiveKeepAlive(this);

        input_time_interval = (TextView) findViewById(R.id.input_time_interval);
        input_fixed_count = (TextView) findViewById(R.id.input_fixed_count);
        input_fixed_joystick_enabled = (CheckBox) findViewById(R.id.input_fixed_joystick_enabled);
        input_fixed_joystick_increment = (TextView) findViewById(R.id.input_fixed_joystick_increment);
        input_trip_hold_destination = (CheckBox) findViewById(R.id.input_trip_hold_destination);

        input_follow_real_movement = (CheckBox) findViewById(R.id.input_follow_real_movement);
        input_step_length_meters = (TextView) findViewById(R.id.input_step_length_meters);
        input_watchdog_enabled = (CheckBox) findViewById(R.id.input_watchdog_enabled);
        input_watchdog_timeout_seconds = (TextView) findViewById(R.id.input_watchdog_timeout_seconds);
        input_aggressive_keep_alive = (CheckBox) findViewById(R.id.input_aggressive_keep_alive);
        button_leak_check = (Button) findViewById(R.id.button_leak_check);

        button_cancel = (Button) findViewById(R.id.button_cancel);
        button_save = (Button) findViewById(R.id.button_save);

        reset();

        input_follow_real_movement.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    requestActivityRecognitionPermissionIfNeeded();
                }
            }
        });

        if (input_follow_real_movement.isChecked()) {
            requestActivityRecognitionPermissionIfNeeded();
        }

        button_leak_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PreferencesActivity.this, LeakCheckActivity.class));
            }
        });

        button_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreferencesActivity.this.finish();
            }
        });

        button_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAndClose();
            }
        });
    }

    private void saveAndClose() {
        int time_interval;
        int fixed_count;
        boolean fixed_joystick_enabled;
        double fixed_joystick_increment;
        boolean trip_hold_destination;

        boolean follow_real_movement;
        double step_length_meters;
        boolean watchdog_enabled;
        int watchdog_timeout_seconds;
        boolean aggressive_keep_alive;

        String text = null;

        try {
            text = input_time_interval.getText().toString();
            time_interval = Integer.parseInt(text, 10);
        }
        catch (Exception e) {
            showError(getString(R.string.error_number_format, text));
            return;
        }

        try {
            text = input_fixed_count.getText().toString();
            fixed_count = Integer.parseInt(text, 10);
        }
        catch (Exception e) {
            showError(getString(R.string.error_number_format, text));
            return;
        }

        fixed_joystick_enabled = input_fixed_joystick_enabled.isChecked();

        try {
            text = input_fixed_joystick_increment.getText().toString();
            fixed_joystick_increment = Double.parseDouble(text.replace(',', '.'));
        }
        catch (Exception e) {
            showError(getString(R.string.error_number_format, text));
            return;
        }

        trip_hold_destination = input_trip_hold_destination.isChecked();

        follow_real_movement = input_follow_real_movement.isChecked();

        try {
            text = input_step_length_meters.getText().toString();
            step_length_meters = EnhancedPrefs.clampStepLength(
                Double.parseDouble(text.replace(',', '.'))
            );
        }
        catch (Exception e) {
            showError(getString(R.string.error_number_format, text));
            return;
        }

        watchdog_enabled = input_watchdog_enabled.isChecked();

        try {
            text = input_watchdog_timeout_seconds.getText().toString();
            watchdog_timeout_seconds = EnhancedPrefs.clampWatchdogTimeout(
                Integer.parseInt(text, 10)
            );
        }
        catch (Exception e) {
            showError(getString(R.string.error_number_format, text));
            return;
        }

        aggressive_keep_alive = input_aggressive_keep_alive.isChecked();

        SharedPrefsState modifiedState = new SharedPrefsState(
            originalState.bookmarks,
            time_interval,
            fixed_count,
            fixed_joystick_enabled,
            fixed_joystick_increment,
            trip_hold_destination,
            originalState.trip_origin_lat,
            originalState.trip_origin_lon,
            originalState.trip_destination_lat,
            originalState.trip_destination_lon,
            originalState.trip_duration
        );

        short diff_fields = originalState.diff(modifiedState);
        boolean existingChanged = (diff_fields != 0);

        if (existingChanged) {
            SharedPreferences.Editor editor = SharedPrefs.getSharedPreferencesEditor(PreferencesActivity.this);
            boolean flush = false;
            short mask;

            mask = (1 << 1);
            if ((diff_fields & mask) == mask) {
                SharedPrefs.putTimeInterval(editor, PreferencesActivity.this, time_interval, flush);
            }

            mask = (1 << 2);
            if ((diff_fields & mask) == mask) {
                SharedPrefs.putFixedCount(editor, PreferencesActivity.this, fixed_count, flush);
            }

            mask = (1 << 3);
            if ((diff_fields & mask) == mask) {
                SharedPrefs.putFixedJoystickEnabled(editor, PreferencesActivity.this, fixed_joystick_enabled, flush);
            }

            mask = (1 << 4);
            if ((diff_fields & mask) == mask) {
                SharedPrefs.putFixedJoystickIncrement(editor, PreferencesActivity.this, fixed_joystick_increment, flush);
            }

            mask = (1 << 5);
            if ((diff_fields & mask) == mask) {
                SharedPrefs.putTripHoldDestination(editor, PreferencesActivity.this, trip_hold_destination, flush);
            }

            editor.commit();
        }

        boolean enhancedChanged =
               (follow_real_movement != originalFollowRealMovement)
            || (Math.abs(step_length_meters - originalStepLengthMeters) > 1e-9d)
            || (watchdog_enabled != originalWatchdogEnabled)
            || (watchdog_timeout_seconds != originalWatchdogTimeoutSeconds)
            || (aggressive_keep_alive != originalAggressiveKeepAlive);

        if (enhancedChanged) {
            EnhancedPrefs.save(
                PreferencesActivity.this,
                follow_real_movement,
                step_length_meters,
                watchdog_enabled,
                watchdog_timeout_seconds,
                aggressive_keep_alive
            );
        }

        if (existingChanged || enhancedChanged) {
            LocationService.doSharedPrefsChange(PreferencesActivity.this, true);
        }

        PreferencesActivity.this.finish();
    }

    private void reset() {
        input_time_interval.setText(Integer.toString(originalState.time_interval, 10));
        input_fixed_count.setText(Integer.toString(originalState.fixed_count, 10));
        input_fixed_joystick_enabled.setChecked(originalState.fixed_joystick_enabled);
        input_fixed_joystick_increment.setText(Double.toString(originalState.fixed_joystick_increment));
        input_trip_hold_destination.setChecked(originalState.trip_hold_destination);

        input_follow_real_movement.setChecked(originalFollowRealMovement);
        input_step_length_meters.setText(Double.toString(originalStepLengthMeters));
        input_watchdog_enabled.setChecked(originalWatchdogEnabled);
        input_watchdog_timeout_seconds.setText(Integer.toString(originalWatchdogTimeoutSeconds, 10));
        input_aggressive_keep_alive.setChecked(originalAggressiveKeepAlive);
    }

    private void requestActivityRecognitionPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;

        if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        requestPermissions(
            new String[] { Manifest.permission.ACTIVITY_RECOGNITION },
            REQUEST_ACTIVITY_RECOGNITION
        );
    }

    private void showError(String text) {
        Toast.makeText(PreferencesActivity.this, text, Toast.LENGTH_SHORT).show();
    }
}

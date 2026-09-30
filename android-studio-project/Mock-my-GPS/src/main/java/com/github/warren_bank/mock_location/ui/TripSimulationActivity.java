package com.github.warren_bank.mock_location.ui;

import com.github.warren_bank.mock_location.R;
import com.github.warren_bank.mock_location.data_model.BookmarkItem;
import com.github.warren_bank.mock_location.data_model.LocPoint;
import com.github.warren_bank.mock_location.data_model.SharedPrefs;
import com.github.warren_bank.mock_location.security_model.RuntimePermissions;
import com.github.warren_bank.mock_location.service.LocationService;
import com.github.warren_bank.mock_location.service.trip.TripPathGenerator;
import com.github.warren_bank.mock_location.service.trip.TripPathPrefs;
import com.github.warren_bank.mock_location.service.trip.TripWaypointCodec;
import com.github.warren_bank.mock_location.ui.logic.TripEditState;
import com.github.warren_bank.mock_location.ui.logic.TripStartPermissionPolicy;
import com.github.warren_bank.mock_location.ui.interfaces.RuntimePermissionsListener;
import com.github.warren_bank.mock_location.ui.interfaces.RuntimePermissionsRequester;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class TripSimulationActivity extends Activity
implements RuntimePermissionsListener {
    private LocPoint originalLocOrigin;
    private LocPoint originalLocDestination;
    private int originalTripDuration;
    private int originalPathType;
    private double originalPathAmplitude;
    private int originalPathCycles;
    private int originalPathWraps;
    private String originalWaypointsText;

    private TextView label_trip_origin;
    private TextView input_trip_origin;
    private TextView input_trip_waypoints;
    private TextView label_trip_destination;
    private TextView input_trip_destination;
    private TextView input_trip_duration;
    private Spinner input_trip_path_type;
    private TextView input_trip_path_amplitude;
    private TextView input_trip_path_cycles;
    private TextView input_trip_path_wraps;
    private Button button_paste_trip_origin;
    private Button button_paste_trip_waypoints;
    private Button button_paste_trip_destination;
    private Button button_toggle_state;
    private Button button_update;

    private short diff_fields = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_simulation);
        loadOriginals();

        label_trip_origin = (TextView) findViewById(R.id.label_trip_origin);
        input_trip_origin = (TextView) findViewById(R.id.input_trip_origin);
        input_trip_waypoints = (TextView) findViewById(R.id.input_trip_waypoints);
        label_trip_destination = (TextView) findViewById(R.id.label_trip_destination);
        input_trip_destination = (TextView) findViewById(R.id.input_trip_destination);
        input_trip_duration = (TextView) findViewById(R.id.input_trip_duration);
        input_trip_path_type = (Spinner) findViewById(R.id.input_trip_path_type);
        input_trip_path_amplitude = (TextView) findViewById(R.id.input_trip_path_amplitude);
        input_trip_path_cycles = (TextView) findViewById(R.id.input_trip_path_cycles);
        input_trip_path_wraps = (TextView) findViewById(R.id.input_trip_path_wraps);
        button_paste_trip_origin = (Button) findViewById(R.id.button_paste_trip_origin);
        button_paste_trip_waypoints = (Button) findViewById(R.id.button_paste_trip_waypoints);
        button_paste_trip_destination = (Button) findViewById(R.id.button_paste_trip_destination);
        button_toggle_state = (Button) findViewById(R.id.button_toggle_state);
        button_update = (Button) findViewById(R.id.button_update);

        input_trip_origin.addTextChangedListener(new SimpleWatcher() {
            public void afterTextChanged(Editable s) {
                label_trip_origin.setVisibility(View.GONE);
                if (!LocationService.isTripModeStarted()) return;
                try {
                    LocPoint value = new LocPoint(s.toString());
                    diff_fields = TripEditState.update(diff_fields, TripEditState.ORIGIN_MASK, !originalLocOrigin.equals(value));
                    checkDiff();
                } catch(Exception e) {}
            }
        });

        input_trip_waypoints.addTextChangedListener(new SimpleWatcher() {
            public void afterTextChanged(Editable s) {
                if (!LocationService.isTripModeStarted()) return;
                String value = TripWaypointCodec.normalize(s.toString());
                diff_fields = TripEditState.update(diff_fields, TripEditState.WAYPOINTS_MASK, !originalWaypointsText.equals(value));
                checkDiff();
            }
        });

        input_trip_destination.addTextChangedListener(new SimpleWatcher() {
            public void afterTextChanged(Editable s) {
                label_trip_destination.setVisibility(View.GONE);
                if (!LocationService.isTripModeStarted()) return;
                try {
                    LocPoint value = new LocPoint(s.toString());
                    diff_fields = TripEditState.update(diff_fields, TripEditState.DESTINATION_MASK, !originalLocDestination.equals(value));
                    checkDiff();
                } catch(Exception e) {}
            }
        });

        input_trip_duration.addTextChangedListener(new SimpleWatcher() {
            public void afterTextChanged(Editable s) {
                if (!LocationService.isTripModeStarted()) return;
                try {
                    int value = Integer.parseInt(s.toString(), 10);
                    diff_fields = TripEditState.update(diff_fields, TripEditState.DURATION_MASK, originalTripDuration != value);
                    checkDiff();
                } catch(Exception e) {}
            }
        });

        input_trip_path_amplitude.addTextChangedListener(new SimpleWatcher() {
            public void afterTextChanged(Editable s) {
                if (!LocationService.isTripModeStarted()) return;
                try {
                    double value = TripPathGenerator.sanitizeAmplitude(Double.parseDouble(s.toString().replace(',', '.')));
                    diff_fields = TripEditState.update(diff_fields, TripEditState.PATH_AMPLITUDE_MASK, Math.abs(originalPathAmplitude - value) > 1e-9d);
                    checkDiff();
                } catch(Exception e) {}
            }
        });

        input_trip_path_cycles.addTextChangedListener(new SimpleWatcher() {
            public void afterTextChanged(Editable s) {
                if (!LocationService.isTripModeStarted()) return;
                try {
                    int value = TripPathGenerator.sanitizeCycles(Integer.parseInt(s.toString(), 10));
                    diff_fields = TripEditState.update(diff_fields, TripEditState.PATH_CYCLES_MASK, originalPathCycles != value);
                    checkDiff();
                } catch(Exception e) {}
            }
        });

        input_trip_path_wraps.addTextChangedListener(new SimpleWatcher() {
            public void afterTextChanged(Editable s) {
                if (!LocationService.isTripModeStarted()) return;
                try {
                    int value = TripPathGenerator.sanitizeWraps(Integer.parseInt(s.toString(), 10));
                    diff_fields = TripEditState.update(diff_fields, TripEditState.PATH_WRAPS_MASK, originalPathWraps != value);
                    checkDiff();
                } catch(Exception e) {}
            }
        });

        input_trip_path_type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!LocationService.isTripModeStarted()) return;
                int value = TripPathGenerator.sanitizeType(position);
                diff_fields = TripEditState.update(diff_fields, TripEditState.PATH_TYPE_MASK, originalPathType != value);
                checkDiff();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        button_paste_trip_origin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pasteSingleCoordinate(input_trip_origin); }
        });
        button_paste_trip_waypoints.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pasteWaypointList(); }
        });
        button_paste_trip_destination.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pasteSingleCoordinate(input_trip_destination); }
        });

        button_toggle_state.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (LocationService.isTripModeStarted()) {
                    LocationService.doStop(TripSimulationActivity.this, true);
                    button_toggle_state.setText(R.string.label_button_start);
                    button_update.setVisibility(View.GONE);
                } else {
                    requestPermissions();
                }
            }
        });

        button_update.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (LocationService.isTripModeStarted()) requestPermissions();
                else button_update.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocPoint origin = SharedPrefs.getTripOrigin(this);
        LocPoint destination = SharedPrefs.getTripDestination(this);
        int duration = SharedPrefs.getTripDuration(this);
        int pathType = TripPathPrefs.getType(this);
        double amplitude = TripPathPrefs.getAmplitude(this);
        int cycles = TripPathPrefs.getCycles(this);
        int wraps = TripPathPrefs.getWraps(this);
        String waypoints = TripPathPrefs.getWaypointsText(this);

        if (!LocationService.isTripModeStarted()) {
            originalLocOrigin = origin;
            originalLocDestination = destination;
            originalTripDuration = duration;
            originalPathType = pathType;
            originalPathAmplitude = amplitude;
            originalPathCycles = cycles;
            originalPathWraps = wraps;
            originalWaypointsText = waypoints;
            diff_fields = 0;
        }
        reset(origin, destination, duration, pathType, amplitude, cycles, wraps, waypoints);
    }

    private void loadOriginals() {
        originalLocOrigin = SharedPrefs.getTripOrigin(this);
        originalLocDestination = SharedPrefs.getTripDestination(this);
        originalTripDuration = SharedPrefs.getTripDuration(this);
        originalPathType = TripPathPrefs.getType(this);
        originalPathAmplitude = TripPathPrefs.getAmplitude(this);
        originalPathCycles = TripPathPrefs.getCycles(this);
        originalPathWraps = TripPathPrefs.getWraps(this);
        originalWaypointsText = TripPathPrefs.getWaypointsText(this);
    }

    private void reset(LocPoint origin, LocPoint destination, int duration, int pathType, double amplitude, int cycles, int wraps, String waypoints) {
        label_trip_origin.setVisibility(View.GONE);
        label_trip_destination.setVisibility(View.GONE);
        input_trip_origin.setText(origin.toString());
        input_trip_waypoints.setText(TripWaypointCodec.normalize(waypoints));
        input_trip_destination.setText(destination.toString());
        input_trip_duration.setText(Integer.toString(duration, 10));
        input_trip_path_type.setSelection(TripPathGenerator.sanitizeType(pathType));
        input_trip_path_amplitude.setText(Double.toString(TripPathGenerator.sanitizeAmplitude(amplitude)));
        input_trip_path_cycles.setText(Integer.toString(TripPathGenerator.sanitizeCycles(cycles), 10));
        input_trip_path_wraps.setText(Integer.toString(TripPathGenerator.sanitizeWraps(wraps), 10));

        BookmarkItem item = SharedPrefs.getBookmarkItem(this, origin);
        if (item != null) {
            label_trip_origin.setText(item.title);
            label_trip_origin.setVisibility(View.VISIBLE);
        }
        item = SharedPrefs.getBookmarkItem(this, destination);
        if (item != null) {
            label_trip_destination.setText(item.title);
            label_trip_destination.setVisibility(View.VISIBLE);
        }

        if (LocationService.isTripModeStarted()) {
            button_toggle_state.setText(R.string.label_button_stop);
            checkDiff();
        } else {
            button_toggle_state.setText(R.string.label_button_start);
            button_update.setVisibility(View.GONE);
        }
    }

    private void checkDiff() {
        button_update.setVisibility((diff_fields == 0) ? View.GONE : View.VISIBLE);
    }

    private void requestPermissions() {
        boolean granted = RuntimePermissions.hasMandatoryPermissions(this);
        if (TripStartPermissionPolicy.shouldStartImmediately(granted)) {
            doStart();
            return;
        }
        RuntimePermissionsRequester requester = (RuntimePermissionsRequester) getParent();
        requester.requestTripRuntimePermissions(this);
    }

    private String clipboardText() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip() || clipboard.getPrimaryClip() == null || clipboard.getPrimaryClip().getItemCount() == 0) return "";
        CharSequence value = clipboard.getPrimaryClip().getItemAt(0).coerceToText(this);
        return value == null ? "" : value.toString();
    }

    private void pasteSingleCoordinate(TextView target) {
        LocPoint point = TripWaypointCodec.first(clipboardText());
        if (point == null) {
            Toast.makeText(this, "No latitude, longitude pair found in clipboard.", Toast.LENGTH_LONG).show();
            return;
        }
        target.setText(point.toString());
    }

    private void pasteWaypointList() {
        String value = TripWaypointCodec.normalize(clipboardText());
        if (value.isEmpty()) {
            Toast.makeText(this, "No waypoint coordinates found in clipboard.", Toast.LENGTH_LONG).show();
            return;
        }
        input_trip_waypoints.setText(value);
    }

    public void doStart() {
        try {
            LocPoint origin = new LocPoint(input_trip_origin.getText().toString());
            LocPoint destination = new LocPoint(input_trip_destination.getText().toString());
            int duration = Integer.parseInt(input_trip_duration.getText().toString(), 10);
            int pathType = TripPathGenerator.sanitizeType(input_trip_path_type.getSelectedItemPosition());
            double amplitude = TripPathGenerator.sanitizeAmplitude(Double.parseDouble(input_trip_path_amplitude.getText().toString().replace(',', '.')));
            int cycles = TripPathGenerator.sanitizeCycles(Integer.parseInt(input_trip_path_cycles.getText().toString(), 10));
            int wraps = TripPathGenerator.sanitizeWraps(Integer.parseInt(input_trip_path_wraps.getText().toString(), 10));
            String rawWaypoints = input_trip_waypoints.getText().toString();
            String waypoints = TripWaypointCodec.normalize(rawWaypoints);
            if (!rawWaypoints.trim().isEmpty() && waypoints.isEmpty()) throw new NumberFormatException("No valid waypoint coordinates");

            LocationService.doStart(this, true, origin, destination, duration, pathType, amplitude, cycles, wraps, waypoints);
            SharedPrefs.putTripOrigin(this, origin);
            SharedPrefs.putTripDestination(this, destination);
            SharedPrefs.putTripDuration(this, duration);
            TripPathPrefs.save(this, pathType, amplitude, cycles, wraps, waypoints);

            originalLocOrigin = origin;
            originalLocDestination = destination;
            originalTripDuration = duration;
            originalPathType = pathType;
            originalPathAmplitude = amplitude;
            originalPathCycles = cycles;
            originalPathWraps = wraps;
            originalWaypointsText = waypoints;
            diff_fields = 0;
            button_toggle_state.setText(R.string.label_button_stop);
            button_update.setVisibility(View.GONE);
        }
        catch (Exception e) {
            Toast.makeText(this, "Could not start Trip: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private abstract static class SimpleWatcher implements TextWatcher {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}

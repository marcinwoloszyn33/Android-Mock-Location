package com.github.warren_bank.mock_location.ui;

import com.github.warren_bank.mock_location.R;
import com.github.warren_bank.mock_location.data_model.BookmarkItem;
import com.github.warren_bank.mock_location.data_model.LocPoint;
import com.github.warren_bank.mock_location.data_model.SharedPrefs;
import com.github.warren_bank.mock_location.security_model.RuntimePermissions;
import com.github.warren_bank.mock_location.service.LocationService;
import com.github.warren_bank.mock_location.service.trip.TripDraftPrefs;
import com.github.warren_bank.mock_location.service.trip.TripPathGenerator;
import com.github.warren_bank.mock_location.service.trip.TripPathPrefs;
import com.github.warren_bank.mock_location.service.trip.TripWaypointCodec;
import com.github.warren_bank.mock_location.ui.interfaces.RuntimePermissionsListener;
import com.github.warren_bank.mock_location.ui.interfaces.RuntimePermissionsRequester;
import com.github.warren_bank.mock_location.ui.logic.TripStartPermissionPolicy;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class TripSimulationActivity extends Activity implements RuntimePermissionsListener {
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
    private Button button_toggle_state;
    private Button button_update;
    private boolean suppressDraftEvents = false;
    private boolean pendingUpdate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_simulation);
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
        button_toggle_state = (Button) findViewById(R.id.button_toggle_state);
        button_update = (Button) findViewById(R.id.button_update);

        TripDraftPrefs.ensureInitialized(this);
        loadDraftIntoUi();
        installDraftListeners();

        button_toggle_state.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (LocationService.isTripModeStarted()) {
                    saveDraftFromUi(TripDraftPrefs.isDirty(TripSimulationActivity.this));
                    LocationService.doStop(TripSimulationActivity.this, true);
                    TripDraftPrefs.setDirty(TripSimulationActivity.this, false);
                    pendingUpdate = false;
                    updateButtons();
                } else {
                    requestPermissions(false);
                }
            }
        });

        button_update.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (LocationService.isTripModeStarted() && TripDraftPrefs.isDirty(TripSimulationActivity.this)) {
                    requestPermissions(true);
                }
            }
        });
        updateButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // V14: never overwrite the editor from saved active settings here.
        updateButtons();
    }

    @Override
    protected void onPause() {
        saveDraftFromUi(LocationService.isTripModeStarted() && TripDraftPrefs.isDirty(this));
        super.onPause();
    }

    private void loadDraftIntoUi() {
        suppressDraftEvents = true;
        try {
            label_trip_origin.setVisibility(View.GONE);
            label_trip_destination.setVisibility(View.GONE);
            input_trip_origin.setText(TripDraftPrefs.getOrigin(this));
            input_trip_waypoints.setText(TripDraftPrefs.getWaypoints(this));
            input_trip_destination.setText(TripDraftPrefs.getDestination(this));
            input_trip_duration.setText(TripDraftPrefs.getDuration(this));
            input_trip_path_type.setSelection(TripDraftPrefs.getType(this));
            input_trip_path_amplitude.setText(TripDraftPrefs.getAmplitude(this));
            input_trip_path_cycles.setText(TripDraftPrefs.getCycles(this));
            input_trip_path_wraps.setText(TripDraftPrefs.getWraps(this));
            refreshBookmarkLabels();
        } finally {
            suppressDraftEvents = false;
        }
    }

    private void installDraftListeners() {
        input_trip_origin.addTextChangedListener(new DraftWatcher() {
            public void afterTextChanged(Editable s) { label_trip_origin.setVisibility(View.GONE); draftChanged(); }
        });
        input_trip_waypoints.addTextChangedListener(new DraftWatcher() {
            public void afterTextChanged(Editable s) { draftChanged(); }
        });
        input_trip_destination.addTextChangedListener(new DraftWatcher() {
            public void afterTextChanged(Editable s) { label_trip_destination.setVisibility(View.GONE); draftChanged(); }
        });
        input_trip_duration.addTextChangedListener(new DraftWatcher() { public void afterTextChanged(Editable s) { draftChanged(); } });
        input_trip_path_amplitude.addTextChangedListener(new DraftWatcher() { public void afterTextChanged(Editable s) { draftChanged(); } });
        input_trip_path_cycles.addTextChangedListener(new DraftWatcher() { public void afterTextChanged(Editable s) { draftChanged(); } });
        input_trip_path_wraps.addTextChangedListener(new DraftWatcher() { public void afterTextChanged(Editable s) { draftChanged(); } });
        input_trip_path_type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { draftChanged(); }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void draftChanged() {
        if (suppressDraftEvents) return;
        boolean active = LocationService.isTripModeStarted();
        saveDraftFromUi(active || TripDraftPrefs.isDirty(this));
        updateButtons();
    }

    private void saveDraftFromUi(boolean dirty) {
        if (input_trip_origin == null) return;
        TripDraftPrefs.save(this,
            input_trip_origin.getText().toString(),
            input_trip_waypoints.getText().toString(),
            input_trip_destination.getText().toString(),
            input_trip_duration.getText().toString(),
            input_trip_path_type.getSelectedItemPosition(),
            input_trip_path_amplitude.getText().toString(),
            input_trip_path_cycles.getText().toString(),
            input_trip_path_wraps.getText().toString(),
            dirty
        );
    }

    private void refreshBookmarkLabels() {
        try {
            LocPoint origin = requirePoint(input_trip_origin.getText().toString(), "Origin");
            BookmarkItem item = SharedPrefs.getBookmarkItem(this, origin);
            if (item != null) { label_trip_origin.setText(item.title); label_trip_origin.setVisibility(View.VISIBLE); }
        } catch (Exception ignored) {}
        try {
            LocPoint destination = requirePoint(input_trip_destination.getText().toString(), "Destination");
            BookmarkItem item = SharedPrefs.getBookmarkItem(this, destination);
            if (item != null) { label_trip_destination.setText(item.title); label_trip_destination.setVisibility(View.VISIBLE); }
        } catch (Exception ignored) {}
    }

    private void updateButtons() {
        boolean active = LocationService.isTripModeStarted();
        button_toggle_state.setText(active ? R.string.label_button_stop : R.string.label_button_start);
        button_update.setVisibility(active ? View.VISIBLE : View.GONE);
        button_update.setEnabled(active && TripDraftPrefs.isDirty(this));
    }

    private void requestPermissions(boolean update) {
        pendingUpdate = update;
        boolean granted = RuntimePermissions.hasMandatoryPermissions(this);
        if (TripStartPermissionPolicy.shouldStartImmediately(granted)) { doStart(); return; }
        RuntimePermissionsRequester requester = (RuntimePermissionsRequester) getParent();
        requester.requestTripRuntimePermissions(this);
    }

    @Override
    public void doStart() {
        boolean update = pendingUpdate && LocationService.isTripModeStarted();
        pendingUpdate = false;
        try {
            LocPoint destination = requirePoint(input_trip_destination.getText().toString(), "Destination");
            int duration = requirePositiveInt(input_trip_duration.getText().toString(), "Duration");
            int pathType = TripPathGenerator.sanitizeType(input_trip_path_type.getSelectedItemPosition());
            double amplitude = TripPathGenerator.sanitizeAmplitude(Double.parseDouble(cleanNumber(input_trip_path_amplitude.getText().toString())));
            int cycles = TripPathGenerator.sanitizeCycles(Integer.parseInt(input_trip_path_cycles.getText().toString().trim(), 10));
            int wraps = TripPathGenerator.sanitizeWraps(Integer.parseInt(input_trip_path_wraps.getText().toString().trim(), 10));
            String rawWaypoints = input_trip_waypoints.getText().toString();
            String waypoints = TripWaypointCodec.normalize(rawWaypoints);
            if (!rawWaypoints.trim().isEmpty() && waypoints.isEmpty()) throw new NumberFormatException("No valid waypoint coordinates");

            if (update) {
                if (LocationService.doUpdateTrip(this, true, destination, duration, pathType, amplitude, cycles, wraps, waypoints) == null)
                    throw new IllegalStateException("Trip is no longer active");
                SharedPrefs.putTripDestination(this, destination);
                SharedPrefs.putTripDuration(this, duration);
            } else {
                LocPoint origin = requirePoint(input_trip_origin.getText().toString(), "Origin");
                LocationService.doStart(this, true, origin, destination, duration, pathType, amplitude, cycles, wraps, waypoints);
                SharedPrefs.putTripOrigin(this, origin);
                SharedPrefs.putTripDestination(this, destination);
                SharedPrefs.putTripDuration(this, duration);
            }
            TripPathPrefs.save(this, pathType, amplitude, cycles, wraps, waypoints);
            saveDraftFromUi(false);
            TripDraftPrefs.setDirty(this, false);
            updateButtons();
        } catch (Exception e) {
            Toast.makeText(this, (update ? "Could not update Trip: " : "Could not start Trip: ") + e.getMessage(), Toast.LENGTH_LONG).show();
            updateButtons();
        }
    }

    private static LocPoint requirePoint(String raw, String name) {
        LocPoint point = TripWaypointCodec.first(TripWaypointCodec.cleanupNumericWhitespace(raw));
        if (point == null) throw new NumberFormatException(name + " must be latitude, longitude");
        return point;
    }
    private static int requirePositiveInt(String raw, String name) {
        int value = Integer.parseInt(raw.trim(), 10);
        if (value <= 0) throw new NumberFormatException(name + " must be greater than 0");
        return value;
    }
    private static String cleanNumber(String raw) {
        return TripWaypointCodec.cleanupNumericWhitespace(raw).trim().replace(',', '.');
    }
    private abstract static class DraftWatcher implements TextWatcher {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}

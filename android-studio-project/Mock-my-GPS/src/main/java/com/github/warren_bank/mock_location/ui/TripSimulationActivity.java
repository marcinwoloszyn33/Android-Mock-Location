package com.github.warren_bank.mock_location.ui;

import com.github.warren_bank.mock_location.R;
import com.github.warren_bank.mock_location.data_model.BookmarkItem;
import com.github.warren_bank.mock_location.data_model.LocPoint;
import com.github.warren_bank.mock_location.data_model.SharedPrefs;
import com.github.warren_bank.mock_location.security_model.RuntimePermissions;
import com.github.warren_bank.mock_location.service.LocationService;
import com.github.warren_bank.mock_location.service.trip.TripPathGenerator;
import com.github.warren_bank.mock_location.service.trip.TripPathPrefs;
import com.github.warren_bank.mock_location.ui.logic.TripEditState;
import com.github.warren_bank.mock_location.ui.logic.TripStartPermissionPolicy;
import com.github.warren_bank.mock_location.ui.interfaces.RuntimePermissionsListener;
import com.github.warren_bank.mock_location.ui.interfaces.RuntimePermissionsRequester;

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

public class TripSimulationActivity extends Activity
implements RuntimePermissionsListener {
    private LocPoint originalLocOrigin;
    private LocPoint originalLocDestination;
    private int originalTripDuration;
    private int originalPathType;
    private double originalPathAmplitude;
    private int originalPathCycles;

    private TextView label_trip_origin;
    private TextView input_trip_origin;
    private TextView label_trip_destination;
    private TextView input_trip_destination;
    private TextView input_trip_duration;
    private Spinner input_trip_path_type;
    private TextView input_trip_path_amplitude;
    private TextView input_trip_path_cycles;
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
        label_trip_destination = (TextView) findViewById(R.id.label_trip_destination);
        input_trip_destination = (TextView) findViewById(R.id.input_trip_destination);
        input_trip_duration = (TextView) findViewById(R.id.input_trip_duration);
        input_trip_path_type = (Spinner) findViewById(R.id.input_trip_path_type);
        input_trip_path_amplitude = (TextView) findViewById(R.id.input_trip_path_amplitude);
        input_trip_path_cycles = (TextView) findViewById(R.id.input_trip_path_cycles);
        button_toggle_state = (Button) findViewById(R.id.button_toggle_state);
        button_update = (Button) findViewById(R.id.button_update);

        input_trip_origin.addTextChangedListener(new SimpleWatcher() {
            public void afterTextChanged(Editable s) {
                label_trip_origin.setVisibility(View.GONE);
                if (!LocationService.isTripModeStarted()) return;
                try {
                    LocPoint value = new LocPoint(s.toString());
                    diff_fields = TripEditState.update(
                        diff_fields,
                        TripEditState.ORIGIN_MASK,
                        !originalLocOrigin.equals(value)
                    );
                    checkDiff();
                }
                catch(Exception e) {}
            }
        });

        input_trip_destination.addTextChangedListener(new SimpleWatcher() {
            public void afterTextChanged(Editable s) {
                label_trip_destination.setVisibility(View.GONE);
                if (!LocationService.isTripModeStarted()) return;
                try {
                    LocPoint value = new LocPoint(s.toString());
                    diff_fields = TripEditState.update(
                        diff_fields,
                        TripEditState.DESTINATION_MASK,
                        !originalLocDestination.equals(value)
                    );
                    checkDiff();
                }
                catch(Exception e) {}
            }
        });

        input_trip_duration.addTextChangedListener(new SimpleWatcher() {
            public void afterTextChanged(Editable s) {
                if (!LocationService.isTripModeStarted()) return;
                try {
                    int value = Integer.parseInt(s.toString(), 10);
                    diff_fields = TripEditState.update(
                        diff_fields,
                        TripEditState.DURATION_MASK,
                        originalTripDuration != value
                    );
                    checkDiff();
                }
                catch(Exception e) {}
            }
        });

        input_trip_path_amplitude.addTextChangedListener(new SimpleWatcher() {
            public void afterTextChanged(Editable s) {
                if (!LocationService.isTripModeStarted()) return;
                try {
                    double value = TripPathGenerator.sanitizeAmplitude(
                        Double.parseDouble(s.toString().replace(',', '.'))
                    );
                    diff_fields = TripEditState.update(
                        diff_fields,
                        TripEditState.PATH_AMPLITUDE_MASK,
                        Math.abs(originalPathAmplitude - value) > 1e-9d
                    );
                    checkDiff();
                }
                catch(Exception e) {}
            }
        });

        input_trip_path_cycles.addTextChangedListener(new SimpleWatcher() {
            public void afterTextChanged(Editable s) {
                if (!LocationService.isTripModeStarted()) return;
                try {
                    int value = TripPathGenerator.sanitizeCycles(
                        Integer.parseInt(s.toString(), 10)
                    );
                    diff_fields = TripEditState.update(
                        diff_fields,
                        TripEditState.PATH_CYCLES_MASK,
                        originalPathCycles != value
                    );
                    checkDiff();
                }
                catch(Exception e) {}
            }
        });

        input_trip_path_type.setOnItemSelectedListener(
            new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(
                    AdapterView<?> parent,
                    View view,
                    int position,
                    long id
                ) {
                    if (!LocationService.isTripModeStarted()) return;
                    int value = TripPathGenerator.sanitizeType(position);
                    diff_fields = TripEditState.update(
                        diff_fields,
                        TripEditState.PATH_TYPE_MASK,
                        originalPathType != value
                    );
                    checkDiff();
                }

                public void onNothingSelected(AdapterView<?> parent) {}
            }
        );

        button_toggle_state.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (LocationService.isTripModeStarted()) {
                    LocationService.doStop(
                        TripSimulationActivity.this,
                        true
                    );
                    button_toggle_state.setText(R.string.label_button_start);
                    button_update.setVisibility(View.GONE);
                }
                else {
                    requestPermissions();
                }
            }
        });

        button_update.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (LocationService.isTripModeStarted()) {
                    requestPermissions();
                }
                else {
                    button_update.setVisibility(View.GONE);
                }
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

        if (!LocationService.isTripModeStarted()) {
            originalLocOrigin = origin;
            originalLocDestination = destination;
            originalTripDuration = duration;
            originalPathType = pathType;
            originalPathAmplitude = amplitude;
            originalPathCycles = cycles;
            diff_fields = 0;
        }

        reset(origin, destination, duration, pathType, amplitude, cycles);
    }

    private void loadOriginals() {
        originalLocOrigin = SharedPrefs.getTripOrigin(this);
        originalLocDestination = SharedPrefs.getTripDestination(this);
        originalTripDuration = SharedPrefs.getTripDuration(this);
        originalPathType = TripPathPrefs.getType(this);
        originalPathAmplitude = TripPathPrefs.getAmplitude(this);
        originalPathCycles = TripPathPrefs.getCycles(this);
    }

    private void reset(
        LocPoint origin,
        LocPoint destination,
        int duration,
        int pathType,
        double amplitude,
        int cycles
    ) {
        label_trip_origin.setVisibility(View.GONE);
        label_trip_destination.setVisibility(View.GONE);

        input_trip_origin.setText(origin.toString());
        input_trip_destination.setText(destination.toString());
        input_trip_duration.setText(Integer.toString(duration, 10));
        input_trip_path_type.setSelection(
            TripPathGenerator.sanitizeType(pathType)
        );
        input_trip_path_amplitude.setText(
            Double.toString(
                TripPathGenerator.sanitizeAmplitude(amplitude)
            )
        );
        input_trip_path_cycles.setText(
            Integer.toString(
                TripPathGenerator.sanitizeCycles(cycles),
                10
            )
        );

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
        }
        else {
            button_toggle_state.setText(R.string.label_button_start);
            button_update.setVisibility(View.GONE);
        }
    }

    private void checkDiff() {
        button_update.setVisibility(
            (diff_fields == 0) ? View.GONE : View.VISIBLE
        );
    }

    private void requestPermissions() {
        boolean granted =
            RuntimePermissions.hasMandatoryPermissions(this);

        if (
            TripStartPermissionPolicy.shouldStartImmediately(granted)
        ) {
            doStart();
            return;
        }

        RuntimePermissionsRequester requester =
            (RuntimePermissionsRequester) getParent();
        requester.requestTripRuntimePermissions(this);
    }

    public void doStart() {
        try {
            LocPoint origin = new LocPoint(
                input_trip_origin.getText().toString()
            );
            LocPoint destination = new LocPoint(
                input_trip_destination.getText().toString()
            );
            int duration = Integer.parseInt(
                input_trip_duration.getText().toString(),
                10
            );
            int pathType = TripPathGenerator.sanitizeType(
                input_trip_path_type.getSelectedItemPosition()
            );
            double amplitude =
                TripPathGenerator.sanitizeAmplitude(
                    Double.parseDouble(
                        input_trip_path_amplitude
                            .getText()
                            .toString()
                            .replace(',', '.')
                    )
                );
            int cycles = TripPathGenerator.sanitizeCycles(
                Integer.parseInt(
                    input_trip_path_cycles.getText().toString(),
                    10
                )
            );

            LocationService.doStart(
                this,
                true,
                origin,
                destination,
                duration,
                pathType,
                amplitude,
                cycles
            );

            SharedPrefs.putTripOrigin(this, origin);
            SharedPrefs.putTripDestination(this, destination);
            SharedPrefs.putTripDuration(this, duration);
            TripPathPrefs.save(this, pathType, amplitude, cycles);

            originalLocOrigin = origin;
            originalLocDestination = destination;
            originalTripDuration = duration;
            originalPathType = pathType;
            originalPathAmplitude = amplitude;
            originalPathCycles = cycles;
            diff_fields = 0;

            button_toggle_state.setText(R.string.label_button_stop);
            button_update.setVisibility(View.GONE);
        }
        catch (Exception e) {
            Toast.makeText(
                this,
                "Could not start Trip: " + e.getMessage(),
                Toast.LENGTH_LONG
            ).show();
        }
    }

    private abstract static class SimpleWatcher
    implements TextWatcher {
        public void beforeTextChanged(
            CharSequence s,
            int start,
            int count,
            int after
        ) {}

        public void onTextChanged(
            CharSequence s,
            int start,
            int before,
            int count
        ) {}
    }
}

package com.github.warren_bank.mock_location.ui;

import com.github.warren_bank.mock_location.R;
import com.github.warren_bank.mock_location.service.LocationService;
import com.github.warren_bank.mock_location.service.looper.LocationThreadManager;
import com.github.warren_bank.mock_location.service.looper.MockLocationProviderManager;
import com.github.warren_bank.mock_location.service.looper.ProviderStatus;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class LeakCheckActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leak_check);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        ProviderStatus status = MockLocationProviderManager.getProviderStatus(this);

        boolean sessionActive = LocationService.isStarted();

        setText(R.id.leak_status_service,
            sessionActive ? "Mock session: ACTIVE" : "Mock session: stopped");

        setText(R.id.leak_status_gps,
            "GPS provider: " + coreState(status.gpsActive, sessionActive));

        setText(R.id.leak_status_network,
            "Network provider: " + coreState(status.networkActive, sessionActive));

        setText(R.id.leak_status_android_fused,
            "Android Fused provider: " + state(status.androidFusedActive, status.androidFusedSupported));

        setText(R.id.leak_status_vendor_fused,
            status.vendorFusedName + ": " + state(status.vendorFusedActive, status.vendorFusedSupported));

        LocationThreadManager manager = LocationService.getLocationThreadManager();
        if (manager != null) {
            setText(R.id.leak_status_motion,
                "Follow real movement sensors: "
                    + (manager.hasRequiredMotionSensors() ? "accelerometer + magnetometer available" : "required sensor missing"));
            setText(R.id.leak_status_watchdog,
                "Anti-stop state: " + manager.getWatchdogState().name());
        }
        else {
            setText(R.id.leak_status_motion, "Follow real movement sensors: not initialized");
            setText(R.id.leak_status_watchdog, "Anti-stop state: not initialized");
        }
    }

    private String coreState(boolean active, boolean sessionActive) {
        if (active) return "ACTIVE / mock path enabled";
        if (sessionActive) return "not active / unavailable on this device";
        return "inactive; start a mock session to verify";
    }

    private String state(boolean active, boolean supported) {
        if (active) return "ACTIVE / mock path enabled";
        if (supported) return "available, currently inactive";
        return "not available in this build/device";
    }

    private void setText(int id, String text) {
        TextView view = (TextView) findViewById(id);
        if (view != null) view.setText(text);
    }
}

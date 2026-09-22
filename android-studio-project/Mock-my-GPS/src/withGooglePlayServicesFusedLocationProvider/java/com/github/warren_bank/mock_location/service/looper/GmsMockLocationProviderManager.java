package com.github.warren_bank.mock_location.service.looper;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import android.content.Context;
import android.location.Location;

public class GmsMockLocationProviderManager {

    private static FusedLocationProviderClient client = null;
    private static int advanceTimeMillis = 45000;

    protected static boolean isAvailable(Context context) {
        try {
            return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
                == ConnectionResult.SUCCESS;
        }
        catch (Exception e) {
            return false;
        }
    }

    protected static boolean isActive() {
        return client != null;
    }

    protected static void startMockingLocation(Context context) {
        stopMockingLocation();

        if (!isAvailable(context)) return;

        try {
            client = LocationServices.getFusedLocationProviderClient(context);
            client.setMockMode(true);
        }
        catch (Exception e) {
            stopMockingLocation();
        }
    }

    protected static boolean exec(MockLocationFix fix) {
        if (client == null || fix == null) return false;

        try {
            Location mockLocation = MockLocationProvider.getLocation(fix, advanceTimeMillis);
            client.setMockLocation(mockLocation);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    protected static void stopMockingLocation() {
        if (client != null) {
            try {
                client.setMockMode(false);
            }
            catch (Exception e) {}
            client = null;
        }
    }
}

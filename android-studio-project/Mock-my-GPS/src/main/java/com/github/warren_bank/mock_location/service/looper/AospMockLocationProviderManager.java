package com.github.warren_bank.mock_location.service.looper;

import com.github.warren_bank.mock_location.service.microg_nlp_backend.UnifiedNlpManager;

import android.content.Context;
import android.location.LocationManager;
import android.os.Build;

public class AospMockLocationProviderManager {

    private static MockLocationProvider mockNetwork = null;
    private static MockLocationProvider mockGps = null;
    private static MockLocationProvider mockFused = null;
    private static UnifiedNlpManager nlpManager = null;

    protected static void startMockingLocation(Context context) {
        startMockingLocationNetwork(context);
        startMockingLocationGps(context);

        if (Build.VERSION.SDK_INT >= 31) {
            startMockingLocationFused(context);
        }

        try {
            nlpManager = new UnifiedNlpManager(context);
        }
        catch (Exception e) {
            nlpManager = null;
        }
    }

    private static void startMockingLocationNetwork(Context context) {
        stopMockingLocationNetwork();

        try {
            mockNetwork = new MockLocationProvider(LocationManager.NETWORK_PROVIDER, context);
        }
        catch (SecurityException e) {
            stopMockingLocationNetwork();
        }
    }

    private static void startMockingLocationGps(Context context) {
        stopMockingLocationGps();

        try {
            mockGps = new MockLocationProvider(LocationManager.GPS_PROVIDER, context);
        }
        catch (SecurityException e) {
            stopMockingLocationGps();
        }
    }

    private static void startMockingLocationFused(Context context) {
        stopMockingLocationFused();

        try {
            mockFused = new MockLocationProvider(LocationManager.FUSED_PROVIDER, context);
        }
        catch (SecurityException e) {
            stopMockingLocationFused();
        }
    }

    protected static boolean exec(MockLocationFix fix) {
        if (fix == null) return false;

        boolean success = false;

        if (mockNetwork != null) {
            try {
                mockNetwork.pushLocation(fix);
                success = true;
            }
            catch (Exception e) {}
        }

        if (mockGps != null) {
            try {
                mockGps.pushLocation(fix);
                success = true;
            }
            catch (Exception e) {}
        }

        if (mockFused != null) {
            try {
                mockFused.pushLocation(fix);
                success = true;
            }
            catch (Exception e) {}
        }

        if (nlpManager != null) {
            try {
                nlpManager.update(fix.getLatitude(), fix.getLongitude());
            }
            catch (Exception e) {}
        }

        return success;
    }

    protected static void stopMockingLocation() {
        stopMockingLocationNetwork();
        stopMockingLocationGps();
        stopMockingLocationFused();
        nlpManager = null;
    }

    protected static ProviderStatus getProviderStatus(
        String vendorFusedName,
        boolean vendorFusedSupported,
        boolean vendorFusedActive
    ) {
        return new ProviderStatus(
            mockGps != null,
            mockNetwork != null,
            Build.VERSION.SDK_INT >= 31,
            mockFused != null,
            vendorFusedName,
            vendorFusedSupported,
            vendorFusedActive
        );
    }

    private static void stopMockingLocationNetwork() {
        if (mockNetwork != null) {
            try {
                mockNetwork.shutdown();
            }
            catch (Exception e) {}
            mockNetwork = null;
        }
    }

    private static void stopMockingLocationGps() {
        if (mockGps != null) {
            try {
                mockGps.shutdown();
            }
            catch (Exception e) {}
            mockGps = null;
        }
    }

    private static void stopMockingLocationFused() {
        if (mockFused != null) {
            try {
                mockFused.shutdown();
            }
            catch (Exception e) {}
            mockFused = null;
        }
    }
}

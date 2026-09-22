package com.github.warren_bank.mock_location.service.looper;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;

public class MockLocationProvider {
    String providerName;
    Context ctx;

    public MockLocationProvider(String name, Context ctx) {
        this.providerName = name;
        this.ctx = ctx;

        int powerUsage = 0;
        int accuracy = 5;

        if (Build.VERSION.SDK_INT >= 30) {
            powerUsage = 1;
            accuracy = 2;
        }

        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        startup(lm, powerUsage, accuracy, 3, 0);
    }

    private void startup(LocationManager lm, int powerUsage, int accuracy, int maxRetryCount, int currentRetryCount) {
        if (currentRetryCount < maxRetryCount) {
            try {
                shutdown();
                lm.addTestProvider(providerName, false, false, false, false, false, true, true, powerUsage, accuracy);
                lm.setTestProviderEnabled(providerName, true);
            }
            catch (Exception e) {
                startup(lm, powerUsage, accuracy, maxRetryCount, currentRetryCount + 1);
            }
        }
        else {
            throw new SecurityException("Not allowed to perform MOCK_LOCATION");
        }
    }

    public void pushLocation(double lat, double lon) {
        pushLocation(new MockLocationFix(lat, lon, 0f, 1f, 3f, 3d));
    }

    public void pushLocation(MockLocationFix fix) {
        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        Location mockLocation = getLocation(providerName, fix, 0);
        lm.setTestProviderLocation(providerName, mockLocation);
    }

    protected static Location getLocation(double lat, double lon) {
        return getLocation(new MockLocationFix(lat, lon, 0f, 1f, 3f, 3d), 0);
    }

    protected static Location getLocation(double lat, double lon, int advanceTimeMillis) {
        return getLocation(new MockLocationFix(lat, lon, 0f, 1f, 3f, 3d), advanceTimeMillis);
    }

    protected static Location getLocation(MockLocationFix fix) {
        return getLocation(fix, 0);
    }

    protected static Location getLocation(MockLocationFix fix, int advanceTimeMillis) {
        return getLocation("fused", fix, advanceTimeMillis);
    }

    protected static Location getLocation(String providerName, double lat, double lon) {
        return getLocation(providerName, new MockLocationFix(lat, lon, 0f, 1f, 3f, 3d), 0);
    }

    protected static Location getLocation(String providerName, double lat, double lon, int advanceTimeMillis) {
        return getLocation(
            providerName,
            new MockLocationFix(lat, lon, 0f, 1f, 3f, 3d),
            advanceTimeMillis
        );
    }

    protected static Location getLocation(String providerName, MockLocationFix fix, int advanceTimeMillis) {
        Location mockLocation = new Location(providerName);
        mockLocation.setLatitude(fix.getLatitude());
        mockLocation.setLongitude(fix.getLongitude());
        mockLocation.setAltitude(fix.getAltitudeMeters());
        mockLocation.setTime(System.currentTimeMillis() + advanceTimeMillis);
        mockLocation.setSpeed(fix.getSpeedMps());
        mockLocation.setBearing(fix.getBearingDegrees());
        mockLocation.setAccuracy(fix.getAccuracyMeters());

        if (Build.VERSION.SDK_INT >= 26) {
            mockLocation.setBearingAccuracyDegrees(5f);
            mockLocation.setVerticalAccuracyMeters(8f);
            mockLocation.setSpeedAccuracyMetersPerSecond(0.35f);
        }

        if (Build.VERSION.SDK_INT >= 17) {
            mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }

        return mockLocation;
    }

    public void shutdown() {
        try {
            LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            lm.removeTestProvider(providerName);
        }
        catch (Exception e) {}
    }
}

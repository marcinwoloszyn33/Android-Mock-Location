package com.github.warren_bank.mock_location.service.looper;

public final class MockLocationFix {
    private final double latitude;
    private final double longitude;
    private final float speedMps;
    private final float bearingDegrees;
    private final float accuracyMeters;
    private final double altitudeMeters;

    public MockLocationFix(
        double latitude,
        double longitude,
        float speedMps,
        float bearingDegrees,
        float accuracyMeters,
        double altitudeMeters
    ) {
        if (!isFinite(latitude) || latitude < -90d || latitude > 90d)
            throw new IllegalArgumentException("latitude out of range");
        if (!isFinite(longitude))
            throw new IllegalArgumentException("longitude invalid");

        this.latitude = latitude;
        this.longitude = normalizeLongitude(longitude);
        this.speedMps = sanitizeNonNegative(speedMps, 0f);
        this.bearingDegrees = normalizeBearing(bearingDegrees);
        this.accuracyMeters = Math.max(1f, sanitizeNonNegative(accuracyMeters, 3f));
        this.altitudeMeters = isFinite(altitudeMeters) ? altitudeMeters : 3d;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getSpeedMps() {
        return speedMps;
    }

    public float getBearingDegrees() {
        return bearingDegrees;
    }

    public float getAccuracyMeters() {
        return accuracyMeters;
    }

    public double getAltitudeMeters() {
        return altitudeMeters;
    }

    private static double normalizeLongitude(double longitude) {
        return ((longitude + 540d) % 360d) - 180d;
    }

    private static float normalizeBearing(float bearing) {
        if (Float.isNaN(bearing) || Float.isInfinite(bearing)) return 1f;
        float normalized = bearing % 360f;
        if (normalized < 0f) normalized += 360f;
        return normalized;
    }

    private static float sanitizeNonNegative(float value, float fallback) {
        if (Float.isNaN(value) || Float.isInfinite(value) || value < 0f) return fallback;
        return value;
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}

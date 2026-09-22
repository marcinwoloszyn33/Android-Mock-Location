package com.github.warren_bank.mock_location.service.motion;

import com.github.warren_bank.mock_location.data_model.LocPoint;

public final class GeoMover {
    private static final double EARTH_RADIUS_METERS = 6371008.8d;

    private GeoMover() {}

    public static LocPoint move(LocPoint start, double distanceMeters, float bearingDegrees) {
        if (start == null) return null;

        double lat = start.getLatitude();
        double lon = start.getLongitude();

        if (!isFinite(lat) || !isFinite(lon) || !isFinite(distanceMeters) || !isFinite(bearingDegrees)) {
            return new LocPoint(start);
        }

        if (distanceMeters <= 0d) {
            return new LocPoint(start);
        }

        double normalizedBearing = normalizeBearing(bearingDegrees);

        double lat1 = Math.toRadians(lat);
        double lon1 = Math.toRadians(lon);
        double brng = Math.toRadians(normalizedBearing);
        double angularDistance = distanceMeters / EARTH_RADIUS_METERS;

        double sinLat1 = Math.sin(lat1);
        double cosLat1 = Math.cos(lat1);
        double sinAd = Math.sin(angularDistance);
        double cosAd = Math.cos(angularDistance);

        double lat2 = Math.asin(
            sinLat1 * cosAd +
            cosLat1 * sinAd * Math.cos(brng)
        );

        double lon2 = lon1 + Math.atan2(
            Math.sin(brng) * sinAd * cosLat1,
            cosAd - sinLat1 * Math.sin(lat2)
        );

        double outLat = Math.toDegrees(lat2);
        double outLon = normalizeLongitude(Math.toDegrees(lon2));

        if (!isFinite(outLat) || !isFinite(outLon)) {
            return new LocPoint(start);
        }

        return new LocPoint(outLat, outLon);
    }

    static double normalizeLongitude(double longitude) {
        return ((longitude + 540d) % 360d) - 180d;
    }

    static float normalizeBearing(float bearing) {
        float normalized = bearing % 360f;
        if (normalized < 0f) normalized += 360f;
        return normalized;
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}

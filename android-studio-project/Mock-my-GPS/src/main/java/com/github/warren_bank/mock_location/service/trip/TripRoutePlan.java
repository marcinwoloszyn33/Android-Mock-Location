package com.github.warren_bank.mock_location.service.trip;

import com.github.warren_bank.mock_location.data_model.LocPoint;

import java.util.ArrayList;

public final class TripRoutePlan {
    private static final double EARTH_RADIUS_METERS = 6371000d;
    private static final double EARTH_CIRCUMFERENCE_METERS = 2d * Math.PI * EARTH_RADIUS_METERS;

    private TripRoutePlan() {}

    public static LocPoint getPoint(
        LocPoint origin, LocPoint target, String waypointText,
        int type, double progress, double amplitudeMeters, int cycles, int wraps
    ) {
        if (origin == null || target == null) return null;
        double t = clamp01(progress);
        if (t <= 0d) return new LocPoint(origin);
        if (t >= 1d) return new LocPoint(target);

        // With no waypoints, preserve V12 behavior byte-for-byte at the
        // geometry API boundary, including global-wrap semantics.
        if (TripWaypointCodec.parse(waypointText).isEmpty()) {
            return TripPathGenerator.getPoint(
                origin, target, type, t, amplitudeMeters, cycles, wraps
            );
        }

        ArrayList<LocPoint> route = routePoints(origin, target, waypointText);
        double routeDistance = routeDistance(route);
        int cleanWraps = TripPathGenerator.sanitizeWraps(wraps);
        double wrapDistance = Math.abs((double) cleanWraps) * EARTH_CIRCUMFERENCE_METERS;
        double totalDistance = wrapDistance + routeDistance;
        if (totalDistance < 0.01d) return new LocPoint(origin);

        double travelled = totalDistance * t;
        if (wrapDistance > 0d && travelled < wrapDistance) {
            return TripPathGenerator.getPoint(
                origin, origin, type, travelled / wrapDistance,
                amplitudeMeters, cycles, cleanWraps
            );
        }

        travelled = Math.max(0d, travelled - wrapDistance);
        for (int i = 0; i < route.size() - 1; i++) {
            LocPoint a = route.get(i);
            LocPoint b = route.get(i + 1);
            double segmentDistance = distanceMeters(a, b);
            if (segmentDistance < 0.01d) continue;
            if (travelled <= segmentDistance) {
                double local = Math.max(0d, Math.min(1d, travelled / segmentDistance));
                return TripPathGenerator.getPoint(
                    a, b, type, local, amplitudeMeters, cycles, 0
                );
            }
            travelled -= segmentDistance;
        }
        return new LocPoint(target);
    }

    public static String remainingWaypointsText(
        LocPoint origin, LocPoint target, String waypointText,
        double progress, int wraps
    ) {
        if (origin == null || target == null) return "";
        ArrayList<LocPoint> waypoints = TripWaypointCodec.parse(waypointText);
        if (waypoints.isEmpty()) return "";

        double t = clamp01(progress);
        if (t >= 1d) return "";
        if (t <= 0d) return TripWaypointCodec.encode(waypoints);

        ArrayList<LocPoint> route = routePoints(origin, target, waypointText);
        double routeDistance = routeDistance(route);
        int cleanWraps = TripPathGenerator.sanitizeWraps(wraps);
        double wrapDistance = Math.abs((double) cleanWraps) * EARTH_CIRCUMFERENCE_METERS;
        double totalDistance = wrapDistance + routeDistance;
        if (totalDistance < 0.01d) return "";

        double travelled = totalDistance * t;
        if (travelled < wrapDistance) return TripWaypointCodec.encode(waypoints);
        travelled -= wrapDistance;

        for (int i = 0; i < route.size() - 1; i++) {
            double segmentDistance = distanceMeters(route.get(i), route.get(i + 1));
            if (segmentDistance < 0.01d) continue;
            if (travelled <= segmentDistance) {
                ArrayList<LocPoint> remaining = new ArrayList<LocPoint>();
                int waypointCount = waypoints.size();
                for (int routeIndex = i + 1; routeIndex <= waypointCount; routeIndex++) {
                    remaining.add(route.get(routeIndex));
                }
                return TripWaypointCodec.encode(remaining);
            }
            travelled -= segmentDistance;
        }
        return "";
    }

    public static double distanceMeters(LocPoint a, LocPoint b) {
        if (a == null || b == null) return 0d;
        double lat1 = Math.toRadians(a.getLatitude());
        double lat2 = Math.toRadians(b.getLatitude());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(wrapLongitudeDelta(b.getLongitude() - a.getLongitude()));
        double sinLat = Math.sin(dLat * 0.5d);
        double sinLon = Math.sin(dLon * 0.5d);
        double h = (sinLat * sinLat) + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon;
        h = Math.max(0d, Math.min(1d, h));
        return 2d * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(h));
    }

    private static ArrayList<LocPoint> routePoints(LocPoint origin, LocPoint target, String waypointText) {
        ArrayList<LocPoint> points = new ArrayList<LocPoint>();
        points.add(new LocPoint(origin));
        points.addAll(TripWaypointCodec.parse(waypointText));
        points.add(new LocPoint(target));
        return points;
    }

    private static double routeDistance(ArrayList<LocPoint> route) {
        double total = 0d;
        for (int i = 0; i < route.size() - 1; i++) total += distanceMeters(route.get(i), route.get(i + 1));
        return total;
    }

    private static double wrapLongitudeDelta(double degrees) {
        double value = degrees % 360d;
        if (value > 180d) value -= 360d;
        if (value < -180d) value += 360d;
        return value;
    }

    private static double clamp01(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0d;
        return Math.max(0d, Math.min(1d, value));
    }
}

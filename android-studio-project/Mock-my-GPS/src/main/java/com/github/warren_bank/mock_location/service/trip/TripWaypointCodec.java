package com.github.warren_bank.mock_location.service.trip;

import com.github.warren_bank.mock_location.data_model.LocPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TripWaypointCodec {
    private static final int MAX_POINTS = 100;
    private static final Pattern COORDINATE = Pattern.compile(
        "([-+]?\\d{1,2}(?:\\.\\d+)?)\\s*°?\\s*([NnSs])?\\s*[,;]\\s*" +
        "([-+]?\\d{1,3}(?:\\.\\d+)?)\\s*°?\\s*([EeWw])?"
    );

    private TripWaypointCodec() {}

    public static ArrayList<LocPoint> parse(String text) {
        ArrayList<LocPoint> result = new ArrayList<LocPoint>();
        if (text == null || text.trim().isEmpty()) return result;

        String normalized = cleanupNumericWhitespace(text)
            .replace("%2C", ",")
            .replace("%2c", ",")
            .replace("%20", " ");

        Matcher matcher = COORDINATE.matcher(normalized);
        while (matcher.find() && result.size() < MAX_POINTS) {
            try {
                double lat = Double.parseDouble(matcher.group(1));
                double lon = Double.parseDouble(matcher.group(3));
                String ns = matcher.group(2);
                String ew = matcher.group(4);

                if (ns != null) {
                    lat = "S".equalsIgnoreCase(ns) ? -Math.abs(lat) : Math.abs(lat);
                }
                if (ew != null) {
                    lon = "W".equalsIgnoreCase(ew) ? -Math.abs(lon) : Math.abs(lon);
                }

                if (isValid(lat, lon)) result.add(new LocPoint(lat, lon));
            }
            catch (Exception ignored) {}
        }
        return result;
    }

    public static LocPoint first(String text) {
        ArrayList<LocPoint> points = parse(text);
        return points.isEmpty() ? null : points.get(0);
    }

    public static String normalize(String text) {
        return encode(parse(text));
    }

    public static String encode(List<LocPoint> points) {
        if (points == null || points.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (LocPoint point : points) {
            if (point == null || !isValid(point.getLatitude(), point.getLongitude())) continue;
            if (count >= MAX_POINTS) break;
            if (out.length() > 0) out.append('\n');
            out.append(Double.toString(point.getLatitude()));
            out.append(", ");
            out.append(Double.toString(point.getLongitude()));
            count++;
        }
        return out.toString();
    }

    public static String cleanupNumericWhitespace(String text) {
        if (text == null) return "";
        return text.replaceAll("(?<=\\d)\\.\\s+(?=\\d)", ".");
    }

    private static boolean isValid(double lat, double lon) {
        return !Double.isNaN(lat) && !Double.isInfinite(lat)
            && !Double.isNaN(lon) && !Double.isInfinite(lon)
            && lat >= -90d && lat <= 90d && lon >= -180d && lon <= 180d;
    }
}

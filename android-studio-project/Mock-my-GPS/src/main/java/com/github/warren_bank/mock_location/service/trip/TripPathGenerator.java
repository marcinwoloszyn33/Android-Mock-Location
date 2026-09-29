package com.github.warren_bank.mock_location.service.trip;

import com.github.warren_bank.mock_location.data_model.LocPoint;

public final class TripPathGenerator {
    public static final int TYPE_STRAIGHT = 0;
    public static final int TYPE_ARC_LEFT = 1;
    public static final int TYPE_ARC_RIGHT = 2;
    public static final int TYPE_S_CURVE = 3;
    public static final int TYPE_SINE = 4;
    public static final int TYPE_ZIGZAG = 5;
    public static final int TYPE_SAWTOOTH = 6;
    public static final int TYPE_SERPENTINE = 7;
    public static final int TYPE_SLALOM = 8;
    public static final int TYPE_TRIANGLE = 9;
    public static final int TYPE_SQUARE = 10;
    public static final int TYPE_RECTANGLE = 11;
    public static final int TYPE_DIAMOND = 12;
    public static final int TYPE_PENTAGON = 13;
    public static final int TYPE_HEXAGON = 14;
    public static final int TYPE_OCTAGON = 15;
    public static final int TYPE_CIRCLE_LOOP = 16;
    public static final int TYPE_ELLIPSE_LOOP = 17;
    public static final int TYPE_SPIRAL_OUT = 18;
    public static final int TYPE_SPIRAL_IN = 19;
    public static final int TYPE_SPIRAL_OUT_IN = 20;
    public static final int TYPE_FIGURE_EIGHT = 21;
    public static final int TYPE_INFINITY = 22;
    public static final int TYPE_CLOVER = 23;
    public static final int TYPE_STAR_5 = 24;
    public static final int TYPE_STAR_6 = 25;
    public static final int TYPE_U_TURN = 26;
    public static final int TYPE_HAIRPIN = 27;
    public static final int TYPE_LISSAJOUS = 28;
    public static final int TYPE_SMOOTH_RANDOM = 29;

    public static final int TYPE_COUNT = 30;
    public static final double DEFAULT_AMPLITUDE_METERS = 25d;
    public static final double MAX_AMPLITUDE_METERS = 100000000d;
    public static final int DEFAULT_CYCLES = 3;
    public static final int DEFAULT_WRAPS = 0;
    public static final int MAX_ABS_WRAPS = 20;

    private static final double EARTH_RADIUS_METERS = 6371000d;
    private static final double EARTH_CIRCUMFERENCE_METERS = 2d * Math.PI * EARTH_RADIUS_METERS;

    private TripPathGenerator() {}

    public static int sanitizeType(int type) {
        if (type < 0 || type >= TYPE_COUNT) return TYPE_STRAIGHT;
        return type;
    }

    public static double sanitizeAmplitude(double meters) {
        if (Double.isNaN(meters) || Double.isInfinite(meters) || meters <= 0d)
            return DEFAULT_AMPLITUDE_METERS;
        return Math.max(1d, Math.min(MAX_AMPLITUDE_METERS, meters));
    }

    public static int sanitizeCycles(int cycles) {
        if (cycles <= 0) return DEFAULT_CYCLES;
        return Math.max(1, Math.min(20, cycles));
    }

    public static int sanitizeWraps(int wraps) {
        return Math.max(-MAX_ABS_WRAPS, Math.min(MAX_ABS_WRAPS, wraps));
    }

    public static LocPoint getPoint(
    LocPoint origin, LocPoint target, int type, double progress,
    double amplitudeMeters, int cycles
) {
    return getPoint(origin,target,type,progress,amplitudeMeters,cycles,DEFAULT_WRAPS);
}

public static LocPoint getPoint(
    LocPoint origin, LocPoint target, int type, double progress,
    double amplitudeMeters, int cycles, int wraps
) {
    if (origin == null || target == null) return null;
    double t = clamp01(progress);
    if (t <= 0d) return new LocPoint(origin);
    if (t >= 1d) return new LocPoint(target);

    type = sanitizeType(type);
    double amp = sanitizeAmplitude(amplitudeMeters);
    int cyc = sanitizeCycles(cycles);
    int wr = sanitizeWraps(wraps);

    double baseDistance = greatCircleDistanceMeters(origin, target);
    double bearing = (baseDistance < 0.01d) ? 90d : initialBearingDegrees(origin, target);
    double total = baseDistance + ((double) wr * EARTH_CIRCUMFERENCE_METERS);
    double baseAlong = total * t;
    double[] local = localPoint(type, t, total, amp, cyc);
    double alongDelta = local[0] - baseAlong;
    double sideDelta = local[1];

    LocPoint base = destinationPoint(origin, bearing, baseAlong);
    double course = courseAtDistance(origin, bearing, baseAlong, total);
    double offset = Math.hypot(alongDelta, sideDelta);
    if (offset < 0.000001d) return base;
    double offsetBearing = course + Math.toDegrees(Math.atan2(sideDelta, alongDelta));
    return destinationPoint(base, offsetBearing, offset);
}

private static double greatCircleDistanceMeters(LocPoint a, LocPoint b) {
    double lat1 = Math.toRadians(a.getLatitude());
    double lat2 = Math.toRadians(b.getLatitude());
    double dLat = lat2 - lat1;
    double dLon = Math.toRadians(wrapLongitudeDelta(b.getLongitude() - a.getLongitude()));
    double sl = Math.sin(dLat * 0.5d), so = Math.sin(dLon * 0.5d);
    double h = (sl*sl) + Math.cos(lat1)*Math.cos(lat2)*so*so;
    h = Math.max(0d, Math.min(1d, h));
    return 2d * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(h));
}

private static double initialBearingDegrees(LocPoint from, LocPoint to) {
    double lat1 = Math.toRadians(from.getLatitude());
    double lat2 = Math.toRadians(to.getLatitude());
    double dLon = Math.toRadians(wrapLongitudeDelta(to.getLongitude() - from.getLongitude()));
    double y = Math.sin(dLon) * Math.cos(lat2);
    double x = Math.cos(lat1)*Math.sin(lat2) - Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
    if (Math.abs(x) < 1e-15d && Math.abs(y) < 1e-15d) return 90d;
    return normalizeBearing(Math.toDegrees(Math.atan2(y, x)));
}

private static double courseAtDistance(LocPoint origin, double bearing, double distance, double total) {
    double direction = (total < 0d) ? -1d : 1d;
    if (Math.abs(total) < 0.01d) direction = 1d;
    LocPoint here = destinationPoint(origin, bearing, distance);
    LocPoint ahead = destinationPoint(origin, bearing, distance + direction*1000d);
    return initialBearingDegrees(here, ahead);
}

private static LocPoint destinationPoint(LocPoint start, double bearingDegrees, double distanceMeters) {
    double ad = distanceMeters / EARTH_RADIUS_METERS;
    double br = Math.toRadians(bearingDegrees);
    double lat1 = Math.toRadians(start.getLatitude());
    double lon1 = Math.toRadians(start.getLongitude());
    double sinLat1 = Math.sin(lat1), cosLat1 = Math.cos(lat1);
    double sinD = Math.sin(ad), cosD = Math.cos(ad);
    double sinLat2 = sinLat1*cosD + cosLat1*sinD*Math.cos(br);
    sinLat2 = Math.max(-1d, Math.min(1d, sinLat2));
    double lat2 = Math.asin(sinLat2);
    double y = Math.sin(br)*sinD*cosLat1;
    double x = cosD - sinLat1*Math.sin(lat2);
    double lon2 = lon1 + Math.atan2(y,x);
    return new LocPoint(Math.toDegrees(lat2), normalizeLongitude(Math.toDegrees(lon2)));
}

private static double normalizeBearing(double degrees) {
    double v = degrees % 360d;
    if (v < 0d) v += 360d;
    return v;
}

    private static double[] localPoint(int type, double t, double d, double a, int cycles) {
        double x = d * t;
        double y = 0d;
        double theta = 2d * Math.PI * cycles * t;
        double envelope = Math.sin(Math.PI * t);

        switch (type) {
            case TYPE_ARC_LEFT:
                y = a * Math.sin(Math.PI * t);
                break;
            case TYPE_ARC_RIGHT:
                y = -a * Math.sin(Math.PI * t);
                break;
            case TYPE_S_CURVE:
                y = a * Math.sin(2d * Math.PI * t);
                break;
            case TYPE_SINE:
                y = a * Math.sin(theta);
                break;
            case TYPE_ZIGZAG:
                y = a * envelope * triangleWave(cycles * t);
                break;
            case TYPE_SAWTOOTH:
                y = a * envelope * sawWave(cycles * t);
                break;
            case TYPE_SERPENTINE:
                y = a * (0.65d + 0.35d * envelope) * Math.sin(theta);
                break;
            case TYPE_SLALOM:
                y = a * envelope * Math.tanh(3d * Math.sin(theta));
                break;
            case TYPE_TRIANGLE:
                y = a * (1d - Math.abs((2d * t) - 1d));
                break;
            case TYPE_SQUARE:
                return threeSideBox(t, d, a);
            case TYPE_RECTANGLE:
                return threeSideBox(t, d, a * 0.55d);
            case TYPE_DIAMOND:
                y = a * triangleWave(2d * t) * envelope;
                break;
            case TYPE_PENTAGON:
                return addOffset(x, polygonOffset(t, 5, a, cycles));
            case TYPE_HEXAGON:
                return addOffset(x, polygonOffset(t, 6, a, cycles));
            case TYPE_OCTAGON:
                return addOffset(x, polygonOffset(t, 8, a, cycles));
            case TYPE_CIRCLE_LOOP: {
                double[] o = ellipseLoop(theta, a, a);
                x += o[0];
                y = o[1];
                break;
            }
            case TYPE_ELLIPSE_LOOP: {
                double[] o = ellipseLoop(theta, a * 0.65d, a);
                x += o[0];
                y = o[1];
                break;
            }
            case TYPE_SPIRAL_OUT: {
                double r = a * t;
                x += r * (Math.cos(theta) - 1d);
                y = r * Math.sin(theta);
                break;
            }
            case TYPE_SPIRAL_IN: {
                double r = a * (1d - t);
                x += r * (Math.cos(theta) - 1d);
                y = r * Math.sin(theta);
                break;
            }
            case TYPE_SPIRAL_OUT_IN: {
                double r = a * Math.sin(Math.PI * t);
                x += r * (Math.cos(theta) - 1d);
                y = r * Math.sin(theta);
                break;
            }
            case TYPE_FIGURE_EIGHT:
                x += a * 0.65d * Math.sin(theta);
                y = a * 0.50d * Math.sin(2d * theta);
                break;
            case TYPE_INFINITY:
                x += a * 0.80d * Math.sin(2d * theta);
                y = a * 0.55d * Math.sin(theta);
                break;
            case TYPE_CLOVER: {
                double r = a * Math.sin(2d * theta);
                x += r * Math.cos(theta);
                y = r * Math.sin(theta);
                break;
            }
            case TYPE_STAR_5:
                return addOffset(x, starOffset(t, 5, a, cycles));
            case TYPE_STAR_6:
                return addOffset(x, starOffset(t, 6, a, cycles));
            case TYPE_U_TURN:
                x += a * 0.80d * Math.sin(2d * Math.PI * t);
                y = a * Math.sin(Math.PI * t);
                break;
            case TYPE_HAIRPIN:
                x += a * envelope * Math.sin(4d * Math.PI * t);
                y = a * envelope * Math.tanh(4d * Math.sin(2d * Math.PI * t));
                break;
            case TYPE_LISSAJOUS:
                x += a * 0.55d * envelope
                    * Math.sin(2d * Math.PI * (cycles + 1) * t);
                y = a * envelope
                    * Math.sin(2d * Math.PI * (cycles + 2) * t);
                break;
            case TYPE_SMOOTH_RANDOM:
                x += a * 0.18d * envelope * (
                    Math.sin(6.11d * Math.PI * t)
                    + 0.45d * Math.sin(13.7d * Math.PI * t + 0.8d)
                );
                y = a * envelope * (
                    0.58d * Math.sin(2d * Math.PI * cycles * t + 0.35d)
                    + 0.27d * Math.sin(2d * Math.PI * (cycles + 3) * t + 1.1d)
                    + 0.15d * Math.sin(2d * Math.PI * (cycles + 7) * t + 2.4d)
                );
                break;
            case TYPE_STRAIGHT:
            default:
                break;
        }

        return new double[] {x, y};
    }

    private static double[] threeSideBox(double t, double d, double height) {
        if (t < (1d / 3d)) {
            return new double[] {0d, height * (t * 3d)};
        }
        if (t < (2d / 3d)) {
            return new double[] {
                d * ((t - (1d / 3d)) * 3d),
                height
            };
        }
        return new double[] {
            d,
            height * (1d - ((t - (2d / 3d)) * 3d))
        };
    }

    private static double[] addOffset(double baseX, double[] offset) {
        return new double[] {baseX + offset[0], offset[1]};
    }

    private static double[] ellipseLoop(double theta, double rx, double ry) {
        return new double[] {
            rx * (Math.cos(theta) - 1d),
            ry * Math.sin(theta)
        };
    }

    private static double[] polygonOffset(
        double t,
        int sides,
        double radius,
        int repeats
    ) {
        int n = Math.max(3, sides);
        double phase = fractional(t * repeats);
        double scaled = phase * n;
        int i = (int) Math.floor(scaled);
        double u = scaled - i;

        double a0 = 2d * Math.PI * (i % n) / n;
        double a1 = 2d * Math.PI * ((i + 1) % n) / n;

        double x0 = radius * Math.cos(a0);
        double y0 = radius * Math.sin(a0);
        double x1 = radius * Math.cos(a1);
        double y1 = radius * Math.sin(a1);

        return new double[] {
            lerp(x0, x1, u) - radius,
            lerp(y0, y1, u)
        };
    }

    private static double[] starOffset(
        double t,
        int points,
        double radius,
        int repeats
    ) {
        int count = points * 2;
        double phase = fractional(t * repeats);
        double scaled = phase * count;
        int i = (int) Math.floor(scaled);
        double u = scaled - i;

        double[] p0 = starVertex(i % count, radius, count);
        double[] p1 = starVertex((i + 1) % count, radius, count);
        double[] start = starVertex(0, radius, count);

        return new double[] {
            lerp(p0[0], p1[0], u) - start[0],
            lerp(p0[1], p1[1], u) - start[1]
        };
    }

    private static double[] starVertex(int index, double radius, int count) {
        double r = ((index & 1) == 0) ? radius : radius * 0.42d;
        double angle = 2d * Math.PI * index / count;
        return new double[] {
            r * Math.cos(angle),
            r * Math.sin(angle)
        };
    }

    private static double triangleWave(double value) {
        double f = fractional(value);
        return 1d - (4d * Math.abs(f - 0.5d));
    }

    private static double sawWave(double value) {
        return (2d * fractional(value)) - 1d;
    }

    private static double fractional(double value) {
        return value - Math.floor(value);
    }

    private static double lerp(double a, double b, double t) {
        return a + ((b - a) * t);
    }

    private static double clamp01(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0d;
        return Math.max(0d, Math.min(1d, value));
    }

    private static double wrapLongitudeDelta(double degrees) {
        double value = degrees % 360d;
        if (value > 180d) value -= 360d;
        if (value < -180d) value += 360d;
        return value;
    }

    private static double normalizeLongitude(double longitude) {
        double value = longitude % 360d;
        if (value > 180d) value -= 360d;
        if (value < -180d) value += 360d;
        return value;
    }
}

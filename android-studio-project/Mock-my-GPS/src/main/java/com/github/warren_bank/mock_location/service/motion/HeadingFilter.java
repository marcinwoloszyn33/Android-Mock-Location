package com.github.warren_bank.mock_location.service.motion;

public final class HeadingFilter {
    private final float alpha;
    private boolean initialized;
    private double x;
    private double y;

    public HeadingFilter(float alpha) {
        if (Float.isNaN(alpha) || Float.isInfinite(alpha)) alpha = 0.25f;
        this.alpha = Math.max(0.01f, Math.min(1f, alpha));
    }

    public synchronized float update(float headingDegrees) {
        float heading = normalize(headingDegrees);
        double radians = Math.toRadians(heading);
        double nx = Math.cos(radians);
        double ny = Math.sin(radians);

        if (!initialized) {
            x = nx;
            y = ny;
            initialized = true;
        }
        else {
            x = ((1d - alpha) * x) + (alpha * nx);
            y = ((1d - alpha) * y) + (alpha * ny);

            double length = Math.sqrt((x * x) + (y * y));
            if (length > 1e-9d) {
                x /= length;
                y /= length;
            }
        }

        float out = (float) Math.toDegrees(Math.atan2(y, x));
        return normalize(out);
    }

    public synchronized void reset() {
        initialized = false;
        x = 0d;
        y = 0d;
    }

    static float normalize(float degrees) {
        if (Float.isNaN(degrees) || Float.isInfinite(degrees)) return 0f;
        float normalized = degrees % 360f;
        if (normalized < 0f) normalized += 360f;
        return normalized;
    }
}

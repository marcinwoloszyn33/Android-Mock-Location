package com.github.warren_bank.mock_location.service.motion;

public final class StepDetector {
    public static final float DEFAULT_TRIGGER_THRESHOLD = 1.15f;
    public static final float DEFAULT_RELEASE_THRESHOLD = 0.35f;
    public static final long DEFAULT_MIN_INTERVAL_MS = 280L;

    private final float triggerThreshold;
    private final float releaseThreshold;
    private final long minIntervalMs;

    private boolean armed = true;
    private long lastStepMs = Long.MIN_VALUE;

    public StepDetector() {
        this(DEFAULT_TRIGGER_THRESHOLD, DEFAULT_RELEASE_THRESHOLD, DEFAULT_MIN_INTERVAL_MS);
    }

    public StepDetector(float triggerThreshold, float releaseThreshold, long minIntervalMs) {
        this.triggerThreshold = Math.max(0.1f, triggerThreshold);
        this.releaseThreshold = Math.max(0f, Math.min(this.triggerThreshold, releaseThreshold));
        this.minIntervalMs = Math.max(100L, minIntervalMs);
    }

    public synchronized boolean update(float verticalLinearAcceleration, long timestampMs) {
        float magnitude = Math.abs(verticalLinearAcceleration);

        if (magnitude <= releaseThreshold) {
            armed = true;
            return false;
        }

        if (!armed || magnitude < triggerThreshold) {
            return false;
        }

        armed = false;

        if (lastStepMs != Long.MIN_VALUE && timestampMs - lastStepMs < minIntervalMs) {
            return false;
        }

        lastStepMs = timestampMs;
        return true;
    }

    public synchronized void reset() {
        armed = true;
        lastStepMs = Long.MIN_VALUE;
    }
}

package com.github.warren_bank.mock_location.service.motion;

public final class FallbackCadenceGate {
    public static final long DEFAULT_MIN_INTERVAL_MS = 300L;
    public static final long DEFAULT_MAX_INTERVAL_MS = 1200L;

    private final long minIntervalMs;
    private final long maxIntervalMs;
    private long lastCandidateMs = Long.MIN_VALUE;

    public FallbackCadenceGate() {
        this(DEFAULT_MIN_INTERVAL_MS, DEFAULT_MAX_INTERVAL_MS);
    }

    public FallbackCadenceGate(long minIntervalMs, long maxIntervalMs) {
        this.minIntervalMs = Math.max(100L, minIntervalMs);
        this.maxIntervalMs = Math.max(this.minIntervalMs + 1L, maxIntervalMs);
    }

    public synchronized boolean accept(long timestampMs) {
        if (lastCandidateMs == Long.MIN_VALUE) {
            lastCandidateMs = timestampMs;
            return false;
        }

        long intervalMs = timestampMs - lastCandidateMs;
        lastCandidateMs = timestampMs;

        if (intervalMs < minIntervalMs || intervalMs > maxIntervalMs) {
            return false;
        }

        return true;
    }

    public synchronized void reset() {
        lastCandidateMs = Long.MIN_VALUE;
    }
}

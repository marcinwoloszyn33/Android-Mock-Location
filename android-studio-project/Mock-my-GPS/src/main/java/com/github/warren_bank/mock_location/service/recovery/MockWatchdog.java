package com.github.warren_bank.mock_location.service.recovery;

public final class MockWatchdog {
    public enum State {
        ACTIVE,
        RECOVERING,
        STOPPED
    }

    private boolean enabled;
    private long timeoutMs;
    private long retryIntervalMs;

    private State state = State.STOPPED;
    private long lastSuccessMs = Long.MIN_VALUE;
    private long lastRecoveryAttemptMs = Long.MIN_VALUE;

    public MockWatchdog(boolean enabled, long timeoutMs, long retryIntervalMs) {
        configure(enabled, timeoutMs, retryIntervalMs);
    }

    public synchronized void configure(boolean enabled, long timeoutMs, long retryIntervalMs) {
        this.enabled = enabled;
        this.timeoutMs = Math.max(1000L, timeoutMs);
        this.retryIntervalMs = Math.max(1000L, retryIntervalMs);
    }

    public synchronized void markStarted(long nowMs) {
        state = State.ACTIVE;
        lastSuccessMs = nowMs;
        lastRecoveryAttemptMs = Long.MIN_VALUE;
    }

    public synchronized void markInjectionSuccess(long nowMs) {
        if (state == State.STOPPED) return;
        state = State.ACTIVE;
        lastSuccessMs = nowMs;
    }

    public synchronized void markInjectionFailure(long nowMs) {
        // Failure is intentionally passive. A single provider error must not stop the loop.
    }

    public synchronized boolean shouldRecover(long nowMs) {
        if (!enabled || state == State.STOPPED || lastSuccessMs == Long.MIN_VALUE) return false;
        if (nowMs - lastSuccessMs < timeoutMs) return false;
        if (lastRecoveryAttemptMs != Long.MIN_VALUE && nowMs - lastRecoveryAttemptMs < retryIntervalMs) return false;
        return true;
    }

    public synchronized void markRecoveryStarted(long nowMs) {
        if (state == State.STOPPED) return;
        state = State.RECOVERING;
        lastRecoveryAttemptMs = nowMs;
    }

    public synchronized void markRecoverySucceeded(long nowMs) {
        if (state == State.STOPPED) return;
        state = State.ACTIVE;
        lastSuccessMs = nowMs;
    }

    public synchronized void markStopped() {
        state = State.STOPPED;
    }

    public synchronized State getState() {
        return state;
    }

    public synchronized long getLastSuccessMs() {
        return lastSuccessMs;
    }
}

package com.github.warren_bank.mock_location.service.recovery;
public final class LongTripPolicy {
    public static final long FORCE_PROVIDER_RECOVERY_GAP_MS = 10000L;
    public static final long HEARTBEAT_INTERVAL_MS = 10L * 60L * 1000L;
    private LongTripPolicy() {}
    public static boolean shouldHoldWakeLock(boolean running, boolean tripActive, boolean aggressive) {
        return running && (tripActive || aggressive);
    }
    public static boolean shouldForceProviderRecovery(long gapMs) {
        return gapMs >= FORCE_PROVIDER_RECOVERY_GAP_MS;
    }
}

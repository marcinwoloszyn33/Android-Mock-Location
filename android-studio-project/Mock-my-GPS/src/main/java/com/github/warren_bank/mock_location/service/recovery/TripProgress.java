package com.github.warren_bank.mock_location.service.recovery;

public final class TripProgress {
private TripProgress() {}

public static long remainingMillis(int totalIterations, int completedIterations, int intervalMs) {
int safeIntervalMs = (intervalMs > 0) ? intervalMs : 100;
long remainingIterations = Math.max(0L, (long) totalIterations - (long) completedIterations);
return remainingIterations * (long) safeIntervalMs;
}

public static int secondsForRestore(long remainingMs) {
if (remainingMs <= 0L) return 0;
long seconds = (remainingMs + 999L) / 1000L;
return (seconds > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) seconds;
}
}

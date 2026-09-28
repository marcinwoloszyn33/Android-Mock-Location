package com.github.warren_bank.mock_location.service.recovery;
public final class TripTimeline {
    private TripTimeline() {}
    public static long durationMillis(int seconds) {
        long safe = Math.max(1L, (long) seconds);
        return safe > Long.MAX_VALUE / 1000L ? Long.MAX_VALUE : safe * 1000L;
    }
    public static double progress(long start, long duration, long now) {
        if (duration <= 0L) return 1d;
        if (now <= start) return 0d;
        long elapsed = now - start;
        if (elapsed >= duration) return 1d;
        return Math.max(0d, Math.min(1d, (double) elapsed / (double) duration));
    }
    public static long remainingMillis(long start, long duration, long now) {
        if (duration <= 0L) return 0L;
        if (now <= start) return duration;
        long elapsed = now - start;
        return elapsed >= duration ? 0L : duration - elapsed;
    }
    public static long wallClockEnd(long nowWall, long remaining) {
        if (remaining <= 0L) return nowWall;
        return nowWall > Long.MAX_VALUE - remaining ? Long.MAX_VALUE : nowWall + remaining;
    }
}

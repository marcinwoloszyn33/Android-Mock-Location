package com.github.warren_bank.mock_location.service.recovery;
import org.junit.Test;
import static org.junit.Assert.*;
public class TripTimelineTest {
    @Test public void elapsedTimeDrivesProgress() {
        long start = 1000L;
        long duration = 6L * 60L * 60L * 1000L;
        assertEquals(1d/3d, TripTimeline.progress(start, duration, start + 2L*60L*60L*1000L), 0.000001d);
        assertEquals(5d/6d, TripTimeline.progress(start, duration, start + 5L*60L*60L*1000L), 0.000001d);
    }
    @Test public void remainingIsClamped() {
        assertEquals(2500L, TripTimeline.remainingMillis(1000L, 10000L, 8500L));
        assertEquals(0L, TripTimeline.remainingMillis(1000L, 10000L, 12000L));
    }
}

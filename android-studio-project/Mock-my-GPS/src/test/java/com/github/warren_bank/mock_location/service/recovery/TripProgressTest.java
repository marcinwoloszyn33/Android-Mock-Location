package com.github.warren_bank.mock_location.service.recovery;

import org.junit.Test;
import static org.junit.Assert.*;

public class TripProgressTest {
    @Test
    public void remainingMillisUsesUnfinishedIterations() {
        assertEquals(75000L, TripProgress.remainingMillis(200, 50, 500));
    }

    @Test
    public void restoreSecondsRoundsUpSoTripDoesNotEndEarly() {
        assertEquals(76, TripProgress.secondsForRestore(75001L));
        assertEquals(1, TripProgress.secondsForRestore(1L));
    }

    @Test
    public void completedTripHasNoRemainingTime() {
        assertEquals(0L, TripProgress.remainingMillis(50, 50, 500));
        assertEquals(0, TripProgress.secondsForRestore(0L));
    }
}

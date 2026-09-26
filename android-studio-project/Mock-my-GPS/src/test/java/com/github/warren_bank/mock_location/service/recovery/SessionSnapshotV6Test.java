package com.github.warren_bank.mock_location.service.recovery;

import org.junit.Test;
import static org.junit.Assert.*;

public class SessionSnapshotV6Test {
    @Test
    public void tripMayRememberThatFollowShouldResumeAfterTrip() {
        SessionSnapshot snapshot = new SessionSnapshot(
            true,
            52.2297d,
            21.0122d,
            true,
            0.74d,
            true,
            SessionSnapshot.MODE_TRIP,
            52.2400d,
            21.0300d,
            30000L
        );

        assertEquals(SessionSnapshot.MODE_TRIP, snapshot.mode);
        assertTrue(snapshot.followRealMovement);
        assertTrue(snapshot.hasResumableTrip());
    }
}

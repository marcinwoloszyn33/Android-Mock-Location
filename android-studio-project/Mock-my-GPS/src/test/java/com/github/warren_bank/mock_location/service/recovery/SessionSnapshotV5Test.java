package com.github.warren_bank.mock_location.service.recovery;

import org.junit.Test;
import static org.junit.Assert.*;

public class SessionSnapshotV5Test {
    @Test
    public void resumableTripRoundTripKeepsTargetAndRemainingTime() {
        SessionSnapshot original = new SessionSnapshot(
            true,
            52.2297d,
            21.0122d,
            false,
            0.74d,
            true,
            SessionSnapshot.MODE_TRIP,
            52.2400d,
            21.0300d,
            65432L
        );

        SessionSnapshot restored = SessionSnapshot.fromJson(original.toJson());

        assertTrue(restored.active);
        assertEquals(SessionSnapshot.MODE_TRIP, restored.mode);
        assertTrue(restored.hasResumableTrip());
        assertEquals(52.2400d, restored.tripTargetLatitude, 0d);
        assertEquals(21.0300d, restored.tripTargetLongitude, 0d);
        assertEquals(65432L, restored.tripRemainingMs);
    }

    @Test
    public void oldTripSnapshotWithoutRouteIsNotResumable() {
        SessionSnapshot oldStyle = new SessionSnapshot(
            true, 52d, 21d, false, 0.74d, false, SessionSnapshot.MODE_TRIP
        );
        assertFalse(oldStyle.hasResumableTrip());
    }
}

package com.github.warren_bank.mock_location.service.recovery;

import org.junit.Test;

import static org.junit.Assert.*;

public class SessionSnapshotTest {
    @Test
    public void jsonRoundTripKeepsActiveSession() {
        SessionSnapshot original = new SessionSnapshot(true, 52.2297d, 21.0122d, true, 0.81d, true);
        SessionSnapshot restored = SessionSnapshot.fromJson(original.toJson());

        assertTrue(restored.active);
        assertEquals(52.2297d, restored.latitude, 0d);
        assertEquals(21.0122d, restored.longitude, 0d);
        assertTrue(restored.followRealMovement);
        assertEquals(0.81d, restored.stepLengthMeters, 0d);
        assertTrue(restored.aggressiveKeepAlive);
        assertEquals(SessionSnapshot.MODE_FOLLOW_REAL_MOVEMENT, restored.mode);
    }

    @Test
    public void invalidCoordinatesBecomeInactive() {
        SessionSnapshot snapshot = new SessionSnapshot(true, 200d, 21d, true, 0.74d, true);
        assertFalse(snapshot.active);
        assertFalse(snapshot.followRealMovement);
        assertFalse(snapshot.aggressiveKeepAlive);
    }

    @Test
    public void invalidStepLengthFallsBackToDefault() {
        SessionSnapshot snapshot = new SessionSnapshot(true, 52d, 21d, false, 0d, false);
        assertEquals(SessionSnapshot.DEFAULT_STEP_LENGTH_METERS, snapshot.stepLengthMeters, 0d);
    }

    @Test
    public void tripModeRoundTripsEvenThoughTripRouteIsNotStored() {
        SessionSnapshot original = new SessionSnapshot(
            true, 52d, 21d, false, 0.74d, false, SessionSnapshot.MODE_TRIP
        );
        SessionSnapshot restored = SessionSnapshot.fromJson(original.toJson());
        assertEquals(SessionSnapshot.MODE_TRIP, restored.mode);
    }

    @Test
    public void malformedJsonBecomesInactive() {
        SessionSnapshot snapshot = SessionSnapshot.fromJson("{not json");
        assertFalse(snapshot.active);
    }
}

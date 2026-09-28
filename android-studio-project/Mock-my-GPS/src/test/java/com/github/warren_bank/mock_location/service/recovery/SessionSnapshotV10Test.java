package com.github.warren_bank.mock_location.service.recovery;
import org.junit.Test;
import static org.junit.Assert.*;
public class SessionSnapshotV10Test {
    @Test public void wallClockEndSurvivesSerialization() {
        long now = 1700000000000L;
        long end = now + 3600000L;
        SessionSnapshot s = new SessionSnapshot(true,52.2d,21.0d,false,0.74d,true,SessionSnapshot.MODE_TRIP,52.3d,21.1d,3600000L,end);
        SessionSnapshot r = SessionSnapshot.fromJson(s.toJson());
        assertTrue(r.hasTripRoute());
        assertEquals(end, r.tripEndWallClockMs);
        assertEquals(3600000L, r.remainingTripMs(now));
    }
}

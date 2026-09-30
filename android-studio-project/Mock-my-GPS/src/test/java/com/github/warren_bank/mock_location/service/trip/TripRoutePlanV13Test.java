package com.github.warren_bank.mock_location.service.trip;

import com.github.warren_bank.mock_location.data_model.LocPoint;
import org.junit.Test;
import static org.junit.Assert.*;

public class TripRoutePlanV13Test {
    @Test public void straightTripPassesThroughEquidistantWaypointAtHalfTime() {
        LocPoint point = TripRoutePlan.getPoint(
            new LocPoint(0d,0d), new LocPoint(0d,10d), "0, 5",
            TripPathGenerator.TYPE_STRAIGHT, 0.5d, 25d, 1, 0
        );
        assertEquals(0d, point.getLatitude(), 0.01d);
        assertEquals(5d, point.getLongitude(), 0.05d);
    }
    @Test public void remainingWaypointListDropsPassedWaypoints() {
        String early = TripRoutePlan.remainingWaypointsText(new LocPoint(0d,0d), new LocPoint(0d,10d), "0, 5", 0.25d, 0);
        String late = TripRoutePlan.remainingWaypointsText(new LocPoint(0d,0d), new LocPoint(0d,10d), "0, 5", 0.75d, 0);
        assertTrue(early.contains("5.0"));
        assertEquals("", late);
    }
    @Test public void globalWrapIsAppliedOnceForWholeWaypointTrip() {
        LocPoint point = TripRoutePlan.getPoint(new LocPoint(0d,0d), new LocPoint(0d,10d), "0, 5", TripPathGenerator.TYPE_STRAIGHT, 0.25d, 25d, 1, 1);
        assertNotNull(point);
        assertTrue(point.getLongitude() >= -180d && point.getLongitude() <= 180d);
    }
}

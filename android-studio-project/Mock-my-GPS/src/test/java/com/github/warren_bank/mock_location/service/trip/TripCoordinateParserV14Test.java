package com.github.warren_bank.mock_location.service.trip;

import com.github.warren_bank.mock_location.data_model.LocPoint;
import org.junit.Test;
import java.util.ArrayList;
import static org.junit.Assert.*;

public class TripCoordinateParserV14Test {
    @Test public void parsesNormalSixDecimalCoordinates() {
        LocPoint p = TripWaypointCodec.first("53.932823, 27.648549");
        assertNotNull(p);
        assertEquals(53.932823d, p.getLatitude(), 0.0000001d);
        assertEquals(27.648549d, p.getLongitude(), 0.0000001d);
    }
    @Test public void removesAccidentalSpaceAfterDecimalDot() {
        LocPoint p = TripWaypointCodec.first("53. 932823, 27.648549");
        assertNotNull(p);
        assertEquals(53.932823d, p.getLatitude(), 0.0000001d);
        assertEquals(27.648549d, p.getLongitude(), 0.0000001d);
    }
    @Test public void preservesWaypointOrder() {
        ArrayList<LocPoint> points = TripWaypointCodec.parse("53.932823, 27.648549\n53.895227, 27.549850");
        assertEquals(2, points.size());
        assertEquals(53.932823d, points.get(0).getLatitude(), 0.0000001d);
        assertEquals(53.895227d, points.get(1).getLatitude(), 0.0000001d);
    }
}

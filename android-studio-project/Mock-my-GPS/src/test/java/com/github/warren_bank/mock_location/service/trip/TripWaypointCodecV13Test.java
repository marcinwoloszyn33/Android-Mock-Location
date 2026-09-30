package com.github.warren_bank.mock_location.service.trip;

import com.github.warren_bank.mock_location.data_model.LocPoint;
import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

public class TripWaypointCodecV13Test {
    @Test public void parsesPlainMultilineCoordinates() {
        ArrayList<LocPoint> points = TripWaypointCodec.parse("54.7104, 20.4522\n53.9, 27.559");
        assertEquals(2, points.size());
        assertEquals(54.7104d, points.get(0).getLatitude(), 0d);
        assertEquals(27.559d, points.get(1).getLongitude(), 0d);
    }
    @Test public void parsesCoordinatesFromGoogleMapsStyleUrl() {
        LocPoint point = TripWaypointCodec.first("https://www.google.com/maps/@52.2297,21.0122,10z");
        assertNotNull(point);
        assertEquals(52.2297d, point.getLatitude(), 0d);
        assertEquals(21.0122d, point.getLongitude(), 0d);
    }
    @Test public void rejectsInvalidLatitudeLongitude() {
        assertTrue(TripWaypointCodec.parse("123.0, 999.0").isEmpty());
    }
}

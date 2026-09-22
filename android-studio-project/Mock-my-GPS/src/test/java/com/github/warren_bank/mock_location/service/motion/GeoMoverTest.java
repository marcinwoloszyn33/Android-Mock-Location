package com.github.warren_bank.mock_location.service.motion;

import com.github.warren_bank.mock_location.data_model.LocPoint;

import org.junit.Test;

import static org.junit.Assert.*;

public class GeoMoverTest {
    @Test
    public void movesNorthByAboutOneMeter() {
        LocPoint start = new LocPoint(52.2297, 21.0122);
        LocPoint out = GeoMover.move(start, 1.0d, 0f);

        assertEquals(52.22970899d, out.getLatitude(), 0.000003d);
        assertEquals(21.0122d, out.getLongitude(), 0.000003d);
    }

    @Test
    public void movesEastWithoutMaterialLatitudeChange() {
        LocPoint start = new LocPoint(52.2297, 21.0122);
        LocPoint out = GeoMover.move(start, 1.0d, 90f);

        assertEquals(52.2297d, out.getLatitude(), 0.000003d);
        assertTrue(out.getLongitude() > start.getLongitude());
    }

    @Test
    public void movesSouthAndWestInExpectedDirections() {
        LocPoint start = new LocPoint(52.2297, 21.0122);
        LocPoint south = GeoMover.move(start, 1.0d, 180f);
        LocPoint west = GeoMover.move(start, 1.0d, 270f);

        assertTrue(south.getLatitude() < start.getLatitude());
        assertTrue(west.getLongitude() < start.getLongitude());
    }

    @Test
    public void normalizesLongitudeAcrossDateLine() {
        LocPoint start = new LocPoint(0d, 179.99999d);
        LocPoint out = GeoMover.move(start, 5.0d, 90f);
        assertTrue(out.getLongitude() >= -180d && out.getLongitude() < 180d);
    }

    @Test
    public void negativeDistanceDoesNotMove() {
        LocPoint start = new LocPoint(52.2297, 21.0122);
        LocPoint out = GeoMover.move(start, -1.0d, 90f);

        assertEquals(start.getLatitude(), out.getLatitude(), 0d);
        assertEquals(start.getLongitude(), out.getLongitude(), 0d);
    }
}

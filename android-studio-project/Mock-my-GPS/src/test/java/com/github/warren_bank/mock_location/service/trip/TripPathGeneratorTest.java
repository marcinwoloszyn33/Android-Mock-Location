package com.github.warren_bank.mock_location.service.trip;

import com.github.warren_bank.mock_location.data_model.LocPoint;
import org.junit.Test;
import static org.junit.Assert.*;

public class TripPathGeneratorTest {
    private static final LocPoint A = new LocPoint(52.2297d, 21.0122d);
    private static final LocPoint B = new LocPoint(52.2397d, 21.0322d);

    @Test
    public void libraryContainsAtLeastThirtyShapes() {
        assertTrue(TripPathGenerator.TYPE_COUNT >= 30);
    }

    @Test
    public void everyShapeStartsAndEndsExactlyAtRequestedFakePoints() {
        for (int type = 0; type < TripPathGenerator.TYPE_COUNT; type++) {
            LocPoint start = TripPathGenerator.getPoint(A, B, type, 0d, 25d, 3);
            LocPoint end = TripPathGenerator.getPoint(A, B, type, 1d, 25d, 3);
            assertTrue("start type=" + type, start.equals(A, 1e-9d));
            assertTrue("end type=" + type, end.equals(B, 1e-9d));
        }
    }

    @Test
    public void everyShapeProducesFiniteIntermediateCoordinates() {
        for (int type = 0; type < TripPathGenerator.TYPE_COUNT; type++) {
            for (int i = 1; i < 10; i++) {
                LocPoint p = TripPathGenerator.getPoint(A, B, type, i / 10d, 30d, 4);
                assertFalse(Double.isNaN(p.getLatitude()));
                assertFalse(Double.isNaN(p.getLongitude()));
                assertFalse(Double.isInfinite(p.getLatitude()));
                assertFalse(Double.isInfinite(p.getLongitude()));
                assertTrue(p.getLatitude() >= -90d && p.getLatitude() <= 90d);
                assertTrue(p.getLongitude() >= -180d && p.getLongitude() <= 180d);
            }
        }
    }

    @Test
    public void squareIsNotTheSameAsStraight() {
        LocPoint straight = TripPathGenerator.getPoint(
            A, B, TripPathGenerator.TYPE_STRAIGHT, 0.25d, 40d, 3
        );
        LocPoint square = TripPathGenerator.getPoint(
            A, B, TripPathGenerator.TYPE_SQUARE, 0.25d, 40d, 3
        );
        assertFalse(straight.equals(square, 1e-7d));
    }
}

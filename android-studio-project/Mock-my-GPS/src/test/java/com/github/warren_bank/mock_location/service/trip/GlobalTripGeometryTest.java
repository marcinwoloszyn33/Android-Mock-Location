package com.github.warren_bank.mock_location.service.trip;
import com.github.warren_bank.mock_location.data_model.LocPoint;
import org.junit.Test;
import static org.junit.Assert.*;

public class GlobalTripGeometryTest {
    @Test public void acceptsGlobalSizes() {
        assertEquals(30000d, TripPathGenerator.sanitizeAmplitude(30000d), 0d);
        assertEquals(200000d, TripPathGenerator.sanitizeAmplitude(200000d), 0d);
        assertEquals(40075000d, TripPathGenerator.sanitizeAmplitude(40075000d), 0d);
        assertEquals(100000000d, TripPathGenerator.sanitizeAmplitude(100000000d), 0d);
        assertEquals(100000000d, TripPathGenerator.sanitizeAmplitude(250000000d), 0d);
    }

    @Test public void signedWrapsAreSupported() {
        assertEquals(0, TripPathGenerator.sanitizeWraps(0));
        assertEquals(1, TripPathGenerator.sanitizeWraps(1));
        assertEquals(-1, TripPathGenerator.sanitizeWraps(-1));
        assertEquals(20, TripPathGenerator.sanitizeWraps(999));
        assertEquals(-20, TripPathGenerator.sanitizeWraps(-999));
    }

    @Test public void straightCanCircleWorldWithoutNewShape() {
        LocPoint start = new LocPoint(0d, 0d);
        LocPoint q = TripPathGenerator.getPoint(start,start,TripPathGenerator.TYPE_STRAIGHT,0.25d,25d,1,1);
        LocPoint h = TripPathGenerator.getPoint(start,start,TripPathGenerator.TYPE_STRAIGHT,0.50d,25d,1,1);
        LocPoint w = TripPathGenerator.getPoint(start,start,TripPathGenerator.TYPE_STRAIGHT,0.25d,25d,1,-1);
        LocPoint f = TripPathGenerator.getPoint(start,start,TripPathGenerator.TYPE_STRAIGHT,1d,25d,1,1);
        assertEquals(90d, q.getLongitude(), 0.02d);
        assertTrue(Math.abs(Math.abs(h.getLongitude()) - 180d) < 0.02d);
        assertEquals(-90d, w.getLongitude(), 0.02d);
        assertEquals(0d, f.getLongitude(), 0d);
    }

    @Test public void hugeShapeAlwaysReturnsValidCoordinates() {
        LocPoint a = new LocPoint(52.2297d, 21.0122d);
        LocPoint b = new LocPoint(35.6762d, 139.6503d);
        for (int i=1; i<100; i++) {
            LocPoint p = TripPathGenerator.getPoint(a,b,TripPathGenerator.TYPE_FIGURE_EIGHT,i/100d,20000000d,3,1);
            assertNotNull(p);
            assertTrue(p.getLatitude() >= -90d && p.getLatitude() <= 90d);
            assertTrue(p.getLongitude() >= -180d && p.getLongitude() <= 180d);
        }
    }
}

package com.github.warren_bank.mock_location.service.looper;

import org.junit.Test;

import static org.junit.Assert.*;

public class MockLocationFixTest {
    @Test
    public void normalizesLongitudeAndBearing() {
        MockLocationFix fix = new MockLocationFix(52d, 181d, 1f, 361f, 3f, 10d);
        assertEquals(-179d, fix.getLongitude(), 0d);
        assertEquals(1f, fix.getBearingDegrees(), 0.0001f);
    }

    @Test
    public void sanitizesNegativeMetadata() {
        MockLocationFix fix = new MockLocationFix(52d, 21d, -2f, 0f, -1f, Double.NaN);
        assertEquals(0f, fix.getSpeedMps(), 0d);
        assertEquals(3f, fix.getAccuracyMeters(), 0d);
        assertEquals(3d, fix.getAltitudeMeters(), 0d);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidLatitude() {
        new MockLocationFix(91d, 21d, 0f, 0f, 3f, 3d);
    }
}

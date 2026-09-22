package com.github.warren_bank.mock_location.service.motion;

import org.junit.Test;

import static org.junit.Assert.*;

public class HeadingFilterTest {
    @Test
    public void wrapsAcrossNorthUsingShortArc() {
        HeadingFilter filter = new HeadingFilter(0.25f);
        filter.update(359f);
        float out = filter.update(1f);

        assertTrue(out > 350f || out < 10f);
    }

    @Test
    public void normalizesNegativeAndOver360Values() {
        assertEquals(359f, HeadingFilter.normalize(-1f), 0.0001f);
        assertEquals(1f, HeadingFilter.normalize(361f), 0.0001f);
    }

    @Test
    public void constantHeadingConvergesToSameHeading() {
        HeadingFilter filter = new HeadingFilter(0.25f);
        float out = 0f;
        for (int i = 0; i < 10; i++) out = filter.update(90f);
        assertEquals(90f, out, 0.1f);
    }
}

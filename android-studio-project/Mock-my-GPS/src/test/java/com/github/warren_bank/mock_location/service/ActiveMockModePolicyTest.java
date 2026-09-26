package com.github.warren_bank.mock_location.service;

import org.junit.Test;
import static org.junit.Assert.*;

public class ActiveMockModePolicyTest {
    @Test
    public void fixedIsActiveOnlyOutsideTrip() {
        assertTrue(ActiveMockModePolicy.isFixedActive(true, true, false));
        assertFalse(ActiveMockModePolicy.isFixedActive(true, true, true));
        assertFalse(ActiveMockModePolicy.isFixedActive(false, true, false));
    }

    @Test
    public void tripIsActiveOnlyWhileFlyModeRuns() {
        assertTrue(ActiveMockModePolicy.isTripActive(true, true, true));
        assertFalse(ActiveMockModePolicy.isTripActive(true, true, false));
        assertFalse(ActiveMockModePolicy.isTripActive(false, true, true));
    }
}

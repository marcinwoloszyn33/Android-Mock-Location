package com.github.warren_bank.mock_location.data_model;

import org.junit.Test;

import static org.junit.Assert.*;

public class EnhancedPrefsTest {
    @Test
    public void stepLengthIsClampedToSafeRange() {
        assertEquals(0.20d, EnhancedPrefs.clampStepLength(0.01d), 0d);
        assertEquals(2.00d, EnhancedPrefs.clampStepLength(9d), 0d);
        assertEquals(0.74d, EnhancedPrefs.clampStepLength(0.74d), 0d);
    }

    @Test
    public void invalidStepLengthUsesDefault() {
        assertEquals(EnhancedPrefs.DEFAULT_STEP_LENGTH_METERS, EnhancedPrefs.clampStepLength(0d), 0d);
        assertEquals(EnhancedPrefs.DEFAULT_STEP_LENGTH_METERS, EnhancedPrefs.clampStepLength(Double.NaN), 0d);
    }

    @Test
    public void watchdogTimeoutIsClamped() {
        assertEquals(5, EnhancedPrefs.clampWatchdogTimeout(1));
        assertEquals(120, EnhancedPrefs.clampWatchdogTimeout(999));
        assertEquals(15, EnhancedPrefs.clampWatchdogTimeout(15));
    }
}

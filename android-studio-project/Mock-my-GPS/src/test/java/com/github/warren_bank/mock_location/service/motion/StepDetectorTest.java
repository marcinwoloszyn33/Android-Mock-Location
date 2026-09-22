package com.github.warren_bank.mock_location.service.motion;

import org.junit.Test;

import static org.junit.Assert.*;

public class StepDetectorTest {
    @Test
    public void belowThresholdDoesNotCountStep() {
        StepDetector detector = new StepDetector();
        assertFalse(detector.update(0.5f, 1000L));
    }

    @Test
    public void triggerThenReleaseCountsOneStep() {
        StepDetector detector = new StepDetector();
        assertTrue(detector.update(1.3f, 1000L));
        assertFalse(detector.update(1.4f, 1050L));
        assertFalse(detector.update(0.2f, 1100L));
    }

    @Test
    public void secondPeakInsideDebounceIsIgnored() {
        StepDetector detector = new StepDetector();
        assertTrue(detector.update(1.3f, 1000L));
        detector.update(0.2f, 1050L);
        assertFalse(detector.update(1.3f, 1200L));
    }

    @Test
    public void peakAfterDebounceIsAccepted() {
        StepDetector detector = new StepDetector();
        assertTrue(detector.update(1.3f, 1000L));
        detector.update(0.2f, 1100L);
        assertTrue(detector.update(1.3f, 1300L));
    }
}

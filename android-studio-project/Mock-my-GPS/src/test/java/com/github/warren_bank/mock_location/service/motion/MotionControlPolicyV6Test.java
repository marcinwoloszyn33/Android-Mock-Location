package com.github.warren_bank.mock_location.service.motion;

import org.junit.Test;
import static org.junit.Assert.*;

public class MotionControlPolicyV6Test {
    @Test
    public void trackerStaysRunningDuringTripWhenFollowIsConfigured() {
        assertTrue(MotionControlPolicy.shouldRunMotionTracker(true, true));
    }

    @Test
    public void tripTemporarilyRejectsRealStepWithoutDisablingFollowSetting() {
        assertFalse(MotionControlPolicy.shouldAcceptRealMovementStep(true, true, true));
        assertTrue(MotionControlPolicy.shouldAcceptRealMovementStep(true, true, false));
    }
}

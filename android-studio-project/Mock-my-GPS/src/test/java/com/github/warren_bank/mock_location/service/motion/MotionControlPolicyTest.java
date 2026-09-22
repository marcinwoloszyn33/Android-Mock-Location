package com.github.warren_bank.mock_location.service.motion;

import org.junit.Test;

import static org.junit.Assert.*;

public class MotionControlPolicyTest {
    @Test
    public void hardwareStepDetectorIsPreferredWhenUsable() {
        assertTrue(MotionControlPolicy.shouldUseHardwareStepDetector(true, true));
    }

    @Test
    public void hardwareStepDetectorIsNotUsedWithoutPermission() {
        assertFalse(MotionControlPolicy.shouldUseHardwareStepDetector(true, false));
    }

    @Test
    public void joystickStaysVisibleDuringFollowRealMovement() {
        assertTrue(MotionControlPolicy.shouldShowJoystick(
            true,
            true,
            false,
            true,
            true
        ));
    }

    @Test
    public void joystickIsHiddenInFlyMode() {
        assertFalse(MotionControlPolicy.shouldShowJoystick(
            true,
            true,
            true,
            true,
            true
        ));
    }
}

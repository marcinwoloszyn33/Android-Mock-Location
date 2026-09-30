package com.github.warren_bank.mock_location.service.motion;

import org.junit.Test;
import static org.junit.Assert.*;

public class TripJoystickPolicyV13Test {
    @Test public void joystickCanBeShownDuringTripWhenEnabledAndOverlayGranted() {
        assertTrue(MotionControlPolicy.shouldShowJoystick(true, false, true, true, true));
    }
    @Test public void joystickStillNeedsOverlayPermission() {
        assertFalse(MotionControlPolicy.shouldShowJoystick(true, false, true, true, false));
    }
}

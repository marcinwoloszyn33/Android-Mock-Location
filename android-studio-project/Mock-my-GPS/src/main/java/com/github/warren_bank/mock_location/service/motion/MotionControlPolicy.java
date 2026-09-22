package com.github.warren_bank.mock_location.service.motion;

public final class MotionControlPolicy {
    private MotionControlPolicy() {}

    public static boolean shouldUseHardwareStepDetector(
        boolean sensorAvailable,
        boolean activityRecognitionPermissionGrantedOrNotRequired
    ) {
        return sensorAvailable && activityRecognitionPermissionGrantedOrNotRequired;
    }

    public static boolean shouldShowJoystick(
        boolean started,
        boolean followRealMovementEnabled,
        boolean flyMode,
        boolean joystickEnabled,
        boolean overlayPermissionGranted
    ) {
        return started && !flyMode && joystickEnabled && overlayPermissionGranted;
    }
}

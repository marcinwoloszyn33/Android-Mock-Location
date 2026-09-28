package com.github.warren_bank.mock_location.ui.logic;

public final class TripStartPermissionPolicy {
    private TripStartPermissionPolicy() {}

    public static boolean shouldStartImmediately(boolean mandatoryLocationPermissionsGranted) {
        return mandatoryLocationPermissionsGranted;
    }
}

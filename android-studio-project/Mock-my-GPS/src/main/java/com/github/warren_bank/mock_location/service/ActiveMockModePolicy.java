package com.github.warren_bank.mock_location.service;

public final class ActiveMockModePolicy {
    private ActiveMockModePolicy() {}

    public static boolean isFixedActive(boolean serviceRunning, boolean managerStarted, boolean flyMode) {
        return serviceRunning && managerStarted && !flyMode;
    }

    public static boolean isTripActive(boolean serviceRunning, boolean managerStarted, boolean flyMode) {
        return serviceRunning && managerStarted && flyMode;
    }
}

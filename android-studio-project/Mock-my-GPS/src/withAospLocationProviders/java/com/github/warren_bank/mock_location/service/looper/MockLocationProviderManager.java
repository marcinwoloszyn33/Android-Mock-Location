package com.github.warren_bank.mock_location.service.looper;

import android.content.Context;

public class MockLocationProviderManager {

    protected static void startMockingLocation(Context context) {
        AospMockLocationProviderManager.startMockingLocation(context);
    }

    protected static boolean exec(MockLocationFix fix) {
        return AospMockLocationProviderManager.exec(fix);
    }

    protected static void stopMockingLocation() {
        AospMockLocationProviderManager.stopMockingLocation();
    }

    public static ProviderStatus getProviderStatus(Context context) {
        return AospMockLocationProviderManager.getProviderStatus(
            "Not included in this build",
            false,
            false
        );
    }
}

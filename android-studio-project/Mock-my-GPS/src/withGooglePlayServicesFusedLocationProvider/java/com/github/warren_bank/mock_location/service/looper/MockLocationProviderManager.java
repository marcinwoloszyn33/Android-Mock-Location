package com.github.warren_bank.mock_location.service.looper;

import android.content.Context;

public class MockLocationProviderManager {

    protected static void startMockingLocation(Context context) {
        AospMockLocationProviderManager.startMockingLocation(context);
        GmsMockLocationProviderManager.startMockingLocation(context);
    }

    protected static boolean exec(MockLocationFix fix) {
        boolean aospSuccess = AospMockLocationProviderManager.exec(fix);
        boolean gmsSuccess = GmsMockLocationProviderManager.exec(fix);
        return aospSuccess || gmsSuccess;
    }

    protected static void stopMockingLocation() {
        AospMockLocationProviderManager.stopMockingLocation();
        GmsMockLocationProviderManager.stopMockingLocation();
    }

    public static ProviderStatus getProviderStatus(Context context) {
        return AospMockLocationProviderManager.getProviderStatus(
            "Google Play Services Fused",
            GmsMockLocationProviderManager.isAvailable(context),
            GmsMockLocationProviderManager.isActive()
        );
    }
}

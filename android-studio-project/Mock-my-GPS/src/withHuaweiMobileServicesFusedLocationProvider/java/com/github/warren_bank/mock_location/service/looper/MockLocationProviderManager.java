package com.github.warren_bank.mock_location.service.looper;

import android.content.Context;

public class MockLocationProviderManager {

    protected static void startMockingLocation(Context context) {
        AospMockLocationProviderManager.startMockingLocation(context);
        HmsMockLocationProviderManager.startMockingLocation(context);
    }

    protected static boolean exec(MockLocationFix fix) {
        boolean aospSuccess = AospMockLocationProviderManager.exec(fix);
        boolean hmsSuccess = HmsMockLocationProviderManager.exec(fix);
        return aospSuccess || hmsSuccess;
    }

    protected static void stopMockingLocation() {
        AospMockLocationProviderManager.stopMockingLocation();
        HmsMockLocationProviderManager.stopMockingLocation();
    }

    public static ProviderStatus getProviderStatus(Context context) {
        return AospMockLocationProviderManager.getProviderStatus(
            "Huawei Mobile Services Fused",
            HmsMockLocationProviderManager.isAvailable(context),
            HmsMockLocationProviderManager.isActive()
        );
    }
}

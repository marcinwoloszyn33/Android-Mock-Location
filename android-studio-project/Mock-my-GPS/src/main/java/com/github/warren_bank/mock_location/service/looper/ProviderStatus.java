package com.github.warren_bank.mock_location.service.looper;

public final class ProviderStatus {
    public final boolean gpsActive;
    public final boolean networkActive;
    public final boolean androidFusedSupported;
    public final boolean androidFusedActive;
    public final String vendorFusedName;
    public final boolean vendorFusedSupported;
    public final boolean vendorFusedActive;

    public ProviderStatus(
        boolean gpsActive,
        boolean networkActive,
        boolean androidFusedSupported,
        boolean androidFusedActive,
        String vendorFusedName,
        boolean vendorFusedSupported,
        boolean vendorFusedActive
    ) {
        this.gpsActive = gpsActive;
        this.networkActive = networkActive;
        this.androidFusedSupported = androidFusedSupported;
        this.androidFusedActive = androidFusedActive;
        this.vendorFusedName = (vendorFusedName == null) ? "Not included" : vendorFusedName;
        this.vendorFusedSupported = vendorFusedSupported;
        this.vendorFusedActive = vendorFusedActive;
    }
}

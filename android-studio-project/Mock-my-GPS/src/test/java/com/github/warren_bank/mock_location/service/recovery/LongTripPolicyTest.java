package com.github.warren_bank.mock_location.service.recovery;
import org.junit.Test;
import static org.junit.Assert.*;
public class LongTripPolicyTest {
    @Test public void tripAlwaysHoldsWakeLock() {
        assertTrue(LongTripPolicy.shouldHoldWakeLock(true, true, false));
        assertFalse(LongTripPolicy.shouldHoldWakeLock(true, false, false));
        assertTrue(LongTripPolicy.shouldHoldWakeLock(true, false, true));
    }
    @Test public void longGapForcesProviderRecovery() {
        assertFalse(LongTripPolicy.shouldForceProviderRecovery(9999L));
        assertTrue(LongTripPolicy.shouldForceProviderRecovery(10000L));
    }
}

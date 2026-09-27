package com.github.warren_bank.mock_location.ui.logic;

import com.github.warren_bank.mock_location.ui.interfaces.RuntimePermissionsListener;

import org.junit.Test;

import static org.junit.Assert.*;

public class RuntimeStartRouterTest {
    private static final class Marker implements RuntimePermissionsListener {
        @Override
        public void doStart() {}
    }

    @Test
    public void explicitTripRequesterWinsOverStaleCurrentActivity() {
        Marker trip = new Marker();
        Marker staleFixed = new Marker();

        RuntimePermissionsListener chosen =
            RuntimeStartRouter.choose(trip, staleFixed);

        assertSame(trip, chosen);
    }

    @Test
    public void currentActivityRemainsFallbackForLegacyCallers() {
        Marker current = new Marker();

        RuntimePermissionsListener chosen =
            RuntimeStartRouter.choose(null, current);

        assertSame(current, chosen);
    }
}

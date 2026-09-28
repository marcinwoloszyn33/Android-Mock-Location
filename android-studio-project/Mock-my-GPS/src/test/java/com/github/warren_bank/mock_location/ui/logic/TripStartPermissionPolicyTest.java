package com.github.warren_bank.mock_location.ui.logic;

import org.junit.Test;

import static org.junit.Assert.*;

public class TripStartPermissionPolicyTest {
    @Test
    public void startsImmediatelyWhenMandatoryLocationPermissionsAlreadyExist() {
        assertTrue(TripStartPermissionPolicy.shouldStartImmediately(true));
    }

    @Test
    public void usesDedicatedPermissionFlowWhenMandatoryLocationPermissionsAreMissing() {
        assertFalse(TripStartPermissionPolicy.shouldStartImmediately(false));
    }
}

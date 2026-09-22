package com.github.warren_bank.mock_location.service.motion;

import org.junit.Test;

import static org.junit.Assert.*;

public class FallbackCadenceGateTest {
    @Test
    public void isolatedCandidateIsIgnored() {
        FallbackCadenceGate gate = new FallbackCadenceGate();
        assertFalse(gate.accept(1000L));
    }

    @Test
    public void secondCandidateAtWalkingCadenceIsAccepted() {
        FallbackCadenceGate gate = new FallbackCadenceGate();
        assertFalse(gate.accept(1000L));
        assertTrue(gate.accept(1600L));
    }

    @Test
    public void tooFastMotionIsRejected() {
        FallbackCadenceGate gate = new FallbackCadenceGate();
        assertFalse(gate.accept(1000L));
        assertFalse(gate.accept(1100L));
    }

    @Test
    public void longGapRequiresNewCadencePair() {
        FallbackCadenceGate gate = new FallbackCadenceGate();
        assertFalse(gate.accept(1000L));
        assertFalse(gate.accept(2500L));
        assertTrue(gate.accept(3100L));
    }
}

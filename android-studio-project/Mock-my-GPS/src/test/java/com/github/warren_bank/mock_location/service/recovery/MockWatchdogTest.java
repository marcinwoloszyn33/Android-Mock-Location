package com.github.warren_bank.mock_location.service.recovery;

import org.junit.Test;

import static org.junit.Assert.*;

public class MockWatchdogTest {
    @Test
    public void doesNotTimeoutBeforeWindow() {
        MockWatchdog watchdog = new MockWatchdog(true, 15000L, 5000L);
        watchdog.markStarted(1000L);
        assertFalse(watchdog.shouldRecover(15999L));
    }

    @Test
    public void timesOutAfterWindow() {
        MockWatchdog watchdog = new MockWatchdog(true, 15000L, 5000L);
        watchdog.markStarted(1000L);
        assertTrue(watchdog.shouldRecover(16000L));
    }

    @Test
    public void successResetsTimeoutClock() {
        MockWatchdog watchdog = new MockWatchdog(true, 15000L, 5000L);
        watchdog.markStarted(1000L);
        watchdog.markInjectionSuccess(12000L);
        assertFalse(watchdog.shouldRecover(26000L));
        assertTrue(watchdog.shouldRecover(27000L));
    }

    @Test
    public void stoppedNeverRequestsRecovery() {
        MockWatchdog watchdog = new MockWatchdog(true, 15000L, 5000L);
        watchdog.markStarted(1000L);
        watchdog.markStopped();
        assertFalse(watchdog.shouldRecover(999999L));
        assertEquals(MockWatchdog.State.STOPPED, watchdog.getState());
    }

    @Test
    public void recoveryReturnsToActiveAfterSuccess() {
        MockWatchdog watchdog = new MockWatchdog(true, 15000L, 5000L);
        watchdog.markStarted(1000L);
        watchdog.markRecoveryStarted(16000L);
        assertEquals(MockWatchdog.State.RECOVERING, watchdog.getState());
        watchdog.markRecoverySucceeded(16010L);
        assertEquals(MockWatchdog.State.ACTIVE, watchdog.getState());
        assertFalse(watchdog.shouldRecover(30000L));
    }

    @Test
    public void recoveryIsThrottled() {
        MockWatchdog watchdog = new MockWatchdog(true, 15000L, 5000L);
        watchdog.markStarted(1000L);
        assertTrue(watchdog.shouldRecover(16000L));
        watchdog.markRecoveryStarted(16000L);
        assertFalse(watchdog.shouldRecover(20000L));
        assertTrue(watchdog.shouldRecover(21000L));
    }
}

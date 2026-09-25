package com.github.warren_bank.mock_location.ui.logic;

import org.junit.Test;
import static org.junit.Assert.*;

public class TripEditStateTest {
    @Test
    public void changedFieldSetsItsBit() {
        short diff = TripEditState.update((short) 0, TripEditState.DURATION_MASK, true);
        assertEquals(TripEditState.DURATION_MASK, diff);
    }

    @Test
    public void unchangedFieldClearsItsBit() {
        short diff = TripEditState.update(TripEditState.DURATION_MASK, TripEditState.DURATION_MASK, false);
        assertEquals(0, diff);
    }

    @Test
    public void updatingOneFieldPreservesOtherBits() {
        short diff = TripEditState.update(TripEditState.ORIGIN_MASK, TripEditState.DESTINATION_MASK, true);
        assertEquals((short) (TripEditState.ORIGIN_MASK | TripEditState.DESTINATION_MASK), diff);
    }
}

package com.github.warren_bank.mock_location.ui.logic;

import org.junit.Test;
import static org.junit.Assert.*;

public class BookmarkSelectionPolicyTest {
    @Test
    public void activeFixedSessionUpdatesImmediately() {
        assertTrue(BookmarkSelectionPolicy.shouldApplyFixedImmediately(true));
    }

    @Test
    public void stoppedSessionOnlyStoresBookmarkSelection() {
        assertFalse(BookmarkSelectionPolicy.shouldApplyFixedImmediately(false));
    }
}

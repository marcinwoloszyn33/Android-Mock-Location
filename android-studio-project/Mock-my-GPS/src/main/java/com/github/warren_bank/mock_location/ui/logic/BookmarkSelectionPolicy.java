package com.github.warren_bank.mock_location.ui.logic;

public final class BookmarkSelectionPolicy {
private BookmarkSelectionPolicy() {}

public static boolean shouldApplyFixedImmediately(boolean serviceStarted) {
return serviceStarted;
}
}

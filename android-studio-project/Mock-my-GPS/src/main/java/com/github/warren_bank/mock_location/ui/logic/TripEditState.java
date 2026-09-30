package com.github.warren_bank.mock_location.ui.logic;

public final class TripEditState {
    public static final short ORIGIN_MASK = (1 << 0);
    public static final short DESTINATION_MASK = (1 << 1);
    public static final short DURATION_MASK = (1 << 2);
    public static final short PATH_TYPE_MASK = (1 << 3);
    public static final short PATH_AMPLITUDE_MASK = (1 << 4);
    public static final short PATH_CYCLES_MASK = (1 << 5);
    public static final short PATH_WRAPS_MASK = (1 << 6);
    public static final short WAYPOINTS_MASK = (1 << 7);
    private TripEditState() {}
    public static short update(short diffFields, short mask, boolean changed) {
        return changed ? (short) (diffFields | mask) : (short) (diffFields & ~mask);
    }
}

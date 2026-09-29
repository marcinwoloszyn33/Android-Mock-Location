package com.github.warren_bank.mock_location.ui.logic;
public final class TripEditState {
 public static final short ORIGIN_MASK=(1<<0), DESTINATION_MASK=(1<<1), DURATION_MASK=(1<<2), PATH_TYPE_MASK=(1<<3), PATH_AMPLITUDE_MASK=(1<<4), PATH_CYCLES_MASK=(1<<5), PATH_WRAPS_MASK=(1<<6);
 private TripEditState() {}
 public static short update(short d,short m,boolean c){return c?(short)(d|m):(short)(d&~m);}
}

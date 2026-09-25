package com.github.warren_bank.mock_location.service.recovery;

import com.google.gson.Gson;

public final class SessionSnapshot {
    public static final double DEFAULT_STEP_LENGTH_METERS = 0.74d;

    public static final String MODE_FIXED = "FIXED";
    public static final String MODE_FOLLOW_REAL_MOVEMENT = "FOLLOW_REAL_MOVEMENT";
    public static final String MODE_TRIP = "TRIP";

    public final boolean active;
    public final double latitude;
    public final double longitude;
    public final boolean followRealMovement;
    public final double stepLengthMeters;
    public final boolean aggressiveKeepAlive;
    public final String mode;
    public final double tripTargetLatitude;
    public final double tripTargetLongitude;
    public final long tripRemainingMs;

    public SessionSnapshot(
        boolean active,
        double latitude,
        double longitude,
        boolean followRealMovement,
        double stepLengthMeters,
        boolean aggressiveKeepAlive
    ) {
        this(
            active,
            latitude,
            longitude,
            followRealMovement,
            stepLengthMeters,
            aggressiveKeepAlive,
            followRealMovement ? MODE_FOLLOW_REAL_MOVEMENT : MODE_FIXED
        );
    }

    public SessionSnapshot(
boolean active,
double latitude,
double longitude,
boolean followRealMovement,
double stepLengthMeters,
boolean aggressiveKeepAlive,
String mode
) {
this(
  active,
  latitude,
  longitude,
  followRealMovement,
  stepLengthMeters,
  aggressiveKeepAlive,
  mode,
  0d,
  0d,
  0L
);
}

public SessionSnapshot(
boolean active,
double latitude,
double longitude,
boolean followRealMovement,
double stepLengthMeters,
boolean aggressiveKeepAlive,
String mode,
double tripTargetLatitude,
double tripTargetLongitude,
long tripRemainingMs
) {
boolean validCoordinates = isValidCoordinates(latitude, longitude);

this.active = active && validCoordinates;
this.latitude = validCoordinates ? latitude : 0d;
this.longitude = validCoordinates ? longitude : 0d;
this.followRealMovement = this.active && followRealMovement;
this.stepLengthMeters = sanitizeStepLength(stepLengthMeters);
this.aggressiveKeepAlive = this.active && aggressiveKeepAlive;
this.mode = sanitizeMode(this.active, this.followRealMovement, mode);

boolean validTripRoute =
     this.active
  && MODE_TRIP.equals(this.mode)
  && isValidCoordinates(tripTargetLatitude, tripTargetLongitude)
  && tripRemainingMs > 0L;

this.tripTargetLatitude = validTripRoute ? tripTargetLatitude : 0d;
this.tripTargetLongitude = validTripRoute ? tripTargetLongitude : 0d;
this.tripRemainingMs = validTripRoute ? tripRemainingMs : 0L;
}

public boolean hasResumableTrip() {
return active
  && MODE_TRIP.equals(mode)
  && tripRemainingMs > 0L
  && isValidCoordinates(tripTargetLatitude, tripTargetLongitude);
}

    public static SessionSnapshot inactive() {
        return new SessionSnapshot(
            false,
            0d,
            0d,
            false,
            DEFAULT_STEP_LENGTH_METERS,
            false,
            MODE_FIXED
        );
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static SessionSnapshot fromJson(String json) {
        if (json == null || json.trim().isEmpty()) return inactive();

        try {
            RawSnapshot raw = new Gson().fromJson(json, RawSnapshot.class);
            if (raw == null) return inactive();

            return new SessionSnapshot(
                raw.active,
                raw.latitude,
                raw.longitude,
                raw.followRealMovement,
                raw.stepLengthMeters,
                raw.aggressiveKeepAlive,
                raw.mode,
                raw.tripTargetLatitude,
                raw.tripTargetLongitude,
                raw.tripRemainingMs
            );
        }
        catch (Exception e) {
            return inactive();
        }
    }

    private static final class RawSnapshot {
        boolean active;
        double latitude;
        double longitude;
        boolean followRealMovement;
        double stepLengthMeters;
        boolean aggressiveKeepAlive;
        String mode;
        double tripTargetLatitude;
        double tripTargetLongitude;
        long tripRemainingMs;
    }

    private static String sanitizeMode(boolean active, boolean followRealMovement, String mode) {
        if (!active) return MODE_FIXED;
        if (followRealMovement) return MODE_FOLLOW_REAL_MOVEMENT;
        if (MODE_TRIP.equals(mode)) return MODE_TRIP;
        return MODE_FIXED;
    }

    private static double sanitizeStepLength(double value) {
        if (!isFinite(value) || value <= 0d) return DEFAULT_STEP_LENGTH_METERS;
        return Math.max(0.20d, Math.min(2.00d, value));
    }

    private static boolean isValidCoordinates(double latitude, double longitude) {
        return isFinite(latitude)
            && isFinite(longitude)
            && latitude >= -90d
            && latitude <= 90d
            && longitude >= -180d
            && longitude <= 180d;
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}

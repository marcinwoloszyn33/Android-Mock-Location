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
    public final long tripEndWallClockMs;
    public final String tripWaypoints;

    public SessionSnapshot(boolean active,double latitude,double longitude,boolean follow,double step,boolean keepAlive) {
        this(active,latitude,longitude,follow,step,keepAlive,follow ? MODE_FOLLOW_REAL_MOVEMENT : MODE_FIXED);
    }
    public SessionSnapshot(boolean active,double latitude,double longitude,boolean follow,double step,boolean keepAlive,String mode) {
        this(active,latitude,longitude,follow,step,keepAlive,mode,0d,0d,0L,0L,"");
    }
    public SessionSnapshot(boolean active,double latitude,double longitude,boolean follow,double step,boolean keepAlive,String mode,double targetLat,double targetLon,long remaining) {
        this(active,latitude,longitude,follow,step,keepAlive,mode,targetLat,targetLon,remaining,0L,"");
    }
    public SessionSnapshot(boolean active,double latitude,double longitude,boolean follow,double step,boolean keepAlive,String mode,double targetLat,double targetLon,long remaining,long endWall) {
        this(active,latitude,longitude,follow,step,keepAlive,mode,targetLat,targetLon,remaining,endWall,"");
    }
    public SessionSnapshot(boolean active,double latitude,double longitude,boolean follow,double step,boolean keepAlive,String mode,double targetLat,double targetLon,long remaining,long endWall,String waypoints) {
        boolean valid = valid(latitude, longitude);
        this.active = active && valid;
        this.latitude = valid ? latitude : 0d;
        this.longitude = valid ? longitude : 0d;
        this.followRealMovement = this.active && follow;
        this.stepLengthMeters = sanitizeStep(step);
        this.aggressiveKeepAlive = this.active && keepAlive;
        this.mode = sanitizeMode(this.active, this.followRealMovement, mode);
        boolean trip = this.active && MODE_TRIP.equals(this.mode) && valid(targetLat,targetLon) && (remaining > 0L || endWall > 0L);
        this.tripTargetLatitude = trip ? targetLat : 0d;
        this.tripTargetLongitude = trip ? targetLon : 0d;
        this.tripRemainingMs = trip ? Math.max(0L, remaining) : 0L;
        this.tripEndWallClockMs = trip ? Math.max(0L, endWall) : 0L;
        this.tripWaypoints = trip && waypoints != null ? waypoints.trim() : "";
    }
    public boolean hasTripRoute() {
        return active && MODE_TRIP.equals(mode) && valid(tripTargetLatitude,tripTargetLongitude) && (tripRemainingMs > 0L || tripEndWallClockMs > 0L);
    }
    public long remainingTripMs(long nowWall) {
        if (!hasTripRoute()) return 0L;
        return tripEndWallClockMs > 0L ? Math.max(0L, tripEndWallClockMs - nowWall) : Math.max(0L, tripRemainingMs);
    }
    public boolean hasResumableTrip() { return hasTripRoute() && remainingTripMs(System.currentTimeMillis()) > 0L; }
    public static SessionSnapshot inactive() { return new SessionSnapshot(false,0d,0d,false,DEFAULT_STEP_LENGTH_METERS,false,MODE_FIXED); }
    public String toJson() { return new Gson().toJson(this); }
    public static SessionSnapshot fromJson(String json) {
        if (json == null || json.trim().isEmpty()) return inactive();
        try {
            RawSnapshot r = new Gson().fromJson(json, RawSnapshot.class);
            if (r == null) return inactive();
            return new SessionSnapshot(r.active,r.latitude,r.longitude,r.followRealMovement,r.stepLengthMeters,r.aggressiveKeepAlive,r.mode,r.tripTargetLatitude,r.tripTargetLongitude,r.tripRemainingMs,r.tripEndWallClockMs,r.tripWaypoints);
        } catch (Exception e) { return inactive(); }
    }
    private static final class RawSnapshot {
        boolean active; double latitude; double longitude; boolean followRealMovement; double stepLengthMeters; boolean aggressiveKeepAlive; String mode; double tripTargetLatitude; double tripTargetLongitude; long tripRemainingMs; long tripEndWallClockMs; String tripWaypoints;
    }
    private static String sanitizeMode(boolean active, boolean follow, String mode) {
        if (!active) return MODE_FIXED;
        if (MODE_TRIP.equals(mode)) return MODE_TRIP;
        return follow ? MODE_FOLLOW_REAL_MOVEMENT : MODE_FIXED;
    }
    private static double sanitizeStep(double v) {
        if (!finite(v) || v <= 0d) return DEFAULT_STEP_LENGTH_METERS;
        return Math.max(0.20d, Math.min(2.00d, v));
    }
    private static boolean valid(double lat, double lon) { return finite(lat) && finite(lon) && lat >= -90d && lat <= 90d && lon >= -180d && lon <= 180d; }
    private static boolean finite(double v) { return !Double.isNaN(v) && !Double.isInfinite(v); }
}

package com.github.warren_bank.mock_location.service;

import com.github.warren_bank.mock_location.R;
import com.github.warren_bank.mock_location.data_model.EnhancedPrefs;
import com.github.warren_bank.mock_location.data_model.LocPoint;
import com.github.warren_bank.mock_location.service.looper.LocationThreadManager;
import com.github.warren_bank.mock_location.service.recovery.MockSessionState;
import com.github.warren_bank.mock_location.service.recovery.SessionSnapshot;
import com.github.warren_bank.mock_location.service.recovery.TripProgress;
import com.github.warren_bank.mock_location.service.recovery.LongTripPolicy;
import com.github.warren_bank.mock_location.service.trip.TripPathGenerator;
import com.github.warren_bank.mock_location.service.trip.TripPathPrefs;
import com.github.warren_bank.mock_location.ui.MainActivity;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.widget.RemoteViews;

public class LocationService extends Service {
    private final static int NOTIFICATION_ID          = 1;
    private final static String ACTION_START          = "START";
    private final static String ACTION_STOP           = "STOP";
    private final static String ACTION_PREFS          = "SHARED_PREFS_CHANGE";
    private final static String ACTION_HEARTBEAT      = "LONG_TRIP_HEARTBEAT";
    private final static int HEARTBEAT_REQUEST_CODE   = 7042;
    private final static String EXTRA_ORIGIN_LAT      = "ORIGIN_LAT";
    private final static String EXTRA_ORIGIN_LON      = "ORIGIN_LON";
    private final static String EXTRA_DESTINATION_LAT = "DESTINATION_LAT";
    private final static String EXTRA_DESTINATION_LON = "DESTINATION_LON";
    private final static String EXTRA_TRIP_DURATION   = "TRIP_DURATION";
    private final static String EXTRA_TRIP_PATH_TYPE = "TRIP_PATH_TYPE";
    private final static String EXTRA_TRIP_PATH_AMPLITUDE = "TRIP_PATH_AMPLITUDE";
    private final static String EXTRA_TRIP_PATH_CYCLES = "TRIP_PATH_CYCLES";
    private final static String EXTRA_TRIP_PATH_WRAPS = "TRIP_PATH_WRAPS";
    private final static String EXTRA_TRIP_WAYPOINTS = "TRIP_WAYPOINTS";

    private static boolean running = false;
    private static LocationThreadManager LTM = null;

    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();

        LTM = LocationThreadManager.get();
        LTM.init(LocationService.this);

        showNotification();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        processIntent(intent);
        return START_STICKY;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        processIntent(intent);
    }

    @Override
    public void onDestroy() {
        running = false;
        if (LTM != null && LTM.isStarted()) {
            LTM.stop();
        }
        releaseWakeLock();
        hideNotification();
        super.onDestroy();
    }

    private String getNotificationChannelId() {
        return getPackageName();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            String channelId = getNotificationChannelId();
            NotificationManager NM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel NC = new NotificationChannel(
                channelId,
                channelId,
                NotificationManager.IMPORTANCE_HIGH
            );

            NC.setDescription(channelId);
            NC.setSound(null, null);
            NM.createNotificationChannel(NC);
        }
    }

    private void showNotification() {
        Notification notification = getNotification();

        if (Build.VERSION.SDK_INT >= 5) {
            createNotificationChannel();

            if (Build.VERSION.SDK_INT >= 29)
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            else
                startForeground(NOTIFICATION_ID, notification);
        }
        else {
            NotificationManager NM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NM.notify(NOTIFICATION_ID, notification);
        }
    }

    private void hideNotification() {
        if (Build.VERSION.SDK_INT >= 5) {
            stopForeground(true);
        }
        else {
            NotificationManager NM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NM.cancel(NOTIFICATION_ID);
        }
    }

    private Notification getNotification() {
        Notification notification;

        if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder builder = new Notification.Builder(
                LocationService.this,
                getNotificationChannelId()
            );

            if (Build.VERSION.SDK_INT >= 31) {
                builder.setContentTitle(getString(R.string.notification_service_content_line1));
                builder.setContentText(getString(R.string.notification_service_content_line2));
                builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
            }

            notification = builder.build();
        }
        else {
            notification = new Notification();
        }

        notification.when          = System.currentTimeMillis();
        notification.flags         = 0;
        notification.flags        |= Notification.FLAG_ONGOING_EVENT;
        notification.flags        |= Notification.FLAG_NO_CLEAR;
        notification.icon          = R.drawable.launcher;
        notification.tickerText    = getString(R.string.notification_service_ticker);
        notification.contentIntent = getPendingIntent_MainActivity();

        if (Build.VERSION.SDK_INT >= 16) {
            notification.priority = Notification.PRIORITY_HIGH;
        }
        else {
            notification.flags |= Notification.FLAG_HIGH_PRIORITY;
        }

        if (Build.VERSION.SDK_INT >= 21) {
            notification.visibility = Notification.VISIBILITY_PUBLIC;
        }

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.service_notification);
        contentView.setImageViewResource(R.id.notification_icon, R.drawable.launcher);
        contentView.setTextViewText(
            R.id.notification_text_line1,
            getString(R.string.notification_service_content_line1)
        );
        contentView.setTextViewText(
            R.id.notification_text_line2,
            getString(R.string.notification_service_content_line2)
        );

        if (Build.VERSION.SDK_INT < 31)
            notification.contentView = contentView;
        if (Build.VERSION.SDK_INT >= 16)
            notification.bigContentView = contentView;
        if (Build.VERSION.SDK_INT >= 21)
            notification.headsUpContentView = contentView;

        return notification;
    }

    private PendingIntent getPendingIntent_MainActivity() {
        Intent intent = new Intent(LocationService.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        String current_tab_tag = (!LTM.isFlyMode())
            ? getString(R.string.MainActivity_tab_1_tag)
            : getString(R.string.MainActivity_tab_2_tag);

        intent.putExtra(getString(R.string.MainActivity_extra_current_tab_tag), current_tab_tag);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23)
            flags |= PendingIntent.FLAG_IMMUTABLE;

        return PendingIntent.getActivity(LocationService.this, 0, intent, flags);
    }

    private PendingIntent getPendingIntent_StopService() {
        Intent intent = doStop(LocationService.this, false);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23)
            flags |= PendingIntent.FLAG_IMMUTABLE;

        return PendingIntent.getService(LocationService.this, 0, intent, flags);
    }

    private void processIntent(Intent intent) {
        if (intent == null) {
            restoreActiveSessionIfPresent();
            return;
        }

        String action = intent.getAction();
        if (action == null)
            return;

        switch (action) {
            case ACTION_START: {
                LocPoint origin = processIntentExtras(intent);
                if (origin != null) {
                    running = true;
                    LTM.start(origin);
                    refreshWakeLock();
                    if (LTM.isFlyMode()) scheduleTripHeartbeat(); else cancelTripHeartbeat();
                }
                break;
            }

            case ACTION_STOP: {
                running = false;
                cancelTripHeartbeat();
                MockSessionState.clear(LocationService.this);
                releaseWakeLock();
                LTM.stop();
                stopSelf();
                break;
            }

            case ACTION_PREFS: {
                LTM.onSharedPrefsChange((short) 0);
                refreshWakeLock();
                if (LTM.isFlyMode()) scheduleTripHeartbeat(); else cancelTripHeartbeat();
                break;
            }

            case ACTION_HEARTBEAT: {
                if (!running || LTM == null || !LTM.isStarted()) restoreActiveSessionIfPresent();
                if (running && LTM != null && LTM.isFlyMode()) {
                    LTM.ensureTripLoopAwake();
                    refreshWakeLock();
                    scheduleTripHeartbeat();
                } else {
                    cancelTripHeartbeat();
                }
                break;
            }
        }
    }

    private void restoreActiveSessionIfPresent() {
    if (running || (LTM != null && LTM.isStarted())) return;
    SessionSnapshot snapshot = MockSessionState.load(LocationService.this);
    if (snapshot == null || !snapshot.active) { cancelTripHeartbeat(); stopSelf(); return; }
    EnhancedPrefs.restoreSessionValues(LocationService.this,snapshot.followRealMovement,snapshot.stepLengthMeters,snapshot.aggressiveKeepAlive);
    LTM.onSharedPrefsChange((short) 0);
    running = true;
    LTM.start(new LocPoint(snapshot.latitude, snapshot.longitude));
    if (snapshot.hasTripRoute()) {
        long remaining = snapshot.remainingTripMs(System.currentTimeMillis());
        LocPoint target = new LocPoint(snapshot.tripTargetLatitude, snapshot.tripTargetLongitude);
        if (remaining > 0L) {
            int seconds = TripProgress.secondsForRestore(remaining);
            LTM.flyToLocation(
                target,
                seconds,
                TripPathPrefs.getType(LocationService.this),
                TripPathPrefs.getAmplitude(LocationService.this),
                TripPathPrefs.getCycles(LocationService.this),
                TripPathPrefs.getWraps(LocationService.this),
                snapshot.tripWaypoints
            );
        } else {
            LTM.jumpToLocation(target);
        }
    }
    refreshWakeLock();
    if (LTM.isFlyMode()) scheduleTripHeartbeat(); else cancelTripHeartbeat();
}

    private LocPoint processIntentExtras(Intent intent) {
        double origin_lat      = intent.getDoubleExtra(EXTRA_ORIGIN_LAT,      2000.0);
        double origin_lon      = intent.getDoubleExtra(EXTRA_ORIGIN_LON,      2000.0);
        double destination_lat = intent.getDoubleExtra(EXTRA_DESTINATION_LAT, 2000.0);
        double destination_lon = intent.getDoubleExtra(EXTRA_DESTINATION_LON, 2000.0);
        int    trip_duration   = intent.getIntExtra(EXTRA_TRIP_DURATION,      0);
        int trip_path_type = intent.getIntExtra(
            EXTRA_TRIP_PATH_TYPE,
            TripPathGenerator.TYPE_STRAIGHT
        );
        double trip_path_amplitude = intent.getDoubleExtra(
            EXTRA_TRIP_PATH_AMPLITUDE,
            TripPathGenerator.DEFAULT_AMPLITUDE_METERS
        );
        int trip_path_cycles = intent.getIntExtra(
            EXTRA_TRIP_PATH_CYCLES,
            TripPathGenerator.DEFAULT_CYCLES
        );
        int trip_path_wraps = intent.getIntExtra(EXTRA_TRIP_PATH_WRAPS,TripPathGenerator.DEFAULT_WRAPS);
        String trip_waypoints = intent.getStringExtra(EXTRA_TRIP_WAYPOINTS);
        if (trip_waypoints == null) trip_waypoints = "";

        if ((origin_lat > 1000) || (origin_lon > 1000))
            return null;

        LocPoint origin = new LocPoint(origin_lat, origin_lon);
        LTM.jumpToLocation(origin);

        if ((destination_lat > 1000) || (destination_lon > 1000) || (trip_duration <= 0))
            return origin;

        LocPoint destination = new LocPoint(destination_lat, destination_lon);
        LTM.flyToLocation(
            destination,
            trip_duration,
            trip_path_type,
            trip_path_amplitude,
            trip_path_cycles,
            trip_path_wraps,
            trip_waypoints
        );
        return origin;
    }

    private void refreshWakeLock() {
    boolean tripActive = running && LTM != null && LTM.isFlyMode();
    boolean hold = LongTripPolicy.shouldHoldWakeLock(running,tripActive,EnhancedPrefs.getAggressiveKeepAlive(LocationService.this));
    if (hold) acquireWakeLock(); else releaseWakeLock();
}

private PendingIntent getPendingIntent_TripHeartbeat() {
    Intent intent = new Intent(LocationService.this, LocationService.class);
    intent.setAction(ACTION_HEARTBEAT);
    int flags = PendingIntent.FLAG_UPDATE_CURRENT;
    if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
    if (Build.VERSION.SDK_INT >= 26) return PendingIntent.getForegroundService(LocationService.this,HEARTBEAT_REQUEST_CODE,intent,flags);
    return PendingIntent.getService(LocationService.this,HEARTBEAT_REQUEST_CODE,intent,flags);
}

private void scheduleTripHeartbeat() {
    try {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = getPendingIntent_TripHeartbeat();
        long at = SystemClock.elapsedRealtime() + LongTripPolicy.HEARTBEAT_INTERVAL_MS;
        am.cancel(pi);
        if (Build.VERSION.SDK_INT >= 23) am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,at,pi);
        else am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,at,pi);
    } catch (Exception e) {}
}

private void cancelTripHeartbeat() {
    try {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am != null) am.cancel(getPendingIntent_TripHeartbeat());
    } catch (Exception e) {}
}

    private void acquireWakeLock() {
        if (wakeLock != null && wakeLock.isHeld())
            return;

        try {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager == null) return;

            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                getPackageName() + ":mock_location"
            );
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire();
        }
        catch (Exception e) {
            wakeLock = null;
        }
    }

    private void releaseWakeLock() {
        if (wakeLock == null)
            return;

        try {
            if (wakeLock.isHeld())
                wakeLock.release();
        }
        catch (Exception e) {}

        wakeLock = null;
    }

    public static Intent doStart(
    Context context,
    boolean broadcast,
    LocPoint origin,
    LocPoint destination,
    int trip_duration
) {
    return doStart(
        context,
        broadcast,
        origin,
        destination,
        trip_duration,
        TripPathGenerator.TYPE_STRAIGHT,
        TripPathGenerator.DEFAULT_AMPLITUDE_METERS,
        TripPathGenerator.DEFAULT_CYCLES,
        TripPathGenerator.DEFAULT_WRAPS,
        ""
    );
}

public static Intent doStart(
    Context context,
    boolean broadcast,
    LocPoint origin,
    LocPoint destination,
    int trip_duration,
    int trip_path_type,
    double trip_path_amplitude,
    int trip_path_cycles,
    int trip_path_wraps
) {
    return doStart(
        context,
        broadcast,
        origin,
        destination,
        trip_duration,
        trip_path_type,
        trip_path_amplitude,
        trip_path_cycles,
        trip_path_wraps,
        ""
    );
}

public static Intent doStart(
    Context context,
    boolean broadcast,
    LocPoint origin,
    LocPoint destination,
    int trip_duration,
    int trip_path_type,
    double trip_path_amplitude,
    int trip_path_cycles,
    int trip_path_wraps,
    String trip_waypoints
) {
    if (origin == null)
        return null;

    Intent intent = new Intent(context, LocationService.class);
    addIntentExtras(
        intent,
        origin,
        destination,
        trip_duration,
        trip_path_type,
        trip_path_amplitude,
        trip_path_cycles,
        trip_path_wraps,
        trip_waypoints
    );
    return doAction(context, intent, ACTION_START, broadcast);
}

private static void addIntentExtras(
    Intent intent,
    LocPoint origin,
    LocPoint destination,
    int trip_duration,
    int trip_path_type,
    double trip_path_amplitude,
    int trip_path_cycles,
    int trip_path_wraps,
    String trip_waypoints
) {
    boolean is_trip = (destination != null) && (trip_duration > 0);

    intent.putExtra(EXTRA_ORIGIN_LAT, origin.getLatitude());
    intent.putExtra(EXTRA_ORIGIN_LON, origin.getLongitude());

    if (is_trip) {
        intent.putExtra(EXTRA_DESTINATION_LAT, destination.getLatitude());
        intent.putExtra(EXTRA_DESTINATION_LON, destination.getLongitude());
        intent.putExtra(EXTRA_TRIP_DURATION, trip_duration);
        intent.putExtra(
            EXTRA_TRIP_PATH_TYPE,
            TripPathGenerator.sanitizeType(trip_path_type)
        );
        intent.putExtra(
            EXTRA_TRIP_PATH_AMPLITUDE,
            TripPathGenerator.sanitizeAmplitude(trip_path_amplitude)
        );
        intent.putExtra(
            EXTRA_TRIP_PATH_CYCLES,
            TripPathGenerator.sanitizeCycles(trip_path_cycles)
        );
        intent.putExtra(EXTRA_TRIP_PATH_WRAPS,TripPathGenerator.sanitizeWraps(trip_path_wraps));
        intent.putExtra(EXTRA_TRIP_WAYPOINTS, trip_waypoints == null ? "" : trip_waypoints);
    }
}

    public static Intent doStop(Context context, boolean broadcast) {
        if (!running) return null;

        Intent intent = new Intent(context, LocationService.class);
        return doAction(context, intent, ACTION_STOP, broadcast);
    }

    public static Intent doSharedPrefsChange(Context context, boolean broadcast) {
        if (!running) return null;

        Intent intent = new Intent(context, LocationService.class);
        return doAction(context, intent, ACTION_PREFS, broadcast);
    }

    private static Intent doAction(Context context, Intent intent, String action, boolean broadcast) {
        intent.setAction(action);
        if (broadcast) {
            if (Build.VERSION.SDK_INT >= 26 && ACTION_START.equals(action)) context.startForegroundService(intent);
            else context.startService(intent);
        }
        return intent;
    }

    public static boolean isStarted() {
        return running;
    }

    public static boolean isFixedModeStarted() {
        boolean managerStarted = LTM != null && LTM.isStarted();
        boolean flyMode = managerStarted && LTM.isFlyMode();
        return ActiveMockModePolicy.isFixedActive(running, managerStarted, flyMode);
    }

    public static boolean isTripModeStarted() {
        boolean managerStarted = LTM != null && LTM.isStarted();
        boolean flyMode = managerStarted && LTM.isFlyMode();
        return ActiveMockModePolicy.isTripActive(running, managerStarted, flyMode);
    }

    public static LocationThreadManager getLocationThreadManager() {
        return LTM;
    }
}

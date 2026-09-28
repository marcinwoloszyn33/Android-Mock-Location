package com.github.warren_bank.mock_location.service.looper;
import com.github.warren_bank.mock_location.service.recovery.LongTripPolicy;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.SystemClock;
public class LocationThread extends HandlerThread {
    private Context mContext;
    private LocationThreadManager mLocationThreadManager;
    private int mTimeInterval;
    private Handler mHandler;
    private volatile long mLastRunElapsedMs = 0L;
    public LocationThread(Context context, LocationThreadManager manager, int interval) {
        super("LocationThread", Process.THREAD_PRIORITY_MORE_FAVORABLE);
        mContext = context; mLocationThreadManager = manager; mTimeInterval = Math.max(50, interval);
    }
    @Override public synchronized void start() { super.start(); mHandler = new Handler(getLooper()); mHandler.post(mUpdateLocation); }
    public void startThread() { MockLocationProviderManager.startMockingLocation(mContext); start(); }
    public void stopThread() {
        MockLocationProviderManager.stopMockingLocation();
        if (mHandler != null) mHandler.removeCallbacksAndMessages(null);
        try { quit(); interrupt(); } catch (Exception e) {}
        mLocationThreadManager = null;
    }
    public void updateTimeInterval(int interval) { mTimeInterval = Math.max(50, interval); }
    public void kickNow() {
        final Handler h = mHandler; if (h == null) return;
        h.post(new Runnable() { @Override public void run() { Handler current = mHandler; if (current == null) return; current.removeCallbacks(mUpdateLocation); mUpdateLocation.run(); }});
    }
    private final Runnable mUpdateLocation = new Runnable() {
        @Override public void run() {
            LocationThreadManager manager = mLocationThreadManager;
            long now = SystemClock.elapsedRealtime();
            long gap = (mLastRunElapsedMs > 0L && now >= mLastRunElapsedMs) ? now - mLastRunElapsedMs : 0L;
            mLastRunElapsedMs = now;
            try {
                if (manager != null) {
                    MockLocationFix fix = manager.getUpdateFix();
                    if (LongTripPolicy.shouldForceProviderRecovery(gap)) {
                        recoverProviders(manager, now, fix);
                    } else {
                        boolean success = fix != null && MockLocationProviderManager.exec(fix);
                        manager.onInjectionResult(success, now);
                        if (manager.shouldRecover(now)) recoverProviders(manager, now, manager.getLastFixForRecovery());
                    }
                }
            } catch (Exception e) {
                manager = mLocationThreadManager;
                if (manager != null) manager.onInjectionResult(false, SystemClock.elapsedRealtime());
            } finally {
                manager = mLocationThreadManager;
                if (manager != null && manager.shouldContinue() && mHandler != null) mHandler.postDelayed(this, mTimeInterval);
            }
        }
    };
    private void recoverProviders(LocationThreadManager manager, long now, MockLocationFix fix) {
        manager.markRecoveryStarted(now);
        boolean success = false;
        try {
            MockLocationProviderManager.stopMockingLocation();
            MockLocationProviderManager.startMockingLocation(mContext);
            success = fix != null && MockLocationProviderManager.exec(fix);
        } catch (Exception e) { success = false; }
        long done = SystemClock.elapsedRealtime();
        manager.onInjectionResult(success, done);
        if (success) manager.markRecoverySucceeded(done);
    }
    public Handler getHandler() { return mHandler; }
}

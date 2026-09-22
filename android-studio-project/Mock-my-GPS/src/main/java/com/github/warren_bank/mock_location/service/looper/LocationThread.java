package com.github.warren_bank.mock_location.service.looper;

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

    public LocationThread(Context context, LocationThreadManager locationThreadManager, int timeInterval) {
        super("LocationThread", Process.THREAD_PRIORITY_MORE_FAVORABLE);

        mContext = context;
        mLocationThreadManager = locationThreadManager;
        mTimeInterval = Math.max(50, timeInterval);
    }

    @Override
    public synchronized void start() {
        super.start();

        mHandler = new Handler(getLooper());
        mHandler.post(mUpdateLocation);
    }

    public void startThread() {
        MockLocationProviderManager.startMockingLocation(mContext);
        start();
    }

    public void stopThread() {
        MockLocationProviderManager.stopMockingLocation();

        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }

        try {
            quit();
            interrupt();
        }
        catch (Exception e) {}

        mLocationThreadManager = null;
    }

    public void updateTimeInterval(int timeInterval) {
        mTimeInterval = Math.max(50, timeInterval);
    }

    private final Runnable mUpdateLocation = new Runnable() {
        @Override
        public void run() {
            LocationThreadManager manager = mLocationThreadManager;
            long nowMs = SystemClock.elapsedRealtime();

            try {
                if (manager != null) {
                    MockLocationFix fix = manager.getUpdateFix();
                    boolean success = false;

                    if (fix != null) {
                        success = MockLocationProviderManager.exec(fix);
                    }

                    manager.onInjectionResult(success, nowMs);

                    if (manager.shouldRecover(nowMs)) {
                        recoverProviders(manager, nowMs);
                    }
                }
            }
            catch (Exception e) {
                manager = mLocationThreadManager;
                if (manager != null) {
                    manager.onInjectionResult(false, SystemClock.elapsedRealtime());
                }
            }
            finally {
                manager = mLocationThreadManager;
                if (manager != null && manager.shouldContinue() && mHandler != null) {
                    mHandler.postDelayed(this, mTimeInterval);
                }
            }
        }
    };

    private void recoverProviders(LocationThreadManager manager, long nowMs) {
        manager.markRecoveryStarted(nowMs);

        boolean success = false;
        try {
            MockLocationProviderManager.stopMockingLocation();
            MockLocationProviderManager.startMockingLocation(mContext);

            MockLocationFix lastFix = manager.getLastFixForRecovery();
            success = (lastFix != null) && MockLocationProviderManager.exec(lastFix);
        }
        catch (Exception e) {
            success = false;
        }

        long finishedMs = SystemClock.elapsedRealtime();
        manager.onInjectionResult(success, finishedMs);

        if (success) {
            manager.markRecoverySucceeded(finishedMs);
        }
    }

    public Handler getHandler() {
        return mHandler;
    }
}

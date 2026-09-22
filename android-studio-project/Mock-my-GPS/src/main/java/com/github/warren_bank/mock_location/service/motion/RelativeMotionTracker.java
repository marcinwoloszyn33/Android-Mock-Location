package com.github.warren_bank.mock_location.service.motion;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

public final class RelativeMotionTracker implements SensorEventListener {
    public interface Listener {
        void onStep(float bearingDegrees, long timestampMs);
    }

    private static final float GRAVITY_ALPHA = 0.80f;
    private static final float MIN_MAGNETIC_FIELD_UT = 15f;
    private static final float MAX_MAGNETIC_FIELD_UT = 100f;

    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final Sensor magnetometer;
    private final Listener listener;
    private final StepDetector stepDetector = new StepDetector();
    private final HeadingFilter headingFilter = new HeadingFilter(0.25f);

    private final float[] gravity = new float[3];
    private final float[] magnetic = new float[3];
    private final float[] rotation = new float[9];
    private final float[] orientation = new float[3];

    private boolean haveGravity;
    private boolean haveMagnetic;
    private boolean haveHeading;
    private boolean running;
    private float filteredHeading;

    public RelativeMotionTracker(Context context, Listener listener) {
        sensorManager = (SensorManager) context.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = (sensorManager != null) ? sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
        magnetometer = (sensorManager != null) ? sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) : null;
        this.listener = listener;
    }

    public synchronized boolean start() {
        if (running) return true;
        if (sensorManager == null || accelerometer == null || magnetometer == null) return false;

        resetState();

        boolean accelOk = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        boolean magneticOk = sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);

        running = accelOk && magneticOk;
        if (!running) sensorManager.unregisterListener(this);
        return running;
    }

    public synchronized void stop() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        running = false;
        resetState();
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public boolean hasRequiredSensors() {
        return sensorManager != null && accelerometer != null && magnetometer != null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null) return;

        int type = event.sensor.getType();

        if (type == Sensor.TYPE_MAGNETIC_FIELD) {
            float magnitude = vectorMagnitude(event.values);
            if (magnitude >= MIN_MAGNETIC_FIELD_UT && magnitude <= MAX_MAGNETIC_FIELD_UT) {
                copy3(event.values, magnetic);
                haveMagnetic = true;
                updateHeading();
            }
            return;
        }

        if (type != Sensor.TYPE_ACCELEROMETER) return;

        if (!haveGravity) {
            copy3(event.values, gravity);
            haveGravity = true;
        }
        else {
            for (int i = 0; i < 3; i++) {
                gravity[i] = (GRAVITY_ALPHA * gravity[i]) + ((1f - GRAVITY_ALPHA) * event.values[i]);
            }
        }

        updateHeading();

        float gravityMagnitude = vectorMagnitude(gravity);
        if (gravityMagnitude < 1f) return;

        float lx = event.values[0] - gravity[0];
        float ly = event.values[1] - gravity[1];
        float lz = event.values[2] - gravity[2];

        float vertical = (
            (lx * gravity[0]) +
            (ly * gravity[1]) +
            (lz * gravity[2])
        ) / gravityMagnitude;

        if (haveHeading && stepDetector.update(vertical, SystemClock.elapsedRealtime())) {
            Listener currentListener = listener;
            if (currentListener != null) {
                currentListener.onStep(filteredHeading, SystemClock.elapsedRealtime());
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void updateHeading() {
        if (!haveGravity || !haveMagnetic) return;

        if (SensorManager.getRotationMatrix(rotation, null, gravity, magnetic)) {
            SensorManager.getOrientation(rotation, orientation);
            float azimuth = (float) Math.toDegrees(orientation[0]);
            filteredHeading = headingFilter.update(azimuth);
            haveHeading = true;
        }
    }

    private void resetState() {
        haveGravity = false;
        haveMagnetic = false;
        haveHeading = false;
        filteredHeading = 0f;
        stepDetector.reset();
        headingFilter.reset();
    }

    private static void copy3(float[] from, float[] to) {
        if (from == null || from.length < 3) return;
        to[0] = from[0];
        to[1] = from[1];
        to[2] = from[2];
    }

    private static float vectorMagnitude(float[] values) {
        if (values == null || values.length < 3) return 0f;
        return (float) Math.sqrt(
            (values[0] * values[0]) +
            (values[1] * values[1]) +
            (values[2] * values[2])
        );
    }
}

package com.pixelnetica.easyscan.camera;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by Denis on 09.02.2015.
 */
public class MotionDetector implements SensorEventListener {

    public interface MotionDetectorCallbacks {
        public void onMotionDetect(MotionDetector source, double motionModule);
    }

    // Sensor data
    private SensorManager mgr;
    private Sensor accelerometer;
    private float [] gravity;
    private float [] motion;

    // Callbacks
    MotionDetectorCallbacks mCallbacks;

    public MotionDetector(Context context) {
        mgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    /**
     * start watching
     */
    public void start() {
        if (accelerometer != null) {
            mgr.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    /**
     * stop watching
     */
    public void release() {
        if (accelerometer != null) {
            mgr.unregisterListener(this, accelerometer);
        }

        // reset watching
        gravity = null;
        motion = null;
    }

    void setCallbacks(MotionDetectorCallbacks callback) {
        this.mCallbacks = callback;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Nothing to do
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Initialization
        if (gravity == null) {
            gravity = new float[3];
            motion = new float[3];
            for (int i = 0; i < 3; i++) {
                gravity[i] = event.values[i];
                motion[i] = 0;
            }
            return;
        }

        // Normal way
        double motionModule = 0.0;
        for (int i = 0; i < 3; i++) {
            gravity[i] = (float) (0.1 * event.values[i] + 0.9 * gravity[i]);
            motion[i] = event.values[i] - gravity[i];

            motionModule += motion[i]*motion[i];
        }
        motionModule = Math.sqrt(motionModule);

        // Notify owner
        if (mCallbacks != null) {
            mCallbacks.onMotionDetect(this, motionModule);
        }
    }
}

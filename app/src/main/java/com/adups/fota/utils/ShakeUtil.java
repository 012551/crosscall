package com.adups.fota.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.adups.fota.MyApplication;
import com.adups.fota.callback.ShakeCallback;

public class ShakeUtil implements SensorEventListener2 {

    private static final int SPEED_SHRES_HOLD = 4500;//这个值越大需要越大的力气来摇晃手机
    private static final int UPDATE_INTERVAL_TIME = 100;
    private static ShakeUtil shakeUtil;
    private ShakeCallback callback;
    private float lastX, lastY, lastZ;
    private long lastUpdateTime;

    public static ShakeUtil getInstance() {
        if (shakeUtil == null)
            shakeUtil = new ShakeUtil();
        return shakeUtil;
    }

    public boolean hasAccelerometer(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            return sensor != null;
        }
        return false;
    }

    public void setOnShakingListener(ShakeCallback callback) {
        SensorManager sensorManager = (SensorManager) MyApplication.getAppContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (sensor != null)
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
        }
        this.callback = callback;
    }

    public void removeShakingListener() {
        SensorManager sensorManager = (SensorManager) MyApplication.getAppContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null)
            sensorManager.unregisterListener(this);
        this.callback = null;
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) MyApplication.getAppContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
                VibrationEffect effect = VibrationEffect.createOneShot(500, 100);
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(500);
            }
        }
    }

    @Override
    public void onFlushCompleted(Sensor sensor) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long currentUpdateTime = System.currentTimeMillis();
        long timeInterval = currentUpdateTime - lastUpdateTime;
        if (timeInterval < UPDATE_INTERVAL_TIME)
            return;
        lastUpdateTime = currentUpdateTime;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float deltaX = x - lastX;
        float deltaY = y - lastY;
        float deltaZ = z - lastZ;

        lastX = x;
        lastY = y;
        lastZ = z;

        double speed = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) / timeInterval * 10000;
        if (speed >= SPEED_SHRES_HOLD && callback != null) {
            callback.onShaking();
            vibrate();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}

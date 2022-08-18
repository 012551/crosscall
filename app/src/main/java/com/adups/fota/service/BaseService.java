package com.adups.fota.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;


import androidx.annotation.Nullable;

import com.adups.fota.R;
import com.adups.fota.utils.LogUtil;

public class BaseService extends IntentService {

    public static final int INNER_SERVICE_ID = 1;

    private String name;

    public BaseService(String name) {
        super(name);
        this.name = name;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(INNER_SERVICE_ID, getNotification());
        LogUtil.d(name);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.d(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }

    private Notification getNotification() {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setTicker("")
                .setContentTitle("")
                .setSmallIcon(R.mipmap.ic_launcher);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel = new NotificationChannel(com.adups.fota.manager.NotificationManager.NOTIFICATION_CHANNEL,
                    getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
            notificationChannel.enableVibration(false);
            notificationChannel.enableLights(false);
            if (notificationManager != null)
                notificationManager.createNotificationChannel(notificationChannel);
            builder.setChannelId(com.adups.fota.manager.NotificationManager.NOTIFICATION_CHANNEL);
        }
        return builder.build();
    }

}

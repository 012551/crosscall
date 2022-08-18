package com.adups.fota.manager;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

import com.adups.fota.config.Const;
import com.adups.fota.config.Setting;
import com.adups.fota.config.TaskID;
import com.adups.fota.receiver.MyReceiver;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PreferencesUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

public class AlarmManager {

    private static void alarm(Context context, int taskId, long time) {
        Intent alarm = new Intent(context, MyReceiver.class);
        alarm.setAction(Const.SEND_CUSTOM_SERVICE_ACTION);
        alarm.putExtra(TaskID.TASK, taskId);
        PendingIntent operation = PendingIntent.getBroadcast(context, taskId, alarm, PendingIntent.FLAG_UPDATE_CURRENT);
        android.app.AlarmManager manager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) {
            LogUtil.d("alarm time = " + SimpleDateFormat.getDateTimeInstance().format(time)
                    + " , requestCode = " + taskId);
            int sdkInt = Build.VERSION.SDK_INT;
            if (sdkInt < Build.VERSION_CODES.KITKAT) {
                manager.set(android.app.AlarmManager.RTC_WAKEUP, time, operation);
            } else if (sdkInt < Build.VERSION_CODES.M) {
                manager.setExact(android.app.AlarmManager.RTC_WAKEUP, time, operation);
            } else {
                manager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, time, operation);
            }
        }
    }

    public static void cancel(Context context){
        Intent alarm = new Intent(context, MyReceiver.class);
        alarm.setAction(Const.SEND_CUSTOM_SERVICE_ACTION);
        alarm.putExtra(TaskID.TASK, TaskID.TASK_DOWNLOAD_NIGHT);
        PendingIntent operation = PendingIntent.getBroadcast(context, TaskID.TASK_DOWNLOAD_NIGHT, alarm, PendingIntent.FLAG_UPDATE_CURRENT);
        android.app.AlarmManager manager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(operation);
    }
    public static void queryActivate(Context context) {
        long time = System.currentTimeMillis() + DeviceInfoUtil.getInstance().getQueryActivate() - SystemClock.elapsedRealtime();
        alarm(context, TaskID.TASK_QUERY_ACTIVATE, time);
    }

    private static void querySchedule(Context context, long interval_time) {
        long time = System.currentTimeMillis() + interval_time;
        alarm(context, TaskID.TASK_QUERY_AUTO, time);
    }

    public static void installForce(Context context, long time) {
        alarm(context, TaskID.TASK_FORCE_UPDATE, time);
    }

    public static void installDelay(Context context, long time) {
        alarm(context, TaskID.TASK_INSTALL_DELAY, time);
    }

    public static void rebootForce(Context context, long time) {
        alarm(context, TaskID.TASK_FORCE_REBOOT, time);
    }

    public static void stopLogOutAlarm(Context context) {
        long time = System.currentTimeMillis() + 2 * 60 * 60 * 1000;
        SpManager.setStopLogOutTime(time);
        alarm(context, TaskID.TASK_STOP_LOG_OUT, time);
    }

    public static void dialogInstallDelay(Context context) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, new Random().nextInt(4));
        alarm(context, TaskID.TASK_DIALOG_INSTALL_DELAY, calendar.getTimeInMillis());
    }

    public static void resetStopLogOutAlarm(Context context) {
        alarm(context, TaskID.TASK_STOP_LOG_OUT, SpManager.getStopLogOutTime());
    }

    public static void download_later(Context context, long time) {
        alarm(context, TaskID.TASK_DOWNLOAD_DELAY, time);
    }

    public static void install_later(Context context, long time) {
        alarm(context, TaskID.TASK_INSTALL_LATER, time);
    }

    public static void download_night(Context context, long time) {
        alarm(context, TaskID.TASK_DOWNLOAD_NIGHT, time);
    }

    public static void download_install_later(Context context, long time) {
        alarm(context, TaskID.TASK_DOWNLOAD_INSTALL_DELAY, time);
    }

    public static void pop_cancel(Context context, long time) {
        long canceltime = System.currentTimeMillis() + time;
        LogUtil.d("The new version pop or download success pop will show in the " + SimpleDateFormat.getDateTimeInstance().format(canceltime));
        alarm(context,TaskID.TASK_NEW_VERSION_POP_DELAY, canceltime);
    }
    public static void notify_cancel(Context context, long time) {
        long canceltime = System.currentTimeMillis() + time;
        LogUtil.d("Notify cancel and new version notify will show in the " + SimpleDateFormat.getDateTimeInstance().format(canceltime));
        alarm(context,TaskID.TASK_NOTIFY_CANCEL, canceltime);
    }



    public static long getDefaultQuerySchedule(Context context) {
        long freq = PreferencesUtils.getLong(context, Setting.QUERY_SERVER_FREQ, Setting.DEFAULT_SERVER_FREQ);
        if (freq == Setting.DEFAULT_SERVER_FREQ){
            freq = PreferencesUtils.getInt(context, Setting.QUERY_LOCAL_FREQ, Setting.getDefaultFreq());
        }
        return freq * 1000 * 60;
    }

    public static void queryScheduleAlarm(Context context) {
        long interval = getDefaultQuerySchedule(context);
        querySchedule(context, interval);
    }

}

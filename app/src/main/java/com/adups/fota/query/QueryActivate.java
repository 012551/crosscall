package com.adups.fota.query;

import android.content.Context;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;

import com.adups.fota.MyApplication;
import com.adups.fota.config.Const;
import com.adups.fota.config.Setting;
import com.adups.fota.config.TaskID;
import com.adups.fota.manager.AlarmManager;
import com.adups.fota.service.CustomActionIntentService;
import com.adups.fota.service.CustomActionService;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.FileUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PreferencesUtils;
import com.adups.fota.utils.StorageUtil;

/**
 * Created by xw on 15-12-26.
 */
public class QueryActivate {

    public static boolean isOverActivateTime(Context context) {
        long startup_time = SystemClock.elapsedRealtime();
        Boolean device_provisioned = (Settings.Global.getInt(MyApplication.getAppContext().getContentResolver(), Settings.Global.DEVICE_PROVISIONED , 0) == 1);
        LogUtil.d("isOverActivateTime device_provisioned: " + device_provisioned);
        //开机时间超过，15分钟
        if ((startup_time >= DeviceInfoUtil.getInstance().getQueryActivate()) && device_provisioned) {
            if (!getFlag(context))
                putFlag(context);
            return true;
        } else if (!getFlag(context)) {
            AlarmManager.queryActivate(context);
        }
        long activate_total_time = PreferencesUtils.getLong(context, Setting.ACTIVATE_TOTAL_TIME, 0);
        //累计激活总时间是否超过15分钟
        if ((activate_total_time >= DeviceInfoUtil.getInstance().getQueryActivate()) && device_provisioned) {
            return true;
        }
        //判断文件是否存在
        return getFlag(context);
    }

    public static void activateAlarmCallback(Context context) {
        LogUtil.d("enter");
        Boolean device_provisioned = (Settings.Global.getInt(MyApplication.getAppContext().getContentResolver(), Settings.Global.DEVICE_PROVISIONED , 0) == 0);
        LogUtil.d("activateAlarmCallback device_provisioned: " + device_provisioned);
        if ((SystemClock.elapsedRealtime() < DeviceInfoUtil.getInstance().getQueryActivate()) || device_provisioned) { //开机时间超过小于15分钟
            AlarmManager.queryActivate(context);
            return;
        }
        PreferencesUtils.putLong(context, Setting.ACTIVATE_TOTAL_TIME, DeviceInfoUtil.getInstance().getQueryActivate());
        putFlag(context);
        AlarmManager.queryScheduleAlarm(context);
        CustomActionService.enqueueWork(context, TaskID.TASK_QUERY_AUTO);
    }

    /**
     * only on BootUp Completed Broadcast
     */
    public static void queryVerify(Context context) {
        if (isOverActivateTime(context)) {
            LogUtil.d("ota is activated");
            return;
        }
        AlarmManager.queryActivate(context);
    }

    private static boolean getFlag(Context context) {
        boolean isExit = !TextUtils.isEmpty(FileUtil.readFileSdcardFile(StorageUtil.getExternalMidWatchPackagePath(context) + Const.ACTIVATE_FILE));
        if (!isExit && FileUtil.hasReadPermission(context))
            isExit = !TextUtils.isEmpty(FileUtil.readFileSdcardFile(StorageUtil.getExternalMidWatchPath() + Const.ACTIVATE_FILE));
        LogUtil.d("dogWatch exists : " + isExit);
        return isExit;
    }

    private static void putFlag(Context context) {
        if (FileUtil.hasWritePermission(context))
            FileUtil.writeByteData(StorageUtil.getExternalMidWatchPath() + Const.ACTIVATE_FILE, "uu".getBytes());
        FileUtil.writeByteData(StorageUtil.getExternalMidWatchPackagePath(context) + Const.ACTIVATE_FILE, "uu".getBytes());
        LogUtil.d("put dogWatch");
    }

}

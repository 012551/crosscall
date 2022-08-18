package com.adups.fota.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;
import android.text.TextUtils;

import com.adups.fota.MyApplication;
import com.adups.fota.activity.BaseActivity;
import com.adups.fota.config.Const;
import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.config.TaskID;
import com.adups.fota.download.DownVersion;
import com.adups.fota.install.Install;
import com.adups.fota.manager.AlarmManager;
import com.adups.fota.manager.NoticeManager;
import com.adups.fota.manager.NotificationManager;
import com.adups.fota.manager.SpManager;
import com.adups.fota.query.QueryActivate;
import com.adups.fota.query.QueryVersion;
import com.adups.fota.receiver.BatteryReceiver;
import com.adups.fota.report.ReportManager;
import com.adups.fota.request.RequestParam;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.NetWorkUtil;
import com.adups.fota.utils.PreferencesUtils;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

public class CustomActionIntentService extends BaseService {


    public CustomActionIntentService() {
        super("CustomActionIntentService");
    }

    public static void enqueueWork(Context context, int taskId) {
        Intent intentService = new Intent(context, CustomActionIntentService.class);
        intentService.setAction(Const.SEND_CUSTOM_SERVICE_ACTION);
        intentService.putExtra(TaskID.TASK, taskId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intentService);
        } else {
            context.startService(intentService);
        }
    }

    @Override
    protected void onHandleIntent(@NonNull Intent intent) {
        String action = intent.getAction();
        LogUtil.d("action = " + action);
        if (!TextUtils.isEmpty(action) &&
                action.equalsIgnoreCase(Const.SEND_CUSTOM_SERVICE_ACTION)) {
            int taskId = intent.getIntExtra(TaskID.TASK, Integer.MAX_VALUE);
            LogUtil.d("task id = " + taskId);
            int version_status = PreferencesUtils.getInt(getApplicationContext(), Setting.FOTA_UPDATE_STATUS, Status.STATE_QUERY_NEW_VERSION);
            PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            boolean isScreenon = powerManager.isScreenOn();
            boolean isMobileConnect = NetWorkUtil.isMobileConnected(this);
            //boolean isOnlyWifi = PreferencesUtils.getBoolean(this, Setting.DOWNLOAD_ONLY_WIFI, DeviceInfoUtil.getInstance().isOnlyWifi());
            boolean isRoaming = NetWorkUtil.isRoaming(MyApplication.getAppContext());
            boolean is2G = NetWorkUtil.is2GConnected(MyApplication.getAppContext());
            boolean isWifi = NetWorkUtil.isWiFiConnected(MyApplication.getAppContext());
            LogUtil.d("version_status = " + version_status + "; isScreenon = " + isScreenon + "; isMobileConnect = " + isMobileConnect +
                    "; isRoaming = " + isRoaming + "; is2G = " + is2G);

            switch (taskId) {
                case TaskID.TASK_FORCE_DOWNLOAD:
                    if (DeviceInfoUtil.getInstance().getCanUse() == 0) {
                        LogUtil.d("TASK_FORCE_DOWNLOAD is disable");
                        break;
                    }
                    DownVersion.getInstance().forceDownload(this);
                    break;
                case TaskID.TASK_FORCE_UPDATE:
                case TaskID.TASK_DIALOG_INSTALL_DELAY:
                    if (DeviceInfoUtil.getInstance().getCanUse() == 0) {
                        LogUtil.d("TASK_FORCE_UPDATE is disable");
                        break;
                    }
                    Install.force_update(this);
                    break;
                case TaskID.TASK_FORCE_REBOOT:
                    if (DeviceInfoUtil.getInstance().getCanUse() == 0) {
                        LogUtil.d("TASK_FORCE_REBOOT is disable");
                        break;
                    }
                    Install.force_reboot(this);
                    break;
                case TaskID.TASK_QUERY_ACTIVATE:
                    if (DeviceInfoUtil.getInstance().getCanUse() == 0) {
                        LogUtil.d("TASK_QUERY_ACTIVATE is disable");
                        break;
                    }
                    QueryActivate.activateAlarmCallback(this);
                    break;
                case TaskID.TASK_QUERY_AUTO:
                    if (DeviceInfoUtil.getInstance().getCanUse() == 0) {
                        AlarmManager.queryScheduleAlarm(this);
                        LogUtil.d("TASK_QUERY_AUTO is disable");
                        break;
                    }
                    QueryVersion.getInstance().onQuerySchedule(this, false);//闹钟触发检测
                    DownVersion.getInstance().onDownloadTask(this);//闹钟触发下载
                    Status.downloadCompleteTask(this);

                    break;
                case TaskID.TASK_INSTALL_DELAY:
                    if (DeviceInfoUtil.getInstance().getCanUse() == 0) {
                        LogUtil.d("TASK_INSTALL_DELAY is disable");
                        break;
                    }
                    Install.installDelayCallback(this);
                    break;
                case TaskID.TASK_NOTICE_CLICK:
                    if (DeviceInfoUtil.getInstance().getCanUse() == 0) {
                        LogUtil.d("TASK_NOTICE_CLICK is disable");
                        break;
                    }
                    NoticeManager.click(this, 0);
                    break;
                case TaskID.TASK_TO_CHECK_VERSION:
                    if (DeviceInfoUtil.getInstance().getCanUse() == 0) {
                        LogUtil.d("TASK_TO_CHECK_VERSION is disable");
                        break;
                    }
                    QueryVersion.getInstance().onQueryScheduleTask(this);
                    break;
                case TaskID.TASK_STOP_LOG_OUT:
                    LogUtil.setSaveLog(false);
                    Map<String, String> map = new HashMap<>();
                    map.put(Setting.INTENT_PARAM_TASK_ID, SpManager.getLogTaskId());
                    map.put(RequestParam.ACTION_TYPE, BaseActivity.FCM_REPORT_TYPE_LOG);
                    map.put(RequestParam.LOG, PreferencesUtils.getString(this, Setting.DEBUG_LOG_PATH));
                    ReportManager.getInstance().fcmReport(this, map);
                    SpManager.resetStopLogOutTime();
                    break;
                case TaskID.TASK_BATTERY_MONITOR:
                    NotificationManager.getInstance().cancel(this, NotificationManager.NOTIFY_DL_COMPLETED);
                    IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                    registerReceiver(new BatteryReceiver(), intentFilter);
                    break;

                case TaskID.TASK_NEW_VERSION_POP_DELAY:
                case TaskID.TASK_NOTIFY_CANCEL:
                    if (DeviceInfoUtil.getInstance().getCanUse() == 0) {
                        LogUtil.d("TASK_NOTIFY_CANCEL is disable");
                        break;
                    }
                    if (version_status == Status.STATE_NEW_VERSION_READY) {
//                    if (!isScreenon && !isMobileConnect) {
//                        Trace.d("download begin");
//                        DownVersion.with(this).download(DownVersion.AUTO);
//                    } else {
                        LogUtil.d("show new version notify");
                        NotificationManager.getInstance().showNewVersion(this, false);
//                    }
                    }


                    if (version_status == Status.STATE_DL_PKG_COMPLETE) {
//                    if (!isScreenon && !isMobileConnect) {
//                        Trace.d("install begin");
//                        Install.force_update(this);
//                    } else {
                        LogUtil.d("show download completed notify");
                        NotificationManager.getInstance().showDownloadCompleted(this, false);
//                    }
                    }
                    break;
//            case TaskID.TASK_NEW_VERSION_POP_DELAY:
//                NotifyManager.with(this).showDownloadAndInstall(this, true);
//                break;
                case TaskID.TASK_INSTALL_LATER:
                    if (DeviceInfoUtil.getInstance().getCanUse() == 0) {
                        LogUtil.d("TASK_INSTALL_LATER is disable");
                        break;
                    }
                    if (!isScreenon) {
                        LogUtil.d("install begin");
                        Install.force_update(this);
                    } else {
                        Long alarmtime = PreferencesUtils.getLong(this, "INSTALL_DELAY");
                        Long installtime = alarmtime + 1000 * 60 * 60 * 24;
                        AlarmManager.install_later(this, installtime);
                        LogUtil.d("No force update,The new version install will begin in the " + SimpleDateFormat.getDateTimeInstance().format(installtime));
                        NotificationManager.getInstance().cancel(this, NotificationManager.NOTIFY_DL_COMPLETED);
                        NotificationManager.getInstance().showDownloadAndInstall(this, true,TaskID.TASK_INSTALL_LATER);
                    }
                    break;
                case TaskID.TASK_DOWNLOAD_DELAY:
                    if (DeviceInfoUtil.getInstance().getCanUse() == 0) {
                        LogUtil.d("TASK_DOWNLOAD_DELAY is disable");
                        break;
                    }
                    if (!isScreenon && isWifi && !is2G && !isRoaming) {
                        LogUtil.d("download begin");
                        DownVersion.getInstance().download(this, DownVersion.AUTO);
                    } else {
                        Long alarmtime = PreferencesUtils.getLong(this, "DOWNLOAD_DELAY");
                        Long downloadtime = alarmtime + 1000 * 60 * 60 * 24;
                        LogUtil.d("alarmtime =  " + alarmtime + "; downloadtime = " + downloadtime);
                        AlarmManager.download_later(this, downloadtime);
                        LogUtil.d("No force download ,The new version download will begin in the " + SimpleDateFormat.getDateTimeInstance().format(downloadtime));
                        NotificationManager.getInstance().cancel(this, NotificationManager.NOTIFY_NEW_VERSION);
                        NotificationManager.getInstance().showDownloadAndInstall(this, true,TaskID.TASK_DOWNLOAD_DELAY);
                    }
                    break;
                case TaskID.TASK_DOWNLOAD_INSTALL_DELAY:
                    if (DeviceInfoUtil.getInstance().getCanUse() == 0) {
                        LogUtil.d("TASK_DOWNLOAD_INSTALL_DELAY is disable");
                        break;
                    }
                    if (!isScreenon && isWifi && !is2G && !isRoaming) {
                        LogUtil.d("download and install begin");
                        PreferencesUtils.putInt(getApplicationContext(), "download_install_later", 1);
                        DownVersion.getInstance().download(this, DownVersion.AUTO);
                    } else {
                        Long alarmtime = PreferencesUtils.getLong(this, "DOWNLOAD_INSTALL_DELAY");
                        Long downloadandinstalltime = alarmtime + 1000 * 60 * 60 * 24;
                        LogUtil.d("alarmtime =  " + alarmtime + "; downloadandinstalltime = " + downloadandinstalltime);
                        AlarmManager.download_install_later(this, downloadandinstalltime);
                        LogUtil.d("No force download and install ,The new version download and install will begin in the " + SimpleDateFormat.getDateTimeInstance().format(downloadandinstalltime));
                        NotificationManager.getInstance().cancel(this, NotificationManager.NOTIFY_NEW_VERSION);
                        NotificationManager.getInstance().showDownloadAndInstall(this, true,TaskID.TASK_DOWNLOAD_INSTALL_DELAY);
                    }
                    break;
                case TaskID.TASK_DOWNLOAD_NIGHT:
                    DownVersion.getInstance().slientDownload(this);
                    break;
            }
        }
    }

}
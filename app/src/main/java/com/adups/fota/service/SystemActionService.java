package com.adups.fota.service;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.text.TextUtils;

import com.adups.fota.callback.RequestCallback;
import com.adups.fota.config.Status;
import com.adups.fota.download.DownVersion;
import com.adups.fota.manager.AlarmManager;
import com.adups.fota.manager.JobScheduleManager;
import com.adups.fota.manager.SpManager;
import com.adups.fota.query.QueryVersion;
import com.adups.fota.report.ReportManager;
import com.adups.fota.request.RequestManager;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.NetWorkUtil;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

public class SystemActionService extends JobIntentService implements RequestCallback {

    private static final int JOB_ID = 100;

    private static boolean isReceivedBoot = false;

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, SystemActionService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) return;
        if (action.equalsIgnoreCase(Intent.ACTION_MEDIA_BAD_REMOVAL)
                || action.equalsIgnoreCase(Intent.ACTION_MEDIA_REMOVED))
            LogUtil.setSaveLog(false);
        LogUtil.d("action = " + action);
        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
                LogUtil.d("isReceivedBoot : " + isReceivedBoot);
                if (isReceivedBoot) return;
                isReceivedBoot = true;
                AlarmManager.queryScheduleAlarm(this);//开机后设置查询闹钟，保证查询频率
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH)
                    JobScheduleManager.scheduleJob(this, JobScheduleManager.CHANGE_NETWORK_JOB_ID);
                if (SpManager.getStopLogOutTime() != 0L)//fcm功能，如果下发上报log时间内重启手机，则重新设定上报闹钟
                    AlarmManager.resetStopLogOutAlarm(this);
            case ConnectivityManager.CONNECTIVITY_ACTION:
                doAction(false);
                break;
            case Intent.ACTION_POWER_DISCONNECTED:
                doAction(true);
                break;
        }
    }

    private void doAction(boolean powerDisconnect) {
        if (DeviceInfoUtil.getInstance().getCanUse() == 0) {
            LogUtil.d("can not use fota to doAction");
        } else {
            boolean isConnected = NetWorkUtil.isConnected(this);
            LogUtil.d("isConnected = " + isConnected);
            if (isConnected) {
                QueryVersion.getInstance().onQuerySchedule(this, true);//检测
                if (!(powerDisconnect && Status.getVersionStatus(this) == Status.STATE_DOWNLOADING))
                    DownVersion.getInstance().onDownloadTask(this);//下载
                if (SpManager.isEuNoReport())
                    RequestManager.report(this, this);//重新上报gdpr相关
                ReportManager.getInstance().reportData(this);//上报数据库数据
            }
        }
    }

    @Override
    public void onResponseReturn(String data) throws Exception {
        LogUtil.d(data);
        SpManager.setNoReportStatus(false);
    }

}

package com.adups.fota.install;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import com.adups.fota.activity.InstallFailActivity;
import com.adups.fota.bean.EventMessage;
import com.adups.fota.bean.VersionBean;
import com.adups.fota.config.Const;
import com.adups.fota.config.Event;
import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.config.TaskID;
import com.adups.fota.manager.AlarmManager;
import com.adups.fota.manager.NoticeManager;
import com.adups.fota.manager.NotificationManager;
import com.adups.fota.query.QueryInfo;
import com.adups.fota.report.ReportData;
import com.adups.fota.system.Recovery;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.FileUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PackageUtils;
import com.adups.fota.utils.PreferencesUtils;
import com.adups.fota.utils.StorageUtil;
import com.adups.fota.utils.SystemSettingUtil;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.Calendar;

/**
 * Created by xw on 15-12-16.
 */
public class Install {

    public static final int INSTALL_DEFAULT_BATTERY = 30;    // 30 % ota_battery left
    private static final int INSTALL_FORCE_UPDATE_ALARM_TIME = 10;    //  min
    private static boolean isInstalling = false;

    public static void enterRecovery(final Context context, final String filePath) {
        if (!isSupportAbUpdate()) {
            Status.setVersionStatus(context, Status.STATE_REBOOT);
            NotificationManager.getInstance().cancel(context, NotificationManager.NOTIFY_DL_COMPLETED);
            if (Const.DEBUG_MODE || (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q))
                PreferencesUtils.putBoolean(context, Setting.FOTA_ENTER_RECOVERY, true);
            else
                SystemSettingUtil.putInt(Setting.RECOVERY_FROM_THIRD, 1);
        }
        SystemSettingUtil.putInt("fota_downloaded",0);
        PreferencesUtils.putString(context, Setting.FOTA_ORIGINAL_VERSION, DeviceInfoUtil.getInstance().getLocalVersion());
        LogUtil.d("update file path = " + filePath);
        StorageUtil.init(context);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isSupportAbUpdate()) {
                        Recovery.getInstance().executeAb(context, filePath);
                    } else if (Recovery.getInstance().execute(context, filePath) <= 0) {
                        LogUtil.d("install execute error!");
                        EventBus.getDefault().post(new EventMessage(Event.INSTALL, Status.UPDATE_FOTA_FAIL, 0, 0, null));
                        ReportData.postInstallResult(context, false, Status.UPDATE_FOTA_EXE_FAIL, null);
                    }
                } catch (Exception e) {
                    LogUtil.d("install exception : " + e.getMessage());
                }
            }
        }).start();
    }

    public static boolean isSupportAbUpdate() {
        return DeviceInfoUtil.getInstance().isSupportAbUpdate();
    }

    public static void installFail(Context context, String reason) {
        LogUtil.d(" installFail reason = " + reason);
        Intent intent = new Intent(context, InstallFailActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("reason", reason);
        context.startActivity(intent);

    }
    private static void doNormalUpdate(Context context) {
        if (isSupportAbUpdate()) {
            EventBus.getDefault().post(new EventMessage(Event.INSTALL, 100, 0, Recovery.AB_VERIFYING, Recovery.AB_FLAG));
        }
        String sh256FromServer = null;
        VersionBean model = QueryInfo.getInstance().getVersionInfo();
        if (model != null) {
            sh256FromServer = model.getSha();
        }
        int status = 0;
        BaseInstallVerify installResult;
        if (TextUtils.isEmpty(sh256FromServer)) {
            installResult = new OldInstallVerify();
        } else {
            installResult = new NewInstallVerify();
        }
        try {
            status = installResult.verify(context, StorageUtil.getPackageFileName(context), sh256FromServer);
        } catch (Exception e) {
            ReportData.postInstall(context, ReportData.INSTALL_STATUS_CAUSE_VERIFY_EXCEPTION);
        }
        LogUtil.d("verify package status = " + status);
        if (status == Status.UPDATE_STATUS_OK) {
            String updatePath = StorageUtil.getPackageFileName(context);
            File update1File = new File(Const.UPDATE_FILE1);
            File update2File = new File(Const.UPDATE_FILE2);
            File update4File = new File(Const.UPDATE_FILE4);
            /*just for Spreadtrum*/
            StorageUtil.deleteModemFile(StorageUtil.getSdcardRoot(context, false));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (update1File.exists()) {
                    LogUtil.d("update1File = " + update1File.getPath());
                    boolean isCopyOk = FileUtil.copy(Const.UPDATE_FILE1, Const.UPDATE_FILE3, true);
                    if (isCopyOk) {
                        updatePath = Const.UPDATE_FILE3;
                    }
                } else if (update2File.exists()) {
                    LogUtil.d("update2File = " + update2File.getPath());
                    boolean isCopyOk = FileUtil.copy(Const.UPDATE_FILE2, Const.UPDATE_FILE3, true);
                    if (isCopyOk) {
                        updatePath = Const.UPDATE_FILE3;
                    }
                } else if (update4File.exists()) {
                    LogUtil.d("update4File = " + update4File.getPath());
                    boolean isCopyOk = FileUtil.copy(Const.UPDATE_FILE4, Const.UPDATE_FILE3, true);
                    if (isCopyOk) {
                        updatePath = Const.UPDATE_FILE3;
                    }
                } else {
                    updatePath = StorageUtil.getPackageFileName(context);
                }
            } else {
                updatePath = StorageUtil.getPackageFileName(context);
            }
            LogUtil.d("updatePath = " + updatePath);
            final String recoveryPath = updatePath;
            if (model != null) {
                PreferencesUtils.putString(context, Setting.FOTA_UPDATE_VERSION, model.getVersionName());
                PreferencesUtils.putBoolean(context, Setting.FOTA_INSTALL_RESULT_POP,
                        QueryInfo.getInstance().getPolicyValue(QueryInfo.INSTALL_RESULT_POP, Boolean.class));
                PreferencesUtils.putInt(context, Setting.OTA_OLD_NOTIFY_FLAG,
                        QueryInfo.getInstance().getPolicyValue(QueryInfo.QUERY_NOTICE_TYPE, Integer.class));
            }
            PreferencesUtils.putBoolean(context, Setting.FOTA_UPDATE_LOCAL, false);
            PreferencesUtils.putInt(context, "query_count", 0);
            PreferencesUtils.putInt(context, "download_install_later", 0);

            enterRecovery(context, recoveryPath);
        } else {
            LogUtil.d("no install reason : status not correct");
            NoticeManager.updateShortcut(context, 1);
            QueryInfo.getInstance().reset(context);
            ReportData.postInstallResult(context, false, status, installResult.getReason());
        }
        // notify message
        EventBus.getDefault().post(new EventMessage(Event.INSTALL, status, 0, 0, null));
    }

    public static void update(final Context context) {
        if (PackageUtils.getUserId(android.os.Process.myUid()) != 0) {
            LogUtil.d("no update reason : not system user");
            return;
        }
        if (isInstalling) {
            ReportData.postInstall(context, ReportData.INSTALL_STATUS_CAUSE_INSTALLING);
            return;
        }
        if (isSupportAbUpdate()) {
            EventBus.getDefault().post(new EventMessage(Event.INSTALL, 100, 0, 5, Recovery.AB_FLAG));
        }
        isInstalling = true;
        LogUtil.d("ota install update start");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    VersionBean version = QueryInfo.getInstance().getVersionInfo();
                    if (version != null) {
                        // normal update
                        LogUtil.d("ota install normal ");
                        doNormalUpdate(context);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    LogUtil.d("update exception : " + e.getMessage());
                    ReportData.postInstall(context, ReportData.INSTALL_STATUS_CAUSE_EXCEPTION);
                }
                isInstalling = false;
            }
        }).start();
    }

    public static void force_update(Context context) {
        String[] time = QueryInfo.getInstance().getPolicyValueArray(QueryInfo.INSTALL_TIME);
        int statu = Status.getVersionStatus(context);
        LogUtil.d("statu = " + statu);
        if (Status.getVersionStatus(context) != Status.STATE_DL_PKG_COMPLETE) {
            LogUtil.d("no force update reason : status not correct");
            ReportData.postInstall(context, ReportData.INSTALL_STATUS_CAUSE_NOT_DLCOMPLETE);
            return;
        }
        int startTime, endTime;
        try {
            startTime = Integer.valueOf(time[0]);
            endTime = Integer.valueOf(time[1]);
            if (startTime > endTime) { //处理跨天 2017年2月21日17:15:10
                endTime = 24 + endTime;
            }
        } catch (Exception e) {
            startTime = 0;
            endTime = 24;
        }
        int isHourRange = isHourRange(startTime, endTime);
        if (!canForceUpdate(context) || isHourRange != 0) {
            LogUtil.d("no force update reason : ota battery low or no in hour range");
            ReportData.postInstall(context, ReportData.INSTALL_STATUS_CAUSE_NOT_RIGHT_TIME);
            LogUtil.d("show install fail notify and set install alarm");
            Long installlater = PreferencesUtils.getLong(context, "DOWNLOAD_INSTALL_DELAY", 0);
            AlarmManager.install_later(context, installlater + 1000 * 60 * 60 * 24);
            NotificationManager.getInstance().cancel(context, NotificationManager.NOTIFY_DL_COMPLETED);
            NotificationManager.getInstance().showDownloadAndInstall(context, true, TaskID.TASK_INSTALL_LATER);
        } else {
            LogUtil.d("time arrive start force update");
            ReportData.postInstall(context, ReportData.INSTALL_STATUS_AUTO);
            update(context);
        }
    }

    public static void force_reboot(Context context) {
        if (!isSupportAbUpdate()) {
            LogUtil.d("not support ab update,no need force_reboot");
            return;
        }
        String[] time = QueryInfo.getInstance().getPolicyValueArray(QueryInfo.INSTALL_TIME);
        if (Status.getVersionStatus(context) != Status.STATE_REBOOT) {
            ReportData.postInstall(context, ReportData.REBOOT_STATUS_CAUSE_NOT_DLCOMPLETE);
            return;
        }
        int startTime, endTime;
        if (QueryInfo.getInstance().getPolicyValue(QueryInfo.INSTALL_FORCED, Boolean.class)) {
            try {
                startTime = Integer.valueOf(time[0]);
                endTime = Integer.valueOf(time[1]);
                if (startTime > endTime) { //处理跨天 2017年2月21日17:15:10
                    endTime = 24 + endTime;
                }
            } catch (Exception e) {
                startTime = 0;
                endTime = 24;
            }
        } else {
            ReportData.postInstall(context, ReportData.DOWN_STATUS_CAUSE_NOT_FORCE_REBOOT);
            return;
        }
        int isHourRange = isHourRange(startTime, endTime);
        if (DeviceInfoUtil.getInstance().isScreenOn(context) || isHourRange != 0) {
            ReportData.postInstall(context, ReportData.REBOOT_STATUS_CAUSE_NOT_RIGHT_TIME);
            forceRebootAlarm(context, isHourRange, startTime, endTime);
        } else {
            LogUtil.d("[force_update] time arrive start force update");
            ReportData.postInstall(context, ReportData.REBOOT_STATUS_AUTO);
            Recovery.getInstance().reboot(context);
        }
    }

    private static boolean canForceUpdate(Context context) {
        int batteryLevel = DeviceInfoUtil.getInstance().getRealBattery(context);
        boolean isScreenOn = DeviceInfoUtil.getInstance().isScreenOn(context);
        int level = DeviceInfoUtil.getInstance().getBattery();
        LogUtil.d("realBattery = " + batteryLevel + "; limitBattery = " + level + "; isScreenOn = " + isScreenOn);
        return batteryLevel >= level && (!isScreenOn || DeviceInfoUtil.getInstance().isScreen());
    }

    private static int isHourRange(int startTime, int endTime) {
        long currentTime = System.currentTimeMillis();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(currentTime);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int ret;
        LogUtil.d("startTime = " + startTime + "; endTime = " + endTime + "; hour = " + hour);
        if (hour < startTime) {
            ret = -1;
        } else if (hour >= endTime) {
            ret = 1;
        } else {
            ret = 0;
        }
        return ret;
    }

    private static void forceUpdateAlarm(Context context, int isHourRange, int startTime, int endTime) {
        LogUtil.d("isHourRange = " + isHourRange
                + "; startTime = " + startTime
                + "; endTime = " + endTime);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        // now time < start time
        if (isHourRange == -1) {
            c.set(Calendar.HOUR_OF_DAY, startTime);
            c.set(Calendar.MINUTE, 0);
        }
        //  now time > end time ,then next day start time
        else if (isHourRange == 1) {
            c.add(Calendar.DATE, 1);
            c.set(Calendar.HOUR_OF_DAY, startTime);
            c.set(Calendar.MINUTE, 0);
        } else {
            c.set(Calendar.MINUTE, c.get(Calendar.MINUTE) + INSTALL_FORCE_UPDATE_ALARM_TIME);
        }
        LogUtil.d("force time " + c.getTimeInMillis());
        AlarmManager.installForce(context, c.getTimeInMillis());
    }

    private static void forceRebootAlarm(Context context, int isHourRange, int startTime, int endTime) {
        LogUtil.d("[forceRebootAlarm] isHourRange,startTime,endTime ==="
                + isHourRange + "||"
                + startTime + "||"
                + endTime);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        // now time < start time
        if (isHourRange == -1) {
            c.set(Calendar.HOUR_OF_DAY, startTime);
            c.set(Calendar.MINUTE, 0);
        }
        //  now time > end time ,then next day start time
        else if (isHourRange == 1) {
            c.add(Calendar.DATE, 1);
            c.set(Calendar.HOUR_OF_DAY, startTime);
            c.set(Calendar.MINUTE, 0);
        } else {
            c.set(Calendar.MINUTE, c.get(Calendar.MINUTE) + INSTALL_FORCE_UPDATE_ALARM_TIME);
        }
        LogUtil.d("[forceUpdateAlarm] force time " + c.getTimeInMillis());
        AlarmManager.rebootForce(context, c.getTimeInMillis());
    }

    public static boolean isBatteryAbility(Context context, int level) {
        int batteryLevel = DeviceInfoUtil.getInstance().getRealBattery(context);
        return batteryLevel >= level;
    }

    public static boolean isEnoughSpace(Context context, String file_path) {
        VersionBean versionInfo = QueryInfo.getInstance().getVersionInfo();
        if (versionInfo != null && versionInfo.getIsOldPkg() == 1) {
            return true;
        }
        long unzipSize = (long) (FileUtil.getFileSize(file_path) * 1.5);
        return (StorageUtil.SDCARD_STATE_OK == StorageUtil.checkSdcardIsAvailable(context, unzipSize));
    }


    /**
     * install delay function,user select a schedule to update later on
     *
     * @param context {@linkplain Context}
     */
    public static void installDelayCallback(Context context) {
        if (Status.getVersionStatus(context) == Status.STATE_DL_PKG_COMPLETE) {
            int notice_type = QueryInfo.getInstance().getPolicyValue(QueryInfo.INSTALL_NOTICE_TYPE, Integer.class);
            boolean notice_resident = QueryInfo.getInstance().getPolicyValue(QueryInfo.INSTALL_NOTICE_RESIDENT, Boolean.class);
            LogUtil.d("installDelay notice : notice_type = " + notice_type + "; notice_resident = " + notice_resident);
            if (notice_type == 0) {//notification,桌面图标功能去除
                NotificationManager.getInstance().showDownloadCompleted(context, notice_resident);
            }else if (notice_type ==2){
                NoticeManager.forwardPopWindow(context, Status.STATE_DL_PKG_COMPLETE);
            }
            if (notice_type != 3){
                long schedule_time = PreferencesUtils.getLong(context, Setting.FOTA_INSTALL_DELAY_SCHEDULE);
                if (schedule_time > 0)
                    AlarmManager.installDelay(context, schedule_time + System.currentTimeMillis());
            }
        }
    }

}



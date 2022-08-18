package com.adups.fota.config;

import android.content.Context;
import android.text.TextUtils;

import com.adups.fota.bean.EventMessage;
import com.adups.fota.bean.VersionBean;
import com.adups.fota.install.Install;
import com.adups.fota.manager.NoticeManager;
import com.adups.fota.manager.NotificationManager;
import com.adups.fota.query.QueryInfo;
import com.adups.fota.query.QueryVersion;
import com.adups.fota.report.ReportData;
import com.adups.fota.service.CustomActionIntentService;
import com.adups.fota.service.CustomActionService;
import com.adups.fota.system.Recovery;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.FileUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PreferencesUtils;
import com.adups.fota.utils.StorageUtil;
import com.adups.fota.utils.SystemSettingUtil;

import org.greenrobot.eventbus.EventBus;

import java.io.File;

public class Status {

    public static final int STATE_QUERY_NEW_VERSION = 0;
    public static final int STATE_NEW_VERSION_READY = 1;
    public static final int STATE_DOWNLOADING = 2;
    public static final int STATE_PAUSE_DOWNLOAD = 3;
    public static final int STATE_DL_PKG_COMPLETE = 4;
    public static final int STATE_AB_UPDATING = 5;
    public static final int STATE_REBOOT = 6;
    public static final int STATE_DL_PKG_COMPLETE_ACTION = 7;

    public static final int UPDATE_STATUS_UNZIP_ERROR = 401;
    public static final int UPDATE_STATUS_CKSUM_ERROR = 402;
    public static final int UPDATE_STATUS_RUNCHECKERROR = 403;
    public static final int UPDATE_STATUS_ROM_DAMAGED = 404;
    public static final int UPDATE_STATUS_OK = 405;
    public static final int UPDATE_FOTA_RENAME_FAIL = 408;
    public static final int UPDATE_FOTA_NO_MD5 = 409;
    public static final int UPDATE_FOTA_NO_PKG = 410;
    public static final int UPDATE_FOTA_PKG_MD5_FAIL = 411;
    public static final int UPDATE_FOTA_NO_REBOOT = 412;
    public static final int UPDATE_FOTA_SUCCESS = 413;
    public static final int UPDATE_FOTA_FAIL = 414;
    public static final int UPDATE_FOTA_EXE_FAIL = 415;
    public static final int UPDATE_STATUS_SH256_ERROR = 416;
    public static final int UPDATE_STATUS_ONLY_WIFI = 417;
    public static final int UPDATE_STATUS_NO_SDCARD = 418;
    public static final int UPDATE_STATUS_SDCARD_ILLEGAL = 419;
    public static final int UPDATE_STATUS_USER_CANCEL = 420;

    public final static int DOWNLOAD_STATUS_NONE = 0;
    public final static int DOWNLOAD_STATUS_DOWNLOADING = 1;
    public final static int DOWNLOAD_STATUS_PAUSE = 2;


    /**
     * invoke function when query new version or change current version
     *
     * @param context Context
     */
    public static void queryNewVersion(Context context, VersionBean version) {
        if (QueryInfo.getInstance().getVersionInfo() == null) {
            LogUtil.d("version empty");
            return;
        }
        LogUtil.d("version exist");

        try {
            SystemSettingUtil.putInt("fota_updateavailable",1);
        }catch (Exception e){}
        LogUtil.d("fota_updateavailable: "+ SystemSettingUtil.getInt("fota_updateavailable",0));

        setVersionStatus(context, Status.STATE_NEW_VERSION_READY);
        PreferencesUtils.putString(context, Setting.FOTA_ORIGINAL_VERSION, DeviceInfoUtil.getInstance().getLocalVersion());
        PreferencesUtils.putString(context, Setting.FOTA_UPDATE_VERSION, version.getVersionName());
        PreferencesUtils.putInt(context, Setting.FOTA_UPDATE_TYPE, QueryVersion.getInstance().getQueryVersionType());
        //clear notify
        NotificationManager.getInstance().cancel(context, NotificationManager.NOTIFY_DOWNLOADING);
        String download_path = QueryInfo.getInstance().getPolicyValue(QueryInfo.DOWNLOAD_PATH, String.class);
        doUpgradePath(download_path);
        NoticeManager.query(context);
        CustomActionService.enqueueWork(context, TaskID.TASK_FORCE_DOWNLOAD);
    }


    /**
     * invoke function when packages had completed
     *
     * @param context Context
     */
    public static void setDownloadCompleted(Context context) {
        LogUtil.d(" ");
        EventBus.getDefault().post(new EventMessage(Event.DOWNLOAD, Event.DOWNLOAD_SUCCESS, 0, 0, null));
        PreferencesUtils.putLong(context, Setting.FOTA_INSTALL_DELAY_SCHEDULE, 0);
        setVersionStatus(context, Status.STATE_DL_PKG_COMPLETE);
        //clear notify
        NotificationManager.getInstance().cancel(context, NotificationManager.NOTIFY_DOWNLOADING);
        NoticeManager.download(context);
    }

    /**
     * invoke function when packages had completed
     *
     * @param context Context
     */
    public static void downloadCompletedInstall(Context context) {
        boolean force_install = QueryInfo.getInstance().getPolicyValue(QueryInfo.INSTALL_FORCED, Boolean.class);
        int download_install_later = PreferencesUtils.getInt(context, "download_install_later", 0);
        LogUtil.d("download_completed_instal,force_install = " + force_install);
        LogUtil.d("download_completed_instal,download_install_later = " + download_install_later);
        /*if (Install.isSupportAbUpdate()) {  //?support ab update
            if (Install.isBatteryAbility(context, DeviceInfoUtil.getInstance().getBattery())) {  //battery is not enough
                ReportData.postInstall(context, ReportData.INSTALL_STATUS_AUTO);
                Install.update(context.getApplicationContext());
            } else { //battery is not enough and set alarm
                LogUtil.d("no update reason : battery not enough");
                CustomActionIntentService.enqueueWork(context, TaskID.TASK_BATTERY_MONITOR);
                ReportData.postInstall(context, ReportData.INSTALL_STATUS_CAUSE_NOT_RIGHT_TIME);
                EventBus.getDefault().post(new EventMessage(Event.INSTALL, 100, 0, Recovery.AB_BATTERY_LOW, "ab"));
            }
        }*/
        //not support ab update
        if (force_install || download_install_later == 1) {
            PreferencesUtils.putInt(context, "download_install_later", 0);
            Install.force_update(context);
        } else {
            LogUtil.d("no update reason : not support ab update and no force install");
            ReportData.postInstall(context, ReportData.INSTALL_STATUS_CAUSE_NOT_FORCE_UPGRADE);
        }
    }

    /**
     * invoke function when recover to idle status
     *
     * @param context Context
     */
    public static void idleReset(Context context) {
        ReportData.postDownload(context, ReportData.DOWN_STATUS_CANCEL, 0);
        QueryInfo.getInstance().reset(context);
    }

    public static void resetFactory(Context context) {
        FileUtil.delFolder(context.getFilesDir().getPath() + "/shared_prefs");
        idleReset(context);
    }

    private static void doUpgradePath(String upgradePath) {
        String[] temp;
        if (!TextUtils.isEmpty(upgradePath) && (upgradePath.contains("#"))) {
            temp = upgradePath.split("#");
            if (temp.length == 3) {
                StorageUtil.setUpgradePath(temp[0], temp[1], temp[2]);
                LogUtil.d("ota download path :" +
                        "path[0]=" + temp[0] +
                        ",path[1]=" + temp[1] +
                        ",path[2]=" + temp[2]);
            }
        }
    }

    public static void downloadCompleteTask(Context context) {
        int status = getVersionStatus(context);
        LogUtil.d("downloadCompleteTask,status=" + status);
        if (status == Status.STATE_DL_PKG_COMPLETE) {
            downloadCompletedInstall(context);
        } else if (status == Status.STATE_AB_UPDATING) {
            if (PreferencesUtils.getBoolean(context, Setting.FOTA_UPDATE_LOCAL, false)) {
                String path = PreferencesUtils.getString(context, Setting.FOTA_UPDATE_LOCAL_PATH, "");
                if (!TextUtils.isEmpty(path) && new File(path).exists()) {
                    Recovery.getInstance().executeAb(context, path);
                } else {
                    idleReset(context);
                }
                return;
            }
            Recovery.getInstance().executeAb(context, StorageUtil.getPackageFileName(context));
        } else if (status == Status.STATE_REBOOT) {
            Install.force_reboot(context);
        }
    }

    public static int getVersionStatus(Context context) {
        return PreferencesUtils.getInt(context, Setting.FOTA_UPDATE_STATUS, Status.STATE_QUERY_NEW_VERSION);
    }

    public static void setVersionStatus(Context context, int status) {
        PreferencesUtils.putInt(context, Setting.FOTA_UPDATE_STATUS, status);
        if (!Const.DEBUG_MODE)
            SystemSettingUtil.putInt(Setting.FOTA_UPDATE_STATUS, status);
    }

}

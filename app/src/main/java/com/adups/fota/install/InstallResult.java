package com.adups.fota.install;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.adups.fota.activity.InstallResultActivity;
import com.adups.fota.config.Const;
import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.manager.NoticeManager;
import com.adups.fota.manager.SpManager;
import com.adups.fota.query.QueryInfo;
import com.adups.fota.report.ReportData;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PreferencesUtils;
import com.adups.fota.utils.SystemSettingUtil;

/**
 * Created by xw on 15-12-16.
 */
public class InstallResult {

    public static boolean verify(Context context) {
        synchronized (InstallResult.class) {
            boolean rebootRecovery = PreferencesUtils.getBoolean(context, Setting.FOTA_ENTER_RECOVERY) ||
                    (SystemSettingUtil.getInt(Setting.FOTA_ENTER_RECOVERY, 0) == 1);
            int status = Status.getVersionStatus(context);
            LogUtil.d("install verify,rebootRecovery : " + rebootRecovery + " ,status : " + status);
            if (rebootRecovery || status == Status.STATE_REBOOT) {
                boolean result = isSuccess(context);
                reportInstallResult(context, result);
                if (!rebootRecovery) {
                    PreferencesUtils.putString(context, Setting.FOTA_ORIGINAL_VERSION, "");
                    PreferencesUtils.putString(context, Setting.FOTA_UPDATE_VERSION, "");
                }
                PreferencesUtils.putBoolean(context, Setting.FOTA_ENTER_RECOVERY, false);
                SystemSettingUtil.putInt(Setting.FOTA_ENTER_RECOVERY, 0);
                QueryInfo.getInstance().reset(context);
                if (1 == PreferencesUtils.getInt(context, Setting.OTA_OLD_NOTIFY_FLAG, 0)) {
                    NoticeManager.updateShortcut(context, 1);
                }
                int type = PreferencesUtils.getInt(context, Setting.FOTA_UPDATE_TYPE, -1);
                LogUtil.d("FOTA_UPDATE_TYPE = " + type);
                if (result) {
                    Status.setVersionStatus(context, Status.STATE_QUERY_NEW_VERSION);
                    PreferencesUtils.putString(context, Setting.FOTA_ORIGINAL_VERSION, DeviceInfoUtil.getInstance().getLocalVersion());
                    if (isShow(context, rebootRecovery)) {
                        displayUpdateResult(context);
                    }
                    SpManager.removeUpgradeLaterTimes();
                }
                setUpdateRecord(context, result);
                if (result) {
                    //发送升级成功广播
                    sendUpdateSuccessBroadcast(context);
                }
            }
            return rebootRecovery;
        }
    }

    //记录升级失败次数
    private static void setUpdateRecord(Context context, boolean result) {
        int updateFailCount = PreferencesUtils.getInt(context, Setting.FOTA_INSTALL_FAIL_COUNTS, 0);
        if (!result) {
            updateFailCount++;
            PreferencesUtils.putInt(context, Setting.FOTA_INSTALL_FAIL_COUNTS, updateFailCount);
        }
    }

    //记录升级失败次数
    static void setVerifiedRecord(Context context) {
        int updateFailCount = PreferencesUtils.getInt(context, Setting.FOTA_INSTALL_FAIL_COUNTS, 0);
        updateFailCount++;
        PreferencesUtils.putInt(context, Setting.FOTA_INSTALL_FAIL_COUNTS, updateFailCount);
    }

    private static boolean isShow(Context context, boolean isNew) {
        if (isNew) {
            return PreferencesUtils.getBoolean(context, Setting.FOTA_INSTALL_RESULT_POP, false);
        } else {
            return 0 == PreferencesUtils.getInt(context, Setting.OTA_OLD_POP_FLAG, 1);
        }
    }

    public static boolean isSuccess(Context context) {
        String deviceVersion = DeviceInfoUtil.getInstance().getLocalVersion();
        String oldVersion = PreferencesUtils.getString(context, Setting.FOTA_ORIGINAL_VERSION, "");
        LogUtil.d("oldVersion = " + oldVersion + ";deviceVersion = " + deviceVersion);
        return !TextUtils.isEmpty(oldVersion) && !oldVersion.equalsIgnoreCase(deviceVersion);
    }

    private static void displayUpdateResult(Context context) {
        LogUtil.d("forward InstallResultActivity");
        String device_version = DeviceInfoUtil.getInstance().getLocalVersion();
        Intent intent = new Intent(context, InstallResultActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("version", device_version);
        context.startActivity(intent);
    }

    /**
     * save result to database,and report to server later on
     *
     * @param result success or fail for update
     */
    private static void reportInstallResult(Context context, boolean result) {
        LogUtil.d("install report,install success:" + result);
        ReportData.postInstallResult(context, result,
                (result ? Status.UPDATE_FOTA_SUCCESS : Status.UPDATE_FOTA_FAIL), null);
    }

    //判断是否发送升级成功的广播
    private static void sendUpdateSuccessBroadcast(Context mContext) {
        if (DeviceInfoUtil.getInstance().isSendSuccessBroadcast()) {
            LogUtil.d("sendUpdateSuccessBroadcast");
            Intent intent = new Intent();
            intent.setAction(Const.SEND_UPDATE_SUCCESS_BROADCAST);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            mContext.sendBroadcast(intent, Const.SEND_BROADCAST_PERMISSION);
        }
    }

}



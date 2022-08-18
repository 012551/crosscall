package com.adups.fota.manager;

import android.content.Context;
import android.net.Uri;

import com.adups.fota.MyApplication;
import com.adups.fota.config.Setting;
import com.adups.fota.request.RequestParam;
import com.adups.fota.utils.PreferencesUtils;


public class SpManager {

    private static final String CACHE_URL = "cache_url";
    private static final String STOP_LOG_OUT_TIME = "stop_log_out_time";
    private static final String UPGRADE_CHECK_STATUS = "upgrade_check_status";
    private static final String UPGRADE_LATER_TIMES = "upgrade_later_times";
    private static final String DOWNLOAD_START_TIME = "download_start_time";
    private static final String DOWNLOAD_USE_TIME = "download_use_time";
    private static final String DOWNLOAD_PERCENT = "download_percent";

    private static Context getContext() {
        return MyApplication.getAppContext();
    }

    public static String getFcmId() {
        return PreferencesUtils.getString(getContext(), RequestParam.FCM_ID);
    }

    public static void setFcmId(String fcmId) {
        PreferencesUtils.putString(getContext(), RequestParam.FCM_ID, fcmId);
    }

    public static void setCacheUrl(String cacheUrl) {
        PreferencesUtils.putString(getContext(), CACHE_URL, cacheUrl);
    }

    public static String getLogTaskId() {
        return PreferencesUtils.getString(getContext(), Setting.INTENT_PARAM_TASK_ID);
    }

    public static void setLogTaskId(String taskId) {
        PreferencesUtils.putString(getContext(), Setting.INTENT_PARAM_TASK_ID, taskId);
    }

    public static long getStopLogOutTime() {
        return PreferencesUtils.getLong(getContext(), STOP_LOG_OUT_TIME, 0L);
    }

    public static void setStopLogOutTime(long time) {
        PreferencesUtils.putLong(getContext(), STOP_LOG_OUT_TIME, time);
    }

    public static void resetStopLogOutTime() {
        setStopLogOutTime(0L);
    }

    public static boolean getUpgradeCheckStatus() {
        return PreferencesUtils.getBoolean(getContext(), UPGRADE_CHECK_STATUS, false);
    }

    public static void setUpgradeCheckStatus(boolean status) {
        PreferencesUtils.putBoolean(getContext(), UPGRADE_CHECK_STATUS, status);
    }

    public static boolean isUpgradeLaterOverTimes() {
        return PreferencesUtils.getInt(getContext(), UPGRADE_LATER_TIMES, 0) > 2;
    }

    public static void addUpgradeLaterTimes() {
        PreferencesUtils.putInt(getContext(), UPGRADE_LATER_TIMES,
                PreferencesUtils.getInt(getContext(), UPGRADE_LATER_TIMES, 0) + 1);
    }

    public static void removeUpgradeLaterTimes() {
        PreferencesUtils.putInt(getContext(), UPGRADE_LATER_TIMES, 0);
    }

    public static void recordDownloadStartTime(long time) {
        PreferencesUtils.putLong(getContext(), DOWNLOAD_START_TIME, time);
    }

    public static long getDownloadStartTime() {
        return PreferencesUtils.getLong(getContext(), DOWNLOAD_START_TIME, 0L);
    }

    public static void clearDownloadStartTime() {
        recordDownloadStartTime(0L);
    }

    public static void recordDownloadUseTime(long time) {
        PreferencesUtils.putLong(getContext(), DOWNLOAD_USE_TIME, time + getDownloadUseTime());
    }

    public static long getDownloadUseTime() {
        return PreferencesUtils.getLong(getContext(), DOWNLOAD_USE_TIME, 0L);
    }

    public static void clearDownloadUseTime() {
        PreferencesUtils.putLong(getContext(), DOWNLOAD_USE_TIME, 0L);
    }

    public static void clearDownloadRecords() {
        clearDownloadStartTime();
        clearDownloadUseTime();
    }

    /*
     * 最低一分钟，最长一天
     * */
    public static String[] getJobQueryTime() {
        return PreferencesUtils.getString(getContext(),
                Setting.QUERY_JOB_SCHEDULE_TIME, "5#1440").split("#");
    }

    public static void setJobQueryTime(String value) {
        PreferencesUtils.putString(getContext(), Setting.QUERY_JOB_SCHEDULE_TIME, value);
    }

    /*
     * 最低60分钟，最长一天
     * */
    public static String[] getJobDownloadTime() {
        return PreferencesUtils.getString(getContext(),
                Setting.QUERY_JOB_SCHEDULE_DOWNLOADING_TIME, "60#1440").split("#");
    }

    public static void setJobDownloadTime(String value) {
        PreferencesUtils.putString(getContext(), Setting.QUERY_JOB_SCHEDULE_DOWNLOADING_TIME, value);
    }

    public static boolean getRejectStatus() {
        return PreferencesUtils.getBoolean(getContext(), Setting.REJECT_STATUS, false);
    }

    public static void setRejectStatus(boolean flag) {
        PreferencesUtils.putBoolean(getContext(), Setting.REJECT_STATUS, flag);
    }

    public static boolean isUserReject() {
        return MyApplication.isBootExit() && getRejectStatus();
    }

    public static void setConnectNetValue() {
        if (MyApplication.isBootExit()) {
            Uri uri = Uri.parse("content://com.adups.privacypolicy.MyContentProvider/reject_status");
            setRejectStatus(Boolean.valueOf(getContext().getContentResolver().getType(uri)));
        }
        PreferencesUtils.putBoolean(getContext(), Setting.EU_CONNECT_NET, !MyApplication.isBootExit() || !getRejectStatus());
    }

    public static boolean isEuNoReport() {
        return PreferencesUtils.getBoolean(getContext(), Setting.EU_NO_REPORT, false);
    }

    public static void setNoReportStatus(boolean flag) {
        PreferencesUtils.putBoolean(getContext(), Setting.EU_NO_REPORT, flag);
    }

    public static String getString(String key) {
        return PreferencesUtils.getString(getContext(), key);
    }

    public static String getString(String key, String defaultValue) {
        return PreferencesUtils.getString(getContext(), key, defaultValue);
    }

    public static void setString(String key, String value) {
        PreferencesUtils.putString(getContext(), key, value);
    }

    public static int getDownloadPercent() {
        return PreferencesUtils.getInt(getContext(), DOWNLOAD_PERCENT, 0);
    }

    public static void setDownloadPercent(int percent) {
        PreferencesUtils.putInt(getContext(), DOWNLOAD_PERCENT, percent);
    }

}

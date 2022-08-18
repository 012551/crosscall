package com.adups.fota.report;

import android.content.Context;
import android.text.TextUtils;

import com.adups.fota.bean.ReportBaseBean;
import com.adups.fota.bean.ReportDownloadBean;
import com.adups.fota.bean.ReportInstallBean;
import com.adups.fota.bean.ReportInstallResultBean;
import com.adups.fota.bean.ReportQueryBean;
import com.adups.fota.bean.VersionBean;
import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.download.DownVersion;
import com.adups.fota.manager.SpManager;
import com.adups.fota.query.QueryInfo;
import com.adups.fota.query.QueryVersion;
import com.adups.fota.request.RequestResult;
import com.adups.fota.system.Recovery;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.JsonUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PreferencesUtils;
import com.adups.fota.utils.StorageUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportData {

    public static final String TYPE_FCM = "fcm";
    public static final String CHECK_STATUS_CLEAN_CACHE = "cause_clean_cache";
    public static final String DOWN_STATUS_DOWNLOAD = "download";
    public static final String DOWN_STATUS_CANCEL = "cancel";
    public static final String DOWN_STATUS_PAUSE = "pause";
    public static final String DOWN_STATUS_RESUME = "resume";
    public static final String DOWN_STATUS_FINISH = "finish";
    public static final String DOWN_STATUS_CAUSE_DOWNLOADING = "cause_downloading";
    public static final String DOWN_STATUS_CAUSE_NOT_ENOUGH = "cause_not_enough";
    public static final String DOWN_STATUS_CAUSE_PARSER_ERROR = "cause_parser_error";
    public static final String DOWN_STATUS_CAUSE_SAME_VERSION = "cause_same_version";
    public static final String DOWN_STATUS_CAUSE_EXCEPTION = "cause_exception";
    public static final String DOWN_STATUS_CAUSE_UNAUTO = "cause_unauto";
    public static final String DOWN_STATUS_CAUSE_FAIL = "cause_fail";
    public static final String DOWN_STATUS_CAUSE_SHA256 = "cause_sha256";
    public static final String DOWN_STATUS_CAUSE_DEVICE_ROOTED = "cause_device_rooted";
    public static final String DOWN_STATUS_CAUSE_INSTALL_FAIL_5 = "cause_install_fail_5";
    public static final String DOWN_STATUS_CAUSE_MODEL_NULL = "cause_model_null";
    public static final String DOWN_STATUS_CAUSE_START_EXCEPTION = "cause_start_exception";
    public static final String DOWN_CAUSE_NET_CHANGE_DOWNLOADING = "cause_net_change_downloading";
    public static final String INSTALL_STATUS_INSTALL = "update";
    public static final String INSTALL_STATUS_DELAY = "delay";
    public static final String INSTALL_STATUS_AUTO = "auto";
    //升级打点
    public static final String INSTALL_STATUS_CAUSE_NOT_FORCE_UPGRADE = "cause_not_force_upgrade";
    public static final String INSTALL_STATUS_CAUSE_EXCEPTION = "cause_exception";
    public static final String INSTALL_STATUS_CAUSE_VERIFY_EXCEPTION = "cause_verify_exception";
    public static final String INSTALL_STATUS_CAUSE_NOT_DLCOMPLETE = "cause_not_dlcomplete";
    public static final String INSTALL_STATUS_CAUSE_INSTALLING = "cause_installing";
    public static final String INSTALL_STATUS_CAUSE_NOT_RIGHT_TIME = "cause_not_right_time";
    public static final String INSTALL_STATUS_CAUSE_UNZIP_FAILED = "cause_unzip_failed";
    public static final String INSTALL_STATUS_CAUSE_GET_ENTRY_FAILED = "cause_get_entry_failed";
    public static final String DOWN_STATUS_CAUSE_NOT_FORCE_REBOOT = "cause_not_force_reboot";
    public static final String REBOOT_STATUS_CAUSE_NOT_DLCOMPLETE = "reboot_cause_not_dlcomplete";
    public static final String REBOOT_STATUS_CAUSE_NOT_RIGHT_TIME = "reboot_cause_not_right_time";
    public static final String REBOOT_STATUS_AUTO = "auto_reboot";
    public static final String INSTALL_CODE_A = "A";      // fotabinder is not running
    private static final String QUERY = "check";
    private static final String DOWNLOAD = "download";
    private static final String UPGRADE = "upgrade";
    private static final String UPGRADE_RESULT = "upgradeResult";
    private static final String INSTALL_CODE_1 = "1";      // install success
    private static final String INSTALL_CODE_2 = "2";      // install fail
    private static final String INSTALL_CODE_3 = "3";      // md5Encode file  is not exist
    private static final String INSTALL_CODE_4 = "4";      // update.zip not found when checking md5Encode
    private static final String INSTALL_CODE_5 = "5";      // fail that get md5Encode file on update.zip
    private static final String INSTALL_CODE_6 = "6";      // md5Encode is not equal
    private static final String INSTALL_CODE_7 = "7";      // fail that rename packages.zip
    private static final String INSTALL_CODE_8 = "8";      // unzip fail
    private static final String INSTALL_CODE_9 = "9";      // rom is damaged
    private static final String INSTALL_CODE_B = "B";      // reboot is not running
    private static final String INSTALL_CODE_C = "C";      // device is being rooted
    private static final String INSTALL_CODE_10 = "10";      // zip get sha256 fail
    private static final String INSTALL_CODE_11 = "11";      // only wifi
    private static final String INSTALL_CODE_12 = "12";      // no sdcard
    private static final String INSTALL_CODE_13 = "13";      // sdcard illegal
    private static final String INSTALL_CODE_14 = "14";      // user cancel

    public static <T> String format(T object) {
        return (object != null) ? JsonUtil.toJson(object) : null;
    }

    private static String getCurrentTime() {
        return new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(new Date());
    }

    private static String getVersion() {
        String version = null;
        VersionBean bean = QueryInfo.getInstance().getVersionInfo();
        if (bean != null)
            version = bean.getVersionName();
        if (TextUtils.isEmpty(version))
            version = DeviceInfoUtil.getInstance().getLocalVersion();
        return version;
    }

    public static void postQuery(Context context, int checkType, int type, RequestResult result) {
        if (result != null) {
            ReportQueryBean bean = new ReportQueryBean();
            bean.setStatus(result.isSuccess() ? 1 : 2);
            bean.setErrCode(String.valueOf(result.getError_code()));
            bean.setReason(result.getError_message());
            bean.setTime(getCurrentTime());
            bean.setVersion(getVersion());
            bean.setCheckType(checkType);
            bean.setApn(DeviceInfoUtil.getInstance().getNetWorkType(context));
            bean.setType(type);
            ReportBaseBean baseBean = new ReportBaseBean();
            baseBean.setAction(QUERY);
            baseBean.setData(bean);
            ReportManager.getInstance().reportDataOrInsertDB(context, QUERY, format(baseBean), false);
        }
    }

    public static void postDownload(Context context, String status, long useTime) {
        ReportDownloadBean bean = new ReportDownloadBean();
        bean.setTime(getCurrentTime());
        bean.setStatus(status);
        bean.setVersion(getVersion());
        bean.setDuration(useTime/1000); //后台以s为单位
        bean.setBackground(DownVersion.getInstance().getDownloadFlag());
        bean.setType(QueryVersion.getInstance().getQueryVersionType());
        bean.setApn(DeviceInfoUtil.getInstance().getNetWorkType(context));
        ReportBaseBean baseBean = new ReportBaseBean();
        baseBean.setAction(DOWNLOAD);
        baseBean.setData(bean);
        ReportManager.getInstance().reportDataOrInsertDB(context, DOWNLOAD, format(baseBean), false);
        if (status.equalsIgnoreCase(ReportData.DOWN_STATUS_CANCEL) || status.equalsIgnoreCase(ReportData.DOWN_STATUS_FINISH) ||
                status.equalsIgnoreCase(ReportData.CHECK_STATUS_CLEAN_CACHE))
            SpManager.clearDownloadRecords();
    }

    /**
     * 记录安装操作，立即，延时安装
     *
     * @param context Context
     * @param status  {@linkplain  ReportData#INSTALL_STATUS_DELAY#INSTALL_STATUS_INSTALL}
     */
    public static void postInstall(Context context, String status) {
        ReportInstallBean bean = new ReportInstallBean();
        bean.setStatus(status);
        bean.setTime(getCurrentTime());
        bean.setNewVersion(getVersion());
        bean.setOldVersion(DeviceInfoUtil.getInstance().getLocalVersion());
        bean.setType(QueryVersion.getInstance().getQueryVersionType());
        bean.setForced(QueryInfo.getInstance().getPolicyValue(QueryInfo.INSTALL_FORCED, Boolean.class) ? 1 : 0);
        ReportBaseBean baseBean = new ReportBaseBean();
        baseBean.setAction(UPGRADE);
        baseBean.setData(bean);
        ReportManager.getInstance().reportDataOrInsertDB(context, UPGRADE, format(baseBean), false);
    }

    /**
     * 记录安装结果，出错信息
     */
    public static void postInstallResult(Context context, boolean isSuccess, int status, String reason) {
        deleteErrorLog(isSuccess);
        ReportInstallResultBean bean = new ReportInstallResultBean();
        bean.setTime(getCurrentTime());
        bean.setOldVersion(getRealLocalVersion(PreferencesUtils.getString(context, Setting.FOTA_ORIGINAL_VERSION, "")));
        if (!TextUtils.isEmpty(reason) && reason.equalsIgnoreCase(Recovery.AB_FLAG))
            bean.setNewVersion(getVersion());
        else
            bean.setNewVersion(isSuccess ? DeviceInfoUtil.getInstance().getLocalVersion()
                    : PreferencesUtils.getString(context, Setting.FOTA_UPDATE_VERSION, DeviceInfoUtil.getInstance().getLocalVersion()));
        bean.setType(PreferencesUtils.getInt(context, Setting.FOTA_UPDATE_TYPE, QueryVersion.QUERY_VERSION_DIFF));
        bean.setStatus(isSuccess ? 1 : 0);
        bean.setErrCode(StatusToErrCode(status));
        bean.setReason(TextUtils.isEmpty(reason) ? "" : reason);
        ReportBaseBean baseBean = new ReportBaseBean();
        baseBean.setAction(UPGRADE_RESULT);
        baseBean.setData(bean);
        ReportManager.getInstance().reportDataOrInsertDB(context, UPGRADE_RESULT, format(baseBean), !isSuccess);
    }

    //升级成功删除errorlog
    private static void deleteErrorLog(boolean isSuccess) {
        if (isSuccess) StorageUtil.deleteErrorLogFile();
    }

    //获取真实的本地版本号，取合法V5版本版本号
    private static String getRealLocalVersion(String local_version) {
        String needCut = "_other";
        String project = DeviceInfoUtil.getInstance().getProject();
        LogUtil.d("local_version = " + local_version + "; project = " + project);
        if (!TextUtils.isEmpty(local_version) && local_version.contains(needCut) &&
                !TextUtils.isEmpty(project) && project.contains(needCut)) {
            String str = project.substring(0, project.lastIndexOf("_"));
            local_version = local_version.substring(str.length() + 1, local_version.lastIndexOf("_"));
        }
        return local_version;
    }

    private static String StatusToErrCode(int status) {
        String code;
        switch (status) {
            case Status.UPDATE_FOTA_FAIL:
                code = INSTALL_CODE_2;
                break;
            case Status.UPDATE_FOTA_SUCCESS:
                code = INSTALL_CODE_1;
                break;
            case Status.UPDATE_STATUS_UNZIP_ERROR:
                code = INSTALL_CODE_4;
                break;
            case Status.UPDATE_STATUS_CKSUM_ERROR:
                code = INSTALL_CODE_6;
                break;
            case Status.UPDATE_STATUS_RUNCHECKERROR:
                code = INSTALL_CODE_8;
                break;
            case Status.UPDATE_STATUS_ROM_DAMAGED:
                code = INSTALL_CODE_9;
                break;
            case Status.UPDATE_FOTA_PKG_MD5_FAIL:
                code = INSTALL_CODE_5;
                break;
            case Status.UPDATE_FOTA_RENAME_FAIL:
                code = INSTALL_CODE_7;
                break;
            case Status.UPDATE_FOTA_NO_MD5:
                code = INSTALL_CODE_3;
                break;
            case Status.UPDATE_FOTA_NO_REBOOT:
                code = INSTALL_CODE_B;
                break;
            case Status.UPDATE_FOTA_EXE_FAIL:
                code = INSTALL_CODE_C;
                break;
            case Status.UPDATE_STATUS_SH256_ERROR:
                code = INSTALL_CODE_10;
                break;
            case Status.UPDATE_STATUS_ONLY_WIFI:
                code = INSTALL_CODE_11;
                break;
            case Status.UPDATE_STATUS_NO_SDCARD:
                code = INSTALL_CODE_12;
                break;
            case Status.UPDATE_STATUS_SDCARD_ILLEGAL:
                code = INSTALL_CODE_13;
                break;
            case Status.UPDATE_STATUS_USER_CANCEL:
                code = INSTALL_CODE_14;
                break;
            default:
                code = status + "";
                break;
        }
        return code;
    }

}

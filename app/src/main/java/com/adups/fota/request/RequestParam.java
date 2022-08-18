package com.adups.fota.request;

import android.content.Context;
import android.provider.Settings;

import com.adups.fota.BuildConfig;
import com.adups.fota.MyApplication;
import com.adups.fota.manager.SpManager;
import com.adups.fota.query.QueryVersion;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.EncryptUtil;
import com.adups.fota.utils.FileUtil;
import com.adups.fota.utils.JsonUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.MidUtil;
import com.adups.fota.utils.PackageUtils;
import com.adups.fota.utils.StorageUtil;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.RequestBody;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestParam {

    public final static String VERSION = "version";
    public final static String FCM_ID = "fcmId";
    public final static String ACTION_TYPE = "action_type";
    public final static String DOWNLOAD_PATH = "download_path";
    public final static String SAVE_PATH = "save_path";
    public final static String STORAGE_SPACE = "storage_space";
    public final static String LOG = "log";
    public final static String KEY = "key";
    public final static String SHA_KEY = "shaKey";
    private final static String IMEI = "imei";
    private final static String IMEI1 = "imei1";
    private final static String IMEI2 = "imei2";
    private final static String STATUS = "status";
    private final static String DEVICE_TYPE = "deviceType";
    private final static String CONNECT_TYPE = "connectType";
    private final static String PLATFORM = "platform";
    private final static String PROJECT = "project";
    private final static String SDK_LEVEL = "sdkLevel";
    private final static String SDK_RELEASE = "sdkRelease";
    private final static String RESOLUTION = "resolution";
    private final static String APP_VERSION = "appVersion";
    private final static String APP_CODE = "appCode";
    private final static String MAC = "mac";
    private final static String ANDROID_ID = "androidId";
    private final static String TIME = "time";
    private final static String AGREE_TYPE = "agreeType";
    private final static String UPGRADE_AGREEMENT = "upgradeAgreement";
    private final static String IS_ACTIVE = "isActive";
    private final static String BATTERY = "battery";

    private final static String DEVICES_INFO_EXT = "devicesinfoExt";
    private final static String SW_FINGERPRINT = "swFingerprint";
    private final static String MID = "mid";
    private final static String IS_NEW_MID = "isNewMid";
    private final static String LOCAL = "local";
    private final static String OPERATOR = "operator";
    private final static String SECOND_OPERATOR = "secondOperator";
    private final static String SPN_ONE = "spn1";
    private final static String SPN_TWO = "spn2";
    private final static String SEND_ID = "sendId";
    private final static String SEND_ID_VALUE = "1075259712158";
    private final static String FOTA_SIGN = "fotaSign";
    private final static String ESN = "esn";
    private final static String RESULT = "result";

    private static Map<String, String> baseParams(Context context) {
        Map<String, String> param = new HashMap<>();
        param.put("device_type", DeviceInfoUtil.getInstance().getDeviceType());
        param.put("connect_type", String.valueOf(DeviceInfoUtil.getInstance().getNetWorkType(context)));
        param.put(PLATFORM, DeviceInfoUtil.getInstance().getPlatform());
        param.put(PROJECT, DeviceInfoUtil.getInstance().getProject());
        param.put(VERSION, DeviceInfoUtil.getInstance().getLocalVersion());
        param.put(DEVICES_INFO_EXT, DeviceInfoUtil.getInstance().getDeviceVersionExt());
        param.put(SW_FINGERPRINT, DeviceInfoUtil.getInstance().getInfoSwFingerprint());
        param.put("sdk_level", DeviceInfoUtil.getInstance().getSdkLevel());
        param.put("sdk_release", DeviceInfoUtil.getInstance().getSdkRelease());
        param.put(RESOLUTION, DeviceInfoUtil.getInstance().getScreenResolution(context));
        param.put(MID, MidUtil.getSyncMid(context));
        param.put(IS_NEW_MID, String.valueOf(MidUtil.isNewMid));
        return param;
    }

    public static Map<String, String> queryParams(Context context, int queryType) {
        Map<String, String> param = baseParams(context);
        param.put(APP_VERSION, PackageUtils.getAppVersionName(context) + BuildConfig.AND_VERSION + "_" + BuildConfig.APK_BUILD_DATE);
        param.put(APP_CODE, String.valueOf(PackageUtils.getAppVersionCode(context)));
        param.put(LOCAL, DeviceInfoUtil.getInstance().getFotaLanguage());
        param.put(OPERATOR, DeviceInfoUtil.getInstance().getOperator(context));
//        param.put(SECOND_OPERATOR, DeviceInfoUtil.getInstance().getMinorOperator());
        param.put(SPN_ONE, DeviceInfoUtil.getInstance().getMainSPN(context));
        param.put(SPN_TWO, "");
        param.put(SEND_ID, SEND_ID_VALUE);
        param.put(FOTA_SIGN, PackageUtils.getSignatureMd5(context, context.getPackageName()));
        param.put(ANDROID_ID, Settings.System.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
        param.put(FCM_ID, SpManager.getFcmId());
        param.put(AGREE_TYPE, String.valueOf(MyApplication.isBootExit()));
        param.put(UPGRADE_AGREEMENT, String.valueOf(SpManager.getUpgradeCheckStatus()));
        param.put(IS_ACTIVE, String.valueOf(queryType == QueryVersion.QUERY_MANUAL));
        if (SpManager.isUserReject()) {
            param.put(IMEI1, EncryptUtil.getMD5(DeviceInfoUtil.getInstance().getDeviceIMEI(context)));
            param.put(IMEI2, EncryptUtil.getMD5(DeviceInfoUtil.getInstance().getMinorIMEI()));
            param.put(MAC, EncryptUtil.getMD5(DeviceInfoUtil.getInstance().getMacAddress(context)));
            param.put(ESN, EncryptUtil.getMD5(DeviceInfoUtil.getInstance().getESN()));
        } else {
            param.put(IMEI1, DeviceInfoUtil.getInstance().getDeviceIMEI(context));
            param.put(IMEI2, DeviceInfoUtil.getInstance().getMinorIMEI());
            param.put(MAC, DeviceInfoUtil.getInstance().getMacAddress(context));
            param.put(ESN, DeviceInfoUtil.getInstance().getESN());
        }
        return param;
    }

    public static Map<String, String> getReportEuParam(Context context) {
        Map<String, String> param = new HashMap<>();
        param.put(IMEI1, DeviceInfoUtil.getInstance().getDeviceIMEI(context));
        param.put(IMEI2, DeviceInfoUtil.getInstance().getMinorIMEI());
        param.put(DEVICE_TYPE, DeviceInfoUtil.getInstance().getDeviceType());
        param.put(CONNECT_TYPE, String.valueOf(DeviceInfoUtil.getInstance().getNetWorkType(context)));
        param.put(PLATFORM, DeviceInfoUtil.getInstance().getPlatform());
        param.put(PROJECT, DeviceInfoUtil.getInstance().getProject());
        param.put(VERSION, DeviceInfoUtil.getInstance().getLocalVersion());
        param.put(SDK_LEVEL, DeviceInfoUtil.getInstance().getSdkLevel());
        param.put(SDK_RELEASE, DeviceInfoUtil.getInstance().getSdkRelease());
        param.put(RESOLUTION, DeviceInfoUtil.getInstance().getScreenResolution(context));
        param.put(APP_VERSION, PackageUtils.getAppVersionName(context) + BuildConfig.AND_VERSION + "_" + BuildConfig.APK_BUILD_DATE);
        param.put(APP_CODE, String.valueOf(PackageUtils.getAppVersionCode(context)));
        param.put(MAC, DeviceInfoUtil.getInstance().getMacAddress(context));
        if (SpManager.isEuNoReport())
            param.put(STATUS, String.valueOf(true));
        return param;
    }

    public static Map<String, String> getFcmReportParam(Context context, Map<String, String> param) {
        param.put(IMEI1, DeviceInfoUtil.getInstance().getDeviceIMEI(context));
        param.put(IMEI2, DeviceInfoUtil.getInstance().getMinorIMEI());
        param.put(PROJECT, DeviceInfoUtil.getInstance().getProject());
        param.put(VERSION, DeviceInfoUtil.getInstance().getLocalVersion());
        param.put(APP_VERSION, PackageUtils.getAppVersionName(context) + BuildConfig.AND_VERSION + "_" + BuildConfig.APK_BUILD_DATE);
        param.put(TIME, SimpleDateFormat.getDateTimeInstance().format(System.currentTimeMillis()));
        LogUtil.d("fcm : " + param.toString());
        return param;
    }

    public static RequestBody getRequestBody(Context context, List<String> list, boolean needLog) {
        Map<String, String> params = new HashMap<>();
        params.put(MID, MidUtil.getSyncMid(context));
        if (SpManager.getRejectStatus()) {
            params.put(IMEI, EncryptUtil.getMD5(DeviceInfoUtil.getInstance().getDeviceIMEI(context)));
            params.put(IMEI2, EncryptUtil.getMD5(DeviceInfoUtil.getInstance().getMinorIMEI()));/*imei2*/
        } else {
            params.put(IMEI, DeviceInfoUtil.getInstance().getDeviceIMEI(context));
            params.put(IMEI2, DeviceInfoUtil.getInstance().getMinorIMEI());/*imei2*/
        }
        params.put("connect_type", String.valueOf(DeviceInfoUtil.getInstance().getNetWorkType(context)));
        params.put(APP_VERSION, PackageUtils.getAppVersionName(context) +
                BuildConfig.AND_VERSION + "_" + BuildConfig.APK_BUILD_DATE);
        params.put(PROJECT, DeviceInfoUtil.getInstance().getProject());
        params.put(VERSION, DeviceInfoUtil.getInstance().getLocalVersion());
        params.put(RESULT, JsonUtil.listToJson(list));
        params.put(TIME, SimpleDateFormat.getDateTimeInstance().format(System.currentTimeMillis()));
        params.put(BATTERY, String.valueOf(DeviceInfoUtil.getInstance().getRealBattery(context)));
        if (LogUtil.logOut || FileUtil.isExistTraceFile(context))
            LogUtil.d("reportParams : " + params.toString());
        MultipartBuilder multipart = new MultipartBuilder();
        for (Map.Entry<String, String> entry : params.entrySet())
            multipart.addFormDataPart(entry.getKey(), entry.getValue());
        File logFile = StorageUtil.getErrorLogFile();
        MediaType mediaType = MediaType.parse("text/plain");
        if (needLog && logFile != null) {
            multipart.addFormDataPart(LOG, "error.log",
                    RequestBody.create(mediaType, logFile));
        } else {
            multipart.addFormDataPart(LOG, "error.log",
                    RequestBody.create(mediaType, ""));
        }
        return multipart.build();
    }

}

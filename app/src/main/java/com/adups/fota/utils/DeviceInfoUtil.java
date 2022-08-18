package com.adups.fota.utils;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.adups.fota.MyApplication;
import com.adups.fota.config.Const;
import com.adups.fota.config.Setting;
import com.adups.fota.install.Install;
import com.adups.fota.query.QueryInfo;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class DeviceInfoUtil {

    private static final String RO_PRODUCT_NAME = "ro.product.name";
    private static final String RO_PRODUCT_BOARD = "ro.product.board";
    private static final String RO_PRODUCT_MODEL = "ro.product.model";
    private static final String RO_PRODUCT_BRAND = "ro.product.brand";
    private static final String INFO_LANGUAGE_23 = "ro.product.locale";
    private static final String RO_PRODUCT_DEVICE = "ro.product.device";
    private static final String RO_PRODUCT_PRODUCT = "ro.product.product";
    private static final String RO_PRODUCT_PLATFORM = "ro.product.platform";
    private static final String INFO_LANGUAGE = "ro.product.locale.language";
    private static final String RO_PRODUCT_MANUFACTURER = "ro.product.manufacturer";

    private static final String INFO_FOTA_OEM = "ro.fota.oem";
    private static final String OTA_CONFIG_POP = "ro.fota.pop";
    private static final String INFO_FOTA_TYPE = "ro.fota.type";
    private static final String OTA_CONFIG_EXIT = "ro.fota.exit";
    private static final String OTA_CONFIG_PATH = "ro.fota.path";
    private static final String OTA_CONFIG_LOCAL_ID = "ro.fota.id";// sn/mac/imei
    private static final String OTA_CONFIG_CYCLE = "ro.fota.cycle";// 1 or 3
    private static final String OTA_CONFIG_SCREEN = "ro.fota.screen";
    private static final String INFO_FOTA_DEVICES = "ro.fota.device";
    private static final String INFO_FOTA_VERSION = "ro.fota.version";
    private static final String INFO_FOTA_NO_RING = "ro.fota.no_ring";
    private static final String OTA_CONFIG_BATTERY = "ro.fota.battery";
    private static final String OTA_CONFIG_DISPLAY = "ro.fota.display";
    private static final String OTA_CONFIG_FMCHECK = "ro.fota.fmcheck";
    private static final String INFO_FOTA_LANGUAGE = "ro.fota.language";
    private static final String INFO_FOTA_PLATFORM = "ro.fota.platform";
    private static final String INFO_FOTA_NO_TOUCH = "ro.fota.no_touch";
    private static final String OTA_CONFIG_ACTIVATE = "ro.fota.activate";
    private static final String INFO_FOTA_AB_UPDATE = "ro.build.ab_update";
    private static final String OTA_CONFIG_WIFI_ONLY = "ro.fota.wifi.only";
    private static final String OTA_CONFIG_WIFI_AUTO = "ro.fota.auto.wifi";
    private static final String OTA_CONFIG_FMSUCCESS = "ro.fota.fmsuccess";
    private static final String OTA_CONFIG_LOCAL_UPDATE = "ro.fota.localupdate";
    private static final String INFO_FOTA_DISPLAY_VERSION = "ro.fota.version.display";
    private static final String INFO_FOTA_SHOW_FINALIZING_PRO = "ro.fota.finalizing.pro";

    private static final String INFO_FOTA_GMS = "ro.fota.gms";
    private static final String INFO_OPTR = "ro.operator.optr";
    private static final String INFO_FOTA_4_4_DEVICEID1 = "gsm.fota_deviceid1"; //4.4imei1
    private static final String INFO_FOTA_4_4_DEVICEID2 = "gsm.fota_deviceid2"; //4.4imei2
    private static final String INFO_FOTA_DEVICEID1 = "persist.sys.fota_deviceid1"; //imei1
    private static final String INFO_FOTA_DEVICEID2 = "persist.sys.fota_deviceid2"; //imei2
    private static final String INFO_FOTA_DEVICEID3 = "persist.sys.fota_deviceid3"; //esn
    private static final String INFO_FOTA_DEVICEID4 = "persist.sys.fota_deviceid4"; //meid

    //定制化
    private static final String RO_FOTA_ROAM = "ro.fota.roam";


    private static DeviceInfoUtil deviceInfoUtil;

    private DeviceInfoUtil() {

    }

    public static DeviceInfoUtil getInstance() {
        if (deviceInfoUtil == null)
            deviceInfoUtil = new DeviceInfoUtil();
        return deviceInfoUtil;
    }

    private String getSystemProperties(String key) {
        return SystemProperties.get(key, "");
    }

    private int getSystemIntProperties(String key) {
        return SystemProperties.getInt(key, 0);
    }

    private String replaceCharacter(String value) {
        return value.replaceAll("_", "\\$");
    }

    public String getPlatform() {
        return getSystemProperties(INFO_FOTA_PLATFORM);
    }

    public String getShowFinalizingPro() {
        return getSystemProperties(INFO_FOTA_SHOW_FINALIZING_PRO);
    }

    public String getSdkLevel() {
        return String.valueOf(Build.VERSION.SDK_INT);
    }

    public String getSdkRelease() {
        return Build.VERSION.RELEASE;
    }

    public String getScreenResolution(Context context) {
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        return screenWidth + "#" + screenHeight;
    }

    public String getModel() {
        return Build.MODEL;
    }

    public String getDeviceType() {
        String type = getSystemProperties(INFO_FOTA_TYPE);
        if (TextUtils.isEmpty(type))
            type = "phone";
        return type;
    }

    public String getProject() {
        if (Const.DEBUG_MODE)
            return Const.DEBUG_MODE_PROJECT_NAME;
        String oem = getSystemProperties(INFO_FOTA_OEM);
        if (TextUtils.isEmpty(oem))
            oem = "unknownoem";
        else
            oem = replaceCharacter(oem);
        String product = getSystemProperties(INFO_FOTA_DEVICES);
        if (TextUtils.isEmpty(product))
            product = "_unknownproduct_" + getFotaLanguage();
        else
            product = "_" + replaceCharacter(product) + "_" + getFotaLanguage();
        String operator = replaceCharacter(getSystemProperties(INFO_OPTR));
        if (operator.equalsIgnoreCase("OP01")) {
            operator = "CMCC";
        } else if (operator.equalsIgnoreCase("OP02")) {
            operator = "CU";
        } else
            operator = "other";
        return oem + product + "_" + operator;
    }

    public String getLocalVersion() {
        if (Const.DEBUG_MODE)
            return Const.DEBUG_MODE_VERSION;
        String version = getSystemProperties(INFO_FOTA_VERSION);
        if (TextUtils.isEmpty(version))
            version = "unknownbuildnumber";
        return version;
    }

    public String getDisplayVersion() {
        return getSystemProperties(INFO_FOTA_DISPLAY_VERSION);
    }

    public String getDeviceVersionExt() {
        String productModel = getSystemProperties(RO_PRODUCT_MODEL);
        String productBrand = getSystemProperties(RO_PRODUCT_BRAND);
        String productName = getSystemProperties(RO_PRODUCT_NAME);
        String productDevice = getSystemProperties(RO_PRODUCT_DEVICE);
        String productBoard = getSystemProperties(RO_PRODUCT_BOARD);
        String productManufacturer = getSystemProperties(RO_PRODUCT_MANUFACTURER);
        String boardPlatform = getSystemProperties(RO_PRODUCT_PLATFORM);
        String buildProduct = getSystemProperties(RO_PRODUCT_PRODUCT);
        return replaceCharacter(productModel) + "_" + replaceCharacter(productBrand) + "_" +
                replaceCharacter(productName) + "_" + replaceCharacter(productDevice) + "_" +
                replaceCharacter(productBoard) + "_" + replaceCharacter(productManufacturer) + "_" +
                replaceCharacter(boardPlatform) + "_" + replaceCharacter(buildProduct);
    }

    public String getInfoSwFingerprint() {
        return Build.FINGERPRINT;
    }

    public String getFotaLanguage() {
        String language = getSystemProperties(INFO_FOTA_LANGUAGE);
        if (TextUtils.isEmpty(language))
            language = getSystemProperties(INFO_LANGUAGE_23);
        if (TextUtils.isEmpty(language))
            language = getSystemProperties(INFO_LANGUAGE);
        if (TextUtils.isEmpty(language))
            language = "en";
        return replaceCharacter(language);
    }

    public boolean isShowLocalUpdate() {
        return getSystemIntProperties(OTA_CONFIG_LOCAL_UPDATE) == 0;
    }

    public boolean isSendSuccessBroadcast() {
        return getSystemIntProperties(OTA_CONFIG_FMSUCCESS) == 1;
    }

    public boolean isSendNewVersionBroadcast() {
        return getSystemIntProperties(OTA_CONFIG_FMCHECK) == 1;
    }

    public boolean isShowBtnPop() {
        return getSystemIntProperties(OTA_CONFIG_POP) == 0;
    }

    public int getBattery() {
        int battery = QueryInfo.getInstance().getPolicyValue(QueryInfo.INSTALL_BATTERY, Integer.class);
        LogUtil.d("battery = " + battery);
        if (battery != Install.INSTALL_DEFAULT_BATTERY && battery != 0) {
            return battery;
        }
        battery = SystemProperties.getInt(OTA_CONFIG_BATTERY, -1);
        int level;
        if (battery > -1 && battery < 100)
            level = battery;
        else
            level = Install.INSTALL_DEFAULT_BATTERY;
        LogUtil.d("limit battery = " + level);
        return level;
    }


    public int getRealBattery(Context context) {
        int battery = 0;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent intent = context.registerReceiver(null, filter);
            if (intent != null)
                battery = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, battery);
        } else {
            BatteryManager manager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (manager != null)
                battery = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        LogUtil.d("real battery = " + battery);
        return battery;
    }

    public boolean isBatteryStatusOk(Context context) {
        int status = 0;
        int battery = getRealBattery(context);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent intent = context.registerReceiver(null, filter);
            if (intent != null)
                status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, status);
        } else {
            BatteryManager manager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (manager != null)
                status = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS);
        }
        return battery >= 30 || status == BatteryManager.BATTERY_STATUS_CHARGING;
    }

    public boolean isScreen() {
        return getSystemIntProperties(OTA_CONFIG_SCREEN) == 1;
    }

    public boolean isShowExit() {
        return getSystemIntProperties(OTA_CONFIG_EXIT) == 0;
    }

    public boolean isAutoWifi() {
//        return getSystemIntProperties(OTA_CONFIG_WIFI_AUTO) == 1;
        return true;
    }

    public boolean isSlientDownload() {
        return true;
    }

    public boolean isOnlyWifi() {
        return getSystemIntProperties(OTA_CONFIG_WIFI_ONLY) == 1;
    }

    public String getCheckCycle() {
        String cycle = getSystemProperties(OTA_CONFIG_CYCLE);
        if (!TextUtils.isEmpty(cycle))
            return cycle;
        return "1";
    }

    public long getQueryActivate() {
        long activate = SystemProperties.getLong(OTA_CONFIG_ACTIVATE, 0L);
        if (activate > 1 && activate < 24 * 60)
            return activate * 60 * 1000;
        return Setting.ELAPSEDREAL_TIME;
    }

    public String getDisplay() {
        if (Const.DEBUG_MODE)
            return "2";
        String display = getSystemProperties(OTA_CONFIG_DISPLAY);
        if (!TextUtils.isEmpty(display))
            return display;
        return "0";
    }

    private String getFotaId() {
        return getSystemProperties(OTA_CONFIG_LOCAL_ID);
    }

    private boolean isStringAllZero(String imei) {
        try {
            for (int i = 0; i < imei.length(); i++) {
                if (Integer.valueOf(String.valueOf(imei.charAt(i))) != 0)
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private long letterToNum(String input) {
        for (byte b : input.getBytes()) {
            return (b - 96);
        }
        return 0;
    }

    private String cast2Number(String str) {
        char[] array = str.toCharArray();
        StringBuilder builder = new StringBuilder();
        for (char c : array) {
            if (c >= 48 && c <= 57)
                builder.append(c);
            else
                builder.append(letterToNum(String.valueOf(c).toLowerCase()));
        }
        return builder.toString();
    }

    private String getImei1() {
        if (Const.DEBUG_MODE)
            return Const.DEBUG_MODE_IMEI;
        String imei = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            TelephonyManager telephonyManager = (TelephonyManager) MyApplication.getAppContext().getSystemService(Context.TELEPHONY_SERVICE);
            imei = telephonyManager.getImei(0);
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            imei = getSystemProperties(INFO_FOTA_4_4_DEVICEID1);
        } else {
            imei = getSystemProperties(INFO_FOTA_DEVICEID1);
        }
        if (!TextUtils.isEmpty(imei)) {
            imei = EncryptUtil.decodeRoValue(imei.replaceAll(",", ""));
            if (!TextUtils.isEmpty(imei) && !isStringAllZero(imei))
                return cast2Number(imei);
        }
        return getMeid();
    }

    private String getImei2() {
        if (Const.DEBUG_MODE)
            return Const.DEBUG_MODE_IMEI;
        String imei = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            TelephonyManager telephonyManager = (TelephonyManager) MyApplication.getAppContext().getSystemService(Context.TELEPHONY_SERVICE);
            imei = telephonyManager.getImei(1);
        } else if (TextUtils.isEmpty(imei) && Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            imei = getSystemProperties(INFO_FOTA_4_4_DEVICEID1);
        } else {
            imei = getSystemProperties(INFO_FOTA_DEVICEID2);
        }
        if (!TextUtils.isEmpty(imei)) {
            imei = EncryptUtil.decodeRoValue(imei.replaceAll(",", ""));
            if (!TextUtils.isEmpty(imei) && !isStringAllZero(imei))
                return cast2Number(imei);
        }
        return getMeid();
    }

    private String getEsn() {
        return EncryptUtil.decodeRoValue(getSystemProperties(INFO_FOTA_DEVICEID3).replaceAll(",", ""));
    }

    private String getMeid() {
        String meid = getSystemProperties(INFO_FOTA_DEVICEID4);
        if (!TextUtils.isEmpty(meid)) {
            meid = EncryptUtil.decodeRoValue(meid.replaceAll(",", ""));
            if (!TextUtils.isEmpty(meid) && !isStringAllZero(meid))
                return cast2Number(meid);
        }
        return "";
    }

    public boolean isSupportAbUpdate() {
        return SystemProperties.getBoolean(INFO_FOTA_AB_UPDATE, false);
    }

    public boolean isNoTouch() {
        return getSystemIntProperties(INFO_FOTA_NO_TOUCH) == 1;
    }

    public boolean isNoRing() {
        return getSystemIntProperties(INFO_FOTA_NO_RING) == 1;
    }

    public String getPath() {
        String path = getSystemProperties(OTA_CONFIG_PATH);
        LogUtil.d("ro.fota.path = " + path);
        return path;
    }

    public boolean isScreenOn(Context context) {
        PowerManager manager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return manager != null && manager.isScreenOn();
    }

    public boolean isGms2() {
        return getSystemIntProperties(INFO_FOTA_GMS) == 2;
    }

    public String getOperator(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager == null ? "" : telephonyManager.getSimOperator();
    }

    public String getMainSPN(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager == null ? "" : telephonyManager.getSimOperatorName();
    }

    private String getIMEI() {
        try {
            String imei1 = getImei1();
            String imei2 = getImei2();
            long imei1Long = Long.parseLong(imei1);
            long imei2Long = Long.parseLong(imei2);
            if (imei1Long >= imei2Long)
                return imei1;
            return imei2;
        } catch (Exception e) {
            return "";
        }
    }

    public String getMinorIMEI() {
        try {
            String imei1 = getImei1();
            String imei2 = getImei2();
            long imei1Long = Long.parseLong(imei1);
            long imei2Long = Long.parseLong(imei2);
            if (imei1Long > imei2Long)
                return imei2;
            else if (imei1Long < imei2Long)
                return imei1;
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    public String getESN() {
        if (Const.DEBUG_MODE)
            return Const.DEBUG_MODE_IMEI;
        String esn = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            esn = Build.getSerial();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            esn = getEsn();
        }
        if (TextUtils.isEmpty(esn))
            esn = getSnFromSystemProperties();
        return TextUtils.isEmpty(esn) ? Build.SERIAL : esn;
    }

    private String getSnFromSystemProperties() {
        String[] properties = {"ro.boot.serialno", "ro.serialno"};
        String value = null;
        for (String property : properties) {
            value = SystemProperties.get(property);
            LogUtil.d("SN VALUE : " + value);
            if (!TextUtils.isEmpty(value)) return value;
        }
        return value;
    }

    public String getDeviceIMEI(Context context) {
        if (Const.DEBUG_MODE)
            return Const.DEBUG_MODE_IMEI;
        String imei;
        String deviceType = getDeviceType();
        if ("pad".equalsIgnoreCase(deviceType)
                || "tv".equalsIgnoreCase(deviceType)
                || "box".equalsIgnoreCase(deviceType)) {
            imei = getESN();
        } else if ("phone".equalsIgnoreCase(deviceType)
                || "pad_phone".equalsIgnoreCase(deviceType)) {
            imei = getIMEI();
        } else {
            imei = getESN();
        }
        String id = getFotaId();
        LogUtil.d("id = " + id);
        if (!TextUtils.isEmpty(id)) {
            if ("sn".equalsIgnoreCase(id)) {
                imei = getESN();
            } else if ("mac".equalsIgnoreCase(id)) {
                imei = getMacAddress(context);
            } else if ("imei".equalsIgnoreCase(id)) {//增加imei定制2016年3月14日10:00:27
                imei = getIMEI();
            }
        }
        return imei;
    }

    public int getNetWorkType(Context context) {
        int type = -1;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return type;
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) return type;
        int nType = networkInfo.getType();
        if (nType == ConnectivityManager.TYPE_MOBILE) {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null)
                type = telephonyManager.getNetworkType();
        } else if (nType == ConnectivityManager.TYPE_WIFI) {
            type = -2;
        }
        return type;
    }

    public String getMacAddress(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = null;
            if (wifiManager != null)
                info = wifiManager.getConnectionInfo();
            if (info != null)
                return info.getMacAddress();
        }
        return getMac();
    }

    private String getMac() {
        String str = "";
        String macSerial = "";
        try {
            Process process = Runtime.getRuntime().exec("cat /sys/class/net/wlan0/address ");
            InputStreamReader ir = new InputStreamReader(process.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            for (; null != str; ) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim();// 去空格
                    break;
                }
            }
            if (TextUtils.isEmpty(macSerial))
                macSerial = loadFileAsString("/sys/class/net/eth0/address").toUpperCase().substring(0, 17);
            if (TextUtils.isEmpty(macSerial)) {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface element = interfaces.nextElement();
                    if (element.getName().equalsIgnoreCase("wlan0")) {
                        byte[] address = element.getHardwareAddress();
                        if (address == null || address.length == 0)
                            continue;
                        StringBuilder buf = new StringBuilder();
                        for (byte b : address)
                            buf.append(String.format("%02X:", b));
                        if (buf.length() > 0)
                            buf.deleteCharAt(buf.length() - 1);
                        macSerial = buf.toString();
                    }
                }
            }
        } catch (Exception ex) {
            LogUtil.d(ex.getMessage());
        }
        if (TextUtils.isEmpty(macSerial))
            macSerial = getNewMac();
        if (TextUtils.isEmpty(macSerial))
            macSerial = "ff:ff:ff:ff:ff:ff";
        return macSerial;
    }

    private static String getNewMac() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return null;
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    private String loadFileAsString(String fileName) throws Exception {
        FileReader reader = new FileReader(fileName);
        String text = loadReaderAsString(reader);
        reader.close();
        return text;
    }

    private String loadReaderAsString(Reader reader) throws Exception {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        int readLength = reader.read(buffer);
        while (readLength >= 0) {
            builder.append(buffer, 0, readLength);
            readLength = reader.read(buffer);
        }
        return builder.toString();
    }

    public boolean isKeyguard(Context context) {
        KeyguardManager mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return mKeyguardManager.inKeyguardRestrictedInputMode();
    }

    public int getCanUse() {
        return SystemSettingUtil.getInt("system_update_control", 1);
    }

    public void setCanUse(int value) {
        try {
            SystemSettingUtil.putInt("system_update_control", value);
        } catch (Exception e) {

        }
    }

    public int getRoamStatus() {
        return SystemSettingUtil.getInt(RO_FOTA_ROAM, 0);
    }
}

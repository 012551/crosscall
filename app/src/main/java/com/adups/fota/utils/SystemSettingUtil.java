package com.adups.fota.utils;

import android.content.ContentResolver;
import android.provider.Settings;
import android.text.TextUtils;

import com.adups.fota.MyApplication;

/**
 * Created by Ruansu on 2018/3/21 0021.
 */

public class SystemSettingUtil {

    private static int TRUE = 1;
    private static int FALSE = -1;

    private static ContentResolver contentResolver;

    private static ContentResolver getContentResolver() {
        if (contentResolver == null)
            contentResolver = MyApplication.getAppContext().getContentResolver();
        return contentResolver;
    }

    public static void putString(String key, String value) {
        Settings.Global.putString(getContentResolver(), key, value);
    }

    public static String getString(String key) {
        return Settings.Global.getString(getContentResolver(), key);
    }

    public static String getString(String key, String deValue) {
        String value = Settings.Global.getString(getContentResolver(), key);
        return TextUtils.isEmpty(value) ? deValue : value;
    }

    public static void putInt(String key, int value) {
        Settings.Global.putInt(getContentResolver(), key, value);
    }

    public static int getInt(String key, int deValue) {
        return Settings.Global.getInt(getContentResolver(), key, deValue);
    }

    public static void putFloat(String key, float value) {
        Settings.Global.putFloat(getContentResolver(), key, value);
    }

    public static float getFloat(String key, float deValue) {
        return Settings.Global.getFloat(getContentResolver(), key, deValue);
    }

    public static void putLong(String key, long value) {
        Settings.Global.putLong(getContentResolver(), key, value);
    }

    public static long getLong(String key, long deValue) {
        return Settings.Global.getLong(getContentResolver(), key, deValue);
    }

    public static void putBoolean(String key, boolean value) {
        if (value)
            putInt(key, TRUE);
        else
            putInt(key, FALSE);
    }

    public static boolean getBoolean(String key, boolean deValue) {
        int value = getInt(key, 0);
        if (value == TRUE) {
            return true;
        } else if (value == FALSE) {
            return false;
        } else {
            return deValue;
        }
    }

}

package com.adups.fota.utils;

import android.content.Context;
import android.text.TextUtils;

import com.adups.fota.config.Setting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MidUtil {

    public static final String LOCAL_MID_FILE = ".srcMid";
    public static final String baidu = String.valueOf(new char[]{'h', 't', 't', 'p', 's', ':', '/', '/', 'w', 'w', 'w', '.', 'b', 'a', 'i', 'd', 'u', '.', 'c', 'o', 'm',});
    public static final String google = String.valueOf(new char[]{'h', 't', 't', 'p', 's', ':', '/', '/', 'w', 'w', 'w', '.', 'g', 'o', 'o', 'g', 'l', 'e', '.', 'c', 'o', 'm',});
    public static final String adups = String.valueOf(new char[]{'h', 't', 't', 'p', 's', ':', '/', '/', 'w', 'w', 'w', '.', 'a', 'd', 'u', 'p', 's', '.', 'c', 'o', 'm',});
    private static final String LOG_TAG = "mid";
    public static int isNewMid = 0;        // 0:  exist mid,  1: create mid    2: tv box, pad exist mid

    public static String getSyncMid(Context context) {
        synchronized (MidUtil.class) {
            String mid = PreferencesUtils.getString(context, Setting.MID, "");
            String sd_mid = getMidFromSdcard(context);
            LogUtil.d("getSyncMid, mid = " + mid + " sd_mid = " + sd_mid);
            if (TextUtils.isEmpty(mid) || "0".equals(mid)) {
                // get from sdcard
                if (TextUtils.isEmpty(sd_mid) || "0".equals(sd_mid)) {
                    mid = "";
                } else {
                    mid = sd_mid;
                }
                PreferencesUtils.putString(context, Setting.MID, mid);
            } else {
                if (!mid.equals(sd_mid)) {
                    // synchronized to sdcard
                    putMidSdcard(context, mid);
                }
            }
            return mid;
        }
    }

    private static String getMidFromSdcard(Context context) {
        return readMID(context);
    }

    private static void putMidSdcard(Context context, String content) {
        writeMID(context, content);
    }


    public static void writeMID(Context context, String mid) {
        LogUtil.d("writeMID, mid = " + mid);
        if (FileUtil.hasWritePermission(context))
            write(StorageUtil.getExternalMidWatchPath() + LOCAL_MID_FILE, mid);
        write(StorageUtil.getExternalMidWatchPackagePath(context) + LOCAL_MID_FILE, mid);
    }

    private static void write(String path, String mid) {
        try {
            File file = new File(path);
            FileOutputStream fos = new FileOutputStream(file);
            byte[] bytes = mid.getBytes();
            fos.write(bytes);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String readMID(Context context) {
        File file = null;
        if (FileUtil.hasReadPermission(context))
            file = new File(StorageUtil.getExternalMidWatchPath() + LOCAL_MID_FILE);
        if (file != null && file.exists()) {
            return read(file);
        } else {
            file = new File(StorageUtil.getExternalMidWatchPackagePath(context) + LOCAL_MID_FILE);
            if (file.exists())
                return read(file);
        }
        return "";
    }

    private static String read(File file) {
        String data = "";
        try {
            FileInputStream fin = new FileInputStream(file);
            int length = fin.available();
            byte[] buffer = new byte[length];
            fin.read(buffer);
            data = new String(buffer, StandardCharsets.UTF_8);
            fin.close();
        } catch (Exception e) {
            LogUtil.d("readMID, Exception:" + e);
            e.printStackTrace();
        }
        return data;
    }

    private static String getSyncNetworkTime(String pingUrl) {
        String date = "";
        try {
            URL url = new URL(pingUrl);
            URLConnection uc = url.openConnection();
            uc.setReadTimeout(1000 * 15);
            uc.setConnectTimeout(1000 * 15);
            uc.setDoInput(true);
            uc.setDoInput(true);
            uc.connect();
            long ld = uc.getDate();
            date = (new SimpleDateFormat("yyyyMMddHHmmss", Locale.US)).format(new Date(ld));
            LogUtil.d("getSyncNetworkTime date = " + date);
        } catch (Exception e) {
            LogUtil.d("getSyncNetworkTime e = " + e.toString());
        }
        return date;
    }

    private static String getNetworkTime(String[] pingUrl) {
        String date = "";
        for (String url : pingUrl) {
            date = getSyncNetworkTime(url);
            if (!TextUtils.isEmpty(date)) {
                break;
            }
        }
        return date;
    }

    private static String generateMidByDate(String date) {
        Random random = new Random();
        String last = "" + (random.nextInt(9000) + 1000);
        String mid = "";
        char[] c = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
        for (int i = 0; i < 2; i++) {
            mid = mid + c[random.nextInt(c.length)] + "";
        }
        mid = date + mid + last;
        LogUtil.d("generateMidByDate, mid = " + mid);
        return mid;
    }

    public static boolean isExistMid(Context context) {
        String dataMid = PreferencesUtils.getString(context, Setting.MID, "");
        String sdMid = getMidFromSdcard(context);
        isNewMid = 0;
        if (TextUtils.isEmpty(dataMid) && TextUtils.isEmpty(sdMid)) {
            isNewMid = 1;
            return false;
        }
        return true;
    }


    /**
     * 获取网络时间创建MID
     *
     * @param context
     * @return
     */
    private static boolean createMid(Context context) {
        String date = "";
        String mid = "";
        int fail_count = PreferencesUtils.getInt(context, Setting.MID_SYN_TIME_FAIL, 0);

        if (fail_count < Setting.MAX_MID_SYN_FAIL_COUNTS) {
            String[] urls = {baidu, google, adups};
            date = getNetworkTime(urls);
            if (TextUtils.isEmpty(date) || isWrong(date)) { //added newMid 20151012
                PreferencesUtils.putInt(context, Setting.MID_SYN_TIME_FAIL, fail_count + 1);
                return false;
            }
        }

        if (date.isEmpty()) {
            date = (new SimpleDateFormat("yyyyMMddHHmmss", Locale.US)).format(new Date());
        }

        if (!date.isEmpty()) {
            mid = generateMidByDate(date);
        }

        if (!mid.isEmpty()) {
            putMidSdcard(context, mid);
            PreferencesUtils.putString(context, Setting.MID, mid);
            PreferencesUtils.putInt(context, Setting.MID_SYN_TIME_FAIL, 0);
            isNewMid = 1;
            return true;
        }
        isNewMid = 0;
        return false;
    }


    public static boolean isWrong(String mid) {

        if (mid.length() < 4) return true;
        Pattern p = Pattern
                .compile("^\\d{4}$");
        Matcher m = p.matcher(mid.substring(0, 4));
        if (m.matches()) {
            int i = Integer.parseInt(mid.substring(0, 4)) - 2016;
            return i < 0;

        }
        return true;
    }


    public static boolean checkMidValid(Context context) {

        return (isExistMid(context) || createMid(context));

    }

    public static void reset(Context context) {
        // whether new MID
        if (MidUtil.isNewMid == 1) {
            File file = null;
            if (FileUtil.hasReadPermission(context))
                file = new File(StorageUtil.getExternalMidWatchPath() + LOCAL_MID_FILE);
            if (file != null && file.exists()) file.delete();
            file = new File(StorageUtil.getExternalMidWatchPackagePath(context) + LOCAL_MID_FILE);
            if (file.exists()) file.delete();
        }
    }

}

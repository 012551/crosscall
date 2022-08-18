package com.adups.fota.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.adups.fota.MyApplication;
import com.adups.fota.bean.VersionBean;
import com.adups.fota.query.QueryInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import androidx.core.app.ActivityCompat;

/**
 * Created by xw on 15-12-15.
 */
public class NetWorkUtil {

    private static final int NETWORK_TYPE_NULL = 100;
    private static final int NETWORK_TYPE_WIFI = 101;
    private static final int NETWORK_TYPE_MOBILE = 102;

    public static boolean isConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager != null) {
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            if (networkInfo != null) {
                if (networkInfo.isConnected()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        NetworkCapabilities capabilities = manager.getNetworkCapabilities(manager.getActiveNetwork());
                        if (capabilities != null)
                            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static void printfNet(final Context context) {
        try {
            if (LogUtil.getSaveLog()) { //catch log if save log turn on
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        final VersionBean model = QueryInfo.getInstance().getVersionInfo();
                        if ((model != null)) {
                            NetWorkUtil.checkNetwork(context);
                            String tempUrl = model.getDeltaUrl().substring(model.getDeltaUrl().indexOf("//") + 2);
                            NetWorkUtil.pingDNS(tempUrl.substring(0, tempUrl.indexOf("/")));
                        }
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean is2GConnected(Context context) {
        LogUtil.d("is2GConnected");
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int connectStatus = telephonyManager.getNetworkType();
        LogUtil.d("connectStatus = " + connectStatus);
        switch (connectStatus) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return true;

            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return false;

            case TelephonyManager.NETWORK_TYPE_LTE:
                return false;

            default:
                return false;
        }
//        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//        //NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
//
//        //zhangzhou 2g start
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
//            Network currentNetwork = connectivityManager.getActiveNetwork();
//            if (currentNetwork == null) {
//                LogUtil.d("当前网络为空");
//                return false;
//            }
//            NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(currentNetwork);
//            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
//                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//                if (ActivityCompat.checkSelfPermission(MyApplication.getAppContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
//                    LogUtil.d("没有读取网络权限");
//                    return false;
//                }
//                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
//                    int type = tm.getDataNetworkType();
//                    LogUtil.d("network type:" + type);
//                    if (type == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
//                        LogUtil.d("网络状态未知");
//                        return false;
//                    }
//                    switch (type) {
//                        case TelephonyManager.NETWORK_TYPE_GPRS: // 联通2g
//                        case TelephonyManager.NETWORK_TYPE_CDMA: // 电信2g
//                        case TelephonyManager.NETWORK_TYPE_EDGE: // 移动2g
//                        case TelephonyManager.NETWORK_TYPE_1xRTT:
//                        case TelephonyManager.NETWORK_TYPE_IDEN:
//                            return true;
//                        default:
//                            return false;
//                    }
//                }
//            }
//        }
    }

    public static boolean isRoaming(Context context) {
        boolean result = false;
        try {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = null;
            if (manager != null) {
                info = manager.getActiveNetworkInfo();
            } else {
                LogUtil.d("manager is null !!!");
            }
            if (info != null) {
                result = info.isRoaming();
            } else {
                LogUtil.d("info is null !!!");
            }

        } catch (NullPointerException e) {
            LogUtil.e("NullPointerException");
        }
        return result;
    }

    public static boolean isWiFiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = null;
        if (connectivityManager != null)
            netInfo = connectivityManager.getActiveNetworkInfo();
        return (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI);
    }

    public static boolean isMobileConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = null;
        if (connectivityManager != null)
            netInfo = connectivityManager.getActiveNetworkInfo();
        return (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_MOBILE);
    }

    private static int getNetWorkType(Context context) {
        if (isWiFiConnected(context)) {
            return NETWORK_TYPE_WIFI;
        }
        if (isMobileConnected(context)) {
            return NETWORK_TYPE_MOBILE;
        }
        return NETWORK_TYPE_NULL;
    }

    private static void checkNetwork(Context context) {
        int type = NetWorkUtil.getNetWorkType(context);
        if (type == NetWorkUtil.NETWORK_TYPE_MOBILE) {
            LogUtil.d("Network type : mobile. ip = " + getIpAddress());
        } else if (type == NetWorkUtil.NETWORK_TYPE_WIFI) {
            LogUtil.d("Network type : wifi. ip = " + getLocalIpAddress(context));
        } else {
            LogUtil.d("Network type : other");
        }
    }

    private static String getIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getLocalIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = null;
        if (wifiManager != null)
            wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = 0;
        if (wifiInfo != null)
            ipAddress = wifiInfo.getIpAddress();
        @SuppressLint("DefaultLocale") String ipv4 = String.format("%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        LogUtil.d("getLocalIpAddress() mobile net ipv4 = " + ipv4);
        return ipv4;
    }

    private static void pingDNS(final String domain) {
        LogUtil.d("get() domain = " + domain);
        Process process = null;
        BufferedReader bufferedReader = null;
        try {
            process = Runtime.getRuntime().exec("ping " + domain);
            bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            StringBuilder log = new StringBuilder();
            String line;
            int i = 2;
            while (i-- > 0 && (line = bufferedReader.readLine()) != null) {
                LogUtil.d("get() line = " + line);
                log.append(line);
            }
            process.getInputStream().close();
            LogUtil.d("get() result = " + log.toString());
        } catch (IOException e) {
            LogUtil.d("get() e= " + e.getMessage());
        } finally {
            try {
                if (process != null) {
                    process.destroy();
                }
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (Exception ignored) {

            }
        }
    }
}


package com.adups.fota;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.adups.fota.callback.DialCallback;
import com.adups.fota.config.Const;
import com.adups.fota.config.ServerApi;
import com.adups.fota.config.Setting;
import com.adups.fota.manager.SpManager;
import com.adups.fota.receiver.MyReceiver;
import com.adups.fota.system.UpdateVerify;
import com.adups.fota.utils.CrashHandler;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.NetWorkUtil;
import com.adups.fota.utils.OkHttpUtil;
import com.adups.fota.utils.PackageUtils;
import com.adups.fota.utils.PreferencesUtils;
import com.adups.fota.utils.StorageUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

public class MyApplication extends Application {

    protected static Context mContext;
    private static boolean isCalling = false;
    private static DialCallback dialCallback;
    private PhoneStateListener phoneStateListener = new PhoneStateListener() {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            if (NetWorkUtil.isWiFiConnected(mContext)) {
                LogUtil.d("onCallStateChanged but WiFiConnected ,ignored");
                return;
            }
            switch (state) {
                case TelephonyManager.CALL_STATE_OFFHOOK:
                case TelephonyManager.CALL_STATE_RINGING:
                    isCalling = true;
                    if (dialCallback != null) dialCallback.onPhoneCalling();
                    break;
                default:
                    isCalling = false;
                    if (dialCallback != null) dialCallback.onPhoneOff();
                    break;
            }
            LogUtil.d("phone state : " + state + " , isCalling : " + isCalling);
        }

    };

    public static boolean isCalling() {
        return isCalling;
    }

    public static void setOnPhoneCallingListener(DialCallback callback) {
        dialCallback = callback;
    }

    public static void removePhoneCallingListener() {
        dialCallback = null;
    }

    public static Context getAppContext() {
        return mContext;
    }

    public static boolean isBootExit() {
        PackageManager packageManager = mContext.getPackageManager();
        ApplicationInfo info = null;
        try {
            info = packageManager.getApplicationInfo(Const.FOTA_BOOT_PACKAGE_NAME, 0);
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        }
        return info != null;
    }

    public static void getGmsId() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    FirebaseApp.initializeApp(mContext);
                    FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                        @Override
                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                SpManager.setFcmId(task.getResult().getToken());
                            }
                        }
                    });
                } catch (Exception e) {
                    LogUtil.d(e.getMessage());
                }
            }
        }.start();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this.getApplicationContext();
        LogUtil.d("appVersion = " + PackageUtils.getAppVersionName(this) + BuildConfig.AND_VERSION + "_" + BuildConfig.APK_BUILD_DATE);
        try {
            getGmsId();
            updateVerify();
            initBroadcast();
            OkHttpUtil.resetDNS();
            initTelephonyManager();
            StorageUtil.init(this);
            CrashHandler.getInstance().init(this);
            FirebaseMessaging.getInstance().setAutoInitEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String initUrl = PreferencesUtils.getString(this, Setting.QUERY_URL);
        if (!TextUtils.isEmpty(initUrl) && !initUrl.equals(ServerApi.SERVER_DOMAIN)) {
            PreferencesUtils.putString(mContext, Setting.QUERY_URL, ServerApi.SERVER_DOMAIN);
        }
        SpManager.setConnectNetValue();
    }

    private void initBroadcast() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        intentFilter.addAction(Intent.ACTION_DATE_CHANGED);
        intentFilter.addAction(Const.DOWNLOAD_STOP_BROADCAST);
        intentFilter.addAction(Const.INSTALL_NOW_BROADCAST);
        intentFilter.addAction(Const.SEND_NEW_VERSION_BROADCAST);
        intentFilter.addAction(Const.DOWNLOAD_NOW_BROADCAST);
        MyReceiver myReceiver = new MyReceiver();
        registerReceiver(myReceiver, intentFilter);
    }

    private void initTelephonyManager() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null)
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void updateVerify() {
        new UpdateVerify().startVerify(this);
    }

}

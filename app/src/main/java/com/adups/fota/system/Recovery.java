package com.adups.fota.system;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.os.RecoverySystem;
import android.os.UpdateEngine;
import android.os.UpdateEngineCallback;
import android.text.TextUtils;

import com.adups.fota.bean.EventMessage;
import com.adups.fota.config.Const;
import com.adups.fota.config.Event;
import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.install.Install;
import com.adups.fota.manager.NoticeManager;
import com.adups.fota.manager.NotificationManager;
import com.adups.fota.query.QueryInfo;
import com.adups.fota.report.ReportData;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.FileUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PreferencesUtils;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Recovery {

    public static final int AB_INSTALLING = 600;
    public static final int AB_INSTALL_RESULT = 601;
    public static final int AB_BATTERY_LOW = 617;
    public static final int AB_GET_PARAMS_FAIL = 618;
    public static final int AB_CONNECT_REBOOT_FAIL = 619;
    public static final int AB_VERIFYING = 602;
    public static final int AB_PACKAGE_NOT_EXIST = 620;
    public static final int AB_REBOOT_IS_LOW = 621;
    public static final String AB_FLAG = "ab";

    private static final int UPDATE_ERROR = 0;
    private static final int UPDATE_SUCCESS = 1;
    private static final String UPDATE_SYSTEM = "";//重启原因
    private volatile static Recovery recovery = null;
    private boolean isApplied = false;
    private long lastAbTime;
    private UpdateEngine mUpdateEngine = null;
    private Context mContext;
    private String mPkgPath;
    private PowerManager.WakeLock mWakeLock;

    private Recovery() {
    }

    public static Recovery getInstance() {
        if (recovery == null) {
            synchronized (Recovery.class) {
                if (recovery == null) {
                    recovery = new Recovery();
                }
            }
        }
        return recovery;
    }

    public int execute(Context context, String path) {
        try {
            if (TextUtils.isEmpty(path)) {
                LogUtil.d("package path empty");
                return UPDATE_ERROR;
            }
            LogUtil.d("recovery path : " + path);
            File file = new File(path);
            if (file.exists()) {
                LogUtil.d("go to update system");
                RecoverySystem.installPackage(context, file);
                LogUtil.d("still running after update system");
            } else {
                LogUtil.d("package file not exist");
            }
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        }
        return UPDATE_ERROR;
    }

    private void modifyFilePermission(String path) {
        try {
            String command = "chmod 666 " + path;
            LogUtil.d("command = " + command);
            Process process = Runtime.getRuntime().exec(command);
            if (process.waitFor() == 0)
                LogUtil.d("chmod success!");
            else
                LogUtil.d("chmod fail!");
        } catch (Exception e) {
            LogUtil.d("chmod fail!");
        }
    }

    public void executeAb(final Context context, final String path) {
        LogUtil.d("recovery ab path : " + path);
        modifyFilePermission(path);
        mContext = context;
        mPkgPath = path;
        LogUtil.d("engine = " + mUpdateEngine + "; Status = " + Status.getVersionStatus(mContext));
        if (mUpdateEngine == null) {
            isApplied = false;
            mUpdateEngine = new UpdateEngine();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mUpdateEngine.bind(new UpdateEngineCallback() {
                        @Override
                        public void onStatusUpdate(int status, float percent) {
                            LogUtil.d("update status,percent = " + percent + ",status=" + status +
                                    ",show=" + DeviceInfoUtil.getInstance().getShowFinalizingPro());
                            switch (status) {
                                case UpdateEngine.UpdateStatusConstants.IDLE://只调用了bind，还未调用applyPayload方法，并且可以调用applyPayload，开始调用applyPayload
                                    acquireWakeLock();
                                    if (!isApplied)
                                        abUpdate(mContext, mPkgPath, mUpdateEngine);//调起升级
                                    break;
                                case UpdateEngine.UpdateStatusConstants.DOWNLOADING:
                                    EventBus.getDefault().post(new EventMessage(Event.INSTALL, status,
                                            Const.SHOWFINALIZINGPRO.equalsIgnoreCase(DeviceInfoUtil.getInstance().getShowFinalizingPro()) ?
                                                    Double.valueOf(Math.floor(percent * 50)).longValue() : Double.valueOf(Math.floor(percent * 100)).longValue(),
                                            AB_INSTALLING, AB_FLAG));
                                    break;
                                case UpdateEngine.UpdateStatusConstants.FINALIZING:
                                    if (Const.SHOWFINALIZINGPRO.equalsIgnoreCase(DeviceInfoUtil.getInstance().getShowFinalizingPro()))
                                        EventBus.getDefault().post(new EventMessage(Event.INSTALL, status,
                                                Double.valueOf(Math.floor(percent * 50)).longValue() + 50,
                                                AB_INSTALLING, AB_FLAG));
                                    break;
                                case UpdateEngine.UpdateStatusConstants.UPDATED_NEED_REBOOT:
                                    abSuccess(mContext);
                                    releaseWakeLock();
                                    break;
                            }
                        }

                        @Override
                        public void onPayloadApplicationComplete(int errorCode) {
                            LogUtil.d("onPayloadApplicationComplete,errorCode = " + errorCode);
                            if (errorCode == UpdateEngine.ErrorCodeConstants.SUCCESS) {
                                ReportData.postInstallResult(mContext, true,
                                        errorCode, AB_FLAG); //上报系统安装成功
                                abSuccess(mContext);
                            } else {
                                abFail(mContext, errorCode, true);
                            }
                        }
                    });
                }
            }).start();
        } else if (Status.getVersionStatus(mContext) < Status.STATE_AB_UPDATING) {
            abUpdate(mContext, mPkgPath, mUpdateEngine);//调起升级
        }

///**

//**/
    }
    //ab升级
    private void abUpdate(final Context context, final String path, final UpdateEngine updateEngine) {
        LogUtil.d("abUpdate enter");
        if (!TextUtils.isEmpty(path) && new File(path).exists()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<String> valueList = FileUtil.getHeaderValue(path);
                        String[] headerKeyValuePairs = new String[valueList.size()];
                        String[] headerKeyValuePairs2 = valueList.toArray(headerKeyValuePairs);
                        LogUtil.d("headerKeyValuePairs = " + Arrays.toString(headerKeyValuePairs2) +
                                ",headerKeyValuePairs length = " + headerKeyValuePairs.length);
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && headerKeyValuePairs2.length <= 1) {
                            LogUtil.d("length <= 1");
                            NoticeManager.updateShortcut(context, 1);
                            QueryInfo.getInstance().reset(context);
                            abFail(context, AB_GET_PARAMS_FAIL, false);
                            return;
                        }
//                        lastAbTime = System.currentTimeMillis();
                        Status.setVersionStatus(context, Status.STATE_AB_UPDATING);
                        EventBus.getDefault().post(new EventMessage(Event.INSTALL, 99, 0,
                                AB_VERIFYING, AB_FLAG));
                        String recoveryPath = "file://" + path;
                        long offset = FileUtil.getOffset(path);
                        long size = Long.parseLong(headerKeyValuePairs2[1].replace("FILE_SIZE=", ""));
                        isApplied = true;
                        updateEngine.applyPayload(recoveryPath, offset, size, headerKeyValuePairs2);
                    } catch (Exception e) {
                        LogUtil.d(e.getMessage());
                    }
                }
            }).start();
        } else {
            LogUtil.d("executeAb,path is not valid");
            abFail(context, AB_PACKAGE_NOT_EXIST, false);
        }
    }

    public void reboot(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            LogUtil.d("reboot,update system");
            powerManager.reboot(UPDATE_SYSTEM);
        }
    }

    private void abFail(Context context, int errCode, boolean complete) {
        LogUtil.d("abFail,errCode = " + errCode + ",isComplete = " + complete);
        if (errCode == 20) { //adb root，adb remount会导致system分区改变，ab升级安装时报20
            PreferencesUtils.putBoolean(context, Setting.FOTA_ROM_DAMAGED, true);
        }
        Status.setVersionStatus(context, Status.STATE_QUERY_NEW_VERSION);
        QueryInfo.getInstance().reset(context);
        NoticeManager.updateShortcut(context, 1);
        ReportData.postInstallResult(context, false, errCode, AB_FLAG);
        if (complete) {
            EventBus.getDefault().post(new EventMessage(Event.INSTALL, errCode, 0,
                    AB_INSTALL_RESULT, AB_FLAG));
        } else {
            EventBus.getDefault().post(new EventMessage(Event.INSTALL, 100, 0, errCode, AB_FLAG));
        }

        if (mUpdateEngine != null) {
            mUpdateEngine.resetStatus();
        }
    }

    private void abSuccess(Context context) {
        LogUtil.d("abSuccess enter");
        Status.setVersionStatus(context, Status.STATE_REBOOT);
        EventBus.getDefault().post(new EventMessage(Event.INSTALL, UpdateEngine.ErrorCodeConstants.SUCCESS,
                0, AB_INSTALL_RESULT, AB_FLAG)); //通知主界面
        PreferencesUtils.putBoolean(context, Setting.FOTA_ENTER_RECOVERY, true);
        Install.force_reboot(context);
        int canUse = DeviceInfoUtil.getInstance().getCanUse();
        LogUtil.d("canUse = " + canUse);
        if(canUse == 1){
            NotificationManager.getInstance().showAbInstallSuccess(context, false);
        }
        if (PreferencesUtils.getBoolean(context, Setting.FOTA_UPDATE_LOCAL, false)
                || (QueryInfo.getInstance().getPolicyValue(QueryInfo.INSTALL_NOTICE_TYPE, Integer.class) != 3)) {
            LogUtil.d("show reboot notify");
            NotificationManager.getInstance().cancel(context, NotificationManager.NOTIFY_DL_COMPLETED);
//            NotificationManager.getInstance().showAbInstallSuccess(context, false);
        }
    }

    /**
     *获取电源锁，保持该服务
     *在屏幕熄灭时仍然获取CPU时，保持运行
     */
    public void acquireWakeLock() {
        LogUtil.d("acquireWakeLock = " + mWakeLock);
        if (null == mWakeLock) {
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "fota:Recovery");
            if (null != mWakeLock) {
                LogUtil.d("acquireWakeLock; mWakeLock = " + mWakeLock);
                mWakeLock.acquire();
            } else {
                LogUtil.e("mWakeLock is null!");
            }

        } else {
            LogUtil.d("acquireWakeLock; mWakeLock = " + mWakeLock);
            mWakeLock.acquire();
        }
    }

    /***
     * 释放设备电源锁
     */
    public void releaseWakeLock() {
        LogUtil.d("releaseWakeLock = " + mWakeLock);
        if (null != mWakeLock) {
            mWakeLock.release();
            mWakeLock = null;
        } else {
            LogUtil.e("mWakeLock is null!");
        }
    }


}
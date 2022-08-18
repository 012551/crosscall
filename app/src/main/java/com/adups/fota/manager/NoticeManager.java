package com.adups.fota.manager;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.database.Cursor;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.content.pm.ShortcutManagerCompat;

import com.adups.fota.GoogleOtaClient;
import com.adups.fota.MyApplication;
import com.adups.fota.R;
import com.adups.fota.activity.PopupActivity;
import com.adups.fota.activity.ShortCutActivity;
import com.adups.fota.config.Const;
import com.adups.fota.config.Status;
import com.adups.fota.config.TaskID;
import com.adups.fota.install.Install;
import com.adups.fota.query.QueryInfo;
import com.adups.fota.receiver.MyReceiver;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PreferencesUtils;

import java.util.List;

/**
 * send notice to user when query new version ,download completed ,no install
 * Created by xw on 16-1-18.
 */
public class NoticeManager {

    /**
     * 检测到新版本，提示用户
     *
     * @param context
     */
    public static void query(Context context) {
        boolean isScreenOn = DeviceInfoUtil.getInstance().isScreenOn(context);
        boolean isTop = isTopActivity("com.adups.fota");
        int query_count = PreferencesUtils.getInt(context, "query_count", 0);
        int noticestatus = PreferencesUtils.getInt(context, "downloadandinstall", 0);
        boolean hasBroadcast = PreferencesUtils.getBoolean(context, "download_now_broadcast");
        LogUtil.d("hasBroadcast = " + hasBroadcast + "; isScreenOn = " + isScreenOn + "; isTop = " + isTop + "; noticestatus = " + noticestatus +"; query_count = " + query_count);
        if (isScreenOn && !isTop && query_count != 1 && !hasBroadcast) {
            LogUtil.d("show new version popwindow");
            PreferencesUtils.putInt(context, "query_count", 1);
            Intent intent = new Intent(context, PopupActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(TaskID.STATUS, Status.STATE_NEW_VERSION_READY);
            context.startActivity(intent);
        } else {
            if (noticestatus != 1) {
                LogUtil.d("show new version notify");
                NotificationManager.getInstance().showNewVersion(context, false);
            }
        }

//        int notice_type = QueryInfo.getInstance().getPolicyValue(QueryInfo.QUERY_NOTICE_TYPE, Integer.class);
//        boolean notice_resident = QueryInfo.getInstance().getPolicyValue(QueryInfo.QUERY_NOTICE_RESIDENT, Boolean.class);
//        int status = Status.getVersionStatus(context);
//        LogUtil.d("query notice : notice_type = " + notice_type + "; notice_resident = " + notice_resident);
//        if (Const.DEBUG_MODE)
//            notice_type = 2;
//        switch (notice_type) {
//            case 0:// notification
//                if (status == Status.STATE_DL_PKG_COMPLETE) {
//                    NotificationManager.getInstance().showDownloadCompleted(context, notice_resident);
//                    forwardPopWindow(context, Status.STATE_DL_PKG_COMPLETE);
//                } else {
//                    NotificationManager.getInstance().showNewVersion(context, notice_resident);
//                }
//                break;
//            case 1:// shortcut
//                updateShortcut(context, 2);
//                break;
//            case 2:// popupWindow
//                forwardPopWindow(context, Status.STATE_NEW_VERSION_READY);
//                break;
//        }
    }

    public static void forwardPopWindow(Context context, int status) {
        if (SpManager.isUpgradeLaterOverTimes()) {//弹窗稍后安装提示超过三次
            LogUtil.d("already remind over three times");
            Install.force_update(context.getApplicationContext());
            return;
        }
        if (!ActivityManager.getManager().isMainActivityTop()) {
            LogUtil.d("forward PopupActivity");
            ActivityManager.getManager().finishMainActivity();
            if (ActivityManager.getManager().isActivityTop(PopupActivity.class)) return;
            Intent intent = new Intent(context, PopupActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(TaskID.STATUS, status);
            context.startActivity(intent);
        } else {
            LogUtil.d("GoogleOtaClient on the top");
        }
    }

    /**
     * 检测到同一版本，按下载提示配置通知用户
     *
     * @param context
     */
    public static void download(Context context) {
        boolean force_install = QueryInfo.getInstance().getPolicyValue(QueryInfo.INSTALL_FORCED, Boolean.class);
        int download_install_later = PreferencesUtils.getInt(context, "download_install_later", 0);
        LogUtil.d("force_install = " + force_install);
        LogUtil.d("download_install_later = " + download_install_later);
        if(!force_install || download_install_later == 0){
            NotificationManager.getInstance().showDownloadCompleted(context, false);
        }
//        if (Status.getVersionStatus(context) == Status.STATE_DL_PKG_COMPLETE) {
//            int notice_type = QueryInfo.getInstance().getPolicyValue(QueryInfo.INSTALL_NOTICE_TYPE, Integer.class);
//            boolean notice_resident = QueryInfo.getInstance().getPolicyValue(QueryInfo.INSTALL_NOTICE_RESIDENT, Boolean.class);
//            LogUtil.d("download notice : notice_type = "
//                    + notice_type + "||notice_resident = " + notice_resident);
//            if (notice_type == 0) {// notification,桌面图标功能去除
//                NotificationManager.getInstance().showDownloadCompleted(context, notice_resident);
//            }
//            forwardPopWindow(context, Status.STATE_DL_PKG_COMPLETE);
//        }
    }

    public static void downloadPause(Context context) {
        NotificationManager.getInstance().cancel(context,NotificationManager.NOTIFY_CONTINUE_RESET);
        NotificationManager.getInstance().showDownloadPause(context,false);
    }


    public static void downloadCancel(Context context) {
        NotificationManager.getInstance().cancel(context, NotificationManager.NOTIFY_DOWNLOADING);
    }


    public static void click(Context context, int status) {
        if (status == Status.STATE_NEW_VERSION_READY) {
            Intent intent = new Intent(context, GoogleOtaClient.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    /**
     * 删除程序的快捷方式
     */
    public static void delShortcut(Context context) {

    }

    private static boolean hasShortcut(Context context) {
        return true;
    }

    /**
     * 创建快捷方式
     *
     * @param context
     **/
    public static void delAndAddShortcut(Context context) {

    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private static ShortcutInfo getShortcutInfo(Context context, Intent intent) {
        ShortcutInfo.Builder builder = new ShortcutInfo.Builder(context, context.getString(R.string.app_name))
                .setShortLabel(context.getString(R.string.app_name))
                .setLongLabel(context.getString(R.string.app_name))
                .setIcon(Icon.createWithResource(context, R.mipmap.newversion_shortcut));
        if (intent != null) builder.setIntent(intent);
        return builder.build();
    }

    /**
     * 更新快捷方式
     *
     * @param context
     * @param flag,   1--del Shortcut, 2--del the add Shortcut
     **/
    public static void updateShortcut(Context context, int flag) {
        LogUtil.d("updateShortcut,flag=" + flag);
        //快捷方式的图标, 1--删除图标 2--删除再创建图标
        switch (flag) {
            case 1:
                delShortcut(context);
                break;
            case 2:
                delAndAddShortcut(context);
                break;
        }
    }

    private static boolean isFotaTop() {
        boolean isTop = false;
        android.app.ActivityManager am = (android.app.ActivityManager) MyApplication.getAppContext().getSystemService(Context.ACTIVITY_SERVICE);
        try {
            if (am != null) {
                List<android.app.ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
                if (tasks != null && !tasks.isEmpty()) {
                    android.app.ActivityManager.RunningTaskInfo taskInfo = tasks.get(0);
                    String packageName = taskInfo.topActivity.getPackageName();
                    String className = taskInfo.topActivity.getClassName();
                    LogUtil.d("packageName = " + packageName + ",className=" + className);
                    if (packageName.equals("com.adups.fota")) {
                        isTop = true;
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        }
        return isTop;
    }

    public static void install(Context context) {
        int notice_type = QueryInfo.getInstance().getPolicyValue(QueryInfo.INSTALL_NOTICE_TYPE, Integer.class);
        if (notice_type == 1) {
            // shortcut
            updateShortcut(context, 1);
        }
    }

    /**
     * 返回当前的应用是否处于前台显示状态
     *
     * @param $packageName
     * @return
     */
    public static boolean isTopActivity(String $packageName) {
        boolean isTop = false;
        android.app.ActivityManager am = (android.app.ActivityManager) MyApplication.getAppContext().getSystemService(Context.ACTIVITY_SERVICE);
        try {
            if (am != null) {
                List<android.app.ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
                if (tasks != null && !tasks.isEmpty()) {
                    android.app.ActivityManager.RunningTaskInfo taskInfo = tasks.get(0);
                    String packageName = taskInfo.topActivity.getPackageName();
                    String className = taskInfo.topActivity.getClassName();
                    LogUtil.d("packageName = " + packageName + ",className=" + className);
                    if (packageName.equals("com.adups.fota")) {
                        isTop = true;
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        }
        return isTop;
    }

}

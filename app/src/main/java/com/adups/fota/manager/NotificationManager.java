package com.adups.fota.manager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.adups.fota.GoogleOtaClient;
import com.adups.fota.MyApplication;
import com.adups.fota.R;
import com.adups.fota.TimePickerWindow;
import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.config.TaskID;
import com.adups.fota.install.Install;
import com.adups.fota.query.QueryInfo;
import com.adups.fota.receiver.MyReceiver;
import com.adups.fota.service.CustomActionService;
import com.adups.fota.service.FcmService;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PreferencesUtils;

/**
 * Created by xw on 15-12-25.
 */
public class NotificationManager {

    public static final int NOTIFY_NEW_VERSION = R.string.app_name + 1;
    public static final int NOTIFY_DOWNLOADING = R.string.app_name + 1;
    public static final int NOTIFY_DOWNLOAD_PAUSE = R.string.app_name + 1;
    public static final int NOTIFY_AB_INSTALL_SUCCESS = R.string.app_name + 1;
    public static final int NOTIFY_DL_COMPLETED = 105;
    public static final int NOTIFY_CONTINUE_RESET = 106;
    public static final String NOTIFICATION_CHANNEL = "channel_fota";
    private static NotificationManager mInstance = null;

    public static NotificationManager getInstance() {
        if (mInstance == null) {
            synchronized (NotificationManager.class) {
                if (mInstance == null) {
                    mInstance = new NotificationManager();
                }
            }
        }
        return mInstance;
    }

    public void showNewVersion(Context context, boolean isOngoing) {
        Intent intent = new Intent(context, GoogleOtaClient.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, Status.STATE_NEW_VERSION_READY,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);
        PreferencesUtils.putInt(context, "query_count", 1);
        showNotification(context, NOTIFY_NEW_VERSION, null, context.getString(R.string.notification_content_newversion), pendingIntent, isOngoing);
    }


    public void showDownloadProgress(Context context, String title, String content) {
//        if (!isShowProgressNotify()) {
//            return;
//        }
        LogUtil.d("enter");
        Intent intent = new Intent(context, GoogleOtaClient.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, Status.STATE_DOWNLOADING,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);
        showNotification(context, NOTIFY_DOWNLOADING, null, content, pendingIntent, false);
    }

    public void showDisable(Context context, String title, String content) {
        if (!isShowProgressNotify()) {
            return;
        }
        Intent intent = new Intent(context, GoogleOtaClient.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, Status.STATE_DOWNLOADING,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);
        showNotification(context, NOTIFY_DOWNLOADING, null, content, pendingIntent, false);
    }

    public void showDownloadPause(Context context, boolean isOngoing) {
//        if (!isShowProgressNotify()) {
//            return;
//        }
        Intent intent = new Intent(context, GoogleOtaClient.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, Status.STATE_PAUSE_DOWNLOAD,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);
        showNotification(context, NOTIFY_DOWNLOAD_PAUSE, null, context.getString(R.string.ota_notify_download_pause_content), pendingIntent, isOngoing);
    }

    public void showDownloadCompleted(Context context, boolean isOngoing) {
        if (Install.isSupportAbUpdate() && !Install.isBatteryAbility(MyApplication.getAppContext(), Install.INSTALL_DEFAULT_BATTERY)) {
            CustomActionService.enqueueWork(context, TaskID.TASK_BATTERY_MONITOR);
            return;
        }
        Intent intent = new Intent(context, GoogleOtaClient.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(Setting.INTENT_PARAM_NOTIFY_ID, NOTIFY_DL_COMPLETED);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, Status.STATE_DL_PKG_COMPLETE,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);

//        Intent actionIntent = new Intent(context, GoogleOtaClient.class);//通知栏按钮点击
//        actionIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        actionIntent.putExtra(Setting.INTENT_PARAM_NOTIFY_ID, NOTIFY_DL_COMPLETED);
//        actionIntent.putExtra(Setting.INTENT_PARAM_FLAG, Setting.INTENT_PARAM_FLAG_INSTALL);
//        PendingIntent actionpendingIntent = PendingIntent.getActivity(context, Status.STATE_DL_PKG_COMPLETE_ACTION,
//                actionIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        showNotification(context, NOTIFY_DL_COMPLETED, null, context.getString(R.string.download_completed_text),
                pendingIntent, isOngoing);
    }

    public void showAbInstallSuccess(Context context, boolean isOngoing) {
        int status = Status.getVersionStatus(context);
        if (status != Status.STATE_REBOOT) {
            LogUtil.d("status: " + status + "; do not need Reboot");
            return;
        }
        Intent intent = new Intent(context, GoogleOtaClient.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, Status.STATE_REBOOT,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);
        showNotification(context, NOTIFY_AB_INSTALL_SUCCESS, null, context.getString(R.string.updated_need_reboot), pendingIntent, isOngoing);
    }

    public void showDownloadAndInstall(Context context, boolean isOngoing, int taskId) {
        android.app.NotificationManager manager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        LogUtil.d("taskId = " + taskId);
        PreferencesUtils.putInt(context, "downloadandinstall", 1);
        Intent intent = new Intent(context, GoogleOtaClient.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                intent, 0);

        Intent continueIntent = new Intent(context, GoogleOtaClient.class);//通知栏continue按钮
        continueIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        continueIntent.putExtra(Setting.INTENT_PARAM_NOTIFY_ID, NOTIFY_CONTINUE_RESET);
        continueIntent.putExtra(Setting.INTENT_PARAM_FLAG, Setting.INTENT_PARAM_FLAG_CONTINUE);
        PendingIntent continuependingIntent = PendingIntent.getActivity(context, Status.STATE_NEW_VERSION_READY,
                continueIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent resetIntent = new Intent(context, TimePickerWindow.class);//通知栏reset按钮
        resetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        resetIntent.putExtra(Setting.INTENT_PARAM_NOTIFY_ID, NOTIFY_CONTINUE_RESET);
        resetIntent.putExtra(Setting.INTENT_PARAM_FLAG, Setting.INTENT_PARAM_FLAG_RESET);

        if (taskId == TaskID.TASK_DOWNLOAD_DELAY) {
            resetIntent.putExtra(TaskID.STATUS, TaskID.TASK_DOWNLOAD_DELAY);
        } else if (taskId == TaskID.TASK_INSTALL_LATER) {
            resetIntent.putExtra(TaskID.STATUS, TaskID.TASK_INSTALL_LATER);
        } else if (taskId == TaskID.TASK_DOWNLOAD_INSTALL_DELAY) {
            resetIntent.putExtra(TaskID.STATUS, TaskID.TASK_DOWNLOAD_INSTALL_DELAY);
        }


        PendingIntent resetpendingIntent = PendingIntent.getActivity(context, Status.STATE_NEW_VERSION_READY,
                resetIntent, PendingIntent.FLAG_CANCEL_CURRENT);


        Intent intentCancel = new Intent(context, MyReceiver.class);
        intentCancel.setAction("notification_cancelled");
        intentCancel.putExtra(MyReceiver.TYPE, NOTIFY_CONTINUE_RESET);
        PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(context, 0, intentCancel, PendingIntent.FLAG_ONE_SHOT);


        String appName = context.getString(R.string.app_name);
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.mipmap.status_bar_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.notification_bar_icon))
                .setTicker(appName)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.notify_content))
                .setStyle(new Notification.BigTextStyle().bigText(context.getString(R.string.notify_content)))
                .setContentIntent(pendingIntent)
                .addAction(0, context.getString(R.string.notify_positive), continuependingIntent)
                .addAction(0, context.getString(R.string.notify_negative), resetpendingIntent)
                .setDeleteIntent(pendingIntentCancel);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setColor(ContextCompat.getColor(context, R.color.notification_text_color));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL, appName, manager.IMPORTANCE_LOW);
            notificationChannel.enableVibration(false);
            notificationChannel.enableLights(false);
            notificationChannel.setSound(null, null);
            manager.createNotificationChannel(notificationChannel);
            builder.setChannelId(NOTIFICATION_CHANNEL);
        }


        Notification notify = builder.build();
        if (isOngoing) {
            notify.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR | Notification.PRIORITY_MAX;
        } else {
            notify.flags = Notification.FLAG_AUTO_CANCEL | Notification.PRIORITY_MAX;
        }

        manager.notify(NOTIFY_CONTINUE_RESET, notify);

    }


    public void showNotification(Context context, int notifyId, String title, String content, PendingIntent pendingIntent, boolean isOngoing) {
        showNotification(context, notifyId, title, content, pendingIntent, null, isOngoing);
    }

    public void showNotification(Context context, int notifyId, String title, String content, PendingIntent pendingIntent,
                                 PendingIntent actionpendingIntent, boolean isOngoing) {
        String appName = context.getString(R.string.app_name);
        Intent intentCancel = new Intent(context, MyReceiver.class);
        intentCancel.setAction("notification_cancelled");
        intentCancel.putExtra(MyReceiver.TYPE, NOTIFY_CONTINUE_RESET);
        PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(context, 0, intentCancel, PendingIntent.FLAG_ONE_SHOT);

        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.mipmap.status_bar_icon)
                .setTicker(appName)
                .setContentTitle(TextUtils.isEmpty(title) ? appName : title)
                .setContentText(content)
                .setStyle(new Notification.BigTextStyle().bigText(content))
                .setDeleteIntent(pendingIntentCancel)
                .setContentIntent(pendingIntent);
        switch (notifyId) {
            case FcmService.NOTIFY_FCM_REPORT_FILE_PATH:
            case FcmService.NOTIFY_FCM_REPORT_STORAGE_SPACE:
            case FcmService.NOTIFY_FCM_REPORT_LOG:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    builder.addAction(new Notification.Action.Builder(null, context.getString(R.string.agree), pendingIntent).build());
                else
                    builder.addAction(0, context.getString(R.string.agree), pendingIntent);
                break;
            case FcmService.NOTIFY_FCM_MESSAGE:
                break;
//            case NOTIFY_DL_COMPLETED://安装提醒增加安装按钮
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
//                    builder.addAction(new Notification.Action.Builder(null, context.getString(R.string.update_now),
//                            actionpendingIntent == null ? pendingIntent : actionpendingIntent).build());
//                else
//                    builder.addAction(0, context.getString(R.string.update_now), actionpendingIntent == null ? pendingIntent : actionpendingIntent);
//                break;
            default:
                builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.notification_bar_icon));
                break;
        }
        android.app.NotificationManager manager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                builder.setColor(ContextCompat.getColor(context, R.color.notification_text_color));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL, appName, android.app.NotificationManager.IMPORTANCE_LOW);
                notificationChannel.enableVibration(false);
                notificationChannel.enableLights(false);
                notificationChannel.setShowBadge(true);
                manager.createNotificationChannel(notificationChannel);
                builder.setChannelId(NOTIFICATION_CHANNEL);
                builder.setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL);
            }
            Notification notify = builder.build();
            if (isOngoing) {
                notify.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR | Notification.PRIORITY_MAX;
            } else {
                notify.flags = Notification.FLAG_AUTO_CANCEL;
            }
            cancel(context, NOTIFY_CONTINUE_RESET);
            manager.notify(notifyId, notify);
            LogUtil.d("notification : title = " + title + ",content = " + content
                    + ",notifyId = " + notifyId + ", is resident : " + isOngoing);
        }
    }


    public void cancel(Context context, int id) {
        LogUtil.d("cancel : id = " + id);
        android.app.NotificationManager manager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.deleteNotificationChannel(String.valueOf(id));
            }
            PreferencesUtils.putInt(context, "downloadandinstall", 0);
            manager.cancel(id);
        }
    }


    private boolean isShowProgressNotify() {
        int notice_type = QueryInfo.getInstance().getPolicyValue(QueryInfo.QUERY_NOTICE_TYPE, Integer.class);
        LogUtil.d("isShowProgressNotify = " + notice_type);
        return notice_type == 0;
    }

}

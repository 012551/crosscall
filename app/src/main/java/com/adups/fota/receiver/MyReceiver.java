package com.adups.fota.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Trace;
import android.text.TextUtils;

import com.adups.fota.MyApplication;
import com.adups.fota.config.Const;
import com.adups.fota.config.Status;
import com.adups.fota.config.TaskID;
import com.adups.fota.download.DownVersion;
import com.adups.fota.install.Install;
import com.adups.fota.manager.AlarmManager;
import com.adups.fota.query.QueryInfo;
import com.adups.fota.query.QueryVersion;
import com.adups.fota.service.CustomActionIntentService;
import com.adups.fota.service.CustomActionService;
import com.adups.fota.service.SystemActionService;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PreferencesUtils;
import com.adups.fota.utils.ToastUtil;

public class MyReceiver extends BroadcastReceiver {
    public static final String TYPE = "type";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            LogUtil.d(intent.toString());
            int version_status = Status.getVersionStatus(context);
            LogUtil.d("version_status = " + version_status);
            String action = intent.getAction();
            if (!TextUtils.isEmpty(action) && !action.equalsIgnoreCase(Const.SEND_CUSTOM_SERVICE_ACTION)) {
                SystemActionService.enqueueWork(context, intent);
            } else {
                CustomActionService.enqueueWork(context, intent.getIntExtra(TaskID.TASK, TaskID.TASK_QUERY_AUTO));
            }
            if ("notification_cancelled".equals(action)) {
                //处理滑动清除和点击删除事件
                LogUtil.d("notification_cancelled");
                PreferencesUtils.putInt(context, "query_count", 0);
                AlarmManager.notify_cancel(context, 1000 * 60 * 10);
            }

            if (Const.DOWNLOAD_NOW_BROADCAST.equals(action) && version_status == Status.STATE_NEW_VERSION_READY) {
                if (QueryInfo.getInstance().getVersionInfo() == null) {
//                    DeviceInfoUtil.getInstance().setCanUse(1);
                    PreferencesUtils.putBoolean(MyApplication.getAppContext(), "download_now_broadcast", true);
                    QueryVersion.getInstance().onQuery(MyApplication.getAppContext(), QueryVersion.QUERY_MANUAL);
                    LogUtil.d("版本不存在，或当前已是最新版本。启用fota");
                } else {
//                    DeviceInfoUtil.getInstance().setCanUse(1);
                    DownVersion.getInstance().download(MyApplication.getAppContext(), DownVersion.MANUAL);
                }
            }

            if (Const.INSTALL_NOW_BROADCAST.equals(action) && version_status >= Status.STATE_DL_PKG_COMPLETE) {
                LogUtil.d("enter update");
                Install.update(MyApplication.getAppContext());
            } else {
                LogUtil.d("当前没有升级包");
            }

            if (Const.DOWNLOAD_STOP_BROADCAST.equals(action)) {
                if (version_status == 2) {
                    DownVersion.getInstance().pause(MyApplication.getAppContext());
//                    DeviceInfoUtil.getInstance().setCanUse(0);
                    LogUtil.d("下载暂停");
                } else {
//                    DeviceInfoUtil.getInstance().setCanUse(0);
                    LogUtil.d("当前没有升级包正在下载");
                }
            }

            if (Const.SEND_NEW_VERSION_BROADCAST.equals(action)) {
                LogUtil.d("enter:" + PreferencesUtils.getBoolean(MyApplication.getAppContext(), "download_now_broadcast", false));
                if (PreferencesUtils.getBoolean(MyApplication.getAppContext(), "download_now_broadcast", false)) {
                    DownVersion.getInstance().download(MyApplication.getAppContext(), DownVersion.MANUAL);
                    PreferencesUtils.putBoolean(MyApplication.getAppContext(), "download_now_broadcast", false);
                }
            }
        }
    }

}

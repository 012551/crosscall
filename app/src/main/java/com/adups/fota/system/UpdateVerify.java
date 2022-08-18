package com.adups.fota.system;

import android.content.Context;

import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.install.InstallResult;
import com.adups.fota.manager.NotificationManager;
import com.adups.fota.query.QueryActivate;
import com.adups.fota.query.QueryInfo;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PreferencesUtils;

public class UpdateVerify {

    public void startVerify(Context context) {
        intLogConfig(context);
        int status = Status.getVersionStatus(context);
        if (status == Status.STATE_AB_UPDATING && InstallResult.isSuccess(context)) {
            Status.setVersionStatus(context, Status.STATE_REBOOT);
            status = Status.STATE_REBOOT;
        }
        LogUtil.d("startVerify status = " + status);
        isNotifyMessage(context, InstallResult.verify(context), status);
        QueryActivate.queryVerify(context);
        if (status == Status.STATE_DOWNLOADING) { //如果开机就处理下载过程中，就将状态设置成下载暂停
            Status.setVersionStatus(context, Status.STATE_PAUSE_DOWNLOAD);
        } else if (status >= Status.STATE_DL_PKG_COMPLETE) {
            Status.downloadCompleteTask(context);
        }
    }

    private void intLogConfig(Context context) {
        LogUtil.setSaveLog(PreferencesUtils.getBoolean(context, Setting.DEBUG_SWITCH));
        LogUtil.setSaveLogPath(PreferencesUtils.getString(context, Setting.DEBUG_LOG_PATH, ""));
    }

    //判断是否长驻通知栏
    private void isNotifyMessage(Context context, boolean isRebootRecovery, int status) {
        LogUtil.d("isNotifyMessage,isRebootRecovery = " + isRebootRecovery + " status = " + status);
        //新版本提示策略配置
        int noticeType = QueryInfo.getInstance().getPolicyValue(QueryInfo.QUERY_NOTICE_TYPE, Integer.class);
        boolean noticeResident = QueryInfo.getInstance().getPolicyValue(QueryInfo.QUERY_NOTICE_RESIDENT, Boolean.class);
        //下载完成提示策略配置
        int installNoticeType = QueryInfo.getInstance().getPolicyValue(QueryInfo.INSTALL_NOTICE_TYPE, Integer.class);
        boolean installNoticeResident = QueryInfo.getInstance().getPolicyValue(QueryInfo.INSTALL_NOTICE_RESIDENT, Boolean.class);

        LogUtil.d("noticeType = " + noticeType + "||"
                + "noticeResident = " + noticeResident + "||"
                + "installNoticeType = " + installNoticeType + "||"
                + "installNoticeResident = " + installNoticeResident
        );
        if (!isRebootRecovery && status != Status.STATE_QUERY_NEW_VERSION) {
            if ((noticeType == 0 && noticeResident) ||
                    (installNoticeType == 0 && installNoticeResident)) {
                if (status == Status.STATE_DL_PKG_COMPLETE) {
                    LogUtil.d("showDownloadCompleted");
                    NotificationManager.getInstance().showDownloadCompleted(context, true);
                } else {
                    LogUtil.d("showNewVersion");
                    NotificationManager.getInstance().showNewVersion(context, true);
                }
            }
        }
    }

}

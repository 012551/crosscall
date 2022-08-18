package com.adups.fota.query;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;

import com.adups.fota.bean.CheckBean;
import com.adups.fota.bean.EventMessage;
import com.adups.fota.bean.FlagBean;
import com.adups.fota.bean.VersionBean;
import com.adups.fota.config.Const;
import com.adups.fota.config.Event;
import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.config.TaskID;
import com.adups.fota.download.DownVersion;
import com.adups.fota.install.Install;
import com.adups.fota.manager.ActivityManager;
import com.adups.fota.manager.AlarmManager;
import com.adups.fota.manager.JobScheduleManager;
import com.adups.fota.manager.NoticeManager;
import com.adups.fota.manager.SpManager;
import com.adups.fota.report.ReportData;
import com.adups.fota.request.RequestResult;
import com.adups.fota.service.CustomActionIntentService;
import com.adups.fota.service.CustomActionService;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.FileUtil;
import com.adups.fota.utils.JsonUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.MidUtil;
import com.adups.fota.utils.PreferencesUtils;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

public class ParserVersion {

    public void parser(Context context, RequestResult result) {
        int noticestatus = PreferencesUtils.getInt(context, "downloadandinstall", 0);
        LogUtil.d("noticestatus = " + noticestatus);
        if (result != null) {
            if (result.isSuccess()) {
                if (Status.getVersionStatus(context) >= Status.STATE_DL_PKG_COMPLETE) {
//                    NotificationManager.getInstance(context).showDownloadCompleted(context, true);
                    if (noticestatus != 1) {
                        NoticeManager.download(context);
                    }
                    LogUtil.d("query succeed,but a system has already been installed successfully");
                    return;
                }
                parserData(context, result.getResult());
            } else {
                MidUtil.reset(context);
                LogUtil.d("query version error, mid reset");
                realTipsException(result);
            }
        }
    }

    private void realTipsException(RequestResult result) {
        LogUtil.d("Status_code = " + result.getStatus_code());
        switch (result.getError_code()) {
            case Event.ERROR_IO:
                EventBus.getDefault().post(new EventMessage(Event.QUERY, Event.ERROR_IO, result.getStatus_code(), 0, result.getError_message()));
                break;
            default:
                EventBus.getDefault().post(new EventMessage(Event.QUERY, Event.ERROR_UNKNOWN, result.getStatus_code(), 0, result.getError_message()));
                break;
        }
    }

    private void parserData(Context context, String content) {
        LogUtil.d("content = " + content);
        CheckBean checkBean = JsonUtil.jsonObj(content, CheckBean.class);
        if (checkBean != null) {
            LogUtil.d("status = " + checkBean.getStatus());
            AlarmManager.queryScheduleAlarm(context);
            FlagBean flagBean = checkBean.getFlag();
            if (flagBean != null) {
                String mid = flagBean.getMid();
                if (!TextUtils.isEmpty(mid))
                    PreferencesUtils.putString(context, Setting.MID, mid);
                long checkFreq = flagBean.getCheckFreq();
                if (checkFreq != 0L)
                    PreferencesUtils.putLong(context, Setting.QUERY_SERVER_FREQ, checkFreq);
                String jobScheduleTime = flagBean.getJobScheduleTime();
                if (!TextUtils.isEmpty(jobScheduleTime))
                    SpManager.setJobQueryTime(jobScheduleTime);
                String jobScheduleDownloadingTime = flagBean.getJobScheduleDownloadingTime();
                if (!TextUtils.isEmpty(jobScheduleDownloadingTime))
                    SpManager.setJobDownloadTime(jobScheduleDownloadingTime);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    JobScheduleManager.scheduleJob(context, JobScheduleManager.CHANGE_NETWORK_JOB_ID);
                PreferencesUtils.putInt(context, Setting.QUERY_FULL, flagBean.getIsFull());
                PreferencesUtils.putInt(context, Setting.FOTA_UPGRADE, flagBean.getIsUpgrade());
            }
            VersionBean version = checkBean.getVersion();
            if (version != null) {
                LogUtil.d("version exists");
                version_process(context, version);
            } else {
                queryNotify(context, Event.QUERY_NO_VERSION, null);
                if (Const.QUERY_VERSION_BY_FULL) {
                    diffQuery(context);
                }
            }
        }
    }

    //当全量检测未检测到全量包时进行一次差分检测
    private void diffQuery(final Context context) {
        LogUtil.d("QueryVersionType = " + QueryVersion.getInstance().getQueryVersionType());
        if (QueryVersion.getInstance().getQueryVersionType() == QueryVersion.QUERY_VERSION_FULL
                && QueryVersion.getInstance().getQueryType() == QueryVersion.QUERY_SCHEDULE) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //关闭全量检测，下次进行差分检测
                    PreferencesUtils.putBoolean(context, Setting.FOTA_FULL_QUERY, false);
                    SystemClock.sleep(3000);
                    QueryVersion.getInstance().onQueryScheduleTask(context);
                }
            }).start();
        }
    }

    private void version_process(Context context, VersionBean newVersion) {
        if (newVersion != null) {
            String deviceVersion = DeviceInfoUtil.getInstance().getLocalVersion();
            LogUtil.d("deviceVersion = " + deviceVersion);
            String newVersionName = newVersion.getVersionName();
            if (!TextUtils.isEmpty(newVersionName) && newVersionName.equals(deviceVersion)) {
                EventBus.getDefault().post(new EventMessage(Event.QUERY, Event.QUERY_SAME_VERSION, 0, 0, null));
                ReportData.postDownload(context, ReportData.DOWN_STATUS_CAUSE_SAME_VERSION, 0);
                return;
            }
            VersionBean currentVersion = QueryInfo.getInstance().getVersionInfo();//获取之前的版本信息
            String content = JsonUtil.toJson(newVersion);
            //将检测到的版本，用文件的形式保存,保存失败则不去处理新版本信息
            if (!FileUtil.writeInternalFile(context, Const.VERSION_FILE, content)) {
                LogUtil.d("version_process  writeInternalFile error ");
                EventBus.getDefault().post(new EventMessage(Event.QUERY, Event.ERROR_SDCARD_NOT_ENOUGH, 0, 0, null));
                ReportData.postDownload(context, ReportData.DOWN_STATUS_CAUSE_NOT_ENOUGH, 0);
                return;
            }
            int versionStatus = Status.getVersionStatus(context);
            LogUtil.d("versionStatus = " + versionStatus);
            boolean isChange = isPolicyChange(context, content);
            if (currentVersion != null) {
                if (!TextUtils.isEmpty(newVersionName) && !newVersionName.equals(currentVersion.getVersionName())) {
                    LogUtil.d("new version version name different");
                    clearFailCount(context, newVersion.getVersionName());
                    QueryInfo.getInstance().update(context, newVersion);
                    queryNotify(context, Event.QUERY_CHANGE_VERSION, newVersion);
                } else if (!TextUtils.isEmpty(newVersion.getDeltaUrl()) &&
                        !newVersion.getDeltaUrl().equals(currentVersion.getDeltaUrl())) {
                    LogUtil.d("new version delta url different");
                    clearFailCount(context, newVersion.getVersionName());
                    QueryInfo.getInstance().update(context, newVersion);
                    queryNotify(context, Event.QUERY_CHANGE_VERSION, newVersion);
                } else if (isChange) {
                    LogUtil.d("new version delta content different ");
                    QueryInfo.getInstance().update(context, newVersion);//策略变化，相同版本也会执行
                    queryNotify(context, Event.QUERY_POLICY_CHANGE, newVersion);
                } else {
                    setVersionTips(context, newVersion, versionStatus, false);
                }
            } else {
                setVersionTips(context, newVersion, versionStatus, isChange);
            }
        }
        LogUtil.d("download_path_server : " +
                QueryInfo.getInstance().getPolicyValue(QueryInfo.DOWNLOAD_PATH_SERVER, Integer.class));
    }

    //设置版本是否是最新版本还是相同版本
    private void setVersionTips(Context context, VersionBean new_version, int version_status, boolean isChange) {
        LogUtil.d("version_status = " + version_status + "; isChange = " + isChange);
        if (version_status == Status.STATE_QUERY_NEW_VERSION || isChange) {
            clearFailCount(context, new_version.getVersionName());
            QueryInfo.getInstance().update(context, new_version);
            queryNotify(context, Event.QUERY_NEW_VERSION, new_version);
        } else {
            queryNotify(context, Event.QUERY_SAME_VERSION, new_version);
        }
    }

    private void queryNotify(Context context, int type, VersionBean version) {
        switch (type) {
            case Event.QUERY_POLICY_CHANGE:
                //lirenqi 20170925
                if (isSystemDamaged(context, version)) {
                    return;
                }
                doSameVersion(context);
                break;
            case Event.QUERY_NEW_VERSION:
            case Event.QUERY_CHANGE_VERSION:
                //lirenqi 20170925
                if (isSystemDamaged(context, version)) {
                    return;
                }
                boolean isLegal = onVersionChange(context, version);
                if (!isLegal) {
                    EventBus.getDefault().post(new EventMessage(Event.QUERY, Event.QUERY_NO_VERSION, 0, 0, null));
                    return;
                } else {
                    //上报检测成功
                    RequestResult result = new RequestResult();
                    result.setIsSuccess(true);
                    ReportData.postQuery(context,
                            QueryVersion.QUERY_REAL,//真实检测到打点
                            QueryVersion.QUERY_VERSION_DIFF,
                            result);
                    //发送新版本广播
                    sendNewVersionBroadcast(context);
                }
                break;
            case Event.QUERY_SAME_VERSION:
                if (!ActivityManager.getManager().isMainActivityTop()) {
                    NoticeManager.query(context);
                }
                doSameVersion(context);
                break;
            case Event.QUERY_NO_VERSION:
                if (QueryVersion.getInstance().isSystemDamaged(context, version)) {
                    EventBus.getDefault().post(new EventMessage(Event.QUERY, Status.UPDATE_STATUS_ROM_DAMAGED, 0, 0, null));
                    return;
                }
                break;
            case Event.ERROR_IO:
                ReportData.postDownload(context, ReportData.DOWN_STATUS_CAUSE_PARSER_ERROR, 0);
                break;
        }
        EventBus.getDefault().post(new EventMessage(Event.QUERY, type, 0, 0, null));
    }

    /**
     * @param context Context
     * @param version {@linkplain   VersionBean }
     */
    private boolean onVersionChange(Context context, VersionBean version) {
        DownVersion.getInstance().cancel(context);
        Status.queryNewVersion(context, version);
        return true;
    }

    /**
     * 初始下载或者安装失败次数
     *
     * @param versionName 检测到版本，版本号
     */
    private void clearFailCount(Context context, String versionName) {
        //检测到新版本，将升级失败记录清除
        PreferencesUtils.putInt(context, Setting.FOTA_INSTALL_FAIL_COUNTS, 0);
        PreferencesUtils.putString(context, Setting.FOTA_INSTALL_FAIL_VERSION, versionName);
    }

    /**
     * 判断策略是否发生变化
     */
    private boolean isPolicyChange(Context context, String content) {
        boolean isChange = false;
        try {
            JSONObject json = new JSONObject(content);
            String newPolicy = "";
            if (json.has("policy"))
                newPolicy = json.getString("policy");
            if (!TextUtils.isEmpty(newPolicy)) {
                String oldPolicy = PreferencesUtils.getString(context, Setting.FOTA_POLICY_CONTENT, "");
                LogUtil.d("newPolicy = " + newPolicy + " ; oldPolicy = " + oldPolicy);
                if (!newPolicy.equalsIgnoreCase(oldPolicy)) {
                    PreferencesUtils.putString(context, Setting.FOTA_POLICY_CONTENT, newPolicy);
                    isChange = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogUtil.d("isPolicyChange : " + isChange);
        return isChange;
    }

    //判断是否发送检测到新版本的广播
    private void sendNewVersionBroadcast(Context mContext) {
        LogUtil.d("sendNewVersionBroadcast");
        Intent intent = new Intent();
        intent.setAction(Const.SEND_NEW_VERSION_BROADCAST);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        mContext.sendBroadcast(intent, Const.SEND_BROADCAST_PERMISSION);
    }

    private void doSameVersion(Context context) {
        int version_status = Status.getVersionStatus(context);
        LogUtil.d("version_status = " + version_status);
        //周期达到还处理在正在下载，让状态暂停，恢复下载
        if (version_status == Status.STATE_DOWNLOADING) { //lirenqi 20170925 解决下载状态错乱问题
            DownVersion.getInstance().pause(context);
            ReportData.postDownload(context, ReportData.DOWN_STATUS_CAUSE_DOWNLOADING, 0);
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            DownVersion.getInstance().download(context, DownVersion.AUTO);
        } else if (version_status == Status.STATE_DL_PKG_COMPLETE) {
            boolean force_install = QueryInfo.getInstance().getPolicyValue(QueryInfo.INSTALL_FORCED, Boolean.class);
            if (force_install) {
                LogUtil.d("force_install = " + force_install);
                Install.force_update(context);
            }
        } else {
            CustomActionService.enqueueWork(context, TaskID.TASK_FORCE_DOWNLOAD);
        }
    }

    private boolean isSystemDamaged(Context context, VersionBean version) {
        if (QueryVersion.getInstance().isSystemDamaged(context, version)) {
            //已root的设备不可升级
            if (Status.getVersionStatus(context) == Status.STATE_DOWNLOADING) {
                DownVersion.getInstance().cancel(context);
            }
            EventBus.getDefault().post(new EventMessage(Event.QUERY, Status.UPDATE_STATUS_ROM_DAMAGED, 0, 0, null));
            ReportData.postDownload(context, ReportData.DOWN_STATUS_CAUSE_DEVICE_ROOTED, 0);
            return true;
        }
        return false;
    }

}

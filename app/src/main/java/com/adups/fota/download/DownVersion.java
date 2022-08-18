package com.adups.fota.download;

import android.content.Context;
import android.text.format.Time;

import com.adups.fota.MyApplication;
import com.adups.fota.bean.EventMessage;
import com.adups.fota.bean.VersionBean;
import com.adups.fota.config.Event;
import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.config.TaskID;
import com.adups.fota.install.Install;
import com.adups.fota.manager.AlarmManager;
import com.adups.fota.manager.NoticeManager;
import com.adups.fota.query.QueryInfo;
import com.adups.fota.report.ReportData;
import com.adups.fota.report.ReportManager;
import com.adups.fota.service.CustomActionIntentService;
import com.adups.fota.service.CustomActionService;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.NetWorkUtil;
import com.adups.fota.utils.PreferencesUtils;
import com.adups.fota.utils.StorageUtil;
import com.adups.fota.utils.SystemSettingUtil;

import org.greenrobot.eventbus.EventBus;
import java.util.Calendar;
import java.util.Set;

public class DownVersion {

    public static final int AUTO = 1;
    public static final int MANUAL = 0;
    private static DownVersion downVersion;
    private int download_flag;      //  AUTO  or MANUAL

    private DownVersion() {
        download_flag = MANUAL;
    }

    public static DownVersion getInstance() {
        if (downVersion == null) {
            synchronized (DownVersion.class) {
                if (downVersion == null) {
                    downVersion = new DownVersion();
                }
            }
        }
        return downVersion;
    }

    public void download(Context mContext, int flag) {
        synchronized (DownVersion.class) {
            //lirenqi 20171103 add for change wifi to mobile don't download auto
            if (DownPackage.getInstance().isDownloading()) {
                int version_status = Status.getVersionStatus(mContext);
                LogUtil.d("version_status = " + version_status);
                if (version_status != Status.STATE_PAUSE_DOWNLOAD) {
                    LogUtil.d("downloading package ");
                    ReportData.postDownload(mContext, ReportData.DOWN_STATUS_CAUSE_DOWNLOADING, 0);
                    return;
                }
            } else {
                LogUtil.d("mDown == null; flag = " + flag);
            }
            Status.setVersionStatus(mContext, Status.STATE_DOWNLOADING);
            if (flag == DownVersion.AUTO) {
                LogUtil.d("download AUTO");
            }

            try{
                SystemSettingUtil.putInt("fota_updateavailable",0);
            }catch (Exception e){}

            download_flag = flag;
            DownPackage.getInstance().execute();
        }
    }

    public int getDownloadFlag() {
        return download_flag;
    }

    public void pause(Context mContext) {
        synchronized (this) {
            LogUtil.d("");
            DownPackage.getInstance().pause(mContext);
            NoticeManager.downloadPause(mContext);
        }
    }

    public void cancel(Context mContext) {
        synchronized (this) {
            LogUtil.d("");
            DownPackage.getInstance().cancel(mContext);
            NoticeManager.downloadCancel(mContext);
        }
    }

    public void onClickCancel(Context mContext) {
        cancel(mContext);
        Status.idleReset(mContext);
    }

    //zhangzhou AutoDownload
    private boolean isAutoDownload(Context mContext) {
        boolean isWifi = NetWorkUtil.isWiFiConnected(mContext);
        boolean isAutoDown;
        int version_status = Status.getVersionStatus(mContext);
        if (isFailOverCount(mContext)) {
            return false;
        }
        if (NetWorkUtil.isMobileConnected(mContext) && NetWorkUtil.isRoaming(mContext)) {
            LogUtil.d("The current roaming in data flow");
            return false;
        }
        if (NetWorkUtil.isConnected(mContext) && NetWorkUtil.is2GConnected(mContext)) {
            LogUtil.d("can not autoDownload 2G");
            return false;
        }
        //夜间静默下载
        if (PreferencesUtils.getBoolean(mContext, Setting.SLIENT_DOWNLOAD, false)
        && isHourRange() == 0 && isCanAutoDown(mContext, isWifi, false)){
            isAutoDown = true;
            LogUtil.d("SLIENT_DOWNLOAD is open");
        }
        else{
            isAutoDown = isCanAutoDown(mContext, isWifi, false);
        }
        LogUtil.d("isAutoDown= " + isAutoDown + "; version_status= " + version_status);

        //zhangzhou
        return ((version_status == Status.STATE_NEW_VERSION_READY || version_status == Status.STATE_PAUSE_DOWNLOAD)//检测到版本，或者下载暂停时
                && isAutoDown);//判断是否满足自动下载
    }

    /**
     * 判断是否可以自动下载
     *
     * @param context
     * @param isWifi      是否当前连接wifi
     * @param isNetChange 是否从网络切换进来
     */
    private boolean isCanAutoDown(Context context, boolean isWifi, boolean isNetChange) {
        try {
            int autoDownload = QueryInfo.getInstance().getPolicyValue(QueryInfo.DOWNLOAD_AUTO, Integer.class);
            LogUtil.d("autoDownload = " + autoDownload);
            if (autoDownload == 2) {//后台关闭
                return false;
            } else if (autoDownload == 1) {//后台打开自动下载
                return followServerDownload(isWifi);//配置强制升级或者自动下载 并且 任意义网络或者wifi连接;
            } else if (autoDownload == 0) {//以客户端为准
                return followLocalDownload(context, isWifi, isNetChange);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    //判断服务器配置是否满足自动下载
    private boolean followServerDownload(boolean isWiFi) {
        boolean isForced = QueryInfo.getInstance().getPolicyValue(QueryInfo.INSTALL_FORCED, Boolean.class);
        boolean isForcedWifi = QueryInfo.getInstance().getPolicyValue(QueryInfo.DOWNLOAD_WIFI, Boolean.class);
        boolean autoDownload = QueryInfo.getInstance().getPolicyValue(QueryInfo.DOWNLOAD_AUTO, Boolean.class);
        LogUtil.d("isForced = " + isForced + "; isForcedWifi = " + isForcedWifi + "; autoDownload = " + autoDownload);
        //配置强制升级或者自动下载 并且 任意义网络或者wifi连接
        if ((isForced || autoDownload) && (!isForcedWifi)) {//配置了自动下载，或者强制升级并且是任意网络
            return true;
        } else return (isForced || autoDownload) && isWiFi;
    }

    //判断本地设置是否满足自动下载
    private boolean followLocalDownload(Context mContext, boolean isWifi, boolean isDownloading) {
        boolean isReturn = true;
        boolean isWifiAuto = PreferencesUtils.getBoolean(mContext, Setting.DOWNLOAD_WIFI_AUTO, DeviceInfoUtil.getInstance().isAutoWifi());
        boolean isOnlyWifi = PreferencesUtils.getBoolean(mContext, Setting.DOWNLOAD_ONLY_WIFI, DeviceInfoUtil.getInstance().isOnlyWifi());
        int version_status = Status.getVersionStatus(mContext);
        LogUtil.d("isWifi = " + isWifi + "; isWifiAuto = " + isWifiAuto + "; isOnlyWifi = " + isOnlyWifi);
        if (isDownloading || version_status == Status.STATE_DOWNLOADING) { //正在下载过程中，会授到仅wifi开关的影响
            if (!isWifi) {//配置仅wifi，但是不是wifi网络，不自动下载
                isReturn = false;
            }
        } else {//非下载过程中，判断是否自动下载
            if (!isWifiAuto) {//wifi自动下载，关闭将不自动下载
                isReturn = false;
            } else if (!isWifi) {//wifi自动下载打开，但是没有wifi，不会自动下载
                isReturn = false;
            }
        }
        return isReturn;
    }


    //增加判断是否超过升级失败数 2016年10月18日15:12:24
    private boolean isFailOverCount(Context mContext) {
        try {
            int updatefailcount = PreferencesUtils.getInt(mContext, Setting.FOTA_INSTALL_FAIL_COUNTS);
            if (updatefailcount >= 5) {
                if (ReportManager.getInstance().isRunnable(mContext, ReportData.DOWN_STATUS_CAUSE_INSTALL_FAIL_5, ReportManager.REPORT_INVALID_TIME)) {
                    ReportData.postDownload(mContext, ReportData.DOWN_STATUS_CAUSE_INSTALL_FAIL_5, 0);
                }
                LogUtil.d("updatefailcount = " + updatefailcount + ", return true!!!");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void forceDownload(Context mContext) {
        if (NetWorkUtil.isMobileConnected(mContext) && MyApplication.isCalling()) return;
        VersionBean model = QueryInfo.getInstance().getVersionInfo();
        long size = model.getFileSize();
        switch (QueryInfo.getInstance().getPolicyValue(QueryInfo.DOWNLOAD_PATH_SERVER, Integer.class)) {
            case QueryInfo.DOWNLOAD_PATH_IGNORE:
                switch (DeviceInfoUtil.getInstance().getPath()) {
                    case StorageUtil.PATH_INTERNAL:
                        if (!StorageUtil.checkInsideSpaceAvailable(mContext, size)) {
                            LogUtil.d("inside has no space , return");
                            if (ReportManager.getInstance().isRunnable(mContext,
                                    ReportManager.DOWN_STATUS_CAUSE_NOT_ENOUGH, ReportManager.REPORT_INVALID_TIME))
                                ReportData.postDownload(mContext, ReportData.DOWN_STATUS_CAUSE_NOT_ENOUGH, 0);
                            return;
                        }
                        break;
                    case StorageUtil.PATH_EXTERNAL:
                        if (!StorageUtil.isSdcardMounted(mContext)) {
                            LogUtil.d("sdcard not mounted , return");
                            if (ReportManager.getInstance().isRunnable(mContext,
                                    ReportManager.DOWN_STATUS_CAUSE_NOT_ENOUGH, ReportManager.REPORT_INVALID_TIME))
                                ReportData.postDownload(mContext, ReportData.DOWN_STATUS_CAUSE_NOT_ENOUGH, 0);
                            return;
                        }
                        if (!StorageUtil.checkOutsideSpaceAvailable(mContext, size)) {
                            LogUtil.d("sdcard has no space , return");
                            if (ReportManager.getInstance().isRunnable(mContext,
                                    ReportManager.DOWN_STATUS_CAUSE_NOT_ENOUGH, ReportManager.REPORT_INVALID_TIME))
                                ReportData.postDownload(mContext, ReportData.DOWN_STATUS_CAUSE_NOT_ENOUGH, 0);
                            return;
                        }
                        break;
                    default:
                        if (!StorageUtil.checkSpaceAvailable(mContext, size)) {
                            LogUtil.d("device has no space , return");
                            if (ReportManager.getInstance().isRunnable(mContext,
                                    ReportManager.DOWN_STATUS_CAUSE_NOT_ENOUGH, ReportManager.REPORT_INVALID_TIME))
                                ReportData.postDownload(mContext, ReportData.DOWN_STATUS_CAUSE_NOT_ENOUGH, 0);
                            return;
                        }
                        break;
                }
                break;
            case QueryInfo.DOWNLOAD_PATH_INSIDE:
                if (!StorageUtil.checkInsideSpaceAvailable(mContext, size)) {
                    LogUtil.d("inside has no space , return");
                    if (ReportManager.getInstance().isRunnable(mContext,
                            ReportManager.DOWN_STATUS_CAUSE_NOT_ENOUGH, ReportManager.REPORT_INVALID_TIME))
                        ReportData.postDownload(mContext, ReportData.DOWN_STATUS_CAUSE_NOT_ENOUGH, 0);
                    return;
                }
                break;
            case QueryInfo.DOWNLOAD_PATH_OUTSIDE:
                if (!StorageUtil.isSdcardMounted(mContext)) {
                    LogUtil.d("sdcard not mounted , return");
                    if (ReportManager.getInstance().isRunnable(mContext, ReportManager.DOWN_STATUS_CAUSE_NOT_ENOUGH, ReportManager.REPORT_INVALID_TIME))
                        ReportData.postDownload(mContext, ReportData.DOWN_STATUS_CAUSE_NOT_ENOUGH, 0);
                    return;
                }
                if (!StorageUtil.checkOutsideSpaceAvailable(mContext, size)) {
                    LogUtil.d("sdcard has no space , return");
                    if (ReportManager.getInstance().isRunnable(mContext, ReportManager.DOWN_STATUS_CAUSE_NOT_ENOUGH, ReportManager.REPORT_INVALID_TIME))
                        ReportData.postDownload(mContext, ReportData.DOWN_STATUS_CAUSE_NOT_ENOUGH, 0);
                    return;
                }
                break;
        }
        if (isAutoDownload(mContext)) {
            download(mContext, DownVersion.AUTO);
        } else {
            LogUtil.d("no download reason : not satisfy auto download condition");
            if (Status.getVersionStatus(mContext) == Status.STATE_NEW_VERSION_READY &&
                    ReportManager.getInstance().isRunnable(mContext, ReportManager.DOWN_STATUS_CAUSE_UNAUTO, ReportManager.REPORT_INVALID_TIME)) {
                ReportData.postDownload(mContext, ReportData.DOWN_STATUS_CAUSE_UNAUTO, 0);
            }
        }
    }

    public boolean isDownloading(Context mContext) {
        //2017年5月22日17:51:21 增加正在下载状态
        return (DownPackage.getInstance().isDownloading() && Status.getVersionStatus(mContext) == Status.STATE_DOWNLOADING);
    }

    /**
     * verify auto download ,force download, pause download when network type is changed
     */
    public void onDownloadTask(Context context) {
        VersionBean model = QueryInfo.getInstance().getVersionInfo();
        if (model == null) {
            return;
        }
        if (isDownloading(context)) {
            boolean isWifi = NetWorkUtil.isWiFiConnected(context);
            boolean isAutoDown = isCanAutoDown(context, isWifi, true);
            LogUtil.d("isAutoDown= " + isAutoDown);
            if (!isAutoDown && !isWifi) {
                ReportData.postDownload(context, ReportData.DOWN_CAUSE_NET_CHANGE_DOWNLOADING, 0);
                // wifi to mobile type,then pause download
                pause(context);
                EventBus.getDefault().post(new EventMessage(Event.DOWNLOAD, Event.NETWORK_TYPE_WIFI_TO_MOBILE, 0, 0, null));
            }
        } else {
            CustomActionService.enqueueWork(context, TaskID.TASK_FORCE_DOWNLOAD);
        }
    }

    public void slientDownload(Context context){
        if (!PreferencesUtils.getBoolean(context, Setting.SLIENT_DOWNLOAD, false)){
            LogUtil.d("not allow slientDownload!");
            return;
        }
        if (Status.getVersionStatus(context) != Status.STATE_NEW_VERSION_READY && Status.getVersionStatus(context) != Status.STATE_PAUSE_DOWNLOAD){
            LogUtil.d("slientDownload, but status = " + Status.getVersionStatus(context));
            return;
        }
        LogUtil.d("isHourange: "+ isHourRange());
        if (isHourRange() == 0){
            boolean batteryEnough = DeviceInfoUtil.getInstance().isBatteryStatusOk(context);
            boolean isConnected = NetWorkUtil.isConnected(context);
            boolean isWifi = NetWorkUtil.isWiFiConnected(context);
            LogUtil.d("batteryEnough = " + batteryEnough + "; isConnected = " + isConnected + "; isWifi = " + isWifi);
            if (batteryEnough && isConnected && isWifi){
                DownVersion.getInstance().download(context, DownVersion.AUTO);
            }else {
                nightDownloadAlarm(context);
            }
        }else {
            nightDownloadAlarm(context);
        }
    }
    private static int isHourRange() {
        long currentTime = System.currentTimeMillis();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(currentTime);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        LogUtil.d("isHourRange, hour: " + hour);
        if (hour < 1) {
            return -1;
        } else if (hour >= 5) {
            return 1;
        } else {
            return 0;
        }
    }
    public void nightDownloadAlarm(Context context) {
        int isHourRange = isHourRange();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        // now time < start time
        if (isHourRange == -1) {
            c.set(Calendar.HOUR_OF_DAY, 1);
            c.set(Calendar.MINUTE, 0);
        }
        //  now time > end time ,then next day start time
        else if (isHourRange == 1) {
            c.add(Calendar.DATE, 1);
            c.set(Calendar.HOUR_OF_DAY, 1);
            c.set(Calendar.MINUTE, 0);
        } else {
            c.set(Calendar.HOUR, c.get(Calendar.HOUR) + 1);
        }
        AlarmManager.download_night(context, c.getTimeInMillis());
    }
}
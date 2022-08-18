package com.adups.fota.query;

import android.content.Context;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;

import com.adups.fota.bean.EventMessage;
import com.adups.fota.bean.VersionBean;
import com.adups.fota.config.Const;
import com.adups.fota.config.Event;
import com.adups.fota.config.ServerApi;
import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.download.DownPackage;
import com.adups.fota.manager.AlarmManager;
import com.adups.fota.report.ReportData;
import com.adups.fota.request.RequestManager;
import com.adups.fota.request.RequestResult;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.MidUtil;
import com.adups.fota.utils.NetWorkUtil;
import com.adups.fota.utils.OkHttpUtil;
import com.adups.fota.utils.PackageUtils;
import com.adups.fota.utils.PreferencesUtils;
import com.adups.fota.utils.SystemSettingUtil;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by xw on 15-12-18.
 */
public class QueryVersion {

    public static final int QUERY_SCHEDULE = 1;
    public static final int QUERY_MANUAL = 2;
    public static final int QUERY_REAL = 4;
    public static final int QUERY_VERSION_DIFF = 1;
    public static final int QUERY_VERSION_FULL = 2;
    private static final int QUERY_PUSH = 3;
    private static QueryVersion mQueryVersion = null;
    private static int errorTime = 0;//检测失败次数超过3次将强制设置检测周期启点
    private boolean isQuerying;
    private int mQueryType;
    private int mQueryVersionType = QUERY_VERSION_DIFF;

    private QueryVersion() {
    }

    public static QueryVersion getInstance() {
        if (mQueryVersion == null) {
            synchronized (QueryVersion.class) {
                if (mQueryVersion == null) {
                    mQueryVersion = new QueryVersion();
                }
            }
        }
        return mQueryVersion;
    }

    public void onQuerySchedule(Context mContext, boolean systemAction) {
        if (systemAction)
            AlarmManager.queryScheduleAlarm(mContext);
        boolean isConnected = NetWorkUtil.isConnected(mContext);
        boolean isOverSchedule = isOverSchedule(mContext);
        boolean isOverActivateTime = QueryActivate.isOverActivateTime(mContext);
        boolean is2G = NetWorkUtil.is2GConnected(mContext);
        boolean isRoaming = NetWorkUtil.isRoaming(mContext);
        boolean isFinishSetupWizard = SystemSettingUtil.getInt(Settings.Global.DEVICE_PROVISIONED,0) == 1;
        if (PackageUtils.getUserId(Process.myUid()) != 0) {
            LogUtil.d("onQuerySchedule,not system user,return");
            return;
        }
        LogUtil.d("isOverSchedule = " + isOverSchedule + "; isConnected = " + isConnected + "; isOverActivateTime = " + isOverActivateTime +
                "; is2G = " + is2G + "; isRoaming = " + isRoaming + "; isFinishSetupWizard = " + isFinishSetupWizard);
        if (isOverActivateTime && isConnected && isOverSchedule && isFinishSetupWizard) {
            if (!is2G && !isRoaming){
                onQueryScheduleTask(mContext);
                //判断是否下载清除
                resetFactory(mContext);
            }else {
                AlarmManager.queryScheduleAlarm(mContext);
            }

        }
    }

    /**
     * querying
     *
     * @return
     */
    public boolean isQuerying() {
        return isQuerying;
    }

    public int getQueryType() {
        return mQueryType;
    }


    public int getQueryVersionType() {
        return mQueryVersionType;
    }

    public void setQueryVersionType(int QueryVersionType) {
        mQueryVersionType = QueryVersionType;
    }

    /**
     * query version
     *
     * @param mContext
     * @param query_type         {@linkplain #QUERY_MANUAL#QUERY_PUSH#QUERY_SCHEDULE}
     * @param query_version_type {@linkplain #QUERY_VERSION_DIFF#QUERY_VERSION_FULL}
     */
    public void onQuery(Context mContext, int query_type, int query_version_type) {
        LogUtil.d("query_type = " + query_type + "; isQuerying = " + isQuerying);
        synchronized (this) {
            if (isQuerying) {
              //  EventBus.getDefault().post(new EventMessage(Event.QUERY, Event.QUERY_ONGOING, 0, 0, null));
                return;
            }
            isQuerying = true;
            mQueryType = query_type;
            mQueryVersionType = query_version_type;
            query(mContext);
        }
    }

    /**
     * @param mContext
     * @param type     {@linkplain  #QUERY_MANUAL#QUERY_SCHEDULE#QUERY_PUSH }
     */
    public void onQuery(Context mContext, int type) {
        onQuery(mContext, type, QUERY_VERSION_DIFF);
    }

    private void query(final Context mContext) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                LogUtil.d("thread start");
                onQueryTask(mContext);
                AlarmManager.queryScheduleAlarm(mContext);
                LogUtil.d("thread end");
            }

        }).start();
    }

    public void onQueryScheduleTask(Context mContext) {
        if (DeviceInfoUtil.getInstance().getCanUse() == 0){
            LogUtil.d("disable fota");
            return;
        }
        LogUtil.d("onQueryScheduleTask");
        mQueryVersionType = QUERY_VERSION_DIFF;
        //自动检测时先判断是否被root，考虑是否需要全量包检测
        if (Const.QUERY_VERSION_BY_FULL
                && PreferencesUtils.getBoolean(mContext, Setting.FOTA_ROM_DAMAGED, false)
                && PreferencesUtils.getBoolean(mContext, Setting.FOTA_FULL_QUERY, false)) {
            mQueryVersionType = QUERY_VERSION_FULL;
        }
        onQuery(mContext, QUERY_SCHEDULE, mQueryVersionType);
    }

    private void onQueryTask(Context mContext) {
        try {
            LogUtil.d("onQueryTask:start");
            if (Const.QUERY_VERSION_BY_FULL) {
                //每次检测时都将全量检测条件打开，下次检测到root后就可以进行全量检测了
                PreferencesUtils.putBoolean(mContext, Setting.FOTA_FULL_QUERY, true);
            }
            EventBus.getDefault().post(new EventMessage(Event.QUERY, Event.QUERY_RUNNING, 0, 0, null));
            //去掉mid，影响检测版本 2016年12月28日14:03:05
            MidUtil.checkMidValid(mContext);
            //判断是否是初始状态，是将重置
            if (Status.getVersionStatus(mContext) == Status.STATE_QUERY_NEW_VERSION) {
                QueryInfo.getInstance().reset(mContext);
            }
            resetDamageFlag(mContext);
            // 延迟0.5s，让等待框展示，防止消失太快
            if (mQueryType == QUERY_MANUAL) {
                SystemClock.sleep(500);
            }
            String query_url = "";
            String domain_server = PreferencesUtils.getString(mContext, Setting.QUERY_URL, ServerApi.SERVER_DOMAIN);
            if (mQueryVersionType == QUERY_VERSION_DIFF) {
                query_url = domain_server + ServerApi.QUERY;
            } else if (mQueryVersionType == QUERY_VERSION_FULL) {
                query_url = domain_server + ServerApi.QUERY_FULL;
            }
            RequestResult result = RequestManager.queryRequest(mContext, query_url, mQueryType);
            LogUtil.d("query result : "
                    + "http status code = " + result.getStatus_code()
                    + " error_code = " + result.getError_code()
                    + " error_message = " + result.getError_message()
            );
            ReportData.postQuery(mContext,
                    mQueryType,
                    mQueryVersionType,
                    result);
            //解析后台返回版本json结果
            new ParserVersion().parser(mContext, result);
            logicCheckResult(mContext, result, domain_server);
        } catch (Exception e) {
            LogUtil.d(e.toString());
            e.printStackTrace();
            // 空间不足提示
            if (mContext.getFilesDir() == null) {
                ReportData.postDownload(mContext, ReportData.DOWN_STATUS_CAUSE_NOT_ENOUGH, 0);
                EventBus.getDefault().post(new EventMessage(Event.QUERY, Event.ERROR_SDCARD_NOT_ENOUGH, 0, 0, null));
            } else {
                EventBus.getDefault().post(new EventMessage(Event.QUERY, Event.ERROR_UNKNOWN, 0, 0, null));
            }
        } finally {
            isQuerying = false;
        }
    }

    //判断check结果
    private void logicCheckResult(Context mContext, RequestResult result, String domain_server) {
        if (result == null) {
            return;
        }
        if (result.isSuccess()) { //与后台交互成功后设置最后检测时间，2017年1月7日13:37:29
            //最新检测时间
            PreferencesUtils.putLong(mContext, Setting.QUERY_LAST_TIME, System.currentTimeMillis());
        } else {
            errorTime = errorTime + 1;
            if (errorTime >= 3) {//检测失败次数超过3次将强制设置检测周期启点
                errorTime = 0;
                //最新检测时间
                PreferencesUtils.putLong(mContext, Setting.QUERY_LAST_TIME, System.currentTimeMillis());
            }
            OkHttpUtil.resetDNS();//清空dns
            resetServerDomain(mContext, domain_server);
        }
    }

    /**
     * it is time up to check
     *
     * @param mContext
     * @return
     */
    private boolean isOverSchedule(Context mContext) {
        long last_time = PreferencesUtils.getLong(mContext, Setting.QUERY_LAST_TIME, 0);
        long now = System.currentTimeMillis();
        return ((last_time + AlarmManager.getDefaultQuerySchedule(mContext)) <= now);
    }

    private boolean isSwitchDomain(Context mContext) {
        int counts = PreferencesUtils.getInt(mContext, Setting.QUERY_FAIL_COUNTS);
        if (counts >= Setting.MAX_QUERY_RETRY_COUNTS) {
            PreferencesUtils.putInt(mContext, Setting.QUERY_FAIL_COUNTS, -1);
            return true;
        } else {
            PreferencesUtils.putInt(mContext, Setting.QUERY_FAIL_COUNTS, counts + 1);
        }
        return false;
    }

    private void resetServerDomain(Context mContext, String domain_server) {
        if (isSwitchDomain(mContext)) {
            if (TextUtils.isEmpty(domain_server)) {
                PreferencesUtils.putString(mContext, Setting.QUERY_URL, ServerApi.SERVER_DOMAIN);
            } else {
                if (ServerApi.SERVER_DOMAIN.equals(domain_server)) {
                    PreferencesUtils.putString(mContext, Setting.QUERY_URL, ServerApi.SERVER_DOMAIN2);
                } else {
                    PreferencesUtils.putString(mContext, Setting.QUERY_URL, ServerApi.SERVER_DOMAIN);
                }
            }
        }
    }

    /**
     * 检测前判断是否与当前版本号一致，前提条件ROM已损坏
     *
     * @param mContext
     */
    private void resetDamageFlag(Context mContext) {
        boolean isDamaged = PreferencesUtils.getBoolean(mContext, Setting.FOTA_ROM_DAMAGED, false);
        if (isDamaged) {
            String damaged_version = PreferencesUtils.getString(mContext, Setting.FOTA_ROM_DAMAGED_VERSION, "FOTA");
            if (!damaged_version.equals(DeviceInfoUtil.getInstance().getLocalVersion())) {
                PreferencesUtils.putBoolean(mContext, Setting.FOTA_ROM_DAMAGED, false);
                PreferencesUtils.getString(mContext, Setting.FOTA_ROM_DAMAGED_VERSION, "FOTA");
            }
        }
    }


    public boolean isSystemDamaged(Context mContext, VersionBean version) {
        if (mQueryVersionType == QUERY_VERSION_DIFF) {
            boolean isDamaged = PreferencesUtils.getBoolean(mContext, Setting.FOTA_ROM_DAMAGED, false);
            boolean urlChanged = isChangeDeltaurl(mContext, version);
            boolean isUpgrade = PreferencesUtils.getInt(mContext, Setting.FOTA_UPGRADE, 0) == 1;
            LogUtil.d("isDamaged = " + isDamaged + "; urlChanged = " + urlChanged);
            if (urlChanged) {
                //重新设置rom是否损坏标识
                PreferencesUtils.putBoolean(mContext, Setting.FOTA_ROM_DAMAGED, false);
                return false;
            }
            //lirenqi 20171023 后台配置root可升级的话，不再提醒用户设备已root
            if (!isUpgrade) {
                return false;
            }
            return isDamaged;
        } else {
            return false;
        }
    }


    public boolean isFullUpdate(Context mContext) {
        int type = PreferencesUtils.getInt(mContext, Setting.FOTA_UPDATE_TYPE, QUERY_VERSION_DIFF);
        boolean isFullUpdate = (type == QUERY_VERSION_FULL);
        LogUtil.d("isFullUpdate = " + isFullUpdate);
        return isFullUpdate;
    }
    /*
     * 判断升级包下载地址是否发生变化
     *
     * */

    private boolean isChangeDeltaurl(Context context, VersionBean version) {
        boolean ischange = false;
        if (version == null) {
            LogUtil.d("version == null");
            return ischange;
        }
        try {
            String old_deltaurl = PreferencesUtils.getString(context, Setting.FOTA_DELTA_URL, "");
            String new_deltaurl = version.getDeltaUrl();
            if (!TextUtils.isEmpty(new_deltaurl) && !old_deltaurl.equals(new_deltaurl)) {
                LogUtil.d("isChangeDeltaUrl = true");
                //保存升级包下载地址
                PreferencesUtils.putString(context, Setting.FOTA_DELTA_URL, new_deltaurl);
                ischange = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ischange;
    }

    //清除缓存,恢复出厂
    private void resetFactory(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(60 * 1000);
                    int isClean = QueryInfo.getInstance().getPolicyValue(QueryInfo.CLEAR_CACHE, Integer.class);
                    String server_url = "";
                    VersionBean bean = QueryInfo.getInstance().getVersionInfo();
                    if (bean != null)
                        server_url = bean.getDeltaUrl();
                    String cache_url = PreferencesUtils.getString(context, "cache_url", "");
                    if (isClean == 1 && !cache_url.equals(server_url)) {//判断后台是否配置清除缓存，并同一版本未清除过
                        LogUtil.d("execute clear cache ");
                        DownPackage.getInstance().cancel(context);
                        Status.resetFactory(context);
                        PreferencesUtils.putString(context, "cache_url", server_url);
                        EventBus.getDefault().post(new EventMessage(Event.QUERY, Event.QUERY_INIT_VERSION, 0, 0, null));
                        ReportData.postDownload(context, ReportData.CHECK_STATUS_CLEAN_CACHE, 0);
                        Thread.sleep(5 * 1000);
                        onQueryScheduleTask(context);
                    }
                } catch (Exception e) {
                    LogUtil.d(e.getMessage());
                }
            }
        }).start();
    }

}

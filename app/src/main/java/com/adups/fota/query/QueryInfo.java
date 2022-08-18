package com.adups.fota.query;

import android.content.Context;
import android.text.Html;
import android.text.TextUtils;

import com.adups.fota.MyApplication;
import com.adups.fota.bean.LanguageBean;
import com.adups.fota.bean.PolicyBean;
import com.adups.fota.bean.VersionBean;
import com.adups.fota.config.Const;
import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.manager.NotificationManager;
import com.adups.fota.utils.FileUtil;
import com.adups.fota.utils.JsonUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PreferencesUtils;
import com.adups.fota.utils.StorageUtil;

import java.util.HashMap;
import java.util.List;

public class QueryInfo {

    // 检测通知类型 :  [0:通知栏 1:桌面图标 2:弹窗 3: 无]
    public static final String QUERY_NOTICE_TYPE = "query_notice_type";
    // 检测通知是否常驻: [1: 常驻  0: 默认]
    public static final String QUERY_NOTICE_RESIDENT = "query_notice_resident";
    // [1,0] 1为自动下载，0为否
    public static final String DOWNLOAD_AUTO = "download_auto";
    // [1,0] 1为仅wifi,0为否
    public static final String DOWNLOAD_WIFI = "download_wifi";
    // [aaa#bbb#ccc]
    public static final String DOWNLOAD_PATH = "download_path";
    //1为清除缓存，0则不清除 默认值为0
    public static final String CLEAR_CACHE = "clear_cache";
    //1为只下载外置T卡，2为只下载内置存储 0为忽略 默认值为0
    public static final String DOWNLOAD_PATH_SERVER = "download_path_server";

    public static final int DOWNLOAD_PATH_IGNORE = 0;
    public static final int DOWNLOAD_PATH_OUTSIDE = 1;
    public static final int DOWNLOAD_PATH_INSIDE = 2;

    // [aaa#bbb#ccc]
    public static final String INSTALL_TIME = "install_time";
    // 安装通知类型 :  [0:通知栏 1:桌面图标 2:弹窗 3: 无]
    public static final String INSTALL_NOTICE_TYPE = "install_notice_type";
    // 安装通知是否常驻: [1: 常驻  0: 默认]
    public static final String INSTALL_NOTICE_RESIDENT = "install_notice_resident";
    // [1,0]
    public static final String INSTALL_FORCED = "install_forced";

    public static final String INSTALL_RESULT_POP = "install_result_pop";
    //升级电量
    public static final String INSTALL_BATTERY = "install_battery";

    private static QueryInfo mQuery = null;
    private String nowLanguage="";
    private HashMap<String, PolicyBean> mPolicyMap = new HashMap<>();
    private VersionBean version;

    private QueryInfo(Context context) {
        this.version = JsonUtil.jsonObj(FileUtil.readInternalFile(context, Const.VERSION_FILE), VersionBean.class);
        if (version != null) {
            updatePolicy(version.getPolicy());
        }
    }

    public static QueryInfo getInstance() {
        if (mQuery == null) {
            synchronized (QueryInfo.class) {
                if (mQuery == null) {
                    mQuery = new QueryInfo(MyApplication.getAppContext());
                }
            }
        }
        return mQuery;
    }

    public VersionBean getVersionInfo() {
        LogUtil.d("getVersionInfo,version="+version);
        try {
            if (version == null) {
                version = JsonUtil.jsonObj(FileUtil.readInternalFile(MyApplication.getAppContext(), Const.VERSION_FILE), VersionBean.class);
                if (version != null)
                    updatePolicy(version.getPolicy());
            }
        } catch (Exception e) {
            LogUtil.d("getVersionInfo,Exception e="+e.getMessage());
            e.printStackTrace();
        }
        return version;
    }

    public void update(Context context, VersionBean model) {
        synchronized (this) {
            if (model != null) {
                this.version = model;
                updatePolicy(version.getPolicy());
                initDownloadPath(context);
            }
        }
    }

    /*清除版本信息*/
    public void reset(Context context) {
        synchronized (this) {
            try {
                LogUtil.d("clear version txt");
                /*清空下载路径*/
                StorageUtil.deleteErrFile(context, StorageUtil.getPackagePathName(context));
                PreferencesUtils.putString(context, Setting.UPDATE_PACKAGE_PATH, "");
                NotificationManager.getInstance().cancel(context, NotificationManager.NOTIFY_NEW_VERSION);
                NotificationManager.getInstance().cancel(context, NotificationManager.NOTIFY_DL_COMPLETED);
                NotificationManager.getInstance().cancel(context, NotificationManager.NOTIFY_CONTINUE_RESET);
                version = null;
                mPolicyMap.clear();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                FileUtil.deleteFile(context.getFilesDir().getPath() + "/" + Const.VERSION_FILE);
                Status.setVersionStatus(context, Status.STATE_QUERY_NEW_VERSION);
            }
        }
    }

    /**
     * 从键中获取值
     * 示例: boolean  install_auto =  getPolicy("install_auto",Boolean.class)
     *
     * @param key policy keyword  { @linkplain #NOTICE_TYPE#INSTALL_BATTERY }
     * @param cls class Name   eg: String.class, Integer.class Boolean.class
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getPolicyValue(String key, Class<T> cls) {
        try {
            synchronized (this) {
                PolicyBean policy = mPolicyMap.get(key);
                if (cls.equals(String.class)) {
                    return (policy != null) ? (T) policy.getValue() : null;
                } else if (cls.equals(Integer.class)) {
                    return (policy != null) ? (T) Integer.valueOf(policy.getValue()) : (T) Integer.valueOf(0);
                } else if (cls.equals(Boolean.class)) {
                    return (policy != null) ? (T) (Boolean.valueOf(1 == Integer.valueOf(policy.getValue()))) : (T) Boolean.valueOf(false);
                }
            }
        } catch (NumberFormatException e) {
            return (T) Integer.valueOf(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String[] getPolicyValueArray(String key) {
        synchronized (this) {
            PolicyBean policy = mPolicyMap.get(key);
            return (policy == null || policy.getValue() == null) ? null : policy.getValue().split("#");
        }
    }

    private void updatePolicy(List<PolicyBean> policyBeans) {
        if (policyBeans != null) {
            mPolicyMap.clear();
            for (PolicyBean policy : policyBeans) {
                mPolicyMap.put(policy.getKey(), policy);
            }
            PreferencesUtils.putBoolean(MyApplication.getAppContext(),
                    Setting.FOTA_INSTALL_RESULT_POP, getPolicyValue(QueryInfo.INSTALL_RESULT_POP, Boolean.class));
        }
    }

    private void initDownloadPath(Context context) {
        // 下载路径
        String download_path = getPolicyValue(QueryInfo.DOWNLOAD_PATH, String.class);
        String[] temp;
        if (!TextUtils.isEmpty(download_path) && (download_path.contains("#"))) {
            PreferencesUtils.putString(context, Setting.FOTA_DOWNLOAD_PATH, download_path);
            temp = download_path.split("#");
            if (temp.length == 3) {
                StorageUtil.setUpgradePath(temp[0], temp[1], temp[2]);
            }
        }
    }

    public String getReleaseNotes(Context context,boolean needHtml) {
        String data = null;
        VersionBean bean = getVersionInfo();
        if (bean != null) {
            String mLanguageStr = context.getResources().getConfiguration().locale.getLanguage();
            String country = context.getResources().getConfiguration().locale.getCountry();
            int index = 0;
            if ("zh".equals(mLanguageStr)) {
                mLanguageStr = mLanguageStr + "_" + country;
            }
            List<LanguageBean> languages = version.getReleaseNotes();
            if (languages != null && languages.size() > 0) {
                int language_size = languages.size();
                if (language_size == 1) {
                    // 获取默认语言
                    nowLanguage = languages.get(0).getCountry();
                    data = languages.get(0).getContent().replaceAll("#FFFFFF", "#434343");
                } else {
                    for (int i = 0; i < language_size; i++) {
                        if (mLanguageStr.contains(languages.get(i).getCountry())) {
                            index = i;
                            break;
                        }
                    }
                    nowLanguage = languages.get(index).getCountry();
                    data = languages.get(index).getContent().replaceAll("#FFFFFF", "#434343");
                }
            }
        }
        if (!TextUtils.isEmpty(data)){
            if (needHtml) return data;
            else return Html.fromHtml(data).toString();
        }
        return null;
    }

    public String getNowLanguage(){
        return nowLanguage;
    }

}

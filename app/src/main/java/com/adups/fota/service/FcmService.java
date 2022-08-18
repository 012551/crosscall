package com.adups.fota.service;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.adups.fota.GoogleOtaClient;
import com.adups.fota.MyApplication;
import com.adups.fota.R;
import com.adups.fota.bean.EventMessage;
import com.adups.fota.bean.FcmDataBean;
import com.adups.fota.bean.FcmMessageBean;
import com.adups.fota.config.Const;
import com.adups.fota.config.Event;
import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.manager.NotificationManager;
import com.adups.fota.manager.SpManager;
import com.adups.fota.query.QueryActivate;
import com.adups.fota.query.QueryInfo;
import com.adups.fota.query.QueryVersion;
import com.adups.fota.report.ReportData;
import com.adups.fota.request.RequestParam;
import com.adups.fota.utils.EncryptUtil;
import com.adups.fota.utils.JsonUtil;
import com.adups.fota.utils.LogUtil;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.reflect.TypeToken;

import org.greenrobot.eventbus.EventBus;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class FcmService extends FirebaseMessagingService {

    public static final int NOTIFY_FCM_MESSAGE = 101;
    public static final int NOTIFY_FCM_REPORT_FILE_PATH = 102;
    public static final int NOTIFY_FCM_REPORT_STORAGE_SPACE = 103;
    public static final int NOTIFY_FCM_REPORT_LOG = 104;
    private static final int FCM_MESSAGE = 1;
    private static final int FCM_ORDER = 2;
    private static final int BUTTON_FLAG_HOME_PAGE = 1;
    private static final int BUTTON_FLAG_WHITE_LIST = 2;
    private static final int BUTTON_FLAG_PRIVACY = 3;
    private static final int ORDER_CHECK = 1;
    private static final int ORDER_CLEAR = 2;
    private static final int ORDER_REPORT_FILE_PATH = 3;
    private static final int ORDER_REPORT_STORAGE_SPACE = 4;
    private static final int ORDER_REPORT_LOG = 5;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Map<String, String> map = remoteMessage.getData();
        if (map != null) {
            try {
                dealWithMessage(this, JsonUtil.map2Json(map));
            } catch (Exception e) {
                LogUtil.d(e.getMessage());
                QueryVersion.getInstance().onQueryScheduleTask(this);
            }
        }
    }

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        if (!TextUtils.isEmpty(s)) {
            SpManager.setFcmId(s);
            LogUtil.d("onNewToken");
            if (QueryActivate.isOverActivateTime(this)) {
                QueryVersion.getInstance().onQueryScheduleTask(this);
            }
        }
    }

    private void dealWithMessage(Context context, String message) throws Exception {
        if (TextUtils.isEmpty(message)) return;
        LogUtil.d("fcm message : " + message);
        FcmMessageBean bean = JsonUtil.jsonObj(message, FcmMessageBean.class);
        if (bean == null ||
                (bean.getButton() == BUTTON_FLAG_PRIVACY && !MyApplication.isBootExit()))//跳转开机向导，但是没有安装开机向导
            return;
        switch (bean.getType()) {
            case FCM_MESSAGE:
                FcmDataBean dataBean = getDataBean(context, bean);
                if (dataBean != null)
                    NotificationManager.getInstance().showNotification(context, NOTIFY_FCM_MESSAGE,
                            dataBean.getTitle(), dataBean.getText(),
                            getFcmPendingIntent(context, bean), getNotify(bean));
                break;
            case FCM_ORDER:
                int order = Integer.valueOf(EncryptUtil.desDecode(bean.getData(), bean.getId()));
                LogUtil.d("order : " + order);
                switch (order) {
                    case ORDER_CLEAR:
                        LogUtil.d("execute clear cache ");
                        Status.resetFactory(context);
                        SpManager.setCacheUrl(QueryInfo.getInstance().getVersionInfo().getDeltaUrl());
                        EventBus.getDefault().post(new EventMessage(Event.QUERY, Event.QUERY_INIT_VERSION, 0, 0, null));
                        ReportData.postDownload(context, ReportData.CHECK_STATUS_CLEAN_CACHE, 0);
                        QueryVersion.getInstance().onQueryScheduleTask(this);
                        break;
                    case ORDER_REPORT_FILE_PATH:
                        switch (Status.getVersionStatus(context)) {
                            case Status.STATE_NEW_VERSION_READY:
                            case Status.STATE_DOWNLOADING:
                            case Status.STATE_PAUSE_DOWNLOAD:
                            case Status.STATE_DL_PKG_COMPLETE:
                                NotificationManager.getInstance().showNotification(context, NOTIFY_FCM_REPORT_FILE_PATH,
                                        context.getString(R.string.analysis),
                                        context.getString(R.string.content),
                                        getFcmPendingIntent(context, bean), getNotify(bean));
                        }
                        break;
                    case ORDER_REPORT_STORAGE_SPACE:
                        NotificationManager.getInstance().showNotification(context, NOTIFY_FCM_REPORT_STORAGE_SPACE,
                                context.getString(R.string.analysis),
                                context.getString(R.string.content),
                                getFcmPendingIntent(context, bean), getNotify(bean));
                        break;
                    case ORDER_REPORT_LOG:
                        NotificationManager.getInstance().showNotification(context, NOTIFY_FCM_REPORT_LOG,
                                context.getString(R.string.analysis),
                                context.getString(R.string.content),
                                getFcmPendingIntent(context, bean), getNotify(bean));
                        break;
                    default:
                        QueryVersion.getInstance().onQueryScheduleTask(context);
                        break;
                }
                break;
            default:
                QueryVersion.getInstance().onQueryScheduleTask(context);
                break;
        }
    }

    private FcmDataBean getDataBean(Context context, FcmMessageBean bean) {
        List<FcmDataBean> fcmDataBeans = JsonUtil.json2List(bean.getData(), new TypeToken<List<FcmDataBean>>() {
        }.getType());
        int size = fcmDataBeans == null ? 0 : fcmDataBeans.size();
        if (size == 0) return null;
        if (size > 1) {
            String locale = getLocale(context);
            if (!TextUtils.isEmpty(locale))
                for (FcmDataBean fcmDataBean : fcmDataBeans) {
                    if (locale.contains(fcmDataBean.getLang())) {
                        return fcmDataBean;
                    }
                }
        }
        return fcmDataBeans.get(0);
    }

    private String getLocale(Context context) {
        String language = context.getResources().getConfiguration().locale.getLanguage();
        String country = context.getResources().getConfiguration().locale.getCountry();
        if ("zh".equals(language)) {
            language = language + "_" + country;
        }
        return language;
    }

    private PendingIntent getFcmPendingIntent(Context context, FcmMessageBean bean) throws Exception {
        Intent intent;
        switch (bean.getButton()) {
            case BUTTON_FLAG_WHITE_LIST:
                intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
                break;
            case BUTTON_FLAG_PRIVACY:
                intent = new Intent();
                ComponentName componentName = new ComponentName(Const.FOTA_BOOT_PACKAGE_NAME, Const.FOTA_BOOT_ACTIVITY_NAME);
                intent.setComponent(componentName);
                intent.putExtra("param", (Serializable) RequestParam.getReportEuParam(this));
                break;
            default:
                intent = new Intent(context, GoogleOtaClient.class);
                break;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int notifyId = 0;
        switch (bean.getType()) {
            case FCM_ORDER:
                switch (Integer.valueOf(EncryptUtil.desDecode(bean.getData(), bean.getId()))) {
                    case ORDER_REPORT_FILE_PATH:
                        notifyId = NOTIFY_FCM_REPORT_FILE_PATH;
                        break;
                    case ORDER_REPORT_STORAGE_SPACE:
                        notifyId = NOTIFY_FCM_REPORT_STORAGE_SPACE;
                        break;
                    case ORDER_REPORT_LOG:
                        notifyId = NOTIFY_FCM_REPORT_LOG;
                        break;
                }
                break;
            default:
                notifyId = NOTIFY_FCM_MESSAGE;
                break;
        }
        intent.setAction("com.adups.fota");
        intent.putExtra(Setting.INTENT_PARAM_NOTIFY_ID, notifyId);
        intent.putExtra(Setting.INTENT_PARAM_TASK_ID, bean.getId());
        return PendingIntent.getActivity(context, notifyId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private boolean getNotify(FcmMessageBean bean) {
        return bean.getNotify() != 0;
    }

}

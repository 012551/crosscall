package com.adups.fota.report;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.adups.fota.bean.ReportBean;
import com.adups.fota.config.ServerApi;
import com.adups.fota.config.Setting;
import com.adups.fota.manager.DatabaseManager;
import com.adups.fota.request.RequestParam;
import com.adups.fota.utils.JsonUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.OkHttpUtil;
import com.adups.fota.utils.PreferencesUtils;
import com.adups.fota.utils.StorageUtil;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 重构数据上报，统一上报数据入口，统一删除数据
 * Created by brave on 2017/1/20.
 */
public class ReportManager {

    public static final long REPORT_INVALID_TIME = 1000 * 60 * 60 * 6; //阻止无效数据上报
    public static final String DOWN_STATUS_CAUSE_NOT_ENOUGH = "down_status_cause_not_enough";
    public static final String DOWN_STATUS_CAUSE_UNAUTO = "down_status_cause_unauto";
    private static final long REPORT_RETRY_TIME = 1000 * 60 * 60 * 2;
    //防止频繁上报限制
    private static final int REPORT_MAX = 100;
    private static ReportManager mInstance = null;

    private ReportManager() {
    }

    public static ReportManager getInstance() {
        if (mInstance == null) {
            synchronized (ReportManager.class) {
                if (mInstance == null) {
                    mInstance = new ReportManager();
                }
            }
        }
        return mInstance;
    }

    public void reportData(Context context) {
        if (!isRunnable(context, "scheduleReportData", REPORT_RETRY_TIME)) {
            LogUtil.d("not arrive report data schedule!");
            return;
        }
        scheduleReportData(context);
    }

    private void scheduleReportData(Context mContext) {
        reportDataFromDB(mContext);
    }

    private void insertAnalytics(String type, String result) {
        LogUtil.d("insert data to db, type = " + type + "; result = " + result);
        DatabaseManager.getInstance().addData(type, result);
    }

    private void reportDataFromDB(Context context) {
        List<ReportBean> list = DatabaseManager.getInstance().getData();
        if (list == null || list.isEmpty()) return;
        LogUtil.d("record items size = " + list.size());
        List<String> result = new ArrayList<>();
        for (ReportBean item : list) {
            if (item.getAction().equalsIgnoreCase(ReportData.TYPE_FCM))
                fcmReport(context, (Map<String, String>) JsonUtil.json2Object(item.getResult()));
            result.add(item.getResult());
        }
        String url = PreferencesUtils.getString(context, Setting.QUERY_URL, ServerApi.SERVER_DOMAIN) + ServerApi.REPORT;
        Request request = new Request.Builder()
                .url(url)
                .post(RequestParam.getRequestBody(context, result, true))
                .build();
        LogUtil.d("http URL = " + url);
        OkHttpUtil.enqueue(request, new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                LogUtil.d(e.getMessage());
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String responseBody=response.body().string();
                LogUtil.d("response status_code = " + response.code() + "; isSuccessful = " + response.isSuccessful()
                        + "; body() = " + responseBody);
                if (response.isSuccessful() || !TextUtils.isEmpty(responseBody) && responseBody.contains("ok")) {
                    DatabaseManager.getInstance().deleteContent();
                    StorageUtil.deleteErrorLogFile();
                }
            }
        });
    }

    public void reportDataOrInsertDB(Context context, final String type, final String content, boolean needLog) {
        LogUtil.d("type = " + type + "; result = " + content);
        List<String> result = new ArrayList<>();
        result.add(content);
        String url = PreferencesUtils.getString(context, Setting.QUERY_URL, ServerApi.SERVER_DOMAIN) + ServerApi.REPORT;
        Request request = new Request.Builder()
                .url(url)
                .post(RequestParam.getRequestBody(context, result, needLog))
                .build();
        LogUtil.d("http URL = " + url);
        OkHttpUtil.enqueue(request, new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                LogUtil.d(e.getMessage());
                insertAnalytics(type, content);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String responseBody=response.body().string();
                LogUtil.d("response status_code = " + response.code() + "; isSuccessful = " + response.isSuccessful()
                        + "; body() = " + responseBody);
                if ((response.isSuccessful() || !TextUtils.isEmpty(responseBody) && responseBody.contains("ok"))) {
                    StorageUtil.deleteErrorLogFile();
                    LogUtil.d("reportData success!!!");
                } else {
                    insertAnalytics(type, content);
                }
            }
        });
    }

    public void fcmReport(Context context, Map<String, String> param) {
        if (param == null) return;
        reportOrInsertDb(PreferencesUtils.getString(context, Setting.QUERY_URL, ServerApi.SERVER_DOMAIN)
                + ServerApi.FCM_REPORT, RequestParam.getFcmReportParam(context, param));
    }

    private void reportOrInsertDb(final String url, final Map<String, String> param) {
        if (param == null) return;
        MultipartBuilder builder = new MultipartBuilder();
        for (Map.Entry<String, String> entry : param.entrySet()) {
            String key = entry.getKey();
            if (key.equalsIgnoreCase(RequestParam.LOG)) {
                builder.addFormDataPart(key, "log.txt",
                        RequestBody.create(MediaType.parse("text/plain"), new File(entry.getValue())));
            } else
                builder.addFormDataPart(key, entry.getValue());
        }
        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .build();
        LogUtil.d("http URL = " + url);
        OkHttpUtil.enqueue(request, new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                LogUtil.d("http request fail,message:" + e.getMessage());
                insertAnalytics(ReportData.TYPE_FCM, JsonUtil.map2Json(param));
            }

            @Override
            public void onResponse(Response response) throws IOException {
                LogUtil.d("response status_code = " + response.code()
                        + "; isSuccessful = " + response.isSuccessful()
                        + "; body() = " + response.body().string());
            }
        });
    }

    public synchronized boolean isRunnable(Context mContext, String tag, long minDuration) {
        long currentTimestamp = System.currentTimeMillis();
        long lastTimestamp = mContext.getSharedPreferences("runstats", 0)
                .getLong(tag + "CHECKTIME", 0);
        if ((currentTimestamp - lastTimestamp) < 0) {
            refreshRunStats(mContext, tag);
            return true;
        }
        boolean isOverTime = ((currentTimestamp - lastTimestamp) > minDuration);
        if (isOverTime) {
            refreshRunStats(mContext, tag);
        }
        return isOverTime;
    }

    private synchronized void refreshRunStats(Context mContext, String tag) {
        long currentTimestamp = System.currentTimeMillis();
        SharedPreferences.Editor editor = mContext.getSharedPreferences("runstats", 0).edit();
        editor.putLong(tag + "CHECKTIME", currentTimestamp);
        editor.apply();
    }

}

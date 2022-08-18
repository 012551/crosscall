package com.adups.fota.utils;

import com.adups.fota.MyApplication;
import com.adups.fota.request.RequestResult;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OkHttpUtil {

    private static OkHttpClient okHttpClient;
    private static List<Call> calls;
    private Call mCall = null;

    private synchronized static OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient();
            okHttpClient.setConnectTimeout(20, TimeUnit.SECONDS);
            okHttpClient.setReadTimeout(30, TimeUnit.SECONDS);
            okHttpClient.setWriteTimeout(30, TimeUnit.SECONDS);
            okHttpClient.setFollowRedirects(true);
            okHttpClient.setRetryOnConnectionFailure(true);
            okHttpClient.setFollowSslRedirects(true);
            okHttpClient.setHostnameVerifier(AduHostnameVerifier.INSTANCE);
        }
        return okHttpClient;
    }

    private static Call getCall(Request request) {
        Call call = getOkHttpClient().newCall(request);
        if (calls == null)
            calls = new ArrayList<>();
        if (!calls.contains(call))
            calls.add(call);
        return call;
    }

    /*该不会开启异步线程*/
    public static Response execute(Request request) throws IOException {
        Response response = getCall(request).execute();
        if (response != null && response.isSuccessful()) {
            return response;
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }

    /* 该不会开启异步线程。带参返回方法*/
    public static Response execute(Request request, RequestResult result) throws IOException {
        Response response = getCall(request).execute();
        if (response != null && response.isSuccessful()) {
            return response;
        } else {
            result.setStatus_code(response.code());
            throw new IOException("Unexpected code " + response);
        }
    }

    /*开启异步线程访问网络*/
    public static void enqueue(Request request, Callback responseCallback) {
        getCall(request).enqueue(responseCallback);
    }

    public static void cancelRequest() {
        if (calls != null && !calls.isEmpty()) {
            for (Call call : calls)
                call.cancel();
            calls.clear();
        }
    }

    public static void resetDNS() {
        Security.setProperty("networkaddress.cache.ttl", String.valueOf(0));
        Security.setProperty("networkaddress.cache.negative.ttl", String.valueOf(0));
    }

    //获取文件的大小
    public long getFileLength(String url) throws IOException {
        Request request = new Request.Builder().url(url).addHeader("Connection","close").build();
        Response response = getOkHttpClient().newCall(request).execute();
        long length = response.body().contentLength();
        return length;
    }

    public InputStream downloadFile(String url, long begin, int end) throws IOException{
        String mRange = "bytes=";
        if (begin >=0 ) {
            mRange = mRange + begin + "-";
        }

        if (end > 0 && end > begin) {
            mRange = mRange + end;
        }

        Request request = new Request.Builder().url(url).
                addHeader("Connection","close").
                addHeader("Range", mRange).build();

        mCall = getOkHttpClient().newCall(request);
        Response response = mCall.execute();
        if (response.isSuccessful()) {
            return response.body().byteStream();
        }else LogUtil.d("downloadFile,http error code="+response.code());
        return null;
    }

    public void downloadCancel() {
        if (mCall != null) {
            mCall.cancel();
        }
    }
}

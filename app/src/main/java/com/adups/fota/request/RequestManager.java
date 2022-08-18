package com.adups.fota.request;

import android.content.Context;

import com.adups.fota.callback.RequestCallback;
import com.adups.fota.config.Event;
import com.adups.fota.config.ServerApi;
import com.adups.fota.utils.EncryptUtil;
import com.adups.fota.utils.LogUtil;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RequestManager {

    public static void report(Context context, RequestCallback callback) {
        encryptAsyncRequest(ServerApi.EU_REPORT, RequestParam.getReportEuParam(context), callback);
    }

    public static RequestResult queryRequest(Context context, String url, int queryType) {
        return encryptRequest(url, RequestParam.queryParams(context, queryType));
    }

    private static RequestResult encryptRequest(String url, Map<String, String> param) {
        StringBuilder stringBuilder = new StringBuilder();
        if (param != null)
            for (Map.Entry<String, String> entry : param.entrySet())
                stringBuilder.append("&").append(entry.getKey()).append("=").append(entry.getValue());
        String result = stringBuilder.toString();
        LogUtil.d("params : " + result);
        RequestResult requestResult = new RequestResult();
        requestResult.setStart_time(System.currentTimeMillis());
        Map<String, String> encryptParam = new HashMap<>();
        String encrypt = EncryptUtil.encode(result);
        encryptParam.put(RequestParam.KEY, encrypt);
        encryptParam.put(RequestParam.SHA_KEY, EncryptUtil.sha256(encrypt));
        LogUtil.d("encryptParams : " + encryptParam.toString());
        try {
            Response response = HttpRequest.post(url, encryptParam);
            requestResult.setIsSuccess(response.isSuccessful());
            requestResult.setStatus_code(response.code());
            requestResult.setResult(response.body().string());
            requestResult.setEnd_time(System.currentTimeMillis());
        } catch (IOException e) {
            return requestResult.setIsSuccess(false).setError_code(Event.ERROR_IO).setEnd_time(System.currentTimeMillis())
                    .setError_message(e.getMessage());
        } catch (Exception e1) {
            return requestResult.setIsSuccess(false).setError_code(Event.ERROR_UNKNOWN).setEnd_time(System.currentTimeMillis())
                    .setError_message(e1.getMessage());
        }
        return requestResult;
    }

    private static void encryptAsyncRequest(String url, Map<String, String> param, RequestCallback callback) {
        StringBuilder stringBuilder = new StringBuilder();
        if (param != null)
            for (Map.Entry<String, String> entry : param.entrySet())
                stringBuilder.append("&").append(entry.getKey()).append("=").append(entry.getValue());
        String result = stringBuilder.toString();
        LogUtil.d("params : " + result);
        Map<String, String> encryptParam = new HashMap<>();
        encryptParam.put(RequestParam.KEY, EncryptUtil.encode(result));
        HttpRequest.asyncPost(url, encryptParam, callback);
    }

}

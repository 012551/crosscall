package com.adups.fota.request;

import com.adups.fota.callback.RequestCallback;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.OkHttpUtil;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.Map;

public class HttpRequest {

    public static Response post(String url, Map<String, String> params) throws IOException {
        FormEncodingBuilder form = new FormEncodingBuilder();
        if (params != null)
            for (Map.Entry<String, String> entry : params.entrySet())
                form.add(entry.getKey(), entry.getValue());
        Request request = new Request.Builder()
                .url(url)
                .post(form.build())
                .build();
        LogUtil.d("http url = " + url);
        return OkHttpUtil.execute(request);
    }

    public static void asyncPost(String url, Map<String, String> params, final RequestCallback callback) {
        FormEncodingBuilder form = new FormEncodingBuilder();
        if (params != null)
            for (Map.Entry<String, String> entry : params.entrySet())
                form.add(entry.getKey(), entry.getValue());
        Request request = new Request.Builder()
                .url(url)
                .post(form.build())
                .build();
        LogUtil.d("http url = " + url);
        OkHttpUtil.enqueue(request, new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                LogUtil.d("http request fail,message:" + e.getMessage());
            }

            @Override
            public void onResponse(Response response) {
                try {
                    LogUtil.d("http request success,response code = " + response.code()
                            + ",response content:" + response.body().string());
                    if (response.isSuccessful()) {
                        if (callback != null)
                            callback.onResponseReturn(response.body().string());
                    } else
                        LogUtil.d("http request error,message:" + response.message());
                } catch (Exception e) {
                    LogUtil.d("http request exception,message:" + e.getMessage());
                }
            }
        });
    }

}

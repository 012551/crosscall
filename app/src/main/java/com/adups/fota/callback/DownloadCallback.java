package com.adups.fota.callback;

/**
 * Created by xw on 15-12-25.
 */
public interface DownloadCallback {

    void onFail(String tag, int reason, String message);

    void onProgress(String tag, long total_size, long download_size);

    void onSuccess(String tag, String filePath);

    void onStart(String tag);

}

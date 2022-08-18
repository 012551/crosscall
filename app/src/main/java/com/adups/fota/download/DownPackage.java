package com.adups.fota.download;

import android.content.Context;
import android.text.TextUtils;


import com.adups.fota.MyApplication;
import com.adups.fota.R;
import com.adups.fota.bean.DownloadBean;
import com.adups.fota.bean.EventMessage;
import com.adups.fota.bean.VersionBean;
import com.adups.fota.config.Const;
import com.adups.fota.config.Event;
import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.manager.NoticeManager;
import com.adups.fota.manager.NotificationManager;
import com.adups.fota.manager.SpManager;
import com.adups.fota.query.QueryInfo;
import com.adups.fota.report.ReportData;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.NetWorkUtil;
import com.adups.fota.utils.OkHttpUtil;
import com.adups.fota.utils.PreferencesUtils;
import com.adups.fota.utils.StorageUtil;
import com.adups.fota.utils.SystemSettingUtil;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import static com.adups.fota.manager.NoticeManager.isTopActivity;

public class DownPackage {

    private static final int DEFAULT_CHUNK_SIZE =1024 * 8 ;
    private static final long DEFAULT_REFREASH_TIME =1000 ;
    private static DownPackage mDownPackage;
    private VersionBean model;
    private Thread mThread = null;
    private int downloadId, progress;
    private String downloadFilePath;
    private boolean isDownloading, isPaused;
    private DownloadBean downloadInfo;
    private OkHttpUtil okhttp = null;

    private DownPackage() {
        progress = SpManager.getDownloadPercent();

        downloadInfo=new DownloadBean();
        downloadInfo.setRetryCount(3);

        if (okhttp == null) {
            okhttp = new OkHttpUtil();
        }

        if (mThread == null) {
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    download();
                }
            });
        }

    }

    public static DownPackage getInstance() {
        if (mDownPackage == null) {
            synchronized (DownPackage.class) {
                if (mDownPackage == null) {
                    mDownPackage = new DownPackage();
                }
            }
        }
        return mDownPackage;
    }

    //初始化
    public void init() {
        model = QueryInfo.getInstance().getVersionInfo();
        if (TextUtils.isEmpty(downloadInfo.getDownloadUrl()) || !downloadInfo.getDownloadUrl().equalsIgnoreCase(model.getDeltaUrl())) {
            downloadInfo.setDownloadTotalSize(0);
        }
        downloadInfo.setDownloadUrl(model.getDeltaUrl());
        downloadInfo.setDownloadDir(StorageUtil.getPackagePathName(MyApplication.getAppContext()));
        downloadInfo.setDownloadFileName(Const.PACKAGE_NAME);
        File folder = new File(downloadInfo.getDownloadDir());
        if (!folder.exists()) {
            folder.mkdirs();
        }
        if (!folder.exists()) {
            downloadErrorCallback(MyApplication.getAppContext().getString(R.string.sdcard_crash_dir_un_build));
            LogUtil.d(downloadInfo.getDownloadDir() + " is illness");
            return ;
        }
        String downloadPath = StorageUtil.getPackagePathName(MyApplication.getAppContext());
        LogUtil.d("download url = "+downloadInfo.getDownloadUrl()+",,package size ="+model.getFileSize());
        PreferencesUtils.putString(MyApplication.getAppContext(), Setting.UPDATE_PACKAGE_PATH, downloadPath);
    }

    private long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public void execute() {
        LogUtil.d("thread state = " + mThread.getState());

        if (mThread.getState() == Thread.State.NEW) {
            mThread.start();
        } else if (mThread.getState() == Thread.State.TERMINATED) {
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    download();
                }
            });
            mThread.start();
        }
    }

    private synchronized void download() {
        init();
        int retry = 0;
        while (retry < downloadInfo.getRetryCount()) {
            retry ++;
            LogUtil.d("retry = " + retry + "; max retry = " + downloadInfo.getRetryCount());
            if (okhttp != null) {
                try {
                    downloadInfo.setTagFileSize(okhttp.getFileLength(downloadInfo.getDownloadUrl()));

                    File mFile = new File(downloadInfo.getDownloadDir() + downloadInfo.getDownloadFileName());
                    if (mFile.isFile()) {
                        LogUtil.d("download size = " + downloadInfo.getDownloadTotalSize() + "; file size = " + mFile.length() + "; isInterrupted = " + mThread.isInterrupted());
                        if (mFile.length() > DEFAULT_CHUNK_SIZE) {
                            downloadInfo.setDownloadTotalSize(mFile.length());
                        }
                    }

                    if (downloadInfo.getTagFileSize() == downloadInfo.getDownloadTotalSize()) {
                        LogUtil.d("");
                        downloadSuccessCallback();
                        return;
                    }

                    InputStream is = okhttp.downloadFile(downloadInfo.getDownloadUrl(),downloadInfo.getDownloadTotalSize(),-1);

                    if (is != null) {
                        isDownloading = true;
                        SpManager.recordDownloadStartTime(getCurrentTime());
                        EventBus.getDefault().post(new EventMessage(Event.DOWNLOAD, Event.DOWNLOAD_START, 0, 0, null));

                        RandomAccessFile raf = new RandomAccessFile(downloadInfo.getDownloadDir() + downloadInfo.getDownloadFileName(), "rwd");

                        //跳转到起始位置
                        raf.seek(downloadInfo.getDownloadTotalSize());

                        //把流写入到文件
                        byte[] buffer = new byte[DEFAULT_CHUNK_SIZE];
                        int len = 0;
                        long prevTime = 0L;
                        while((len = is.read(buffer)) != -1){
                            //如果暂停下载  点击暂停 false 就直接return 点击下载true接着下载
                            if(mThread.isInterrupted()){
                                LogUtil.d("mThread interrupted");
                                is.close();
                                raf.close();
                                downloadErrorCallback("");
                                return;//标准线程结束
                            }

                            //写数据
                            raf.write(buffer, 0, len);

                            downloadInfo.setDownloadTotalSize(downloadInfo.getDownloadTotalSize() + len);
                            if (System.currentTimeMillis() - prevTime > DEFAULT_REFREASH_TIME) {
                                prevTime = System.currentTimeMillis();
                                int progress = Long.valueOf((100 * downloadInfo.getDownloadTotalSize()) / downloadInfo.getTagFileSize()).intValue();
                                LogUtil.d("download size = " + downloadInfo.getDownloadTotalSize() + "/" + downloadInfo.getTagFileSize()+",,state="+mThread.getState() );
                                isDownloading = true;
                                if (progress < 0 || progress > 100) return;
                                LogUtil.d(String.valueOf(progress));
                                if (progress < getProgress()) return;
                                setProgress(progress);
                                showProgress(MyApplication.getAppContext());
                            }
                        }
                        is.close();
                        raf.close();
                        if (downloadInfo.getDownloadTotalSize() == downloadInfo.getTagFileSize()) {
                            downloadInfo.setDownloadTotalSize(0);
                            downloadSuccessCallback();
                            return;
                        }else {
                            LogUtil.e("download failed!!");
                            downloadErrorCallback("download failed!!");
                        }
                    } else if (retry == downloadInfo.getRetryCount()) {
                        LogUtil.e("retry is over;download get data failed");
                        downloadErrorCallback("download failed,get data failed");
                    }
                } catch (Exception e) {
                    LogUtil.e("Exception e= "+e.getMessage()+",,state="+mThread.getState());
                    if (e.getMessage().startsWith("Unable to resolve host")) {
                        if ((retry == downloadInfo.getRetryCount())) {
                            LogUtil.e("Unable to resolve host!!");
                            downloadErrorCallback("network failed!");
                        }
                    } else if (e.getMessage().contains("interrupted") || e.getMessage().contains("Socket closed")) {
                        LogUtil.e("Pause or Stop");
                        downloadErrorCallback("");
                        return;
                    } else if (e.getMessage().contains("unexpected end of stream")) {
                        downloadErrorCallback("timeout");
                        return;
                    }else {
                        downloadErrorCallback(e.getMessage());
                        return;
                    }
                    e.printStackTrace();
                }
            }
        }
    }

    private void downloadSuccessCallback() {
        isDownloading = false;
        downloadFinish(MyApplication.getAppContext());
    }

    private void downloadErrorCallback( String error) {
        isDownloading = false;
        doDownloadFail(MyApplication.getAppContext(), error);
        OkHttpUtil.resetDNS();//清空dns
    }


    private void recordUseTime() {
        SpManager.recordDownloadUseTime(getCurrentTime() - SpManager.getDownloadStartTime());
    }

    private long getTakeTime() {
        return SpManager.getDownloadUseTime();
    }

    public void pause(Context mContext) {
        LogUtil.d("pause,okhttp="+okhttp);
        if (okhttp != null) {
            mThread.interrupt();
            okhttp.downloadCancel();
        }
    }

    public void cancel(Context mContext) {
        LogUtil.d(getClass().getSimpleName());
        if (okhttp != null) {
            mThread.interrupt();
            okhttp.downloadCancel();
        }
        if (downloadInfo!=null) {
            downloadInfo.setDownloadTotalSize(0);
        }
        recordUseTime();
        progress = 0;
        SpManager.setDownloadPercent(0);
        ReportData.postDownload(mContext, ReportData.DOWN_STATUS_CANCEL, getTakeTime());
        StorageUtil.deleteErrFile(mContext, StorageUtil.getPackagePathName(mContext));
    }

    public boolean isPause() {
        return isPaused;
    }

    public boolean isDownloading() {
        return mThread!=null && mThread.getState()== Thread.State.RUNNABLE;
    }

    private void showProgress(Context mContext) {
//        if (DeviceInfoUtil.getInstance().getCanUse() == 0){
//            pause(mContext);
//            NotificationManager.getInstance().showDownloadProgress(mContext, mContext.getString(R.string.notification_content_title), "System update is disabled by your administrator.");
//            return;
//        }
        SpManager.setDownloadPercent(progress);
        String content = mContext.getString(R.string.download_progress) + progress + "%";
        NotificationManager.getInstance().cancel(mContext,NotificationManager.NOTIFY_CONTINUE_RESET);

        boolean isTop = isTopActivity("com.adups.fota");
        if (!isTop) {
			LogUtil.d( "enter istop=" +isTop);
            NotificationManager.getInstance().showDownloadProgress(mContext, mContext.getString(R.string.notification_content_title), content);
        }else {
            NotificationManager.getInstance().cancel(mContext,NotificationManager.NOTIFY_DOWNLOADING);
        }
        EventBus.getDefault().post(new EventMessage(Event.DOWNLOAD, Event.DOWNLOAD_PROGRESS, progress, 0, null));
        LogUtil.d("downloading package percent = " + content);
    }

    private void downloadFinish(Context mContext) {
        LogUtil.d("download is completed");
        recordUseTime();
        progress = 0;
        SpManager.setDownloadPercent(0);
        Status.setDownloadCompleted(mContext);

        //向系统写入下载完成的参数
        try{
            SystemSettingUtil.putInt("fota_downloaded",1);
        }catch (Exception e){}
        LogUtil.d("fota_downloaded:"+ SystemSettingUtil.getInt("fota_downloaded",0));

        //上报下载完成
        ReportData.postDownload(mContext, ReportData.DOWN_STATUS_FINISH, getTakeTime());
        Status.downloadCompletedInstall(mContext);
    }

    public long size() {
        return (model != null) ? model.getFileSize() : 0;
    }

    private void doDownloadFail(Context mContext, String errorMessage) {
        recordUseTime();
        LogUtil.d("STATE_PAUSE_DOWNLOAD,pkg download fail reason : " + errorMessage);
        Status.setVersionStatus(mContext, Status.STATE_PAUSE_DOWNLOAD);
        EventBus.getDefault().post(new EventMessage(Event.DOWNLOAD, Event.DOWNLOAD_FAIL, 0, 0, errorMessage));
        ReportData.postDownload(mContext, ReportData.DOWN_STATUS_CAUSE_FAIL + "#" + errorMessage, getTakeTime());
        //2016年12月8日13:54:16 通知栏也显示暂停状态
        NoticeManager.downloadPause(mContext);
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
}

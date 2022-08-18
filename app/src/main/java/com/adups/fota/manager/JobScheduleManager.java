package com.adups.fota.manager;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.adups.fota.config.TaskID;
import com.adups.fota.service.CustomActionIntentService;
import com.adups.fota.service.CustomActionService;
import com.adups.fota.utils.LogUtil;

/*android提供的任务调度服务，特殊设备上并不会按照设定的最小时间触发任务，如投影仪，车机，此设备状态并非常用，任务调度时间会变长，手机会正常触发*/
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class JobScheduleManager extends JobService {

    public static final int CHANGE_NETWORK_JOB_ID = 100;
    public static final int DOWNLOAD_JOB_ID = 101;

    public static final long TIME_REFERENCE = 60 * 1000;//一分钟

    private static long CHANGE_NETWORK_MIN_TIME, CHANGE_NETWORK_DEADLINE_TIME;
    private static long DOWNLOAD_MIN_TIME, DOWNLOAD_DEADLINE_TIME;

    private static void initTimes() {
        String[] jobQueryTime = SpManager.getJobQueryTime();
        CHANGE_NETWORK_MIN_TIME = Long.parseLong(jobQueryTime[0]) * TIME_REFERENCE;
        CHANGE_NETWORK_DEADLINE_TIME = Long.parseLong(jobQueryTime[1]) * TIME_REFERENCE;
        LogUtil.d("change network min time : " + CHANGE_NETWORK_MIN_TIME
                + ",change network deadline time : " + CHANGE_NETWORK_DEADLINE_TIME);
        String[] jobDownloadTime = SpManager.getJobDownloadTime();
        DOWNLOAD_MIN_TIME = Long.parseLong(jobDownloadTime[0]) * TIME_REFERENCE;
        DOWNLOAD_DEADLINE_TIME = Long.parseLong(jobDownloadTime[1]) * TIME_REFERENCE;
    }

    public static void scheduleJob(Context context, int jobId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            initTimes();
            ComponentName componentName = new ComponentName(context, JobScheduleManager.class);
            JobInfo.Builder builder = new JobInfo.Builder(jobId, componentName);
            switch (jobId) {
                case CHANGE_NETWORK_JOB_ID:
                    builder.setMinimumLatency(CHANGE_NETWORK_MIN_TIME);
                    builder.setOverrideDeadline(CHANGE_NETWORK_DEADLINE_TIME);
                    break;
                case DOWNLOAD_JOB_ID:
                    builder.setMinimumLatency(DOWNLOAD_MIN_TIME);
                    builder.setOverrideDeadline(DOWNLOAD_DEADLINE_TIME);
                    break;
            }
            builder.setPersisted(true);
            builder.setRequiresCharging(false);
            builder.setRequiresDeviceIdle(false);
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setRequiresStorageNotLow(false);
                builder.setRequiresBatteryNotLow(false);
            }
            LogUtil.d("schedule job id : " + jobId + " ;schedule status : " + jobScheduler.schedule(builder.build()));
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        int jobId = params.getJobId();
        LogUtil.d("start job id : " + jobId);
        switch (jobId) {
            case CHANGE_NETWORK_JOB_ID:
                CustomActionService.enqueueWork(this, TaskID.TASK_QUERY_AUTO);
                break;
            case DOWNLOAD_JOB_ID:
                //原代码此事件无任何操作，暂时空余
                break;
        }
        scheduleJob(this, jobId);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

}

package com.adups.fota.bean;

public class FlagBean {

    private long check_freq;
    private int isFull, isupgrade;
    private String mid, job_schedule_time, job_schedule_downloading_time, sendId;

    public long getCheckFreq() {
        return check_freq;
    }

    public int getIsFull() {
        return isFull;
    }

    public int getIsUpgrade() {
        return isupgrade;
    }

    public String getMid() {
        return mid;
    }

    public String getJobScheduleTime() {
        return job_schedule_time;
    }

    public String getJobScheduleDownloadingTime() {
        return job_schedule_downloading_time;
    }

    public String getSendId() {
        return sendId;
    }
}

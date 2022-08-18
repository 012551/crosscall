package com.adups.fota.bean;

public class ReportDetailBean {

    public String version, time;
    public int type, apn;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getApn() {
        return apn;
    }

    public void setApn(int apn) {
        this.apn = apn;
    }
}

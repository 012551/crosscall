package com.adups.fota.bean;

public class ReportInstallBean extends ReportInstallBaseBean {

    private String status;
    private int forced;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getForced() {
        return forced;
    }

    public void setForced(int forced) {
        this.forced = forced;
    }
}

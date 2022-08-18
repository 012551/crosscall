package com.adups.fota.bean;

public class ReportQueryBean extends ReportDetailBean {

    private String errCode, reason;
    private int check_type, status;

    public String getErrCode() {
        return errCode;
    }

    public void setErrCode(String errCode) {
        this.errCode = errCode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getCheckType() {
        return check_type;
    }

    public void setCheckType(int check_type) {
        this.check_type = check_type;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}

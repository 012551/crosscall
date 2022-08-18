package com.adups.fota.install;

import android.content.Context;

public abstract class BaseInstallVerify {

    public String reason;

    public abstract int verify(Context context, String zipFilePath, String md5Code);

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}



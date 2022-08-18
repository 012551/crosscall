package com.adups.fota.bean;

import java.util.List;

/**
 * Created by xw on 15-12-26.
 */
public class VersionBean {

    private long filesize;
    private int isOldPkg, issilent;
    private List<PolicyBean> policy;
    private List<LanguageBean> releasenotes;
    private String versionName, deltaurl, md5sum, sha;

    public int getIsSilent() {
        return issilent;
    }

    public int getIsOldPkg() {
        return isOldPkg;
    }

    public String getVersionName() {
        return versionName;
    }

    public String getDeltaUrl() {
        return deltaurl;
    }

    public String getMd5sum() {
        return md5sum;
    }

    public long getFileSize() {
        return filesize;
    }

    public List<PolicyBean> getPolicy() {
        return policy;
    }

    public List<LanguageBean> getReleaseNotes() {
        return releasenotes;
    }

    public String getSha() {
        return sha;
    }

}

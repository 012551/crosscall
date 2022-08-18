package com.adups.fota.bean;

public class DownloadBean {

    private boolean segmentDownload;
    private int retryCount, downloadStatus;
    private String downloadUrl, downloadDir, downloadFileName, tagFileName, tagId;
    private long tagFileSize, downloadTotalSize, downloadBlockSize, rangeStart, rangeEnd, downloadSimulateTotalSize;

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getDownloadDir() {
        return downloadDir;
    }

    public void setDownloadDir(String downloadDir) {
        this.downloadDir = downloadDir;
    }

    public String getDownloadFileName() {
        return downloadFileName;
    }

    public void setDownloadFileName(String downloadFileName) {
        this.downloadFileName = downloadFileName;
    }

    public String getTagFileName() {
        return tagFileName;
    }

    public void setTagFileName(String tagFileName) {
        this.tagFileName = tagFileName;
    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public long getTagFileSize() {
        return tagFileSize;
    }

    public void setTagFileSize(long tagFileSize) {
        this.tagFileSize = tagFileSize;
    }

    public long getDownloadTotalSize() {
        return downloadTotalSize;
    }

    public void setDownloadTotalSize(long downloadTotalSize) {
        this.downloadTotalSize = downloadTotalSize;
    }

    public long getDownloadBlockSize() {
        return downloadBlockSize;
    }

    public void setDownloadBlockSize(long downloadBlockSize) {
        this.downloadBlockSize = downloadBlockSize;
    }

    public long getRangeStart() {
        return rangeStart;
    }

    public void setRangeStart(long rangeStart) {
        this.rangeStart = rangeStart;
    }

    public long getRangeEnd() {
        return rangeEnd;
    }

    public void setRangeEnd(long rangeEnd) {
        this.rangeEnd = rangeEnd;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(int downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public boolean isSegmentDownload() {
        return segmentDownload;
    }

    public boolean getSegmentDownload() {
        return segmentDownload;
    }

    public void setSegmentDownload(boolean segmentDownload) {
        this.segmentDownload = segmentDownload;
    }

    public long getDownloadSimulateTotalSize() {
        return downloadSimulateTotalSize;
    }

    public void setDownloadSimulateTotalSize(long downloadSimulateTotalSize) {
        this.downloadSimulateTotalSize = downloadSimulateTotalSize;
    }

}

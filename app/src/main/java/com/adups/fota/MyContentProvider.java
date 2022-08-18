package com.adups.fota;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import com.adups.fota.config.Setting;
import com.adups.fota.manager.SpManager;

public class MyContentProvider extends ContentProvider {

    private static final int REJECT_CODE = 1;

    private static final String AUTHORITY = "com.adups.fota.MyContentProvider";

    private UriMatcher uriMatcher;

    @Override
    public boolean onCreate() {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, Setting.REJECT_STATUS, REJECT_CODE);
        return false;
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }


    @Override
    public String getType(Uri uri) {
        return null;
    }


    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        switch (uriMatcher.match(uri)) {
            case REJECT_CODE:
                SpManager.setRejectStatus(values.getAsBoolean(Setting.REJECT_STATUS));
                SpManager.setNoReportStatus(!values.getAsBoolean(Setting.REPORT_STATUS));
                SpManager.setConnectNetValue();
                break;
        }
        return 0;
    }

}

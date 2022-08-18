package com.adups.fota.manager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.adups.fota.MyApplication;
import com.adups.fota.bean.ReportBean;
import com.adups.fota.utils.EncryptUtil;
import com.adups.fota.utils.LogUtil;

import java.util.ArrayList;
import java.util.List;

public class DatabaseManager extends SQLiteOpenHelper {

    private static final int DB_VERSION = 4;
    private static final String DB_NAME = EncryptUtil.md5Encode(String.valueOf(new char[]{'o', 't', 'a', '.', 'd', 'b',}));

    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS ";
    private static final String TABLE_NAME = String.valueOf(new char[]{'r', 'e', 'p', 'o', 'r', 't'});
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_RESULT = "result";
    private static final String COLUMN_TIME = "time";

    private static final String DB_TABLE_CREATE =
            CREATE_TABLE + TABLE_NAME + " ("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_TYPE + " TEXT, "
                    + COLUMN_TIME + " LONG, "
                    + COLUMN_RESULT + " TEXT )";

    private static DatabaseManager manager;
    private static SQLiteDatabase sqLiteDatabase;

    private DatabaseManager(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static DatabaseManager getInstance() {
        if (manager == null)
            manager = new DatabaseManager(MyApplication.getAppContext());
        try {
            if (sqLiteDatabase == null)
                sqLiteDatabase = manager.getWritableDatabase();
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        }
        return manager;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            db.execSQL(DB_TABLE_CREATE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

    public void addData(String type, String result) {
        if (TextUtils.isEmpty(result) || TextUtils.isEmpty(type) || sqLiteDatabase == null) return;
        ContentValues value = new ContentValues();
        value.put(COLUMN_TYPE, type);
        value.put(COLUMN_RESULT, result);
        value.put(COLUMN_TIME, System.currentTimeMillis());
        sqLiteDatabase.insert(TABLE_NAME, null, value);
    }

    public void deleteContent() {
        if (sqLiteDatabase == null) return;
        sqLiteDatabase.delete(TABLE_NAME, null, null);
    }

    public List<ReportBean> getData() {
        if (sqLiteDatabase == null) return null;
        List<ReportBean> list = new ArrayList<>();
        Cursor cursor = sqLiteDatabase.query(TABLE_NAME,
                null, null, null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                    ReportBean bean = new ReportBean();
                    bean.setAction(cursor.getString(cursor.getColumnIndex(COLUMN_TYPE)));
                    bean.setResult(cursor.getString(cursor.getColumnIndex(COLUMN_RESULT)));
                    bean.setTime(cursor.getLong(cursor.getColumnIndex(COLUMN_TIME)));
                    list.add(bean);
            }
            cursor.close();
        }
        return list;
    }

}

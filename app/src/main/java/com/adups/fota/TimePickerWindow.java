package com.adups.fota;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adups.fota.callback.ClickCallback;
import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.config.TaskID;
import com.adups.fota.manager.AlarmManager;
import com.adups.fota.manager.NotificationManager;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.DialogUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PreferencesUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TimePickerWindow extends Activity {
    MaterialDialog mDialog;
    TimePicker timePicker;
    //    MaterialDialog.Builder build;
    int status;
    int notifyId;
    private ExitReceiver receiver;
    private static final String SYSTEM_DIALOG_REASON_KEY = "reason";
    private static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fota_pop_window);
//        build = new MaterialDialog.Builder(this);
        status = getIntent().getIntExtra(TaskID.STATUS, -1);
        notifyId = getIntent().getIntExtra(Setting.INTENT_PARAM_NOTIFY_ID, 0);
        receiver = new ExitReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        String permission = "com.adups.fota.permission";
        Handler handler = new Handler();
        registerReceiver(receiver, filter,permission,handler);
        init();
        if (DeviceInfoUtil.getInstance().getCanUse() == 0){
            showDisableDialog();
        }
    }

    public void showDisableDialog() {
        LogUtil.d("[showDisableDialog] ============");
        DialogUtil.showPositiveDialog(this,
                "ATTENTION!", "System update is disabled by your administrator.",
                new ClickCallback() {
                    @Override
                    public void onClick() {
                        Intent intent=new Intent(Intent.ACTION_MAIN);
                        intent.setAction(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        System.exit(0);
                    }
                }, null, Gravity.CENTER);
    }

    private void init() {

        int state = PreferencesUtils.getInt(getApplicationContext(), Setting.FOTA_UPDATE_STATUS, Status.STATE_QUERY_NEW_VERSION);
        PreferencesUtils.putInt(this, "downloadandinstall", 0);
        LogUtil.d("status = " + status + "; state = " + state + "; notifyId = " + notifyId);
        String title = null;
        if (status == TaskID.TASK_DOWNLOAD_DELAY) {
            title = getString(R.string.dialog_download_later_title);
        } else if (status == TaskID.TASK_DOWNLOAD_INSTALL_DELAY) {
            title = getString(R.string.dialog_download_install_later_title);
        } else if (status == TaskID.TASK_INSTALL_LATER) {
            title = getString(R.string.dialog_install_later);
        }
        mDialog = DialogUtil.showSystemDialog(this, title, null, getString(R.string.btn_ok), new ClickCallback() {
            @Override
            public void onClick() {
                setLaterAlarm();
                NotificationManager.getInstance().cancel(getApplicationContext(), NotificationManager.NOTIFY_CONTINUE_RESET);
                NotificationManager.getInstance().cancel(getApplicationContext(), NotificationManager.NOTIFY_NEW_VERSION);
                NotificationManager.getInstance().cancel(getApplicationContext(), NotificationManager.NOTIFY_DL_COMPLETED);
                finish();
                Intent intent = new Intent();// 创建Intent对象
                intent.setAction(Intent.ACTION_MAIN);// 设置Intent动作
                intent.addCategory(Intent.CATEGORY_HOME);// 设置Intent种类
                startActivity(intent);// 将Intent传递给Activity
            }
        }, null, null, null, getLayoutInflater().inflate(R.layout.time_picker, null), false);
//        build = new MaterialDialog.Builder(this)
//                .positiveText(R.string.btn_ok)
//                .onPositive(new MaterialDialog.SingleButtonCallback() {
//                    @Override
//                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
//                        setLaterAlarm();
//                        NotificationManager.getInstance().cancel(getApplicationContext(), NotificationManager.NOTIFY_CONTINUE_RESET);
//                        NotifyManager.with(getApplicationContext()).cancel(getApplicationContext(), NotifyManager.NOTIFY_NEW_VERSION);
//                        NotifyManager.with(getApplicationContext()).cancel(getApplicationContext(), NotifyManager.NOTIFY_DL_COMPLETED);
//                        finish();
//                        Intent intent = new Intent();// 创建Intent对象
//                        intent.setAction(Intent.ACTION_MAIN);// 设置Intent动作
//                        intent.addCategory(Intent.CATEGORY_HOME);// 设置Intent种类
//                        startActivity(intent);// 将Intent传递给Activity
//
//                    }
//                })
//                .customView(R.layout.time_picker, false);


        timePicker = mDialog.findViewById(R.id.time_picker);
//        timePicker.setDescendantFocusability(TimePicker.FOCUS_BLOCK_DESCENDANTS);
        timePicker.setIs24HourView(true);

        WindowManager windowManager = (WindowManager)
                getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display d = windowManager.getDefaultDisplay();
        WindowManager.LayoutParams p = mDialog.getWindow().getAttributes();
        p.width = (int) (d.getWidth() * 0.90);
        p.height = (int) (d.getHeight() * 0.90);
        mDialog.getWindow().setAttributes(p);
        mDialog.setCancelable(false);
        mDialog.setCanceledOnTouchOutside(false);

        mDialog.show();
        LogUtil.d("exit");
    }

    public static long getStringToDate(String dateString, String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
//        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        Date date = new Date();
        try {
            date = dateFormat.parse(dateString);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return date.getTime();
    }

    public static String getDateToString(long milSecond, String pattern) {
        Date date = new Date(milSecond);
        SimpleDateFormat format = new SimpleDateFormat(pattern);
//        format.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        return format.format(date);
    }

    private void setLaterAlarm() {
        LogUtil.d("");
        PreferencesUtils.putInt(this, "downloadandinstall", 0);
        Long currenttime = Calendar.getInstance().getTimeInMillis();
        String currentstring = getDateToString(currenttime, "yyyy-MM-dd HH:mm:ss");
        LogUtil.d("currenttime = " + currenttime + "; currentstring = " + currentstring);
        String currentdata = getDateToString(currenttime, "yyyy-MM");
        int currentdd = Integer.parseInt(getDateToString(currenttime, "dd"));
        String currenthh = getDateToString(currenttime, "HH");
        String currentss = getDateToString(currenttime, "ss");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            LogUtil.d("currenthour = " + timePicker.getHour());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (timePicker.getHour() < Integer.parseInt(currenthh)) {
                currentdd += 1;
            }
        }
        String alarmtimestring = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmtimestring = currentdata + "-" + currentdd + " " + timePicker.getHour() + ":" + timePicker.getMinute() + ":" + currentss;
        }
        LogUtil.d("alarmtimestring = " + alarmtimestring);
        Long alarmtime = getStringToDate(alarmtimestring, "yyyy-MM-dd HH:mm:ss");
        LogUtil.d("alarmtime = " + alarmtime + "; status = " + status);
        if (status == TaskID.TASK_DOWNLOAD_DELAY) {


            TimeZone timeZone = TimeZone.getDefault();
            String id = timeZone.getID(); //获取时区id
            String name = timeZone.getDisplayName(); //获取名字
            int time = timeZone.getRawOffset(); //获取时差，返回值毫秒
            LogUtil.d("id = " + id);
            LogUtil.d("name = " + name);
            LogUtil.d("time = " + time);

            LogUtil.d("The new version download will begin in the " + alarmtimestring);
            PreferencesUtils.putLong(this, "DOWNLOAD_DELAY", alarmtime);
            AlarmManager.download_later(getApplicationContext(), alarmtime);
        } else if (status == TaskID.TASK_DOWNLOAD_INSTALL_DELAY) {
            PreferencesUtils.putLong(this, "DOWNLOAD_INSTALL_DELAY", alarmtime);
            LogUtil.d("The new version download and install will begin in the " + alarmtimestring);
            AlarmManager.download_install_later(getApplicationContext(), alarmtime);
        } else if (status == TaskID.TASK_INSTALL_LATER) {
            LogUtil.d("The new version install will begin in the " + alarmtimestring);
            PreferencesUtils.putLong(this, "INSTALL_DELAY", alarmtime);
            AlarmManager.install_later(getApplicationContext(), alarmtime);
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DeviceInfoUtil.getInstance().getCanUse() == 0){
            showDisableDialog();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiver!=null) {
            unregisterReceiver(receiver);
        }
    }

    class ExitReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            LogUtil.d("action = " + action);

            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {

                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                LogUtil.d("reason: " + reason);
                if (SYSTEM_DIALOG_REASON_HOME_KEY.equals(reason)) {
                    // 短按Home键
                    AlarmManager.pop_cancel(getApplicationContext(), 1000 * 60 * 10);
                    finish();
                    LogUtil.d("按了home键");

                }

            }
        }
    }
}

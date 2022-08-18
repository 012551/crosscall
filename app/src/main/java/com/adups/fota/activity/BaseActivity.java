package com.adups.fota.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.adups.fota.GoogleOtaClient;
import com.adups.fota.R;
import com.adups.fota.bean.VersionBean;
import com.adups.fota.callback.ClickCallback;
import com.adups.fota.config.Setting;
import com.adups.fota.config.TaskID;
import com.adups.fota.install.Install;
import com.adups.fota.manager.ActivityManager;
import com.adups.fota.manager.AlarmManager;
import com.adups.fota.manager.NotificationManager;
import com.adups.fota.manager.SpManager;
import com.adups.fota.query.QueryInfo;
import com.adups.fota.report.ReportData;
import com.adups.fota.report.ReportManager;
import com.adups.fota.request.RequestParam;
import com.adups.fota.service.CustomActionIntentService;
import com.adups.fota.service.CustomActionService;
import com.adups.fota.service.FcmService;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.DialogUtil;
import com.adups.fota.utils.FileUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PreferencesUtils;
import com.adups.fota.utils.StorageUtil;
import com.adups.fota.utils.ToastUtil;
import com.adups.fota.view.TitleView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by brave on 2015/10/31.
 */
public abstract class BaseActivity extends AppCompatActivity implements View.OnClickListener, DialogInterface.OnDismissListener {

    public static final String FCM_REPORT_TYPE_LOG = "3";
    private static final int REQUEST_READ_CODE = 100;
    private static final String FCM_REPORT_TYPE_FILE_PATH = "1";
    private static final String FCM_REPORT_TYPE_STORAGE_SPACE = "2";
    public DrawerLayout mDrawerLayout;
    // 是否允许全屏
    private boolean mAllowFullScreen = true;
    private boolean isFront = false;
    private Map<String, String> param;
    private LinearLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.d(getActivityName());
        ActivityManager.getManager().pushActivity(this);
        // 屏幕方向配置定制
        if ("1".equals(DeviceInfoUtil.getInstance().getDisplay())) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else if ("2".equals(DeviceInfoUtil.getInstance().getDisplay())) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        if (mAllowFullScreen) {
            requestWindowFeature(Window.FEATURE_NO_TITLE); // 取消标题
        }
        super.setContentView(R.layout.base_activity_layout);
        layout = findViewById(R.id.layout);
        TitleView titleView = findViewById(R.id.titleView);
        setTitleView(titleView);
        initWidget();

        int canUse = DeviceInfoUtil.getInstance().getCanUse();
        LogUtil.d("canUse = " + canUse);
        if (canUse == 0) {
            showDisableDialog();
        }
        dealWithIntent(getIntent());

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        LogUtil.d(getActivityName());
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogUtil.d(getActivityName());
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtil.d(getActivityName());
        isFront = true;
        int canUse = DeviceInfoUtil.getInstance().getCanUse();
        LogUtil.d("canUse = " + canUse);
        if (canUse == 0) {
            showDisableDialog();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogUtil.d(getActivityName());
        isFront = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtil.d(getActivityName());
        if (getActivityName().equalsIgnoreCase(PopupActivity.class.getSimpleName())
                || getActivityName().equalsIgnoreCase(InstallResultActivity.class.getSimpleName())) {

        } else {
            closeDialog();
//            ActivityManager.getManager().finishMainActivity();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.d(getActivityName());
        ActivityManager.getManager().removeActivity(this);
    }

    protected abstract void setTitleView(TitleView titleView);

    protected abstract void initWidget();

    protected abstract void widgetClick(View v);

    public void setAllowFullScreen(boolean allowFullScreen) {
        this.mAllowFullScreen = allowFullScreen;
    }

    @Override
    public void onClick(View v) {
        widgetClick(v);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        dealWithIntent(intent);
    }

    public void onTitleClick(View view) {
        finish();
    }

    public void openMenu() {
        if (mDrawerLayout != null) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        layout.removeAllViews();
        layout.addView(LayoutInflater.from(this).inflate(layoutResID, null),
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
    }

    public boolean isFront() {
        return isFront;
    }

    private String getActivityName() {
        return getClass().getSimpleName();
    }

    public void dealWithIntent(Intent intent) {
        if (intent != null) {
            int notifyId = intent.getIntExtra(Setting.INTENT_PARAM_NOTIFY_ID, 0);
            param = new HashMap<>();
            param.put(Setting.INTENT_PARAM_TASK_ID, intent.getStringExtra(Setting.INTENT_PARAM_TASK_ID));
            switch (notifyId) {
                case FcmService.NOTIFY_FCM_REPORT_FILE_PATH:
                    param.put(RequestParam.ACTION_TYPE, FCM_REPORT_TYPE_FILE_PATH);
                    String url = "";
                    VersionBean bean = QueryInfo.getInstance().getVersionInfo();
                    if (bean != null) url = bean.getDeltaUrl();
                    param.put(RequestParam.DOWNLOAD_PATH, url);
                    param.put(RequestParam.SAVE_PATH, StorageUtil.getPackagePathName(this));
                    ReportManager.getInstance().fcmReport(this, param);
                    break;
                case FcmService.NOTIFY_FCM_REPORT_STORAGE_SPACE:
                    if (FileUtil.hasReadPermission(this)) {
                        param.put(RequestParam.ACTION_TYPE, FCM_REPORT_TYPE_STORAGE_SPACE);
                        param.put(RequestParam.STORAGE_SPACE, getStorageSpace());
                        ReportManager.getInstance().fcmReport(this, param);
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_CODE);
                    break;
                case FcmService.NOTIFY_FCM_REPORT_LOG:
                    if (!LogUtil.getSaveLog()) {
                        openDebug();
                        SpManager.setLogTaskId(param.get(Setting.INTENT_PARAM_TASK_ID));
                        AlarmManager.stopLogOutAlarm(this);
                    }
                    break;
            }
            NotificationManager.getInstance().cancel(this, notifyId);
        }
    }

    private String getStorageSpace() {
        File file = getExternalCacheDir();
        StringBuilder stringBuilder = new StringBuilder();
        if (file != null)
            stringBuilder.append(Formatter.formatFileSize(this, file.getTotalSpace())).append("/")
                    .append(Formatter.formatFileSize(this, file.getUsableSpace()));
        if (StorageUtil.isSdcardMounted(this)) {
            String sdCardPath = StorageUtil.getStoragePath(this, true);
            if (!TextUtils.isEmpty(sdCardPath))
                file = new File(sdCardPath);
            if (file != null)
                stringBuilder.append("&").append(Formatter.formatFileSize(this, file.getTotalSpace())).append("/")
                        .append(Formatter.formatFileSize(this, file.getUsableSpace()));
        }
        return stringBuilder.toString();
    }

    public void openDebug() {
        String path = StorageUtil.getCatchLogPath(this);
        if (!StorageUtil.isSpaceEnough(path, 1024 * 1024)) {
            ToastUtil.showToast(R.string.sdcard_crash_or_unmount);
            return;
        }
        path += "/fota";
        File folder = new File(path);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        String date = SimpleDateFormat.getDateTimeInstance().format(System.currentTimeMillis());
        date = date.replaceAll(" ", "-");
        date = date.replaceAll(":", "-");
        File saveLog = new File(folder.getAbsolutePath() + "/" + date + ".txt");
        PreferencesUtils.putBoolean(this, Setting.DEBUG_SWITCH, true);
        PreferencesUtils.putString(this, Setting.DEBUG_LOG_PATH, saveLog.getAbsolutePath());
        LogUtil.setSaveLog(true);
        LogUtil.setSaveLogPath(saveLog.getAbsolutePath());
        LogUtil.d("log out path : " + folder.getAbsolutePath());
        ToastUtil.showToast(getString(R.string.start_catch_log) + " to " + folder.getAbsolutePath());
    }

    /**
     * click Button [install]
     */
    @SuppressLint("StringFormatInvalid")
    public void onClickInstallNow() {
        LogUtil.d("enter");
        if (DeviceInfoUtil.getInstance().getCanUse() == 0) {
            LogUtil.d("can not use fota to onClickInstallNow");
            showDisableDialog();
            return;
        }
        int level = DeviceInfoUtil.getInstance().getBattery();
        if (!Install.isBatteryAbility(this, level)) {
            LogUtil.d("no update reason : battery not enough");
            CustomActionService.enqueueWork(this, TaskID.TASK_BATTERY_MONITOR);
            DialogUtil.showBaseCustomDialog(this, R.mipmap.ota_battery,
                    getString(R.string.ota_battery_low, level), this);
            return;
        }
        if (!Install.isEnoughSpace(this, StorageUtil.getPackageFileName(this))) {
            LogUtil.d("no update reason : sd card status not illegal");
            DialogUtil.showPositiveDialog(this,
                    getString(R.string.battery_remove_title), getString(R.string.sdcard_crash_or_unmount),
                    null, this);
            return;
        }

        DialogUtil.showDialog(this,
                getString(R.string.not_support_fota_title), getString(R.string.update_prompt),
                new ClickCallback() {
                    @Override
                    public void onClick() {
                        doUpdate();
                    }
                }, new ClickCallback() {
                    @Override
                    public void onClick() {
                        finishPopWindows();
                    }
                });
    }

    private void finishPopWindows() {
        if (!getActivityName().equalsIgnoreCase(GoogleOtaClient.class.getSimpleName()))
            finish();
    }

    public void showDisableDialog() {
        LogUtil.d("[showDisableDialog] ============");
        DialogUtil.showPositiveDialog(this,
                "ATTENTION!", "System update is disabled by your administrator.",
                new ClickCallback() {
                    @Override
                    public void onClick() {
                        ActivityManager.getManager().finishAllActivity();
//                        Intent intent=new Intent(Intent.ACTION_MAIN);
//                        intent.addCategory(Intent.CATEGORY_HOME);
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        startActivity(intent);
//                        System.exit(0);
//                        finish();
                    }
                }, null, Gravity.CENTER);
    }

    /**
     * click  Confirm  button
     */
    public void doUpdate() {
        LogUtil.d("enter");
        PreferencesUtils.putInt(this, Setting.INSTALL_LATER_COUNT, 0);
        DialogUtil.showCustomDialog(this, R.layout.dialog_update_unzip, this);
        ReportData.postInstall(this, ReportData.INSTALL_STATUS_INSTALL);
        Install.update(this.getApplicationContext());
    }

    public void closeDialog() {
        DialogUtil.closeDialog();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finishPopWindows();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_READ_CODE:
                for (int i = 0; i < permissions.length; i++) {
                    if (permissions[i].equalsIgnoreCase(Manifest.permission.READ_EXTERNAL_STORAGE)
                            && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        param.put(RequestParam.ACTION_TYPE, FCM_REPORT_TYPE_STORAGE_SPACE);
                        param.put(RequestParam.STORAGE_SPACE, getStorageSpace());
                        ReportManager.getInstance().fcmReport(this, param);
                    }
                }
                break;
        }
    }

}

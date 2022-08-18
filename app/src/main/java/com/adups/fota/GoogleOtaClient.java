package com.adups.fota;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.UpdateEngine;
import android.provider.Settings;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.adups.fota.activity.BaseActivity;
import com.adups.fota.bean.EventMessage;
import com.adups.fota.bean.VersionBean;
import com.adups.fota.callback.ClickCallback;
import com.adups.fota.callback.DialCallback;
import com.adups.fota.callback.FunctionClickCallback;
import com.adups.fota.callback.InstallDelayCallback;
import com.adups.fota.config.Const;
import com.adups.fota.config.Event;
import com.adups.fota.config.ServerApi;
import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.config.TaskID;
import com.adups.fota.download.DownPackage;
import com.adups.fota.download.DownVersion;
import com.adups.fota.install.Install;
import com.adups.fota.manager.ActivityManager;
import com.adups.fota.manager.AlarmManager;
import com.adups.fota.manager.NotificationManager;
import com.adups.fota.manager.SpManager;
import com.adups.fota.query.QueryInfo;
import com.adups.fota.query.QueryVersion;
import com.adups.fota.report.ReportData;
import com.adups.fota.service.CustomActionService;
import com.adups.fota.system.Recovery;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.DialogUtil;
import com.adups.fota.utils.FileUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.MidUtil;
import com.adups.fota.utils.NetWorkUtil;
import com.adups.fota.utils.PackageUtils;
import com.adups.fota.utils.PreferencesUtils;
import com.adups.fota.utils.StorageUtil;
import com.adups.fota.utils.SystemSettingUtil;
import com.adups.fota.utils.ToastUtil;
import com.adups.fota.view.DeviceFunctionView;
import com.adups.fota.view.FooterLayout;
import com.adups.fota.view.InstallDelayView;
import com.adups.fota.view.NetworkWarnView;
import com.adups.fota.view.PopWindowsLayout;
import com.adups.fota.view.ProgressLayout;
import com.adups.fota.view.ProgressTextLayout;
import com.adups.fota.view.TitleView;
import com.google.android.material.appbar.AppBarLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GoogleOtaClient extends BaseActivity implements InstallDelayCallback, FunctionClickCallback, DialCallback, View.OnClickListener {

    private static final String ERROR_REASON_PAUSE = "PAUSE";
    private static final String ERROR_REASON_RESPONSE_UNDONE = "UNDONE";

    private static final int MSG_DELAY_TIME = 11;
    private static final int MSG_ADDITIONAL = 22;
    private static final int MSG_AUTO_CHECK = 33;
    private static final int MSG_FULL_CHECK = 34;

    private static final int REQUEST_CODE = 200;

    private static final long[] schedule_array = {Setting.INSTALL_DELAY_SCHEDULE_1,
            Setting.INSTALL_DELAY_SCHEDULE_2, Setting.INSTALL_DELAY_SCHEDULE_3,};

    private int delayTimeCounts = 15;
    private int mClickCount;
    private boolean isActiveSafe = false;
    private int userPause = 0;
    private long lastClickTime = 0L;
    //界面处理
    private FooterLayout mFooterLayout;
    private ProgressLayout mProgress;
    private ImageView setting;
    private LinearLayout ab_view, pro_view, pre_view;
    private TextView mUpdateTip, mReleaseView, battery_tip, update_txt, pro_txt, mReleaseNote, mNight_note;
    private Button mNight_update;
    private ProgressBar progress_update_id;
    private AppBarLayout appBarLayout;
    private ProgressTextLayout progressTextLayout;
//    private ShakeView shakeView;

    private MaterialDialog dialog;

    private ExitReceiver receiver;
    private static final String SYSTEM_DIALOG_REASON_KEY = "reason";
    private static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DELAY_TIME:
                    String title = getString(R.string.remind_last_time) + "(" + delayTimeCounts + ")";
                    if (delayTimeCounts > 0) {
                        if (dialog == null) {
                            InstallDelayView installDelayView = new InstallDelayView(GoogleOtaClient.this);
                            installDelayView.setOnItemClickListener(GoogleOtaClient.this);
                            dialog = DialogUtil.showNoButtonCustomDialog(GoogleOtaClient.this, title, Gravity.CENTER,
                                    GoogleOtaClient.this, installDelayView);
                        } else {
                            dialog.setTitle(title);
                        }
                        delayTimeCounts--;
                        mHandler.sendEmptyMessageDelayed(MSG_DELAY_TIME, 1000);
                    } else {
                        onClickSchedule(1);// 倒计时结束默认选择4小时
                    }
                    break;
                case MSG_AUTO_CHECK:
                    if (Status.getVersionStatus(GoogleOtaClient.this) == Status.STATE_QUERY_NEW_VERSION
                            && isOverRemindDate() && NetWorkUtil.isConnected(GoogleOtaClient.this)) {
                        onClickQuery(false);
                    }
                    break;
                case MSG_FULL_CHECK:
                    queryFullRom();
                    break;
                case 100:
                    statusAction(msg.arg1);
                    break;
            }
        }
    };

    @Override
    protected void setTitleView(TitleView titleView) {
        titleView.setImage(R.mipmap.small_logo);
        if (DeviceInfoUtil.getInstance().isShowBtnPop()) { //后台配置是否显示右上角设置
            titleView.setSettingVisible(true);
            titleView.setSettingClickListener(this);
        }
        setting = titleView.getSetting();
    }

    @Override
    public void onTitleClick(View view) {
        if (++mClickCount > 4) {
            mClickCount = 0;
            onClickTitle();
            LogUtil.setLogOut(true);
        }
    }

    @Override
    public void initWidget() {
        LogUtil.d("enter");
        //如果处于访客模式则退出
        if (PackageUtils.getUserId(android.os.Process.myUid()) != 0) {
            ToastUtil.showToast(R.string.guest_hint);
            finish();
        }


        setContentView(R.layout.activity_ota_client);
        initView();

        int canUse = DeviceInfoUtil.getInstance().getCanUse();
        LogUtil.d("canUse = " + canUse);
        if (canUse == 0) {
            showDisableDialog();
        }


//        registerReceiver(receiver, new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        if (DeviceInfoUtil.getInstance().isNoRing())
            appBarLayout.setVisibility(View.GONE);
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
        mHandler.sendEmptyMessageDelayed(MSG_AUTO_CHECK, 1000);
        LogUtil.d("exit");
    }

    private void initView() {
        LogUtil.d("enter");
        mFooterLayout = findViewById(R.id.footer_layout);
        mFooterLayout.setOnClickListener(this);
        mProgress = findViewById(R.id.progress_layout);
        mUpdateTip = findViewById(R.id.ota_update_tip);
        mReleaseView = findViewById(R.id.relese_view);
        mReleaseNote = findViewById(R.id.relese_note);//zhangzhou
        mNight_update = findViewById(R.id.night_update_button);//zhangshou
        mNight_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LogUtil.d("enter Night_update");
                pop_Night_Update();
            }
        });
        mNight_note = findViewById(R.id.night_update_note);//zhangzhou
        mNight_note.setVisibility(View.GONE);//zhangzhou
        mNight_update.setVisibility(View.GONE);//zhangzhou
        pre_view = findViewById(R.id.pre_view);
        ab_view = findViewById(R.id.ab_view);
        pro_view = findViewById(R.id.pro_view);
        battery_tip = findViewById(R.id.battery_tip);
        update_txt = findViewById(R.id.update_txt);
        pro_txt = findViewById(R.id.pro_txt);
        progress_update_id = findViewById(R.id.progress_update_id);
        appBarLayout = findViewById(R.id.appBarLayout);
        progressTextLayout = findViewById(R.id.progressLayout);
//      shakeView = findViewById(R.id.shakeView);
        LogUtil.d("[initView] finish");
    }

    @Override
    public void dealWithIntent(Intent intent) {
        super.dealWithIntent(intent);
        if (intent != null) {
            int notifyId = intent.getIntExtra(Setting.INTENT_PARAM_NOTIFY_ID, 0);
            String flag = intent.getStringExtra(Setting.INTENT_PARAM_FLAG);
            int status = PreferencesUtils.getInt(getApplicationContext(), Setting.FOTA_UPDATE_STATUS, Status.STATE_QUERY_NEW_VERSION);
            LogUtil.d("flag = " + flag + "; status = " + status + "; notifyId = " + notifyId);


            if (!TextUtils.isEmpty(flag)) {
                if (flag.equalsIgnoreCase(Setting.INTENT_PARAM_FLAG_INSTALL)) {
                    onClickInstallNow();
                    resetAlarm();
                } else if (flag.equalsIgnoreCase(Setting.INTENT_PARAM_FLAG_CONTINUE)) {
                    PreferencesUtils.putInt(this, "downloadandinstall", 0);
                    if (status == Status.STATE_DL_PKG_COMPLETE) {
                        initInstallView();
                        onClickInstallNow();
                    } else if (status == Status.STATE_NEW_VERSION_READY) {
                        onClickDownload();
                    }
                } else if (flag.equalsIgnoreCase(Setting.INTENT_PARAM_DOWNLOAD)) {
                    onClickDownload();
                } else if (flag.equalsIgnoreCase(Setting.INTENT_PARAM_INSTALL)) {
                    onClickInstallNow();
                }
                //zhangzhou
                else if (flag.equalsIgnoreCase(Setting.INTENT_PARAM_NIGHT)) {
                    pop_Night_Update();
                }

            }

            NotificationManager.getInstance().cancel(this, notifyId);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        initStatus();
        MyApplication.setOnPhoneCallingListener(this);
        ActivityManager.getManager().setTopActivity(true);
        int canUse = DeviceInfoUtil.getInstance().getCanUse();
        LogUtil.d("canUse = " + canUse);
        if (canUse == 0) {
            showDisableDialog();
        }
//        ShakeUtil.getInstance().setOnShakingListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ActivityManager.getManager().setTopActivity(false);
//        ShakeUtil.getInstance().removeShakingListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Status.getVersionStatus(this) == Status.STATE_AB_UPDATING) {
            PreferencesUtils.putInt(this, Setting.FOTA_AB_PROGRESS, progress_update_id.getProgress());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyApplication.removePhoneCallingListener();
        mHandler.removeCallbacksAndMessages(null);
//        if (receiver!=null) {
//            unregisterReceiver(receiver);
//        }
        EventBus.getDefault().unregister(this);
    }

    private void resetAlarm() {
        long schedule_time = PreferencesUtils.getLong(this, Setting.FOTA_INSTALL_DELAY_SCHEDULE);
        LogUtil.d("schedule_time = " + schedule_time);
        if (schedule_time > 0) {
            AlarmManager.installDelay(this, schedule_time + System.currentTimeMillis());
        }
    }

    private void initStatus() {
        int version_status = Status.getVersionStatus(this);
        LogUtil.d("version_status = " + version_status);
        showAbView(false, false);
        closeDialog();
        switch (version_status) {
            case Status.STATE_QUERY_NEW_VERSION:
                initIdleView();
                break;
            case Status.STATE_NEW_VERSION_READY:
                if (QueryInfo.getInstance().getVersionInfo() != null) {
                    userPause = 0;
                    initNewVersionView();
                } else {
                    initIdleView();
                }
                break;
            case Status.STATE_DOWNLOADING:
                initDownloadView();
                break;
            case Status.STATE_PAUSE_DOWNLOAD:
                if (QueryInfo.getInstance().getVersionInfo() == null) {
                    initIdleView();
                } else {
                    initPauseView(true);
                }
                if (PreferencesUtils.getBoolean(MyApplication.getAppContext(), Setting.SLIENT_DOWNLOAD, false)) {
                    DownVersion.getInstance().slientDownload(MyApplication.getAppContext());
                    LogUtil.d("slientDown: " + PreferencesUtils.getBoolean(MyApplication.getAppContext(), Setting.SLIENT_DOWNLOAD, false));
                }
                break;
            case Status.STATE_DL_PKG_COMPLETE:
                initInstallView();
                break;
            case Status.STATE_AB_UPDATING:
                initInstallingView();
                break;
            case Status.STATE_REBOOT:
                initRebootView();
                break;
            default:
                initIdleView();
                break;
        }
    }

    private void initInstallingView() {
        int progress = PreferencesUtils.getInt(this, Setting.FOTA_AB_PROGRESS, 0);
        LogUtil.d("initInstallingView progress = " + progress);
        showAbView(true, true);
        update_txt.setText(R.string.ab_installing);
        mFooterLayout.init(Status.STATE_AB_UPDATING);
        pro_txt.setText(String.format("%s%%", String.valueOf(progress)));
        if (progress > 100) {
            progress = 100;
        }
        progress_update_id.setProgress(progress);
        DialogUtil.closeDialog();
        if (Status.getVersionStatus(this) == Status.STATE_AB_UPDATING) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Recovery.getInstance().executeAb(GoogleOtaClient.this,
                            StorageUtil.getPackageFileName(GoogleOtaClient.this)); //如果处于ab升级空闲状态则复位
                }

            }).start();
        }
    }

    private void initRebootView() {
        showAbView(true, false);
        battery_tip.setText(R.string.updated_need_reboot);
        mFooterLayout.init(Status.STATE_REBOOT);
    }

    private void initIdleView() {
        LogUtil.d("enter");
        int version_status = Status.STATE_QUERY_NEW_VERSION;
        Status.setVersionStatus(this, version_status);
        showAbView(false, false);
        mUpdateTip.setVisibility(View.GONE);
        progressTextLayout.setVisibility(View.GONE);
        TextView mNight_note = findViewById(R.id.night_update_note);//zhangzhou
        mNight_note.setVisibility(View.GONE);//zhangzhou
        mNight_update.setVisibility(View.GONE);//zhangzhou
//        shakeView.setContent(R.string.shake_to_check);
        String alias_name = DeviceInfoUtil.getInstance().getDisplayVersion();
        if (TextUtils.isEmpty(alias_name)) {
            alias_name = DeviceInfoUtil.getInstance().getLocalVersion();
        }
        String data = getResources().getString(R.string.htmlstring_version) +
                getString(R.string.current_version_text) +
                getResources().getString(R.string.htmlstring_version_end) +
                getResources().getString(R.string.htmlstring_code_head) +
                alias_name +
                getResources().getString(R.string.htmlstring_code_end);
        mReleaseView.setText(Html.fromHtml(data));
        mReleaseNote.setText("");
        mFooterLayout.init(version_status);
        mProgress.reset();
        LogUtil.d("exit");
    }

    private void initNewVersionView() {
        LogUtil.d("enter");
        int version_status = Status.STATE_NEW_VERSION_READY;
        Status.setVersionStatus(this, version_status);
        mUpdateTip.setVisibility(View.VISIBLE);
        progressTextLayout.setVisibility(View.GONE);
//      shakeView.setContent(R.string.shake_to_download);
        if (PreferencesUtils.getBoolean(MyApplication.getAppContext(), Setting.SLIENT_DOWNLOAD, false)) {
            DownVersion.getInstance().slientDownload(MyApplication.getAppContext());
            LogUtil.d("slientDown: " + PreferencesUtils.getBoolean(MyApplication.getAppContext(), Setting.SLIENT_DOWNLOAD, false));
        }
        loadReleaseNotes();

        mNight_note.setVisibility(View.VISIBLE);//zhangzhou
        //TextView mNight_note = findViewById(R.id.night_update_note);
        mNight_note.setVisibility(View.VISIBLE);
        mNight_update.setVisibility(View.VISIBLE);
        mFooterLayout.init(version_status);
        mProgress.reset();
        mProgress.setVersionTip(getString(R.string.new_version_text));
        LogUtil.d("exit");
    }

    //夜间弹框
    public void pop_Night_Update() {
        DialogUtil.showSlientDialog(
                this, getString(R.string.night_down_title),
                getString(R.string.night_down_content)
        );
    }


    private void initDownloadView() {
        LogUtil.d("enter");
        if (!DownVersion.getInstance().isDownloading(this)) {
            Status.setVersionStatus(this, Status.STATE_PAUSE_DOWNLOAD);
            initPauseView(true);
        } else {
//            shakeView.setVisibility(View.GONE);
            int version_status = Status.STATE_DOWNLOADING;
            mNight_note.setVisibility(View.VISIBLE);//zhangzhou_init
            mNight_update.setVisibility(View.VISIBLE);
            Status.setVersionStatus(this, version_status);
            mUpdateTip.setVisibility(View.VISIBLE);
            loadReleaseNotes();
            int percent = getPercent();
            mFooterLayout.init(version_status);
            setDownloadProgress(percent);
        }
        LogUtil.d("exit");
    }

    private void initDownloadStartView() {
        LogUtil.d("enter");
        int version_status = Status.STATE_DOWNLOADING;
        Status.setVersionStatus(this, version_status);
        mUpdateTip.setVisibility(View.VISIBLE);
//        shakeView.setVisibility(View.GONE);
        loadReleaseNotes();
        mFooterLayout.init(version_status);
        int percent = getPercent();
        setDownloadProgress(percent);
        LogUtil.d("exit");
    }

    private void initPauseView(boolean reload) {
        LogUtil.d("enter");
        int version_status = Status.STATE_PAUSE_DOWNLOAD;
        Status.setVersionStatus(this, version_status);
        mUpdateTip.setVisibility(View.VISIBLE);
        mNight_update.setVisibility(View.VISIBLE);
        mNight_note.setVisibility(View.VISIBLE);
//        shakeView.setVisibility(View.VISIBLE);
//        shakeView.setContent(R.string.shake_to_download);
        if (reload) {
            loadReleaseNotes();
        }
        int percent = getPercent();
        mFooterLayout.init(version_status);
        setDownloadProgress(percent);
        LogUtil.d("exit");
    }

    private void initInstallView() {
        LogUtil.d("enter");
        VersionBean version = QueryInfo.getInstance().getVersionInfo();
        if (version == null) {
            initIdleView();
            return;
        }
        mNight_update.setVisibility(View.VISIBLE);
        mNight_note.setVisibility(View.VISIBLE);
        int version_status = Status.STATE_DL_PKG_COMPLETE;
        Status.setVersionStatus(this, version_status);
        mUpdateTip.setVisibility(View.VISIBLE);
//      shakeView.setContent(R.string.shake_to_update);
        loadReleaseNotes();
        mFooterLayout.init(version_status);

        //取消AB升级时，下载完成后自动安装
        /*if (!Install.isSupportAbUpdate()) {
            showAbView(true, false);
            if (Install.isBatteryAbility(this, DeviceInfoUtil.getInstance().getBattery())) {  //电量充足则自动进行ab系统安装
                ReportData.postInstall(this, ReportData.INSTALL_STATUS_INSTALL);
                Install.update(this);
            } else { //电量不充足则UI提示
                LogUtil.d("no update reason : battery not enough");
                CustomActionIntentService.enqueueWork(this, TaskID.TASK_BATTERY_MONITOR);
                EventBus.getDefault().post(new EventMessage(Event.INSTALL, 100, 0,
                        Recovery.AB_BATTERY_LOW, Recovery.AB_FLAG));
            }
        } else {
            LogUtil.d("no update reason : not support ab update");
        }*/
        mProgress.setDownLoadProgress(100);
        LogUtil.d("exit");
    }

    //加载版本信息
    private void loadReleaseNotes() {
        LogUtil.d("enter");

        //zhangzhou start
        String version_data = null;
        String textArrar[] = null;
        //zhangzhou end

        VersionBean version = QueryInfo.getInstance().getVersionInfo();
        if (version != null) {

            //zhangzhou
            version_data = QueryInfo.getInstance().getReleaseNotes(this, true);
            LogUtil.d("version_data:" + version_data);
            textArrar = version_data.split("</div>");
            String version_info = textArrar[0] + "</div>";
            String version_note = textArrar[1] + "</div>";
            version_note = version_note.replaceFirst("<br/>", "");
            version_info = Html.fromHtml(version_info).toString().trim();
            LogUtil.d("版本信息：" + version_info);
            LogUtil.d("修改点：：" + version_note);
            mReleaseView.setText(version_info);
            mReleaseNote.setText(Html.fromHtml(version_note));
            //zhangzhou

            LogUtil.d("loadReleaseNotes:" + QueryInfo.getInstance().getNowLanguage());
            if (QueryInfo.getInstance().getNowLanguage().equals("zh_CN")) {
                Typeface typeFace = Typeface.createFromAsset(MyApplication.getAppContext().getAssets(), "fonts/wen_quan.ttf");
                mReleaseView.setTypeface(typeFace);
            } else {
                mReleaseView.setTypeface(null);
            }
            if (version.getIsSilent() == 0) {
                mHandler.sendEmptyMessageDelayed(MSG_ADDITIONAL, 1000);
            }
        }
        LogUtil.d("exit");
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void widgetClick(View v) {
        if (v.getTag() == null) {
            return;
        } else if (isClick2Fast()) {
            ToastUtil.showToast(R.string.button_click_toast);
            return;
        }
        int id = (Integer) v.getTag();
        switch (id) {
            case Const.TAG_CHECK://点击check按钮
                //initRebootView();
                onClickQuery(true);
                break;
            case Const.TAG_LEFT_MENU://点击左上角
                openMenu();
                break;
            case Const.TAG_DOWNLOAD_CANCEL://点击取消按钮
                onClickCancel();
                break;
            case Const.TAG_DOWNLOAD://点击下载
                onClickDownload();
                //initInstallingView();
                break;
            case Const.TAG_DOWNLOAD_PAUSE://点击暂停
                onClickPause();
                break;
            case Const.TAG_DOWNLOAD_RESUME://点击恢复
            case Const.TAG_DOWNLOAD_RETRY:
                onClickResume(true);
                break;
            case Const.TAG_UPDATE_LATER://点击稍后安装
                onClickInstallLater();
                break;
            case Const.TAG_UPDATE_NOW://点击立即升级
                //initInstallingView();
                if (Install.isSupportAbUpdate()) {
                    showAbView(true, false);
                    if (Install.isBatteryAbility(this, DeviceInfoUtil.getInstance().getBattery())) {  //电量充足则自动进行ab系统安装
                        ReportData.postInstall(this, ReportData.INSTALL_STATUS_INSTALL);
                        Install.update(this);
                    } else { //电量不充足则UI提示
                        LogUtil.d("no update reason : battery not enough");
                        CustomActionService.enqueueWork(this, TaskID.TASK_BATTERY_MONITOR);
                        EventBus.getDefault().post(new EventMessage(Event.INSTALL, 100, 0,
                                Recovery.AB_BATTERY_LOW, Recovery.AB_FLAG));
                    }
                } else {
                    onClickInstallNow();
                    LogUtil.d("no update reason : not support ab update");
                }
                break;
            case Const.TAG_REBOOT_NOW://点击立即重启
                Recovery.getInstance().reboot(this);
                break;
            case Const.TAG_BTN_POP:
                new PopWindowsLayout().setPopWinLayout(this, v);
                break;
            case Const.TAG_DOWNLOAD_LATER:
                if (DeviceInfoUtil.getInstance().getCanUse() == 0) {
                    LogUtil.d("can not use fota to download_later");
                    showDisableDialog();
                    return;
                }
                Intent intent = new Intent("com.adups.fota");
                intent.putExtra(TaskID.STATUS, TaskID.TASK_DOWNLOAD_DELAY);
                intent.putExtra(Setting.INTENT_PARAM_FLAG, Setting.INTENT_PARAM_DOWNLOAD);
                intent.setClass(getApplicationContext(), TimePickerWindow.class);
                startActivity(intent);
                break;
            case Const.TAG_DOWNLOAD_INSTALL_LATER:
                if (DeviceInfoUtil.getInstance().getCanUse() == 0) {
                    LogUtil.d("can not use fota to download_install_later");
                    showDisableDialog();
                    return;
                }
//                PreferencesUtils.putInt(getApplicationContext(), "download_install_later", 1);
                Intent intent1 = new Intent("com.adups.fota");
                intent1.putExtra(TaskID.STATUS, TaskID.TASK_DOWNLOAD_INSTALL_DELAY);
                intent1.setClass(getApplicationContext(), TimePickerWindow.class);
                startActivity(intent1);
                break;
        }
    }

    //    private void showDisableDialog(){
//        DialogUtil.showPositiveDialog(this,
//                "ATTENTION!", "System update is disabled by your administrator.",
//                new ClickCallback() {
//                    @Override
//                    public void onClick() {
//                        finish();
//                    }
//                }, null);
//    }
    //click  button [query]
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void onClickQuery(boolean isClick) {
        if (DeviceInfoUtil.getInstance().getCanUse() == 0) {
            if (isClick) {
                showDisableDialog();
            }
            LogUtil.d("can not use fota to query; isClick: " + isClick);
            return;
        }
        boolean isConnected = NetWorkUtil.isConnected(this);
        LogUtil.d("isConnected = " + isConnected + ", is2GConnected = " + NetWorkUtil.is2GConnected(this));
        if (isConnected) {
            if (!NetWorkUtil.isWiFiConnected(this) && NetWorkUtil.is2GConnected(this)) {
                LogUtil.d("currrent is 2G network");
                PreferencesUtils.putBoolean(this, "download_now_broadcast", false);
                DialogUtil.showPositiveDialog(this, getString(R.string.not_support_fota_title), getString(R.string.not_allow_with2g));
                return;
            }
            if (NetWorkUtil.isMobileConnected(this) && NetWorkUtil.isRoaming(this)) {
                DialogUtil.showDialog(this, getString(R.string.check_now), getString(R.string.netRoaming_content_tip),
                        new ClickCallback() {
                            @Override
                            public void onClick() {
                                QueryVersion.getInstance().onQuery(GoogleOtaClient.this, QueryVersion.QUERY_MANUAL);
                            }
                        });
            } else {
                QueryVersion.getInstance().onQuery(this, QueryVersion.QUERY_MANUAL);
            }
        } else {
            showNoNetWorkDialog();
        }
    }

    //click Button [download]
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void onClickDownload() {
        if (DeviceInfoUtil.getInstance().getCanUse() == 0) {
            LogUtil.d("can not use fota to onClickDownload;");
            showDisableDialog();
            return;
        }
        LogUtil.d("");
        if (MyApplication.isCalling() && !NetWorkUtil.isWiFiConnected(this)) {
            DialogUtil.showPositiveDialog(this, getString(R.string.not_support_fota_title),
                    getString(R.string.call_warn), this);
            return;
        }
        if (NetWorkUtil.isConnected(this) && NetWorkUtil.is2GConnected(this)) {
            LogUtil.d("currrent is 2G network");
            DialogUtil.showPositiveDialog(this, getString(R.string.not_support_fota_title), getString(R.string.not_allow_with2g));
            return;
        }


        if (NetWorkUtil.isMobileConnected(this) && NetWorkUtil.isRoaming(this)) {
            if (DeviceInfoUtil.getInstance().getRoamStatus() == 1) {
                DialogUtil.showCustomDialog(this, getString(R.string.romimg_notice),
                        getString(R.string.btn_cancel), new ClickCallback() {
                            @Override
                            public void onClick() {
                                LogUtil.d("User refuses to use traffic to download");
                                return;
                            }
                        }, getString(R.string.btn_download), new ClickCallback() {
                            @Override
                            public void onClick() {
                                LogUtil.d("User agrees to use data download");
                                agreeDownload();
                                return;
                            }
                        }, this, null);
                return;
            } else {
                DialogUtil.showDialog(this, getString(R.string.not_support_fota_title), getString(R.string.network_roaming),
                        getString(R.string.ota_wifi_setting), new ClickCallback() {
                            @Override
                            public void onClick() {
                                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                            }
                        }, getString(android.R.string.cancel), null);
                return;
            }

        }

        if (NetWorkUtil.isConnected(this)) {
            LogUtil.d("");
            boolean isOnlyWifi = PreferencesUtils.getBoolean(this, Setting.DOWNLOAD_ONLY_WIFI, DeviceInfoUtil.getInstance().isOnlyWifi());
            boolean isMobile = NetWorkUtil.isMobileConnected(this);
            if (isOnlyWifi && isMobile) {//客户端配置了仅wifi,或者后台配置了仅wifi,会提醒用户
                LogUtil.d("no download reason : only support wifi update but mobile wifi off");
                ReportData.postInstallResult(this, false, Status.UPDATE_STATUS_ONLY_WIFI, null);
                DialogUtil.showPositiveDialog(this, getString(R.string.not_support_fota_title),
                        getString(R.string.setting_network_tip), this);
                return;
            }
            VersionBean version = QueryInfo.getInstance().getVersionInfo();
            if (version != null) {
                long size = version.getFileSize();
                switch (QueryInfo.getInstance().getPolicyValue(QueryInfo.DOWNLOAD_PATH_SERVER, Integer.class)) {
                    case QueryInfo.DOWNLOAD_PATH_IGNORE:
                        switch (DeviceInfoUtil.getInstance().getPath()) {
                            case StorageUtil.PATH_INTERNAL:
                                if (!StorageUtil.checkInsideSpaceAvailable(this, size)) {
                                    LogUtil.d("no download reason : space not enough");
                                    ReportData.postInstallResult(this, false, Status.UPDATE_STATUS_SDCARD_ILLEGAL, null);
                                    DialogUtil.showPositiveDialog(this, getString(R.string.sdCard_upgrade_memory_space_not_enough),
                                            getString(R.string.sdcard_crash_or_unmount), this);
                                    return;
                                }
                                break;
                            case StorageUtil.PATH_EXTERNAL:
                                if (!StorageUtil.isSdcardMounted(this)) {
                                    LogUtil.d("no download reason : no sd card mounted");
                                    ReportData.postInstallResult(this, false, Status.UPDATE_STATUS_NO_SDCARD, null);
                                    DialogUtil.showPositiveDialog(this, getString(R.string.not_support_fota_title),
                                            getString(R.string.unmount_sdcard), this);
                                    return;
                                }
                                if (!StorageUtil.checkOutsideSpaceAvailable(this, size)) {
                                    LogUtil.d("no download reason : sd card status illegal");
                                    ReportData.postInstallResult(this, false, Status.UPDATE_STATUS_SDCARD_ILLEGAL, null);
                                    DialogUtil.showPositiveDialog(this, getString(R.string.sdCard_upgrade_memory_space_not_enough),
                                            getString(R.string.sdcard_crash_or_unmount), this);
                                    return;
                                }
                                break;
                            default:
                                if (!StorageUtil.checkSpaceAvailable(this, size)) {
                                    LogUtil.d("no download reason : space not enough");
                                    ReportData.postInstallResult(this, false, Status.UPDATE_STATUS_SDCARD_ILLEGAL, null);
                                    DialogUtil.showPositiveDialog(this, getString(R.string.sdCard_upgrade_memory_space_not_enough),
                                            getString(R.string.sdcard_crash_or_unmount), this);
                                    return;
                                }
                                break;
                        }
                        break;
                    case QueryInfo.DOWNLOAD_PATH_INSIDE:
                        if (!StorageUtil.checkInsideSpaceAvailable(this, size)) {
                            LogUtil.d("no download reason : space not enough");
                            ReportData.postInstallResult(this, false, Status.UPDATE_STATUS_SDCARD_ILLEGAL, null);
                            DialogUtil.showPositiveDialog(this, getString(R.string.sdCard_upgrade_memory_space_not_enough),
                                    getString(R.string.sdcard_crash_or_unmount), this);
                            return;
                        }
                        break;
                    case QueryInfo.DOWNLOAD_PATH_OUTSIDE:
                        if (!StorageUtil.isSdcardMounted(this)) {
                            LogUtil.d("no download reason : no sd card mounted");
                            ReportData.postInstallResult(this, false, Status.UPDATE_STATUS_NO_SDCARD, null);
                            DialogUtil.showPositiveDialog(this, getString(R.string.not_support_fota_title),
                                    getString(R.string.unmount_sdcard), this);
                            return;
                        }
                        if (!StorageUtil.checkOutsideSpaceAvailable(this, size)) {
                            LogUtil.d("no download reason : sd card status illegal");
                            ReportData.postInstallResult(this, false, Status.UPDATE_STATUS_SDCARD_ILLEGAL, null);
                            DialogUtil.showPositiveDialog(this, getString(R.string.sdCard_upgrade_memory_space_not_enough),
                                    getString(R.string.sdcard_crash_or_unmount), this);
                            return;
                        }
                        break;
                }
            }
            if (NetWorkUtil.isMobileConnected(this)) {
                showDownloadNoticeDialog();
            } else {
                DownVersion.getInstance().download(this, DownVersion.MANUAL);
                int version_status = Status.STATE_DOWNLOADING;
                Status.setVersionStatus(this, version_status);
                mFooterLayout.init(version_status);

            }
        } else {
            LogUtil.d("no download reason : no net connect");
            showNoNetWorkDialog();
        }



    }


    private void agreeDownload(){
        if (NetWorkUtil.isConnected(this)) {
            LogUtil.d("");
            boolean isOnlyWifi = PreferencesUtils.getBoolean(this, Setting.DOWNLOAD_ONLY_WIFI, DeviceInfoUtil.getInstance().isOnlyWifi());
            boolean isMobile = NetWorkUtil.isMobileConnected(this);
            if (isOnlyWifi && isMobile) {//客户端配置了仅wifi,或者后台配置了仅wifi,会提醒用户
                LogUtil.d("no download reason : only support wifi update but mobile wifi off");
                ReportData.postInstallResult(this, false, Status.UPDATE_STATUS_ONLY_WIFI, null);
                DialogUtil.showPositiveDialog(this, getString(R.string.not_support_fota_title),
                        getString(R.string.setting_network_tip), this);
                return;
            }
            VersionBean version = QueryInfo.getInstance().getVersionInfo();
            if (version != null) {
                long size = version.getFileSize();
                switch (QueryInfo.getInstance().getPolicyValue(QueryInfo.DOWNLOAD_PATH_SERVER, Integer.class)) {
                    case QueryInfo.DOWNLOAD_PATH_IGNORE:
                        switch (DeviceInfoUtil.getInstance().getPath()) {
                            case StorageUtil.PATH_INTERNAL:
                                if (!StorageUtil.checkInsideSpaceAvailable(this, size)) {
                                    LogUtil.d("no download reason : space not enough");
                                    ReportData.postInstallResult(this, false, Status.UPDATE_STATUS_SDCARD_ILLEGAL, null);
                                    DialogUtil.showPositiveDialog(this, getString(R.string.sdCard_upgrade_memory_space_not_enough),
                                            getString(R.string.sdcard_crash_or_unmount), this);
                                    return;
                                }
                                break;
                            case StorageUtil.PATH_EXTERNAL:
                                if (!StorageUtil.isSdcardMounted(this)) {
                                    LogUtil.d("no download reason : no sd card mounted");
                                    ReportData.postInstallResult(this, false, Status.UPDATE_STATUS_NO_SDCARD, null);
                                    DialogUtil.showPositiveDialog(this, getString(R.string.not_support_fota_title),
                                            getString(R.string.unmount_sdcard), this);
                                    return;
                                }
                                if (!StorageUtil.checkOutsideSpaceAvailable(this, size)) {
                                    LogUtil.d("no download reason : sd card status illegal");
                                    ReportData.postInstallResult(this, false, Status.UPDATE_STATUS_SDCARD_ILLEGAL, null);
                                    DialogUtil.showPositiveDialog(this, getString(R.string.sdCard_upgrade_memory_space_not_enough),
                                            getString(R.string.sdcard_crash_or_unmount), this);
                                    return;
                                }
                                break;
                            default:
                                if (!StorageUtil.checkSpaceAvailable(this, size)) {
                                    LogUtil.d("no download reason : space not enough");
                                    ReportData.postInstallResult(this, false, Status.UPDATE_STATUS_SDCARD_ILLEGAL, null);
                                    DialogUtil.showPositiveDialog(this, getString(R.string.sdCard_upgrade_memory_space_not_enough),
                                            getString(R.string.sdcard_crash_or_unmount), this);
                                    return;
                                }
                                break;
                        }
                        break;
                    case QueryInfo.DOWNLOAD_PATH_INSIDE:
                        if (!StorageUtil.checkInsideSpaceAvailable(this, size)) {
                            LogUtil.d("no download reason : space not enough");
                            ReportData.postInstallResult(this, false, Status.UPDATE_STATUS_SDCARD_ILLEGAL, null);
                            DialogUtil.showPositiveDialog(this, getString(R.string.sdCard_upgrade_memory_space_not_enough),
                                    getString(R.string.sdcard_crash_or_unmount), this);
                            return;
                        }
                        break;
                    case QueryInfo.DOWNLOAD_PATH_OUTSIDE:
                        if (!StorageUtil.isSdcardMounted(this)) {
                            LogUtil.d("no download reason : no sd card mounted");
                            ReportData.postInstallResult(this, false, Status.UPDATE_STATUS_NO_SDCARD, null);
                            DialogUtil.showPositiveDialog(this, getString(R.string.not_support_fota_title),
                                    getString(R.string.unmount_sdcard), this);
                            return;
                        }
                        if (!StorageUtil.checkOutsideSpaceAvailable(this, size)) {
                            LogUtil.d("no download reason : sd card status illegal");
                            ReportData.postInstallResult(this, false, Status.UPDATE_STATUS_SDCARD_ILLEGAL, null);
                            DialogUtil.showPositiveDialog(this, getString(R.string.sdCard_upgrade_memory_space_not_enough),
                                    getString(R.string.sdcard_crash_or_unmount), this);
                            return;
                        }
                        break;
                }
            }
//            if (NetWorkUtil.isMobileConnected(this)) {
//                showDownloadNoticeDialog();
//            } else {
                DownVersion.getInstance().download(this, DownVersion.MANUAL);
                int version_status = Status.STATE_DOWNLOADING;
                Status.setVersionStatus(this, version_status);
                mFooterLayout.init(version_status);

//            }
        } else {
            LogUtil.d("no download reason : no net connect");
            showNoNetWorkDialog();
        }
    }



    // click Button [pause]
    private void onClickPause() {
        LogUtil.d("enter");
        DownVersion.getInstance().pause(this);
    }

    //click Button [cancel]
    private void onClickCancel() {
        LogUtil.d("enter");
        int status = Status.getVersionStatus(this);
        if (status == Status.STATE_DOWNLOADING) {
            DownVersion.getInstance().pause(this);
            initPauseView(false);
        }
        DialogUtil.showDialog(this, getString(R.string.cancel_download_title),
                getString(R.string.cancel_download_content),
                getString(R.string.cancel_download_positive_btn),
                new ClickCallback() {
                    @Override
                    public void onClick() {
                        doConfirmCancel();
                    }
                },
                getString(R.string.cancel_download_negative_btn),
                null,
                this);
    }

    private void onClickResume(boolean isTouch) {
        LogUtil.d("enter");
        if (DeviceInfoUtil.getInstance().getCanUse() == 0) {
            LogUtil.d("can not use fota to onClickResume;");
            showDisableDialog();
            return;
        }
        if (MyApplication.isCalling() && !NetWorkUtil.isWiFiConnected(this)) {
            DialogUtil.showPositiveDialog(this, getString(R.string.not_support_fota_title),
                    getString(R.string.call_warn), this);
            return;
        }
        if (!NetWorkUtil.isWiFiConnected(this) && NetWorkUtil.is2GConnected(this)) {
            LogUtil.d("currrent is 2G network");
            DialogUtil.showPositiveDialog(this, getString(R.string.not_support_fota_title), getString(R.string.not_allow_with2g));
            return;
        }
        if (NetWorkUtil.isMobileConnected(this) && NetWorkUtil.isRoaming(this)) {
            DialogUtil.showDialog(this, getString(R.string.not_support_fota_title), getString(R.string.network_roaming),
                    getString(R.string.ota_wifi_setting), new ClickCallback() {
                        @Override
                        public void onClick() {
                            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        }
                    }, getString(android.R.string.cancel), null);
            return;
        }
        if (NetWorkUtil.isConnected(this)) {
            boolean isOnlyWifi = PreferencesUtils.getBoolean(this, Setting.DOWNLOAD_ONLY_WIFI, DeviceInfoUtil.getInstance().isOnlyWifi());
            boolean isMobile = NetWorkUtil.isMobileConnected(this);
            if (isOnlyWifi && isMobile) {
                DialogUtil.showPositiveDialog(this, getString(R.string.not_support_fota_title),
                        getString(R.string.setting_network_tip), this);
                LogUtil.d("no download reason : only support wifi update but mobile wifi off");
                ReportData.postInstallResult(this, false, Status.UPDATE_STATUS_ONLY_WIFI, null);
                return;
            }
            VersionBean version = QueryInfo.getInstance().getVersionInfo();
            if (version != null) {
                int status = StorageUtil.checkIsAvailable(this, version.getFileSize());
                if (status == StorageUtil.SDCARD_STATE_UNMOUNT) {
                    LogUtil.d("no download reason : no sd card mounted");
                    ReportData.postInstallResult(this, false, Status.UPDATE_STATUS_NO_SDCARD, null);
                    DialogUtil.showPositiveDialog(this, getString(R.string.not_support_fota_title),
                            getString(R.string.unmount_sdcard), this);
                    return;
                }
                if (status == StorageUtil.SDCARD_STATE_INSUFFICIENT) {
                    LogUtil.d("no download reason : sd card status not illegal");
                    ReportData.postInstallResult(this, false, Status.UPDATE_STATUS_SDCARD_ILLEGAL, null);
                    DialogUtil.showPositiveDialog(this, getString(R.string.sdCard_upgrade_memory_space_not_enough),
                            getString(R.string.sdcard_crash_or_unmount), this);
                    return;
                }
            }
            if (NetWorkUtil.isMobileConnected(this)) {
                if (isTouch) {
                    showDownloadNoticeDialog();
                } else {
                    int autoDownload = QueryInfo.getInstance().getPolicyValue(QueryInfo.DOWNLOAD_AUTO, Integer.class);
                    int autonettype = QueryInfo.getInstance().getPolicyValue(QueryInfo.DOWNLOAD_WIFI, Integer.class);
                    if (autoDownload == 1 && autonettype == 0) {
                    } else {
                        showDownloadNoticeDialog();
                    }
                }
            } else {
                DownVersion.getInstance().download(this, DownVersion.MANUAL);
                int version_status = Status.STATE_DOWNLOADING;
                Status.setVersionStatus(this, version_status);
                mFooterLayout.init(version_status);
            }
        } else {
            LogUtil.d("no download reason : no net connect");
            showNoNetWorkDialog();
        }
    }

    //click Button [install delay]
    private void onClickInstallLater() {
        if (DeviceInfoUtil.getInstance().getCanUse() == 0) {
            LogUtil.d("can not use fota to onClickInstallLater");
            showDisableDialog();
            return;
        }
        LogUtil.d("enter");
        Intent intent = new Intent("com.adups.fota");
        intent.putExtra(TaskID.STATUS, TaskID.TASK_INSTALL_LATER);
        intent.putExtra(Setting.INTENT_PARAM_NOTIFY_ID, NotificationManager.NOTIFY_DL_COMPLETED);
        intent.setClass(getApplicationContext(), TimePickerWindow.class);
        startActivity(intent);
//        int installLaterCount = PreferencesUtils.getInt(this, Setting.INSTALL_LATER_COUNT, 0);
//        installLaterCount++;
//        LogUtil.d("installLaterCount=" + installLaterCount);
//        PreferencesUtils.putInt(this, Setting.INSTALL_LATER_COUNT, installLaterCount);
//        long schedule_time = PreferencesUtils.getLong(this, Setting.FOTA_INSTALL_DELAY_SCHEDULE);
//        LogUtil.d("schedule_time = " + schedule_time);
//        if (schedule_time <= 0) {
//            showInstallDelayDialog();
//        } else {
//            finish();
//        }
    }

    // click Confirm button on  Dialog
    private void doConfirmCancel() {
        LogUtil.d("enter");
        mProgress.setProgress(0);
        DownVersion.getInstance().onClickCancel(this);
        Status.setVersionStatus(this, Status.STATE_QUERY_NEW_VERSION);
        try {
            SystemSettingUtil.putInt("fota_updateavailable", 0);
        } catch (Exception e) {
        }
        userPause = 1;
        initIdleView();
    }

    private void showInstallDelayDialog() {
        delayTimeCounts = 15;
        String title = getString(R.string.remind_last_time) + "(" + delayTimeCounts + ")";
        InstallDelayView installDelayView = new InstallDelayView(this);
        installDelayView.setOnItemClickListener(this);
        dialog = DialogUtil.showNoButtonCustomDialog(this, title, Gravity.CENTER, this, installDelayView);
        delayTimeCounts--;
        mHandler.sendEmptyMessageDelayed(MSG_DELAY_TIME, 1000);
    }

    private void showNoNetWorkDialog() {
        NetworkWarnView networkWarnView = new NetworkWarnView(this);
        DialogUtil.showCustomDialog(this, getString(R.string.not_support_fota_title), this, networkWarnView);
    }

    //show dialog  every time before download when network type is  mobile
    private void showDownloadNoticeDialog() {
        NetworkWarnView networkWarnView = new NetworkWarnView(this);
        networkWarnView.setTitle(getString(R.string.ota_no_wifi_tip));
        DialogUtil.showCustomDialog(this, getString(R.string.not_support_fota_title),
                getString(android.R.string.cancel), new ClickCallback() {
                    @Override
                    public void onClick() {
                        LogUtil.d("no download reason : user cancel");
                        ReportData.postDownload(GoogleOtaClient.this, ReportData.DOWN_STATUS_CANCEL, 0);
                    }
                }, getString(R.string.btn_download), new ClickCallback() {
                    @Override
                    public void onClick() {
                        DownVersion.getInstance().download(GoogleOtaClient.this, DownVersion.MANUAL);
                        int version_status = Status.STATE_DOWNLOADING;
                        Status.setVersionStatus(GoogleOtaClient.this, version_status);
                        mFooterLayout.init(version_status);
                    }
                }, this, networkWarnView);
    }

    private void onClickSchedule(int index) {
        LogUtil.d("delay time: " + schedule_array[index]);
        PreferencesUtils.putLong(this, Setting.FOTA_INSTALL_DELAY_SCHEDULE, schedule_array[index]);
        AlarmManager.installDelay(this, schedule_array[index] + System.currentTimeMillis());
        saveDelayInstallData();
        NotificationManager.getInstance().cancel(this, NotificationManager.NOTIFY_DL_COMPLETED);
        closeDialog();
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            openDebug();
        }
    }

    /**
     * user click 5 times ,then open dialog about app's info [imei,imsi,mdi,version_name,version_code ...]
     * Button Click    {@linkplain  #exportData()#openDebug()#closeDebug()}
     */
    private void onClickTitle() {
        DeviceFunctionView deviceFunctionView = new DeviceFunctionView(this);
        String content =
                "APK Release Date:" + BuildConfig.APK_BUILD_DATE + "\n"
                        + "FCM ID: " + SpManager.getFcmId() + "\n"
                        + "IMEI: " + DeviceInfoUtil.getInstance().getDeviceIMEI(this) + "\n"
                        + "MID: " + MidUtil.getSyncMid(this) + "\n"
                        + "SPN: " + DeviceInfoUtil.getInstance().getMainSPN(this) + "\n"
                        + "AppVersionName: " + PackageUtils.getAppVersionName(this)
                        + BuildConfig.AND_VERSION + "_" + BuildConfig.APK_BUILD_DATE + "\n"
                        + "AppVersionCode: " + PackageUtils.getAppVersionCode(this) + "\n"
                        + "version: " + DeviceInfoUtil.getInstance().getLocalVersion() + "\n"
                        + "project: " + DeviceInfoUtil.getInstance().getProject();
        String imei_formal = "^[A-Za-z0-9:]+$";
        String imeiStr = DeviceInfoUtil.getInstance().getDeviceIMEI(this);
        if (!TextUtils.isEmpty(imeiStr) && !(DeviceInfoUtil.getInstance().getDeviceIMEI(this)).matches(imei_formal))
            ToastUtil.showToast(R.string.match_imei);
        deviceFunctionView.setInfo(content);
        deviceFunctionView.setOnFunctionClickListener(this);
        DialogUtil.showCustomDialog(this, getString(R.string.app_name), this, deviceFunctionView);
    }

    private void exportData() {
        final String oldPath = getFilesDir().getParent() + "/shared_prefs/adupsfota.xml";
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            ToastUtil.showToast(R.string.sdcard_crash_or_unmount);
            return;
        }
        File folder = new File(StorageUtil.getCatchLogPath(this) + "/fota");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        final String newPath = folder.getAbsolutePath() + "/adupsfota.txt";
        final String versionFileSource = getFilesDir().getAbsolutePath() + "/" + Const.VERSION_FILE;
        final String versionFileDest = folder.getAbsolutePath() + "/" + Const.VERSION_FILE;
        new Thread(new Runnable() {

            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                FileUtil.copy(oldPath, newPath, false);
                FileUtil.copy(versionFileSource, versionFileDest, false);
            }
        }).start();
        ToastUtil.showToast(getString(R.string.export_data) + " to " + folder.getAbsolutePath());
    }

    private void closeDebug() {
        LogUtil.setSaveLog(false);
        PreferencesUtils.putBoolean(this, Setting.DEBUG_SWITCH, false);
        ToastUtil.showToast(R.string.stop_catch_log);
    }

    private void deviceRooted() {
        LogUtil.d("enter");
        Status.idleReset(this);
        initIdleView();
        DialogUtil.showBaseCustomDialog(this,
                R.mipmap.ota_root, getString(R.string.ota_device_rooted_content), Gravity.CENTER,
                getString(R.string.ota_full_rom_check), new ClickCallback() {
                    @Override
                    public void onClick() {
                        queryFullRom();
                    }
                }, this);
    }

    /**
     * EventBus  event notify
     *
     * @param event {@linkplain EventMessage }
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(EventMessage event) {
        LogUtil.d(" what  = " + event.getWhat() +
                "; param1= " + event.getArg1() +
                "; param2= " + event.getArg2() +
                "; param3= " + event.getArg3() +
                "");
        switch (event.getWhat()) {
            case Event.QUERY:
                queryCallback(event);
                break;
            case Event.DOWNLOAD:
                downloadCallback(event);
                break;
            case Event.INSTALL:
                installCallback(event);
                break;
        }
    }

    private void setDownloadProgress(int pro) {
        if (DeviceInfoUtil.getInstance().isNoRing()) {
            progressTextLayout.setProgress(pro);
        } else {
            mProgress.setDownLoadProgress(pro);
        }
    }

    private void notifyDownloading(int progress) {
        //lirenqi 20171107 add download display error
        if (Status.getVersionStatus(this) != Status.STATE_DOWNLOADING) {
            int version_status = Status.STATE_DOWNLOADING;
            Status.setVersionStatus(GoogleOtaClient.this, version_status);
            mUpdateTip.setVisibility(View.VISIBLE);
            loadReleaseNotes();
            mFooterLayout.init(version_status);
        }
        setDownloadProgress(progress);
    }

    //防止关闭其他界面的弹窗，增加判断是否在前台
    @Override
    public void closeDialog() {
        if (isFront()) super.closeDialog();
    }

    //query result callback
    private void queryCallback(EventMessage evt) {
        closeDialog();
        int version_status = Status.getVersionStatus(this);
        switch (evt.getArg1()) {
            case Event.QUERY_INIT_VERSION:
                initIdleView();
                PreferencesUtils.putBoolean(this, "download_now_broadcast", false);
                break;
            case Event.QUERY_RUNNING:
                if (version_status == Status.STATE_QUERY_NEW_VERSION && isFront()) {
                    View view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
                    DialogUtil.showNoButtonCustomDialog(this, null, this, view);
                }
                break;
            case Event.QUERY_ONGOING:
                break;
            case Event.QUERY_CHANGE_VERSION:
            case Event.QUERY_NEW_VERSION:
                userPause = 0;
                initNewVersionView();
                break;
            //lirenqi 20171108 modify for policy change
            case Event.QUERY_POLICY_CHANGE:
                if (version_status < Status.STATE_DOWNLOADING) {
                    initNewVersionView();
                }
                break;
            case Event.QUERY_SAME_VERSION:
            case Event.QUERY_NO_VERSION:
                //brave 无差分版本
                noFindVersion();
                PreferencesUtils.putBoolean(this, "download_now_broadcast", false);
                break;
            case Event.ERROR_SDCARD_NOT_ENOUGH:
                ToastUtil.showToast(R.string.sdcard_crash_or_unmount);
                break;
            case Event.ERROR_IO://接收检测时网络异常信息
                PreferencesUtils.putBoolean(this, "download_now_broadcast", false);
                tipsMessage(evt);
                break;
            case Event.ERROR_UNKNOWN:
                PreferencesUtils.putBoolean(this, "download_now_broadcast", false);
                tipsMessage(evt);
                break;
            case Status.UPDATE_STATUS_ROM_DAMAGED:
                PreferencesUtils.putBoolean(MyApplication.getAppContext(), "download_now_broadcast", false);
                deviceRooted();
                break;
            case Event.QUERY_FULL_ROM:
                DownPackage.getInstance().cancel(this);
                Status.idleReset(this);
                initIdleView();
                mHandler.sendEmptyMessageDelayed(MSG_FULL_CHECK, 1200);
                //  queryFullRom();
                break;
            case Event.LEFT_MENU_OPEN:
                updateSafeFlag();
                break;
        }
    }

    public void tipsMessage(EventMessage evt) {
        try {
            if (Status.getVersionStatus(this) == Status.STATE_QUERY_NEW_VERSION && isFront()) {
                if (NetWorkUtil.isConnected(this)) {
                    if (evt.getArg2() != 0 || evt.getObject() == null) {//判断是否存在message信息
                        ToastUtil.showToast(getString(R.string.network_error) + "(" + evt.getArg2() + ")");
                    } else {
                        String domain_com = ServerApi.SERVER_DOMAIN.replaceAll("https://", "");
                        String domain_cn = ServerApi.SERVER_DOMAIN2.replaceAll("https://", "");
                        String sever = "fota server ";
                        ToastUtil.showToast(getString(R.string.network_error) + "(" +
                                evt.getObject().toString().replaceAll(domain_com, sever)
                                        .replaceAll(domain_cn, sever) + ")");
                    }
                } else {
                    ToastUtil.showToast(R.string.ota_toast_no_network);
                }
            }
        } catch (Exception e) {
            ToastUtil.showToast(R.string.ota_toast_no_network);
        }
    }

    //download result callback
    private void downloadCallback(EventMessage evt) {
        switch (evt.getArg1()) {
            case Event.DOWNLOAD_START:
                closeDialog();
                initDownloadStartView();
                break;
            case Event.DOWNLOAD_SUCCESS:
                if (isFront())
                    initInstallView();
                break;
            case Event.DOWNLOAD_PROGRESS:
//                if (DeviceInfoUtil.getInstance().getCanUse() == 0){
//
//                }
                notifyDownloading(Long.valueOf(evt.getArg2()).intValue());
                break;
            case Event.DOWNLOAD_PAUSE:
                Status.setVersionStatus(this, Status.STATE_PAUSE_DOWNLOAD);
//                shakeView.setVisibility(View.VISIBLE);
//                shakeView.setContent(R.string.shake_to_download);
                mFooterLayout.init(Status.STATE_PAUSE_DOWNLOAD);
                break;
            case Event.DOWNLOAD_FAIL:
                //下载过程不处理
                int version_status = Status.getVersionStatus(this);
                if ((version_status != Status.STATE_DOWNLOADING) && (version_status != Status.STATE_PAUSE_DOWNLOAD)) {
                    Status.idleReset(this);
                    initIdleView();
                    break;
                }
                downloadFail(evt.getObject().toString());
                break;
            case Event.NETWORK_TYPE_WIFI_TO_MOBILE:
                initPauseView(false);
                downloadWifiToMobile();
                break;
            case Event.NETWORK_TYPE_MOBILE_TO_WIFI:
                break;
        }
    }

    //install result callback
    private void installCallback(EventMessage evt) {
        closeDialog();
        int level = DeviceInfoUtil.getInstance().getBattery();
        if (evt.getObject() != null && !TextUtils.isEmpty(evt.getObject().toString()) &&
                evt.getObject().toString().equals(Recovery.AB_FLAG)) {
            LogUtil.d("ab installCallback enter");
            if (evt.getArg3() == Recovery.AB_INSTALL_RESULT) {   //install result, 1 means success
                if (evt.getArg1() == UpdateEngine.ErrorCodeConstants.SUCCESS) {
                    //系统安装成功
                    DialogUtil.showDialog(this,
                            getString(R.string.ab_install_success), getString(R.string.updated_need_reboot),
                            new ClickCallback() {
                                @Override
                                public void onClick() {
                                    Recovery.getInstance().reboot(GoogleOtaClient.this);
                                }
                            }, this);
                    showAbView(true, false);
                    battery_tip.setText(R.string.updated_need_reboot);
                    mFooterLayout.init(Status.STATE_REBOOT);
                } else {
                    //系统安装失败
                    LogUtil.d("installCallback,install fail");
                    Status.idleReset(GoogleOtaClient.this);
                    initIdleView();
                    DialogUtil.showPositiveDialog(this, getString(R.string.ab_install_fail),
                            getString(R.string.ab_isnatll_fail_reason) + evt.getArg1(), this);
                }
            } else if (evt.getArg3() == Recovery.AB_INSTALLING) {  // 0 means install progress
                LogUtil.d("installCallback,installing");
                mFooterLayout.init(Status.STATE_AB_UPDATING);
                showAbView(true, true);
                update_txt.setText(R.string.ab_installing);
                int progress = (int) evt.getArg2();
                LogUtil.d("installCallback,installing progress = " + progress);
                if (progress > 100) {
                    progress = 100;
                }
                pro_txt.setText(progress + "%");
                progress_update_id.setProgress(progress);
            } else if (evt.getArg3() == Recovery.AB_BATTERY_LOW) {  //download complete,but battery is low,can not install
                showAbView(true, false);
                mFooterLayout.init(Status.STATE_DL_PKG_COMPLETE);
                battery_tip.setText(getString(R.string.ota_battery_low, level));
            } else if (evt.getArg3() == Recovery.AB_GET_PARAMS_FAIL) {  //can not get parms when ab updating
                Status.idleReset(GoogleOtaClient.this);
                initIdleView();
                DialogUtil.showPositiveDialog(this, getString(R.string.ab_install_fail),
                        getString(R.string.ab_parms_illegal), this);
            } else if (evt.getArg3() == Recovery.AB_CONNECT_REBOOT_FAIL) {  //fota connect reboot fail
                Status.idleReset(GoogleOtaClient.this);
                initIdleView();
                DialogUtil.showPositiveDialog(this, getString(R.string.ab_install_fail),
                        getString(R.string.ab_connect_fail), this);
            } else if (evt.getArg3() == Recovery.AB_VERIFYING) {  //ab update verifying
                DialogUtil.showCustomDialog(this, R.layout.dialog_update_unzip, this);
            } else if (evt.getArg3() == Recovery.AB_PACKAGE_NOT_EXIST) {  //ab升级升级包不存在
                Status.idleReset(GoogleOtaClient.this);
                initIdleView();
                DialogUtil.showPositiveDialog(this, getString(R.string.ab_install_fail),
                        getString(R.string.sdCard_upgrade_find_update_file_fail), this);
            } else if (evt.getArg3() == Recovery.AB_REBOOT_IS_LOW) {  //reboot is low
                showAbView(true, false);
                mFooterLayout.init(Status.STATE_DL_PKG_COMPLETE);
                battery_tip.setText(String.format("ab-%s", getString(R.string.not_support_version)));
                DialogUtil.showPositiveDialog(this, getString(R.string.not_support_fota_title),
                        "ab-" + getString(R.string.not_support_version), this);
            }
            return;
        }
        switch (evt.getArg1()) {
            case Status.UPDATE_STATUS_OK:
                LogUtil.d("UPDATE_STATUS_OK");
                if (!Install.isSupportAbUpdate()) {
                    DialogUtil.showCustomDialog(this, R.layout.dialog_update_reboot, this);
                }
                break;
            case Status.UPDATE_FOTA_NO_PKG:
            case Status.UPDATE_FOTA_RENAME_FAIL:
            case Status.UPDATE_STATUS_UNZIP_ERROR:
            case Status.UPDATE_STATUS_RUNCHECKERROR:
                Status.idleReset(GoogleOtaClient.this);
                initIdleView();
                DialogUtil.showPositiveDialog(this, getString(R.string.not_support_fota_title),
                        getString(R.string.package_unzip_error), this);
                break;
            case Status.UPDATE_FOTA_PKG_MD5_FAIL:
            case Status.UPDATE_FOTA_NO_MD5:
            case Status.UPDATE_STATUS_CKSUM_ERROR:
                Status.idleReset(GoogleOtaClient.this);
                initIdleView();
                DialogUtil.showPositiveDialog(this, getString(R.string.package_error_title),
                        getString(R.string.package_error_message_invalid), this);
                break;
            case Status.UPDATE_STATUS_ROM_DAMAGED:
                deviceRooted();
                break;
            default:
                Status.idleReset(this);
                initIdleView();
                DialogUtil.showPositiveDialog(this, getString(R.string.not_support_fota_title),
                        getString(R.string.not_support_version), this);
                break;
        }
    }

    private void showAbView(boolean showAb, boolean showPro) {
        if (showAb) {
            pre_view.setVisibility(View.GONE);
            ab_view.setVisibility(View.VISIBLE);
            if (showPro) {
                pro_view.setVisibility(View.VISIBLE);
                battery_tip.setVisibility(View.GONE);
            } else {
                pro_view.setVisibility(View.GONE);
                battery_tip.setVisibility(View.VISIBLE);
            }
        } else {
            pre_view.setVisibility(View.VISIBLE);
            ab_view.setVisibility(View.GONE);
        }
    }

    //execute function according to status
    private void statusAction(int status) {
        switch (status) {
            case Status.STATE_DL_PKG_COMPLETE:
                onClickInstallNow();
                break;
            case Status.STATE_NEW_VERSION_READY:
                onClickDownload();
                break;
        }
    }

    private void noFindVersion() {
        LogUtil.d("enter");
        int version_status = Status.getVersionStatus(this);
        if (version_status == Status.STATE_QUERY_NEW_VERSION) {
            mProgress.setVersionTip(getString(R.string.no_new_version));
        }
        if (version_status == Status.STATE_QUERY_NEW_VERSION && isFront()) {
            ToastUtil.showToast(R.string.no_new_version);
        }
    }

    private void downloadWifiToMobile() {
        LogUtil.d("enter");
        boolean isOnlyWifi = PreferencesUtils.getBoolean(this, Setting.DOWNLOAD_ONLY_WIFI, DeviceInfoUtil.getInstance().isOnlyWifi());
        boolean isMobile = NetWorkUtil.isMobileConnected(this);
        if (isOnlyWifi && isMobile) {
            LogUtil.d(" ota network  wifi  to mobile  dialog");
            DialogUtil.showPositiveDialog(this, getString(R.string.not_support_fota_title),
                    getString(R.string.setting_network_tip), this);
            return;
        }
        if (NetWorkUtil.isMobileConnected(this)) {
            showDownloadNoticeDialog();
        }
    }

    private void downloadFail(String message) {
        LogUtil.d("enter");
        if (userPause == 1) {
            Status.idleReset(this);
            LogUtil.d("user cancel cause download fail");
            userPause = 0;
        } else {
            initPauseView(false);
            if (!TextUtils.isEmpty(message)) {
                if (message.equalsIgnoreCase(getString(R.string.package_unzip_error))) {
                    initIdleView();
                    showToastOrDialog(message);
                    Status.idleReset(this);
                    return;
                }
                if (!message.equalsIgnoreCase(ERROR_REASON_RESPONSE_UNDONE) &&
                        !message.equalsIgnoreCase(ERROR_REASON_PAUSE)) {
                    showToastOrDialog(getString(R.string.ota_toast_no_network));
                }
            } else {
                if (!NetWorkUtil.isConnected(this)) {
                    showToastOrDialog(getString(R.string.ota_toast_no_network));
                    return;
                }
                VersionBean version = QueryInfo.getInstance().getVersionInfo();
                if (version != null) {
                    switch (StorageUtil.checkIsAvailable(this, version.getFileSize())) {
                        case StorageUtil.SDCARD_STATE_UNMOUNT:
                            showToastOrDialog(getString(R.string.unmount_sdcard));
                            break;
                        case StorageUtil.SDCARD_STATE_INSUFFICIENT:
                            showToastOrDialog(getString(R.string.sdcard_crash_or_unmount));
                            break;
                    }
                }
            }
        }
    }

    private void showToastOrDialog(String message) {
        LogUtil.d("showToastOrDialog,message=" + message);
        if (isFront()) {
            ToastUtil.showToast(message);
        }
    }

    private void queryFullRom() {
        LogUtil.d("enter");
        if (NetWorkUtil.isConnected(this)) {
            QueryVersion.getInstance().onQuery(this, QueryVersion.QUERY_MANUAL, QueryVersion.QUERY_VERSION_FULL);
        } else {
            showNoNetWorkDialog();
        }
    }

    private void saveDelayInstallData() {
        ReportData.postInstall(this, ReportData.INSTALL_STATUS_DELAY);
    }

    private boolean isOverRemindDate() {
        String pre_data = PreferencesUtils.getString(this, Setting.FOTA_CHECK_ONCE_DAY, "");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String cur_date = dateFormat.format(new Date());
        if (!pre_data.equals(cur_date)) {
            PreferencesUtils.putString(this, Setting.FOTA_CHECK_ONCE_DAY, cur_date);
            return true;
        }
        return false;
    }

    //监听返回键
    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
            mDrawerLayout.closeDrawer(Gravity.LEFT);
//        } else if (Status.getVersionStatus(this) == Status.STATE_DOWNLOADING ||
//                Status.getVersionStatus(this) == Status.DOWNLOAD_STATUS_DOWNLOADING ||
//                Status.getVersionStatus(this) == Status.STATE_AB_UPDATING) {
//            moveTaskToBack(true);
        } else {
            finish();
        }
    }

    private void updateSafeFlag() {
        if (!isActiveSafe) {
            isActiveSafe = true;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        LogUtil.d("keyCode = " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_MENU && DeviceInfoUtil.getInstance().isShowBtnPop()) {//防止后台配置不显示设置按钮，非触屏下按菜单键还可以调出设置弹框
            new PopWindowsLayout().setPopWinLayout(this, setting);
            return true;
        }
        LogUtil.d("keyCode : " + keyCode);
        if (DeviceInfoUtil.getInstance().isNoTouch()) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                mFooterLayout.setNoTouchState();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public int getPercent() {
        VersionBean model = QueryInfo.getInstance().getVersionInfo();
        if (model == null) {
            return 0;
        }
        return SpManager.getDownloadPercent();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mFooterLayout.setNoTouchState();
    }

//    @Override
//    public void onShaking() {
//        closeDialog();
//        int status = Status.getVersionStatus(this);
//        LogUtil.d("status : " + status);
//        switch (status) {
//            case Status.STATE_QUERY_NEW_VERSION:
//                onClickQuery();
//                break;
//            case Status.STATE_NEW_VERSION_READY:
//                onClickDownload();
//                break;
//            case Status.STATE_PAUSE_DOWNLOAD:
//                onClickResume(true);
//                break;
//            case Status.STATE_DL_PKG_COMPLETE:
//                onClickInstallNow();
//                break;
//        }
//    }

    @Override
    public void onClick(int index) {
        onClickSchedule(index);
    }

    @Override
    public void onItemClick(int index) {
        switch (index) {
            case 0:
                exportData();
                break;
            case 1:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
                } else {
                    openDebug();
                }
                break;
            case 2:
                closeDebug();
                break;
        }
    }

    @Override
    public void onPhoneCalling() {
        LogUtil.d("");
        if (Status.getVersionStatus(this) == Status.STATE_DOWNLOADING)
            onClickPause();
    }

    @Override
    public void onPhoneOff() {
        LogUtil.d("");
        if (Status.getVersionStatus(this) == Status.STATE_PAUSE_DOWNLOAD)
            onClickResume(false);
    }

    private boolean isClick2Fast() {
        long time = System.currentTimeMillis() - lastClickTime;
        lastClickTime = System.currentTimeMillis();
        return 0 < time && time < 1000;
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
                    finish();
                    LogUtil.d("按了home键");

                }

            }
        }
    }

}

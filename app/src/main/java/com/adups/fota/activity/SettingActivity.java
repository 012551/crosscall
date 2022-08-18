package com.adups.fota.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.adups.fota.MyApplication;
import com.adups.fota.R;
import com.adups.fota.TimePickerWindow;
import com.adups.fota.callback.FunctionClickCallback;
import com.adups.fota.config.Const;
import com.adups.fota.config.Setting;
import com.adups.fota.download.DownVersion;
import com.adups.fota.manager.AlarmManager;
import com.adups.fota.request.RequestParam;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.DialogUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PreferencesUtils;
import com.adups.fota.view.CheckScheduleView;
import com.adups.fota.view.TitleContentView;
import com.adups.fota.view.TitleView;

import java.io.Serializable;

public class SettingActivity extends BaseActivity implements FunctionClickCallback {

    private TitleContentView checkCycle, updateWifiOnly, wifiAutoDownload,slient_night_view;
    private TextView download_install_later_time, download_later_time, install_later_time;
    private RelativeLayout download_later, download_install_later, install_later;
    private int query_schedule;
    private int queryScheduleIndex = 0;
    private Long downloadlater, installlater, downloadinstalllater;

    @Override
    protected void setTitleView(TitleView titleView) {
        titleView.setContent(getString(R.string.option_settings));
    }

    @Override
    public void initWidget() {
        setContentView(R.layout.activity_setting);
        initViews();
        initTime();
        initData();
    }

    private void initTime() {
        LogUtil.d("initTime");
        downloadlater = PreferencesUtils.getLong(this, "DOWNLOAD_DELAY", 0);
        downloadinstalllater = PreferencesUtils.getLong(this, "DOWNLOAD_INSTALL_DELAY", 0);
        installlater = PreferencesUtils.getLong(this, "INSTALL_DELAY", 0);
        LogUtil.d("downloadlater = " + downloadlater + "; installlater = " + installlater + "; downloadinstalllater = " + downloadinstalllater);
        if (downloadlater != 0) {
            download_later.setVisibility(View.VISIBLE);
            download_later_time.setText(TimePickerWindow.getDateToString(downloadlater,"HH:mm"));
        }
        if (installlater != 0) {
            install_later.setVisibility(View.VISIBLE);
            install_later_time.setText(TimePickerWindow.getDateToString(installlater,"HH:mm"));
        }
        if (downloadinstalllater != 0) {
            download_install_later.setVisibility(View.VISIBLE);
            download_install_later_time.setText(TimePickerWindow.getDateToString(downloadinstalllater,"HH:mm"));
        }
    }

    private void initViews() {
        checkCycle = findViewById(R.id.check_cycle);
        updateWifiOnly = findViewById(R.id.update_wifi_only);
        wifiAutoDownload = findViewById(R.id.wifi_auto_download);
        download_install_later_time = findViewById(R.id.download_install_later_time);
        download_later_time = findViewById(R.id.download_later_time);
        install_later_time = findViewById(R.id.install_later_time);
        slient_night_view = findViewById(R.id.night_auto_download);//zhangzhou
        download_later = findViewById(R.id.download_later);
        download_install_later = findViewById(R.id.download_install_later);
        install_later = findViewById(R.id.install_later);
        checkCycle.setOnClickListener(this);
        updateWifiOnly.setOnClickListener(this);
        wifiAutoDownload.setOnClickListener(this);
        slient_night_view.setOnClickListener(this);
        TitleContentView about = findViewById(R.id.about);
        if (MyApplication.isBootExit()) {
            about.setVisibility(View.VISIBLE);
            about.setOnClickListener(this);
        }
        if (DeviceInfoUtil.getInstance().isNoTouch()) {
            checkCycle.requestFocus();
        }
    }

    private void initData() {
        int query_schedule = PreferencesUtils.getInt(this, Setting.QUERY_LOCAL_FREQ, Setting.getDefaultFreq());
        initQuerySchedule(query_schedule);
        boolean isOnlyWifi = PreferencesUtils.getBoolean(this, Setting.DOWNLOAD_ONLY_WIFI, DeviceInfoUtil.getInstance().isOnlyWifi());
        LogUtil.d("isOnlyWifi = " + isOnlyWifi);
        updateWifiOnly.setChecked(isOnlyWifi);
        boolean isWifiAuto = PreferencesUtils.getBoolean(this, Setting.DOWNLOAD_WIFI_AUTO, DeviceInfoUtil.getInstance().isAutoWifi());
        LogUtil.d("isWifiAuto = " + isWifiAuto);
        wifiAutoDownload.setChecked(isWifiAuto);

        //zhangzhou
        boolean isSlientDownload = PreferencesUtils.getBoolean(this, Setting.SLIENT_DOWNLOAD, false);
        LogUtil.d("isSlientDownload = " + isSlientDownload);
        slient_night_view.setChecked(isSlientDownload);
    }

    private void initQuerySchedule(int level) {
        query_schedule = level;
        if (level == Setting.SCHEDULE_1) {
            queryScheduleIndex = 0;
            checkCycle.setTip(R.string.setting_autocheck_schedule1);
        } else if (level == Setting.SCHEDULE_2) {
            queryScheduleIndex = 1;
            checkCycle.setTip(R.string.setting_autocheck_schedule2);
        } else if (level == Setting.SCHEDULE_3) {
            queryScheduleIndex = 2;
            checkCycle.setTip(R.string.setting_autocheck_schedule3);
        }
    }

    /**
     * upate Setting query schedule according to which user choice
     */
    private void updateQuerySchedule(int choice_index) {
        queryScheduleIndex = choice_index;
        int res_id = R.string.setting_autocheck_schedule1;
        if (choice_index == 0) {
            query_schedule = Setting.SCHEDULE_1;
            res_id = R.string.setting_autocheck_schedule1;
        } else if (choice_index == 1) {
            res_id = R.string.setting_autocheck_schedule2;
            query_schedule = Setting.SCHEDULE_2;
        } else if (choice_index == 2) {
            res_id = R.string.setting_autocheck_schedule3;
            query_schedule = Setting.SCHEDULE_3;
        }
        checkCycle.setTip(res_id);
        PreferencesUtils.putInt(this, Setting.QUERY_LOCAL_FREQ, query_schedule);
        AlarmManager.queryScheduleAlarm(this);
    }

    private void showQueryScheduleChoice() {
        CheckScheduleView checkScheduleView = new CheckScheduleView(this);
        checkScheduleView.setItemChecked(queryScheduleIndex);
        checkScheduleView.setOnItemClickListener(this);
        DialogUtil.showCustomDialog(this, getString(R.string.setting_autocheck_title), checkScheduleView);
    }

    @Override
    public void onItemClick(int index) {
        closeDialog();
        updateQuerySchedule(index);
    }

    @Override
    public void widgetClick(View v) {
        switch (v.getId()) {
            case R.id.check_cycle:
                showQueryScheduleChoice();
                break;
            case R.id.update_wifi_only:
                PreferencesUtils.putBoolean(this, Setting.DOWNLOAD_ONLY_WIFI, updateWifiOnly.isChecked());
                break;
            case R.id.wifi_auto_download:
                PreferencesUtils.putBoolean(this, Setting.DOWNLOAD_WIFI_AUTO, wifiAutoDownload.isChecked());
            case R.id.night_auto_download:
                PreferencesUtils.putBoolean(this, Setting.SLIENT_DOWNLOAD, slient_night_view.isChecked());
                if(slient_night_view.isChecked()){
                    LogUtil.d("Open night_auto_download");
                    //DownVersion.getInstance().onNightDownload(this);
					
					DownVersion.getInstance().slientDownload(MyApplication.getAppContext());
                }else {
                    LogUtil.d("cancel night_auto_download");
                    AlarmManager.cancel(MyApplication.getAppContext());
                }

                break;
            case R.id.about:
                ComponentName componentName = new ComponentName(Const.FOTA_BOOT_PACKAGE_NAME, Const.FOTA_BOOT_ACTIVITY_NAME);
                Intent intent = new Intent();
                intent.setComponent(componentName);
                intent.putExtra("param", (Serializable) RequestParam.getReportEuParam(this));
                startActivity(intent);
                break;
        }
    }

}

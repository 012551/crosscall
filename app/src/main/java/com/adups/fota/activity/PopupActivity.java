package com.adups.fota.activity;

import android.content.BroadcastReceiver;
import android.content.Context;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.adups.fota.GoogleOtaClient;
import com.adups.fota.MyApplication;
import com.adups.fota.R;
import com.adups.fota.TimePickerWindow;
import com.adups.fota.bean.VersionBean;
import com.adups.fota.callback.ClickCallback;
import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.config.TaskID;
import com.adups.fota.manager.ActivityManager;
import com.adups.fota.manager.AlarmManager;
import com.adups.fota.manager.SpManager;
import com.adups.fota.query.QueryInfo;
import com.adups.fota.utils.DialogUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PreferencesUtils;
import com.adups.fota.view.InstallCheckView;
import com.adups.fota.view.TitleView;

import static android.security.KeyStore.getApplicationContext;

public class PopupActivity extends BaseActivity {
    private static final String SYSTEM_DIALOG_REASON_KEY = "reason";
    private static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
    private ExitRecevier receiver;
    private Button mNight_update;

    @Override
    protected void setTitleView(TitleView titleView) {
        titleView.setVisibility(View.GONE);
    }

    @Override
    protected void initWidget() {
        setContentView(R.layout.activity_fota_pop_window);
        init(getIntent());
        SpManager.addUpgradeLaterTimes();
        receiver = new ExitRecevier();
        registerReceiver(receiver, new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        closeDialog();
        init(intent);
        SpManager.addUpgradeLaterTimes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActivityManager.getManager().finishMainActivity();
    }

    @Override
    protected void widgetClick(View v) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    private void init(final Intent intent) {
        final int status = intent.getIntExtra(TaskID.STATUS, -1);
        View view = getLayoutInflater().inflate(R.layout.dialog_ota_client, null);
        LogUtil.d("status : " + status);
        switch (status) {
            case Status.STATE_NEW_VERSION_READY:
                DialogUtil.showDialog(this, null, QueryInfo.getInstance().getReleaseNotes(this, false),
                        getString(R.string.btn_download_later), new ClickCallback() {
                            @Override
                            public void onClick() {
                                finish();
                                if (status == Status.STATE_NEW_VERSION_READY) {
                                    intent.putExtra(TaskID.STATUS, TaskID.TASK_DOWNLOAD_DELAY);
                                    intent.putExtra(Setting.INTENT_PARAM_FLAG, Setting.INTENT_PARAM_DOWNLOAD);
                                } else if (status == Status.STATE_DL_PKG_COMPLETE) {
                                    intent.putExtra(TaskID.STATUS, TaskID.TASK_INSTALL_LATER);
                                    intent.putExtra(Setting.INTENT_PARAM_FLAG, Setting.INTENT_PARAM_INSTALL);
                                }
//                            PreferencesUtils.putInt(MyApplication.getAppContext(), "query_count", 0);
                                intent.setClass(MyApplication.getAppContext(), TimePickerWindow.class);
                                intent.setAction("com.adups.fota");
                                startActivity(intent);
                            }

                        }, getString(R.string.btn_download), new ClickCallback() {
                            @Override
                            public void onClick() {
                                finish();
                                if (status == Status.STATE_NEW_VERSION_READY) {
                                    intent.putExtra(TaskID.STATUS, TaskID.TASK_DOWNLOAD_NOW);
                                    intent.putExtra(Setting.INTENT_PARAM_FLAG, Setting.INTENT_PARAM_DOWNLOAD);
                                } else if (status == Status.STATE_DL_PKG_COMPLETE) {
                                    intent.putExtra(TaskID.STATUS, Status.STATE_DL_PKG_COMPLETE);
                                    intent.putExtra(Setting.INTENT_PARAM_FLAG, Setting.INTENT_PARAM_INSTALL);
                                }
//                            PreferencesUtils.putInt(MyApplication.getAppContext(), "query_count", 0);
                                intent.setClass(MyApplication.getAppContext(), GoogleOtaClient.class);
                                intent.setAction("com.adups.fota");
                                startActivity(intent);
                            }
                        }, getString(R.string.btn_download_install_later), new ClickCallback() {
                            @Override
                            public void onClick() {

//                            PreferencesUtils.putInt(MyApplication.getAppContext(), "download_install_later", 1);
                                intent.putExtra(TaskID.STATUS, TaskID.TASK_DOWNLOAD_INSTALL_DELAY);
//                            PreferencesUtils.putInt(MyApplication.getAppContext(), "query_count", 0);
                                intent.setClass(MyApplication.getAppContext(), TimePickerWindow.class);
                                intent.setAction("com.adups.fota");
                                startActivity(intent);
                                finish();
                            }
                        },
                        view, null);

                break;
            case Status.STATE_DL_PKG_COMPLETE:
                if (SpManager.getUpgradeCheckStatus()) {
                    DialogUtil.showDialog(this, getString(R.string.new_to_upgrade), null,
                            getString(R.string.update_now), new ClickCallback() {
                                @Override
                                public void onClick() {
                                    startActivity(new Intent("com.adups.fota", null, PopupActivity.this, GoogleOtaClient.class));
                                    finish();
                                }
                            }, getString(R.string.update_later), new ClickCallback() {
                                @Override
                                public void onClick() {
                                    if (SpManager.getUpgradeCheckStatus()) {
                                        AlarmManager.dialogInstallDelay(PopupActivity.this);
                                    }
                                    finish();
                                }
                            });
                } else {
                    SpManager.setUpgradeCheckStatus(true);
                    DialogUtil.showCustomDialog(this, getString(R.string.new_to_upgrade),
                            getString(R.string.update_now), new ClickCallback() {
                                @Override
                                public void onClick() {
                                    startActivity(new Intent("com.adups.fota", null, PopupActivity.this, GoogleOtaClient.class));
                                    finish();
                                }
                            }, getString(R.string.update_later), new ClickCallback() {
                                @Override
                                public void onClick() {
                                    if (SpManager.getUpgradeCheckStatus()) {
                                        AlarmManager.dialogInstallDelay(PopupActivity.this);
                                    }
                                    finish();
                                }
                            }, null, new InstallCheckView(this));
                }
                break;
            default:
                finish();
                break;
        }
    }

    class ExitRecevier extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {

                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                LogUtil.d("reason: " + reason);
                if (SYSTEM_DIALOG_REASON_HOME_KEY.equals(reason)) {
                    // 短按Home键
                    PreferencesUtils.putInt(context, "query_count", 0);
                    AlarmManager.pop_cancel(MyApplication.getAppContext(), 1000 * 60 * 10);
                    finish();
                    LogUtil.d("按了home键");

                }

            }
        }
    }

}

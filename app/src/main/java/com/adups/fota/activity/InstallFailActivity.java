package com.adups.fota.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.annotation.NonNull;

import com.adups.fota.GoogleOtaClient;
import com.adups.fota.MaterialDialog;
import com.adups.fota.R;
import com.adups.fota.activity.BaseActivity;
import com.adups.fota.callback.ClickCallback;
import com.adups.fota.utils.DialogUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.view.TitleView;




public class InstallFailActivity extends BaseActivity {
    String reason;

    @Override
    protected void initWidget() {

    }

    @Override
    protected void widgetClick(View v) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_result);

        reason = getIntent().getStringExtra("reason");
        LogUtil.d("[onCreate] reason = " + reason);

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                showResultDialog(reason);
            }
        });
    }


    private void showResultDialog(String reason) {

        LogUtil.d("[showResultDialog] ============");
//        DialogUtil.showCustomDialog(this, getString(R.string.battery_remove_title), reason,getString(R.string.ota_button_text_know), new ClickCallback() {
//            @Override
//            public void onClick() {
//                finish();
//            }
//        },false);
//            new MaterialDialog.Builder(this)
//                    .title(R.string.battery_remove_title)
//                    .content(reason)
//                    .positiveText(R.string.ota_button_text_know)
//                    .onPositive(new MaterialDialog.SingleButtonCallback() {
//                        @Override
//                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
//                            finish();
//                        }
//                    })
//                    .cancelable(false)
//                    .show();


    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void setTitleView(TitleView titleView) {

    }


}

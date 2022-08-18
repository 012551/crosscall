package com.adups.fota.activity;

import android.os.Handler;
import android.view.View;

import com.adups.fota.R;
import com.adups.fota.callback.ClickCallback;
import com.adups.fota.utils.DialogUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.view.TitleView;

public class InstallResultActivity extends BaseActivity {

    @Override
    protected void setTitleView(TitleView titleView) {
        titleView.setVisibility(View.GONE);
    }

    @Override
    protected void initWidget() {
        setContentView(R.layout.activity_update_result);
        String version = getIntent().getStringExtra("version");
        LogUtil.d("[onCreate] version name = " + version);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                showResultDialog();
            }
        });
    }

    @Override
    protected void widgetClick(View v) {

    }

    private void showResultDialog() {
        LogUtil.d("[showResultDialog] ============");
        DialogUtil.showPositiveDialog(this,
                getString(R.string.updateSuccessTitle), getString(R.string.updateSuccess, ""),
                new ClickCallback() {
                    @Override
                    public void onClick() {
                        finish();
                    }
                }, null);
    }

}

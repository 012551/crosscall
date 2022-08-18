package com.adups.fota.activity;

import android.view.View;

import com.adups.fota.R;
import com.adups.fota.manager.NoticeManager;
import com.adups.fota.view.TitleView;

public class ShortCutActivity extends BaseActivity {

    @Override
    protected void setTitleView(TitleView titleView) {
        titleView.setVisibility(View.GONE);
    }

    @Override
    protected void initWidget() {
        setContentView(R.layout.activity_short_cut);
        NoticeManager.delAndAddShortcut(this);
        finish();
    }

    @Override
    protected void widgetClick(View v) {

    }

}

package com.adups.fota.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.adups.fota.R;
import com.adups.fota.manager.SpManager;
import com.adups.fota.utils.DeviceInfoUtil;

public class InstallCheckView extends LinearLayout implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private TextView content;

    public InstallCheckView(Context context) {
        super(context);
        init();
    }

    public InstallCheckView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public InstallCheckView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.install_check_view_layout, this);
        CheckBox checkBox = findViewById(R.id.checkBox);
        TextView title = findViewById(R.id.title);
        content = findViewById(R.id.content);
        checkBox.setOnCheckedChangeListener(this);
        checkBox.setChecked(true);
        title.setOnClickListener(this);
        if (DeviceInfoUtil.getInstance().isNoTouch()) {
            checkBox.setFocusable(true);
            title.setFocusable(true);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        SpManager.setUpgradeCheckStatus(b);
    }

    @Override
    public void onClick(View v) {
        content.setVisibility(VISIBLE);
    }

}

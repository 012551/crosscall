package com.adups.fota.view;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.adups.fota.R;
import com.adups.fota.utils.DeviceInfoUtil;

public class NetworkWarnView extends LinearLayout implements View.OnClickListener {

    private TextView title;

    public NetworkWarnView(Context context) {
        super(context);
        init();
    }

    public NetworkWarnView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NetworkWarnView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.dialog_no_network, this);
        title = findViewById(R.id.dialog_prompt_content);
        TextView content = findViewById(R.id.wifi_enter);
        content.setOnClickListener(this);
        if (DeviceInfoUtil.getInstance().isNoTouch()) {
            content.setFocusable(true);
            content.requestFocus();
        }
    }

    public void setTitle(String text) {
        title.setText(text);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        getContext().startActivity(intent);
    }
}

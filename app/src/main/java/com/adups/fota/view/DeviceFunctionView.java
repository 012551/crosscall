package com.adups.fota.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.adups.fota.R;
import com.adups.fota.callback.FunctionClickCallback;

public class DeviceFunctionView extends LinearLayout implements View.OnClickListener {

    private TextView info;

    private FunctionClickCallback callback;

    public DeviceFunctionView(Context context) {
        super(context);
        init();
    }

    public DeviceFunctionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DeviceFunctionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.dialog_debug_info, this);
        info = findViewById(R.id.info);
        findViewById(R.id.button_export_data).setOnClickListener(this);
        findViewById(R.id.button_start_debug).setOnClickListener(this);
        findViewById(R.id.button_stop_debug).setOnClickListener(this);
    }

    public void setInfo(String text) {
        info.setText(text);
    }

    public void setOnFunctionClickListener(FunctionClickCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onClick(View v) {
        int index = 0;
        switch (v.getId()) {
            case R.id.button_export_data:
                index = 0;
                break;
            case R.id.button_start_debug:
                index = 1;
                break;
            case R.id.button_stop_debug:
                index = 2;
                break;
        }
        if (callback != null) callback.onItemClick(index);
    }

}

package com.adups.fota.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import com.adups.fota.R;
import com.adups.fota.callback.FunctionClickCallback;
import com.adups.fota.utils.DeviceInfoUtil;

import java.util.ArrayList;
import java.util.List;

public class CheckScheduleView extends LinearLayout implements View.OnClickListener {

    private List<RadioButton> radioButtons;

    private FunctionClickCallback callback;

    public CheckScheduleView(Context context) {
        super(context);
        init();
    }

    public CheckScheduleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CheckScheduleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.activity_setting_dialog_schedule_choice, this);
        RadioButton radioButton1 = findViewById(R.id.RadioButton1);
        RadioButton radioButton2 = findViewById(R.id.RadioButton2);
        RadioButton radioButton3 = findViewById(R.id.RadioButton3);
        radioButtons = new ArrayList<>();
        radioButtons.add(radioButton1);
        radioButtons.add(radioButton2);
        radioButtons.add(radioButton3);
        for (RadioButton button : radioButtons) {
            button.setOnClickListener(this);
            if (DeviceInfoUtil.getInstance().isNoTouch())
                button.setFocusable(true);
        }
    }

    public void setItemChecked(int index) {
        if (radioButtons == null) return;
        if (!radioButtons.isEmpty() && index <= radioButtons.size()) {
            RadioButton button = radioButtons.get(index);
            button.setChecked(true);
            if (DeviceInfoUtil.getInstance().isNoTouch())
                button.requestFocus();
        }
    }

    public void setOnItemClickListener(FunctionClickCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onClick(View v) {
        int index = 0;
        switch (v.getId()) {
            case R.id.RadioButton1:
                index = 0;
                break;
            case R.id.RadioButton2:
                index = 1;
                break;
            case R.id.RadioButton3:
                index = 2;
                break;
        }
        if (callback != null) callback.onItemClick(index);
    }

}

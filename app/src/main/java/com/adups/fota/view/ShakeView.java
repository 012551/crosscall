package com.adups.fota.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.adups.fota.R;
import com.adups.fota.utils.ShakeUtil;

public class ShakeView extends LinearLayout {

    private TextView textView;

    public ShakeView(Context context) {
        super(context);
        init();
    }

    public ShakeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ShakeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.shake_view_layout, this);
        textView = findViewById(R.id.content);
        if (!ShakeUtil.getInstance().hasAccelerometer(getContext())) setVisibility(GONE);
    }

    public void setContent(int resId) {
        if (!ShakeUtil.getInstance().hasAccelerometer(getContext())) return;
        setVisibility(VISIBLE);
        textView.setText(resId);
    }

}

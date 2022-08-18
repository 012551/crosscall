package com.adups.fota.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.adups.fota.R;
import com.adups.fota.utils.DeviceInfoUtil;

public class TitleContentView extends LinearLayout implements View.OnClickListener {

    private TextView title, content, tip;
    private CheckBox checkBox;
    private ImageView arrow;

    private OnClickListener listener;

    public TitleContentView(Context context) {
        super(context);
        init();
    }

    public TitleContentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        setViewsFromAttrs(attrs);
    }

    public TitleContentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        setViewsFromAttrs(attrs);
    }

    private void init() {
        inflate(getContext(), R.layout.title_content_view_layout, this);
        title = findViewById(R.id.title);
        content = findViewById(R.id.content);
        tip = findViewById(R.id.tip);
        checkBox = findViewById(R.id.checkBox);
        arrow = findViewById(R.id.arrow);
        super.setOnClickListener(this);
        if (DeviceInfoUtil.getInstance().isNoTouch())
            setFocusable(true);
    }

    private void setViewsFromAttrs(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.view);
        String title = typedArray.getString(R.styleable.view_title);
        if (!TextUtils.isEmpty(title)) setTitle(title);
        String content = typedArray.getString(R.styleable.view_content);
        if (!TextUtils.isEmpty(content))
            setContent(content);
        else
            setContentVisible(GONE);
        if (typedArray.getBoolean(R.styleable.view_showBox, false))
            showBox();
        typedArray.recycle();
    }

    public void setTitle(String text) {
        title.setText(text);
    }

    public void setContent(String text) {
        content.setText(text);
    }

    public void setContentVisible(int visible) {
        content.setVisibility(visible);
    }

    public void setTip(String text) {
        tip.setText(text);
    }

    public void setTip(int resID) {
        setTip(getContext().getString(resID));
    }

    public void showBox() {
        checkBox.setVisibility(VISIBLE);
        arrow.setVisibility(GONE);
    }

    public boolean isChecked() {
        return checkBox.isChecked();
    }

    public void setChecked(boolean value) {
        checkBox.setChecked(value);
    }

    public void setOnClickListener(View.OnClickListener l) {
        listener = l;
    }

    @Override
    public void onClick(View v) {
        setChecked(!isChecked());
        if (listener != null)
            listener.onClick(v);
    }

}

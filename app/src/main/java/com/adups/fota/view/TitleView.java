package com.adups.fota.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.adups.fota.R;
import com.adups.fota.config.Const;
import com.adups.fota.utils.DeviceInfoUtil;

public class TitleView extends LinearLayout {

    private TextView content;
    private ImageView image, setting;

    public TitleView(Context context) {
        super(context);
        init();
    }

    public TitleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        setViewsFromAttrs(attrs);
    }

    public TitleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        setViewsFromAttrs(attrs);
    }

    private void init() {
        inflate(getContext(), R.layout.title_view_layout, this);
        image = findViewById(R.id.image);
        setting = findViewById(R.id.setting);
        content = findViewById(R.id.content);
        LinearLayout contentLayout = findViewById(R.id.contentLayout);
        if (DeviceInfoUtil.getInstance().isNoTouch()) {
            setting.setFocusable(true);
            contentLayout.setFocusable(true);
        }
    }

    private void setViewsFromAttrs(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.view);
        String content = typedArray.getString(R.styleable.view_content);
        if (!TextUtils.isEmpty(content)) setContent(content);
        setImage(typedArray.getResourceId(R.styleable.view_icon, R.mipmap.back));
        setSettingVisible(typedArray.getBoolean(R.styleable.view_showSetting, false));
        typedArray.recycle();
    }

    public void setImage(int resId) {
        if (resId != 0) image.setBackgroundResource(resId);
    }

    public void setContent(String text) {
        content.setText(text);
    }

    public void setSettingVisible(boolean visible) {
        if (visible)
            setting.setVisibility(VISIBLE);
        else
            setting.setVisibility(GONE);
    }

    public void setSettingClickListener(OnClickListener listener) {
        setting.setOnClickListener(listener);
        setting.setTag(Const.TAG_BTN_POP);
    }

    public ImageView getSetting() {
        return setting;
    }

}

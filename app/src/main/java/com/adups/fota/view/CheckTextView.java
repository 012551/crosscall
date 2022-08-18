package com.adups.fota.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.adups.fota.MyApplication;
import com.adups.fota.R;
import com.adups.fota.callback.SlientClickCallback;
import com.adups.fota.config.Setting;
import com.adups.fota.download.DownVersion;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PreferencesUtils;
import com.adups.fota.utils.ToastUtil;

public class CheckTextView extends LinearLayout implements View.OnClickListener {

    private CheckBox checkBox;
    private TextView textView;

    private SlientClickCallback SlientClickCallback;

    private boolean isCheck;
    boolean isSlientDownload;

    public CheckTextView(Context context) {
        super(context);
        init();
    }

    public CheckTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        setViewsFromAttrs(attrs);
    }

    public CheckTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        setViewsFromAttrs(attrs);
    }

    private void init() {
        inflate(getContext(), R.layout.check_text_view_layout, this);
        checkBox = findViewById(R.id.checkBox);
        textView = findViewById(R.id.textView);
        textView.setSelected(true);
        isSlientDownload = PreferencesUtils.getBoolean(MyApplication.getAppContext(), Setting.SLIENT_DOWNLOAD, false);
        isCheck = isSlientDownload;
        checkBox.setChecked(isCheck);
        setOnClickListener(this);
    }

    private void setViewsFromAttrs(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.view);
        String content = typedArray.getString(R.styleable.view_substance);
        if (!TextUtils.isEmpty(content)) setContent(content);
        typedArray.recycle();
    }

    public void setOnCheckChangeListener(SlientClickCallback clickCallback) {
        this.SlientClickCallback = clickCallback;
    }

    public void setContent(String text) {
        if (textView != null)
            textView.setText(text);
    }

    @Override
    public void onClick(View v) {
        isCheck = !isCheck;
        checkBox.setChecked(isCheck);
        if(checkBox.isChecked()){
            LogUtil.d("Silent download enabled");
            ToastUtil.showToast(R.string.night_update_toast);
            PreferencesUtils.putBoolean(MyApplication.getAppContext(), Setting.SLIENT_DOWNLOAD, DeviceInfoUtil.getInstance().isSlientDownload());
            DownVersion.getInstance().slientDownload(MyApplication.getAppContext());
        }
        if(!checkBox.isChecked()){
            LogUtil.d("Silent download disabled");
            ToastUtil.showToast(R.string.night_update_disable_toast);
            PreferencesUtils.putBoolean(MyApplication.getAppContext(), Setting.SLIENT_DOWNLOAD, checkBox.isChecked());
        }
        if (SlientClickCallback != null) SlientClickCallback.onViewClicked(getId(), isCheck);
    }

}

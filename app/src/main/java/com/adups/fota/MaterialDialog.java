package com.adups.fota;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.adups.fota.callback.ClickCallback;
import com.adups.fota.callback.SlientClickCallback;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.view.CheckTextView;

public class MaterialDialog extends Dialog {

    private LinearLayout content, bottom,checkTextView;
    private TextView title, message, positiveButtonText, negativeButtonText,neutralButtonText;

    public MaterialDialog(Context context, int themeResId) {
        super(context, themeResId);
        initContentView();
        initDialogWidth();
        setCancelable(false);
    }

    private void initContentView() {
        super.setContentView(R.layout.dialog);
        title = findViewById(R.id.title);
        checkTextView = findViewById(R.id.checkView);
        checkTextView.setVisibility(View.GONE);
        positiveButtonText = findViewById(R.id.positiveButton);
        negativeButtonText = findViewById(R.id.negativeButton);
        neutralButtonText = findViewById(R.id.neutralButton);
        message = findViewById(R.id.message);
        bottom = findViewById(R.id.bottom);
        content = findViewById(R.id.content);
        title.setVisibility(View.GONE);
        bottom.setVisibility(View.GONE);
        positiveButtonText.setVisibility(View.GONE);
        positiveButtonText.setBackgroundResource(R.drawable.button_selector);
        negativeButtonText.setVisibility(View.GONE);
        negativeButtonText.setBackgroundResource(R.drawable.button_selector);
        neutralButtonText.setVisibility(View.GONE);
        neutralButtonText.setBackgroundResource(R.drawable.button_selector);

        message.setVisibility(View.GONE);
        if (DeviceInfoUtil.getInstance().isNoTouch()) {
            positiveButtonText.setFocusable(true);
            positiveButtonText.requestFocus();
            negativeButtonText.requestFocus();
        }
    }

    private void initDialogWidth() {
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            Display display = windowManager.getDefaultDisplay();
            if (display != null) {
                Window window = getWindow();
                if (window != null) {
                    WindowManager.LayoutParams params = window.getAttributes();
                    if (params != null) {
                        params.width = Double.valueOf(display.getWidth() * 0.85).intValue();
                        window.setAttributes(params);
                    }
                }
            }
        }
    }

    public void setContentView(@NonNull View view) {
        message.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);
        content.removeAllViews();
        content.addView(view, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void setTitle(String text) {
        if (TextUtils.isEmpty(text)) {
            title.setVisibility(View.GONE);
            return;
        }
        title.setText(text);
        title.setVisibility(View.VISIBLE);
    }

    //zhangzhou
    public void setCheckSlient(){
        checkTextView.setVisibility(View.VISIBLE);
        CheckTextView agreementCheck = findViewById(R.id.agreementCheck);
        if (agreementCheck != null) {
            agreementCheck.setOnCheckChangeListener(new SlientClickCallback() {
                @Override
                public void onViewClicked(int id, boolean isChecked) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            dismiss();
                        }
                    },1200);
                }
            });
        }
        agreementCheck.setVisibility(View.VISIBLE);
    }

    public void setTitleGravity(int gravity) {
        title.setGravity(gravity);
    }

    public void setBottomGravity(int gravity) {
        bottom.setGravity(gravity);
    }

    public void setMessage(String text) {
        if (TextUtils.isEmpty(text)) {
            message.setVisibility(View.GONE);
            return;
        }
        content.setVisibility(View.GONE);
        message.setText(text);
        message.setVisibility(View.VISIBLE);
    }

    public void setPositiveButton(String text, final ClickCallback callback) {
        if (TextUtils.isEmpty(text)) {
            positiveButtonText.setVisibility(View.GONE);
            return;
        }
        positiveButtonText.setText(text);
        positiveButtonText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null)
                    callback.onClick();
                dismiss();
            }
        });
        positiveButtonText.setVisibility(View.VISIBLE);
        bottom.setVisibility(View.VISIBLE);
    }

    public void setNegativeButton(String text, final ClickCallback callback) {
        if (TextUtils.isEmpty(text)) {
            negativeButtonText.setVisibility(View.GONE);
            return;
        }
        negativeButtonText.setText(text);
        negativeButtonText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null)
                    callback.onClick();
                dismiss();
            }
        });
        negativeButtonText.setVisibility(View.VISIBLE);
        bottom.setVisibility(View.VISIBLE);
    }
    public void setNeutralButton(String text, final ClickCallback callback) {
        if (TextUtils.isEmpty(text)) {
            neutralButtonText.setVisibility(View.GONE);
            return;
        }
        neutralButtonText.setText(text);
        neutralButtonText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null)
                    callback.onClick();
                dismiss();
            }
        });
        neutralButtonText.setVisibility(View.VISIBLE);
        bottom.setVisibility(View.VISIBLE);
    }

    @Override
    public void show() {
        super.show();
        if (positiveButtonText.getVisibility() == View.GONE
                && negativeButtonText.getVisibility() == View.GONE
                && neutralButtonText.getVisibility() == View.GONE)
            bottom.setVisibility(View.GONE);
    }

}
package com.adups.fota.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.adups.fota.R;
import com.adups.fota.config.Const;
import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.query.QueryInfo;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.FileUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PreferencesUtils;

import java.util.Locale;

/**
 * Created by brave on 2016/1/10.
 */
public class FooterLayout extends LinearLayout {

    private TextView bt_check, bt_left, bt_right,bt_download_later,bt_download_install; // 点击检测按钮
    private LinearLayout hl_bt_layout,check_la; // 下载控件区域

    public FooterLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOrientation(HORIZONTAL);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        LayoutInflater.from(getContext()).inflate(R.layout.main_footer, this);
        bt_check = findViewById(R.id.bt_check);
        bt_check.setTag(Const.TAG_CHECK);
        bt_download_later = findViewById(R.id.bt_download_later);
        bt_download_install = findViewById(R.id.bt_download_install);
        bt_download_later.setTag(Const.TAG_DOWNLOAD_LATER);
        bt_download_install.setTag(Const.TAG_DOWNLOAD_INSTALL_LATER);
        check_la = findViewById(R.id.check_la);
        hl_bt_layout = findViewById(R.id.hl_bt_layout);
        hl_bt_layout.setVisibility(GONE);
        bt_left = findViewById(R.id.button_left);
        bt_left.setTag(Const.TAG_DOWNLOAD_PAUSE);
        bt_right = findViewById(R.id.button_right);
        bt_right.setTag(Const.TAG_DOWNLOAD_CANCEL);
        if (DeviceInfoUtil.getInstance().isNoTouch()) {
            bt_left.setFocusable(true);
            bt_left.requestFocus();
            bt_right.setFocusable(true);
        }
        if (View.LAYOUT_DIRECTION_RTL == TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())) {
            bt_left.setBackgroundResource(R.drawable.button_right_selector);
            bt_right.setBackgroundResource(R.drawable.button_left_selector);
        }
    }

    public void setOnClickListener(View.OnClickListener clickListener) {
        bt_check.setOnClickListener(clickListener);
        bt_left.setOnClickListener(clickListener);
        bt_right.setOnClickListener(clickListener);
        bt_download_later.setOnClickListener(clickListener);
        bt_download_install.setOnClickListener(clickListener);
    }

    public void setNoTouchState() {
        if (DeviceInfoUtil.getInstance().isNoTouch()) {
            if (bt_check.getVisibility() == VISIBLE) {
                bt_check.setFocusable(true);
                bt_check.requestFocus();
            } else {
                bt_left.setFocusable(true);
                bt_left.requestFocus();
                bt_right.setFocusable(true);
            }
        }
    }

    //设置按钮状态
    public void setState(boolean isUnCheckState) {
        if (isUnCheckState) { //是否处于未检测状态
            bt_check.setVisibility(VISIBLE);
            hl_bt_layout.setVisibility(GONE);
            if (DeviceInfoUtil.getInstance().isNoTouch()) {
                bt_check.setFocusable(true);
                bt_check.requestFocus();
            }
        } else {
            bt_check.setVisibility(GONE);
            hl_bt_layout.setVisibility(VISIBLE);
            if (DeviceInfoUtil.getInstance().isNoTouch()) {
                bt_left.setFocusable(true);
                bt_left.requestFocus();
                bt_right.setFocusable(true);
            }
        }
    }


    public void init(int status) {
        LogUtil.d("enter init,  status = " +status);
        switch (status) {
            case Status.STATE_QUERY_NEW_VERSION:
                check_la.setVisibility(VISIBLE);
                bt_check.setTag(Const.TAG_CHECK);
                bt_check.setText(R.string.check_now);
                bt_download_later.setVisibility(GONE);
                bt_download_install.setVisibility(GONE);
                setState(true);
                break;
            case Status.STATE_NEW_VERSION_READY:
                check_la.setVisibility(VISIBLE);
                bt_download_later.setVisibility(VISIBLE);
                bt_download_install.setVisibility(VISIBLE);
                bt_download_later.setTag(Const.TAG_DOWNLOAD_LATER);
                bt_download_install.setTag(Const.TAG_DOWNLOAD_INSTALL_LATER);
                updateButton(status);
                setState(true);
                break;
            case Status.STATE_DOWNLOADING:
                bt_left.setTag(Const.TAG_DOWNLOAD_PAUSE);
                bt_right.setTag(Const.TAG_DOWNLOAD_CANCEL);
                check_la.setVisibility(GONE);
                bt_download_later.setVisibility(GONE);
                bt_download_install.setVisibility(GONE);
                bt_left.setText(R.string.btn_pause);
                bt_right.setText(android.R.string.cancel);
                setState(false);
                break;
            case Status.STATE_PAUSE_DOWNLOAD:
                check_la.setVisibility(GONE);
                bt_download_later.setVisibility(GONE);
                bt_download_install.setVisibility(GONE);
                bt_left.setVisibility(VISIBLE);
                bt_right.setVisibility(VISIBLE);
                bt_left.setTag(Const.TAG_DOWNLOAD_RESUME);
                bt_right.setTag(Const.TAG_DOWNLOAD_CANCEL);
                bt_left.setText(R.string.btn_resume);
                bt_right.setText(android.R.string.cancel);
                setState(false);
                break;
            case Status.STATE_DL_PKG_COMPLETE:
                if (QueryInfo.getInstance().getPolicyValue(QueryInfo.INSTALL_NOTICE_TYPE, Integer.class) == 0
                        && PreferencesUtils.getInt(getContext(), Setting.INSTALL_LATER_COUNT, 0) >= 5) {
                    setState(true);
                    bt_check.setTag(Const.TAG_UPDATE_NOW);
                    bt_check.setText(R.string.update_now);
                    break;
                }
                setState(false);
                check_la.setVisibility(GONE);
                bt_download_later.setVisibility(GONE);
                bt_download_install.setVisibility(GONE);
                bt_left.setTag(Const.TAG_UPDATE_NOW);
                bt_right.setTag(Const.TAG_UPDATE_LATER);
                bt_left.setText(R.string.update_now);
                bt_right.setText(R.string.update_later);
                break;
            case Status.STATE_REBOOT:
                setState(true);
                check_la.setVisibility(VISIBLE);
                bt_download_later.setVisibility(GONE);
                bt_download_install.setVisibility(GONE);
                bt_check.setTag(Const.TAG_REBOOT_NOW);
                bt_check.setText(R.string.ab_reboot_now);
                break;
            case Status.STATE_AB_UPDATING:
                check_la.setVisibility(GONE);
                bt_download_later.setVisibility(GONE);
                bt_download_install.setVisibility(GONE);
                bt_check.setVisibility(INVISIBLE);
                hl_bt_layout.setVisibility(GONE);
                break;
        }
    }

    private void updateButton(int state) {
        if (state == Status.STATE_NEW_VERSION_READY) {
            bt_check.setTag(Const.TAG_DOWNLOAD);
            try {
                long size = QueryInfo.getInstance().getVersionInfo().getFileSize();
                bt_check.setText(getResources().getString(R.string.btn_download));
            } catch (Exception e) {
                bt_check.setText(R.string.btn_download);
            }
            setState(true);
        }
    }

}

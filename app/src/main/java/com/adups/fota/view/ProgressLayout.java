package com.adups.fota.view;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.adups.fota.R;

/**
 * Created by brave on 2016/1/10.
 */
public class ProgressLayout extends LinearLayout {
    private ImageView def_img;
    private RelativeLayout rl_download_pro;
    private ProgressRingView mProgress;
    private TextView mProTips, mVersionTip;

    public ProgressLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOrientation(HORIZONTAL);
        LayoutInflater.from(getContext()).inflate(R.layout.main_pro_ring, this);
        def_img = findViewById(R.id.def_img);
        def_img.setTag(R.id.def_img);
        rl_download_pro = findViewById(R.id.rl_download_pro);
        mProgress = findViewById(R.id.download_pro_ring);
        mProTips = findViewById(R.id.txt_progress);
        mVersionTip = findViewById(R.id.def_version_tip);
        Typeface typeFace = Typeface.createFromAsset(context.getAssets(), "fonts/Bariol_Regular.ttf");
        mProTips.setTypeface(typeFace);
    }

    //设置点击默认事件
    public void setOnClickListener(View.OnClickListener clickListener) {
        def_img.setOnClickListener(clickListener);
    }

    //设置下载进度
    public void setDownLoadProgress(int progress) {
        def_img.setVisibility(GONE);
        mVersionTip.setVisibility(GONE);
        rl_download_pro.setVisibility(VISIBLE);
        mProgress.setProgress(progress);
        mProTips.setText("" + progress);
    }

    public void reset() {
        def_img.setImageResource(R.mipmap.icon_update);
        def_img.setVisibility(VISIBLE);
        mVersionTip.setText("");
        mVersionTip.setVisibility(VISIBLE);
        rl_download_pro.setVisibility(GONE);
        mProgress.setProgress(0);
        mProTips.setText("");
    }

    public void setVersionTip(String content) {
        def_img.setImageDrawable(new ColorDrawable(0x00000000));
        mVersionTip.setText(content);
    }

    public int getProgress() {
        return mProgress.getProgress();
    }

    public void setProgress(int progress) {
        mProgress.setProgress(progress);
    }

}

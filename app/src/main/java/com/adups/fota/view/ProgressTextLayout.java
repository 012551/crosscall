package com.adups.fota.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.adups.fota.R;
import com.adups.fota.query.QueryInfo;

import java.text.DecimalFormat;

public class ProgressTextLayout extends LinearLayout {

    private TextView progressText;
    private ProgressBar progressBar;

    public ProgressTextLayout(Context context) {
        super(context);
        init();
    }

    public ProgressTextLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProgressTextLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.progress_text_layout, this);
        progressText = findViewById(R.id.progressText);
        progressBar = findViewById(R.id.progressBar);
    }

    public void setProgress(int progress) {
        setVisibility(VISIBLE);
        DecimalFormat format = new DecimalFormat("0.00");
        long size = QueryInfo.getInstance().getVersionInfo().getFileSize();
        long totalSize = size / 1024 / 1024;
        long currSize = progress * totalSize / 100;
        String text = format.format(currSize) + "MB/" + format.format(totalSize) + "MB";
        progressText.setText(text);
        progressBar.setProgress(progress);
    }

}

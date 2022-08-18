package com.adups.fota.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Administrator on 2016/1/8.
 */
public class ProgressRingView extends View {
    private RectF rectF;
    private boolean once = true;
    private Paint paint;
    private int progress = 0;

    public ProgressRingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (once) {
            rectF = new RectF(0, 0, getWidth(), getHeight());
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.WHITE);
            paint.setAntiAlias(true);                       //设置画笔为无锯齿
            paint.setStyle(Paint.Style.FILL);
            once = false;
        }
        canvas.drawArc(rectF, 272, progress * 360 / 100, true, paint);
        canvas.save();
        canvas.restore();
    }

    public int getProgress() {
        return progress;
    }

    /**
     * @param progress 0~100
     */
    public void setProgress(int progress) {
        if (progress >= 0 && progress <= 100) {
            this.progress = progress;
            invalidate();
        }
    }

}

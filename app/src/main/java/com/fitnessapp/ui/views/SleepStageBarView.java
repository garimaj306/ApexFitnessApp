package com.fitnessapp.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.fitnessapp.R;

/**
 * Custom Multi-Segmented Sleep Stage Horizontal Bar View.
 * Visually breaks down Deep, REM, and Light sleep stages proportionally.
 */
public class SleepStageBarView extends View {

    private Paint deepPaint;
    private Paint remPaint;
    private Paint lightPaint;

    private int deepMinutes = 110;
    private int remMinutes = 95;
    private int lightMinutes = 245;

    private RectF bounds;
    private float cornerRadius = 16f;

    public SleepStageBarView(Context context) {
        super(context);
        init(context);
    }

    public SleepStageBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SleepStageBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        deepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        deepPaint.setColor(ContextCompat.getColor(context, R.color.sleep_deep));

        remPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        remPaint.setColor(ContextCompat.getColor(context, R.color.sleep_rem));

        lightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lightPaint.setColor(ContextCompat.getColor(context, R.color.sleep_light));

        bounds = new RectF();
        cornerRadius = getResources().getDisplayMetrics().density * 8f;
    }

    public void setStages(int deep, int rem, int light) {
        this.deepMinutes = deep > 0 ? deep : 1;
        this.remMinutes = rem > 0 ? rem : 1;
        this.lightMinutes = light > 0 ? light : 1;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float total = deepMinutes + remMinutes + lightMinutes;
        float deepWidth = (deepMinutes / total) * w;
        float remWidth = (remMinutes / total) * w;
        float lightWidth = w - deepWidth - remWidth;

        // Clip rounded rectangle for entire bar
        canvas.save();
        bounds.set(0, 0, w, h);
        android.graphics.Path clipPath = new android.graphics.Path();
        clipPath.addRoundRect(bounds, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW);
        canvas.clipPath(clipPath);

        // 1. Draw Deep Sleep Segment
        canvas.drawRect(0, 0, deepWidth, h, deepPaint);

        // 2. Draw REM Sleep Segment
        canvas.drawRect(deepWidth, 0, deepWidth + remWidth, h, remPaint);

        // 3. Draw Light Sleep Segment
        canvas.drawRect(deepWidth + remWidth, 0, w, h, lightPaint);

        canvas.restore();
    }
}

package com.fitnessapp.ui.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.fitnessapp.R;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Custom Hardware-Accelerated Circular Progress Ring View.
 * Renders glowing neon-mint/cyan gradient arc on deep slate track with inner metric typography.
 */
public class CircularProgressRingView extends View {

    private Paint bgArcPaint;
    private Paint progressArcPaint;
    private Paint stepTextPaint;
    private Paint labelTextPaint;
    private Paint goalTextPaint;

    private RectF arcBounds;
    private float strokeWidth = 42f;

    private long currentSteps = 0;
    private int targetGoal = 10000;
    private float animatedSweepAngle = 0f;

    private int primaryColor;
    private int secondaryColor;
    private int trackColor;

    private static final float START_ANGLE = 135f;
    private static final float TOTAL_SWEEP_ANGLE = 270f;

    public CircularProgressRingView(Context context) {
        super(context);
        init(context);
    }

    public CircularProgressRingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CircularProgressRingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        primaryColor = ContextCompat.getColor(context, R.color.google_fit_cyan);
        secondaryColor = ContextCompat.getColor(context, R.color.google_fit_green);
        trackColor = ContextCompat.getColor(context, R.color.ring_track);
        int textColor = ContextCompat.getColor(context, R.color.text_primary);
        int mutedColor = ContextCompat.getColor(context, R.color.text_secondary);

        strokeWidth = getResources().getDisplayMetrics().density * 16f;

        bgArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgArcPaint.setStyle(Paint.Style.STROKE);
        bgArcPaint.setStrokeWidth(strokeWidth);
        bgArcPaint.setColor(trackColor);
        bgArcPaint.setStrokeCap(Paint.Cap.ROUND);

        progressArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressArcPaint.setStyle(Paint.Style.STROKE);
        progressArcPaint.setStrokeWidth(strokeWidth);
        progressArcPaint.setStrokeCap(Paint.Cap.ROUND);

        stepTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stepTextPaint.setColor(textColor);
        stepTextPaint.setTextAlign(Paint.Align.CENTER);
        stepTextPaint.setTextSize(getResources().getDisplayMetrics().density * 42f);
        stepTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        labelTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelTextPaint.setColor(secondaryColor);
        labelTextPaint.setTextAlign(Paint.Align.CENTER);
        labelTextPaint.setTextSize(getResources().getDisplayMetrics().density * 12f);
        labelTextPaint.setLetterSpacing(0.18f);
        labelTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        goalTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        goalTextPaint.setColor(mutedColor);
        goalTextPaint.setTextAlign(Paint.Align.CENTER);
        goalTextPaint.setTextSize(getResources().getDisplayMetrics().density * 13f);

        arcBounds = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = strokeWidth / 2f + 16f;
        arcBounds.set(padding, padding, w - padding, h - padding);

        // Vibrant neon cyan to mint gradient sweep
        int[] colors = new int[]{primaryColor, secondaryColor, primaryColor};
        SweepGradient shader = new SweepGradient(w / 2f, h / 2f, colors, new float[]{0.0f, 0.75f, 1.0f});
        progressArcPaint.setShader(shader);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        // 1. Draw Slate Track Arc
        canvas.drawArc(arcBounds, START_ANGLE, TOTAL_SWEEP_ANGLE, false, bgArcPaint);

        // 2. Draw Active Neon Gradient Progress Arc
        if (animatedSweepAngle > 0.5f) {
            canvas.drawArc(arcBounds, START_ANGLE, animatedSweepAngle, false, progressArcPaint);
        }

        // 3. Draw Center Inner Metrics
        String stepsStr = NumberFormat.getNumberInstance(Locale.US).format(currentSteps);
        String goalStr = "Goal: " + NumberFormat.getNumberInstance(Locale.US).format(targetGoal);

        float stepY = cy + (stepTextPaint.getTextSize() * 0.32f);
        canvas.drawText("STEPS", cx, stepY - stepTextPaint.getTextSize() * 0.85f, labelTextPaint);
        canvas.drawText(stepsStr, cx, stepY, stepTextPaint);
        canvas.drawText(goalStr, cx, stepY + goalTextPaint.getTextSize() * 1.5f, goalTextPaint);
    }

    public void setProgress(long steps, int goal, boolean animate) {
        this.currentSteps = steps;
        this.targetGoal = goal > 0 ? goal : 10000;

        float ratio = (float) steps / (float) targetGoal;
        if (ratio > 1.0f) ratio = 1.0f;
        float targetAngle = ratio * TOTAL_SWEEP_ANGLE;

        if (animate) {
            ValueAnimator animator = ValueAnimator.ofFloat(animatedSweepAngle, targetAngle);
            animator.setDuration(1200);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                animatedSweepAngle = (float) animation.getAnimatedValue();
                invalidate();
            });
            animator.start();
        } else {
            animatedSweepAngle = targetAngle;
            invalidate();
        }
    }
}

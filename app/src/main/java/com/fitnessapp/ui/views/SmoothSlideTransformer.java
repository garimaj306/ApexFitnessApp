package com.fitnessapp.ui.views;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

/**
 * High-Performance Smooth Horizontal Slide Page Transformer.
 * Optimized for OpenGL Maps, Charts, and 60 FPS Emulator / Device rendering without GPU stalls.
 */
public class SmoothSlideTransformer implements ViewPager2.PageTransformer {

    @Override
    public void transformPage(@NonNull View page, float position) {
        if (position < -1) {
            page.setAlpha(0f);
        } else if (position <= 1) {
            page.setAlpha(1f);
            page.setTranslationX(0f);
        } else {
            page.setAlpha(0f);
        }
    }
}

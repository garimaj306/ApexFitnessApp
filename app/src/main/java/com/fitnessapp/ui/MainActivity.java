package com.fitnessapp.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.fitnessapp.R;
import com.fitnessapp.data.firebase.FirebaseManager;
import com.fitnessapp.ui.adapters.MainPagerAdapter;
import com.fitnessapp.ui.views.SmoothSlideTransformer;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Main Activity hosting the 5-Tab Layout with ViewPager2 Swipe Gestures and Slide Animations.
 */
public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private boolean isNavigatingFromBottom = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is signed in
        if (!FirebaseManager.getInstance(this).isUserAuthenticated()) {
            Intent authIntent = new Intent(this, AuthActivity.class);
            startActivity(authIntent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.view_pager);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        setupViewPagerAndNavigation();
    }

    private void setupViewPagerAndNavigation() {
        MainPagerAdapter pagerAdapter = new MainPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(1); // Lazy loading: loads adjacent tab only to prevent OpenGL/Map thread contention
        viewPager.setPageTransformer(new SmoothSlideTransformer());

        // Synchronize ViewPager2 Swipe gestures -> BottomNavigationView
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (!isNavigatingFromBottom) {
                    switch (position) {
                        case 0:
                            bottomNavigationView.setSelectedItemId(R.id.nav_today);
                            break;
                        case 1:
                            bottomNavigationView.setSelectedItemId(R.id.nav_workout);
                            break;
                        case 2:
                            bottomNavigationView.setSelectedItemId(R.id.nav_vitals);
                            break;
                        case 3:
                            bottomNavigationView.setSelectedItemId(R.id.nav_insights);
                            break;
                        case 4:
                            bottomNavigationView.setSelectedItemId(R.id.nav_profile);
                            break;
                    }
                }
            }
        });

        // Synchronize BottomNavigationView Click -> ViewPager2 Smooth Slide Animation
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            int targetPage = 0;

            if (itemId == R.id.nav_today) {
                targetPage = 0;
            } else if (itemId == R.id.nav_workout) {
                targetPage = 1;
            } else if (itemId == R.id.nav_vitals) {
                targetPage = 2;
            } else if (itemId == R.id.nav_insights) {
                targetPage = 3;
            } else if (itemId == R.id.nav_profile) {
                targetPage = 4;
            }

            if (viewPager.getCurrentItem() != targetPage) {
                isNavigatingFromBottom = true;
                viewPager.setCurrentItem(targetPage, true); // Smooth slide transition
                isNavigatingFromBottom = false;
            }
            return true;
        });
    }
}

package com.fitnessapp.ui.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.fitnessapp.ui.insights.InsightsFragment;
import com.fitnessapp.ui.profile.ProfileFragment;
import com.fitnessapp.ui.today.TodayFragment;
import com.fitnessapp.ui.vitals.VitalsFragment;
import com.fitnessapp.ui.workout.WorkoutFragment;

/**
 * ViewPager2 Adapter enabling smooth horizontal swipe gesture navigation across the 5 primary tabs.
 */
public class MainPagerAdapter extends FragmentStateAdapter {

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new TodayFragment();
            case 1:
                return new WorkoutFragment();
            case 2:
                return new VitalsFragment();
            case 3:
                return new InsightsFragment();
            case 4:
                return new ProfileFragment();
            default:
                return new TodayFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}

package uk.co.deanwild.materialshowcaseview;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;


public class TourViewPagerAdapter extends FragmentPagerAdapter {

    List<Fragment> screens = new ArrayList<>();

    public TourViewPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public void setTourScreens(List<Fragment> screens) {
        this.screens = screens;
    }

    @Nullable
    @Override
    public Fragment getItem(int position) {
        return position > (getCount() - 1) ? null : screens.get(position);
    }

    @Override
    public int getCount() {
        return screens.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return ((TourFragment) screens.get(position)).getPageTitle();
    }
}

package uk.co.deanwild.materialshowcaseviewsample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.TourFragment;
import uk.co.deanwild.materialshowcaseview.TourViewPager;
import uk.co.deanwild.materialshowcaseview.TourViewPagerAdapter;
import uk.co.deanwild.materialshowcaseview.model.Position;
import uk.co.deanwild.materialshowcaseview.shape.RectangleShape;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;


public class SimpleSingleExample extends AppCompatActivity implements View.OnClickListener {

    private Button mButtonShow;
    private Button mButtonReset;

    private static final String SHOWCASE_ID = "simple example";
    private LinearLayout.LayoutParams layoutParamsForTour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_single_example);
        mButtonShow = (Button) findViewById(R.id.btn_show);
        mButtonShow.setOnClickListener(this);

        mButtonReset = (Button) findViewById(R.id.btn_reset);
        mButtonReset.setOnClickListener(this);

        presentShowcaseView(1000); // one second delay
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.btn_show) {

            presentShowcaseView(0);

        } else if (v.getId() == R.id.btn_reset) {

            MaterialShowcaseView.resetSingleUse(this, SHOWCASE_ID);
            Toast.makeText(this, "Showcase reset", Toast.LENGTH_SHORT).show();
        }

    }

    private void presentShowcaseView(int withDelay) {
        TourViewPager tourViewPager = new TourViewPager(this);
        tourViewPager.setId(R.id.viewPagerId);
        tourViewPager.setLayoutParams(getLayoutParamsForTour());
        TourViewPagerAdapter tourViewPagerAdapter = new TourViewPagerAdapter(getSupportFragmentManager());
        tourViewPagerAdapter.setTourScreens(getCustomTourScreens());
        tourViewPager.setAdapter(tourViewPagerAdapter);
        new MaterialShowcaseView.Builder(this)
                .setActiveTarget(mButtonShow)
                .setActiveTargetTouchable(true)
                .setActiveTargetShape(new RectangleShape(20, 20))
                .setHightlightTarget(mButtonShow)
                .setHighlightShape(new RectangleShape(20, 20))
                .setUserPrompt(getCustomActionPrompt(), Position.CENTER)
                .setDelay(withDelay) // optional but starting animations immediately in onCreate can make them choppy
                .show();
    }

    @NonNull
    private View getCustomActionPrompt() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(getCustomImageView());
        linearLayout.addView(getCustomTextView());
        return linearLayout;

    }

    @NonNull
    private TextView getCustomTextView() {
        TextView textView = new TextView(this);
        textView.setText("This is a custom text view");
        textView.setTextColor(ContextCompat.getColor(this, R.color.green));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.setMargins(30, 30, 30, 30);
        textView.setLayoutParams(layoutParams);
        textView.setPadding(30, 10, 30, 10);
        return textView;
    }

    @NonNull
    private ImageView getCustomImageView() {
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(R.drawable.swipe_hand_add_money);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.setMargins(30, 30, 30, 30);
        imageView.setLayoutParams(layoutParams);
        imageView.setPadding(30, 10, 30, 10);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("imageview", "click");
            }
        });
        return imageView;
    }

    @NonNull
    private ViewGroup getCustomViewGroup() {
        LinearLayout linearLayout = new LinearLayout(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setBackgroundColor(0xFF000000);
        linearLayout.addView(getCustomTextView());
        linearLayout.addView(getCustomImageView());
        return linearLayout;
    }

    private List<Fragment> getCustomTourScreens() {
        List<Fragment> screens = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            TourFragment tourFragment = new TourFragment();
            tourFragment.setView(getCustomViewGroup());
            screens.add(tourFragment);
        }
        return screens;
    }

    public FrameLayout.LayoutParams getLayoutParamsForTour() {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER);
        layoutParams.setMargins(150, 50, 150, 50);
        return layoutParams;
    }
}

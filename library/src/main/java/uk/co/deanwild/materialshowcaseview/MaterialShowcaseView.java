package uk.co.deanwild.materialshowcaseview;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import uk.co.deanwild.materialshowcaseview.model.Position;
import uk.co.deanwild.materialshowcaseview.shape.CircleShape;
import uk.co.deanwild.materialshowcaseview.shape.NoShape;
import uk.co.deanwild.materialshowcaseview.shape.RectangleShape;
import uk.co.deanwild.materialshowcaseview.shape.Shape;
import uk.co.deanwild.materialshowcaseview.target.Target;
import uk.co.deanwild.materialshowcaseview.target.ViewTarget;

import static android.view.Gravity.BOTTOM;


/**
 * Helper class to show a sequence of showcase screens.
 */
public class MaterialShowcaseView extends FrameLayout implements View.OnTouchListener {

    private static final String LOG_TAG = "Showcase";

    private int mOldHeight;
    private int mOldWidth;
    private Bitmap mBitmap;// = new WeakReference<>(null);
    private Canvas mCanvas;
    private Paint mEraser;
    private Target activeTarget;
    private Target mHighlightTarget;
    private Shape mActiveTargetShape;
    private Shape mHighlightShape;
    private Target spotlightTargetView;
    private int mXPosition;
    private int mYPosition;
    private boolean mWasDismissed = false;
    private int mShapePadding = ShowcaseConfig.DEFAULT_SHAPE_PADDING;

    private View parentContentView;
    private int mGravity;
    private int mContentBottomMargin;
    private int mContentTopMargin;
    private int mContentLeftMargin;
    private boolean mDismissOnTouch = false;
    private boolean mShouldRender = false; // flag to decide when we should actually render
    private int mMaskColour;
    private AnimationFactory mAnimationFactory;
    private boolean mShouldAnimate = true;
    private long mFadeDurationInMillis = ShowcaseConfig.DEFAULT_FADE_TIME;
    private Handler mHandler;
    private long mDelayInMillis = ShowcaseConfig.DEFAULT_DELAY;
    private boolean mSingleUse = false; // should display only once
    private PrefsManager mPrefsManager; // used to store state doe single use mode
    List<IShowcaseListener> mListeners; // external listeners who want to observe when we show and dismiss
    private UpdateOnGlobalLayout mLayoutListener;
    private IDetachedListener mDetachedListener;
    private boolean mActiveTargetTouchable = false;
    private boolean mDismissOnTargetTouch = true;
    private boolean isBackgroundViewActive = false;

    private TourViewPager tourViewPager;
    private TourViewPagerAdapter tourViewPagerAdapter;
    private LinearLayout indicatorLayout;
    private ImageView[] indicators;
    private boolean shouldShowUserPrompt = false;
    private Position userPromptPosition;
    private Position spotlightPosition;
    private boolean shouldShowSpotlight = false;
    private boolean showingTourView = false;

    public MaterialShowcaseView(Context context) {
        super(context);
        init(context);
    }

    public MaterialShowcaseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MaterialShowcaseView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MaterialShowcaseView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }


    private void init(Context context) {
        setWillNotDraw(false);

        // create our animation factory
        mAnimationFactory = new AnimationFactory();

        mListeners = new ArrayList<>();

        // make sure we add a global layout listener so we can adapt to changes
        mLayoutListener = new UpdateOnGlobalLayout();
        getViewTreeObserver().addOnGlobalLayoutListener(mLayoutListener);

        // consume touch events
        setOnTouchListener(this);

        mMaskColour = Color.parseColor(ShowcaseConfig.DEFAULT_MASK_COLOUR);
        setVisibility(INVISIBLE);


        View contentView = LayoutInflater.from(getContext()).inflate(R.layout.showcase_content, this, true);
        parentContentView = contentView.findViewById(R.id.content_box);
    }


    /**
     * Interesting drawing stuff.
     * We draw a block of semi transparent colour to fill the whole screen then we draw of transparency
     * to create a circular "viewport" through to the underlying content
     *
     * @param canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // don't bother drawing if we're not ready
        if (!mShouldRender) return;

        // get current dimensions
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();

        // don't bother drawing if there is nothing to draw on
        if (width <= 0 || height <= 0) return;

        // build a new canvas if needed i.e first pass or new dimensions
        if (mBitmap == null || mCanvas == null || mOldHeight != height || mOldWidth != width) {
            if (mBitmap != null) {
                mBitmap.recycle();
            }
            mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
        }

        // save our 'old' dimensions
        mOldWidth = width;
        mOldHeight = height;

        // clear canvas
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        if (!isBackgroundViewActive) {
            // draw overlay background
            mCanvas.drawColor(mMaskColour);
        }

        // Prepare active element if needed
        checkAndMakeActiveTarget();

        checkAndDrawHighlight();


        // Draw the bitmap on our screens canvas.
        canvas.drawBitmap(mBitmap, 0, 0, null);

    }

    private void checkAndDrawHighlight() {
        // highlight view if asked for it
        if (mHighlightTarget != null && mHighlightShape != null) {
            Paint highLightPaint = new Paint();
            highLightPaint.setStyle(Paint.Style.STROKE);
            highLightPaint.setColor(0xFFFF0000);
            highLightPaint.setStrokeWidth(3.0f);
            mHighlightShape.draw(mCanvas, highLightPaint, mHighlightTarget.getPoint().x, mHighlightTarget.getPoint().y, 0);
        }
    }

    private void checkAndMakeActiveTarget() {
        if (activeTarget == null || mActiveTargetShape == null) {
            return;
        }
        if (mEraser == null) {
            mEraser = new Paint();
            mEraser.setColor(0xFFFFFFFF);
            mEraser.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            mEraser.setFlags(Paint.ANTI_ALIAS_FLAG);
        }

        // draw (erase) shape
        mActiveTargetShape.draw(mCanvas, mEraser, mXPosition, mYPosition, mShapePadding);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        /**
         * If we're being detached from the window without the mWasDismissed flag then we weren't purposefully dismissed
         * Probably due to an orientation change or user backed out of activity.
         * Ensure we reset the flag so the showcase display again.
         */
        if (!mWasDismissed && mSingleUse && mPrefsManager != null) {
            mPrefsManager.resetShowcase();
        }


        notifyOnDismissed();

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mDismissOnTouch) {
            hide();
        }
        if (spotlightTargetView != null && spotlightTargetView.getBounds().contains((int) event.getX(), (int) event.getY())) {
            return true;
        }
        if (isBackgroundViewActive) {
            return false;
        }

        if (mActiveTargetTouchable && activeTarget != null && activeTarget.getBounds().contains((int) event.getX(), (int) event.getY())) {
            if(mDismissOnTargetTouch){
                hide();
            }
            return false;
        }
        return true;
    }


    private void notifyOnDisplayed() {

		if(mListeners != null){
			for (IShowcaseListener listener : mListeners) {
				listener.onShowcaseDisplayed(this);
			}
		}
    }

    private void notifyOnDismissed() {
        if (mListeners != null) {
            for (IShowcaseListener listener : mListeners) {
                listener.onShowcaseDismissed(this);
            }

            mListeners.clear();
            mListeners = null;
        }

        /**
         * internal listener used by sequence for storing progress within the sequence
         */
        if (mDetachedListener != null) {
            mDetachedListener.onShowcaseDetached(this, mWasDismissed);
        }
    }

    public void setUserPrompt(View view, Position position) {
        shouldShowUserPrompt = true;
        userPromptPosition = position;
        clearContentBoxAndAddView(view);

        if (position == Position.ABSOLUTE_CENTER) {
            activeTarget = null;
            mContentTopMargin = getMeasuredHeight() / 2;
            mContentLeftMargin = getMeasuredWidth() / 2;
            mContentBottomMargin = 0;
            mGravity = Gravity.CENTER;
            applyLayoutParams();
        }
    }

    /**
     * Tells us about the "Target" which is the view we want to anchor to.
     * We figure out where it is on screen and (optionally) how big it is.
     * We also figure out whether to place our content and dismiss button above or below it.
     *
     * @param target
     */
    public void setActiveTarget(Target target) {
        activeTarget = target;
    }

    public void setActiveTargetDescriptionView() {

    }

    private void updateContentViewLayout() {
        updateContentViewLayoutAccordingToActiveTarget();

        applyLayoutParams();
    }

    private void updateContentViewLayoutAccordingToActiveTarget() {
        if (activeTarget != null) {

            // apply the target position
            Point targetPoint = activeTarget.getPoint();
            Rect targetBounds = activeTarget.getBounds();
            setPosition(targetPoint);

            int radius = Math.max(targetBounds.height(), targetBounds.width()) / 2;
            if (mActiveTargetShape != null) {
                mActiveTargetShape.updateTarget(activeTarget);
                radius = mActiveTargetShape.getHeight() / 2;
            }

            if (shouldShowContentBox()) {

                // now figure out whether to put content above or below it, OR at the center
                int height = getMeasuredHeight();
                int yPos = targetPoint.y;
                int xPos = targetPoint.x;
                Position position = shouldShowSpotlight ? spotlightPosition : userPromptPosition;

                updateContentLayoutMarginsAccToChildPosition(targetBounds, radius, height, yPos, xPos, position);
            }
        }
    }

    private void updateContentLayoutMarginsAccToChildPosition(Rect targetBounds, int radius, int height, int yPos, int xPos, Position position) {
        if (Position.CENTER_OF_ACTIVE_TARGET.equals(position)) {
            //content should start from the center of target. So we just need to set the upper margin.
            mContentTopMargin = yPos;
            mContentBottomMargin = 0;
            mContentLeftMargin = xPos - targetBounds.width() / 2;
            mGravity = Gravity.TOP;
        } else if (Position.ABOVE_OF_ACTIVE_TARGET.equals(position)) {
            // show user prompt above active target
            mContentTopMargin = 0;
            mContentBottomMargin = (height - yPos) + radius + mShapePadding;
            mContentLeftMargin = 0;
            mGravity = BOTTOM;
        } else if (Position.BELOW_OF_ACTIVE_TARGET.equals(position)) {
            // show user prompt below active target
            mContentTopMargin = yPos + radius + mShapePadding;
            mContentBottomMargin = 0;
            mContentLeftMargin = 0;
            mGravity = Gravity.TOP;
        }
    }

    private boolean shouldShowContentBox() {
        if (shouldShowSpotlight && shouldShowUserPrompt || (!shouldShowSpotlight && !shouldShowUserPrompt)) {
            Log.w(LOG_TAG, "can't show both spotlight and user prompt at the same time");
            return false;
        }
        return (userPromptPosition != Position.ABSOLUTE_CENTER && spotlightPosition != Position.ABSOLUTE_CENTER);
    }

    private void clearContentBoxAndAddView(View view) {
        ((ViewGroup) parentContentView).removeAllViews();
        ((ViewGroup) parentContentView).addView(view);
    }

    private void applyLayoutParams() {

        if (parentContentView != null && parentContentView.getLayoutParams() != null) {
            FrameLayout.LayoutParams contentLP = (LayoutParams) parentContentView.getLayoutParams();

            boolean layoutParamsChanged = false;

            if (contentLP.bottomMargin != mContentBottomMargin) {
                contentLP.bottomMargin = mContentBottomMargin;
                layoutParamsChanged = true;
            }

            if (contentLP.topMargin != mContentTopMargin) {
                contentLP.topMargin = mContentTopMargin;
                layoutParamsChanged = true;
            }

            if (contentLP.leftMargin != mContentLeftMargin) {
                contentLP.leftMargin = mContentLeftMargin;
                layoutParamsChanged = true;
            }

            if (contentLP.gravity != mGravity) {
                contentLP.gravity = mGravity;
                layoutParamsChanged = true;
            }

            /**
             * Only apply the layout params if we've actually changed them, otherwise we'll get stuck in a layout loop
             */
            if (layoutParamsChanged)
                parentContentView.setLayoutParams(contentLP);
        }
    }

    /***
     * for now, if you are showing spotlight, you can't show any other view
     *
     * @param view
     */
    public void setSpotlightView(View view, Position position) {
        shouldShowSpotlight = true;
        spotlightTargetView = new ViewTarget(view);
        spotlightPosition = position;
        clearContentBoxAndAddView(view);

        if (position == Position.ABSOLUTE_CENTER) {
            activeTarget = null;
            mContentTopMargin = getMeasuredHeight() / 2;
            mContentLeftMargin = getMeasuredWidth() / 2;
            mContentBottomMargin = 0;
            mGravity = Gravity.CENTER;
            applyLayoutParams();
        }
    }

    public void setTourScreens(List<Fragment> screens, Context context, FragmentManager fragmentManager,
                               FrameLayout.LayoutParams layoutParams) {
        showingTourView = true;
        tourViewPager = new TourViewPager(context);
        tourViewPager.setId(R.id.viewPagerId);
        tourViewPager.setLayoutParams(layoutParams);
        tourViewPagerAdapter = new TourViewPagerAdapter(fragmentManager);
        tourViewPagerAdapter.setTourScreens(screens);
        tourViewPager.setAdapter(tourViewPagerAdapter);

        ((ViewGroup) parentContentView).removeAllViews();
        ((ViewGroup) parentContentView).addView(tourViewPager);

        indicatorLayout = getDefaultIndicatorLayoutForTour(context);
        indicators = new ImageView[tourViewPagerAdapter.getCount()];
        for (int i = 0; i < tourViewPagerAdapter.getCount(); i++) {
            indicators[i] = new ImageView(context);
            indicators[i].setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            indicators[i].setPadding(10, 0, 10, 0);
            indicators[i].setImageResource(R.drawable.selected_dot_indicator);
            indicatorLayout.addView(indicators[i]);
        }

        ((ViewGroup) parentContentView).addView(indicatorLayout);
        changeTourIndicators(0);

        tourViewPager.addOnPageChangeListener(getTourPageChangeListener());

        mGravity = BOTTOM;
        mContentBottomMargin = 100;
        mContentTopMargin = 50;
        parentContentView.setPadding(50, 200, 50, 200);
        parentContentView.setBackgroundColor(0xFF000000);
        applyLayoutParams();
    }

    private ViewPager.OnPageChangeListener getTourPageChangeListener() {
        return new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                changeTourIndicators(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        };
    }

    private LinearLayout getDefaultIndicatorLayoutForTour(Context context) {
        LinearLayout linearLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.bottomMargin = 25;
        layoutParams.topMargin = 20;
        layoutParams.gravity = Gravity.CENTER;
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        return linearLayout;
    }

    private void changeTourIndicators(int position) {
        for (int i = 0; i < tourViewPagerAdapter.getCount(); i++) {
            if (i == position) {
                indicators[i].setImageResource(R.drawable.selected_dot_indicator);
                continue;
            }
            indicators[i].setImageDrawable(getResources().getDrawable(R.drawable.unselected_dot_indicator));
        }
    }

    /**
     * SETTERS
     */

    void setPosition(Point point) {
        setPosition(point.x, point.y);
    }

    void setPosition(int x, int y) {
        mXPosition = x;
        mYPosition = y;
    }

    private void setActiveTargetShapePadding(int padding) {
        mShapePadding = padding;
    }

    private void setDismissOnTouch(boolean dismissOnTouch) {
        mDismissOnTouch = dismissOnTouch;
    }

    private void setShouldRender(boolean shouldRender) {
        mShouldRender = shouldRender;
    }

    private void setMaskColour(int maskColour) {
        mMaskColour = maskColour;
    }

    private void setDelay(long delayInMillis) {
        mDelayInMillis = delayInMillis;
    }

    private void setFadeDuration(long fadeDurationInMillis) {
        mFadeDurationInMillis = fadeDurationInMillis;
    }

    private void setActiveTargetTouchable(boolean targetTouchable) {
        mActiveTargetTouchable = targetTouchable;
    }

    private void setDismissOnTargetTouch(boolean dismissOnTargetTouch){
        mDismissOnTargetTouch = dismissOnTargetTouch;
    }

    public void addShowcaseListener(IShowcaseListener showcaseListener) {

		if(mListeners != null)
			mListeners.add(showcaseListener);
    }

    public void removeShowcaseListener(MaterialShowcaseSequence showcaseListener) {

		if ((mListeners != null) && mListeners.contains(showcaseListener)) {
			mListeners.remove(showcaseListener);
		}
    }

    void setDetachedListener(IDetachedListener detachedListener) {
        mDetachedListener = detachedListener;
    }

    public void setActiveTargetShape(Shape mShape) {
        this.mActiveTargetShape = mShape;
    }

    /**
     * Set properties based on a config object
     *
     * @param config
     */
    public void setConfig(ShowcaseConfig config) {
        setDelay(config.getDelay());
        setFadeDuration(config.getFadeDuration());
        setMaskColour(config.getMaskColor());
        setActiveTargetShape(config.getShape());
        setActiveTargetShapePadding(config.getShapePadding());
    }

    public boolean hasFired() {
        return mPrefsManager.hasFired();
    }

    public void setHighlightShape(Shape highlightShape) {
        this.mHighlightShape = highlightShape;
    }

    /**
     * REDRAW LISTENER - this ensures we redraw after activity finishes laying out
     */
    private class UpdateOnGlobalLayout implements ViewTreeObserver.OnGlobalLayoutListener {

        @Override
        public void onGlobalLayout() {
            updateContentViewLayout();
            updateHighlightTargetLayout();
        }
    }


    /**
     * BUILDER CLASS
     * Gives us a builder utility class with a fluent API for eaily configuring showcase screens
     */
    public static class Builder {
        private static final int CIRCLE_SHAPE = 0;
        private static final int RECTANGLE_SHAPE = 1;
        private static final int NO_SHAPE = 2;

        private boolean fullWidth = false;
        private int shapeType = CIRCLE_SHAPE;

        final MaterialShowcaseView showcaseView;

        private final Activity activity;

        public Builder(Activity activity) {
            this.activity = activity;

            showcaseView = new MaterialShowcaseView(activity);
        }

        /**
         * Set the title text shown on the ShowcaseView.
         */
        public Builder setActiveTarget(View activeTarget) {
            showcaseView.setActiveTarget(new ViewTarget(activeTarget));
            return this;
        }


        /**
         * Set whether or not the target view can be touched while the showcase is visible.
         *
         * False by default.
         */
        public Builder setActiveTargetTouchable(boolean targetTouchable) {
            showcaseView.setActiveTargetTouchable(targetTouchable);
            return this;
        }

        /**
         * Set whether or not the showcase should dismiss when the target is touched.
         *
         * True by default.
         */
        public Builder setDismissOnTargetTouch(boolean dismissOnTargetTouch){
            showcaseView.setDismissOnTargetTouch(dismissOnTargetTouch);
            return this;
        }

        public Builder setDismissOnTouch(boolean dismissOnTouch) {
            showcaseView.setDismissOnTouch(dismissOnTouch);
            return this;
        }

        public Builder setMaskColour(int maskColour) {
            showcaseView.setMaskColour(maskColour);
            return this;
        }

        public Builder setDelay(int delayInMillis) {
            showcaseView.setDelay(delayInMillis);
            return this;
        }

        public Builder setFadeDuration(int fadeDurationInMillis) {
            showcaseView.setFadeDuration(fadeDurationInMillis);
            return this;
        }

        public Builder setListener(IShowcaseListener listener) {
            showcaseView.addShowcaseListener(listener);
            return this;
        }

        public Builder singleUse(String showcaseID) {
            showcaseView.singleUse(showcaseID);
            return this;
        }

        public Builder setActiveTargetShape(Shape shape) {
            showcaseView.setActiveTargetShape(shape);
            return this;
        }

        public Builder setHighlightShape(Shape shape) {
            showcaseView.setHighlightShape(shape);
            return this;
        }

        public Builder withCircleShape() {
            shapeType = CIRCLE_SHAPE;
            return this;
        }

        public Builder withoutShape() {
            shapeType = NO_SHAPE;
            return this;
        }

        public Builder setActiveTargetShapePadding(int padding) {
            showcaseView.setActiveTargetShapePadding(padding);
            return this;
        }

        public Builder withRectangleShape() {
            return withRectangleShape(false);
        }

        public Builder withRectangleShape(boolean fullWidth) {
            this.shapeType = RECTANGLE_SHAPE;
            this.fullWidth = fullWidth;
            return this;
        }

        public Builder setHightlightTarget(View view) {
            showcaseView.setHighlightTarget(new ViewTarget(view));
            return this;
        }

        public Builder setBackgroundViewActive(boolean value) {
            showcaseView.setBackgroundViewActive(value);
            return this;
        }

        public Builder setSpotlightView(View view, Position position) {
            showcaseView.setSpotlightView(view, position);
            return this;
        }

        public Builder setTourView(List<Fragment> screens, Context context, FragmentManager fragmentManager, FrameLayout.LayoutParams layoutParams) {
            showcaseView.setTourScreens(screens, context, fragmentManager, layoutParams);
            return this;
        }

        public Builder setUserPrompt(View view, Position position) {
            showcaseView.setUserPrompt(view, position);
            return this;
        }

        public MaterialShowcaseView build() {
            if (showcaseView.activeTarget != null && showcaseView.mActiveTargetShape == null) {
                switch (shapeType) {
                    case RECTANGLE_SHAPE: {
                        showcaseView.setActiveTargetShape(new RectangleShape(showcaseView.activeTarget.getBounds(), fullWidth));
                        break;
                    }
                    case CIRCLE_SHAPE: {
                        showcaseView.setActiveTargetShape(new CircleShape(showcaseView.activeTarget));
                        break;
                    }
                    case NO_SHAPE: {
                        showcaseView.setActiveTargetShape(new NoShape());
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("Unsupported shape type: " + shapeType);
                }
            }

            return showcaseView;
        }

        public MaterialShowcaseView show() {
            build().show(activity);
            return showcaseView;
        }

    }

    private void setHighlightTarget(Target viewTarget) {
        mHighlightTarget = viewTarget;
        updateHighlightTargetLayout();
    }

    private void updateHighlightTargetLayout() {
        if (mHighlightTarget != null && mHighlightShape != null) {
            mHighlightShape.updateTarget(mHighlightTarget);
        }
    }

    private void setBackgroundViewActive(boolean value) {
        isBackgroundViewActive = value;
    }

    private void singleUse(String showcaseID) {
        mSingleUse = true;
        mPrefsManager = new PrefsManager(getContext(), showcaseID);
    }

    public void removeFromWindow() {
        if (getParent() != null && getParent() instanceof ViewGroup) {
            ((ViewGroup) getParent()).removeView(this);
        }

        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }

        mEraser = null;
        mAnimationFactory = null;
        mCanvas = null;
        mHandler = null;

        getViewTreeObserver().removeGlobalOnLayoutListener(mLayoutListener);
        mLayoutListener = null;

        if (mPrefsManager != null)
            mPrefsManager.close();

        mPrefsManager = null;


    }


    /**
     * Reveal the showcaseview. Returns a boolean telling us whether we actually did show anything
     *
     * @param activity
     * @return
     */
    public boolean show(final Activity activity) {

        /**
         * if we're in single use mode and have already shot our bolt then do nothing
         */
        if (mSingleUse) {
            if (mPrefsManager.hasFired()) {
                return false;
            } else {
                mPrefsManager.setFired();
            }
        }

        ((ViewGroup) activity.getWindow().getDecorView()).addView(this);

        setShouldRender(true);

        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (mShouldAnimate) {
                    fadeIn();
                } else {
                    setVisibility(VISIBLE);
                    notifyOnDisplayed();
                }
            }
        }, mDelayInMillis);

        return true;
    }


    public void hide() {

        /**
         * This flag is used to indicate to onDetachedFromWindow that the showcase view was dismissed purposefully (by the user or programmatically)
         */
        mWasDismissed = true;

        if (mShouldAnimate) {
            fadeOut();
        } else {
            removeFromWindow();
        }
    }

    public void fadeIn() {
        setVisibility(INVISIBLE);

        mAnimationFactory.fadeInView(this, mFadeDurationInMillis,
                new IAnimationFactory.AnimationStartListener() {
                    @Override
                    public void onAnimationStart() {
                        setVisibility(View.VISIBLE);
                        notifyOnDisplayed();
                    }
                }
        );
    }

    public void fadeOut() {

        mAnimationFactory.fadeOutView(this, mFadeDurationInMillis, new IAnimationFactory.AnimationEndListener() {
            @Override
            public void onAnimationEnd() {
                setVisibility(INVISIBLE);
                removeFromWindow();
            }
        });
    }

    public void resetSingleUse() {
        if (mSingleUse && mPrefsManager != null) mPrefsManager.resetShowcase();
    }

    /**
     * Static helper method for resetting single use flag
     *
     * @param context
     * @param showcaseID
     */
    public static void resetSingleUse(Context context, String showcaseID) {
        PrefsManager.resetShowcase(context, showcaseID);
    }

    /**
     * Static helper method for resetting all single use flags
     *
     * @param context
     */
    public static void resetAll(Context context) {
        PrefsManager.resetAll(context);
    }

    public static int getSoftButtonsBarSizePort(Activity activity) {
        // getRealMetrics is only available with API 17 and +
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int usableHeight = metrics.heightPixels;
            activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            int realHeight = metrics.heightPixels;
            if (realHeight > usableHeight)
                return realHeight - usableHeight;
            else
                return 0;
        }
        return 0;
    }
}

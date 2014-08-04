package com.yildizkabaran.newsdigestsplash.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewManager;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import com.yildizkabaran.newsdigestsplash.BuildConfig;
import com.yildizkabaran.newsdigestsplash.R;

/**
 * A simple view class that displays a number of colorful circles rotating, then eventually the circles will merge
 * together and enlarge as a transparent hole
 * @author yildizkabaran
 *
 */
public class SplashView extends View {

  private static final String TAG = "SplashView";
  
  /**
   * A simple interface to listen to the state of the splash animation
   * @author yildizkabaran
   *
   */
  public static interface ISplashListener {
    public void onStart();
    public void onUpdate(float completionFraction);
    public void onEnd();
  }
  
  /**
   * A Context constructor is provided for creating the view by code. All other constructors use this constructor.
   * @param context
   */
  public SplashView(Context context){
    super(context);
    initialize();
  }

  /**
   * This constructor is redirected to the Context constructor.
   * @param context
   * @param attrs
   */
  public SplashView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize();
    setupAttributes(attrs);
  }

  /**
   * This constructor is redirected to the Context constructor.
   * @param context
   * @param attrs
   */
  public SplashView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
    setupAttributes(attrs);
  }
  
  public static final boolean DEFAULT_REMOVE_FROM_PARENT_ON_END = true;
  public static final int DEFAULT_ROTATION_RADIUS = 90;
  public static final int DEFAULT_CIRCLE_RADIUS = 18;
  public static final int DEFAULT_SPLASH_BG_COLOR = Color.WHITE;
  public static final int DEFAULT_SINGLE_CIRCLE_COLOR = Color.BLACK;
  public static final int DEFAULT_ROTATION_DURATION = 1200;
  public static final int DEFAULT_SPLASH_DURATION = 1200;
  
  private boolean mRemoveFromParentOnEnd = true; // a flag for removing the view from its parent once the animation is over
  private float mRotationRadius = DEFAULT_ROTATION_RADIUS;
  private float mCircleRadius = DEFAULT_CIRCLE_RADIUS;
  private int[] mCircleColors;
  private long mRotationDuration = DEFAULT_ROTATION_DURATION;
  private long mSplashDuration = DEFAULT_SPLASH_DURATION;
  private int mSingleCircleColor;
  private int mSplashBgColor;
  private ISplashListener mSplashListener;
  
  private float mHoleRadius = 0F;
  private float mCurrentRotationAngle = 0F;
  private float mCurrentRotationRadius;
  private float mCurrentSingleCircleRadius;
  
  private SplashState mState = null;
  
  // cache the objects so that we don't have to allocate during onDraw
  private Paint mPaint = new Paint();
  private Paint mPaintBackground = new Paint();
  
  // cache some numeric calculations
  private float mCenterX;
  private float mCenterY;
  private float mDiagonalDist;
  
  /**
   * Setup the custom attributes from XML
   * @param attrs
   */
  private void setupAttributes(AttributeSet attrs) {
    Context context = getContext();

    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NewsDigestSplashView);

    int numAttrs = a.getIndexCount();
    for (int i = 0; i < numAttrs; ++i) {
      int attr = a.getIndex(i);
      switch (attr) {
      case R.styleable.NewsDigestSplashView_removeFromParentOnEnd:
        setRemoveFromParentOnEnd(a.getBoolean(i, DEFAULT_REMOVE_FROM_PARENT_ON_END));
        break;
      case R.styleable.NewsDigestSplashView_circleRadius:
        setCircleRadius(a.getDimensionPixelSize(i, DEFAULT_CIRCLE_RADIUS));
        break;
      case R.styleable.NewsDigestSplashView_rotationRadius:
        setRotationRadius(a.getDimensionPixelSize(i, DEFAULT_ROTATION_RADIUS));
        break;
      case R.styleable.NewsDigestSplashView_rotationDuration:
        setRotationDuration(a.getInteger(i, DEFAULT_ROTATION_DURATION));
        break;
      case R.styleable.NewsDigestSplashView_splashBackgroundColor:
        setSplashBackgroundColor(a.getColor(i, DEFAULT_SPLASH_BG_COLOR));
        break;
      case R.styleable.NewsDigestSplashView_singleCircleColor:
        setSingleCircleColor(a.getColor(i, DEFAULT_SINGLE_CIRCLE_COLOR));
        break;
      case R.styleable.NewsDigestSplashView_splashDuration:
        setSplashDuration(a.getInteger(i, DEFAULT_SPLASH_DURATION));
        break;
      case R.styleable.NewsDigestSplashView_circleColors:
        int arrayId = a.getResourceId(i, -1);
        if(arrayId >= 0){
          int[] circleColors = getResources().getIntArray(arrayId);
          if(circleColors != null){
            setCircleColors(circleColors);
          }
        }
        break;
      }
    }
    a.recycle();
  }
  
  /**
   * Initialized the view properties. No much is done in this method since most variables already have set defaults
   */
  private void initialize(){
    setBackgroundColor(Color.TRANSPARENT);
    
    mPaint.setAntiAlias(true);
    
    mPaintBackground.setStyle(Paint.Style.STROKE);
    mPaintBackground.setAntiAlias(true);
    setSplashBackgroundColor(DEFAULT_SPLASH_BG_COLOR);
  }
  
  public void setCircleRadius(float circleRadius){
    mCircleRadius = circleRadius;
  }
  
  public void setRotationRadius(float rotationRadius){
    mRotationRadius = rotationRadius;
  }
  
  public void setRotationDuration(long duration){
    mRotationDuration = duration;
  }
  
  public void setSplashBackgroundColor(int bgColor){
    mSplashBgColor = bgColor;
    mPaintBackground.setColor(mSplashBgColor);
  }
  
  public void setSingleCircleColor(int circleColor){
    mSingleCircleColor = circleColor;
  }
  
  public void setSplashDuration(long duration){
    mSplashDuration = duration;
  }
  
  public void setCircleColors(int[] circleColors){
    mCircleColors = circleColors;
  }
  
  /**
   * Set the flag to remove or keep the view after the animation is over. This is set to true by default. The view must be inside a ViewManager
   * (or ViewParent) for this to work. Otherwise, the view will not be removed and a warning log will be produced.
   * @param shouldRemove
   */
  public void setRemoveFromParentOnEnd(boolean shouldRemove){
    mRemoveFromParentOnEnd = shouldRemove;
  }
  
  /**
   * Starts the disappear animation. If a listener is provided it will notify the listener on animation events
   * @param listener
   */
  public void splashAndDisappear(final ISplashListener listener){
    mSplashListener = listener;
    
    if(mState != null && mState instanceof RotationState){
      RotationState rotationState = (RotationState) mState;
      rotationState.cancel();
    }
    mState = new MergingState();
  }
  
  @Override
  protected void onSizeChanged (int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    
    mCenterX = w / 2F;
    mCenterY = h / 2F;
    mDiagonalDist = (float) Math.sqrt(w * w + h * h);
  }
  
  private void handleFirstDraw(){
    mState = new RotationState();
    
    mCurrentRotationAngle = 0F;
    mHoleRadius = 0F;
    mCurrentRotationRadius = mRotationRadius;
    mCurrentSingleCircleRadius = mCircleRadius;
  }
  
  @Override
  protected void onDraw(Canvas canvas){
    if(mState == null){
      handleFirstDraw();
    }
    
    if(mCircleColors == null){
      mCircleColors = new int[0];
    }
    
    mState.drawState(canvas);
  }
  
  private void drawBackground(Canvas canvas){
    if(mHoleRadius > 0F){
      float strokeWidth = mDiagonalDist - mHoleRadius;
      float circleRadius = mHoleRadius + strokeWidth / 2;
      
      mPaintBackground.setStrokeWidth(strokeWidth);
      canvas.drawCircle(mCenterX, mCenterY, circleRadius, mPaintBackground);
    } else {
      canvas.drawColor(mSplashBgColor);
    }
  }
  
  private void drawCircles(Canvas canvas){
    int numCircles = mCircleColors.length;
    float rotationAngle = (float) (2 * Math.PI / numCircles);
    for(int i=0; i<numCircles; ++i){
      double angle = mCurrentRotationAngle + (i * rotationAngle);
      double circleX = mCenterX + mCurrentRotationRadius * Math.sin(angle);
      double circleY = mCenterY - mCurrentRotationRadius * Math.cos(angle);
      
      mPaint.setColor(mCircleColors[i]);
      canvas.drawCircle((float) circleX, (float) circleY, mCircleRadius, mPaint);
    }
  }
  
  private void drawSingleCircle(Canvas canvas){
    mPaint.setColor(mSingleCircleColor);
    canvas.drawCircle(mCenterX, mCenterY, mCurrentSingleCircleRadius, mPaint);
  }
  
  private void removeFromParentIfNecessary(){
    // check if we need to remove the view on animation end
    if(mRemoveFromParentOnEnd){
      // get the view parent
      ViewParent parent = getParent();
      // check if a parent exists and that it implements the ViewManager interface
      if(parent != null && parent instanceof ViewManager){
        ViewManager viewManager = (ViewManager) parent;
        // remove the view from its parent
        viewManager.removeView(SplashView.this);
      } else if(BuildConfig.DEBUG) {
        // even though we had to remove the view we either don't have a parent, or the parent does not implement the method
        // necessary to remove the view, therefore create a warning log (but only do this if we are in DEBUG mode)
        Log.w(TAG, "splash view not removed after animation ended because no ViewManager parent was found");
      }
    }
  }
  
  private abstract class SplashState {
    public abstract void drawState(Canvas canvas);
  }
  
  private class RotationState extends SplashState {
    private ValueAnimator mAnimator;
    
    public RotationState(){
      mAnimator = ValueAnimator.ofFloat(0, (float) (Math.PI * 2));
      mAnimator.setDuration(mRotationDuration);
      mAnimator.setInterpolator(new LinearInterpolator());
      mAnimator.addUpdateListener(new AnimatorUpdateListener(){
        @Override
        public void onAnimationUpdate(ValueAnimator animator) {
          mCurrentRotationAngle = (Float) animator.getAnimatedValue();
          invalidate();
        }
      });
      mAnimator.setRepeatCount(ValueAnimator.INFINITE);
      mAnimator.setRepeatMode(ValueAnimator.RESTART);
      mAnimator.start();
    }
    
    @Override
    public void drawState(Canvas canvas){
      drawBackground(canvas);
      drawCircles(canvas);
    }
    
    public void cancel(){
      mAnimator.cancel();
    }
  }
  
  private class MergingState extends SplashState {
    
    public MergingState(){      
      ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
      animator.setDuration(mSplashDuration / 3);
      animator.setInterpolator(new OvershootInterpolator(6F));
      animator.addUpdateListener(new AnimatorUpdateListener(){
        @Override
        public void onAnimationUpdate(ValueAnimator animator) {
          mCurrentRotationRadius = mRotationRadius * (Float) animator.getAnimatedValue();
          invalidate();
          
          if(mSplashListener != null){
            mSplashListener.onUpdate((float) animator.getCurrentPlayTime() / animator.getDuration() / 3);
          }
        }
      });
      animator.addListener(new AnimatorListenerAdapter(){
        @Override
        public void onAnimationStart(Animator animator){
          if(mSplashListener != null){
            mSplashListener.onStart();
          }
        }
        
        @Override
        public void onAnimationEnd(Animator animator){
          mState = new SingularityState();
        }
      });
      animator.reverse();
    }

    @Override
    public void drawState(Canvas canvas){
      drawBackground(canvas);
      drawCircles(canvas);
    }
  }
  
  private class SingularityState extends SplashState {
    
    public SingularityState(){      
      ValueAnimator animator = ValueAnimator.ofFloat(0, mCircleRadius);
      animator.setDuration(mSplashDuration / 3);
      animator.setInterpolator(new OvershootInterpolator(6F));
      animator.addUpdateListener(new AnimatorUpdateListener(){
        @Override
        public void onAnimationUpdate(ValueAnimator animator) {
          mCurrentSingleCircleRadius = (Float) animator.getAnimatedValue();
          invalidate();
          
          if(mSplashListener != null){
            mSplashListener.onUpdate(1F/3 + (float) animator.getCurrentPlayTime() / animator.getDuration() / 3);
          }
        }
      });
      animator.addListener(new AnimatorListenerAdapter(){
        @Override
        public void onAnimationEnd(Animator animator){
          mState = new ExpandingState();
        }
      });
      animator.reverse();
    }
    
    @Override
    public void drawState(Canvas canvas){
      drawBackground(canvas);
      drawSingleCircle(canvas);
    }
  }
  
  private class ExpandingState extends SplashState {
    
    public ExpandingState(){      
      ValueAnimator animator = ValueAnimator.ofFloat(0, mDiagonalDist);
      animator.setDuration(mSplashDuration / 3);
      animator.setInterpolator(new DecelerateInterpolator());
      animator.addUpdateListener(new AnimatorUpdateListener(){
        @Override
        public void onAnimationUpdate(ValueAnimator animator) {
          mHoleRadius = (Float) animator.getAnimatedValue();
          invalidate();
          
          if(mSplashListener != null){
            mSplashListener.onUpdate(2F/3 + (float) animator.getCurrentPlayTime() / animator.getDuration() / 3);
          }
        }
      });
      animator.addListener(new AnimatorListenerAdapter(){
        @Override
        public void onAnimationEnd(Animator animator){
          removeFromParentIfNecessary();
          
          if(mSplashListener != null){
            mSplashListener.onEnd();
          }
        }
      });
      animator.start();
    }
    
    @Override
    public void drawState(Canvas canvas){
      drawBackground(canvas);
    }
  }
}

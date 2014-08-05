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
   * Context constructor
   * @param context
   */
  public SplashView(Context context){
    super(context);
    initialize();
  }

  /**
   * Context and attributes constructor
   * @param context
   * @param attrs
   */
  public SplashView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize();
    setupAttributes(attrs);
  }

  /**
   * Context, attributes, and style constructor
   * @param context
   * @param attrs
   */
  public SplashView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
    setupAttributes(attrs);
  }
  
  /** define some default values **/
  public static final boolean DEFAULT_REMOVE_FROM_PARENT_ON_END = true;
  public static final int DEFAULT_ROTATION_RADIUS = 90; // do not leave this dimension as default since it is in px
  public static final int DEFAULT_CIRCLE_RADIUS = 18; // do not leave this dimension as default since it is in px
  public static final int DEFAULT_SPLASH_BG_COLOR = Color.WHITE;
  public static final int DEFAULT_SINGLE_CIRCLE_COLOR = Color.BLACK;
  public static final int DEFAULT_ROTATION_DURATION = 1200; // ms
  public static final int DEFAULT_SPLASH_DURATION = 1200; // ms
  
  /** some adjustable parameters **/
  private boolean mRemoveFromParentOnEnd = true; // a flag for removing the view from its parent once the animation is over
  private float mRotationRadius = DEFAULT_ROTATION_RADIUS; // the radius of the large circle
  private float mCircleRadius = DEFAULT_CIRCLE_RADIUS; // the radius of each individual small circle
  private int[] mCircleColors; // the color list of the circles, no default is provided here
  private long mRotationDuration = DEFAULT_ROTATION_DURATION; // the duration, in ms, for one complete rotation of the circles
  private long mSplashDuration = DEFAULT_SPLASH_DURATION; // the duration, in ms, for the splash animation to go away
  private int mSingleCircleColor = DEFAULT_SINGLE_CIRCLE_COLOR; // the color of the single circle left in the middle of the splash
  private int mSplashBgColor; // the color of the background, the default is set in initialize()
  private ISplashListener mSplashListener; // reference to the listener for the splash events
  
  /** some parameters to keep the current draw state, these will be changed by animations **/
  private float mHoleRadius = 0F;
  private float mCurrentRotationAngle = 0F;
  private float mCurrentRotationRadius;
  private float mCurrentSingleCircleRadius;
  
  // use state pattern for switching between animations more easily
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
          // TypedArray does not provide a method for obtaining integer arrays so using resources instead
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
   * Initialized the view properties. Not much is done in this method since most variables already have set defaults
   */
  private void initialize(){
    // make the background transparent so that the view does not automatically draw any unwanted colors
    setBackgroundColor(Color.TRANSPARENT);
    
    // we need anti aliasing here, otherwise the circles will look bad
    mPaint.setAntiAlias(true);
    
    // background paint needs to be a stroke paint in order to draw a transparent hole without using image resources
    mPaintBackground.setStyle(Paint.Style.STROKE);
    mPaintBackground.setAntiAlias(true);
    
    // set background color using method so that the paint object gets the color set as well
    // if attributes set a background color, this method will be called again with a new color
    setSplashBackgroundColor(DEFAULT_SPLASH_BG_COLOR);
  }
  
  /**
   * Setter for the radius of each individual circle. Units in px
   * @param circleRadius
   */
  public void setCircleRadius(float circleRadius){
    mCircleRadius = circleRadius;
  }
  
  /**
   * Setter for the radius of the large rotation circle. Units in px
   * @param rotationRadius
   */
  public void setRotationRadius(float rotationRadius){
    mRotationRadius = rotationRadius;
  }
  
  /**
   * Setter for the duration of the circles to complete one full rotation. Units in ms
   * @param duration
   */
  public void setRotationDuration(long duration){
    mRotationDuration = duration;
  }
  
  /**
   * Setter for the background color. Do not use setBackgroundColor otherwise the view will not draw a transparent hole
   * @param bgColor
   */
  public void setSplashBackgroundColor(int bgColor){
    mSplashBgColor = bgColor;
    mPaintBackground.setColor(mSplashBgColor);
  }
  
  /**
   * Setter for the color of the circle to be drawn at the end. This method might be removed in the future with the last circle color used for this value instead
   * @param circleColor
   */
  public void setSingleCircleColor(int circleColor){
    mSingleCircleColor = circleColor;
  }
  
  /**
   * Setter for the duration of the splash animation to take place. The animation has 3 parts, so this duration will be divided to 3 for each animation
   * @param duration
   */
  public void setSplashDuration(long duration){
    mSplashDuration = duration;
  }
  
  /**
   * Setter for the colors of the rotating circles. If the given integers are not actual colors, no circles will be produced.
   * @param circleColors
   */
  public void setCircleColors(int[] circleColors){
    mCircleColors = circleColors;
  }
  
  /**
   * Setter for the flag to remove or keep the view after the animation is over. This is set to true by default. The view must be inside a ViewManager
   * (or ViewParent) for this to work. Otherwise, the view will not be removed and a warning log will be produced.
   * @param shouldRemove
   */
  public void setRemoveFromParentOnEnd(boolean shouldRemove){
    mRemoveFromParentOnEnd = shouldRemove;
  }
  
  /**
   * Starts the splash animation. If a listener is provided it will notify the listener on animation events
   * @param listener
   */
  public void splashAndDisappear(final ISplashListener listener){
    mSplashListener = listener;
    
    if(mState != null && mState instanceof RotationState){
      RotationState rotationState = (RotationState) mState;
      rotationState.cancel();
    }
    
    // post this to the view so that the animation does not immediately try to start in case the UI is busy
    post(new Runnable(){
      @Override
      public void run(){
        mState = new MergingState();
      }
    });
  }
  
  /**
   * Override this method to cache some dimensional values, so that they don't have to be calculated every time
   */
  @Override
  protected void onSizeChanged (int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    
    mCenterX = w / 2F;
    mCenterY = h / 2F;
    mDiagonalDist = (float) Math.sqrt(w * w + h * h) / 2;
  }
  
  /**
   * Called right before first onDraw takes place and sets the initial animation state. mState must be null at this point for this to work.
   * This is necessary for all parameters to be set correctly. Otherwise, for example, if the RotationState is constructed before animation duration is set, the
   * set value will not have an effect.
   */
  private void handleFirstDraw(){
    // since this is the first draw the state is rotation state
    mState = new RotationState();
    
    // start at 0 degrees value, RotationState will change this value
    mCurrentRotationAngle = 0F;
    // initially there is no hole, ExpandingState will change this value
    mHoleRadius = 0F;
    // initially rotation radius is at standard value, MergingState will change this value
    mCurrentRotationRadius = mRotationRadius;
    // initially single circle is at standard value, SingularityState will change this value
    mCurrentSingleCircleRadius = mCircleRadius;
  }
  
  /**
   * Draws the current state. The actual draw state is delegated to State class and draw helper methods.
   */
  @Override
  protected void onDraw(Canvas canvas){
    // if mState is null then this is the first call to draw
    if(mState == null){
      handleFirstDraw();
    }
    
    // check to make sure circle colors are set, otherwise make sure nothing gets drawn in the rotation state
    if(mCircleColors == null){
      mCircleColors = new int[0];
    }
    
    // delegate draw to state
    mState.drawState(canvas);
  }
  
  /**
   * Draws the background either as a solid color or with a transparent hole in the middle
   * @param canvas
   */
  private void drawBackground(Canvas canvas){
    // check if there will be a hole
    if(mHoleRadius > 0F){
      // the way transparent circle is drawn is a little tricky. Instead of cutting a circle out of a rectangle
      // a hollow circle with very thick walls is drawn instead. The thickness of the walls is determined by the
      // stroke width of the paint. The circle radius and stroke with are calculated in order to draw the smallest
      // possible circle that will look like a rectangle with a hole cut out of it in the given view
      float strokeWidth = mDiagonalDist - mHoleRadius;
      float circleRadius = mHoleRadius + strokeWidth / 2;
      
      mPaintBackground.setStrokeWidth(strokeWidth);
      canvas.drawCircle(mCenterX, mCenterY, circleRadius, mPaintBackground);
    } else {
      // there is no hole so use the simplest method for drawing the background
      canvas.drawColor(mSplashBgColor);
    }
  }
  
  /**
   * Draws the given colored small circles around a large circle at a certain radius and angle.
   * @param canvas
   */
  private void drawCircles(Canvas canvas){
    // number of circles provided, this value could be cached in setCircleColors but does not really hurt much
    int numCircles = mCircleColors.length;
    // calculate the angle between each circle, angles are in radians
    float rotationAngle = (float) (2 * Math.PI / numCircles);
    for(int i=0; i<numCircles; ++i){
      // calculate the circle angle using the color position and angle offset
      double angle = mCurrentRotationAngle + (i * rotationAngle);
      // convert the coordinates into cartesian coordinates using simple trigonometry
      double circleX = mCenterX + mCurrentRotationRadius * Math.sin(angle);
      double circleY = mCenterY - mCurrentRotationRadius * Math.cos(angle);
      
      // set the paint color and draw the circle
      mPaint.setColor(mCircleColors[i]);
      canvas.drawCircle((float) circleX, (float) circleY, mCircleRadius, mPaint);
    }
  }
  
  /**
   * Draws a single circle in the middle of the screen with the given color and radius
   * @param canvas
   */
  private void drawSingleCircle(Canvas canvas){
    mPaint.setColor(mSingleCircleColor);
    canvas.drawCircle(mCenterX, mCenterY, mCurrentSingleCircleRadius, mPaint);
  }
  
  /**
   * Checks preferences and removes the view from its parent view if it can. Generates a DEBUG log message if unsuccessful
   */
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
  
  /**
   * An abstract class for taking care of the current animation and draw state
   * @author yildizkabaran
   *
   */
  private abstract class SplashState {
    public abstract void drawState(Canvas canvas);
  }
  
  /**
   * A state that contains an infinitely looping animator for repeated rotation.
   * @author yildizkabaran
   *
   */
  private class RotationState extends SplashState {
    private ValueAnimator mAnimator;
    
    /**
     * The constructor takes care of creating, setting up, and starting the animator
     */
    public RotationState(){
      // make a new animator that will go from 0 to 2PI
      mAnimator = ValueAnimator.ofFloat(0, (float) (Math.PI * 2));
      // set the requested duration, if the setRotationDuration method is called after this is done, then it will have no effect
      mAnimator.setDuration(mRotationDuration);
      // use a LinearInterpolator to make the animation smooth
      mAnimator.setInterpolator(new LinearInterpolator());
      // add an update listener for updating the necessary values
      mAnimator.addUpdateListener(new AnimatorUpdateListener(){
        @Override
        public void onAnimationUpdate(ValueAnimator animator) {
          // put the animated value into mCurrentRotationAngle
          mCurrentRotationAngle = (Float) animator.getAnimatedValue();
          // invalidate the view so that it draws itself again
          invalidate();
        }
      });
      // make the animation loop infinitely
      mAnimator.setRepeatCount(ValueAnimator.INFINITE);
      // make the animation restart from 0 when done
      mAnimator.setRepeatMode(ValueAnimator.RESTART);
      // start the animation
      mAnimator.start();
    }
    
    /**
     * Rotation state needs a background and all circles to be drawn
     */
    @Override
    public void drawState(Canvas canvas){
      drawBackground(canvas);
      drawCircles(canvas);
    }
    
    /**
     * The animator needs to be canceled on state change, otherwise the rotation angle will keep changing and the view will leak
     * even after it is destroyed. This method takes care of that, but is not a very nice way of doing so since it requires typecasting
     * in the splashAndDisappear method
     */
    public void cancel(){
      mAnimator.cancel();
      mAnimator = null;
    }
  }
  
  /**
   * In this state an animator is used to make the circles bounce back a little and then fall into the center of the view.
   * This is the first state of the splash animation
   * @author yildizkabaran
   *
   */
  private class MergingState extends SplashState {
    
    /**
     * The constructor takes care of creating, setting up, and starting the animator
     */
    public MergingState(){
      // Make an animator from 0 (center) to rotation radius, the animator will be used in reverse
      ValueAnimator animator = ValueAnimator.ofFloat(0, mRotationRadius);
      // set the duration to a third of the total duration
      animator.setDuration(mSplashDuration / 3);
      // use an overshoot interpolator to make the circles bounce out before falling into 0
      animator.setInterpolator(new OvershootInterpolator(6F));
      // add an update listener to update draw
      animator.addUpdateListener(new AnimatorUpdateListener(){
        @Override
        public void onAnimationUpdate(ValueAnimator animator) {
          // get the animation value into current rotation radius for the circles to be drawn at
          mCurrentRotationRadius = (Float) animator.getAnimatedValue();
          // invalidate the view to force draw
          invalidate();
          
          // if we have a listener, then update it during the first third of the animation
          // this is bad practice since adding another animation state will require this code to be changed
          if(mSplashListener != null){
            mSplashListener.onUpdate((float) animator.getCurrentPlayTime() / animator.getDuration() / 3);
          }
        }
      });
      // we need some animation state listeners
      animator.addListener(new AnimatorListenerAdapter(){
        @Override
        public void onAnimationStart(Animator animator){
          // inform the listener of splash start, since this is the first splash state
          if(mSplashListener != null){
            mSplashListener.onStart();
          }
        }
        
        @Override
        public void onAnimationEnd(Animator animator){
          // change the state to the next splash state in line
          mState = new SingularityState();
        }
      });
      // run the animation in reverse to get the bounce out then back in effect
      animator.reverse();
    }

    /**
     * Merging requires the background and all circles to be drawn
     */
    @Override
    public void drawState(Canvas canvas){
      drawBackground(canvas);
      drawCircles(canvas);
    }
  }
  
  /**
   * This state is used to make the only circle visible at this time get a little larger then disappear into a single point
   * @author yildizkabaran
   *
   */
  private class SingularityState extends SplashState {
    
    /**
     * The constructor takes care of creating, setting up, and starting the animator
     */
    public SingularityState(){
      // get a value animator from 0 to the radius of each circle, the animator will be used in reverse
      ValueAnimator animator = ValueAnimator.ofFloat(0, mCircleRadius);
      // set the duration to a third of the total duration
      animator.setDuration(mSplashDuration / 3);
      // use an overshoot interpolator to make the circles bounce out before falling into 0
      animator.setInterpolator(new OvershootInterpolator(6F));
      // add an update listener to update draw
      animator.addUpdateListener(new AnimatorUpdateListener(){
        @Override
        public void onAnimationUpdate(ValueAnimator animator) {
          // get the animation value into current single circle radius
          mCurrentSingleCircleRadius = (Float) animator.getAnimatedValue();
          // invalidate the view to force draw
          invalidate();
          
          // if we have a listener, then update it during the second third of the animation
          // this is bad practice since adding another animation state will require this code to be changed
          if(mSplashListener != null){
            mSplashListener.onUpdate(1F/3 + (float) animator.getCurrentPlayTime() / animator.getDuration() / 3);
          }
        }
      });
      animator.addListener(new AnimatorListenerAdapter(){
        @Override
        public void onAnimationEnd(Animator animator){
          // change the state to the next splash state in line
          mState = new ExpandingState();
        }
      });
      // run the animation in reverse to get the enlarge then disappear
      animator.reverse();
    }
    
    /**
     * SinglularityState requires a background and a single circle to be drawn
     */
    @Override
    public void drawState(Canvas canvas){
      drawBackground(canvas);
      drawSingleCircle(canvas);
    }
  }
  
  /**
   * This state uses an animator to draw an increasingly larger transparent hole in the view
   * @author yildizkabaran
   *
   */
  private class ExpandingState extends SplashState {
    
    /**
     * The constructor takes care of creating, setting up, and starting the animator
     */
    public ExpandingState(){   
      // get an animator from 0 to the half diagonal distance of the view
      ValueAnimator animator = ValueAnimator.ofFloat(0, mDiagonalDist);
      // set the duration to a third of the total duration
      animator.setDuration(mSplashDuration / 3);
      // use a decelerate interpolator to give the effect that the transparent hole went into a bang
      animator.setInterpolator(new DecelerateInterpolator());
      // add an update listener to update draw
      animator.addUpdateListener(new AnimatorUpdateListener(){
        @Override
        public void onAnimationUpdate(ValueAnimator animator) {
          // get the animated value into the radius of the transparent hole
          mHoleRadius = (Float) animator.getAnimatedValue();
          // invalidate the view to force draw
          invalidate();

          // if we have a listener, then update it during the last third of the animation
          // this is bad practice since adding another animation state will require this code to be changed
          if(mSplashListener != null){
            mSplashListener.onUpdate(2F/3 + (float) animator.getCurrentPlayTime() / animator.getDuration() / 3);
          }
        }
      });
      animator.addListener(new AnimatorListenerAdapter(){
        @Override
        public void onAnimationEnd(Animator animator){
          // the splash is over so remove from parent if needed
          removeFromParentIfNecessary();
          
          // notify the listener that we are done
          if(mSplashListener != null){
            mSplashListener.onEnd();
          }
        }
      });
      // start the animation in forward direction
      animator.start();
    }
    
    /**
     * The ExpandingState only needs a background
     */
    @Override
    public void drawState(Canvas canvas){
      drawBackground(canvas);
    }
  }
}

package com.yildizkabaran.newsdigestsplash.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewManager;
import android.view.ViewParent;

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
    setupAttributes(attrs);
    initialize();
  }

  /**
   * This constructor is redirected to the Context constructor.
   * @param context
   * @param attrs
   */
  public SplashView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setupAttributes(attrs);
    initialize();
  }
  
  public static final boolean DEFAULT_REMOVE_FROM_PARENT_ON_END = true;
  
  private boolean mRemoveFromParentOnEnd = true; // a flag for removing the view from its parent once the animation is over
  
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
      }
    }
    a.recycle();
  }
  
  /**
   * Initialized the view properties. No much is done in this method since most variables already have set defaults
   */
  private void initialize(){
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
    // create an animator from scale 1 to max
    final ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
    
    // add an update listener so that we draw the view on each update
    animator.addUpdateListener(new AnimatorUpdateListener() {
      @SuppressLint("NewApi")
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        // invalidate the view so that it gets redraw if it needs to be
        invalidate();
        
        // notify the listener if set
        // for some reason this animation can run beyond 100%
        if(listener != null){
          listener.onUpdate((float) animation.getCurrentPlayTime() / animation.getDuration());
        }
      }
    });
    
    // add a listener for the general animation events, use the AnimatorListenerAdapter so that we don't clutter the code
    animator.addListener(new AnimatorListenerAdapter(){
      @Override
      public void onAnimationStart(Animator animation){
        // notify the listener of animation start (if listener is set)
        if(listener != null){
          listener.onStart();
        }
      }
      
      @Override
      public void onAnimationEnd(Animator animation){
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
        
        // notify the listener of animation end (if listener is set)
        if(listener != null){
          listener.onEnd();
        }
      }
    });
    
    // start the animation using post so that the animation does not start if the view is not in foreground
    post(new Runnable(){
      @Override
      public void run(){
        // start the animation in reverse to get the desired effect from the interpolator
        animator.start();
      }
    });
  }
  
  
  @Override
  protected void onDraw(Canvas canvas){
    
  }
}

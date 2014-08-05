package com.yildizkabaran.newsdigestsplash;

import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.yildizkabaran.newsdigestsplash.view.ContentView;
import com.yildizkabaran.newsdigestsplash.view.SplashView;
import com.yildizkabaran.newsdigestsplash.view.SplashView.ISplashListener;

public class MainActivity extends Activity {

  private static final String TAG = "MainActivity";
  private static final boolean DO_XML = false;
  
  private ViewGroup mMainView;
  private SplashView mSplashView;
  private View mContentView;
  private Handler mHandler = new Handler();
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // change the DO_XML variable to switch between code and xml
    if(DO_XML){
      // inflate the view from XML and then get a reference to it
      setContentView(R.layout.activity_main);
      mMainView = (ViewGroup) findViewById(R.id.main_view);
      mSplashView = (SplashView) findViewById(R.id.splash_view);
    } else {
      // create the main view
      mMainView = new FrameLayout(getApplicationContext());
      
      // create the splash view
      mSplashView = new SplashView(getApplicationContext());
      mSplashView.setRemoveFromParentOnEnd(true); // remove the SplashView from MainView once animation is completed
      mSplashView.setSplashBackgroundColor(getResources().getColor(R.color.splash_bg)); // the background color of the view
      mSplashView.setRotationRadius(getResources().getDimensionPixelOffset(R.dimen.splash_rotation_radius)); // radius of the big circle that the little circles will rotate on
      mSplashView.setCircleRadius(getResources().getDimensionPixelSize(R.dimen.splash_circle_radius)); // radius of each circle
      mSplashView.setRotationDuration(getResources().getInteger(R.integer.splash_rotation_duration)); // time for one rotation to be completed by the small circles
      mSplashView.setSplashDuration(getResources().getInteger(R.integer.splash_duration)); // total time taken for the circles to merge together and disappear
      mSplashView.setCircleColors(getResources().getIntArray(R.array.splash_circle_colors)); // the colors of each circle in order
      
      // add splash view to the parent view
      mMainView.addView(mSplashView);
      setContentView(mMainView);
    }
    
    // pretend like we are loading data
    startLoadingData();
  }
  
  private void startLoadingData(){
    // finish "loading data" in a random time between 1 and 3 seconds
    Random random = new Random();
    mHandler.postDelayed(new Runnable(){
      @Override
      public void run(){
        onLoadingDataEnded();
      }
    }, 1000 + random.nextInt(2000));
  }
  
  private void onLoadingDataEnded(){
    Context context = getApplicationContext();
    // now that our data is loaded we can initialize the content view
    mContentView = new ContentView(context);
    // add the content view to the background
    mMainView.addView(mContentView, 0);
    
    // start the splash animation
    mSplashView.splashAndDisappear(new ISplashListener(){
      @Override
      public void onStart(){
        // log the animation start event
        if(BuildConfig.DEBUG){
          Log.d(TAG, "splash started");
        }
      }
      
      @Override
      public void onUpdate(float completionFraction){
        // log animation update events
        if(BuildConfig.DEBUG){
          Log.d(TAG, "splash at " + String.format("%.2f", (completionFraction * 100)) + "%");
        }
      }
      
      @Override
      public void onEnd(){
        // log the animation end event
        if(BuildConfig.DEBUG){
          Log.d(TAG, "splash ended");
        }
        // free the view so that it turns into garbage
        mSplashView = null;
      }
    });
  }
}

# Yahoo! News Digest Splash for Android
by Yildiz Kabaran

A simple replication of Yahoo! News Digest's splash animation as seen in Android and iOS apps. The animation can be used with any number, and any colored circles, with any background, at any speed.

![Preview Image](http://i.imgbox.com/2ujiTEyh.gif)

## Installation

Simply copy the SplashView.java and attrs.xml files into your project and use it like you would use any other custom view.

## Usage

You can create in code:
```
// create and customize the view
SplashView splashView = new SplashView(context);
// remove the SplashView from MainView once animation is completed
splashView.setRemoveFromParentOnEnd(true);
// the background color of the view
splashView.setSplashBackgroundColor(getResources().getColor(R.color.splash_bg));
// radius of the big circle that the little circles will rotate on
splashView.setRotationRadius(getResources().getDimensionPixelOffset(R.dimen.splash_rotation_radius));
// radius of each circle
splashView.setCircleRadius(getResources().getDimensionPixelSize(R.dimen.splash_circle_radius));
// time for one rotation to be completed by the small circles
splashView.setRotationDuration(getResources().getInteger(R.integer.splash_rotation_duration));
// total time taken for the circles to merge together and disappear
splashView.setSplashDuration(getResources().getInteger(R.integer.splash_duration));
// the colors of each circle, in order
splashView.setCircleColors(getResources().getIntArray(R.array.splash_circle_colors));
```

or in XML:
```
<com.yildizkabaran.newsdigestsplash.view.SplashView 
	xmlns:app="http://schemas.android.com/apk/res/com.yildizkabaran.newsdigestsplash"
	android:id="@+id/splash_view"
	android:layout_width="match_parent"
	android:layout_height="match_parent"
	app:removeFromParentOnEnd="true"
	app:circleRadius="@dimen/splash_circle_radius"
	app:rotationRadius="@dimen/splash_rotation_radius"
	app:rotationDuration="@integer/splash_rotation_duration"
	app:splashDuration="@integer/splash_duration"
	app:splashBackgroundColor="@color/splash_bg"
	app:circleColors="@array/splash_circle_colors" />
```

then to run the animation, simply call:
```
// run the animation and listen to the animation events (listener can be left as null)
splashView.splashAndDisappear(new ISplashListener(){
	@Override
	public void onStart(){

	}
	
	@Override
	public void onUpdate(float completionFraction){

	}

	@Override
	public void onEnd(){

	}
});
```

In the sample code a boolean DO_XML variable is provided to demonstrate both methods.

Please feel free to ask for any fixes/customizations/additions to this view.

## Notes

- The view has only been tested on HTC One running Android 4.4.2, and therefore needs to be tested on devices with different versions and screen resolutions.

## Copyright and License

Feel free to use the code in any way you wish.

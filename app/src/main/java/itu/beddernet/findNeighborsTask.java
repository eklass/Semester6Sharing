// http://stackoverflow.com/questions/18021148/display-a-loading-overlay-on-android-screen
package itu.beddernet;

import itu.beddernet.approuter.IBeddernetService;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;

public class findNeighborsTask extends AsyncTask<Object, Object, Object>{
	private Handler derHandler;
	IBeddernetService service;
	private String TAG = itu.beddernet.common.BeddernetInfo.TAG;
	FrameLayout progressBarHolder;
	AlphaAnimation inAnimation;
	AlphaAnimation outAnimation;

	
	public findNeighborsTask(IBeddernetService service, Handler derHandler, FrameLayout progressBarHolder, AlphaAnimation inAnimation, AlphaAnimation outAnimation){
		this.service = service;
		this.derHandler=derHandler;
		this.progressBarHolder=progressBarHolder;
		this.inAnimation=inAnimation;
		this.outAnimation=outAnimation;
	}

	@Override
	protected Object doInBackground(Object... params) {
		try {
			service.findNeighbors();
			Log.e(TAG,"Suche ist fertig");
			//refreshDeviceList();
		} catch (RemoteException e) {
			Log.e(TAG, "Could not request device discovery");
		}
		return null;
	}

	@Override
	protected void onPostExecute(Object o) {
		//super.onPostExecute(o);
		super.onPostExecute(o);
		outAnimation = new AlphaAnimation(1f, 0f);
		outAnimation.setDuration(200);
		progressBarHolder.setAnimation(outAnimation);
		progressBarHolder.setVisibility(View.GONE);
		derHandler.sendEmptyMessage(0);
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		inAnimation = new AlphaAnimation(0f, 1f);
		inAnimation.setDuration(200);
		progressBarHolder.setAnimation(inAnimation);
		progressBarHolder.setVisibility(View.VISIBLE);
	}
}

package itu.beddernet;

import itu.beddernet.approuter.IBeddernetService;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.widget.ArrayAdapter;

public class findNeighborsTask extends AsyncTask<Object, Object, Object>{
	private Handler derHandler;
	IBeddernetService service;
	private String TAG = itu.beddernet.common.BeddernetInfo.TAG;
	
	public findNeighborsTask(IBeddernetService service, Handler derHandler){
		this.service = service;
		this.derHandler=derHandler;
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
		derHandler.sendEmptyMessage(0);
	}
}

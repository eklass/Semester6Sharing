package itu.beddernet.recordSound;

import android.util.Log;

/**
 * Created by erichklassen on 30.06.16.
 */
public class RecordLog {
    private static final String APP_TAG = "AudioRecorder";

    public static int logString(String message){
        return Log.i(APP_TAG,message);
    }
}

//Sammlung an Links: 	    http://stackoverflow.com/questions/23945171/android-app-bluetooth-connection-without-pairing
//Handler:				    http://stackoverflow.com/questions/13079645/android-how-to-wait-asynctask-to-finish-in-mainthread
//Disabling Keyboard: 	    http://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
// Now working on: 		    http://stackoverflow.com/questions/22573301/how-to-pass-a-handler-from-activity-to-service
// 					    	http://stackoverflow.com/questions/6369287/accessing-ui-thread-handler-from-a-service
//                          http://stackoverflow.com/questions/14695537/android-update-activity-ui-from-service
//VoiceButton               http://stackoverflow.com/questions/28711549/how-to-create-a-whatsapp-like-recording-button-with-slide-to-cancel
//(interessant Voice)       http://stackoverflow.com/questions/11116051/how-can-i-record-voice-in-android-as-long-as-hold-a-button
//Workaround CustomListView http://stackoverflow.com/questions/10869197/android-widget-linearlayout-cannot-be-cast-to-android-widget-textview
//CustomListView            http://www.androidinterview.com/android-custom-listview-with-image-and-text-using-arrayadapter/
//PapierFlieger             https://image.freepik.com/vektoren-kostenlos/bunte-papierflieger-vektor_23-2147498226.jpg



package itu.beddernet;

import itu.beddernet.approuter.IBeddernetService;
import itu.beddernet.approuter.IBeddernetServiceCallback;
import itu.beddernet.common.BeddernetInfo;
import itu.beddernet.common.NetworkAddress;
import itu.beddernet.datalink.bluetooth.BluetoothDatalink;
import itu.beddernet.recordSound.RecordLog;
import itu.beddernet.recordSound.recordActivity;
import itu.beddernet.router.dsdv.info.ConfigInfo;
import audio.ui.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources.NotFoundException;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

// TODO: Es bleiben jedoch insgemein noch Probleme/Dinge die erledigt werden müssen:
// TODO:1. Automatisches installieren muss/kann noch implementiert werden


public class BeddernetConsole extends Activity implements ServiceConnection {

    String[] actualDeviceList;
    private List<String> mItems;
    ImageButton oneClickVoicePaperButton;

    /*VOICE-BUTTON VARIABLE*/
    private TextView recordTimeText;
    private ImageButton audioSendButton;
    private View recordPanel;
    private View slideText;
    private float startedDraggingX = -1;
    private float distCanMove = dp(80);
    private long startTime = 0L;
    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedTime = 0L;
    private Timer timer;
    /*Voice-Variable-End*/

    static final public String BEDDERNETCONSOLE_DO_REFRESHING = "refreshDevices";
    static final public String BEDDERNETCONSOLE_DO_SEARCHING = "searchForDevices";
    static final public String COPA_RESULT = "com.controlj.copame.backend.COPAService.REQUEST_PROCESSED";
    static final public String COPA_MESSAGE = "com.controlj.copame.backend.COPAService.COPA_MSG";
    static final public String FIRST_COLOR = "#F781BE";
    static final public String SECOND_COLOR = "#BE81F7";
    static final public String THIRD_COLOR = "#819FF7";
    static final public String FOURTH_COLOR = "#81DAF5";
    static final public String FIFTH_COLOR = "#81F7BE";
    static final public String SIXTH_COLOR = "#9FF781";
    static final public String SEVENTH_COLOR = "#F5DA81";
    private static final int CONTEXT_MENU_SEND_MESSAGE = 1;
    private static final int CONTEXT_MENU_DISCONNECT = 2;
    private static final int CONTEXT_MENU_SEND_FILE = 3;
    private static final int CONTEXT_MENU_SEND_RTT = 4;
    private static final int CONTEXT_MENU_VIEW_SERVICES = 5;
    private static final int CONTEXT_MENU_SEND_FILE_DUPLEX = 6;
    private static final int CONTEXT_MENU_SEND_RECORDED_FILE = 7;
    private static final int MENU_DISCOVERY = 0;
    private static final int MENU_DISCOVERABLE = 1;
    private static final int MENU_BLUETOOTH_OFF = 2;
    private static final int MENU_BEDNET_OFF = 3;
    private static final int MENU_MANUAL_REFRESH = 4;
    private static final int MENU_SERVICES = 5;
    private static final int MENU_SHAREAPP = 6;
    private static final byte FILE_MESSAGE = 1;
    private static final byte TEXT_MESSAGE = 2;
    private static final byte FILE_END = 3;
    private static final byte RTT_MESSAGE = 4;
    private static final byte RTT_MESSAGE_REPLY = 5;
    private static final byte TEST_END = 6;
    private static final byte FILE_END_ACK = 7;
    private static final byte FILE_FRANSFER_REQUEST = 8;
    private static final int bufferSize = 5000;
    public static String applicationIdentifier = "BeddernetConsole";
    public static long applicationIdentifierHash = applicationIdentifier
            .hashCode();
    public boolean sendAudioFile = false;
    public ArrayAdapter<String> mDeviceArrayAdapter;
    public CustomAdapter testAdapter;
    public ArrayList<String> deviceColorList;
    public TextView outputTextView;
    String mFileNameToSend = Environment.getExternalStorageDirectory().getAbsolutePath();
    String mFileNameToPlay = Environment.getExternalStorageDirectory().getAbsolutePath();
    MediaPlayer mp = null;
    byte[] rttMessage = new byte[9];
    ServiceConnection sc = this;
    // Variables for progressbar
    FrameLayout progressBarHolder;
    AlphaAnimation inAnimation;
    AlphaAnimation outAnimation;
    private EditText inputText;
    private long RTTStartTime = 0;
    private long RTTEndTime = 0;
    private String TAG = BeddernetInfo.TAG;
    private IBeddernetService mBeddernetService;

    /*
    * Method to display the starttime for VoiceSlide
    */
    public void startrecord() {
        // TODO Auto-generated method stub
        startTime = SystemClock.uptimeMillis();
        timer = new Timer();
        MyTimerTask myTimer = new MyTimerTask();
        timer.schedule(myTimer, 1000, 1000);
        vibrate();
    }

    /*
    * Method to stop the time for VoiceSlide
    */
    public void stoprecord() {
        // TODO Auto-generated method stub
        if (timer != null) {
            timer.cancel();
        }
        if (recordTimeText.getText().toString().equals("00:00")) {
            return;
        }
        recordTimeText.setText("00:00");
        vibrate();
    }

    /*
    * A class which does the time work for voiceslide
    * */
    class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;
            final String hms = String.format(
                    "%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(updatedTime)
                            - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS
                            .toHours(updatedTime)),
                    TimeUnit.MILLISECONDS.toSeconds(updatedTime)
                            - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS
                            .toMinutes(updatedTime)));
            long lastsec = TimeUnit.MILLISECONDS.toSeconds(updatedTime)
                    - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS
                    .toMinutes(updatedTime));
            System.out.println(lastsec + " hms " + hms);
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    try {
                        if (recordTimeText != null)
                            recordTimeText.setText(hms);
                    } catch (Exception e) {
                        // TODO: handle exception
                    }

                }
            });
        }
    }

    public static int dp(float value) {
        return (int) Math.ceil(1 * value);
    }

    /*
    * A handler to call refreshDeviceList inside the hirarchie (f.e. call from DeviceVO)
    * */
    Handler myHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 0:
                    Log.e(TAG, "Ich wurde gerufen.Anscheinend ist der Thread fertig.");
                    refreshDeviceList();
                    // calling to this function from other pleaces
                    // The notice call method of doing things
                    break;
                case 1:
                    Log.e(TAG,"Ich wurde von CustomAdapter aufgerufen");
                default:
                    break;
            }
        }
    };
    private Activity activity;
    private ListView mDeviceView;
    @SuppressWarnings("unused")
    private String serviceConnectionStatus;
    private int filesPending;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    private BroadcastReceiver receiver;

    private OnClickListener buttonListnener = new OnClickListener() {
        public void onClick(View src) {
            switch (src.getId()) {
                /*case R.id.recVoice:
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    break;
*/
                case R.id.clrTxt:
                    outputTextView.setText("Ausgabe: der Textnachrichten\n");
                    break;

				/*case R.id.refDevice:
					outputTextView.append("Die Liste der verbundenen Geräte wurde aktualisiert\n");
					refreshDeviceList();
					outputTextView.setMovementMethod(new ScrollingMovementMethod());
					break;*/
            }

        }
    };

    /*
    * This method will be called, if someone clicks on a device to communicate
    * OnItemClicklistener nutzt den substring um auf die Adresse zu kommen (eigentlich clever)
    * Kleiner Workaround wegen CustomListView war von Nöten
    * */
    private OnItemClickListener mListListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            LinearLayout ll = (LinearLayout) v; // get the parent layout view
            TextView tv = (TextView) ll.findViewById(R.id.Itemname); // get the child text view
            //String address = ((TextView) v).getText().toString();
            String address = tv.getText().toString();
            if (address != null) {
                address = address.substring(0, 17); // Ugly removes the status
                sendMessage(address);
                //after sendind Message, clear inputText
                inputText.setText("");
                refreshDeviceList();
            }
        }

    };
    private CompoundButton.OnCheckedChangeListener checkBoxListener = new CompoundButton.OnCheckedChangeListener() {

        public void onCheckedChanged(CompoundButton buttonView,
                                     boolean isChecked) {
            if (isChecked) {
                try {
                    mBeddernetService.startMaintainer();
                } catch (Exception e) {
                    Log.e(TAG, "Could not start maintainer", e);
                }
            } else {
                try {
                    mBeddernetService.stopMaintainer();
                } catch (Exception e) {
                    Log.e(TAG, "Could not start maintainer", e);
                }
            }
        }
    };
    private IBeddernetServiceCallback mCallback = new IBeddernetServiceCallback.Stub() {

        private FileOutputStream fileOut;
        private BufferedOutputStream out;
        private boolean transferring = false;
        private long startTime;
        private long endTime;
        private double transferTime;
        private long transferedBytes = 0;
        private double kBitsPerSek = 0;
        // private FileOutputStream fileOutput;
        private long RTTTime;

        /**
         * This is called by the remote service regularly to tell us about new
         * values. Note that IPC calls are dispatched through a thread pool
         * running in each process, so the code executing here will NOT be
         * running in our main thread like most other things -- so, to update
         * the UI, we need to use a Handler to hop over there.
         */

        public long getApplicationIdentifierHash() throws RemoteException {
            Log.d(TAG, "Token sent to server :" + applicationIdentifierHash);
            return applicationIdentifierHash;
        }

        public void update(String senderAddress, byte[] message)
                throws RemoteException {
            // if some send someoneelse a message, that the device list will be updated (to see, who send a message)
            refreshDeviceList();
            byte type = message[0];
            // Log.d(TAG, "Message received at BedderTestPlatform");
            switch (type) {

                case FILE_MESSAGE:
                    if (!transferring) {
                        // outputTextView.append("Receiving file from: "+
                        // senderAddress+ "\n");

                        startTime = System.currentTimeMillis();
                        transferring = true;
                        try {
                            //fileOut = openFileOutput("newRecord.wav", MODE_PRIVATE);
                            out = new BufferedOutputStream(new FileOutputStream(mFileNameToPlay));

                        } catch (Exception e) {
                            Log.e(TAG, "Error in writing to stream, closing outPut stream", e);
                        }

                    }
                    transferedBytes = transferedBytes + message.length;
                    try {
                        out.write(message, 1, message.length - 1);
                    } catch (IOException e1) {
                        Log.d(TAG, "Exception in BedderTestPlatform - out.write",
                                e1);
                    }
                    break;
                case RTT_MESSAGE:
                    // outputTextView.append("Received ping from: "+ senderAddress+
                    // "\n");
                    byte[] messageString = {RTT_MESSAGE_REPLY};
                    mBeddernetService.sendUnicast(senderAddress, null,
                            messageString, applicationIdentifier);
                    break;
                case RTT_MESSAGE_REPLY:
                    RTTEndTime = System.currentTimeMillis();
                    RTTTime = RTTEndTime - RTTStartTime;
                    Log.i(TAG, "RTT reply received, RTT time: " + RTTTime);
                    outputTextView.append("RTT reply received, RTT time: "
                            + RTTTime + "\n");

                case TEXT_MESSAGE:
                    refreshDeviceList();
                    vibrate();
                    String msg = new String(message, 1, message.length - 1);
                    String hexColorForDeviceText = getColorFromAddress(senderAddress);
                    String toSend = "<font color='" + hexColorForDeviceText + "'>" + getNameFromMacadress(senderAddress) + ": " + msg + "</font><br/>";
                    outputTextView.append(Html.fromHtml(toSend));
                    //outputTextView.append("Message received from: " + senderAddress
                    //		+ "Message text: " + msg + "\n");
                    Log.i(TAG, "Text message received: " + msg);
                    break;
                case FILE_END:
                    endTime = System.currentTimeMillis();
                    transferTime = endTime - startTime;
                    kBitsPerSek = (transferedBytes / (transferTime / 1000)) * 8;
                    String result = ("Transfer over: " + transferedBytes
                            + " bytes sent in : " + transferTime
                            + " milliseconds. kilobits per second: "
                            + (int) kBitsPerSek + "\n");
                    Log.i(TAG, result);
                    byte[] ackMessage = {FILE_END_ACK};
                    mBeddernetService.sendUnicast(senderAddress, null, ackMessage,
                            applicationIdentifier);
                    outputTextView.append(result);

                    try {
                        out.close();
                        //fileOut.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Could not close outputStreams", e);
                    }
                    // Clean up
                    transferring = false;
                    transferedBytes = 0;
                    //MediaPlayer mp = MediaPlayer.create(activity, R.raw.walterminion);
                    mp = new MediaPlayer();
                    try {

                        mp.setDataSource(mFileNameToPlay);
                        mp.prepare();
                    } catch (IOException e) {
                        Log.i(TAG, "Fehler beim setzen der DataSource[Methode-onCreate]");
                        e.printStackTrace();
                    }
                    outputTextView.append("Empfangene Datei wird abgespielt...\n");
                    mp.start();
                    while (mp.isPlaying()) {
                    }
                    mp.stop();
                    mp.release();
                    mp = null;
                    break;
                case FILE_END_ACK:
                    fileTransferComplete();
                    break;
                case FILE_FRANSFER_REQUEST:
                    sendFile(senderAddress);
                    break;
                default:
                    break;
            }
            outputTextView.setMovementMethod(new ScrollingMovementMethod());
        }

        public void updateWithSendersApplicationIdentifierHash(
                String senderAddress, long senderApplicationIdentifierHash,
                byte[] message) throws RemoteException {
            // Not implemented
            Log
                    .i(TAG,
                            "nonimplemented update method called, normal onne called instead");
            update(senderAddress, message);

        }
    };

    public void vibrate() {
        // TODO Auto-generated method stub
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(200);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onDestroy() {
        if (mBeddernetService != null) {
            try {
                mBeddernetService.unregisterCallback(mCallback,
                        applicationIdentifier);
            } catch (RemoteException e) {
                Log.e(TAG, "Console could't unregister callback", e);
            }
            unbindService(sc);
        }
        super.onDestroy();
    }

    protected void onResume() {
        Log.d(TAG, "resuming");
        if (mBeddernetService == null) {
            Log.d(TAG, "the service connection is null - rebooting");
            // onCreate(null);
            Intent bindIntent = new Intent(
                    "itu.beddernet.approuter.BeddernetService");
            bindIntent.setPackage("itu.beddernet.approuter");
            this.bindService(bindIntent, this, Context.BIND_AUTO_CREATE);
        }
        super.onResume();
    }

    /**
     * Called when the activity is first created.
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //mFileNameToPlay += "/audiorecordtest.3gp";

        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Intent bindIntent = new Intent("itu.beddernet.approuter.BeddernetService");

        this.bindService(bindIntent, this, Context.BIND_AUTO_CREATE);
        setContentView(R.layout.main);



        /*------ HERE COMES THE VOICEBUTTONFIX -----------*/
        final recordActivity recordSound = new recordActivity();
        recordPanel = findViewById(R.id.record_panel);
        recordTimeText = (TextView) findViewById(R.id.recording_time_text);
        slideText = findViewById(R.id.slideText);
        audioSendButton = (ImageButton) findViewById(R.id.chat_audio_send_button);
        TextView textView = (TextView) findViewById(R.id.slideToCancelTextView);
        textView.setText("SlideToCancel");
        audioSendButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) slideText
                            .getLayoutParams();
                    params.leftMargin = dp(30);
                    slideText.setLayoutParams(params);
                    ViewProxy.setAlpha(slideText, 1);
                    startedDraggingX = -1;

                    RecordLog.logString("Start Recording");
                    recordSound.startRecording();

                    startrecord();
                    audioSendButton.getParent()
                            .requestDisallowInterceptTouchEvent(true);
                    recordPanel.setVisibility(View.VISIBLE);
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP
                        || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                    startedDraggingX = -1;
                    RecordLog.logString("Stop Recording");
                    recordSound.stopRecording(true);
                    stoprecord();
                    // stopRecording(true);

                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                    float x = motionEvent.getX();
                    if (x < -distCanMove) {
                        // stopRecording(false);
                        //RecordLog.logString("Stop Recording");
                        recordSound.stopRecording(false);
                        stoprecord();
                        vibrate();


                    }
                    x = x + ViewProxy.getX(audioSendButton);
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) slideText
                            .getLayoutParams();
                    if (startedDraggingX != -1) {
                        float dist = (x - startedDraggingX);
                        params.leftMargin = dp(30) + (int) dist;
                        slideText.setLayoutParams(params);
                        float alpha = 1.0f + dist / distCanMove;
                        if (alpha > 1) {
                            alpha = 1;
                        } else if (alpha < 0) {
                            alpha = 0;
                        }
                        ViewProxy.setAlpha(slideText, alpha);
                    }
                    if (x <= ViewProxy.getX(slideText) + slideText.getWidth()
                            + dp(30)) {
                        if (startedDraggingX == -1) {
                            startedDraggingX = x;
                            distCanMove = (recordPanel.getMeasuredWidth()
                                    - slideText.getMeasuredWidth() - dp(48)) / 2.0f;
                            if (distCanMove <= 0) {
                                distCanMove = dp(80);
                            } else if (distCanMove > dp(80)) {
                                distCanMove = dp(80);
                            }
                        }
                    }
                    if (params.leftMargin > dp(30)) {
                        params.leftMargin = dp(30);
                        slideText.setLayoutParams(params);
                        ViewProxy.setAlpha(slideText, 1);
                        startedDraggingX = -1;
                    }
                }
                view.onTouchEvent(motionEvent);
                return true;
            }
        });

        /*----------------------HERE IS THE END OF VOICEBUTTONFIX--------------------*/


        Button clrTxtButton = (Button) findViewById(R.id.clrTxt);
        clrTxtButton.setOnClickListener(buttonListnener);
        //Button refDeviceButton = (Button) findViewById(R.id.refDevice);
        //refDeviceButton.setOnClickListener(buttonListnener);

        CheckBox maintainer = (CheckBox) findViewById(R.id.MaintainerBox);
        maintainer.setOnCheckedChangeListener(checkBoxListener);

        // Initialize the array adapter for the conversation thread
        //mDeviceArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mDeviceArrayAdapter = new ArrayAdapter<String>(
                this, R.layout.mylist,
                R.id.Itemname);

        mItems = new ArrayList<String>();
        testAdapter = new CustomAdapter(this,R.layout.mylist,mItems);

        mDeviceView = (ListView) findViewById(R.id.in);
        //mDeviceView.setAdapter(mDeviceArrayAdapter);
        mDeviceView.setAdapter(testAdapter);
        mDeviceView.setOnItemClickListener(mListListener);
        mDeviceView.setHapticFeedbackEnabled(true);
/*        this.setListAdapter(new ArrayAdapter<String>(
                this, R.layout.mylist,
                R.id.Itemname,itemname));
*/
        registerForContextMenu(mDeviceView);

        activity = this;

        outputTextView = (TextView) findViewById(R.id.outputTextView);

        mFileNameToSend += "/audiorecordtest.3gp";
        mFileNameToPlay += "/newStream.3gp";
        mp = new MediaPlayer();

        try {
            mp.setDataSource(mFileNameToPlay);
            mp.prepare();
        } catch (IOException e) {
            Log.i(TAG, "Fehler beim setzen der DataSource[Methode-onCreate]");
            e.printStackTrace();
        }
        progressBarHolder = (FrameLayout) findViewById(R.id.progressBarHolder);
        /*ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
		ObjectAnimator animation = ObjectAnimator.ofInt (progressBar, "progress", 0, 500); // see this max value coming back here, we animale towards that value
		animation.setDuration (100000); //in milliseconds
		animation.setInterpolator (new DecelerateInterpolator());
		animation.start ();
		*/

        //super.setContentView(R.layout.copa);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra(COPA_MESSAGE);
                switch (s){
                    // Normaly used in DeviceFinder and DeviceV0
                    case BEDDERNETCONSOLE_DO_REFRESHING:
                        refreshDeviceList();
                        break;
                    // Normaly used in Discovery Receiver
                    case BEDDERNETCONSOLE_DO_SEARCHING:
                        searchForDevices();
                        break;
                }
                // A Hashtag is only send from the CustomAdapter
                if (s.substring(0,1).equals("#")){
                    // now string s is the number the entry which was clicked
                    s=s.substring(1,2);
                    String addressToSendFile = actualDeviceList[Integer.parseInt(s)*2];
                    Log.e(TAG,"Nachricht von CustomAdapter!!");
                    sendAudioFile = true;
                    sendFile(addressToSendFile);
                    sendAudioFile = false;
                }

                Log.e(TAG, "Die Nachricht vom BroadcastReceiver ist: "+s);
                // do something here.
                // do something here.
            }
        };

        inputText = (EditText) findViewById(R.id.writeSomeText);
        deviceColorList = new ArrayList<String>();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    /* Creates the menu items */
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_MANUAL_REFRESH, 0, "Refresh device list");
        menu.add(0, MENU_DISCOVERY, 0, "Find devices");
        menu.add(0, MENU_SERVICES, 0, "View local services");
        menu.add(0, MENU_DISCOVERABLE, 0, "Discoverable");
        menu.add(0, MENU_BLUETOOTH_OFF, 0, "Bluetooth off");
        menu.add(0, MENU_BEDNET_OFF, 0, "Bednet off");
        menu.add(0, MENU_SHAREAPP, 0, "Share app");
        if(!BluetoothDatalink.getBluetoothDatalinkInstance().stillWaitingForBT){
            searchForDevices();
        }
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_MANUAL_REFRESH:
                refreshDeviceList();
                return true;
            case MENU_DISCOVERABLE:
                try {
                    mBeddernetService.setDiscoverable(true);
                } catch (RemoteException e) {
                    Log.e(TAG, "Remote Exception while making discoverable", e);
                }
                return true;
            case MENU_BEDNET_OFF:
                Log.d(TAG, "onClick: stopping service");
                finish();
                return true;
            case MENU_BLUETOOTH_OFF:
                Log.d(TAG, "Beddernet tries to manually turn Bluetooth off");
                try {
                    mBeddernetService.disableBluetooth();
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to kill bluetooth", e);
                }
                return true;
            case MENU_DISCOVERY:
                searchForDevices();
                return true;

            case MENU_SERVICES:
                Log.d(TAG, "view services in BeddernetConsole");

                try {
                    long[] hashes = mBeddernetService
                            .getAllUAIHOnDevice(NetworkAddress
                                    .castNetworkAddressToString(ConfigInfo.netAddressVal));
                    StringBuilder sb = new StringBuilder();
                    sb.append("Service hashes on devive:\n");
                    for (long l : hashes) {
                        sb.append(l);
                        sb.append("\n");
                    }
                    if (hashes != null) {
                        Toast.makeText(
                                this,
                                "Number of hashes: " + hashes.length + " "
                                        + sb.toString(), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Hashes was null", Toast.LENGTH_LONG)
                                .show();
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to get services", e);
                }


                return true;

            case MENU_SHAREAPP:
                // Get current ApplicationInfo to find .apk path
                ApplicationInfo app = getApplicationContext().getApplicationInfo();
                String filePath = app.sourceDir;

                Intent intent = new Intent(Intent.ACTION_SEND);

                // MIME of .apk is "application/vnd.android.package-archive".
                // but Bluetooth does not accept this. Let's use "*/*" instead.
                intent.setType("*/*");

                // Only use Bluetooth to send .apk
                intent.setPackage("com.android.bluetooth");

                // Append file and send Intent
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
                startActivity(Intent.createChooser(intent, "Share app"));

        }
        /*int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }*/
        return super.onOptionsItemSelected(item);
        //return false;
    }

    public void searchForDevices() {
        //outputTextView.append("Es wird 10 Sekunden lang nach sichtbaren Geräten gesucht\n");
        if (mBeddernetService!=null){
            Log.d(TAG, "findNeighbors in BeddernetConsole");
            new findNeighborsTask(mBeddernetService, myHandler, progressBarHolder, inAnimation, outAnimation).execute(null, null, null);
        }
        else {
            Log.d(TAG, "Service ist null. Maybe Bluetooth is off??");
        }
        outputTextView.setMovementMethod(new ScrollingMovementMethod());
    }

    public void onServiceDisconnected(ComponentName name) {
        serviceConnectionStatus = "Service disconnected";
    }


    private void refreshDeviceList() {
        // #FIX ADAPTER mDeviceArrayAdapter.clear();
        testAdapter.clear();
        //deviceColorList.clear();
        actualDeviceList = null;
        try {
            actualDeviceList = mBeddernetService.getDevicesWithStatus();
        } catch (Exception e) {
            Log.e(TAG, "Remote Exception while getting list of devices", e);
        }
        if (actualDeviceList != null && actualDeviceList.length > 0) {
            Log.d(TAG, "getDevicesWithStatus is not null");
            // actualDeviceList.length/2 removed and <= to <      <--- this is crap from beddernetcreater :D
            for (int i = 0; i < actualDeviceList.length; i = i + 2) {
                Log.d(TAG, "List size: " + actualDeviceList.length);

                //#Edit Change the Macadress to Devicename
                //mDeviceArrayAdapter.add(actualDeviceList[i] + ":" + actualDeviceList[i + 1]);
                try {
                    // #FIX ADAPTER mDeviceArrayAdapter.add(actualDeviceList[i] + ":" + mBeddernetService.getDeviceName(actualDeviceList[i]) + ":" + actualDeviceList[i + 1]);
                    testAdapter.add(actualDeviceList[i] + ":" + mBeddernetService.getDeviceName(actualDeviceList[i]) + ":" + actualDeviceList[i + 1]);
                    testAdapter.notifyDataSetChanged();
                    // If the Mac-Address is already in deviceColorList, than dont add it
                    if (!deviceColorList.contains(actualDeviceList[i])) {
                        deviceColorList.add(actualDeviceList[i]);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Error to get DeviceName");
                    e.printStackTrace();
                }
            }
        }
    }

    public void onServiceConnected(ComponentName className, IBinder service) {
        try {
            Log.d(TAG, "Service connected:" + service.getInterfaceDescriptor());

        } catch (RemoteException e) {
            e.printStackTrace();
            Log.d(TAG, "Service connected but something fucked up");
        }

        mBeddernetService = IBeddernetService.Stub.asInterface(service);
        if (mBeddernetService == null)
            Log.e(TAG, "MyService is nul!!?!");
        // Synchronously
        try {
            applicationIdentifierHash = mBeddernetService.registerCallback(
                    mCallback, applicationIdentifier);
            Log.d(TAG, "AIH received from server on register: "
                    + applicationIdentifierHash);
        } catch (RemoteException e) {

            Log.e(TAG,
                    "Remote exception from service while registering callback: "
                            + e.getMessage());
            e.printStackTrace();
        }
//        searchForDevices();

    }

    @SuppressWarnings("unused")
    private void sendMulticast(String[] addresses, byte[] appMessage) {
        try {
            mBeddernetService.sendMulticast(addresses, null, appMessage,
                    applicationIdentifier);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote exception from service, could not send message");
            e.printStackTrace();
        }
    }

    String getColorFromAddress(String address) {
        int counter = 0;
        for (String addr : deviceColorList) {
            if (addr.equals(address)) {
                break;//If the address is found then everything is okay
            } else {
                counter++;
            }
        }
        switch (counter) {
            case 0:
                return FIRST_COLOR;
            case 1:
                return SECOND_COLOR;
            case 2:
                return THIRD_COLOR;
            case 3:
                return FOURTH_COLOR;
            case 4:
                return FIFTH_COLOR;
            case 5:
                return SIXTH_COLOR;
            case 6:
                return SEVENTH_COLOR;
        }
        return "";
    }

    private void sendMessage(String address, String message) {
        String hexColorForDeviceText = getColorFromAddress(address);
        String deviceName;
        deviceName = getNameFromMacadress(address);
        Log.i(TAG,
                "BedderTestPlatform: DeviceList clicked, sending message to: "
                        + address);
        String toSend = "<font color='" + hexColorForDeviceText + "'>" + message + deviceName + " is closed" + "</font><br/>";
        outputTextView.append(Html.fromHtml(toSend));
        refreshDeviceList();
        try {
            //byte[] message = "---Hello from BedderTestPlatform".getBytes();
            byte[] messageBegin = "-".getBytes();
            byte[] realMessage = message.getBytes();
            // create a messageBegin array that is the size of the two arrays
            byte[] destination = new byte[messageBegin.length + realMessage.length];

            // copy messageBegin into start of destination (from pos 0, copy messageBegin.length bytes)
            System.arraycopy(messageBegin, 0, destination, 0, messageBegin.length);

            // copy mac into end of destination (from pos messageBegin.length, copy realMessage.length bytes)
            System.arraycopy(realMessage, 0, destination, messageBegin.length, realMessage.length);

            destination[0] = 2;
            mBeddernetService.sendUnicast(address, null, destination,
                    applicationIdentifier);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote exception from service, could not send message");
            e.printStackTrace();
        }
        outputTextView.setMovementMethod(new ScrollingMovementMethod());
    }

    private void sendMessage(String address) {
        String deviceName = "Unknown";
        String messageIsend;
        if (inputText == null) {
            messageIsend = "";
        } else {
            messageIsend = inputText.getText().toString();
        }
        deviceName = getNameFromMacadress(address);
        Log.i(TAG,
                "BedderTestPlatform: DeviceList clicked, sending message to: "
                        + address);
        String toSend = "<font color='#CFE2FA'>Me to " + deviceName + ": " + messageIsend + "</font><br/>";
        outputTextView.append(Html.fromHtml(toSend));
        try {


            // Disable the Keyboard
            // Check if no view has focus:
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }


            //byte[] message = "---Hello from BedderTestPlatform".getBytes();
            byte[] messageBegin = "-".getBytes();
            byte[] realMessage = inputText.getText().toString().getBytes();
            // create a messageBegin array that is the size of the two arrays
            byte[] destination = new byte[messageBegin.length + realMessage.length];

            // copy messageBegin into start of destination (from pos 0, copy messageBegin.length bytes)
            System.arraycopy(messageBegin, 0, destination, 0, messageBegin.length);

            // copy mac into end of destination (from pos messageBegin.length, copy realMessage.length bytes)
            System.arraycopy(realMessage, 0, destination, messageBegin.length, realMessage.length);

            destination[0] = 2;
            mBeddernetService.sendUnicast(address, null, destination,
                    applicationIdentifier);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote exception from service, could not send message");
            e.printStackTrace();
        }
        outputTextView.setMovementMethod(new ScrollingMovementMethod());
    }

    public void fileTransferComplete() {

        filesPending--;
        if (filesPending < 0)
            filesPending = 0;
        outputTextView.append("File transfer over, pending: " + filesPending + "\n");
        outputTextView.setMovementMethod(new ScrollingMovementMethod());
    }

    private void sendFile(String address) {
        Log.i(TAG, "Send file called");

        // outputTextView.append("Sending file to " + address + "\n");
        InputStream input = null;
        File file = new File(mFileNameToSend);
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Couldn't open path to record sound", e);
            e.printStackTrace();
        }
        try {
            if (!sendAudioFile) {
                input = activity.getResources().openRawResource(R.raw.walterminion);
            } else if (sendAudioFile) {
                //input = activity.getResources().openRawResource();

                input = fileInputStream;
            }
        } catch (NotFoundException e2) {
            Log.e(TAG, "Couldn't open resource", e2);
        } catch (Exception e) {
            Log.e(TAG, "Could open resource. The target device may have not opened Beddernet");
        }
        byte[] buffer = new byte[bufferSize];
        buffer[0] = FILE_MESSAGE;
        try {
            long startTime = System.currentTimeMillis();
            if (input==null){
                outputTextView.append("Um eine Sprachnachricht zu verschicken, zeichnen Sie die vorher auf.\n");
                Log.e(TAG,"Es muss erst eine Nachricht aufgezeichnet werden");
                return;
            }
            while (input.read(buffer, 1, buffer.length - 1) != -1) {
                try {
                    mBeddernetService.sendUnicast(address, null, buffer,
                            applicationIdentifier);

                } catch (Exception e) {
                    Log.e(TAG, "Could open resource. The target device may have not opened Beddernet");
                }
            }
            byte[] end = new byte[1];
            end[0] = FILE_END;
            mBeddernetService.sendUnicast(address, null, end,
                    applicationIdentifier);
            long endTime = System.currentTimeMillis();
            String result = ("File sent to " + address + "\nSending took:"
                    + (endTime - startTime) + " milliseconds");
            Log.i(TAG, result);
            // outputTextView.append(result);
            byte[] testEnd = new byte[]{TEST_END};
            mBeddernetService.sendUnicast(address, null, testEnd,
                    applicationIdentifier);
        } catch (Exception e) {
            Log.e(TAG, "Error in sending to service", e);
        }
    }

    public void sendRTT(String address) {
        RTTStartTime = System.currentTimeMillis();
        rttMessage[0] = RTT_MESSAGE;
        try {
            mBeddernetService.sendUnicast(address, null, rttMessage,
                    applicationIdentifier);
            outputTextView.append("Ping sent to: " + address + "\n");
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to send RTT message, remote exception");
        }
        outputTextView.setMovementMethod(new ScrollingMovementMethod());
    }

    @SuppressWarnings("unused")
    private void fileTransferTest(int fileIterations, String toAddress) {
        for (int i = 0; i < fileIterations; i++) {
            sendFile(toAddress);
        }
    }

    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(0, CONTEXT_MENU_SEND_MESSAGE, 0, "Send Message");
        menu.add(0, CONTEXT_MENU_SEND_FILE, 0, "Send file");
        menu.add(0, CONTEXT_MENU_SEND_FILE_DUPLEX, 0, "Send and receive file");
        menu.add(0, CONTEXT_MENU_SEND_RTT, 0, "Send ping");
        menu.add(0, CONTEXT_MENU_VIEW_SERVICES, 0, "View services");
        menu.add(0, CONTEXT_MENU_DISCONNECT, 0, "Disconnect");
        menu.add(0, CONTEXT_MENU_SEND_RECORDED_FILE, 0, "Send recorded File");
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        String selectedAddress = (String) mDeviceView.getAdapter().getItem(
                info.position);
        if (selectedAddress != null) {
            selectedAddress = selectedAddress.substring(0, 17);// Removes
            // the status
        }
        switch (item.getItemId()) {
            case CONTEXT_MENU_SEND_MESSAGE:
                sendMessage(selectedAddress);
                Log.i(TAG, "Trying to send message to : " + selectedAddress + "?");
                return true;
            case CONTEXT_MENU_SEND_FILE_DUPLEX:
                new DuplexFileTest(this).execute(selectedAddress, null, null);
                Log.i(TAG, "Trying to send and receive message : "
                        + selectedAddress + "?");
                return true;
            case CONTEXT_MENU_DISCONNECT:
                try {
                    sendMessage(selectedAddress, "The connection to ");
                    mBeddernetService.manualDisconnect(selectedAddress);
                } catch (RemoteException e) {
                    Log.e(TAG, "Could not manually disconnect", e);
                    e.printStackTrace();
                }
                refreshDeviceList();
                return true;

            case CONTEXT_MENU_SEND_FILE:
                if (selectedAddress != "" | selectedAddress != null)
                    sendFile(selectedAddress);
                return true;
            case CONTEXT_MENU_SEND_RTT:
                sendRTT(selectedAddress);
                return true;
            case CONTEXT_MENU_VIEW_SERVICES:
                try {
                    long[] hashes = mBeddernetService
                            .getAllUAIHOnDevice(selectedAddress);
                    StringBuilder sb = new StringBuilder();
                    sb.append("Service hashes on devive:\n");
                    for (long l : hashes) {
                        sb.append(l);
                        sb.append("\n");
                    }
                    if (hashes != null) {
                        Toast.makeText(
                                this,
                                "Number of hashes: " + hashes.length + " "
                                        + sb.toString(), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Hashes was null", Toast.LENGTH_LONG)
                                .show();
                    }
                    Log.i(TAG, "List of services on device called: "
                            + sb.toString());
                } catch (RemoteException e) {
                    Log.e(TAG, "Couldn't show all hashes", e);
                }
            case CONTEXT_MENU_SEND_RECORDED_FILE:
                sendAudioFile = true;
                sendFile(selectedAddress);
                sendAudioFile = false;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "BeddernetConsole Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://itu.beddernet/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver),
                new IntentFilter(COPA_RESULT)
        );
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "BeddernetConsole Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://itu.beddernet/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }

    public TextView getOutputTextView() {
        return outputTextView;
    }
	/*Voice Part, for testing it copy paste here, but for better use, create a seperate Activity*/

    private String getNameFromMacadress(String address) {
        String deviceName = "Unknown";
        try {
            deviceName = mBeddernetService.getDeviceName(address);
        } catch (RemoteException e) {
            Log.i(TAG, "Konnte nicht den Namen der Mac-Adresse ausfindig machen");
            e.printStackTrace();
        }
        return deviceName;
    }

    private class DuplexFileTest extends AsyncTask<String, Object, Object> {

        private BeddernetConsole console;

        public DuplexFileTest(BeddernetConsole console) {
            this.console = console;
        }

        protected Object doInBackground(String... params) {
            // console.duplexFiletransferTest(params[0], 5);
            duplexFileTransfer(params[0], 5);
            return null;
        }

        private void duplexFileTransfer(String address, int fileIterations) {
            for (int i = 0; i < fileIterations; i++) {
                byte[] startFileMsg = {FILE_FRANSFER_REQUEST};
                try {
                    mBeddernetService.sendUnicast(address, null, startFileMsg,
                            applicationIdentifier);
                } catch (RemoteException e1) {
                    Log.e(TAG, "duplexFiletransferTest error", e1);
                }
                sendFile(address);
                filesPending++;
                while (true) {
                    if (filesPending > 0) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    } else {
                        // outputTextView.append("File transfer over, pending:"
                        // + filesPending);
                        break;
                    }
                }
            }
        }

        protected void onPostExecute() {
        }
    }
}
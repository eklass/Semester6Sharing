//Sammlung an Links: 	http://stackoverflow.com/questions/23945171/android-app-bluetooth-connection-without-pairing
//Handler:				http://stackoverflow.com/questions/13079645/android-how-to-wait-asynctask-to-finish-in-mainthread
//Disabling Keyboard: 	http://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
// Now working on: 		http://stackoverflow.com/questions/22573301/how-to-pass-a-handler-from-activity-to-service
// 						http://stackoverflow.com/questions/6369287/accessing-ui-thread-handler-from-a-service
package itu.beddernet;

import itu.beddernet.approuter.IBeddernetService;
import itu.beddernet.approuter.IBeddernetServiceCallback;
import itu.beddernet.common.BeddernetInfo;
import itu.beddernet.common.NetworkAddress;
import itu.beddernet.recordSound.recordActivity;
import itu.beddernet.router.dsdv.info.ConfigInfo;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

// TODO: Es bleiben jedoch insgemein noch Probleme/Dinge die erledigt werden müssen:
// TODO:1. Beim Versenden der Files Exception(Nicht immer
// TODO:2. Automatisches installieren muss noch implementiert werden
// TODO:3. GUI anpassen, dass man beliebigen Text verschicken kann

public class BeddernetConsole extends Activity implements ServiceConnection {

	Handler myHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
				case 0:
					Log.e(TAG,"Ich wurde gerufen.Anscheinend ist der Thread fertig.");
					refreshDeviceList();
					// calling to this function from other pleaces
					// The notice call method of doing things
					break;
				default:
					break;
			}
		}
	};

	private EditText inputText;

	static final public String COPA_RESULT = "com.controlj.copame.backend.COPAService.REQUEST_PROCESSED";
	static final public String COPA_MESSAGE = "com.controlj.copame.backend.COPAService.COPA_MSG";

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
	private static final int MENU_RECORDAUDIO = 7;

	private static final byte FILE_MESSAGE = 1;
	private static final byte TEXT_MESSAGE = 2;
	private static final byte FILE_END = 3;
	private static final byte RTT_MESSAGE = 4;
	private static final byte RTT_MESSAGE_REPLY = 5;
	private static final byte TEST_END = 6;
	private static final byte FILE_END_ACK = 7;
	private static final byte FILE_FRANSFER_REQUEST = 8;

	private static final int bufferSize = 5000;

	private long RTTStartTime = 0;
	private long RTTEndTime = 0;
	public boolean sendAudioFile = false;
	String mFileNameToSend = Environment.getExternalStorageDirectory().getAbsolutePath();
	String mFileNameToPlay = Environment.getExternalStorageDirectory().getAbsolutePath();
	MediaPlayer mp = null;

	byte[] rttMessage = new byte[9];
	private String TAG = BeddernetInfo.TAG;
	private IBeddernetService mBeddernetService;
	private Activity activity;
	public ArrayAdapter<String> mDeviceArrayAdapter;
	private ListView mDeviceView;
	public static String applicationIdentifier = "BeddernetConsole";
	public static long applicationIdentifierHash = applicationIdentifier
			.hashCode();
	@SuppressWarnings("unused")
	private String serviceConnectionStatus;
	ServiceConnection sc = this;
	public TextView outputTextView;
	private int filesPending;

	// Variables for progressbar
	FrameLayout progressBarHolder;
	AlphaAnimation inAnimation;
	AlphaAnimation outAnimation;


	/**
	 * ATTENTION: This was auto-generated to implement the App Indexing API.
	 * See https://g.co/AppIndexing/AndroidStudio for more information.
	 */
	private GoogleApiClient client;
	private BroadcastReceiver receiver;

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
		Intent bindIntent = new Intent(
				"itu.beddernet.approuter.BeddernetService");

		this.bindService(bindIntent, this, Context.BIND_AUTO_CREATE);
		setContentView(R.layout.main);
		Button recVoiceButton = (Button) findViewById(R.id.recVoice);
		recVoiceButton.setOnClickListener(buttonListnener);
		Button clrTxtButton = (Button) findViewById(R.id.clrTxt);
		clrTxtButton.setOnClickListener(buttonListnener);
		//Button refDeviceButton = (Button) findViewById(R.id.refDevice);
		//refDeviceButton.setOnClickListener(buttonListnener);

		//Button MSIBox = (Button) findViewById(R.id.MSI);
		//MSIBox.setOnClickListener(buttonListnener);
		CheckBox maintainer = (CheckBox) findViewById(R.id.MaintainerBox);
		maintainer.setOnCheckedChangeListener(checkBoxListener);

		// Initialize the array adapter for the conversation thread
		mDeviceArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
		mDeviceView = (ListView) findViewById(R.id.in);
		registerForContextMenu(mDeviceView);

		mDeviceView.setAdapter(mDeviceArrayAdapter);
		mDeviceView.setOnItemClickListener(mListListener);
		mDeviceView.setHapticFeedbackEnabled(true);
		activity = this;

		outputTextView = (TextView) findViewById(R.id.outputTextView);

		mFileNameToSend += "/audiorecordtest.3gp";
		mFileNameToPlay += "/newStream.3gp";
		mp = new MediaPlayer();

		try {
			mp.setDataSource(mFileNameToPlay);
			mp.prepare();
		} catch (IOException e) {
			Log.i(TAG,"Fehler beim setzen der DataSource[Methode-onCreate]");
			e.printStackTrace();
		}
		progressBarHolder = (FrameLayout)findViewById(R.id.progressBarHolder);
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
				Log.e(TAG,"ICH WURDE GERUFEN!!! WAS GEHT AB!");
				refreshDeviceList();
				// do something here.
			}
		};


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
		searchForDevices();
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
		return false;
	}

	public void searchForDevices() {
		Log.d(TAG, "findNeighbors in BeddernetConsole");
		//outputTextView.append("Es wird 10 Sekunden lang nach sichtbaren Geräten gesucht\n");
		new findNeighborsTask(mBeddernetService,myHandler,progressBarHolder,inAnimation,outAnimation).execute(null, null, null);
		//Log.d(TAG, "Es wurde erfolgreich nach Geräten gesucht");
		outputTextView.setMovementMethod(new ScrollingMovementMethod());
	}

	public void onServiceDisconnected(ComponentName name) {
		serviceConnectionStatus = "Service disconnected";
	}

	private void refreshDeviceList() {
		mDeviceArrayAdapter.clear();
		String[] deviceList = null;
		try {
			deviceList = mBeddernetService.getDevicesWithStatus();
		} catch (Exception e) {
			Log.e(TAG, "Remote Exception while getting list of devices", e);
		}
		if (deviceList != null && deviceList.length > 0) {
			Log.d(TAG, "getDevicesWithStatus is not null");
			// deviceList.length/2 removed and <= to <      <--- this is crap from beddernetcreater :D
			for (int i = 0; i < deviceList.length; i = i + 2) {
				Log.d(TAG, "List size: " + deviceList.length);

				//#Edit Change the Macadress to Devicename
				//mDeviceArrayAdapter.add(deviceList[i] + ":" + deviceList[i + 1]);
				try {
					mDeviceArrayAdapter.add(deviceList[i]+":"+mBeddernetService.getDeviceName(deviceList[i])+":"+deviceList[i + 1]);
				} catch (RemoteException e) {
					Log.e(TAG,"Error to get DeviceName");
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
	}

	private OnClickListener buttonListnener = new OnClickListener() {
		public void onClick(View src) {
			switch (src.getId()) {
				case R.id.recVoice:
					startActivity(new Intent(getApplicationContext(), recordActivity.class));
					break;

				case R.id.clrTxt:
					outputTextView.setText("Ausgabe:\n");
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
	* */
	private OnItemClickListener mListListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

			String address = ((TextView) v).getText().toString();
			if (address != null) {
				address = address.substring(0, 17); // Ugly removes the status
				sendMessage(address);
				//after sendind Message, clear inputText
				inputText.setText("");
				refreshDeviceList();
			}
		}
	};

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

	private void sendMessage(String address, String message) {
		String deviceName;
		deviceName=getNameFromMacadress(address);
		Log.i(TAG,
				"BedderTestPlatform: DeviceList clicked, sending message to: "
						+ address);
		String toSend="<font color='#FAA987'>"+message+deviceName+" is closed"+"</font><br/>";
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
		String deviceName="Unknown";
		String messageIsend;
		if (inputText==null){
			messageIsend="";
		}
		else {
			messageIsend=inputText.getText().toString();
		}
		deviceName=getNameFromMacadress(address);
		Log.i(TAG,
				"BedderTestPlatform: DeviceList clicked, sending message to: "
						+ address);
		String toSend="<font color='#CFE2FA'>Me to "+deviceName+": " +messageIsend+"</font><br/>";
		outputTextView.append(Html.fromHtml(toSend));
		try {
			inputText = (EditText) findViewById(R.id.txtInput);

			// Disable the Keyboard
			// Check if no view has focus:
			View view = this.getCurrentFocus();
			if (view != null) {
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
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
		outputTextView.append("File transfer over, pending: " + filesPending+"\n");
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
					String msg = new String(message, 1, message.length - 1);
					String toSend="<font color='#CFFAD2'>"+getNameFromMacadress(senderAddress)+": " +msg+"</font><br/>";
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
						Log.i(TAG,"Fehler beim setzen der DataSource[Methode-onCreate]");
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
	/*Voice Part, for testing it copy paste here, but for better use, create a seperate Activity*/

	public TextView getOutputTextView() {
		return outputTextView;
	}
	private String getNameFromMacadress(String address) {
		String deviceName="Unknown";
		try {
			deviceName=mBeddernetService.getDeviceName(address);
		} catch (RemoteException e) {
			Log.i(TAG,"Konnte nicht den Namen der Mac-Adresse ausfindig machen");
			e.printStackTrace();
		}
		return deviceName;
	}
}
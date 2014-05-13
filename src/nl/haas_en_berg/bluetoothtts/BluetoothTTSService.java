package nl.haas_en_berg.bluetoothtts;

import java.io.IOException;
import java.util.UUID;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class BluetoothTTSService extends IntentService {
	private static final String TAG = "BluetoothTTSService";
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static boolean running = false;
	
	private BluetoothAdapter btAdapter;
	private BluetoothSocket btSocket;
	
	private StringBuilder sb = new StringBuilder();
	private TextToSpeech tts;
	private boolean ttsStatus = false;
	private String address;
	private NotificationManager notifyMgr;
	
	public BluetoothTTSService() {
		super("BluetoothTTSService");
	}
	
	public static boolean isRunning() {
		return running;
	}
	
	@Override
	public void onDestroy() {
		Log.i(TAG, "cleaning up...");
		running = false;
		if (tts != null) {
			tts.stop();
			tts.shutdown();
		}
		if (notifyMgr != null) {
			notifyMgr.cancel(1);
		}
		try {
			btSocket.getInputStream().close();
		}
		catch (IOException e) {
		}
		Log.i(TAG, "done");
		
		super.onDestroy();
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		running = true;
		
		address = intent.getStringExtra("address");
		Log.i(TAG, "address: " + address);
		
		tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				if (status == TextToSpeech.SUCCESS) {
					ttsStatus = true;
				}
			}
		});
		
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		
		if (btAdapter == null) {
			Log.e(TAG, "Bluetooth not supported");
			Log.i(TAG, "exiting");
			return;
		}
		else {
			if (btAdapter.isEnabled()) {
				Log.i(TAG, "Bluetooth ON");
			}
			else {
				Log.i(TAG, "Bluetooth OFF");
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivity(enableBtIntent);
				Log.i(TAG, "exiting");
				return;
			}
		}
		
		BluetoothDevice device = btAdapter.getRemoteDevice(address);
		
		try {
			btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
		}
		catch (IOException e) {
			Log.e(TAG, "socket create failed: " + e.getMessage() + ".");
			Log.i(TAG, "exiting");
			return;
		}
		
		btAdapter.cancelDiscovery();
		
		try {
			btSocket.connect();
			Log.i(TAG, "Connection ok");
		}
		catch (IOException e) {
			try {
				btSocket.close();
				Log.e(TAG, "connection failure: " + e.getMessage() + ".");
				Log.i(TAG, "exiting");
				return;
			}
			catch (IOException e2) {
				Log.e(TAG, "unable to close socket during connection failure: " + e.getMessage() + ".");
				Log.i(TAG, "exiting");
				return;
			}
		}
		
		byte[] buffer = new byte[256]; // buffer store for the stream
		int bytes;
		
		Log.i(TAG, "Building notification");
		
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		
		Intent resultIntent = new Intent(this, BluetoothTTSMainActivity.class);
		PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(resultPendingIntent);
		
		builder.setSmallIcon(R.drawable.ic_stat_name);
		builder.setContentText(device.getName());
		builder.setContentTitle("");
		builder.setOngoing(true);
		
		notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notifyMgr.notify(1, builder.build());
		
		Log.i(TAG, "Entering loop...");
		
		while (isRunning()) {
			try {
				bytes = btSocket.getInputStream().read(buffer);
				
				byte[] readBuf = (byte[]) buffer;
				String strIncom = new String(readBuf, 0, bytes);
				sb.append(strIncom);
				int endOfLineIndex = sb.indexOf("\n");
				if (endOfLineIndex > 0) {
					String sbprint = sb.substring(0, endOfLineIndex);
					sb.delete(0, sb.length());
					Log.i(TAG, "String: " + sbprint);
					builder.setContentTitle(sbprint);
					builder.setWhen(System.currentTimeMillis());
					notifyMgr.notify(1, builder.build());
					if (ttsStatus) {
						tts.speak(sbprint, TextToSpeech.QUEUE_FLUSH, null);
					}
				}
			}
			catch (IOException e) {
				Log.e(TAG, "connection closed during loop.");
				try {
					btSocket.getInputStream().close();
					btSocket.getOutputStream().close();
				}
				catch (IOException e1) {
					Log.e(TAG, "cannot close streams");
				}
				try {
					btSocket.close();
				}
				catch (IOException e1) {
					Log.e(TAG, "cannot close socket");
				}
				Log.i(TAG, "exiting");
				return;
			}
		}
		Log.i(TAG, "stopped");
		Log.i(TAG, "exiting");
	}
}

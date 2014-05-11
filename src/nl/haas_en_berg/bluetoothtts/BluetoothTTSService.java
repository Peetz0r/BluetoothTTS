package nl.haas_en_berg.bluetoothtts;

import java.io.IOException;
import java.util.UUID;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class BluetoothTTSService extends IntentService {
	private static final String TAG = "BluetoothTTSService";
	private BluetoothAdapter btAdapter = null;
	private BluetoothSocket btSocket = null;
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	private StringBuilder sb = new StringBuilder();
	private TextToSpeech tts;
	private boolean ttsStatus = false;
	
	// TODO: not hardcode the MAC address
	// private static String address = "00:1B:DC:00:03:47"; // gallium
	private static String address = "EC:55:F9:F1:BF:E2"; // flappie
	
	public BluetoothTTSService() {
		super("BluetoothTTSService");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				Log.i(TAG, "OnInit");
				if (status == TextToSpeech.SUCCESS) {
					Log.i(TAG, "SUCCESS");
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
		
		Log.i(TAG, "Entering loop...");
		
		while (true) {
			try {
				bytes = btSocket.getInputStream().read(buffer);
				
				byte[] readBuf = (byte[]) buffer;
				String strIncom = new String(readBuf, 0, bytes);
				sb.append(strIncom);
				int endOfLineIndex = sb.indexOf("\n");
				if (endOfLineIndex > 0) {
					String sbprint = sb.substring(0, endOfLineIndex);
					sb.delete(0, sb.length());
					Log.i(TAG, "String:\n" + sbprint);
					if(ttsStatus) {
						tts.speak(sbprint, TextToSpeech.QUEUE_FLUSH, null);
					}
					Log.i(TAG, "TTS: " + String.valueOf(ttsStatus));
				}
			}
			catch (IOException e) {
				Log.e(TAG, "connection closed during loop.");
				Log.i(TAG, "exiting");
				return;
			}
		}
		
	}
}

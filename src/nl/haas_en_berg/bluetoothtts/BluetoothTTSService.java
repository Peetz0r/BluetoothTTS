package nl.haas_en_berg.bluetoothtts;

import java.io.IOException;
import java.util.UUID;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

public class BluetoothTTSService extends IntentService {
	private static final String TAG = "BluetoothTTSService";
	private BluetoothAdapter btAdapter = null;
	private BluetoothSocket btSocket = null;
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private StringBuilder sb = new StringBuilder();

	// TODO: not hardcode the MAC address
	private static String address = "00:1B:DC:00:03:47";

	public BluetoothTTSService() {
		super("BluetoothTTSService");
	}

	@Override
	protected void onHandleIntent(Intent arg0) {

		btAdapter = BluetoothAdapter.getDefaultAdapter();

		if (btAdapter == null) {
			Log.e(TAG, "Bluetooth not supported");
			Log.i(TAG, "exiting");
			stopSelf();
		} else {
			if (btAdapter.isEnabled()) {
				Log.i(TAG, "Bluetooth ON");
			} else {
				Log.i(TAG, "Bluetooth OFF");
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivity(enableBtIntent);
				Log.i(TAG, "exiting");
				stopSelf();
			}
		}

		BluetoothDevice device = btAdapter.getRemoteDevice(address);

		try {
			btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
		} catch (IOException e) {
			Log.e(TAG, "socket create failed: " + e.getMessage() + ".");
			Log.i(TAG, "exiting");
			stopSelf();
		}

		btAdapter.cancelDiscovery();

		try {
			btSocket.connect();
			Log.i(TAG, "Connection ok");
		} catch (IOException e) {
			try {
				btSocket.close();
			} catch (IOException e2) {
				Log.e(TAG, "unable to close socket during connection failure: " + e.getMessage() + ".");
				Log.i(TAG, "exiting");
				stopSelf();
			}
		}

		byte[] buffer = new byte[256]; // buffer store for the stream
		int bytes;

		Log.i(TAG, "Entering loop...");
		
		while (true) {
			try {
				// Read from the InputStream
				bytes = btSocket.getInputStream().read(buffer);

				byte[] readBuf = (byte[]) buffer;
				String strIncom = new String(readBuf, 0, bytes);
				sb.append(strIncom);
				int endOfLineIndex = sb.indexOf("\r\n");
				if (endOfLineIndex > 0) {
					String sbprint = sb.substring(0, endOfLineIndex);
					sb.delete(0, sb.length());
					Log.i(TAG, sbprint);
				}
				Log.d(TAG, "...String:" + sb.toString() + "Byte:" + bytes + "...");
			} catch (IOException e) {
				break;
			}
		}

	}

}

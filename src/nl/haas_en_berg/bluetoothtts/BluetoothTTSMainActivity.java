package nl.haas_en_berg.bluetoothtts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.ToggleButton;

public class BluetoothTTSMainActivity extends Activity {
	
	private static final String TAG = "BluetoothTTSMainActivity";
	private static final String PREFS_NAME = "BluetoothTTSPreferences";
	private static final int REQUEST_ENABLE_BT = 1;
	private Spinner spinner;
	private Intent bluetoothTTSServiceIntent;
	private ToggleButton button;
	private SharedPreferences settings;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
		}
		
		bluetoothTTSServiceIntent = new Intent(this, BluetoothTTSService.class);
		settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
	}
	
	@Override
	public View onCreateView(String name, Context context, AttributeSet attrs) {
		button = (ToggleButton) findViewById(R.id.button1);
		
		return super.onCreateView(name, context, attrs);
	}
	
	public void performStart(View view) {
		if (button.isChecked()) {
			String address = ((HashMap<String, String>) spinner.getSelectedItem()).get("address");
			Editor editor = settings.edit();
			editor.putString("address", address);
			editor.commit();
			bluetoothTTSServiceIntent.putExtra("address", address);
			startService(bluetoothTTSServiceIntent);
		}
		else {
			if (bluetoothTTSServiceIntent != null) {
				Log.i(TAG, "stopping...");
				stopService(bluetoothTTSServiceIntent);
			}
			else {
				Log.e(TAG, "not stopping");
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onResume() {
		populateSpinner();
		
		final Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				button.setChecked(BluetoothTTSService.isRunning());
			}
		};
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				handler.obtainMessage(1).sendToTarget();
			}
		}, 100, 100);
		
		super.onResume();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT) {
			populateSpinner();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private void populateSpinner() {
		spinner = (Spinner) findViewById(R.id.spinner1);
		
		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		
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
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
				return;
			}
		}
		
		String address = settings.getString("address", "");
		int position = 0;
		
		ArrayList<Map<String, String>> spinnerArray = new ArrayList<Map<String, String>>();
		Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				Map<String, String> data = new HashMap<String, String>(2);
				data.put("name", device.getName());
				data.put("address", device.getAddress());
				if (device.getAddress().equals(address)) {
					position = spinnerArray.size();
				}
				spinnerArray.add(data);
			}
		}
		SimpleAdapter adapter = new SimpleAdapter(this, spinnerArray, android.R.layout.two_line_list_item, new String[] { "name", "address" }, new int[] { android.R.id.text1, android.R.id.text2 });
		spinner.setAdapter(adapter);
		spinner.setSelection(position);
	}
	
	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		
		public PlaceholderFragment() {
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			return rootView;
		}
	}
	
}

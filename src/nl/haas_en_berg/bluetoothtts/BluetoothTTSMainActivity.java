package nl.haas_en_berg.bluetoothtts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.ToggleButton;

public class BluetoothTTSMainActivity extends Activity {

	private static final String TAG = "BluetoothTTSMainActivity";
	private static final int REQUEST_ENABLE_BT = 1;
	private Spinner spinner;
	private Intent bluetoothTTSServiceIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	public void performStart(View view) {
		if (((ToggleButton) view).isChecked()) {
			bluetoothTTSServiceIntent = new Intent(this, BluetoothTTSService.class);
			String address = ((HashMap<String, String>) spinner.getSelectedItem()).get("address");
			Log.i(TAG, "address: " + address);
			bluetoothTTSServiceIntent.putExtra("address", address);
			startService(bluetoothTTSServiceIntent);
		} else {
			if(bluetoothTTSServiceIntent != null) {
				stopService(bluetoothTTSServiceIntent);
			}
			Log.i(TAG, "stop maybe?");
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
		} else {
			if (btAdapter.isEnabled()) {
				Log.i(TAG, "Bluetooth ON");
			} else {
				Log.i(TAG, "Bluetooth OFF");
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
				return;
			}
		}

		ArrayList<Map<String, String>> spinnerArray = new ArrayList<Map<String, String>>();

		Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				Map<String, String> data = new HashMap<String, String>(2);
				data.put("name", device.getName());
				data.put("address", device.getAddress());
				spinnerArray.add(data);
			}
		}

		SimpleAdapter adapter = new SimpleAdapter(this, spinnerArray, android.R.layout.two_line_list_item,

		new String[] { "name", "address" }, new int[] { android.R.id.text1, android.R.id.text2 });

		spinner.setAdapter(adapter);
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

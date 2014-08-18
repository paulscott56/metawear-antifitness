package za.co.paulscott.antifitness;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.Header;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import za.co.paulscott.adapters.InteractiveArrayAdapter;
import za.co.paulscott.models.DStvHighlights;
import za.co.paulscott.networkhelpers.URLFetcherAsync;
import no.nordicsemi.android.nrftoolbox.AppHelpFragment;
import no.nordicsemi.android.nrftoolbox.dfu.DfuActivity;
import no.nordicsemi.android.nrftoolbox.scanner.ScannerFragment;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.mbientlab.metawear.api.MetaWearBLEService;
import com.mbientlab.metawear.api.MetaWearController;

public class MainActivity extends FragmentActivity implements
		DeviceInfoFragment.Callbacks, ScannerFragment.OnDeviceSelectedListener,
		ServiceConnection {

	private static final String TAG = "MainActivity";
	List<DStvHighlights> list = new ArrayList<DStvHighlights>();

	private ListView lvMovies;
	private InteractiveArrayAdapter adapterMovies;
	public static final String EXTRA_BLE_DEVICE = "com.mbientlab.metawear.app.ModuleActivity.EXTRA_BLE_DEVICE";
	protected static final String ARG_ITEM_ID = "item_id";

	private static final int DFU = 0;
	private static final int REQUEST_ENABLE_BT = 1;
	protected static BluetoothDevice device;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		if (!bluetoothManager.getAdapter().isEnabled()) {
			final Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}
		lvMovies = (ListView) findViewById(R.id.lvMovies);
		ArrayList<DStvHighlights> aMovies = new ArrayList<DStvHighlights>();
		adapterMovies = new InteractiveArrayAdapter(this, aMovies);
		lvMovies.setAdapter(adapterMovies);

		getDstvHighlights();
	}

	private void getDstvHighlights() {
		URLFetcherAsync client = new URLFetcherAsync();
		client.initCookies(getApplicationContext());
		client.get("1", null, new AsyncHttpResponseHandler() {

			@Override
			public void onSuccess(int statusCode, Header[] headers,
					byte[] responseBody) {
				try {
					String str = new String(responseBody, "UTF-8");
					Document doc = Jsoup.parse(str);
					Elements items = doc.select("#ui-news");

					Elements divs = items.select("div.ui-item");
					Log.i(TAG, String.valueOf(divs.size()));
					for (Element div : divs) {
						Elements titleattr = div.select("h2.ui-title");
						String title = titleattr.text();

						Elements contentattr = div.select("div.ui-contents");
						Elements details = contentattr.select("p.ui-details");
						Elements image = details.select("img.ui-thumb");
						String img = image.attr("src");

						Elements authorattr = div.select("span.ui-author");
						String author = authorattr.text();

						Elements timeattr = div.select("span.ui-time");
						String showtime = timeattr.text();

						DStvHighlights hl = new DStvHighlights(title, showtime,
								img, author);
						adapterMovies.add(hl);
					}
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			@Override
			public void onFinish() {
				Log.i(TAG, "All done");
			}

			@Override
			public void onFailure(int arg0, Header[] arg1, byte[] arg2,
					Throwable arg3) {
				// TODO Auto-generated method stub

			}
		});
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		mwService = ((MetaWearBLEService.LocalBinder) service).getService();
		mwController = mwService.getMetaWearController();
		mwController
				.addDeviceCallback(new MetaWearController.DeviceCallbacks() {
					@Override
					public void connected() {
						invalidateOptionsMenu();
					}

					@Override
					public void disconnected() {
						invalidateOptionsMenu();
						if (device != null) {
							mwService.reconnect();
						}
					}
				});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.content.ServiceConnection#onServiceDisconnected(android.content
	 * .ComponentName)
	 */
	@Override
	public void onServiceDisconnected(ComponentName name) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDeviceSelected(BluetoothDevice device, String name) {
		ModuleActivity.device = device;
		try {
		mwService.connect(ModuleActivity.device);
		} catch(NullPointerException e) {
			Toast.makeText(this, "You have not selected a MetaWear device. Sorry, can't connect!", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onDialogCanceled() {
		// TODO Auto-generated method stub

	}

	@Override
	public void startDfu() {
		final Intent dfu = new Intent(this, DfuActivity.class);
		dfu.putExtra(EXTRA_BLE_DEVICE, device);
		startActivityForResult(dfu, DFU);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case DFU:
			device = data.getParcelableExtra(EXTRA_BLE_DEVICE);
			if (device != null) {
				mwService.connect(device);
			}
			break;
		case REQUEST_ENABLE_BT:
			if (resultCode == Activity.RESULT_CANCELED) {
				finish();
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getApplicationContext().unbindService(this);
	}

	private final BroadcastReceiver metaWearUpdateReceiver = MetaWearBLEService
			.getMetaWearBroadcastReceiver();
	private MetaWearBLEService mwService;
	private MetaWearController mwController;
	protected ModuleFragment moduleFragment;
	protected static HashMap<String, Fragment.SavedState> fragStates = new HashMap<>();

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(metaWearUpdateReceiver,
				MetaWearBLEService.getMetaWearIntentFilter());
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(metaWearUpdateReceiver);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.ble_connect:
			final FragmentManager fm = getSupportFragmentManager();
			final ScannerFragment dialog = ScannerFragment.getInstance(
					MainActivity.this, null, true);
			dialog.show(fm, "scan_fragment");
			break;
		case R.id.ble_disconnect:
			device = null;
			mwService.close(true);
			break;
		case R.id.action_about:
			final AppHelpFragment fragment = AppHelpFragment
					.getInstance(R.string.mw_about_text);
			fragment.show(getSupportFragmentManager(), "help_fragment");
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.bledevice, menu);
		try {
			if (mwController.isConnected()) {
				menu.findItem(R.id.ble_connect).setVisible(false);
				menu.findItem(R.id.ble_disconnect).setVisible(true);
			} else {
				menu.findItem(R.id.ble_connect).setVisible(true);
				menu.findItem(R.id.ble_disconnect).setVisible(false);
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
			menu.findItem(R.id.ble_connect).setVisible(true);
			menu.findItem(R.id.ble_disconnect).setVisible(false);
		}
		return true;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (device != null) {
			outState.putParcelable(EXTRA_BLE_DEVICE, device);
		}
		if (moduleFragment != null) {
			getSupportFragmentManager().putFragment(outState, "mContent",
					moduleFragment);
		}
	}

}

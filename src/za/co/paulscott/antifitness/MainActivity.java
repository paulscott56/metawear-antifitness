package za.co.paulscott.antifitness;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;



import org.apache.http.Header;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import za.co.paulscott.adapters.InteractiveArrayAdapter;
import za.co.paulscott.models.DStvHighlights;
import za.co.paulscott.networkhelpers.URLFetcherAsync;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import com.loopj.android.http.AsyncHttpResponseHandler;

public class MainActivity extends Activity implements DeviceInfoFragment.Callbacks, 
ScannerFragment.OnDeviceSelectedListener, ServiceConnection {

	private static final String TAG = "MainActivity";
	List<DStvHighlights> list = new ArrayList<DStvHighlights>();

	private ListView lvMovies;
	private InteractiveArrayAdapter adapterMovies;
	public static final String EXTRA_BLE_DEVICE= 
            "com.mbientlab.metawear.app.ModuleActivity.EXTRA_BLE_DEVICE";
    protected static final String ARG_ITEM_ID = "item_id";

    private static final int DFU = 0;
    private static final int REQUEST_ENABLE_BT = 1;
    protected static BluetoothDevice device;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_main);
	    final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (!bluetoothManager.getAdapter().isEnabled()) {
            final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
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

						Elements contentattr = div
								.select("div.ui-contents");
						Elements details = contentattr
								.select("p.ui-details");
						Elements image = details.select("img.ui-thumb");
						String img = image.attr("src");

						Elements authorattr = div.select("span.ui-author");
						String author = authorattr.text();

						Elements timeattr = div.select("span.ui-time");
						String showtime = timeattr.text();

						DStvHighlights hl = new DStvHighlights(title, showtime, img, author);
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
	
	
	

}

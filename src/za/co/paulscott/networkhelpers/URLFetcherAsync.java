package za.co.paulscott.networkhelpers;

import org.apache.http.impl.cookie.BasicClientCookie;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.*;

public class URLFetcherAsync {

	private final String BASE_URL = "http://mobi.dstv.com/highlights/";
	private final String TAG = "URLFetcher";
	private PersistentCookieStore myCookieStore;

	private AsyncHttpClient client = new AsyncHttpClient();

	public void initCookies(Context context) {
		myCookieStore = new PersistentCookieStore(context);
		client.setCookieStore(myCookieStore);
		// set up the EPG Cookie
		BasicClientCookie epgCookie = new BasicClientCookie("EPGService",
				"40_1");
		epgCookie.setVersion(1);
		epgCookie.setDomain("mobi.dstv.com");
		epgCookie.setPath("/");
		myCookieStore.addCookie(epgCookie);

		BasicClientCookie mwCookie = new BasicClientCookie("mw", "800");
		mwCookie.setVersion(1);
		mwCookie.setDomain(".mobi.dstv.com");
		mwCookie.setPath("/");
		myCookieStore.addCookie(mwCookie);

	}

	public void get(String url, RequestParams params,
			AsyncHttpResponseHandler responseHandler) {
		client.setCookieStore(myCookieStore);
		client.setUserAgent("Android4.4 - antifitness App");
		Log.i(TAG, getAbsoluteUrl(url));
		client.get(getAbsoluteUrl(url), params, responseHandler);
	}

	public void post(String url, RequestParams params,
			AsyncHttpResponseHandler responseHandler) {
		client.post(getAbsoluteUrl(url), params, responseHandler);
	}

	private String getAbsoluteUrl(String relativeUrl) {
		return BASE_URL + relativeUrl;
	}
}

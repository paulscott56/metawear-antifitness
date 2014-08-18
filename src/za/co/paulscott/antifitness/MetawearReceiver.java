package za.co.paulscott.antifitness;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

public class MetawearReceiver extends BroadcastReceiver {

	private static final String TAG = "MetawearReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "Receiver started");
		Toast.makeText(context, "Don't panic but your time is up!",
				Toast.LENGTH_LONG).show();
		Vibrator vibrator = (Vibrator) context
				.getSystemService(Context.VIBRATOR_SERVICE);
		vibrator.vibrate(2000);
	}

}

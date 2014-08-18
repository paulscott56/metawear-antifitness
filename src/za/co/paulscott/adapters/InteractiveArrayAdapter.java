package za.co.paulscott.adapters;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.squareup.picasso.Picasso;

import za.co.paulscott.antifitness.MetawearReceiver;
import za.co.paulscott.antifitness.R;
import za.co.paulscott.models.DStvHighlights;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class InteractiveArrayAdapter extends ArrayAdapter<DStvHighlights> {
	protected static final String TAG = "ListViewAdapter";

	public InteractiveArrayAdapter(Context context,
			ArrayList<DStvHighlights> aMovies) {
		super(context, 0, aMovies);
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {

		OnClickListener mOnTitleClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				DStvHighlights highlight = getItem(position);
				
				SimpleDateFormat formatter = new SimpleDateFormat("EE, dd MMM HH:mm");
				String dt = highlight.getShowtime();

				try {
					Date showdate = formatter.parse(dt);
					int year = Calendar.getInstance().get(Calendar.YEAR);
					Calendar c = Calendar.getInstance();
					c.setTime(showdate);
					c.set(Calendar.YEAR, year);
					showdate = c.getTime();
					long showMillis = showdate.getTime();
					
					pushAppointmentsToCalender(getContext(),
							"Watch DStv", highlight.getDescription(),
							highlight.getAuthor(), highlight.getAuthor(), 1,
							showMillis, true);
					Toast.makeText(getContext(),
							"Show has been added to your Calendar",
							Toast.LENGTH_SHORT).show();
					startAlert(showMillis);
					// debug code...
//					Intent intent = new Intent();
//					intent.setAction("za.co.paulscott.antifitness.mwbroadcast");
//					getContext().sendBroadcast(intent);
					
				
				} catch (ParseException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				

			}
		};

		// Get the data item for this position
		DStvHighlights highlight = getItem(position);
		// Check if an existing view is being reused, otherwise inflate the view
		if (convertView == null) {
			LayoutInflater inflater = LayoutInflater.from(getContext());
			convertView = inflater.inflate(R.layout.item_dstv_highlights,
					parent, false);
		}
		// Lookup views within item layout
		TextView tvTitle = (TextView) convertView.findViewById(R.id.tvTitle);
		TextView tvShowtime = (TextView) convertView
				.findViewById(R.id.tvShowtime);
		TextView tvAuthor = (TextView) convertView.findViewById(R.id.tvAuthor);
		ImageView ivPosterImage = (ImageView) convertView
				.findViewById(R.id.ivPosterImage);
		ImageButton btn = (ImageButton) convertView.findViewById(R.id.btn);
		btn.setFocusable(false);
		btn.setOnClickListener(mOnTitleClickListener);
		btn.setHapticFeedbackEnabled(true);
		btn.setImageResource(R.drawable.ic_cal);

		// btn.setClickable(false);
		// Populate the data into the template view using the data object
		tvTitle.setText(highlight.getDescription());
		tvShowtime.setText(highlight.getShowtime());
		tvAuthor.setText(highlight.getAuthor());
		Picasso.with(getContext()).load(highlight.getImageURL())
				.into(ivPosterImage);
		// Return the completed view to render on screen

		return convertView;
	}

	public static long pushAppointmentsToCalender(Context context,
			String title, String description, String author, String place,
			int status, long startDate, boolean needReminder) {
		String eventUriString = "content://com.android.calendar/events";
		ContentValues eventValues = new ContentValues();

		eventValues.put(CalendarContract.Events.EVENT_TIMEZONE,
				"Africa/Johannesburg");

		eventValues.put("calendar_id", 1); // id, We need to choose from
											// our mobile for primary
											// its 1
		eventValues.put("title", title);
		eventValues.put("description", description + " on " + author);
		eventValues.put("eventLocation", place);

		long endDate = startDate + 1000 * 120 * 60; // For next 2hr

		eventValues.put("dtstart", startDate);
		eventValues.put("dtend", endDate);
		eventValues.put("eventStatus", status); // This information is
		eventValues.put("hasAlarm", 1); // 0 for false, 1 for true

		Uri eventUri = context.getApplicationContext().getContentResolver()
				.insert(Uri.parse(eventUriString), eventValues);
		long eventID = Long.parseLong(eventUri.getLastPathSegment());

		if (needReminder) {
			String reminderUriString = "content://com.android.calendar/reminders";
			ContentValues reminderValues = new ContentValues();
			reminderValues.put("event_id", eventID);
			reminderValues.put("minutes", 15); // Default value of the
												// system. Minutes is a
												// integer
			reminderValues.put(CalendarContract.Reminders.METHOD,
					CalendarContract.Reminders.METHOD_ALERT);

			Uri reminderUri = context.getApplicationContext()
					.getContentResolver()
					.insert(Uri.parse(reminderUriString), reminderValues);
		}

		return eventID;

	}

	public void startAlert(long timeInMillis) {
		Intent intent = new Intent(getContext(), MetawearReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext()
				.getApplicationContext(), 234324243, intent, 0);
		AlarmManager alarmManager = (AlarmManager) getContext()
				.getSystemService(getContext().ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
		Date date = new Date(timeInMillis);
		DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
		String dateFormatted = formatter.format(date);
		Toast.makeText(getContext(), "Alarm set for " + dateFormatted ,
				Toast.LENGTH_LONG).show();
	}

}
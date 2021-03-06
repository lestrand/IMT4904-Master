package no.hig.strand.lars.todoity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import no.hig.strand.lars.todoity.R;
import no.hig.strand.lars.todoity.utils.Utilities;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

public class NewTaskActivity extends FragmentActivity {
	
	private Task mTask;
	private AutoCompleteTextView mLocationText;
	private ArrayAdapter<String> mAutoCompleteAdapter;
	private static Button mActiveTimeButton; // Ugly hack!
	
	public static final int MAP_REQUEST = 1;
	public static final String LOCATION_EXTRA = 
			"no.hig.strand.lars.todoity.LOCATION";
	public static final String TASK_EXTRA = "no.hig.strand.lars.todoity.TASK";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_task);
		// Show the Up button in the action bar.
		setupActionBar();
		
		Bundle data = getIntent().getExtras();
		if (data != null) {
			mTask = data.getParcelable(TASK_EXTRA);
		} else {
			mTask = new Task();	
		}
		
		setupUI();
	}

	
	
	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	
	
	private void setupUI() {
		LinearLayout container = (LinearLayout) findViewById(R.id.container);
		container.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mLocationText.isFocused()) {
					mLocationText.clearFocus();
				}
			}
		});
		
		// Set up the auto complete text view with listeners.
		mLocationText = (AutoCompleteTextView) 
				findViewById(R.id.location_text);
		mLocationText.setText(mTask.getAddress());
		mAutoCompleteAdapter = new PlacesAutoCompleteAdapter(this,
				android.R.layout.simple_dropdown_item_1line);
		mLocationText.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view,
					int position, long id) {
				String location = (String) adapterView
						.getItemAtPosition(position);
				mLocationText.setText(location);
				mTask.setAddress(location);
				new GetLocationCoordinatesFromName().execute(location);
				InputMethodManager imm = (InputMethodManager) 
						getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
						InputMethodManager.HIDE_NOT_ALWAYS);
			}
		});
		mLocationText.setAdapter(mAutoCompleteAdapter);
		
		// Set behavior of the location button.
		Button button = (Button) findViewById(R.id.location_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(
						NewTaskActivity.this, MapActivity.class);
				intent.putExtra(LOCATION_EXTRA, mTask.getLocation());
				startActivityForResult(intent, MAP_REQUEST);
			}
		});
		
		// Set behavior of the category spinner.
		Spinner spinner = (Spinner) findViewById(R.id.category_spinner);
		
		// Read occupation from preferences and set 
		//  predefined categories accordingly.
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		String occupationPref = sharedPref.getString(
				SettingsActivity.PREF_OCCUPATION_KEY, "");
		int spinnerArray; 
		if (occupationPref.equals(getString(R.string.pref_undergraduate))) {
			spinnerArray = R.array.undergraduate_tasks_array;
		} else {
			spinnerArray = R.array.postgraduate_tasks_array;
		}
		ArrayAdapter<CharSequence> adapter = ArrayAdapter
				.createFromResource(this, spinnerArray,
						android.R.layout.simple_spinner_dropdown_item);
		adapter.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		if (! mTask.getCategory().equals("")) {
			spinner.setSelection(adapter.getPosition(mTask.getCategory()));
		}
		
		EditText editText = (EditText) findViewById(R.id.description_edit);
		editText.setText(mTask.getDescription());
		
		// Set behavior of the fixed time check box.
		CheckBox checkBox = (CheckBox) findViewById(R.id.fixed_time_check);
		checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, 
					boolean isChecked) {
				Button fromButton = (Button) findViewById(R.id.from_button);
				Button toButton = (Button) findViewById(R.id.to_button);
				if (isChecked) {
					fromButton.setEnabled(true);
					toButton.setEnabled(true);
				} else {
					fromButton.setEnabled(false);
					toButton.setEnabled(false);
				}
			}
		});
		if (! mTask.getFixedStart().equals("")) {
			checkBox.setChecked(true);
		}
		
		// Set behavior of the fixed time buttons
		OnClickListener timeButtonListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				mActiveTimeButton = (Button) v;
				DialogFragment tpf = new TimePickerFragment();
				tpf.show(getSupportFragmentManager(), "timePicker");
			}
		};
		button = (Button) findViewById(R.id.from_button);
		button.setOnClickListener(timeButtonListener);
		if (! mTask.getFixedStart().equals("")) {
			button.setText(mTask.getFixedStart());
		}
		button = (Button) findViewById(R.id.to_button);
		button.setOnClickListener(timeButtonListener);
		if (! mTask.getFixedEnd().equals("")) {
			button.setText(mTask.getFixedEnd());
		}
		
		// Set behavior of the done/finish button.
		button = (Button) findViewById(R.id.save_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveTask();
			}
		});
	}
	
	
	
	private void saveTask() {
		// Get location in the event that the user did not click an item
		//  on the auto complete adapter (If this is the case, 
		//  GetLocationcoordinatesFromName is never called and the task won't
		//  have GPS coordinates. This might need to be fixed). 
		if (! mLocationText.getText().toString().equals("")) {
			mTask.setAddress(mLocationText.getText().toString());
		}
		// Check if a location is chosen.
		if (mTask.getLocation() != null || ! mTask.getAddress().isEmpty()) {
			// Location is chosen, check if a category is chosen.
			Spinner spinner = (Spinner) findViewById(R.id.category_spinner);
			String category = spinner.getSelectedItem().toString();
			if (! category.isEmpty()) {
				mTask.setCategory(category);
				// Add task description, no check, doesn't need to be filled.
				EditText editText = (EditText) findViewById(
						R.id.description_edit);
				mTask.setDescription(editText.getText().toString());
				
				// Check start and end times if the fixed time box is ticked.
				CheckBox checkBox = (CheckBox) findViewById(
						R.id.fixed_time_check);
				if (checkBox.isChecked()) {
					Button button = (Button) findViewById(R.id.from_button);
					String fixedStart = button.getText().toString();
					button = (Button) findViewById(R.id.to_button);
					String fixedEnd = button.getText().toString();
					if (! fixedStart.equals(getString(R.string.from))) {
						// Has fixed times and times are properly set.
						mTask.setFixedStart(fixedStart);
						if (! fixedEnd.equals(getString(R.string.to))) {
							mTask.setFixedEnd(fixedEnd);
						}
						sendTaskBack();
					} else {
						Toast.makeText(this, 
								getString(R.string.set_time_message), 
								Toast.LENGTH_LONG).show();
					}
				} else {
					// Doesn't have fixed times, send Task to ListActivity.
					sendTaskBack();
				}
			} else {
				Toast.makeText(NewTaskActivity.this, 
						getString(R.string.set_category_message),
						Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(NewTaskActivity.this, 
					getString(R.string.set_location_message),
					Toast.LENGTH_LONG).show();
		}
	}
	
	
	
	private void sendTaskBack() {
		Intent data = new Intent();
		data.putExtra(TASK_EXTRA, mTask);
		setResult(RESULT_OK, data);
		finish();
	}
	
	
	
	@Override
	protected void onActivityResult(int requestCode, 
			int resultCode, Intent data) {
		if (requestCode == MAP_REQUEST) {
			if (resultCode == RESULT_OK) {
				LatLng location = data.getParcelableExtra(LOCATION_EXTRA);
				mTask.setLocation(location);
				new GetLocationCoordinatesFromValue().execute(location);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	
	
	private class GetLocationCoordinatesFromValue 
			extends AsyncTask<LatLng, Void, String> {

		@Override
		protected String doInBackground(LatLng... params) {
			LatLng location = params[0];
			try {
				List<Address> addresses = new Geocoder(getBaseContext())
				.getFromLocation(location.latitude, location.longitude, 1);
				
				if (addresses.size() > 0) {
					Address address = addresses.get(0);
					String value = "";
					for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
						value += address.getAddressLine(i);
						if (i < address.getMaxAddressLineIndex() -1) {
							value += ", ";
						}
					}
					return value;
				}
			} catch (IOException e) {
				Log.d("MTP", "Could not get GeoCoder");
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				mTask.setAddress(result);
				mLocationText.setText(result);
				Button button = (Button) findViewById(R.id.location_button);
				// Just move the focus away from the editable text
				button.requestFocus();
			}
		}
		
	}
	
	
	
	private class GetLocationCoordinatesFromName 
			extends AsyncTask<String, Void, LatLng> {
		
		@Override
		protected LatLng doInBackground(String... params) {
			String value = params[0];
			try {
				List<Address> addresses = new Geocoder(getBaseContext())
						.getFromLocationName(value, 1);
				if (addresses.size() > 0) {
					return new LatLng(addresses.get(0).getLatitude(),
							addresses.get(0).getLongitude());
				}
			} catch (IOException e) {
				Log.d("MTP", "Could not get GeoCoder");
			}
			return null;
		}

		@Override
		protected void onPostExecute(LatLng result) {
			if (result != null) {
				mTask.setLocation(result);
			}
		}
		
	}
	
	
	
	
	private class PlacesAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {
	    private ArrayList<String> resultList;

	    public PlacesAutoCompleteAdapter(Context context, int textViewResourceId) {
	        super(context, textViewResourceId);
	    }

	    @Override
	    public int getCount() {
	        return resultList.size();
	    }

	    @Override
	    public String getItem(int index) {
	        return resultList.get(index);
	    }

	    @Override
	    public Filter getFilter() {
	        Filter filter = new Filter() {
	            @Override
	            protected FilterResults performFiltering(
	            		CharSequence constraint) {
	                FilterResults filterResults = new FilterResults();
	                if (constraint != null) {
	                    // Retrieve the autocomplete results.
	                    resultList = Utilities.autocomplete(
	                    		constraint.toString());

	                    // Assign the data to the FilterResults
	                    filterResults.values = resultList;
	                    filterResults.count = resultList.size();
	                }
	                return filterResults;
	            }

	            @Override
	            protected void publishResults(CharSequence constraint,
	            		FilterResults results) {
	                if (results != null && results.count > 0) {
	                    notifyDataSetChanged();
	                }
	                else {
	                    notifyDataSetInvalidated();
	                }
	            }};
	        return filter;
	    }
	}	
	
	
	
	
	public static class TimePickerFragment extends DialogFragment
			implements TimePickerDialog.OnTimeSetListener {
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final Calendar c = Calendar.getInstance();
			int hour = c.get(Calendar.HOUR_OF_DAY);
			int minute = c.get(Calendar.MINUTE);
			
			return new TimePickerDialog(getActivity(), this, 
					hour, minute, true);
		}

		@SuppressLint("SimpleDateFormat")
		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
			Calendar c = Calendar.getInstance();
			c.set(0, 0, 0, hourOfDay, minute);
			
			mActiveTimeButton.setText(formatter.format(c.getTime()));
		}
		
	}
	
	
}

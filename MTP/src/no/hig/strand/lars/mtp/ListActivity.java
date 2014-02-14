package no.hig.strand.lars.mtp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ListActivity extends FragmentActivity {
	
	private ArrayList<Task> mTasks;
	private TaskListAdapter mAdapter;
	private String mDate;
	
	// Holding temporary tasks. Used when checking for already existing tasks.
	//  (Should probably do this some other way).
	private ArrayList<Task> tempTasks; 
	
	public static final int TASK_REQUEST = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list);
		// Show the Up button in the action bar.
		setupActionBar();
		
		mTasks = new ArrayList<Task>();
		mDate = "";
		Intent data = getIntent();
		if (data.hasExtra(MainActivity.TASKS_EXTRA)) {
			mTasks = data.getParcelableArrayListExtra(MainActivity.TASKS_EXTRA);
			mDate = data.getStringExtra(MainActivity.DATE_EXTRA);
		}
		mAdapter = new TaskListAdapter(this, mTasks);
		
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
		Button button = (Button) findViewById(R.id.date_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DialogFragment dpf = new DatePickerFragment();
				dpf.show(getSupportFragmentManager(), "datePicker");
			}
		});
		if (! mDate.equals("")) {
			button.setText(mDate);
		}
		
		button = (Button) findViewById(R.id.new_task_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(ListActivity.this,
						NewTaskActivity.class);
				startActivityForResult(intent, TASK_REQUEST);
			}
		});
		
		button = (Button) findViewById(R.id.save_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveToDatabases();
			}
		});
		
		button = (Button) findViewById(R.id.discard_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				discardList();
			}
		});
		
		ListView listView = (ListView) findViewById(R.id.tasklist_list);
		listView.setAdapter(mAdapter);
	}
	
	
	
	private void saveToDatabases() {
		// Check if there are any tasks to save.
		if (! mTasks.isEmpty()) {
			// Check if a date is set.
			Button button = (Button) findViewById(R.id.date_button);
			String date = button.getText().toString();
			if (! date.equals(getString(R.string.set_date))) {
				new SaveTasksToDatabase().execute(date);
			} else {
				Toast.makeText(this, getString(R.string.set_date_message), 
						Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(this, getString(R.string.no_tasks_message), 
					Toast.LENGTH_LONG).show();
		}
	}
	
	
	
	private void discardList() {
		Utilities.showConfirmDialog(this, getString(R.string.confirm), 
				getString(R.string.discard_list_message), 
				new Utilities.ConfirmDialogListener() {
			@Override
			public void PositiveClick(DialogInterface dialog, int id) {
				mTasks.clear();
				mAdapter = new TaskListAdapter(ListActivity.this, mTasks);
				ListView listView = (ListView) findViewById(R.id.tasklist_list);
				listView.setAdapter(mAdapter);
			}
		});
	}
	
	
	
	private void showExistingListDialog(ArrayList<Task> tasks) {
		tempTasks = tasks;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.existing_list_message));
		builder.setNegativeButton(android.R.string.cancel, 
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		}).setNeutralButton(getString(R.string.add), 
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				for (Task t : tempTasks) {
					mTasks.add(t);
				}
				Button button = (Button) findViewById(R.id.date_button);
				button.setText(mDate);
				mAdapter = new TaskListAdapter(
						ListActivity.this, mTasks);
				ListView list = (ListView) findViewById(R.id.tasklist_list);
				list.setAdapter(mAdapter);
			}
		}).setPositiveButton(getString(R.string.delete), 
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Button button = (Button) findViewById(R.id.date_button);
				button.setText(mDate);
				new DeleteExistingFromDatabase().execute(mDate);
			}
		});
		
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
	
	
	private void deleteExistingList(String date) {
		TasksDb tasksDb;
		tasksDb = new TasksDb(getApplicationContext());
		tasksDb.open();
		
		tasksDb.deleteListByDate(date);
		tasksDb.close();
	}
	
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		if (requestCode == TASK_REQUEST) {
			if (resultCode == RESULT_OK) {
				Task task = data.getParcelableExtra(NewTaskActivity.TASK_EXTRA);
				mTasks.add(task);
				mAdapter = new TaskListAdapter(this, mTasks);
				ListView listView = (ListView) findViewById(R.id.tasklist_list);
				listView.setAdapter(mAdapter);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	
	
	
	public static class DatePickerFragment extends DialogFragment 
			implements DatePickerDialog.OnDateSetListener {
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);
			
			DatePickerDialog dpd = new DatePickerDialog(getActivity(), 
					this, year, month, day);
			dpd.getDatePicker().setMinDate(c.getTimeInMillis());
			
			return dpd;
		}

		@SuppressLint("SimpleDateFormat")
		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMM dd, yyyy");
			Calendar c = new GregorianCalendar(year, monthOfYear, dayOfMonth);
			
			// Just double check that the context is correct.
			if (getActivity() instanceof ListActivity) {
				ListActivity activity = (ListActivity) getActivity();
				String date = formatter.format(c.getTime());
				
				activity.new GetExistingListFromDatabase().execute(date);
			}
		}
		
	}
	
	
	
	private class GetExistingListFromDatabase 
			extends AsyncTask<String, Void, ArrayList<Task>> {
		TasksDb tasksDb;
		
		@Override
		protected ArrayList<Task> doInBackground(String... params) {
			mDate = params[0];
			tasksDb = new TasksDb(ListActivity.this);
			tasksDb.open();
			ArrayList<Task> tasks = tasksDb.getTasksByDate(mDate);
			tasksDb.close();
			
			return tasks;
		}

		@Override
		protected void onPostExecute(ArrayList<Task> result) {
			if (! result.isEmpty()) {
				showExistingListDialog(result);
			} else {
				Button button = (Button) findViewById(R.id.date_button);
				button.setText(mDate);
			}
		}
		
	}
	
	
	
	private class DeleteExistingFromDatabase 
			extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			deleteExistingList(params[0]);
			
			return null;
		}
	}
	
	
	
	private class SaveTasksToDatabase extends AsyncTask<String, Void, Void> {
		TasksDb tasksDb;
		ProgressDialog dialog;
		
		@Override
		protected void onPreExecute() {
			dialog = ProgressDialog.show(ListActivity.this
					, "", getString(R.string.save_message), true);
		}
		
		@Override
		protected Void doInBackground(String... params) {
			deleteExistingList(params[0]);
			
			tasksDb = new TasksDb(ListActivity.this);
			tasksDb.open();
			long listId = tasksDb.insertList(params[0]);
			for (Task t : mTasks) {
				tasksDb.insertTask(listId, t);
			}
			tasksDb.close();
			
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			dialog.dismiss();
			finish();
		}
	}
	
	
	
	private class TaskListAdapter extends ArrayAdapter<Task> {
		private final Context context;
		private final ArrayList<Task> tasks;
		
		public TaskListAdapter(Context context, ArrayList<Task> tasks) {
			super(context, R.layout.item_list_task, tasks);
			this.context = context;
			this.tasks = tasks;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View rowView = inflater.inflate(R.layout.item_list_task,
					parent, false);
			TextView taskText = (TextView) rowView.findViewById(R.id.task_text);
			taskText.setText(tasks.get(position).getCategory() + ": "
							+ tasks.get(position).getDescription());
			TextView locationText = (TextView) rowView
					.findViewById(R.id.location_text);
			locationText.setText(tasks.get(position).getAddress());
			
			// Set up behavior of the edit task button.
			ImageButton button = (ImageButton) rowView
					.findViewById(R.id.edit_button);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					LinearLayout layout = (LinearLayout) v.getParent();
					ListView listView = (ListView) layout.getParent();
					int position = -1;
					for (int i = 0; i < listView.getChildCount(); i++) {
						LinearLayout ll = (LinearLayout) listView.getChildAt(i);
						if (layout.equals(ll)) {
							position = i;
						}
					}
					Intent intent = new Intent(context, NewTaskActivity.class);
					intent.putExtra(NewTaskActivity.TASK_EXTRA,
							mTasks.get(position));
					startActivityForResult(intent, TASK_REQUEST);
				}
			});
			
			// Set up behavior of the delete task button.
			button = (ImageButton) rowView.findViewById(R.id.remove_button);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					LinearLayout layout = (LinearLayout) v.getParent();
					ListView listView = (ListView) layout.getParent();
					int position = -1;
					for (int i = 0; i < listView.getChildCount(); i++) {
						LinearLayout ll = (LinearLayout) listView.getChildAt(i);
						if (layout.equals(ll)) {
							position = i;
						}
					}
					mTasks.remove(position);
					listView.setAdapter(new TaskListAdapter(context, mTasks));
				}
			});
			
			return rowView;
		}
		
	}

}

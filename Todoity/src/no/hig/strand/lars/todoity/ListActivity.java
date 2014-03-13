package no.hig.strand.lars.todoity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import no.hig.strand.lars.todoity.TasksContract.ListEntry;
import no.hig.strand.lars.todoity.services.GeofenceService;
import no.hig.strand.lars.todoity.utils.AppEngineUtilities;
import no.hig.strand.lars.todoity.utils.DatabaseUtilities;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
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
	private TasksDb mTasksDb;
	private String mDate;
	private boolean mIsEditing;
	private int mTempTaskNumber;
	
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
		mTasksDb = TasksDb.getInstance(this);
		mDate = "";
		mIsEditing = false;
		mTempTaskNumber = -1;
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
				new SaveTask().execute(date);
			} else {
				Toast.makeText(this, getString(R.string.set_date_message), 
						Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(this, getString(R.string.no_tasks_message), 
					Toast.LENGTH_LONG).show();
		}
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
				mAdapter = new TaskListAdapter(ListActivity.this, mTasks);
				ListView list = (ListView) findViewById(R.id.tasklist_list);
				list.setAdapter(mAdapter);
			}
		}).setPositiveButton(getString(R.string.delete), 
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Button button = (Button) findViewById(R.id.date_button);
				button.setText(mDate);
				new DatabaseUtilities.DeleteList(
						ListActivity.this, null).execute(mDate);
			}
		});
		
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		if (requestCode == TASK_REQUEST) {
			if (resultCode == RESULT_OK) {
				Task task = data.getParcelableExtra(NewTaskActivity.TASK_EXTRA);
				if (mIsEditing) {
					mTasks.set(mTempTaskNumber, task);
					mIsEditing = false;
				} else {
					mTasks.add(task);
				}
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
				
				activity.new CheckListAvailabilityTask().execute(date);
			}
		}
		
	}
	
	
	
	private class CheckListAvailabilityTask 
			extends AsyncTask<String, Void, ArrayList<Task>> {
		
		@Override
		protected ArrayList<Task> doInBackground(String... params) {
			mDate = params[0];
			ArrayList<Task> tasks = mTasksDb.getTasksByDate(mDate);
			
			return tasks;
		}

		@Override
		protected void onPostExecute(ArrayList<Task> result) {
			Button button = (Button) findViewById(R.id.date_button);
			String date = button.getText().toString();
			if (!mDate.equals(date) && ! result.isEmpty()) {
				showExistingListDialog(result);
			} else {
				button.setText(mDate);
			}
		}
		
	}
	
	
	
	private class SaveTask extends AsyncTask<String, Void, Void> {
		ProgressDialog dialog;
		
		@Override
		protected void onPreExecute() {
			// Display a progress dialog. Need to wait for response before
			//  preceding in application. (Not ideal).
			dialog = ProgressDialog.show(ListActivity.this
					, "", getString(R.string.save_message), true);
		}
		
		@Override
		protected Void doInBackground(String... params) {
			// Save internally to SQLite database
			
			// Get id of the current list or create new if not exists.
			long listId;
			boolean newList = true;
			Cursor c = mTasksDb.fetchListByDate(params[0]);
			if (c.moveToFirst()) {
				listId = c.getLong(c.getColumnIndexOrThrow(ListEntry._ID));
				newList = false;
			} else {
				listId = mTasksDb.insertList(params[0]);
			}
			for (Task t : mTasks) {
				// Task has no ID, it has not been inserted before. Insert it.
				if (t.getId() == 0) {
					long taskId = mTasksDb.insertTask(listId, t);
					t.setId((int)taskId);
					t.setDate(mDate);
					// Save externally to AppEngine
					new AppEngineUtilities.SaveTask(
							ListActivity.this, t).execute();
					
				// If the task has an id, check if we are creating a new list 
				//  or if the task belonged to an old list.
				//  Delete the old task, and insert the new.
				} else if (newList || ! mDate.equals(t.getDate())) {
					mTasksDb.deleteTaskById(t.getId());
					new AppEngineUtilities.RemoveTask(
							ListActivity.this, t).execute();
					long taskId = mTasksDb.insertTask(listId, t);
					t.setId((int)taskId);
					t.setDate(mDate);
					// Save externally to AppEngine
					new AppEngineUtilities.SaveTask(
							ListActivity.this, t).execute();
				} else {
					mTasksDb.updateTask(t);
					new AppEngineUtilities.UpdateTask(
							ListActivity.this, t).execute();
				}
				
			}
			
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			Intent intent = new Intent(ListActivity.this, GeofenceService.class);
			startService(intent);
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
			rowView.setTag(position);
			
			if (tasks.get(position).isFinished()) {
				return null;
			}
			
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
					int position = (Integer) layout.getTag();
					
					mTempTaskNumber = position;
					mIsEditing = true;
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
					int position = (Integer) layout.getTag();
					
					mTasks.remove(position);
					listView.setAdapter(new TaskListAdapter(context, mTasks));
				}
			});
			
			
			
			return rowView;
		}
		
	}

}

package no.hig.strand.lars.todoity;

import java.util.ArrayList;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;


public class TodayFragment extends Fragment {
	
	private View mRootView;
	private ArrayList<Task> mTasks;
	private TodayListAdapter mAdapter;
	private String mDate;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_today, 
				container, false);
		
		mRootView = rootView;
		mTasks = new ArrayList<Task>();
		mAdapter = new TodayListAdapter(getActivity(), mTasks);
		
		setupUI();
        
		return rootView;
	}
	
	
	
	@Override
	public void onResume() {
		mDate = Utilities.getDate();
		new LoadTasksFromDatabase().execute(mDate);
		super.onResume();
	}
	
	
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.context_menu_task, menu);
	}
	
	
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.move_task:
			// TODO move selected task...
			return true;
		}
		return super.onContextItemSelected(item);
	}



	private void setupUI() {
		ListView listView = (ListView) mRootView.findViewById(R.id.tasks_list);
		listView.setAdapter(mAdapter);
		
		Button button = (Button) mRootView.findViewById(R.id.new_list_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), ListActivity.class));
			}
		});
		
		button = (Button) mRootView.findViewById(R.id.edit_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Check if there are tasks to edit
				if (! mTasks.isEmpty()) {
					Intent intent = new Intent(getActivity(),
							ListActivity.class);
					intent.putExtra(MainActivity.TASKS_EXTRA, mTasks);
					intent.putExtra(MainActivity.DATE_EXTRA, mDate);
					startActivity(intent);
				}
			}
		});
		
		button = (Button) mRootView.findViewById(R.id.delete_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Show dialog to the user asking for confirmation of deletion.
				Utilities.showConfirmDialog(getActivity(), 
						getString(R.string.confirm), 
						getString(R.string.delete_list_message), 
						new Utilities.ConfirmDialogListener() {
					@Override
					public void PositiveClick(DialogInterface dialog, int id) {
						String date = Utilities.getDate();
						if (getActivity() instanceof MainActivity) {
							MainActivity activity = 
									(MainActivity) getActivity();
							activity.new DeleteListFromDatabase().execute(date);
						}
					}
				});
			}
		});
	}
	
	
	
	private void startTask() {
		if (getActivity() instanceof MainActivity) {
			MainActivity activity = (MainActivity) getActivity();
			activity.startTask();
		}
	}
	
	
	
	private void pauseTask() {
		if (getActivity() instanceof MainActivity) {
			MainActivity activity = (MainActivity) getActivity();
			activity.pauseTask();
		}
	}
	
	
	
	private class LoadTasksFromDatabase extends AsyncTask<String, Void, Void> {
		private TasksDb tasksDb;
		
		@Override
		protected Void doInBackground(String... params) {
			tasksDb = new TasksDb(getActivity());
			tasksDb.open();
			mTasks.clear();
			mTasks = tasksDb.getTasksByDate(params[0]);
			tasksDb.close();
			
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			mAdapter = new TodayListAdapter(getActivity(), mTasks);
			ListView listView = (ListView) mRootView.findViewById(R.id.tasks_list);
			listView.setAdapter(mAdapter);
		}
		
	}
	
	
	
	private class SetTaskActiveStatus extends AsyncTask<Task, Void, Void> {
		private TasksDb tasksDb;

		@Override
		protected Void doInBackground(Task... params) {
			Task task = params[0];
			
			tasksDb = new TasksDb(getActivity());
			tasksDb.open();
			tasksDb.updateTaskActiveStatus(task.getId(), task.isActive());
			tasksDb.close();
			
			return null;
		}
	}
	
	
	
	private class SetTaskFinishedStatus extends AsyncTask<Task, Void, Void> {
		private TasksDb tasksDb;

		@Override
		protected Void doInBackground(Task... params) {
			Task task = params[0];
			
			tasksDb = new TasksDb(getActivity());
			tasksDb.open();
			tasksDb.updateTaskActiveStatus(task.getId(), task.isFinished());
			tasksDb.close();
			
			return null;
		}
	}
	
	
	
	private class TodayListAdapter extends ArrayAdapter<Task> {
		private final Context context;
		private final ArrayList<Task> tasks;
		
		public TodayListAdapter(Context context, ArrayList<Task> tasks) {
			super(context, R.layout.item_list_task, tasks);
			this.context = context;
			this.tasks = tasks;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View rowView = inflater.inflate(R.layout.item_today_list,
					parent, false);
			rowView.setTag(position);
			registerForContextMenu(rowView);
			
			TextView taskText = (TextView) rowView.findViewById(R.id.task_text);
			taskText.setText(tasks.get(position).getCategory() + ": "
							+ tasks.get(position).getDescription());
			TextView locationText = (TextView) rowView
					.findViewById(R.id.location_text);
			locationText.setText(tasks.get(position).getAddress());
			
			
			Button startPauseButton = (Button) rowView.
					findViewById(R.id.start_pause_button);
			if (tasks.get(position).isActive()) {
				rowView.setBackgroundColor(getResources()
						.getColor(R.color.lightgreen));
				startPauseButton.setText(getString(R.string.pause));
			}
			startPauseButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					LinearLayout layout = (LinearLayout) v.getParent();
					int position = (Integer) layout.getTag();
					
					String text = ((Button) v).getText().toString();
					if (text.equals(getString(R.string.start))) {
						mTasks.get(position).setActive(true);
						new SetTaskActiveStatus().execute(mTasks.get(position));
						((Button) v).setText(getString(R.string.pause));
						layout.setBackgroundColor(getResources()
								.getColor(R.color.lightgreen));
						startTask();
					} else {
						mTasks.get(position).setActive(false);
						new SetTaskActiveStatus().execute(mTasks.get(position));
						((Button) v).setText(getString(R.string.start));
						layout.setBackgroundResource(0);
						pauseTask();
					}
				}
			});
			
			CheckBox checkBox = (CheckBox) rowView
					.findViewById(R.id.finish_check);
			checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(
						CompoundButton buttonView, boolean isChecked) {
					LinearLayout layout = (LinearLayout) buttonView.getParent();
					int position = (Integer) layout.getTag();
					
					TextView taskText = (TextView) layout
							.findViewById(R.id.task_text);
					TextView locationText = (TextView) layout
							.findViewById(R.id.location_text);
					Button button = (Button) layout.findViewById(
							R.id.start_pause_button);
					
					if (isChecked) {
						mTasks.get(position).setFinished(true);
						taskText.setPaintFlags(taskText.getPaintFlags() 
								| Paint.STRIKE_THRU_TEXT_FLAG);
						locationText.setPaintFlags(locationText.getPaintFlags() 
								| Paint.STRIKE_THRU_TEXT_FLAG);
						button.setEnabled(false);
					} else {
						mTasks.get(position).setFinished(false);
						taskText.setPaintFlags(taskText.getPaintFlags() 
								& (~Paint.STRIKE_THRU_TEXT_FLAG));
						locationText.setPaintFlags(locationText.getPaintFlags() 
								& (~Paint.STRIKE_THRU_TEXT_FLAG));
						button.setEnabled(true);
					}
				}
			});
			
			if (tasks.get(position).isFinished()) {
				checkBox.setChecked(true);
				taskText.setPaintFlags(taskText.getPaintFlags() 
						| Paint.STRIKE_THRU_TEXT_FLAG);
				locationText.setPaintFlags(locationText.getPaintFlags() 
						| Paint.STRIKE_THRU_TEXT_FLAG);
				startPauseButton.setEnabled(false);
			}
			
			return rowView;
		}
		
	}

}
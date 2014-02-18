package no.hig.strand.lars.mtp;

import java.util.ArrayList;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
					ListView listView = (ListView) layout.getParent();
					int position = -1;
					for (int i = 0; i < listView.getChildCount(); i++) {
						LinearLayout ll = (LinearLayout) listView.getChildAt(i);
						if (layout.equals(ll)) {
							position = i;
						}
					}
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
			
			
			Button finishButton = (Button) rowView
					.findViewById(R.id.finished_button);
			finishButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// TODO finish task...
					LinearLayout layout = (LinearLayout) v.getParent();
					ListView listView = (ListView) layout.getParent();
					int position = -1;
					for (int i = 0; i < listView.getChildCount(); i++) {
						LinearLayout ll = (LinearLayout) listView.getChildAt(i);
						if (layout.equals(ll)) {
							position = i;
						}
					} // TODO Need some more things for handling finished task.
					mTasks.get(position).setFinished(true);
					TextView textView = (TextView) layout
							.findViewById(R.id.task_text);
					textView.setPaintFlags(textView.getPaintFlags() 
							| Paint.STRIKE_THRU_TEXT_FLAG);
					textView = (TextView) layout
							.findViewById(R.id.location_text);
					textView.setPaintFlags(textView.getPaintFlags() 
							| Paint.STRIKE_THRU_TEXT_FLAG);
					Button button = ((Button) v);
					button.setEnabled(false);
					button = (Button) layout.findViewById(
							R.id.start_pause_button);
					button.setEnabled(false);
				}
			});
			
			if (tasks.get(position).isFinished()) {
				taskText.setPaintFlags(taskText.getPaintFlags() 
						| Paint.STRIKE_THRU_TEXT_FLAG);
				locationText.setPaintFlags(locationText.getPaintFlags() 
						| Paint.STRIKE_THRU_TEXT_FLAG);
				startPauseButton.setEnabled(false);
				finishButton.setEnabled(false);
			}
			
			return rowView;
		}
		
	}

}

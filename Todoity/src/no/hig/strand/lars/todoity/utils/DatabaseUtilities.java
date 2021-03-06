package no.hig.strand.lars.todoity.utils;

import no.hig.strand.lars.todoity.Task;
import no.hig.strand.lars.todoity.TasksContract.ListEntry;
import no.hig.strand.lars.todoity.TasksDb;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

public class DatabaseUtilities {

	public DatabaseUtilities() {}
	
	
	public interface OnDeletionCallback {
		public void onDeletionDone();
	}
	
	public interface OnTaskMovedCallback {
		public void onTaskMoved();
	}
	
	
	
	public static class SaveTask extends AsyncTask<Void, Void, Void> {
		TasksDb tasksDb;
		Task task;
		Context context;
		
		public SaveTask(Context context, Task task) {
			this.task = task;
			this.context = context;
			tasksDb = TasksDb.getInstance(context);
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			long listId = tasksDb.getListIdByDate(task.getDate());
			if (listId < 0) {
				listId = tasksDb.insertList(task.getDate());
			}
			long taskId = tasksDb.insertTask(listId, task);
			task.setId((int) taskId);
			new AppEngineUtilities.SaveTask(context, task).execute();
			
			return null;
		}
	}
	
	
	
	public static class UpdateTask extends AsyncTask<Void, Void, Void> {
		TasksDb tasksDb;
		Task task;
		
		public UpdateTask(Context context, Task task) {
			this.task = task;
			tasksDb = TasksDb.getInstance(context);
		}

		@Override
		protected Void doInBackground(Void... params) {
			tasksDb.updateTask(task);
			return null;
		}
	}
	
	
	
	public static class DeleteList extends AsyncTask<String, Void, Void> {
		OnDeletionCallback callback;
		TasksDb tasksDb;
		
		public DeleteList(Context context, OnDeletionCallback callback) {
			tasksDb = TasksDb.getInstance(context);
			this.callback = callback;
		}

		@Override
		protected Void doInBackground(String... params) {
			tasksDb.deleteListByDate(params[0]);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (callback != null) {
				callback.onDeletionDone();
			}
		}
	}
	
	
	
	public static class DeleteTask extends AsyncTask<Void, Void, Void> {	
		private OnDeletionCallback callback;
		TasksDb tasksDb;
		Task task;
		
		public DeleteTask(Context context, Task task, 
				OnDeletionCallback callback) {
			this.task = task;
			tasksDb = TasksDb.getInstance(context);
			this.callback = callback;
		}

		@Override
		protected Void doInBackground(Void... params) {
			tasksDb.deleteTaskById(task.getId());
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (callback != null) {
				callback.onDeletionDone();
			}
		}
	}
	
	
	
	public static class MoveTaskToDate extends AsyncTask<Void, Void, Void> {
		private OnTaskMovedCallback callback;
		private TasksDb tasksDb;
		private Task task;
		private String date;

		public MoveTaskToDate(Context context, Task task, String date, 
				OnTaskMovedCallback callback) {
			this.callback = callback;
			this.task = task;
			tasksDb = TasksDb.getInstance(context);
			this.date = date;
		}

		@Override
		protected Void doInBackground(Void... params) {			
			// Move the task to the selected date. 
			//  Create list on that date if none exist.
			int listId = -1;
			Cursor c = tasksDb.fetchListByDate(date);
			if (c.moveToFirst()) {
				listId = c.getInt(c.getColumnIndexOrThrow(ListEntry._ID));
			} else {
				listId = (int) tasksDb.insertList(date);
			}
			
			tasksDb.moveTaskToDate(task.getId(), listId);
			
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (callback != null) {
				callback.onTaskMoved();
			}
		}
		
	}
}

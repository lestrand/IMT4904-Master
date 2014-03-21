package no.hig.strand.lars.todoity.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import no.hig.strand.lars.todoity.R;
import no.hig.strand.lars.todoity.SettingsActivity;
import no.hig.strand.lars.todoity.Task;
import no.hig.strand.lars.todoity.TasksDb;
import no.hig.strand.lars.todoity.utils.Utilities;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;

public class RecommenderService extends Service implements 
		GooglePlayServicesClient.ConnectionCallbacks, 
		GooglePlayServicesClient.OnConnectionFailedListener {

	private TasksDb mTasksDb;
	private LocationClient mLocationClient;
	private Location mLastKnownLocation;
	private long mTimeOfCalculation;
	private HashMap<String, Float> mRecommendationMap;
	private String mRecommendedCategory;
	private float mCategoryProbability;
	private ArrayList<Task> mTaskHisory;
	private ArrayList<Task> mPlannedTasks;
	private ArrayList<Task> mRecommendedList;
	
	// The maximum distance between current location and a task location for
	//  the locations to be counted as 'equal'.
	private static final int MAXIMUM_DISTANCE_LOCATION_RECOMMENDATION = 100;
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		mTasksDb = TasksDb.getInstance(this);
		mLocationClient = new LocationClient(this, this, this);
		mLocationClient.connect();
		mLastKnownLocation = null;
		// Get calculation time (since midnight).
		mTimeOfCalculation = Utilities.getTimeOfDay(
				Calendar.getInstance().getTimeInMillis());
		mRecommendationMap = new HashMap<String, Float>();
		mRecommendedCategory = "";
		mCategoryProbability = 0;
		mTaskHisory = mTasksDb.getTaskHistory();
		mPlannedTasks = mTasksDb.getTasksByDate(Utilities.getTodayDate());
		mRecommendedList = new ArrayList<Task>();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		return START_STICKY;
	}
	
	
	
	// Recommend a task for the time provided as parameter
	private void recommend(long recommendationTime) {
		String category = "";
		
		mRecommendationMap.clear();
		timeOfDayRecommendation(recommendationTime);
		timeOfDayAndDayOfWeekRecommendation(recommendationTime);
		locationRecommendation();
		timeOfDayAndLocationRecommendation(recommendationTime);
		timeOfDayAndDayOfWeekAndLocationRecommendation(recommendationTime);
		
		
		for (Entry<String, Float> entry : mRecommendationMap.entrySet()) {
			if (entry.getValue() > mCategoryProbability) {
				mCategoryProbability = entry.getValue();
				mRecommendedCategory = entry.getKey();
			}
		}
		
		
		// A recommendation has been found.
		if (! category.isEmpty()) {
			
			// Check if there is a task with a fixed time that may interfere
			//  with the task to be recommended.
			Task recommendedTask = null;
			Task fixedTask = null;
			long fixedTaskStartTime = 0;
			long timeNow = Calendar.getInstance().getTimeInMillis();
			for (Task task : mPlannedTasks) {
				if (task.getCategory().equals(category) && 
						! mRecommendedList.contains(task)) {
					recommendedTask = task;
				}
				if (! task.getFixedStart().isEmpty()) {
					long taskStartTime = Utilities.getTimeOfDay(task.getFixedStart());
					
					// If the start time of the task is later than 'now' and if
					//  the task is sooner than a previously found task or if
					//  a task have not been found.
					if (taskStartTime - timeNow > 0 && 
							( taskStartTime < fixedTaskStartTime
							|| fixedTaskStartTime == 0 )) {
						fixedTaskStartTime = taskStartTime;
						fixedTask = task;
					}
				}
			}
			
			if (recommendedTask != null) {
				// If a task with a fixed start time has been found, we must
				//  account for this.
				long avgTimeSpent  = getAverageTimeSpentOnTask(category);
				if (fixedTask != null) {
					long timeToFixedStart = fixedTaskStartTime - timeNow;
					
					// If able to find an average time and this time is less 
					//  than the time until the fixed task is to be started. 
					//  Recommend task and perform new recommendation with
					//  new time.
					if (avgTimeSpent > 0 && avgTimeSpent < timeToFixedStart) {
						mRecommendedList.add(recommendedTask);
						mTimeOfCalculation += avgTimeSpent;
						recommend(mTimeOfCalculation);
					} else {
						mRecommendedList.add(fixedTask);
						if (! fixedTask.getFixedEnd().isEmpty()) {
							long timeToNextTask = Utilities.getTimeOfDay(
									fixedTask.getFixedEnd());
							mTimeOfCalculation = timeToNextTask;
						} else {
							avgTimeSpent = getAverageTimeSpentOnTask(
									fixedTask.getCategory());
							mTimeOfCalculation += avgTimeSpent;
						}
						recommend(mTimeOfCalculation);
					}
					
				} else {
					mRecommendedList.add(recommendedTask);
					mTimeOfCalculation += avgTimeSpent;
					recommend(mTimeOfCalculation);
				}
				
			} else {
				// TODO no task were found. go one lower in probability.
			}
			
		}
	}
	
	
	
	/*
	 * Calculate the type of task (category) that is completed most often
	 *  at the time of day of the calculation.
	 */
	private void timeOfDayRecommendation(long recommendationTime) {
		List<String> categories = readCategories();
		HashMap<String, Integer> categoryOccurrences = 
				new HashMap<String, Integer>();
		
		// Put the categories in a HashMap. This is used to count occurrences
		//  of what type of tasks happen at what times.
		for (String category : categories) {
			categoryOccurrences.put(category, 0);
		}
		
		long startTimeTask;
		long endTimeTask;
		for (Task task : mTaskHisory) {
			if (task.getTimeStarted() > 0) {
				// Get start and end time since midnight of the task.
				startTimeTask = Utilities.getTimeOfDay(task.getTimeStarted());
				endTimeTask = Utilities.getTimeOfDay(task.getTimeEnded());
				
				if (startTimeTask < recommendationTime && 
						endTimeTask > recommendationTime) {
					int occurrences = categoryOccurrences
							.get(task.getCategory()) + 1;
					categoryOccurrences.put(task.getCategory(), occurrences);
				}
			}
		}

		addRecommendationsFromMap(categoryOccurrences);
	}
	
	
	
	private void timeOfDayAndDayOfWeekRecommendation(long recommendationTime) {
		List<String> categories = readCategories();
		HashMap<String, Integer> categoryOccurrences = 
				new HashMap<String, Integer>();
		
		// Put the categories in a HashMap. This is used to count occurrences
		//  of what type of tasks happen at what times.
		for (String category : categories) {
			categoryOccurrences.put(category, 0);
		}
		
		int dayNow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
		long startTimeTask;
		long endTimeTask;
		for (Task task : mTaskHisory) {
			if (task.getTimeStarted() > 0) {
				// Get start and end time since midnight of the task.
				int dayTask = Utilities.getDayOfWeek(task.getTimeStarted());
				startTimeTask = Utilities.getTimeOfDay(task.getTimeStarted());
				endTimeTask = Utilities.getTimeOfDay(task.getTimeEnded());
				
				if (startTimeTask < recommendationTime && 
						endTimeTask > recommendationTime && 
						dayNow == dayTask) {
					int occurrences = categoryOccurrences
							.get(task.getCategory()) + 1;
					categoryOccurrences.put(task.getCategory(), occurrences);
				}
			}
		}
		
		addRecommendationsFromMap(categoryOccurrences);
	}
	
	
	
	private void locationRecommendation() {
		if (mLocationClient.isConnected()) {
			mLastKnownLocation = mLocationClient.getLastLocation();
		} else if (mLastKnownLocation == null) {
			return;
		}
		
		List<String> categories = readCategories();
		HashMap<String, Integer> categoryOccurrences = 
				new HashMap<String, Integer>();
		
		// Put the categories in a HashMap. This is used to count occurrences
		//  of what type of tasks happen at what times.
		for (String category : categories) {
			categoryOccurrences.put(category, 0);
		}
		
		for (Task task : mTaskHisory) {
			
			// Check if the task location is the same as the current location.
			// TODO Should use collected location context here instead...
			float[] result = new float[3];
			Location.distanceBetween(mLastKnownLocation.getLatitude(), 
					mLastKnownLocation.getLongitude(), 
					task.getLocation().latitude, 
					task.getLocation().longitude, result);
			float distance = result[0];
			
			if (distance <= MAXIMUM_DISTANCE_LOCATION_RECOMMENDATION) {
				int occurrences = categoryOccurrences
						.get(task.getCategory()) + 1;
				categoryOccurrences.put(task.getCategory(), occurrences);
			}
		}
		
		addRecommendationsFromMap(categoryOccurrences);
	}
	
	
	
	/*
	 * Calculates and recommends the type of task (category) that is completed 
	 * most often at the time of the calculation and the current location.
	 */
	private void timeOfDayAndLocationRecommendation(long recommendationTime) {
		if (mLocationClient.isConnected()) {
			mLastKnownLocation = mLocationClient.getLastLocation();
		} else if (mLastKnownLocation == null) {
			return;
		}
		
		List<String> categories = readCategories();
		HashMap<String, Integer> categoryOccurrences = 
				new HashMap<String, Integer>();
		
		// Put the categories in a HashMap. This is used to count occurrences
		//  of what type of tasks happen at what times.
		for (String category : categories) {
			categoryOccurrences.put(category, 0);
		}
		
		long startTimeTask;
		long endTimeTask;
		for (Task task : mTaskHisory) {
			
			// Check if the task location is the same as the current location.
			// TODO Should use collected location context here instead...
			float[] result = new float[3];
			Location.distanceBetween(mLastKnownLocation.getLatitude(), 
					mLastKnownLocation.getLongitude(), 
					task.getLocation().latitude, 
					task.getLocation().longitude, result);
			float distance = result[0];
			
			if (distance <= MAXIMUM_DISTANCE_LOCATION_RECOMMENDATION &&
					task.getTimeStarted() > 0) {
				// Get start and end time since midnight of the task.
				startTimeTask = Utilities.getTimeOfDay(task.getTimeStarted());
				endTimeTask = Utilities.getTimeOfDay(task.getTimeEnded());
				
				if (startTimeTask < recommendationTime && 
						endTimeTask > recommendationTime) {
					int occurrences = categoryOccurrences
							.get(task.getCategory()) + 1;
					categoryOccurrences.put(task.getCategory(), occurrences);
				}
			}
		}
		
		addRecommendationsFromMap(categoryOccurrences);
	}
	
	
	
	private void timeOfDayAndDayOfWeekAndLocationRecommendation(
			long recommendationTime) {
		if (mLocationClient.isConnected()) {
			mLastKnownLocation = mLocationClient.getLastLocation();
		} else if (mLastKnownLocation == null) {
			return;
		}
		
		List<String> categories = readCategories();
		HashMap<String, Integer> categoryOccurrences = 
				new HashMap<String, Integer>();
		
		// Put the categories in a HashMap. This is used to count occurrences
		//  of what type of tasks happen at what times.
		for (String category : categories) {
			categoryOccurrences.put(category, 0);
		}
		
		int dayNow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
		long startTimeTask;
		long endTimeTask;
		for (Task task : mTaskHisory) {
			
			// Check if the task location is the same as the current location.
			// TODO Should use collected location context here instead...
			float[] result = new float[3];
			Location.distanceBetween(mLastKnownLocation.getLatitude(), 
					mLastKnownLocation.getLongitude(), 
					task.getLocation().latitude, 
					task.getLocation().longitude, result);
			float distance = result[0];
			
			if (distance <= MAXIMUM_DISTANCE_LOCATION_RECOMMENDATION &&
					task.getTimeStarted() > 0) {
				// Get start and end time since midnight of the task.
				int dayTask = Utilities.getDayOfWeek(task.getTimeStarted());
				startTimeTask = Utilities.getTimeOfDay(task.getTimeStarted());
				endTimeTask = Utilities.getTimeOfDay(task.getTimeEnded());
				
				if (startTimeTask < recommendationTime && 
						endTimeTask > recommendationTime &&
						dayTask == dayNow) {
					int occurrences = categoryOccurrences
							.get(task.getCategory()) + 1;
					categoryOccurrences.put(task.getCategory(), occurrences);
				}
			}
		}
		
		addRecommendationsFromMap(categoryOccurrences);
	}
	
	
	
	private List<String> readCategories() {
		List<String> categories;
		
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		String occupationPref = sharedPref.getString(
				SettingsActivity.PREF_OCCUPATION_KEY, "");
		if (occupationPref.equals(getString(R.string.pref_undergraduate))) {
			categories = Arrays.asList(getResources()
					.getStringArray(R.array.undergraduate_tasks_array));
		} else {
			categories = Arrays.asList(getResources()
					.getStringArray(R.array.postgraduate_tasks_array));
		}
		
		return categories;
	}
	
	
	
	private void addRecommendationsFromMap(HashMap<String, Integer> map) {
		float total = 0;
		for (Entry<String, Integer> entry : map.entrySet()) {
			total += entry.getValue();
		}
		
		for (Entry<String, Integer> entry : map.entrySet()) {
			float probability = (float) entry.getValue() / total;
			if (! mRecommendationMap.containsKey(entry.getKey()) ||
					mRecommendationMap.get(entry.getKey()) < probability) {
				mRecommendationMap.put(entry.getKey(), probability);
			}
		}
		
	}
	
	
	
	private long getAverageTimeSpentOnTask(String category) {
		long totalTimeSpent = 0;
		int numberOfTasks = 0;
		for (Task task : mTaskHisory) {
			if (task.getCategory().equals(category)) {
				numberOfTasks += 1;
				totalTimeSpent += task.getTimeSpent();
			}
		}
		return totalTimeSpent / numberOfTasks;
	}
	
	
	
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {}
	
	
	
	@Override
	public void onConnected(Bundle bundle) {
		mLastKnownLocation = mLocationClient.getLastLocation();
		
		recommend(mTimeOfCalculation);
		for (Task task : mRecommendedList) {
			Toast.makeText(this, task.getCategory() + ": " + task.getDescription(), Toast.LENGTH_LONG).show();
		}
		stopSelf();
	}
	
	
	
	@Override
	public void onDisconnected() {
		mLocationClient = null;
	}
	
	
	
	
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
}

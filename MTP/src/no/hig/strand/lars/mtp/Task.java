package no.hig.strand.lars.mtp;

import com.google.android.gms.maps.model.LatLng;

import android.os.Parcel;
import android.os.Parcelable;

public class Task implements Parcelable {

	private String mCategory;
	private String mDescription;
	private LatLng mLocation;
	private boolean mIsActive;
	private String mStartTime;
	private String mEndTime;
	private int mTimeSpent;
	
	
	public Task(String category, String description, LatLng location) {
		mCategory = category;
		mDescription = description;
		mLocation = location;
		mIsActive = false;
		mStartTime = "";
		mEndTime = "";
		mTimeSpent = 0;
	}
	
	
	
	public Task(Parcel in) {
		readFromParcel(in);
	}
	
	
	
	@Override
	public int describeContents() {
		return 0;
	}

	
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mCategory);
		dest.writeString(mDescription);
		dest.writeParcelable(mLocation, flags);
		dest.writeInt((int) (mIsActive ? 1 : 0));
		dest.writeString(mStartTime);
		dest.writeString(mEndTime);
		dest.writeInt(mTimeSpent);
	}
	
	
	
	private void readFromParcel(Parcel in) {
		mCategory = in.readString();
		mDescription = in.readString();
		mLocation = in.readParcelable(null);
		mIsActive = in.readInt() != 0;
		mStartTime = in.readString();
		mEndTime = in.readString();
		mTimeSpent = in.readInt();
	}
	
	
	
	public static final Parcelable.Creator<Task> CREATOR = 
			new Parcelable.Creator<Task>() {

				@Override
				public Task createFromParcel(Parcel source) {
					return new Task(source);
				}

				@Override
				public Task[] newArray(int size) {
					return new Task[size];
				}
		
		
	};
	
	
	
	public String getCategory() {
		return mCategory;
	}

	
	
	public String getDescription() {
		return mDescription;
	}

	
	
	public LatLng getLocation() {
		return mLocation;
	}

	
	
	public boolean isActive() {
		return mIsActive;
	}

	
	
	public String getStartTime() {
		return mStartTime;
	}

	
	
	public String getEndTime() {
		return mEndTime;
	}

	
	
	public int getTimeSpent() {
		return mTimeSpent;
	}

}
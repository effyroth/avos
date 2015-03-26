package com.avos.avoscloud;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.HashMap;
import java.util.Map;

class AnalyticsActivity
  implements Parcelable
{
  private AVDuration duration = new AVDuration();
  private String activityName;
  volatile boolean savedToServer;
  boolean isFragment = false;

  public static final Parcelable.Creator<AnalyticsActivity> CREATOR = new Parcelable.Creator()
  {
    public AnalyticsActivity createFromParcel(Parcel parcel)
    {
      return new AnalyticsActivity(parcel);
    }

    public AnalyticsActivity[] newArray(int i)
    {
      return new AnalyticsActivity[i];
    }
  };

  public AnalyticsActivity(String name)
  {
    this.activityName = name;
    this.savedToServer = false;
  }

  public AnalyticsActivity() {
    this("");
  }

  public void start() {
    this.duration.start();
  }

  public void stop() {
    this.duration.stop();
  }

  public void setDurationValue(long ms) {
    this.duration.setDuration(ms);
  }

  public double getStart() {
    return this.duration.getCreateTimeStamp();
  }

  public String getActivityName() {
    return this.activityName;
  }

  public boolean isStopped() {
    return this.duration.isStopped();
  }

  long myDuration() {
    return this.duration.getActualDuration();
  }

  public Map<String, Object> jsonMap() {
    Map map = new HashMap();
    map.put("name", this.activityName);
    map.put("du", Long.valueOf(myDuration()));
    map.put("ts", Long.valueOf(this.duration.getCreateTimeStamp()));
    return map;
  }

  protected AVDuration getDuration()
  {
    return this.duration;
  }

  protected void setDuration(AVDuration duration) {
    this.duration = duration;
  }

  protected void setActivityName(String activityName) {
    this.activityName = activityName;
  }

  protected boolean isSavedToServer() {
    return this.savedToServer;
  }

  protected void setSavedToServer(boolean savedToServer) {
    this.savedToServer = savedToServer;
  }

  protected boolean isFragment() {
    return this.isFragment;
  }

  protected void setFragment(boolean isFragment) {
    this.isFragment = isFragment;
  }

  public int describeContents()
  {
    return 0;
  }

  public void writeToParcel(Parcel parcel, int i)
  {
    parcel.writeParcelable(this.duration, 1);
    parcel.writeString(this.activityName);
    parcel.writeInt(this.savedToServer ? 1 : 0);
    parcel.writeInt(this.isFragment ? 1 : 0);
  }

  public AnalyticsActivity(Parcel in) {
    this.duration = ((AVDuration)in.readParcelable(AVDuration.class.getClassLoader()));
    this.activityName = in.readString();
    this.savedToServer = (in.readInt() == 1);
    this.isFragment = (in.readInt() == 1);
  }
}
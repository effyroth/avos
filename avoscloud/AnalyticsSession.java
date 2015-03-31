package com.avos.avoscloud;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class AnalyticsSession
  implements Parcelable
{
  private List<AnalyticsActivity> activities;
  private List<AnalyticsEvent> events;
  private String sessionId = "";
  private AVDuration duration = new AVDuration();
  private static final String sessionIdTag = "sessionId";
  private static final String TAG = AnalyticsSession.class.getSimpleName();

  public static final Creator<AnalyticsSession> CREATOR = new Creator()
  {
    public AnalyticsSession createFromParcel(Parcel parcel)
    {
      return new AnalyticsSession(parcel);
    }

    public AnalyticsSession[] newArray(int i)
    {
      return new AnalyticsSession[i];
    }
  };

  public AnalyticsSession()
  {
    this.activities = new ArrayList();
    this.events = new ArrayList();
  }

  public void beginSession() {
    this.sessionId = AnalyticsUtils.uniqueId();
    this.duration.start();
  }

  public void endSession() {
    if (AVUtils.isBlankString(this.sessionId)) {
      return;
    }
    for (AnalyticsActivity a : getActivities()) {
      if (!a.isStopped()) {
        a.stop();
      }
    }
    for (AnalyticsEvent e : getEvents()) {
      if (!e.getDuration().isStopped()) {
        e.stop();
      }
    }
    this.duration.stop();
  }

  public boolean isSessionFinished() {
    return this.duration.isStopped();
  }

  public void pauseSession() {
    this.duration.pause();
  }

  public long getSessionStart() {
    return this.duration.getCreateTimeStamp();
  }

  public String getSessionId() {
    return this.sessionId;
  }

  public void setSessionDuration(long ms) {
    this.duration.setDuration(ms);
  }

  public AnalyticsActivity activityByName(String name, boolean create) {
    for (AnalyticsActivity activity : this.activities) {
      if ((activity.getActivityName().equalsIgnoreCase(name)) && (!activity.isStopped())) {
        return activity;
      }
    }

    AnalyticsActivity activity = null;
    if (create) {
      activity = new AnalyticsActivity(name);
      this.activities.add(activity);
    }
    return activity;
  }

  public AnalyticsEvent eventByName(String name, String label, String key, boolean create) {
    for (AnalyticsEvent event : this.events) {
      if (event.isMatch(name, label, key)) {
        return event;
      }
    }

    AnalyticsEvent event = null;
    if (create) {
      event = new AnalyticsEvent(name);
      event.setLabel(label);
      event.setPrimaryKey(key);
      this.events.add(event);
    }
    return event;
  }

  public void addActivity(String name, long ms) {
    AnalyticsActivity activity = activityByName(name, true);
    activity.setDurationValue(ms);
  }

  public void beginActivity(String name) {
    AnalyticsActivity activity = activityByName(name, true);
    activity.start();
    activity.setSavedToServer(false);
    this.duration.resume();
  }

  public void beginFragment(String name) {
    AnalyticsActivity fragment = activityByName(name, true);
    fragment.setFragment(true);
    fragment.start();
    this.duration.resume();
  }

  public AnalyticsEvent beginEvent(Context context, String name, String label, String key) {
    AnalyticsEvent event = eventByName(name, label, key, true);
    if (!AVUtils.isBlankString(label)) {
      event.setLabel(label);
    }
    event.start();
    this.duration.resume();
    return event;
  }

  public void endEvent(Context context, String name, String label, String key) {
    AnalyticsEvent event = eventByName(name, label, key, false);
    if (event == null) {
      return;
    }
    event.stop();
  }

  public void endActivity(String name) {
    AnalyticsActivity activity = activityByName(name, false);
    if (activity == null)
    {
      Log.i("", "Please call begin activity before using endActivity");
      return;
    }
    activity.setSavedToServer(false);
    activity.stop();
  }

  public void endFragment(String name) {
    AnalyticsActivity fragment = activityByName(name, false);
    if (fragment == null)
    {
      Log.i("", "Please call begin Fragment before using endFragment");
      return;
    }
    fragment.stop();
  }

  public Map<String, Object> launchMap()
  {
    Map map = new HashMap();
    map.put("sessionId", this.sessionId);
    map.put("date", Long.valueOf(this.duration.getCreateTimeStamp()));
    return map;
  }

  public Map<String, Object> activitiesMap(boolean cleanUpAnalysisData)
  {
    List array = new LinkedList();
    long activitiesDuration = 0L;
    for (AnalyticsActivity a : this.activities) {
      synchronized (a) {
        if ((a.isStopped()) && (!a.isSavedToServer())) {
          array.add(a.jsonMap());
          if (cleanUpAnalysisData) {
            a.setSavedToServer(true);
          }
        }
      }
      if (!a.isFragment) {
        activitiesDuration += a.myDuration();
      }
    }

    Map map = new HashMap();
    map.put("activities", array);
    map.put("sessionId", this.sessionId);
    map.put("duration", Long.valueOf(getDuration().getActualDuration()));
    return map;
  }

  public List<Object> eventArray(boolean cleanUpAnalysisData) {
    List array = new LinkedList();
    List toDelete = new LinkedList();
    for (AnalyticsEvent e : this.events) {
      if (e.getDuration().isStopped()) {
        array.add(e.jsonMap());
        toDelete.add(e);
      }
    }
    if (cleanUpAnalysisData) {
      this.events.removeAll(toDelete);
    }
    return array;
  }

  protected boolean hasNewData() {
    for (AnalyticsActivity a : this.activities) {
      if ((a.isStopped()) && (!a.isSavedToServer())) {
        return true;
      }
    }
    for (AnalyticsEvent e : this.events) {
      if (e.getDuration().isStopped()) {
        return true;
      }
    }
    return false;
  }

  protected int getMessageCount() {
    int messageCount = 0;
    for (AnalyticsActivity a : this.activities) {
      if ((a.isStopped()) && (!a.isSavedToServer()))
        messageCount += 2;
      else if ((!a.isSavedToServer()) && (!a.isStopped())) {
        messageCount++;
      }
    }
    for (AnalyticsEvent e : this.events) {
      if (e.getDuration().isStopped())
        messageCount += 2;
      else {
        messageCount++;
      }
    }
    return messageCount;
  }

  public Map<String, Object> jsonMap(Context context, Map<String, String> customInfo, boolean cleanUpAnalysisData)
  {
    if (hasNewData()) {
      Map result = new HashMap();
      Map events = new HashMap();
      events.put("launch", launchMap());
      events.put("terminate", activitiesMap(cleanUpAnalysisData));
      events.put("event", eventArray(cleanUpAnalysisData));

      result.put("events", events);
      Map devInfo = AnalyticsUtils.deviceInfo(context);
      result.put("device", devInfo);
      if (customInfo != null) {
        result.put("customInfo", customInfo);
      }
      return result;
    }

    return null;
  }

  protected Map<String, Object> firstBootMap(Context context, Map<String, String> customInfo)
  {
    Map result = new HashMap();
    Map events = new HashMap();
    events.put("launch", launchMap());
    events.put("terminate", activitiesMap(false));
    result.put("events", events);
    Map devInfo = AnalyticsUtils.deviceInfo(context);
    result.put("device", devInfo);
    if (customInfo != null) {
      result.put("customInfo", customInfo);
    }
    return result;
  }

  protected List<AnalyticsActivity> getActivities()
  {
    return this.activities;
  }

  protected void setActivities(List<AnalyticsActivity> activities) {
    this.activities = activities;
  }

  protected List<AnalyticsEvent> getEvents() {
    return this.events;
  }

  protected void setEvents(List<AnalyticsEvent> events) {
    this.events = events;
  }

  protected AVDuration getDuration()
  {
    return this.duration;
  }

  protected void setDuration(AVDuration duration) {
    this.duration = duration;
  }

  protected void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public int describeContents()
  {
    return 0;
  }

  public void writeToParcel(Parcel out, int i)
  {
    out.writeParcelableArray((Parcelable[])this.activities.toArray(new AnalyticsActivity[0]), 1);

    out.writeParcelableArray((Parcelable[])this.events.toArray(new AnalyticsEvent[0]), 1);
    out.writeParcelable(this.duration, 1);
    out.writeString(this.sessionId);
  }

  public AnalyticsSession(Parcel in) {
    this();
    Parcelable[] parcelActivities = in.readParcelableArray(AnalyticsActivity.class.getClassLoader());

    Parcelable[] parcelEvents = in.readParcelableArray(AnalyticsEvent.class.getClassLoader());
    for (Parcelable activity : parcelActivities) {
      this.activities.add((AnalyticsActivity)activity);
    }

    for (Parcelable event : parcelEvents) {
      this.events.add((AnalyticsEvent)event);
    }
    this.duration = ((AVDuration)in.readParcelable(AVDuration.class.getClassLoader()));
    this.sessionId = in.readString();
  }
}
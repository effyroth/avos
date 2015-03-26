package com.avos.avoscloud;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.HashMap;
import java.util.Map;

class AnalyticsEvent
  implements Parcelable
{
  private AVDuration duration = new AVDuration();
  private Map<String, Object> attributes;
  private String eventName;
  private String labelName;
  private String primaryKey;
  private int accumulation;
  public static final String eventTag = "name";
  public static final String labelTag = "tag";
  public static final String accTag = "acc";
  public static final String primaryKeyTag = "primaryKey";
  public static final String attributesTag = "attributes";
  public static final Parcelable.Creator<AnalyticsEvent> CREATOR = new Parcelable.Creator()
  {
    public AnalyticsEvent createFromParcel(Parcel parcel)
    {
      return new AnalyticsEvent(parcel);
    }

    public AnalyticsEvent[] newArray(int i)
    {
      return new AnalyticsEvent[i];
    }
  };

  public AnalyticsEvent(String name)
  {
    this.eventName = name;
    this.attributes = new HashMap();
    this.accumulation = 1;
  }

  public AnalyticsEvent() {
    this("");
  }

  public void start() {
    this.duration.start();
  }

  public void stop() {
    this.duration.stop();
  }

  public String getEventName() {
    return this.eventName;
  }

  public void setDurationValue(long ms) {
    this.duration.setDuration(ms);
  }

  public void setAccumulation(int acc) {
    if (acc > 0)
      this.accumulation = acc;
  }

  public void setLabel(String label)
  {
    this.labelName = label;
  }

  public void setPrimaryKey(String key) {
    this.primaryKey = key;
  }

  public void addAttributes(Map<String, String> map) {
    if (map != null)
      this.attributes.putAll(map);
  }

  public boolean isMatch(String name, String label, String key)
  {
    if (!this.eventName.equals(name)) {
      return false;
    }

    if (!AnalyticsUtils.isStringEqual(this.labelName, label)) {
      return false;
    }

    if (!AnalyticsUtils.isStringEqual(this.primaryKey, key)) {
      return false;
    }

    if (this.duration.isStopped()) {
      return false;
    }
    return true;
  }

  long myDuration() {
    return this.duration.getActualDuration();
  }

  public Map<String, Object> jsonMap() {
    Map map = new HashMap();
    map.put("name", this.eventName);
    if (!AVUtils.isBlankString(this.labelName))
      map.put("tag", this.labelName);
    else {
      map.put("tag", this.eventName);
    }
    if (!AVUtils.isBlankString(this.primaryKey)) {
      map.put("primaryKey", this.primaryKey);
    }
    if (this.accumulation > 1) {
      map.put("acc", Integer.valueOf(this.accumulation));
    }
    if (this.attributes.size() > 0) {
      try {
        map.put("attributes", this.attributes);
      } catch (Exception exception) {
        exception.printStackTrace();
      }
    }
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

  protected Map<String, Object> getAttributes() {
    return this.attributes;
  }

  protected void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  protected String getLabelName() {
    return this.labelName;
  }

  protected void setLabelName(String labelName) {
    this.labelName = labelName;
  }

  protected String getPrimaryKey() {
    return this.primaryKey;
  }

  protected int getAccumulation() {
    return this.accumulation;
  }

  protected void setEventName(String eventName) {
    this.eventName = eventName;
  }

  public int describeContents()
  {
    return 0;
  }

  public void writeToParcel(Parcel parcel, int i)
  {
    parcel.writeParcelable(this.duration, 1);
    parcel.writeMap(this.attributes);
    parcel.writeString(this.eventName);
    parcel.writeString(this.labelName);
    parcel.writeString(this.primaryKey);
    parcel.writeInt(this.accumulation);
  }

  public AnalyticsEvent(Parcel in) {
    this.duration = ((AVDuration)in.readParcelable(AnalyticsEvent.class.getClassLoader()));
    this.attributes = in.readHashMap(Map.class.getClassLoader());
    this.eventName = in.readString();
    this.labelName = in.readString();
    this.primaryKey = in.readString();
    this.accumulation = in.readInt();
  }
}
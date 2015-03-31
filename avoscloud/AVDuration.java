package com.avos.avoscloud;

import android.os.Parcel;
import android.os.Parcelable;

class AVDuration
  implements Parcelable
{
  private long createTimeStamp;
  private long resumeTimeStamp;
  private long pausedTimeStamp;
  private long duration;
  private boolean stopped;
  public static final Creator<AVDuration> CREATOR = new Creator()
  {
    public AVDuration createFromParcel(Parcel parcel)
    {
      return new AVDuration(parcel);
    }

    public AVDuration[] newArray(int i)
    {
      return new AVDuration[i];
    }
  };

  public AVDuration()
  {
  }

  public long getCreateTimeStamp()
  {
    return this.createTimeStamp;
  }

  public long getActualDuration()
  {
    long tempDuration = this.duration + getLastTimeInterval();
    if (tempDuration < 0L) {
      if (AVOSCloud.showInternalDebugLog()) {
        LogUtil.avlog.d("Negative duration " + tempDuration);
      }
      tempDuration = 0L;
    }
    return tempDuration;
  }

  public long getDuration() {
    return this.duration;
  }

  public void start() {
    this.stopped = false;
    this.createTimeStamp = currentTS();
    this.resumeTimeStamp = this.createTimeStamp;
    this.pausedTimeStamp = -1L;
  }

  public void stop() {
    sync();
    this.stopped = true;
  }

  public boolean isStopped() {
    return this.stopped;
  }

  public void resume() {
    if (this.stopped) {
      return;
    }
    sync();
    this.resumeTimeStamp = currentTS();
  }

  public void pause() {
    this.pausedTimeStamp = currentTS();
  }

  public void setDuration(long ms) {
    this.duration = ms;
  }

  public void addDuration(long ms)
  {
    this.duration += ms;
  }

  public void sync()
  {
    if (this.stopped) {
      return;
    }
    this.duration += getLastTimeInterval();
    this.pausedTimeStamp = -1L;
  }

  private long getLastTimeInterval() {
    long d = 0L;

    if (this.pausedTimeStamp > this.resumeTimeStamp)
      d = this.pausedTimeStamp - this.resumeTimeStamp;
    else if (!this.stopped)
    {
      d = currentTS() - this.resumeTimeStamp;
    }
    return d;
  }

  public static long currentTS() {
    return System.currentTimeMillis();
  }

  protected long getResumeTimeStamp() {
    return this.resumeTimeStamp;
  }

  protected void setResumeTimeStamp(long resumeTimeStamp) {
    this.resumeTimeStamp = resumeTimeStamp;
  }

  protected void setCreateTimeStamp(long createTimeStamp) {
    this.createTimeStamp = createTimeStamp;
  }

  protected void setStopped(boolean stopped) {
    this.stopped = stopped;
  }

  public long getPausedTimeStamp() {
    return this.pausedTimeStamp;
  }

  public void setPausedTimeStamp(long pausedTimeStamp) {
    this.pausedTimeStamp = pausedTimeStamp;
  }

  public int describeContents()
  {
    return 0;
  }

  public void writeToParcel(Parcel parcel, int i)
  {
    parcel.writeLong(this.createTimeStamp);
    parcel.writeLong(this.resumeTimeStamp);
    parcel.writeLong(this.pausedTimeStamp);
    parcel.writeLong(this.duration);
    parcel.writeInt(this.stopped ? 1 : 0);
  }

  public AVDuration(Parcel in) {
    this.createTimeStamp = in.readLong();
    this.resumeTimeStamp = in.readLong();
    this.pausedTimeStamp = in.readLong();
    this.duration = in.readLong();
    this.stopped = (in.readInt() == 1);
  }
}
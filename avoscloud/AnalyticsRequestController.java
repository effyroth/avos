package com.avos.avoscloud;

import android.os.HandlerThread;
import android.os.Message;

abstract class AnalyticsRequestController
{
  static HandlerThread controllerThread = new HandlerThread("com.avos.avoscloud.AnalyticsRequestController");

  public void requestToSend(String currentSessionId)
  {
  }

  public void quit()
  {
  }

  public boolean requestValidate(Message message)
  {
    return true;
  }

  public void onRequestDone()
  {
  }

  public void appraisalSession(AnalyticsSession session)
  {
  }

  static
  {
    controllerThread.start();
  }

  static abstract interface AnalyticsRequestDispatcher
  {
    public abstract void sendRequest();
  }
}
package com.avos.avoscloud;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

abstract class BasicAnalyticsRequestDispatcher extends AnalyticsRequestController
{
  Handler asyncHandler;

  BasicAnalyticsRequestDispatcher(final AnalyticsRequestDispatcher dispatcher)
  {
    this.asyncHandler = new Handler(controllerThread.getLooper())
    {
      public void handleMessage(Message msg) {
        if ((dispatcher != null) && (BasicAnalyticsRequestDispatcher.this.requestValidate(msg))) {
          BasicAnalyticsRequestDispatcher.this.prepareRequest();
          dispatcher.sendRequest();
        }
        BasicAnalyticsRequestDispatcher.this.onRequestDone();
      }
    };
  }

  public void prepareRequest() {
  }

  public boolean requestValidate(Message message) {
    return true;
  }

  public void onRequestDone()
  {
  }
}
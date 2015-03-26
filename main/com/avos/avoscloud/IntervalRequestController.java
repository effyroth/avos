package com.avos.avoscloud;

import android.os.HandlerThread;
import android.os.Looper;

class IntervalRequestController extends BoosterRequestController
{
  IntervalTimer timer;

  public IntervalRequestController(String sessionId, AnalyticsRequestController.AnalyticsRequestDispatcher dispatcher, long countDownInterval)
  {
    super(sessionId, dispatcher);
    this.timer = new IntervalTimer(AnalyticsRequestController.controllerThread.getLooper(), countDownInterval)
    {
      public void onTrigger()
      {
        if (this.val$dispatcher != null) {
          if (AVOSCloud.isDebugLogEnabled()) {
            LogUtil.avlog.d("send stats interval request");
          }
          this.val$dispatcher.sendRequest();
        }
      }
    };
    this.timer.start();
  }

  public void quit() {
    this.timer.cancel();
  }

  public final void skip() {
    this.timer.skip();
  }

  public void prepareRequest()
  {
    if (AVOSCloud.isDebugLogEnabled())
      LogUtil.avlog.d("send stats interval request for new session");
  }

  public void onRequestDone()
  {
    this.currentSessionId = this.tmpSessionId;
    skip();
  }
}
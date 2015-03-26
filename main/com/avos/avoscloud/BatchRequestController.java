package com.avos.avoscloud;

import android.os.Handler;
import android.os.Message;
import java.util.concurrent.atomic.AtomicInteger;

class BatchRequestController extends IntervalRequestController
{
  private final AtomicInteger messageCount;
  private static int messageCountThreshold = 60;

  BatchRequestController(String sessionId, AnalyticsRequestController.AnalyticsRequestDispatcher dispatcher, long defaultInterval)
  {
    super(sessionId, dispatcher, defaultInterval);
    this.messageCount = new AtomicInteger(0);
  }

  public int getMessageCount() {
    return this.messageCount.get();
  }

  public int incMessageCount() {
    return this.messageCount.incrementAndGet();
  }

  protected void resetMessageCount()
  {
    resetMessageCount(0);
  }

  protected void resetMessageCount(int i) {
    this.messageCount.set(i);
  }

  public void prepareRequest()
  {
    if ((AVOSCloud.isDebugLogEnabled()) && (AnalyticsImpl.enableDebugLog))
      LogUtil.avlog.d("send stats batch request");
  }

  public void requestToSend(String sessionId)
  {
    int count = incMessageCount();
    Message message = new Message();
    message.obj = sessionId;
    message.what = count;
    this.asyncHandler.sendMessage(message);
  }

  public boolean requestValidate(Message msg)
  {
    return (super.requestValidate(msg)) || (msg.what >= messageCountThreshold);
  }

  public void appraisalSession(AnalyticsSession session)
  {
    if (session == null)
      resetMessageCount();
    else
      resetMessageCount(session.getMessageCount());
  }
}
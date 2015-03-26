package com.avos.avoscloud;

import android.os.Handler;
import android.os.Message;

class BoosterRequestController extends BasicAnalyticsRequestDispatcher
{
  String currentSessionId;
  String tmpSessionId;

  public BoosterRequestController(String sessionId, AnalyticsRequestController.AnalyticsRequestDispatcher dispatcher)
  {
    super(dispatcher);
    this.currentSessionId = sessionId;
  }

  private Message makeMessage(String sessionId)
  {
    Message msg = new Message();
    msg.obj = sessionId;
    return msg;
  }

  public void requestToSend(String sessionId)
  {
    this.asyncHandler.sendMessage(makeMessage(sessionId));
  }

  public void quit()
  {
  }

  public void prepareRequest()
  {
    if ((AVOSCloud.isDebugLogEnabled()) && (AnalyticsImpl.enableDebugLog))
      LogUtil.avlog.d("sent analytics request on booster");
  }

  public boolean requestValidate(Message message)
  {
    this.tmpSessionId = ((String)message.obj);
    return (!AVUtils.isBlankString(this.currentSessionId)) && (!this.currentSessionId.equals(this.tmpSessionId)) && (super.requestValidate(message));
  }

  public void onRequestDone()
  {
    this.currentSessionId = this.tmpSessionId;
  }
}
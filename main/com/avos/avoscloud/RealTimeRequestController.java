package com.avos.avoscloud;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

class RealTimeRequestController extends BasicAnalyticsRequestDispatcher
{
  static final int REQUEST_FOR_SEND = 19141010;
  static final int REQUEST_END_SEND = 20141010;
  private final Handler reportRequestDispatcher = new Handler(Looper.getMainLooper())
  {
    boolean hasRequestForSend = false;
    boolean hasRequestSending = false;

    public void handleMessage(Message msg)
    {
      switch (msg.what) {
      case 19141010:
        if (this.hasRequestSending)
        {
          this.hasRequestForSend = true;
        }
        else {
          RealTimeRequestController.this.asyncHandler.sendEmptyMessage(19141010);
          this.hasRequestSending = true;
        }
        break;
      case 20141010:
        if (this.hasRequestForSend)
        {
          RealTimeRequestController.this.asyncHandler.sendEmptyMessage(19141010);
          this.hasRequestForSend = false;
          this.hasRequestSending = true;
        } else {
          this.hasRequestSending = false;
        }
        break;
      }
    }
  };

  public RealTimeRequestController(AnalyticsRequestController.AnalyticsRequestDispatcher dispatcher)
  {
    super(dispatcher);
  }

  public void prepareRequest()
  {
    if ((AVOSCloud.isDebugLogEnabled()) && (AnalyticsImpl.enableDebugLog))
      LogUtil.avlog.d("sent real time analytics request");
  }

  public void requestToSend(String currentSessionId)
  {
    this.reportRequestDispatcher.sendMessage(makeMessage());
  }

  public Message makeMessage() {
    Message msg = new Message();
    msg.what = 19141010;
    return msg;
  }

  public boolean requestValidate(Message msg)
  {
    return (super.requestValidate(msg)) && (msg.what == 19141010);
  }

  public void onRequestDone()
  {
    this.reportRequestDispatcher.sendEmptyMessage(20141010);
  }

  public void quit()
  {
  }
}
package com.avos.avoscloud;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

class AnalyticsSessionCacheRepository
{
  private static final int CACHE_REQUEST = 1;
  private static final String SESSION_KEY = "session.key";
  private static final String SESSION_CACHE_FILENAME = "avoscloud-analysis";
  Handler sessionCacheHandler;
  HandlerThread handlerThread;
  static AnalyticsSessionCacheRepository instance = null;

  private AnalyticsSessionCacheRepository()
  {
    this.handlerThread = new HandlerThread("com.avos.avoscloud.AnalyticsCacheHandlerThread");
    this.handlerThread.start();
    this.sessionCacheHandler = new Handler(this.handlerThread.getLooper())
    {
      public void handleMessage(Message message)
      {
        Bundle bundle = message.getData();
        String sessionId = bundle.getString("session.key");
        try {
          if ((!AVUtils.isBlankString(sessionId)) && (message.what == 1)) {
            byte[] sessionData = message.obj == null ? null : AnalyticsSessionCacheRepository.marshall((Parcelable)message.obj);

            File cacheFile = AnalyticsSessionCacheRepository.getSessionCacheFile();
            if ((sessionData != null) && (sessionData.length > 0))
            {
              AVPersistenceUtils.saveContentToFile(sessionData, cacheFile);
            }
            else
              cacheFile.delete();
          }
        }
        catch (Exception e)
        {
        }
      }
    };
  }

  public static AnalyticsSessionCacheRepository getInstance()
  {
    if (instance == null) {
      instance = new AnalyticsSessionCacheRepository();
    }
    return instance;
  }

  void cacheSession(AnalyticsSession session) {
    this.sessionCacheHandler.sendMessage(getCacheRequestMessage(1, session.getSessionId(), session));
  }

  AnalyticsSession getCachedSession()
  {
    byte[] data = AVPersistenceUtils.readContentBytesFromFile(getSessionCacheFile());
    if ((data != null) && (data.length > 0)) {
      AnalyticsSession lastSession = new AnalyticsSession(unMarshall(data));
      lastSession.endSession();
      return lastSession;
    }
    return null;
  }

  static Message getCacheRequestMessage(int code, String sessionId, AnalyticsSession data)
  {
    Message message = new Message();
    message.what = code;
    Bundle bundle = new Bundle();
    bundle.putString("session.key", sessionId);
    if (data != null) {
      message.obj = data;
    }
    message.setData(bundle);

    return message;
  }

  private static byte[] marshall(Parcelable parcelable) {
    Parcel outer = Parcel.obtain();
    parcelable.writeToParcel(outer, 0);
    byte[] data = outer.marshall();
    return data;
  }

  private static Parcel unMarshall(byte[] data) {
    Parcel parcel = Parcel.obtain();
    parcel.unmarshall(data, 0, data.length);
    parcel.setDataPosition(0);
    return parcel;
  }

  private static File getSessionCacheFile() {
    return new File(AVPersistenceUtils.getAnalysisCacheDir(), "avoscloud-analysis");
  }
}
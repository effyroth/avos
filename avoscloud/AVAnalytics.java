package com.avos.avoscloud;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.util.Log;

import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.Map;

public class AVAnalytics
{
  private static final String NEW_CHANNEL_ID = "leancloud";
  private static final String OLD_CHANNEL_ID = "Channel ID";
  public static final String TAG = AVAnalytics.class.getSimpleName();

  private static String endPoint = "statistics";
  private static String appOpen = "_appOpen";
  private static String appOpenWithPush = "_appOpenWithPush";
  private static final String defaultChannel = "AVOS Cloud";
  static AnalyticsImpl impl = AnalyticsImpl.getInstance();

  public static void trackAppOpened(Intent intent)
  {
    Map map = statisticsDictionary(appOpen);
    onEvent(AVOSCloud.applicationContext, "!AV!AppOpen", map);

    if ((intent != null) && (intent.getIntExtra("com.avoscloud.push", -1) == 1))
      trackPushOpened(intent);
  }

  @Deprecated
  public void setDefaultReportPolicy(Context ctx, ReportPolicy policy)
  {
    impl.setReportPolicy(policy);
  }

  private static void trackPushOpened(Intent intent) {
    Map map = statisticsDictionary(appOpenWithPush);
    onEvent(AVOSCloud.applicationContext, "!AV!PushOpen", map);
  }

  public static void setAppChannel(String channel)
  {
    if (AVUtils.isBlankString(channel)) {
      throw new IllegalArgumentException("Blank channel string.");
    }
    impl.setAppChannel(channel);
  }

  static String getAppChannel() {
    return impl.getAppChannel();
  }

  @Deprecated
  public static void SetCustomInfo(Map<String, String> customInfo)
  {
    impl.setCustomInfo(customInfo);
  }

  public static void setCustomInfo(Map<String, String> customInfo)
  {
    impl.setCustomInfo(customInfo);
  }

  public static Map<String, String> getCustomInfo()
  {
    return impl.getCustomInfo();
  }

  private static Map<String, String> statisticsDictionary(String event) {
    if (AVUtils.isBlankString(event)) {
      throw new IllegalArgumentException("Blank event string.");
    }
    Map map = new HashMap();
    map.put("event_id", event);
    map.put("channel", impl.getAppChannel());
    return map;
  }

  private static void postAnalytics(Map<String, Object> map) {
    try {
      String postData = AVUtils.jsonStringFromMapWithNull(map);
      PaasClient.statistisInstance().postObject(endPoint, postData, false, true, new GenericObjectCallback()
      {
        public void onSuccess(String content, AVException e)
        {
          LogUtil.log.d(content);
        }

        public void onFailure(Throwable error, String content)
        {
          LogUtil.log.e(content);
        }
      }
      , null, AVUtils.md5(postData));
    }
    catch (Exception e)
    {
      LogUtil.log.e(TAG, "post analytics data failed.", e);
    }
  }

  public static void start(Context context)
  {
    try
    {
      ApplicationInfo info = context.getPackageManager().getApplicationInfo(context.getPackageName(), 128);

      Bundle bundle = info.metaData;
      if (bundle != null) {
        String channel = info.metaData.get("Channel ID") == null ? null : String.valueOf(info.metaData.get("Channel ID"));

        String newChannel = info.metaData.get("leancloud") == null ? null : String.valueOf(info.metaData.get("leancloud"));

        if (!AVUtils.isBlankString(channel))
          impl.setAppChannel(channel);
        else if (!AVUtils.isBlankString(newChannel))
          impl.setAppChannel(newChannel);
        else {
          impl.setAppChannel("AVOS Cloud");
        }
      }
      impl.enableCrashReport(context, true);
      impl.flushLastSessions(context);
      impl.updateOnlineConfig(context);
      impl.beginSession();
      impl.reportFirstBoot(context);
    } catch (Exception exception) {
      LogUtil.log.e(TAG, "Start context failed.", exception);
    }
  }

  public static void onFragmentStart(String pageName)
  {
    if (AVUtils.isBlankString(pageName)) {
      throw new IllegalArgumentException("Blank page name string.");
    }
    impl.beginFragment(pageName);
  }

  public static void onFragmentEnd(String pageName)
  {
    if (AVUtils.isBlankString(pageName)) {
      throw new IllegalArgumentException("Blank page name string.");
    }
    impl.endFragment(pageName);
  }

  public static void setAutoLocation(boolean b) {
    impl.setAutoLocation(b);
  }

  public static void setSessionContinueMillis(long ms)
  {
    if (ms <= 0L) {
      throw new IllegalArgumentException("Invalid session continute milliseconds.");
    }
    impl.setSessionContinueMillis(ms);
  }

  public static void setDebugMode(boolean enable)
  {
    impl.setEnableDebugLog(enable);
  }

  public static void enableCrashReport(Context context, boolean enable)
  {
    impl.enableCrashReport(context, enable);
  }

  public static void onPause(Context context)
  {
    impl.endActivity(context.getClass().getSimpleName());
    impl.pauseSession();
  }

  public static void onResume(Context context)
  {
    onResume(context, "", "");
  }

  private static void onResume(Context context, String s, String s1)
  {
    String name = context.getClass().getSimpleName();
    if (impl.shouldRegardAsNewSession()) {
      impl.endSession(context);
      impl.beginSession();
      LogUtil.avlog.d("new session start when resume");
    }

    impl.beginActivity(name);
  }

  public static void onError(Context context)
  {
  }

  public static void onError(Context context, String s)
  {
  }

  public static void reportError(Context context, String s)
  {
  }

  public static void reportError(Context context, Throwable throwable)
  {
  }

  static void reportError(Context context, Map<String, Object> crashData, final SaveCallback callback)
  {
    Map map = AnalyticsUtils.deviceInfo(context);
    map.putAll(crashData);
    String jsonString = JSON.toJSONString(map);
    PaasClient.statistisInstance().postObject("stats/crash", jsonString, false, true, new GenericObjectCallback()
    {
      public void onSuccess(String content, AVException e)
      {
        if (AVAnalytics.impl.isEnableDebugLog()) {
          Log.i(AVAnalytics.TAG, "Save success: " + content);
        }
        if (callback != null)
          callback.done(null);
      }

      public void onFailure(Throwable error, String content)
      {
        if (AVAnalytics.impl.isEnableDebugLog()) {
          Log.i(AVAnalytics.TAG, "Save failed: " + content);
        }
        if (callback != null)
          callback.done(AVErrorUtils.createException(error, content));
      }
    }
    , null, AVUtils.md5(jsonString));
  }

  public static void flush(Context context)
  {
    impl.sendInstantRecordingRequest();
  }

  protected static void debugDump(Context context) {
    impl.debugDump(context);
  }

  public static void onEvent(Context context, String eventId)
  {
    onEvent(context, eventId, 1);
  }

  public static void onEvent(Context context, String eventId, int acc)
  {
    onEvent(context, eventId, "", acc);
  }

  public static void onEvent(Context context, String eventId, String label)
  {
    onEvent(context, eventId, label, 1);
  }

  public static void onEvent(Context context, String eventId, String label, int acc)
  {
    AnalyticsEvent event = impl.beginEvent(context, eventId, label, "");
    event.setDurationValue(0L);
    event.setAccumulation(acc);
    impl.endEvent(context, eventId, label, "");
  }

  public static void onEvent(Context context, String eventId, Map<String, String> stringHashMap)
  {
    AnalyticsEvent event = impl.beginEvent(context, eventId, "", "");
    event.addAttributes(stringHashMap);
    impl.endEvent(context, eventId, "", "");
  }

  public static void onEventDuration(Context context, String eventId, long msDuration)
  {
    onEventDuration(context, eventId, "", msDuration);
  }

  public static void onEventDuration(Context context, String eventId, String label, long msDuration)
  {
    onEventDuration(context, eventId, label, null, msDuration);
  }

  public static void onEventDuration(Context context, String eventId, Map<String, String> stringHashMap, long msDuration)
  {
    onEventDuration(context, eventId, "", stringHashMap, msDuration);
  }

  private static void onEventDuration(Context context, String eventId, String label, Map<String, String> stringHashMap, long msDuration)
  {
    AnalyticsEvent event = impl.beginEvent(context, eventId, label, "");
    event.addAttributes(stringHashMap);
    event.setDurationValue(msDuration);
    impl.endEvent(context, eventId, label, "");
  }

  public static void onEventBegin(Context context, String eventId)
  {
    onEventBegin(context, eventId, "");
  }

  public static void onEventEnd(Context context, String eventId)
  {
    impl.endEvent(context, eventId, "", "");
  }

  public static void onEventBegin(Context context, String eventId, String label)
  {
    impl.beginEvent(context, eventId, label, "");
  }

  public static void onEventEnd(Context context, String eventId, String label)
  {
    impl.endEvent(context, eventId, label, "");
  }

  public static void onKVEventBegin(Context context, String eventId, HashMap<String, String> stringStringHashMap, String primaryKey)
  {
    AnalyticsEvent event = impl.beginEvent(context, eventId, "", primaryKey);
    event.setPrimaryKey(primaryKey);
  }

  public static void onKVEventEnd(Context context, String eventId, String primaryKey)
  {
    impl.endEvent(context, eventId, "", primaryKey);
  }

  public static String getConfigParams(Context context, String key)
  {
    return getConfigParams(context, key, "");
  }

  public static String getConfigParams(Context ctx, String key, String defaultValue)
  {
    return impl.getConfigParams(key, defaultValue);
  }

  public static void updateOnlineConfig(Context context)
  {
    impl.updateOnlineConfig(context);
  }
  public void setGender(Context context, String gender) {
  }

  public void setAge(Context context, int i) {
  }

  public void setUserID(Context context, String s, String s1) {
  }

  public static void onKillProcess(Context context) {
  }

  @Deprecated
  public static void setReportPolicy(Context context, ReportPolicy policy) {
    if (policy == null) {
      throw new IllegalArgumentException("Null report policy.");
    }
    impl.setReportPolicy(policy);
  }

  public static void setOnlineConfigureListener(AVOnlineConfigureListener listener)
  {
    if (listener == null) {
      throw new IllegalArgumentException("Null AVOnlineConfigureListener.");
    }
    impl.setAVOnlineConfigureListener(listener);
  }

  public static void setAnalyticsEnabled(boolean enable)
  {
    impl.setAnalyticsEnabled(enable);
  }
}
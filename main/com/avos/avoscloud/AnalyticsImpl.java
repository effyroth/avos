package com.avos.avoscloud;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import com.alibaba.fastjson.JSON;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;

class AnalyticsImpl
  implements AnalyticsRequestController.AnalyticsRequestDispatcher
{
  static AnalyticsImpl instance;
  protected static boolean enableDebugLog = AVOSCloud.showInternalDebugLog();
  private String appChannel = "AVOS Cloud";
  private boolean autoLocation;
  private static final Map<String, AnalyticsSession> sessions = new ConcurrentHashMap();
  private String currentSessionId;
  private static long sessionThreshold = 30000L;

  private static final String TAG = AnalyticsImpl.class.getSimpleName();
  private AVUncaughtExceptionHandler handler = null;
  private AnalyticsOnlineConfig onlineConfig = null;
  private AVOnlineConfigureListener listener = null;
  private Map<String, String> customInfo;
  private volatile Timer updateOnlineConfigTimer = null;
  private static final String firstBootTag = "firstBoot";
  private static final List<String> whiteList = new LinkedList();

  static boolean analysisReportEnableFlag = true;
  AnalyticsRequestController requestController;
  RealTimeRequestController realTimeController;

  private AnalyticsImpl()
  {
    onlineConfig = new AnalyticsOnlineConfig(this);
    requestController = new BatchRequestController(currentSessionId, this, AnalyticsUtils.getRequestInterval());

    realTimeController = new RealTimeRequestController(this);
  }

  public static AnalyticsImpl getInstance()
  {
    if (instance == null) {
      instance = new AnalyticsImpl();
    }
    return instance;
  }

  public void setAutoLocation(boolean b) {
    autoLocation = b;
  }

  public boolean isAutoLocation() {
    return autoLocation;
  }

  public boolean isEnableStats() {
    return onlineConfig.isEnableStats();
  }

  public void setAppChannel(String channel) {
    appChannel = channel;
  }

  public String getAppChannel() {
    return appChannel;
  }

  public void setEnableDebugLog(boolean b) {
    enableDebugLog = b;
  }

  public boolean isEnableDebugLog() {
    return enableDebugLog;
  }

  public void enableCrashReport(Context context, boolean enable) {
    if ((enable) && (handler == null)) {
      handler = new AVUncaughtExceptionHandler(context);
    }
    if (handler != null)
      handler.enableCrashHanlder(enable);
  }

  public ReportPolicy getReportPolicy(Context context)
  {
    ReportPolicy value = onlineConfig.getReportPolicy();

    if ((value == ReportPolicy.REALTIME) && (whiteList.contains(AVOSCloud.applicationId))) {
      return ReportPolicy.REALTIME;
    }
    if ((value == ReportPolicy.REALTIME) && (!AnalyticsUtils.inDebug(context))) {
      return ReportPolicy.BATCH;
    }
    if ((value == ReportPolicy.SENDWIFIONLY) && (!AnalyticsUtils.inDebug(context))) {
      return ReportPolicy.BATCH;
    }
    return value;
  }

  protected void setReportPolicy(ReportPolicy p) {
    if (onlineConfig.setReportPolicy(p)) {
      if (requestController != null) {
        requestController.quit();
      }

      requestController = AnalyticsRequestControllerFactory.getAnalyticsRequestController(currentSessionId, getReportPolicy(AVOSCloud.applicationContext), this);

      AnalyticsSession session = getCurrentSession(false);
      if ((session != null) && ((requestController instanceof BatchRequestController)))
        ((BatchRequestController)requestController).resetMessageCount(session.getMessageCount());
    }
  }

  public void notifyOnlineConfigListener(JSONObject data)
  {
    if (listener != null)
      try {
        listener.onDataReceived(data);
      } catch (Exception e) {
        Log.e(TAG, "Notify online data received failed.", e);
      }
  }

  private AnalyticsSession getCurrentSession(boolean create)
  {
    AnalyticsSession session = sessionByName(currentSessionId);
    if (session != null) {
      return session;
    }
    if (!create) {
      return null;
    }
    session = createSession();
    currentSessionId = session.getSessionId();
    return session;
  }

  public void setSessionContinueMillis(long ms) {
    sessionThreshold = ms;
  }

  public void setSessionDuration(long ms) {
    AnalyticsSession session = getCurrentSession(true);
    session.setSessionDuration(ms);
  }

  static AnalyticsSession sessionByName(String sid)
  {
    if (sid == null) {
      return null;
    }
    return (AnalyticsSession)sessions.get(sid);
  }

  private AnalyticsSession createSession() {
    AnalyticsSession session = new AnalyticsSession();
    session.beginSession();
    if (session.getSessionId() != null) {
      sessions.put(session.getSessionId(), session);
    }
    return session;
  }

  protected void flushLastSessions(Context context)
  {
    AnalyticsSession cachedSession = AnalyticsSessionCacheRepository.getInstance().getCachedSession();

    if ((enableDebugLog) && (cachedSession != null)) {
      LogUtil.avlog.i("get cached sessions:" + cachedSession.getSessionId());
    }
    if (cachedSession != null) {
      sessions.put(cachedSession.getSessionId(), cachedSession);
    }
    sendInstantRecordingRequest();
  }

  public void beginSession() {
    AnalyticsSession session = sessionByName(currentSessionId);
    if (session == null) {
      session = createSession();
    }
    currentSessionId = session.getSessionId();
  }

  public void endSession(Context context) {
    AnalyticsSession session = sessionByName(currentSessionId);
    if (session == null) {
      return;
    }
    session.endSession();
    postRecording();
    currentSessionId = null;
  }

  public void pauseSession() {
    AnalyticsSession session = sessionByName(currentSessionId);
    if (session == null) {
      return;
    }
    session.pauseSession();
  }

  public void addActivity(String name, long ms) {
    getCurrentSession(true).addActivity(name, ms);
  }

  public void beginActivity(String name) {
    getCurrentSession(true).beginActivity(name);
    postRecording();
  }

  public void beginFragment(String name) {
    getCurrentSession(true).beginFragment(name);
    postRecording();
  }

  public void beginEvent(Context context, String name) {
    beginEvent(context, name, "", "");
  }

  public AnalyticsEvent beginEvent(Context context, String name, String label, String key) {
    AnalyticsSession session = getCurrentSession(true);
    AnalyticsEvent event = session.beginEvent(context, name, label, key);
    postRecording();
    return event;
  }

  public void endEvent(Context context, String name, String label, String key) {
    getCurrentSession(true).endEvent(context, name, label, key);
    postRecording();
  }

  public void setCustomInfo(Map<String, String> extensionInfo) {
    customInfo = extensionInfo;
  }

  public Map<String, String> getCustomInfo() {
    return customInfo;
  }

  private long getSessionTimeoutThreshold() {
    return sessionThreshold;
  }

  public boolean shouldRegardAsNewSession() {
    AnalyticsSession session = getCurrentSession(false);
    if (session == null) {
      return true;
    }
    long current = AnalyticsUtils.getCurrentTimestamp();
    long start = session.getDuration().getPausedTimeStamp();
    long delta = current - start;
    if ((delta > getSessionTimeoutThreshold()) && (start > 0L)) {
      return true;
    }
    return false;
  }

  public void endActivity(String name) {
    getCurrentSession(true).endActivity(name);
    postRecording();
  }

  public void endFragment(String name) {
    getCurrentSession(true).endFragment(name);
    postRecording();
  }

  private void dumpJsonMap(Context context) {
    for (AnalyticsSession session : sessions.values()) {
      Map map = session.jsonMap(context, customInfo, false);
      try {
        if (map != null) {
          String jsonString = JSONHelper.toJsonString(map);
          LogUtil.log.d(jsonString);
        }
      } catch (Exception exception) {
        LogUtil.log.e(TAG, "", exception);
      }
    }
  }

  public synchronized void report(Context context, boolean clear)
  {
    try
    {
      saveSessionsToServer(context);

      Iterator iter = sessions.entrySet().iterator();
      while (iter.hasNext()) {
        AnalyticsSession session = (AnalyticsSession)((Map.Entry)iter.next()).getValue();
        if (session.isSessionFinished()) {
          iter.remove();
        }
      }

      AnalyticsSession currentSession = getCurrentSession(false);
      if (requestController != null) {
        requestController.appraisalSession(currentSession);
      }
      if (clear)
        clearSessions();
    }
    catch (Exception e) {
      Log.e(TAG, "Send statstics report failed", e);
    }
  }

  public void debugDump(Context context) {
    if (!enableDebugLog) {
      return;
    }

    for (AnalyticsSession session : sessions.values()) {
      Map map = session.jsonMap(context, customInfo, false);
      Log.i(TAG, "json data: " + map);
    }
  }

  public void postRecording()
  {
    if (AVOSCloud.showInternalDebugLog()) {
      Log.d(TAG, "report policy:" + onlineConfig.getReportPolicy());
    }

    if (!isEnableStats()) return;
    if (requestController != null) {
      requestController.requestToSend(currentSessionId);
    }
    AnalyticsSession currentSession = getCurrentSession(false);
    archiveCurrentSession();
  }

  protected void archiveCurrentSession() {
    AnalyticsSession currentSession = sessionByName(currentSessionId);
    if (currentSession != null)
      AnalyticsSessionCacheRepository.getInstance().cacheSession(currentSession);
  }

  public void saveSessionsToServer(Context context)
  {
    try {
      sendArchivedRequests(true);
      for (AnalyticsSession session : sessions.values()) {
        Map map = session.jsonMap(context, customInfo, true);
        if (map != null) {
          String jsonString = JSON.toJSONString(map);
          if (enableDebugLog) {
            LogUtil.log.i(jsonString);
          }
          sendAnalysisRequest(jsonString, true, true, new GenericObjectCallback()
          {
            public void onSuccess(String content, AVException e)
            {
              if (AnalyticsImpl.enableDebugLog)
                Log.i(AnalyticsImpl.TAG, "Save success: " + content);
            }

            public void onFailure(Throwable error, String content)
            {
              if (AnalyticsImpl.enableDebugLog)
                Log.i(AnalyticsImpl.TAG, "Save failed: " + content);
            }
          });
        }
      }
    }
    catch (Exception e)
    {
      Log.e(TAG, "saveSessionsToServer failed.", e);
    }
  }

  public void clearSessions() {
    sessions.clear();
    currentSessionId = null;
  }

  public void setAVOnlineConfigureListener(AVOnlineConfigureListener listener) {
    this.listener = listener;
    if ((listener != null) && (updateOnlineConfigTimer == null)) {
      updateOnlineConfigTimer = new Timer(true);
      updateOnlineConfigTimer.schedule(new TimerTask()
      {
        public void run() {
          try {
            onlineConfig.update(null, false);
          } catch (Exception e) {
            Log.e(AnalyticsImpl.TAG, "update online config failed", e);
          }
        }
      }
      , 5000L, 5000L);
    }
  }

  protected void updateOnlineConfig(Context context)
  {
    try
    {
      if (onlineConfig != null) {
        if (enableDebugLog) {
          Log.d(TAG, "try to update statistics config from online data");
        }
        onlineConfig.update(context);
      }
    } catch (Exception e) {
      Log.e(TAG, "Update online config failed.", e);
    }
  }

  public void reportFirstBoot(Context context) {
    SharedPreferences sharedPref = context.getSharedPreferences("AVOSCloud-SDK", 0);

    boolean firstBoot = sharedPref.getBoolean("firstBoot", true);
    if (firstBoot) {
      sendInstantRecordingRequest();
      Map firstBootMap = getCurrentSession(false).firstBootMap(context, customInfo);
      if (firstBootMap != null) {
        if (enableDebugLog) {
          LogUtil.avlog.d("report data on first boot");
        }
        String jsonString = JSON.toJSONString(firstBootMap);
        sendAnalysisRequest(jsonString, false, true, null);
      }
      SharedPreferences.Editor editor = sharedPref.edit();
      editor.putBoolean("firstBoot", false);
      editor.commit();
    } else if (enableDebugLog) {
      LogUtil.avlog.d("no need to first boot report");
    }
  }

  protected void sendInstantRecordingRequest() {
    realTimeController.requestToSend(currentSessionId);
  }

  protected String getConfigParams(String key, String defaultValue) {
    String result = onlineConfig.getConfigParams(key);
    if (result == null) {
      return defaultValue;
    }
    return result;
  }

  protected static void sendAnalysisRequest(String jsonString, boolean sync, boolean eventually, GenericObjectCallback callback)
  {
    if (analysisReportEnableFlag)
      PaasClient.statistisInstance().postObject("stats/collect", jsonString, sync, eventually, callback, null, AVUtils.md5(jsonString));
  }

  protected synchronized void setAnalyticsEnabled(boolean enable)
  {
    analysisReportEnableFlag = enable;
  }

  protected synchronized void sendArchivedRequests(boolean sync) {
    if (analysisReportEnableFlag)
      PaasClient.statistisInstance().handleAllArchivedRequest(sync);
  }

  public void sendRequest()
  {
    report(AVOSCloud.applicationContext, false);
  }
}
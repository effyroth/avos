package com.avos.avoscloud;

import android.content.Context;
import android.os.Process;
import android.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AVUncaughtExceptionHandler
  implements Thread.UncaughtExceptionHandler
{
  private final Thread.UncaughtExceptionHandler defaultExceptionHandler;
  private boolean enabled = false;
  private final String LOG_TAG = AVUncaughtExceptionHandler.class.getSimpleName();
  private final Context context;
  private Thread brokenThread;
  private Throwable unhandledThrowable;

  public AVUncaughtExceptionHandler(Context c)
  {
    this.context = c;
    this.defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    Thread.setDefaultUncaughtExceptionHandler(this);
  }

  public void enableCrashHanlder(boolean e) {
    this.enabled = e;
  }

  public void uncaughtException(Thread t, Throwable e)
  {
    try {
      if (!this.enabled) {
        if (this.defaultExceptionHandler != null) {
          Log.w(this.LOG_TAG, "AVUncaughtExceptionHandler is disabled and fallback to default handler.");
          this.defaultExceptionHandler.uncaughtException(t, e);
        } else {
          Log.w(this.LOG_TAG, "AVUncaughtExceptionHandler is disabled and there is no default handler, good luck.");
        }

        return;
      }

      this.brokenThread = t;
      this.unhandledThrowable = e;

      Log.e(this.LOG_TAG, "AVUncaughtExceptionHandler caught a " + e.getClass().getSimpleName() + " exception ");

      handleException(this.unhandledThrowable, false, true);
    }
    catch (Throwable fatality)
    {
      if (this.defaultExceptionHandler != null)
        this.defaultExceptionHandler.uncaughtException(t, e);
    }
  }

  public void handleException(Throwable e, boolean endApplication)
  {
    handleException(e, false, endApplication);
  }

  public void handleException(Throwable e)
  {
    handleException(e, false, false);
  }

  private void handleException(Throwable e, boolean forceSilentReport, boolean endApplication)
  {
    if (!this.enabled) {
      return;
    }

    if (e == null) {
      e = new Exception("Report requested by developer");
    }

    Map map = crashData(this.context, e);
    AVAnalytics.reportError(this.context, map, null);

    if (endApplication)
      endApplication();
  }

  private String getStackTrace(Throwable throwable)
  {
    Writer result = new StringWriter();
    PrintWriter printWriter = new PrintWriter(result);

    Throwable cause = throwable;
    while (cause != null) {
      cause.printStackTrace(printWriter);
      cause = cause.getCause();
    }
    String stacktraceAsString = result.toString();
    printWriter.close();
    return stacktraceAsString;
  }

  private Map<String, Object> crashData(Context context, Throwable throwable) {
    Map crashReportData = new HashMap();
    try {
      crashReportData.put("reason", throwable.toString());
      crashReportData.put("stack_trace", getStackTrace(throwable));
      crashReportData.put("date", AVUtils.stringFromDate(new Date()));
      try
      {
        Class installationClass = Class.forName("com.avos.avoscloud.AVInstallation");
        Method getMethod = installationClass.getMethod("getCurrentInstallation", new Class[0]);
        Method getInstallationIdMethod = installationClass.getMethod("getInstallationId", new Class[0]);
        Object installation = getMethod.invoke(installationClass, new Object[0]);
        String installationId = (String)getInstallationIdMethod.invoke(installation, new Object[0]);
        crashReportData.put("installationId", installationId);
      }
      catch (Exception e) {
      }
      crashReportData.put("packageName", context.getPackageName());
      crashReportData.putAll(AnalyticsUtils.getDeviceInfo(context));
      crashReportData.put("memInfo", AnalyticsUtils.collectMemInfo());
      crashReportData.put("totalDiskSpace", Long.valueOf(AnalyticsUtils.getTotalInternalMemorySize()));
      crashReportData.put("availableDiskSpace", Long.valueOf(AnalyticsUtils.getAvailableInternalMemorySize()));
      crashReportData.put("appFilePath", AnalyticsUtils.getApplicationFilePath(context));
      crashReportData.put("ipAddress", AnalyticsUtils.getLocalIpAddress());
    } catch (RuntimeException e) {
      Log.e(this.LOG_TAG, "Error while retrieving crash data", e);
    }
    return crashReportData;
  }

  private void endApplication() {
    AVAnalytics.impl.pauseSession();
    AVAnalytics.impl.archiveCurrentSession();

    if (this.defaultExceptionHandler != null)
    {
      this.defaultExceptionHandler.uncaughtException(this.brokenThread, this.unhandledThrowable);
    } else {
      Log.e(this.LOG_TAG, this.context.getPackageName() + " fatal error : " + this.unhandledThrowable.getMessage(), this.unhandledThrowable);

      Process.killProcess(Process.myPid());
      System.exit(10);
    }
  }
}
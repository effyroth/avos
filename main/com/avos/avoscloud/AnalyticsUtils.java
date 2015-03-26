package com.avos.avoscloud;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AnalyticsUtils
{
  private static final String TAG = AnalyticsUtils.class.getSimpleName();

  static List<String> CELLPHONEBLACKLIST = Arrays.asList(new String[] { "d2spr" });
  private static final long sendIntervalInDebug = 15000L;
  private static final long sendIntervalInProd = 120000L;

  public static Map<String, String> getNetworkInfo(Context context)
  {
    ConnectivityManager cm = (ConnectivityManager)context.getSystemService("connectivity");

    Map map = new HashMap();
    NetworkInfo info = cm.getActiveNetworkInfo();
    if ((info == null) || (!info.isConnectedOrConnecting()) || (withinInBlackList())) {
      map.put("access_subtype", "offline");
      map.put("access", "offline");
      map.put("carrier", "");
    } else {
      map.put("access_subtype", info.getSubtypeName());
      map.put("access", cleanNetworkTypeName(info.getTypeName()));
      TelephonyManager manager = (TelephonyManager)context.getSystemService("phone");

      String carrierName = manager.getNetworkOperatorName();
      map.put("carrier", carrierName);
    }
    return map;
  }

  private static String cleanNetworkTypeName(String type) {
    if (AVUtils.isBlankString(type)) {
      return "offline";
    }
    String t = type.toUpperCase();
    if (t.contains("WIFI")) {
      return "WiFi";
    }
    if (type.contains("MOBILE")) {
      return "Mobile";
    }
    return type;
  }

  public static Map<String, Object> deviceInfo(Context context) {
    Map map = new HashMap();
    Map networkInfo = getNetworkInfo(context);
    if (networkInfo != null) {
      map.putAll(networkInfo);
    }
    Map deviceInfo = getDeviceInfo(context);
    if (deviceInfo != null) {
      map.putAll(deviceInfo);
    }
    return map;
  }

  public static long getAvailableInternalMemorySize() {
    File path = Environment.getDataDirectory();
    StatFs stat = new StatFs(path.getPath());
    long blockSize = stat.getBlockSize();
    long availableBlocks = stat.getAvailableBlocks();
    return availableBlocks * blockSize;
  }

  public static long getTotalInternalMemorySize() {
    File path = Environment.getDataDirectory();
    StatFs stat = new StatFs(path.getPath());
    long blockSize = stat.getBlockSize();
    long totalBlocks = stat.getBlockCount();
    return totalBlocks * blockSize;
  }

  public static Map<String, Object> getDeviceInfo(Context context) {
    Map map = new HashMap();

    String packageName = context.getApplicationContext().getPackageName();
    map.put("package_name", packageName);
    try {
      PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
      map.put("app_version", info.versionName);
      map.put("version_code", Integer.valueOf(info.versionCode));
      map.put("sdk_version", "Android v3.0");
    } catch (Exception exception) {
      exception.printStackTrace();
    }

    WindowManager wm = (WindowManager)context.getSystemService("window");
    Display display = wm.getDefaultDisplay();
    int width = display.getWidth();
    int height = display.getHeight();
    map.put("resolution", new StringBuilder().append("").append(width).append("*").append(height).toString());

    map.put("device_model", Build.MODEL);
    map.put("device_manufacturer", Build.MANUFACTURER);
    map.put("os_version", Build.VERSION.RELEASE);
    map.put("device_name", Build.DEVICE);
    map.put("device_brand", Build.BRAND);
    map.put("device_board", Build.BOARD);
    map.put("device_manuid", Build.FINGERPRINT);

    map.put("cpu", getCPUInfo());
    map.put("os", "Android");
    map.put("sdk_type", "Android");
    String macAddress = null;
    try {
      WifiManager wifiManager = (WifiManager)AVOSCloud.applicationContext.getSystemService("wifi");

      WifiInfo wInfo = wifiManager.getConnectionInfo();
      macAddress = wInfo.getMacAddress();
    } catch (Exception e) {
      if (AVOSCloud.showInternalDebugLog()) {
        LogUtil.avlog.d(new StringBuilder().append("failed to get wifi mac address").append(e).toString());
      }
    }
    String androidId = Settings.Secure.getString(context.getContentResolver(), "android_id");

    String deviceId = AVUtils.isBlankString(macAddress) ? androidId : AVUtils.md5(new StringBuilder().append(macAddress).append(androidId).toString());

    map.put("device_id", deviceId);
    try
    {
      Class installationClass = Class.forName("com.avos.avoscloud.AVInstallation");
      Method getMethod = installationClass.getMethod("getCurrentInstallation", new Class[0]);
      Method getInstallationIdMethod = installationClass.getMethod("getObjectId", new Class[0]);
      Object installation = getMethod.invoke(installationClass, new Object[0]);
      String installationId = (String)getInstallationIdMethod.invoke(installation, new Object[0]);
      map.put("iid", installationId);
    } catch (Exception e) {
    }
    long offset = TimeZone.getDefault().getRawOffset();

    AVUser loginedUser = AVUser.getCurrentUser();
    if ((loginedUser != null) && (!AVUtils.isBlankString(loginedUser.getObjectId())))
      map.put("uid", loginedUser.getObjectId());
    try
    {
      offset = TimeUnit.HOURS.convert(offset, TimeUnit.MILLISECONDS);
    } catch (NoSuchFieldError e) {
      offset /= 3600000L;
    }
    map.put("time_zone", Long.valueOf(offset));
    map.put("channel", AVAnalytics.getAppChannel());

    if ((!withinInBlackList()) && (AVOSCloud.applicationContext.checkCallingPermission("android.permission.READ_PHONE_STATE") == 0))
    {
      TelephonyManager manager = (TelephonyManager)context.getSystemService("phone");

      String imei = manager.getDeviceId();
      map.put("imei", imei);
    }
    return map;
  }

  public static String collectMemInfo()
  {
    StringBuilder meminfo = new StringBuilder();
    InputStream in = null;
    InputStreamReader reader = null;
    BufferedReader bufferedReader = null;
    try {
      List commandLine = new ArrayList();
      commandLine.add("dumpsys");
      commandLine.add("meminfo");
      commandLine.add(Integer.toString(android.os.Process.myPid()));

      java.lang.Process process = Runtime.getRuntime().exec((String[])commandLine.toArray(new String[commandLine.size()]));

      in = process.getInputStream();
      reader = new InputStreamReader(in);
      bufferedReader = new BufferedReader(reader, 8192);
      while (true)
      {
        String line = bufferedReader.readLine();
        if (line == null) {
          break;
        }
        meminfo.append(line);
        meminfo.append("\n");
      }
      AVPersistenceUtils.closeQuietly(bufferedReader);
      AVPersistenceUtils.closeQuietly(reader);
      AVPersistenceUtils.closeQuietly(in);

      in = process.getErrorStream();
      reader = new InputStreamReader(in);
      bufferedReader = new BufferedReader(reader, 8192);

      StringBuilder errorInfo = new StringBuilder();
      while (true) {
        String line = bufferedReader.readLine();
        if (line == null) {
          break;
        }
        errorInfo.append(line);
      }

      if (process.waitFor() != 0)
        Log.e(TAG, errorInfo.toString());
    }
    catch (Exception e)
    {
      Log.e(TAG, "DumpSysCollector.meminfo could not retrieve data", e);
    } finally {
      AVPersistenceUtils.closeQuietly(bufferedReader);
      AVPersistenceUtils.closeQuietly(reader);
      AVPersistenceUtils.closeQuietly(in);
    }

    return meminfo.toString();
  }

  public static String getCPUInfo() {
    StringBuffer sb = new StringBuffer();
    BufferedReader br = null;
    if (new File("/proc/cpuinfo").exists()) {
      try {
        br = new BufferedReader(new FileReader(new File("/proc/cpuinfo")));
        String line;
        while ((line = br.readLine()) != null) {
          if (line.contains("Processor")) {
            int position = line.indexOf(":");
            if ((position >= 0) && (position < line.length() - 1))
              sb.append(line.substring(position + 1).trim());
          }
        }
      }
      catch (IOException e)
      {
        Log.e(TAG, "getCPUInfo", e);
      } finally {
        AVPersistenceUtils.closeQuietly(br);
      }
    }
    return sb.toString();
  }

  public static String getLocalIpAddress() {
    StringBuilder result = new StringBuilder();
    boolean first = true;
    try {
      Enumeration en = NetworkInterface.getNetworkInterfaces();
      while (en.hasMoreElements()) {
        NetworkInterface intf = (NetworkInterface)en.nextElement();
        Enumeration enumIpAddr = intf.getInetAddresses();
        while (enumIpAddr.hasMoreElements()) {
          InetAddress inetAddress = (InetAddress)enumIpAddr.nextElement();
          if (!inetAddress.isLoopbackAddress()) {
            if (!first) {
              result.append('\n');
            }
            result.append(inetAddress.getHostAddress().toString());
            first = false;
          }
        }
      }
    } catch (SocketException ex) {
      Log.i(TAG, ex.toString());
    }
    return result.toString();
  }

  public static String getApplicationFilePath(Context context) {
    File filesDir = context.getFilesDir();
    if (filesDir != null) {
      return filesDir.getAbsolutePath();
    }
    return "Couldn't retrieve ApplicationFilePath";
  }

  public static long getCurrentTimestamp() {
    return System.currentTimeMillis();
  }

  public static String getRandomString(int length) {
    String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    StringBuilder randomString = new StringBuilder(length);

    for (int i = 0; i < length; i++) {
      randomString.append(letters.charAt(new Random().nextInt(letters.length())));
    }

    return randomString.toString();
  }

  public static String uniqueId() {
    return UUID.randomUUID().toString();
  }

  public static boolean isStringEqual(String src, String target) {
    if ((src == null) && (target == null)) {
      return true;
    }
    if (src != null) {
      return src.equals(target);
    }
    return false;
  }

  private static boolean withinInBlackList()
  {
    if (CELLPHONEBLACKLIST.contains(Build.DEVICE)) {
      return true;
    }
    return false;
  }

  static boolean inDebug(Context context) {
    if (context != null) {
      boolean debug = 0 != (context.getApplicationInfo().flags & 0x2);
      if (debug) {
        Log.i(TAG, new StringBuilder().append("in debug: ").append(debug).toString());
      }
      return debug;
    }
    return false;
  }

  protected static long getRequestInterval()
  {
    return inDebug(AVOSCloud.applicationContext) ? 15000L : 120000L;
  }
}
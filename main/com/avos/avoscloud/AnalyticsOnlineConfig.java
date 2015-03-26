package com.avos.avoscloud;

import android.content.Context;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

class AnalyticsOnlineConfig
{
  private ReportPolicy reportPolicy = ReportPolicy.SEND_INTERVAL;
  protected Map<String, String> config = new HashMap();
  private final AnalyticsImpl parent;
  private boolean enableStats = true;

  public AnalyticsOnlineConfig(AnalyticsImpl ref)
  {
    this.parent = ref;
  }

  public void update(Context context) {
    update(context, true);
  }

  public void update(Context context, final boolean updatePolicy) {
    String endPoint = String.format("statistics/apps/%s/sendPolicy", new Object[] { AVOSCloud.applicationId });
    PaasClient.statistisInstance().getObject(endPoint, null, false, null, new GenericObjectCallback()
    {
      public void onSuccess(String content, AVException e)
      {
        try {
          Map jsonMap = JSONHelper.mapFromString(content);
          Object parameters = jsonMap.get("parameters");
          boolean notifyListener = false;
          if ((parameters != null) && ((parameters instanceof Map))) {
            Map newConfig = (Map)parameters;
            notifyListener = !AnalyticsOnlineConfig.this.config.equals(newConfig);
            AnalyticsOnlineConfig.this.config.clear();
            AnalyticsOnlineConfig.this.config.putAll(newConfig);
            AnalyticsOnlineConfig.this.parent.notifyOnlineConfigListener(new JSONObject(AnalyticsOnlineConfig.this.config));
          }
          if (updatePolicy) {
            Boolean enable = (Boolean)jsonMap.get("enable");
            if (enable != null)
            {
              AnalyticsOnlineConfig.this.enableStats = enable.booleanValue();
            }
            Number policy = (Number)jsonMap.get("policy");
            if (policy != null) {
              ReportPolicy oldPolicy = AnalyticsOnlineConfig.this.reportPolicy;
              ReportPolicy newPolicy = ReportPolicy.valueOf(policy.intValue());
              if ((oldPolicy != newPolicy) || (notifyListener))
                AnalyticsOnlineConfig.this.parent.setReportPolicy(newPolicy);
            }
          }
        }
        catch (Exception exception) {
          exception.printStackTrace();
        }
      }

      public void onFailure(Throwable error, String content)
      {
        LogUtil.log.e("Failed " + content);
      }
    });
  }

  public boolean isEnableStats() {
    return this.enableStats;
  }

  public void setEnableStats(boolean enableStats) {
    this.enableStats = enableStats;
  }

  public boolean setReportPolicy(ReportPolicy p) {
    boolean policyUpdated = this.reportPolicy.value() != p.value();
    this.reportPolicy = p;
    return policyUpdated;
  }

  public ReportPolicy getReportPolicy() {
    return this.reportPolicy;
  }

  public String getConfigParams(String key) {
    Object object = this.config.get(key);
    if ((object instanceof String)) {
      return (String)object;
    }
    return null;
  }
}
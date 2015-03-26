package com.avos.avoscloud;

import org.json.JSONObject;

public abstract interface AVOnlineConfigureListener
{
  public abstract void onDataReceived(JSONObject paramJSONObject);
}
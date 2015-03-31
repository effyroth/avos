package com.avos.avoscloud;

public enum ReportPolicy
{
  REALTIME(0), 
  BATCH(1), 
  SENDDAILY(4), 
  SENDWIFIONLY(5), 
  SEND_INTERVAL(6), 
  SEND_ON_EXIT(7);

  private int value = 0;

  private ReportPolicy(int value) {
    this.value = value;
  }

  public static ReportPolicy valueOf(int value) {
    switch (value) {
    case 0:
      return REALTIME;
    case 1:
      return BATCH;
    case 4:
      return SENDDAILY;
    case 5:
      return SENDWIFIONLY;
    case 6:
      return SEND_INTERVAL;
    case 7:
      return SEND_ON_EXIT;
    case 2:
    case 3: } return null;
  }

  public int value()
  {
    return this.value;
  }
}
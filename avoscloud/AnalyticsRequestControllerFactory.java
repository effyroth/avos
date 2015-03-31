package com.avos.avoscloud;

class AnalyticsRequestControllerFactory
{
  static AnalyticsRequestController getAnalyticsRequestController(String sessionId, ReportPolicy reportPolicy, AnalyticsImpl implement)
  {
    AnalyticsRequestController requestController = null;
    switch (reportPolicy.ordinal()) {
    case 1:
      requestController = new IntervalRequestController(sessionId, implement, AnalyticsUtils.getRequestInterval());

      break;
    case 2:
    case 3:
      requestController = implement.realTimeController;
      break;
    case 4:
      requestController = new BoosterRequestController(sessionId, implement);
      break;
    case 5:
    default:
      requestController = new BatchRequestController(sessionId, implement, AnalyticsUtils.getRequestInterval());
    }

    return requestController;
  }
}
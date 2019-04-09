package com.github.hebelala.tasking.api.trigger.impl;

import com.github.hebelala.tasking.api.trigger.CronTrigger;
import java.util.Date;

/**
 * @author hebelala
 */
public class CronTriggerImpl implements CronTrigger {

  private String cron;

  public CronTriggerImpl(String cron) {
    this.cron = cron;
  }

  @Override
  public String getCron() {
    return cron;
  }

  @Override
  public Date getNextFireTime() {
    // TODO
    return null;
  }
}

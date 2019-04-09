package com.github.hebelala.tasking.api.trigger.impl;

import com.github.hebelala.tasking.api.trigger.JdkTrigger;

/**
 * @author hebelala
 */
public class JdkTriggerImpl implements JdkTrigger {

  private long time;
  private long period;
  private boolean oneShot;
  private boolean atFixedRate;
  private boolean withFixedDelay;

  public JdkTriggerImpl(long time, long period, boolean oneShot, boolean atFixedRate,
      boolean withFixedDelay) {
    this.time = time;
    this.period = period;
    this.oneShot = oneShot;
    this.atFixedRate = atFixedRate;
    this.withFixedDelay = withFixedDelay;
  }

  @Override
  public long getTime() {
    return time;
  }

  @Override
  public long getPeriod() {
    return period;
  }

  @Override
  public boolean oneShot() {
    return oneShot;
  }

  @Override
  public boolean atFixedRate() {
    return atFixedRate;
  }

  @Override
  public boolean withFixedDelay() {
    return withFixedDelay;
  }
}

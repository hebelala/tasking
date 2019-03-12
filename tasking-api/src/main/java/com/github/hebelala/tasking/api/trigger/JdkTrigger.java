package com.github.hebelala.tasking.api.trigger;

/**
 * Schedule the specified timer task for execution at the specified
 * time with the specified period, in milliseconds.  If period is
 * positive, the task is scheduled for repeated execution; if period is
 * zero, the task is scheduled for one-time execution.
 *
 * @author hebelala
 */
public interface JdkTrigger {

    long getTime();

    long getPeriod();

    boolean oneShot();

    boolean atFixedRate();

    boolean withFixedDelay();

}

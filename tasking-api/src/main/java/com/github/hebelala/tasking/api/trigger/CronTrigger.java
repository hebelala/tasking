package com.github.hebelala.tasking.api.trigger;

import java.util.Date;

/**
 * @author hebelala
 */
public interface CronTrigger extends Trigger {

  String getCron();

  Date getNextFireTime();

}

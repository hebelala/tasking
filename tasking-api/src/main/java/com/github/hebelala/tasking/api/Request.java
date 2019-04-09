package com.github.hebelala.tasking.api;

import com.github.hebelala.tasking.api.trigger.Trigger;

/**
 * @author hebelala
 */
public interface Request {

  String getNamespace();

  String getName();

  Trigger getTrigger();

}

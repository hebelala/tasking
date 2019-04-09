package com.github.hebelala.tasking.api;

/**
 * @author hebelala
 */
public interface Application {

  void init();

  Task getTask(Class<? extends Task> taskClass);

  void destroy();

}

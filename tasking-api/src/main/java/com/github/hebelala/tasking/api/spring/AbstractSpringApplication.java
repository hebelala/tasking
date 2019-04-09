package com.github.hebelala.tasking.api.spring;

import com.github.hebelala.tasking.api.Application;
import com.github.hebelala.tasking.api.Task;
import org.springframework.context.ApplicationContext;

/**
 * @author hebelala
 */
public abstract class AbstractSpringApplication implements Application {

  protected ApplicationContext applicationContext;

  @Override
  public Task getTask(Class<? extends Task> taskClass) {
    return applicationContext != null ? applicationContext.getBean(taskClass) : null;
  }

}

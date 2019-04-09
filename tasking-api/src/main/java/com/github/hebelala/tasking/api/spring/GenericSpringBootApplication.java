package com.github.hebelala.tasking.api.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author hebelala
 */
@SpringBootApplication
public class GenericSpringBootApplication extends AbstractSpringApplication {

  @Override
  public void init() {
    applicationContext = SpringApplication.run(getClass());
  }

  @Override
  public void destroy() {
    if (applicationContext != null) {
      ((ConfigurableApplicationContext) applicationContext).close();
    }
  }

}

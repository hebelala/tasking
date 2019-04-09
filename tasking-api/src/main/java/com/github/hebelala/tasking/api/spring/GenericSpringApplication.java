package com.github.hebelala.tasking.api.spring;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author hebelala
 */
public class GenericSpringApplication extends AbstractSpringApplication {

  @Override
  public void init() {
    applicationContext = new ClassPathXmlApplicationContext(configLocations());
  }

  @Override
  public void destroy() {
    if (applicationContext != null) {
      ((ClassPathXmlApplicationContext) applicationContext).close();
    }
  }

  protected String[] configLocations() {
    return new String[]{"applicationContext.xml"};
  }

}

package com.github.hebelala.tasking.api;

/**
 * @author hebelala
 */
public interface App {

    void init();

    Task getTask(Class<? extends Task> taskClass);

    void destroy();

}

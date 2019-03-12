package com.github.hebelala.tasking.demo;

import com.github.hebelala.tasking.api.App;
import com.github.hebelala.tasking.api.Task;

/**
 * @author hebelala
 */
public class DemoApp implements App {
    @Override
    public void init() {

    }

    @Override
    public Task getTask(Class<? extends Task> taskClass) {
        try {
            return taskClass.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void destroy() {

    }
}

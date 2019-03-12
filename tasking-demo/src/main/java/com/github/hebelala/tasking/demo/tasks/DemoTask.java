package com.github.hebelala.tasking.demo.tasks;

import com.github.hebelala.tasking.api.Task;
import com.github.hebelala.tasking.api.TaskRequest;
import com.github.hebelala.tasking.api.TaskResponse;
import com.github.hebelala.tasking.api.trigger.Trigger;

/**
 * @author hebelala
 */
public class DemoTask implements Task {
    @Override
    public TaskResponse run(TaskRequest taskRequest) {
        Trigger trigger = taskRequest.getTrigger();
        System.out.println(trigger.getClass().getName());
        return new TaskResponse();
    }
}

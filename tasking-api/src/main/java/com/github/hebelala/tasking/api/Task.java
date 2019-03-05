package com.github.hebelala.tasking.api;

/**
 * @author hebelala
 */
public interface Task {

	TaskResponse run(TaskRequest taskRequest);

}

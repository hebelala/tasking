package com.github.hebelala.tasking.zookeeper.retry;

/**
 * @author hebelala
 */
public interface Callback {

    void call() throws Exception;

}

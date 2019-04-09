package com.github.hebelala.tasking.zookeeper.retry;

/**
 * @author hebelala
 */
public interface Callback2<V> {

  V call() throws Exception;

}

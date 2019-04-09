package com.github.hebelala.tasking.zookeeper.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hebelala
 */
public class RetryForever {

  protected Logger logger = LoggerFactory.getLogger(getClass());

  private volatile boolean closed;

  public void call(Callback callback) {
    while (!closed) {
      try {
        callback.call();
        return;
      } catch (Throwable t) {
        logger.error(t.getMessage(), t);
      }
    }
  }

  public <V> V call(Callback2<V> callback2) {
    while (!closed) {
      try {
        return callback2.call();
      } catch (Throwable t) {
        logger.error(t.getMessage(), t);
      }
    }
    return null;
  }

  public void close() {
    closed = true;
  }

}

package com.github.hebelala.tasking.zookeeper.monitor;

/**
 * @author hebelala
 */
public abstract class AbstractMonitor {

  protected boolean closed;

  public void close() {
    closed = true;
  }

}

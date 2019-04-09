package com.github.hebelala.tasking.zookeeper;

import com.github.hebelala.tasking.zookeeper.retry.RetryForever;
import java.io.IOException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hebelala
 */
public abstract class TaskingZookeeper implements Watcher {

  protected final Logger log = LoggerFactory.getLogger(getClass());
  protected volatile boolean closed;
  private String connectString;
  private int sessionTimeout;
  private String namespace;
  private String username;
  private String password;
  private ZooKeeper zooKeeper;
  private RetryForever retryForever;

  public TaskingZookeeper(String connectString, int sessionTimeout, String namespace,
      String username, String password) {
    if (namespace != null && namespace.contains("/")) {
      throw new IllegalArgumentException("The namespace cannot include /");
    }
    this.connectString = connectString;
    this.sessionTimeout = sessionTimeout;
    this.namespace = namespace;
    this.username = username;
    this.password = password;
  }

  @Override
  public void process(WatchedEvent event) {
    if (event.getType() == Event.EventType.None) {
      Event.KeeperState state = event.getState();
      switch (state) {
        case Expired:
          try {
            newInstanceZookeeper();
          } catch (InterruptedException | IOException e) {
            log.error(e.getMessage(), e);
          }
          break;
        default:
      }
    }
  }

  private void newInstanceZookeeper() throws InterruptedException, IOException {
    synchronized (this) {
      if (closed) {
        return;
      }
      if (retryForever != null) {
        retryForever.close();
      }
      if (zooKeeper != null) {
        zooKeeper.close();
      }
      beforeNewInstanceZookeeper();
      start();
      afterNewInstanceZookeeper();
    }
  }

  protected abstract void beforeNewInstanceZookeeper();

  protected abstract void afterNewInstanceZookeeper();

  public void start() throws IOException {
    synchronized (this) {
      retryForever = new RetryForever();
      zooKeeper = new ZooKeeper(connectString + "/" + namespace, sessionTimeout, this);
      zooKeeper.addAuthInfo("digest",
          new StringBuilder().append(username).append(':').append(password).toString()
              .getBytes("utf-8"));
    }
  }

  public void stop() {
    synchronized (this) {
      closed = true;
      if (retryForever != null) {
        retryForever.close();
      }
      if (zooKeeper != null) {
        try {
          zooKeeper.close();
        } catch (InterruptedException e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }

  public void blockUntilConnected() throws InterruptedException {
    while (!zooKeeper.getState().isConnected()) {
      Thread.sleep(100L);
    }
  }

  public boolean isStopped() {
    return closed;
  }

  public Stat exists(final String path) {
    return retryForever.call(() -> zooKeeper.exists(path, null));
  }

  public String create(final String path, final String data, final CreateMode createMode) {
    return retryForever.call(() -> {
      try {
        return zooKeeper
            .create(path, data.getBytes("utf-8"), ZooDefs.Ids.CREATOR_ALL_ACL, createMode);
      } catch (KeeperException.NodeExistsException e) {
        return null;
      } catch (KeeperException.NoNodeException e) {
        String parentPath = path.substring(0, path.lastIndexOf('/'));
        if (parentPath.isEmpty()) // The root is a chroot, so fix the parentPath to create
        {
          parentPath = "/";
        }
        create(parentPath, "", CreateMode.PERSISTENT);
        return create(path, data, createMode);
      }
    });
  }

}

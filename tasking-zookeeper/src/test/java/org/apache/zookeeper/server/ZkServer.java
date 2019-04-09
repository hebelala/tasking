package org.apache.zookeeper.server;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkServer extends ZooKeeperServerMain {

  protected Logger logger = LoggerFactory.getLogger(getClass());

  private int port;

  public ZkServer(int port) {
    this.port = port;
  }

  public void start(final boolean fullNew) throws InterruptedException {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          String zooDataDir = ".zooDataDir";
          if (fullNew) {
            delete(new File(zooDataDir));
          }
          ServerConfig config = new ServerConfig();
          config.parse(new String[]{String.valueOf(port), zooDataDir});
          runFromConfig(config);
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
        }
      }

      private void delete(File file) {
        if (file.exists()) {
          if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
              for (File child : files) {
                delete(child);
              }
            }
          }
          file.delete();
        }
      }
    }).start();

    do {
      Thread.sleep(200L);
    }
    while (getCnxnFactory() == null || getCnxnFactory().zkServer == null
        || getCnxnFactory().zkServer.state != ZooKeeperServer.State.RUNNING);
  }

  public void shutdown() {
    super.shutdown();
  }

  public int getPort() {
    return port;
  }

  public String getConnectString() {
    return "localhost:" + port;
  }

  public int getMinSessionTimeout() {
    return getCnxnFactory().getZooKeeperServer().getMinSessionTimeout();
  }
}

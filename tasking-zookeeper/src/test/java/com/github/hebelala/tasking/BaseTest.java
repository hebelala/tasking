package com.github.hebelala.tasking;

import org.apache.zookeeper.server.ZkServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseTest {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected ZkServer startZookeeperServer(int port, boolean fullNew) throws InterruptedException {
    ZkServer zkServer = new ZkServer(port);
    zkServer.start(fullNew);
    return zkServer;
  }

}

package com.github.hebelala.tasking.zookeeper.monitor;

import com.github.hebelala.tasking.BaseTest;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.server.ZkServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StatMonitorTest extends BaseTest {

  private ZkServer zkServer;

  @Before
  public void before() throws Exception {
    zkServer = startZookeeperServer(2181, true);
  }

  @After
  public void after() {
    if (zkServer != null) {
      zkServer.shutdown();
    }
  }

  @Test
  public void test_a_normal() throws Exception {
    ZooKeeper zk = new ZooKeeper(zkServer.getConnectString(), zkServer.getMinSessionTimeout(),
        (event) -> {
        });
    final AtomicBoolean stat = new AtomicBoolean(false);
    StatMonitor statMonitor = new StatMonitor(zk, "/a", false, new StatMonitor.StatListener() {
      @Override
      public void created() {
        stat.set(true);
      }

      @Override
      public void deleted() {
        stat.set(false);
      }
    });

    ZooKeeper zk2 = new ZooKeeper(zkServer.getConnectString(), zkServer.getMinSessionTimeout(),
        (event) -> {
        });

    zk2.create("/a", "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    Thread.sleep(1000L);
    Assert.assertTrue(stat.get());
    zk2.setData("/a", "1".getBytes(), -1);
    Thread.sleep(1000L);
    Assert.assertTrue(stat.get());
    zk2.delete("/a", -1);
    Thread.sleep(1000L);
    Assert.assertFalse(stat.get());

  }

  @Test
  public void test_b_acl() throws Exception {
    ZooKeeper zk = new ZooKeeper(zkServer.getConnectString(), zkServer.getMinSessionTimeout(),
        (event) -> {
        });
    final AtomicBoolean stat = new AtomicBoolean(false);
    StatMonitor statMonitor = new StatMonitor(zk, "/a", false, new StatMonitor.StatListener() {
      @Override
      public void created() {
        stat.set(true);
      }

      @Override
      public void deleted() {
        stat.set(false);
      }
    });

    ZooKeeper zk2 = new ZooKeeper(zkServer.getConnectString(), zkServer.getMinSessionTimeout(),
        (event) -> {
        });

    zk2.addAuthInfo("digest", "admin:123".getBytes());
    zk2.create("/a", "".getBytes(), ZooDefs.Ids.CREATOR_ALL_ACL, CreateMode.PERSISTENT);
    Thread.sleep(1000L);
    Assert.assertTrue(stat.get());
    zk2.setData("/a", "1".getBytes(), -1);
    Thread.sleep(1000L);
    Assert.assertTrue(stat.get());
    zk2.delete("/a", -1);
    Thread.sleep(1000L);
    Assert.assertFalse(stat.get());
  }

  @Test
  public void test_c_disconnected() throws Exception {
    ZooKeeper zk = new ZooKeeper(zkServer.getConnectString(), zkServer.getMinSessionTimeout(),
        (event) -> {
        });
    final AtomicBoolean stat = new AtomicBoolean(false);
    StatMonitor statMonitor = new StatMonitor(zk, "/a", false, new StatMonitor.StatListener() {
      @Override
      public void created() {
        stat.set(true);
      }

      @Override
      public void deleted() {
        stat.set(false);
      }
    });

    ZooKeeper zk2 = new ZooKeeper(zkServer.getConnectString(), zkServer.getMinSessionTimeout(),
        (event) -> {
        });

    zkServer.shutdown();
    Thread.sleep(zkServer.getMinSessionTimeout() / 2);
    Assert.assertFalse(zk.getState().isConnected());
    Assert.assertFalse(stat.get());
    zkServer = startZookeeperServer(zkServer.getPort(), false);
    zk.exists("/", false); // make zk client to connect server right now.
    Assert.assertTrue(zk.getState().isConnected());
    Assert.assertFalse(stat.get());
    zk2.create("/a", "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    Thread.sleep(1000L);
    Assert.assertTrue(stat.get());
    zk2.setData("/a", "1".getBytes(), -1);
    Thread.sleep(1000L);
    Assert.assertTrue(stat.get());
    zk2.delete("/a", -1);
    Thread.sleep(1000L);
    Assert.assertFalse(stat.get());
  }

}

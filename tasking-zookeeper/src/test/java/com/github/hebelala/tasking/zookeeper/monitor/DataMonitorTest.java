package com.github.hebelala.tasking.zookeeper.monitor;

import com.github.hebelala.tasking.BaseTest;
import org.apache.zookeeper.*;
import org.apache.zookeeper.server.ZkServer;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.util.concurrent.atomic.AtomicReference;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DataMonitorTest extends BaseTest {

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
        ZooKeeper zk = new ZooKeeper(zkServer.getConnectString(), zkServer.getMinSessionTimeout(), new Watcher() {
            @Override
            public void process(WatchedEvent event) {

            }
        });
        final AtomicReference<byte[]> myData = new AtomicReference<>(null);
        DataMonitor dataMonitor = new DataMonitor(zk, "/a", null, new DataMonitor.DataListener() {
            @Override
            public void changed(byte[] data) {
                myData.set(data);
            }
        });

        ZooKeeper zk2 = new ZooKeeper(zkServer.getConnectString(), zkServer.getMinSessionTimeout(), new Watcher() {
            @Override
            public void process(WatchedEvent event) {

            }
        });

        zk2.create("/a", "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        Thread.sleep(1000L);
        Assert.assertArrayEquals("".getBytes(), myData.get());
        zk2.setData("/a", "1".getBytes(), -1);
        Thread.sleep(1000L);
        Assert.assertArrayEquals("1".getBytes(), myData.get());
        zk2.delete("/a", -1);
        Thread.sleep(1000L);
        Assert.assertNull(myData.get());

    }

    @Test
    public void test_b_acl() throws Exception {
        ZooKeeper zk = new ZooKeeper(zkServer.getConnectString(), zkServer.getMinSessionTimeout(), new Watcher() {
            @Override
            public void process(WatchedEvent event) {

            }
        });
        final AtomicReference<byte[]> myData = new AtomicReference<>(null);
        DataMonitor dataMonitor = new DataMonitor(zk, "/a", null, new DataMonitor.DataListener() {
            @Override
            public void changed(byte[] data) {
                myData.set(data);
            }
        });

        ZooKeeper zk2 = new ZooKeeper(zkServer.getConnectString(), zkServer.getMinSessionTimeout(), new Watcher() {
            @Override
            public void process(WatchedEvent event) {

            }
        });

        // set acl
        zk2.addAuthInfo("digest", "admin:123".getBytes());
        zk2.create("/a", "".getBytes(), ZooDefs.Ids.CREATOR_ALL_ACL, CreateMode.PERSISTENT);
        Thread.sleep(1000L);
        Assert.assertNull(myData.get());
        zk2.setData("/a", "1".getBytes(), -1);
        Thread.sleep(1000L);
        Assert.assertNull(myData.get());
        zk2.delete("/a", -1);
        Thread.sleep(1000L);
        Assert.assertNull(myData.get());
    }

    @Test
    public void test_c_disconnected() throws Exception {
        ZooKeeper zk = new ZooKeeper(zkServer.getConnectString(), zkServer.getMinSessionTimeout(), new Watcher() {
            @Override
            public void process(WatchedEvent event) {

            }
        });
        final AtomicReference<byte[]> myData = new AtomicReference<>(null);
        DataMonitor dataMonitor = new DataMonitor(zk, "/a", null, new DataMonitor.DataListener() {
            @Override
            public void changed(byte[] data) {
                myData.set(data);
            }
        });

        ZooKeeper zk2 = new ZooKeeper(zkServer.getConnectString(), zkServer.getMinSessionTimeout(), new Watcher() {
            @Override
            public void process(WatchedEvent event) {

            }
        });

        zkServer.shutdown();
        Thread.sleep(zkServer.getMinSessionTimeout() / 2);
        Assert.assertFalse(zk.getState().isConnected());
        Assert.assertNull(myData.get());
        zkServer = startZookeeperServer(zkServer.getPort(), false);
        zk.exists("/", false); // make zk client to connect server right now.
        Assert.assertTrue(zk.getState().isConnected());
        Assert.assertNull(myData.get());
        zk2.create("/a", "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        Thread.sleep(1000L);
        Assert.assertArrayEquals("".getBytes(), myData.get());
        zk2.setData("/a", "1".getBytes(), -1);
        Thread.sleep(1000L);
        Assert.assertArrayEquals("1".getBytes(), myData.get());
        zk2.delete("/a", -1);
        Thread.sleep(1000L);
        Assert.assertNull(myData.get());
    }

}

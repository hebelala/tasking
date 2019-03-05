package com.github.hebelala.tasking.zookeeper;

import com.github.hebelala.tasking.BaseTest;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.server.ZkServer;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.IOException;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TaskingZookeeperTest extends BaseTest {

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
    public void test_a_namespaceIsNotReady() throws IOException, InterruptedException {
        TaskingZookeeper taskingZookeeper = new TaskingZookeeper(zkServer.getConnectString(), zkServer.getMinSessionTimeout(), "abc", "app", "123456") {
            @Override
            protected void beforeNewInstanceZookeeper() {

            }

            @Override
            protected void afterNewInstanceZookeeper() {

            }
        };
        taskingZookeeper.start();
        taskingZookeeper.blockUntilConnected();
        Assert.assertNull(taskingZookeeper.exists("/"));

        TaskingZookeeper taskingZookeeper2 = new TaskingZookeeper(zkServer.getConnectString(), zkServer.getMinSessionTimeout(), "", "app", "123456") {
            @Override
            protected void beforeNewInstanceZookeeper() {

            }

            @Override
            protected void afterNewInstanceZookeeper() {

            }
        };
        taskingZookeeper2.start();
        taskingZookeeper2.blockUntilConnected();
        Assert.assertNotNull(taskingZookeeper2.exists("/"));

        Assert.assertEquals("/a", taskingZookeeper.create("/a", "", CreateMode.PERSISTENT));
        Assert.assertNotNull(taskingZookeeper2.exists("/abc/a"));

        taskingZookeeper.stop();
        taskingZookeeper2.stop();
    }

}

/**
 * Copyright © 2019 hebelala (hebelala@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.hebelala.tasking.zookeeper.monitor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.server.ZkServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.github.hebelala.tasking.BaseTest;

/**
 * @author hebelala
 */
@TestMethodOrder(OrderAnnotation.class)
public class StatMonitorTest extends BaseTest {

	private ZkServer zkServer;

	@BeforeEach
	public void before() throws Exception {
		zkServer = startZookeeperServer(2181, true);
	}

	@AfterEach
	public void after() {
		if (zkServer != null) {
			zkServer.shutdown();
		}
	}

	@Test
	@Order(1)
	public void testNormal() throws Exception {
		ZooKeeper zk = new ZooKeeper(zkServer.getConnectString(), zkServer.getMinSessionTimeout(), (event) -> {
		});
		final AtomicBoolean stat = new AtomicBoolean(false);
		StatMonitor statMonitor = new StatMonitor("/a", false) {

			@Override
			protected void created() {
				stat.set(true);
			}

			@Override
			protected void deleted() {
				stat.set(false);
			};
		};
		statMonitor.registerTo(zk);

		ZooKeeper zk2 = new ZooKeeper(zkServer.getConnectString(), zkServer.getMinSessionTimeout(), (event) -> {
		});

		zk2.create("/a", "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		Thread.sleep(1000L);
		assertTrue(stat.get());

		zk2.setData("/a", "1".getBytes(), -1);
		Thread.sleep(1000L);
		assertTrue(stat.get());

		zk2.delete("/a", -1);
		Thread.sleep(1000L);
		assertFalse(stat.get());

		zk2.create("/a", "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		Thread.sleep(1000L);
		assertTrue(stat.get());

		statMonitor.close();
		zk2.close();
		zk.close();
	}

	@Test
	@Order(2)
	public void testAcl() throws Exception {
		ZooKeeper zk = new ZooKeeper(zkServer.getConnectString(), zkServer.getMinSessionTimeout(), (event) -> {
		});
		final AtomicBoolean stat = new AtomicBoolean(false);
		StatMonitor statMonitor = new StatMonitor("/a", false) {

			@Override
			protected void created() {
				stat.set(true);
			}

			@Override
			protected void deleted() {
				stat.set(false);
			};
		};
		statMonitor.registerTo(zk);

		ZooKeeper zk2 = new ZooKeeper(zkServer.getConnectString(), zkServer.getMinSessionTimeout(), (event) -> {
		});

		zk2.addAuthInfo("digest", "admin:123".getBytes());
		zk2.create("/a", "".getBytes(), ZooDefs.Ids.CREATOR_ALL_ACL, CreateMode.PERSISTENT);
		Thread.sleep(1000L);
		assertTrue(stat.get());

		zk2.setData("/a", "1".getBytes(), -1);
		Thread.sleep(1000L);
		assertTrue(stat.get());

		zk2.delete("/a", -1);
		Thread.sleep(1000L);
		assertFalse(stat.get());

		statMonitor.close();
		zk2.close();
		zk.close();
	}

	@Test
	@Order(3)
	public void testDisconnected() throws Exception {
		ZooKeeper zk = new ZooKeeper(zkServer.getConnectString(), zkServer.getMinSessionTimeout(), (event) -> {
		});
		final AtomicBoolean stat = new AtomicBoolean(false);
		StatMonitor statMonitor = new StatMonitor("/a", false) {

			@Override
			protected void created() {
				stat.set(true);
			}

			@Override
			protected void deleted() {
				stat.set(false);
			};
		};
		statMonitor.registerTo(zk);

		ZooKeeper zk2 = new ZooKeeper(zkServer.getConnectString(), zkServer.getMinSessionTimeout(), (event) -> {
		});

		zkServer.shutdown();
		Thread.sleep(zkServer.getMinSessionTimeout() / 2);
		assertFalse(zk.getState().isConnected());
		assertFalse(stat.get());

		zkServer = startZookeeperServer(zkServer.getPort(), false);
		zk.exists("/", false); // make zk client to connect server right now.
		assertTrue(zk.getState().isConnected());
		assertFalse(stat.get());

		zk2.create("/a", "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		Thread.sleep(1000L);
		assertTrue(stat.get());

		zk2.setData("/a", "1".getBytes(), -1);
		Thread.sleep(1000L);
		assertTrue(stat.get());

		zk2.delete("/a", -1);
		Thread.sleep(1000L);
		assertFalse(stat.get());

		statMonitor.close();
		zk2.close();
		zk.close();
	}
}

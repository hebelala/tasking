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
package com.github.hebelala.tasking.zookeeper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.server.ZkServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.github.hebelala.tasking.BaseTest;

@TestMethodOrder(OrderAnnotation.class)
public class TaskingZookeeperTest extends BaseTest {

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
	public void testNamespaceIsNotReady() throws IOException, InterruptedException {
		TaskingZookeeper taskingZookeeper = new TaskingZookeeper(zkServer.getConnectString(),
				zkServer.getMinSessionTimeout(), "abc", "app", "123456", null);
		taskingZookeeper.start();
		taskingZookeeper.blockUntilConnected();
		assertNull(taskingZookeeper.exists("/"));

		TaskingZookeeper taskingZookeeper2 = new TaskingZookeeper(zkServer.getConnectString(),
				zkServer.getMinSessionTimeout(), "", "app", "123456", null);
		taskingZookeeper2.start();
		taskingZookeeper2.blockUntilConnected();
		assertNotNull(taskingZookeeper2.exists("/"));

		assertEquals("/a", taskingZookeeper.create("/a", "", CreateMode.PERSISTENT));
		assertNotNull(taskingZookeeper2.exists("/abc/a"));

		taskingZookeeper.stop();
		Thread.sleep(1000L);
		taskingZookeeper2.stop();
	}

	@Test
	@Order(2)
	public void testExpired() throws IOException, InterruptedException {
		TaskingZookeeper taskingZookeeper = new TaskingZookeeper(zkServer.getConnectString(),
				zkServer.getMinSessionTimeout(), "abc", "app", "123456", null);
		taskingZookeeper.start();
		taskingZookeeper.blockUntilConnected();
		long sessionId = taskingZookeeper.getSessionId();
		zkServer.closeSession(sessionId);
		Thread.sleep(10000);
	}
}

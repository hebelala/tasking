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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.server.ZkServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.github.hebelala.tasking.BaseTest;

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
		TaskingZookeeper taskingZookeeper = new TaskingZookeeper(zkServer.getConnectString(),
				zkServer.getMinSessionTimeout(), "abc", "app", "123456", null);
		taskingZookeeper.start();
		taskingZookeeper.blockUntilConnected();
		assertThat(taskingZookeeper.exists("/")).isNull();

		TaskingZookeeper taskingZookeeper2 = new TaskingZookeeper(zkServer.getConnectString(),
				zkServer.getMinSessionTimeout(), "", "app", "123456", null);
		taskingZookeeper2.start();
		taskingZookeeper2.blockUntilConnected();
		assertThat(taskingZookeeper2.exists("/")).isNotNull();

		Assert.assertEquals("/a", taskingZookeeper.create("/a", "", CreateMode.PERSISTENT));
		assertThat(taskingZookeeper2.exists("/abc/a")).isNotNull();

		taskingZookeeper.stop();
		Thread.sleep(1000L);
		taskingZookeeper2.stop();
	}

	@Test
	public void test_expired() throws IOException, InterruptedException {
		TaskingZookeeper taskingZookeeper = new TaskingZookeeper(zkServer.getConnectString(),
				zkServer.getMinSessionTimeout(), "abc", "app", "123456", null);
		taskingZookeeper.start();
		taskingZookeeper.blockUntilConnected();
		long sessionId = taskingZookeeper.getSessionId();
		zkServer.closeSession(sessionId);
		Thread.sleep(10000);
	}
}

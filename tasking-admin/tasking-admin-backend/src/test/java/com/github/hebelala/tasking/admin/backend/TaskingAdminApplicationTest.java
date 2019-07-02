package com.github.hebelala.tasking.admin.backend;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TaskingAdminApplication.class)
public class TaskingAdminApplicationTest {

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("tasking.log.appender", "Console");
	}

	@Test
	public void test1() {
		assertTrue(true);
	}

}

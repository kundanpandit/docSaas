package com.docsaas.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// No JobRunr dashboard or worker in tests: avoids port clashes with a running app and stops
// a test context from picking up real jobs from the shared database.
@SpringBootTest(properties = {
		"jobrunr.dashboard.enabled=false",
		"jobrunr.background-job-server.enabled=false"
})
class DocsaasApiApplicationTests {

	@Test
	void contextLoads() {
	}

}

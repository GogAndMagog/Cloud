package org.fizz_buzz.cloud.integration;

import org.fizz_buzz.cloud.configuration.IntegrationTestConfig;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = IntegrationTestConfig.class)
public abstract class IntegrationTestBaseClass {
}

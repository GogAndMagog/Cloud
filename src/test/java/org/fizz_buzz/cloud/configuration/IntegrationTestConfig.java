package org.fizz_buzz.cloud.configuration;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;

import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class IntegrationTestConfig {

    private static final String DB_NAME = "postgres";
    private static final String DB_USER = "admin";
    private static final String DB_PASS = "password";

    private static final String S3_USER = "minioadmin";
    private static final String S3_PASS = "minioadmin";

    private static final String REDIS_PASS = "1234";


    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        PostgreSQLContainer<?> postgresContainer =
                new PostgreSQLContainer<>("postgres:15")
                        .withDatabaseName(DB_NAME)
                        .withUsername(DB_USER)
                        .withPassword(DB_PASS);

        return postgresContainer;
    }

    @Bean
    public MinIOContainer minioContainer(DynamicPropertyRegistry registry) {

        MinIOContainer minioContainer = new MinIOContainer("minio/minio:latest")
                .withPassword(S3_PASS)
                .withUserName(S3_USER);

        return minioContainer;
    }

    @Bean
    @ServiceConnection
    public RedisContainer redisContainer() {

        RedisContainer redisContainer
                = new RedisContainer(DockerImageName.parse("redis:latest"))
                .withEnv("REDIS_PASSWORD", REDIS_PASS);

        return redisContainer;
    }


    @Bean
    public DynamicPropertyRegistrar apiPropertiesRegistrar(MinIOContainer minioContainer) {

        return registry -> {
            registry.add("minio.url", minioContainer::getS3URL);
            registry.add("minio.access-key", minioContainer::getUserName);
            registry.add("minio.secret-key", minioContainer::getPassword);
        };
    }
}

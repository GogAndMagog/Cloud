package org.fizz_buzz.cloud.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.redis.testcontainers.RedisContainer;
import io.swagger.v3.oas.models.media.JsonSchema;
import org.fizz_buzz.cloud.config.S3StorageConfig;
import org.fizz_buzz.cloud.config.SessionsConfig;
import org.fizz_buzz.cloud.dto.request.UserRequestDTO;
import org.fizz_buzz.cloud.dto.response.UserResponseDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
public class AuthTests {

    private static final String DB_NAME = "postgres";
    private static final String DB_USER = "admin";
    private static final String DB_PASS = "password";

    private static final String S3_USER = "minioadmin";
    private static final String S3_PASS = "minioadmin";

    private static final String DEFAULT_LOGIN = "Login5";
    private static final String DEFAULT_PASSWORD = "Login123";

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgresContainer =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName(DB_NAME)
                    .withUsername(DB_USER)
                    .withPassword(DB_PASS);

    @Container
//    @ServiceConnection
    private static final MinIOContainer minioContainer = new MinIOContainer("minio/minio:latest")
            .withPassword(S3_PASS)
            .withUserName(S3_USER)
            .withExposedPorts(9000)
            .withCreateContainerCmdModifier(createContainerCmd ->
                    createContainerCmd
                            .getHostConfig()
                            .withPortBindings(new PortBinding(Ports.Binding.bindPort(9000)
                                    , new ExposedPort(9000))));

    @Container
    @ServiceConnection
    private static final RedisContainer redisContainer = new RedisContainer("latest")
            .withEnv("REDIS_PASSWORD", "1234")
            .withExposedPorts(6379);

    @Autowired private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }

    @Test
    public void registration_CorrectCredentials_Success() throws Exception {

        UserRequestDTO credentials = new UserRequestDTO(DEFAULT_LOGIN, DEFAULT_PASSWORD);
        String json = objectMapper.writeValueAsString(credentials);

        var mvcResult = this.mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(DEFAULT_LOGIN))
                .andReturn();

        assertEquals(MediaType.APPLICATION_JSON_VALUE, mvcResult.getResponse().getContentType());
    }
}

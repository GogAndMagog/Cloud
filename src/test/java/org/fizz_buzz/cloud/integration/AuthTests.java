package org.fizz_buzz.cloud.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fizz_buzz.cloud.dto.request.UserRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@AutoConfigureMockMvc
public class AuthTests extends IntegrationTestBaseClass {

    private static final String LOGIN = "Login";
    private static final String LOGIN_2 = "Login2";
    private static final String LOGIN_3 = "Log";
    private static final String LOGIN_4 = "Login4";
    private static final String LOGIN_5 = "Login5";
    private static final String LOGIN_6 = "Login6";
    private static final String LOGIN_7 = "Login7";
    private static final String LOGIN_8 = "Login8";
    private static final String DEFAULT_PASSWORD = "Login123";
    private static final String WRONG_PASSWORD = "asdsadsd";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }

    @Nested
    class SignUpMethod {

        @Test
        public void registration_CorrectCredentials_Success() throws Exception {

            UserRequestDTO credentials = new UserRequestDTO(LOGIN, DEFAULT_PASSWORD);
            String json = objectMapper.writeValueAsString(credentials);

            var mvcResult = mockMvc.perform(post("/api/v1/auth/sign-up")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value(LOGIN))
                    .andReturn();

            assertEquals(MediaType.APPLICATION_JSON_VALUE, mvcResult.getResponse().getContentType());
        }

        @Test
        public void registration_RegisteredUser_ErrorStatusCode409() throws Exception {

            UserRequestDTO credentials = new UserRequestDTO(LOGIN_2, DEFAULT_PASSWORD);
            String json = objectMapper.writeValueAsString(credentials);

            mockMvc.perform(post("/api/v1/auth/sign-up")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json));
            var mvcResult = mockMvc.perform(post("/api/v1/auth/sign-up")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").exists())
                    .andReturn();

            assertEquals(MediaType.APPLICATION_JSON_VALUE, mvcResult.getResponse().getContentType());
        }

        @Test
        public void registration_ShortUsername_ErrorStatusCode400() throws Exception {

            UserRequestDTO credentials = new UserRequestDTO(LOGIN_3, DEFAULT_PASSWORD);
            String json = objectMapper.writeValueAsString(credentials);

            var mvcResult = mockMvc.perform(post("/api/v1/auth/sign-up")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists())
                    .andReturn();

            assertEquals(MediaType.APPLICATION_JSON_VALUE, mvcResult.getResponse().getContentType());
        }
    }

    @Nested
    class SignInMethod {

        @Test
        public void login_CorrectCredentials_Success() throws Exception {

            UserRequestDTO credentials = new UserRequestDTO(LOGIN_4, DEFAULT_PASSWORD);
            String json = objectMapper.writeValueAsString(credentials);

            mockMvc.perform(post("/api/v1/auth/sign-up")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json));

            var mvcResult = mockMvc.perform(post("/api/v1/auth/sign-in")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(LOGIN_4))
                    .andReturn();

            assertEquals(MediaType.APPLICATION_JSON_VALUE, mvcResult.getResponse().getContentType());
        }

        @Test
        public void login_NonRegisteredUser_ErrorStatusCode401() throws Exception {

            UserRequestDTO credentials = new UserRequestDTO(LOGIN_5, DEFAULT_PASSWORD);
            String json = objectMapper.writeValueAsString(credentials);

            var mvcResult = mockMvc.perform(post("/api/v1/auth/sign-in")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").exists())
                    .andReturn();

            assertEquals(MediaType.APPLICATION_JSON_VALUE, mvcResult.getResponse().getContentType());
        }

        @Test
        public void login_WrongPassword_ErrorStatusCode401() throws Exception {

            UserRequestDTO credentials = new UserRequestDTO(LOGIN_6, DEFAULT_PASSWORD);
            String json = objectMapper.writeValueAsString(credentials);

            mockMvc.perform(post("/api/v1/auth/sign-up")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json));

            UserRequestDTO credentialsWithWrongPassword = new UserRequestDTO(LOGIN_6, WRONG_PASSWORD);
            json = objectMapper.writeValueAsString(credentialsWithWrongPassword);

            var mvcResult = mockMvc.perform(post("/api/v1/auth/sign-in")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").exists())
                    .andReturn();

            assertEquals(MediaType.APPLICATION_JSON_VALUE, mvcResult.getResponse().getContentType());
        }
    }

    @Nested
    class SignOutMethod {

        @Test
        public void logout_CorrectCredentials_Success() throws Exception {

            UserRequestDTO credentials = new UserRequestDTO(LOGIN_7, DEFAULT_PASSWORD);
            String json = objectMapper.writeValueAsString(credentials);

            mockMvc.perform(post("/api/v1/auth/sign-up")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json));

            mockMvc.perform(post("/api/v1/auth/sign-out")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""))
                    .andReturn();
        }

        @Test
        @WithAnonymousUser
        public void logout_UnauthorizedUser_ErrorStatusCode401() throws Exception {

//            UserRequestDTO credentials = new UserRequestDTO(LOGIN_8, DEFAULT_PASSWORD);
//            String json = objectMapper.writeValueAsString(credentials);

//            mockMvc.perform(post("/api/v1/auth/sign-up")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(json));
//            mockMvc.perform(post("/api/v1/auth/sign-out")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(json));

            var mvcResult = mockMvc.perform(post("/api/v1/auth/sign-out")
//                            .with(anonymous())
                    )
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").exists())
                    .andReturn();

            assertEquals(MediaType.APPLICATION_JSON_VALUE, mvcResult.getResponse().getContentType());
        }
    }
}

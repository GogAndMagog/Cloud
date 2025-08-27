package org.fizz_buzz.cloud.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fizz_buzz.cloud.dto.request.UserRequestDTO;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.web.context.WebApplicationContext;

@AutoConfigureMockMvc
public class AuthTests extends IntegrationTestBaseClass {

    private static final String DEFAULT_PASSWORD = "Login123";
    private static final String WRONG_PASSWORD = "asdsadsd";
    private static long currentUserNameId = 0;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private MockMvc mockMvc;


    @Nested
    class SignUpMethod {

        @Test
        public void registration_CorrectCredentials_Success() throws Exception {

            String userName = nextUserName();

            UserRequestDTO credentials = new UserRequestDTO(userName, DEFAULT_PASSWORD);
            String json = objectMapper.writeValueAsString(credentials);

            var mvcResult = mockMvc.perform(post("/api/v1/auth/sign-up")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value(userName))
                    .andReturn();

            assertEquals(MediaType.APPLICATION_JSON_VALUE, mvcResult.getResponse().getContentType());
        }

        @Test
        public void registration_RegisteredUser_ErrorStatusCode409() throws Exception {

            String userName = nextUserName();

            UserRequestDTO credentials = new UserRequestDTO(userName, DEFAULT_PASSWORD);
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

            String userName = "Log";

            UserRequestDTO credentials = new UserRequestDTO(userName, DEFAULT_PASSWORD);
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

            String userName = nextUserName();

            UserRequestDTO credentials = new UserRequestDTO("Login", DEFAULT_PASSWORD);
            String json = objectMapper.writeValueAsString(credentials);

            mockMvc.perform(post("/api/v1/auth/sign-up")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json));

            var mvcResult = mockMvc.perform(post("/api/v1/auth/sign-in")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("Login"))
                    .andReturn();

            assertEquals(MediaType.APPLICATION_JSON_VALUE, mvcResult.getResponse().getContentType());
        }

        @Test
        public void login_NonRegisteredUser_ErrorStatusCode401() throws Exception {

            String userName = nextUserName();

            UserRequestDTO credentials = new UserRequestDTO(userName, DEFAULT_PASSWORD);
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

            String userName = nextUserName();

            UserRequestDTO credentials = new UserRequestDTO(userName, DEFAULT_PASSWORD);
            String json = objectMapper.writeValueAsString(credentials);

            mockMvc.perform(post("/api/v1/auth/sign-up")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json));

            UserRequestDTO credentialsWithWrongPassword = new UserRequestDTO(userName, WRONG_PASSWORD);
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
        @WithMockUser(username = "Login")
        public void logout_CorrectCredentials_Success() throws Exception {

            String userName = nextUserName();

            UserRequestDTO credentials = new UserRequestDTO(userName, DEFAULT_PASSWORD);
            String json = objectMapper.writeValueAsString(credentials);

            mockMvc.perform(post("/api/v1/auth/sign-out"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithAnonymousUser
        public void logout_UnauthorizedUser_ErrorStatusCode401() throws Exception {

            mockMvc.perform(post("/api/v1/auth/sign-out"))
                    .andExpect(status().isUnauthorized());
        }
    }

    private String nextUserName() {

        return "Login" + currentUserNameId++;
    }
}

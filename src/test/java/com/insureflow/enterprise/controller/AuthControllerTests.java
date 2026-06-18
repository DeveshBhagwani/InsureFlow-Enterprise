package com.insureflow.enterprise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insureflow.enterprise.dto.*;
import com.insureflow.enterprise.model.PasswordResetToken;
import com.insureflow.enterprise.model.Role;
import com.insureflow.enterprise.model.UserRole;
import com.insureflow.enterprise.repository.CustomerRepository;
import com.insureflow.enterprise.repository.PasswordResetTokenRepository;
import com.insureflow.enterprise.repository.RefreshTokenRepository;
import com.insureflow.enterprise.repository.RoleRepository;
import com.insureflow.enterprise.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;



    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clear database bypassing foreign keys and soft delete filters
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE nominees");
        jdbcTemplate.execute("TRUNCATE TABLE addresses");
        jdbcTemplate.execute("TRUNCATE TABLE refresh_tokens");
        jdbcTemplate.execute("TRUNCATE TABLE password_reset_tokens");
        jdbcTemplate.execute("TRUNCATE TABLE customers");
        jdbcTemplate.execute("TRUNCATE TABLE user_roles");
        jdbcTemplate.execute("TRUNCATE TABLE users");
        jdbcTemplate.execute("TRUNCATE TABLE role_permissions");
        jdbcTemplate.execute("TRUNCATE TABLE roles");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        // Seed roles
        for (UserRole roleName : UserRole.values()) {
            roleRepository.save(Role.builder().name(roleName).build());
        }
    }

    @Test
    void testRegisterUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@insureflow.com");
        request.setPassword("password123");
        request.setFullName("John Doe");
        request.setRole("CUSTOMER");

        mockMvc.perform(post("/api/v1/auth/register").contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful"));
    }

    @Test
    void testRegisterDuplicateEmailFails() throws Exception {
        // Register first time
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@insureflow.com");
        request.setPassword("password123");
        request.setFullName("John Doe");
        request.setRole("CUSTOMER");

        mockMvc.perform(post("/api/v1/auth/register").contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Register second time
        mockMvc.perform(post("/api/v1/auth/register").contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already in use"));
    }

    @Test
    void testLoginSuccessfully() throws Exception {
        // First register
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("login@insureflow.com");
        registerRequest.setPassword("password123");
        registerRequest.setFullName("Jane Doe");
        registerRequest.setRole("AGENT");

        mockMvc.perform(post("/api/v1/auth/register").contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Now login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("login@insureflow.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login").contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value(notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken").value(notNullValue()))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_AGENT"));
    }

    @Test
    void testLoginInvalidCredentialsFails() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("invalid@insureflow.com");
        loginRequest.setPassword("wrongpassword");

        // Spring security default unauthenticated handler returns 401 custom response
        mockMvc.perform(post("/api/v1/auth/login").contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testRefreshTokenFlow() throws Exception {
        // Register & login
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("refresh@insureflow.com");
        registerRequest.setPassword("password123");
        registerRequest.setFullName("Bobby Tables");
        registerRequest.setRole("ADMIN");
        mockMvc.perform(post("/api/v1/auth/register").contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("refresh@insureflow.com");
        loginRequest.setPassword("password123");
        
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login").contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = loginResult.getResponse().getContentAsString();
        ApiResponse<JwtResponse> apiResponse = objectMapper.readValue(responseContent, 
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, JwtResponse.class));
        
        String refreshToken = apiResponse.getData().getRefreshToken();

        // Refresh token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh").contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value(notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken").value(notNullValue()));
    }

    @Test
    void testPasswordResetFlow() throws Exception {
        // Register
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("reset@insureflow.com");
        registerRequest.setPassword("oldpassword");
        registerRequest.setFullName("Reset Guy");
        registerRequest.setRole("CUSTOMER");
        mockMvc.perform(post("/api/v1/auth/register").contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Request reset
        PasswordResetRequest resetRequest = new PasswordResetRequest();
        resetRequest.setEmail("reset@insureflow.com");

        mockMvc.perform(post("/api/v1/auth/password-reset/request").contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Fetch token from database directly for confirm
        var user = userRepository.findByEmail("reset@insureflow.com").orElseThrow();
        var list = passwordResetTokenRepository.findAll();
        PasswordResetToken resetToken = list.stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow();

        // Confirm reset
        PasswordResetConfirmRequest confirmRequest = new PasswordResetConfirmRequest();
        confirmRequest.setToken(resetToken.getToken());
        confirmRequest.setNewPassword("newsecurepassword");

        mockMvc.perform(post("/api/v1/auth/password-reset/confirm").contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Try logging in with new password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("reset@insureflow.com");
        loginRequest.setPassword("newsecurepassword");

        mockMvc.perform(post("/api/v1/auth/login").contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

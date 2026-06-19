package com.insureflow.enterprise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insureflow.enterprise.dto.ApiResponse;
import com.insureflow.enterprise.dto.PremiumCalculationRequest;
import com.insureflow.enterprise.dto.PremiumCalculationResponse;
import com.insureflow.enterprise.model.Role;
import com.insureflow.enterprise.model.User;
import com.insureflow.enterprise.model.UserRole;
import com.insureflow.enterprise.repository.RoleRepository;
import com.insureflow.enterprise.repository.UserRepository;
import com.insureflow.enterprise.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PremiumCalculationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String userJwtToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE policy_histories");
        jdbcTemplate.execute("TRUNCATE TABLE health_policies");
        jdbcTemplate.execute("TRUNCATE TABLE vehicle_policies");
        jdbcTemplate.execute("TRUNCATE TABLE life_policies");
        jdbcTemplate.execute("TRUNCATE TABLE policies");
        jdbcTemplate.execute("TRUNCATE TABLE refresh_tokens");
        jdbcTemplate.execute("TRUNCATE TABLE password_reset_tokens");
        jdbcTemplate.execute("TRUNCATE TABLE customers");
        jdbcTemplate.execute("TRUNCATE TABLE user_roles");
        jdbcTemplate.execute("TRUNCATE TABLE users");
        jdbcTemplate.execute("TRUNCATE TABLE role_permissions");
        jdbcTemplate.execute("TRUNCATE TABLE roles");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        Role customerRole = roleRepository.save(Role.builder().name(UserRole.CUSTOMER).build());

        User user = User.builder()
                .email("testuser@insureflow.com")
                .password(passwordEncoder.encode("password"))
                .fullName("Test User")
                .roles(Collections.singleton(customerRole))
                .enabled(true)
                .build();
        userRepository.save(user);

        userJwtToken = jwtUtils.generateJwtTokenFromUsername(user.getEmail());
    }

    @Test
    void testCalculateHealthPremium_Young() throws Exception {
        PremiumCalculationRequest request = PremiumCalculationRequest.builder()
                .age(25)
                .policyType("HEALTH")
                .riskScore(1)
                .existingClaims(0)
                .occupation("NORMAL")
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/policies/premium/calculate").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ApiResponse<PremiumCalculationResponse> response = objectMapper.readValue(responseJson,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, PremiumCalculationResponse.class));

        assertNotNull(response.getData());
        assertEquals(0, new BigDecimal("550.00").compareTo(response.getData().getPremiumAmount()));
    }

    @Test
    void testCalculateHealthPremium_SeniorHighRisk() throws Exception {
        PremiumCalculationRequest request = PremiumCalculationRequest.builder()
                .age(55)
                .policyType("HEALTH")
                .riskScore(4)
                .existingClaims(2)
                .occupation("HIGH_RISK")
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/policies/premium/calculate").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ApiResponse<PremiumCalculationResponse> response = objectMapper.readValue(responseJson,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, PremiumCalculationResponse.class));

        assertNotNull(response.getData());
        // Base(500) + Age(350) + Risk(4*50 = 200) + Claims(2*100 = 200) + Occ(200) = 1450.00
        assertEquals(0, new BigDecimal("1450.00").compareTo(response.getData().getPremiumAmount()));
    }

    @Test
    void testCalculateVehiclePremium_Delivery() throws Exception {
        PremiumCalculationRequest request = PremiumCalculationRequest.builder()
                .age(30)
                .policyType("VEHICLE")
                .riskScore(2)
                .existingClaims(1)
                .occupation("DELIVERY")
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/policies/premium/calculate").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ApiResponse<PremiumCalculationResponse> response = objectMapper.readValue(responseJson,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, PremiumCalculationResponse.class));

        assertNotNull(response.getData());
        // Base(300) + Age(50) + Risk(2*80 = 160) + Claims(1*150 = 150) + Occ(150) = 810.00
        assertEquals(0, new BigDecimal("810.00").compareTo(response.getData().getPremiumAmount()));
    }

    @Test
    void testCalculateLifePremium_Hazardous() throws Exception {
        PremiumCalculationRequest request = PremiumCalculationRequest.builder()
                .age(52)
                .policyType("LIFE")
                .riskScore(3)
                .existingClaims(2)
                .occupation("HAZARDOUS")
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/policies/premium/calculate").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ApiResponse<PremiumCalculationResponse> response = objectMapper.readValue(responseJson,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, PremiumCalculationResponse.class));

        assertNotNull(response.getData());
        // Base(400) + Age(300) + Risk(3*100 = 300) + Claims(2*200 = 400) + Occ(250) = 1650.00
        assertEquals(0, new BigDecimal("1650.00").compareTo(response.getData().getPremiumAmount()));
    }

    @Test
    void testCalculatePremium_InvalidPolicyType() throws Exception {
        PremiumCalculationRequest request = PremiumCalculationRequest.builder()
                .age(35)
                .policyType("UNKNOWN")
                .riskScore(1)
                .existingClaims(0)
                .occupation("NORMAL")
                .build();

        mockMvc.perform(post("/api/v1/policies/premium/calculate").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCalculatePremium_ValidationConstraints() throws Exception {
        // Test invalid negative age
        PremiumCalculationRequest requestInvalidAge = PremiumCalculationRequest.builder()
                .age(-5)
                .policyType("HEALTH")
                .riskScore(1)
                .existingClaims(0)
                .occupation("NORMAL")
                .build();

        mockMvc.perform(post("/api/v1/policies/premium/calculate").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalidAge)))
                .andExpect(status().isBadRequest());

        // Test invalid risk score (> 100)
        PremiumCalculationRequest requestInvalidRisk = PremiumCalculationRequest.builder()
                .age(35)
                .policyType("HEALTH")
                .riskScore(105)
                .existingClaims(0)
                .occupation("NORMAL")
                .build();

        mockMvc.perform(post("/api/v1/policies/premium/calculate").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalidRisk)))
                .andExpect(status().isBadRequest());
    }
}

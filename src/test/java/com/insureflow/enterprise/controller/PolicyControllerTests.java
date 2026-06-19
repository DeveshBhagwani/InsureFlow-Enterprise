package com.insureflow.enterprise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insureflow.enterprise.dto.*;
import com.insureflow.enterprise.model.*;
import com.insureflow.enterprise.repository.CustomerRepository;
import com.insureflow.enterprise.repository.PolicyRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PolicyControllerTests {

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
    private PolicyRepository policyRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String customerJwtToken;
    private String otherCustomerJwtToken;
    private String agentJwtToken;

    private Customer customerProfile;
    private Customer otherCustomerProfile;

    @BeforeEach
    void setUp() {
        // Clear DB bypassing referentials
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE policy_histories");
        jdbcTemplate.execute("TRUNCATE TABLE health_policies");
        jdbcTemplate.execute("TRUNCATE TABLE vehicle_policies");
        jdbcTemplate.execute("TRUNCATE TABLE life_policies");
        jdbcTemplate.execute("TRUNCATE TABLE policies");
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
        Role customerRole = roleRepository.save(Role.builder().name(UserRole.CUSTOMER).build());
        Role agentRole = roleRepository.save(Role.builder().name(UserRole.AGENT).build());

        // Create Users
        User user1 = User.builder()
                .email("customer@insureflow.com")
                .password(passwordEncoder.encode("password"))
                .fullName("John Owner")
                .roles(Collections.singleton(customerRole))
                .enabled(true)
                .build();
        userRepository.save(user1);

        User user2 = User.builder()
                .email("other@insureflow.com")
                .password(passwordEncoder.encode("password"))
                .fullName("Jane Stranger")
                .roles(Collections.singleton(customerRole))
                .enabled(true)
                .build();
        userRepository.save(user2);

        User userAgent = User.builder()
                .email("agent@insureflow.com")
                .password(passwordEncoder.encode("password"))
                .fullName("Agent Jack")
                .roles(Collections.singleton(agentRole))
                .enabled(true)
                .build();
        userRepository.save(userAgent);

        // Create Customer Profiles
        customerProfile = customerRepository.save(Customer.builder()
                .user(user1)
                .phone("11111")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .deleted(false)
                .build());

        otherCustomerProfile = customerRepository.save(Customer.builder()
                .user(user2)
                .phone("22222")
                .dateOfBirth(LocalDate.of(1992, 2, 2))
                .deleted(false)
                .build());

        // Generate Tokens
        customerJwtToken = jwtUtils.generateJwtTokenFromUsername(user1.getEmail());
        otherCustomerJwtToken = jwtUtils.generateJwtTokenFromUsername(user2.getEmail());
        agentJwtToken = jwtUtils.generateJwtTokenFromUsername(userAgent.getEmail());
    }

    @Test
    void testCreateHealthPolicyAgentOnly() throws Exception {
        PolicyCreateRequest request = new PolicyCreateRequest();
        request.setCustomerId(customerProfile.getId());
        request.setPolicyType("HEALTH");
        request.setPremiumAmount(new BigDecimal("1200.00"));
        request.setCoverageAmount(new BigDecimal("50000.00"));
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusYears(1));

        PolicyCreateRequest.HealthDetails health = new PolicyCreateRequest.HealthDetails();
        health.setDeductible(new BigDecimal("500.00"));
        health.setCoPayPercentage(new BigDecimal("10.00"));
        health.setPreExistingConditions("None");
        request.setHealthDetails(health);

        mockMvc.perform(post("/api/v1/policies").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.policyType").value("HEALTH"))
                .andExpect(jsonPath("$.data.premiumAmount").value(1200.00))
                .andExpect(jsonPath("$.data.healthDetails.deductible").value(500.00))
                .andExpect(jsonPath("$.data.history", hasSize(1)));
    }

    @Test
    void testCreateVehiclePolicyAgentOnly() throws Exception {
        PolicyCreateRequest request = new PolicyCreateRequest();
        request.setCustomerId(customerProfile.getId());
        request.setPolicyType("VEHICLE");
        request.setPremiumAmount(new BigDecimal("800.00"));
        request.setCoverageAmount(new BigDecimal("25000.00"));
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusYears(1));

        PolicyCreateRequest.VehicleDetails vehicle = new PolicyCreateRequest.VehicleDetails();
        vehicle.setVehicleMake("Toyota");
        vehicle.setVehicleModel("Corolla");
        vehicle.setLicensePlate("XYZ123");
        vehicle.setVehicleValue(new BigDecimal("20000.00"));
        request.setVehicleDetails(vehicle);

        mockMvc.perform(post("/api/v1/policies").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.policyType").value("VEHICLE"))
                .andExpect(jsonPath("$.data.vehicleDetails.vehicleModel").value("Corolla"));
    }

    @Test
    void testCreatePolicyCustomerForbidden() throws Exception {
        PolicyCreateRequest request = new PolicyCreateRequest();
        request.setCustomerId(customerProfile.getId());
        request.setPolicyType("HEALTH");
        request.setPremiumAmount(new BigDecimal("1200.00"));
        request.setCoverageAmount(new BigDecimal("50000.00"));
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusYears(1));

        mockMvc.perform(post("/api/v1/policies").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testRenewAndCancelPolicyFlows() throws Exception {
        // 1. First, let Agent issue a Health policy
        PolicyCreateRequest request = new PolicyCreateRequest();
        request.setCustomerId(customerProfile.getId());
        request.setPolicyType("HEALTH");
        request.setPremiumAmount(new BigDecimal("1200.00"));
        request.setCoverageAmount(new BigDecimal("50000.00"));
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusYears(1));
        
        PolicyCreateRequest.HealthDetails health = new PolicyCreateRequest.HealthDetails();
        health.setDeductible(new BigDecimal("500.00"));
        health.setCoPayPercentage(new BigDecimal("10.00"));
        request.setHealthDetails(health);

        MvcResult createResult = mockMvc.perform(post("/api/v1/policies").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String content = createResult.getResponse().getContentAsString();
        ApiResponse<PolicyResponse> apiResponse = objectMapper.readValue(content,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, PolicyResponse.class));
        
        Long policyId = apiResponse.getData().getId();
        LocalDate oldEndDate = apiResponse.getData().getEndDate();

        // 2. Renew policy as Agent: Success
        PolicyRenewRequest renewRequest = new PolicyRenewRequest();
        renewRequest.setNewEndDate(oldEndDate.plusYears(1));
        renewRequest.setUpdatedPremiumAmount(new BigDecimal("1300.00"));

        mockMvc.perform(put("/api/v1/policies/" + policyId + "/renew").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(renewRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.premiumAmount").value(1300.00))
                .andExpect(jsonPath("$.data.endDate").value(oldEndDate.plusYears(1).toString()))
                .andExpect(jsonPath("$.data.history", hasSize(2)));

        // 3. Customer owner cancels own policy: Success
        PolicyCancelRequest cancelRequest = new PolicyCancelRequest();
        cancelRequest.setReason("Moving out of country");

        mockMvc.perform(put("/api/v1/policies/" + policyId + "/cancel").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.history", hasSize(3)));

        // 4. Other customer tries to cancel this policy: Fails Forbidden (IDOR check)
        mockMvc.perform(put("/api/v1/policies/" + policyId + "/cancel").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherCustomerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isForbidden());
    }
}

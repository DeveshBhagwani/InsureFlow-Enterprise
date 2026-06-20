package com.insureflow.enterprise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insureflow.enterprise.dto.*;
import com.insureflow.enterprise.model.*;
import com.insureflow.enterprise.repository.*;
import com.insureflow.enterprise.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
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
public class ClaimControllerTests {

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
    private ClaimRepository claimRepository;

    @Autowired
    private ClaimHistoryRepository claimHistoryRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String customerJwtToken;
    private String otherCustomerJwtToken;
    private String claimOfficerJwtToken;
    private String agentJwtToken;

    private Customer customerProfile;
    private Customer otherCustomerProfile;
    private Policy customerPolicy;
    private Policy otherCustomerPolicy;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE claim_histories");
        jdbcTemplate.execute("TRUNCATE TABLE claim_documents");
        jdbcTemplate.execute("TRUNCATE TABLE claims");
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

        Role customerRole = roleRepository.save(Role.builder().name(UserRole.CUSTOMER).build());
        Role agentRole = roleRepository.save(Role.builder().name(UserRole.AGENT).build());
        Role officerRole = roleRepository.save(Role.builder().name(UserRole.CLAIM_OFFICER).build());

        User user1 = userRepository.save(User.builder()
                .email("customer@insureflow.com")
                .password(passwordEncoder.encode("password"))
                .fullName("John Owner")
                .roles(Collections.singleton(customerRole))
                .enabled(true)
                .build());

        User user2 = userRepository.save(User.builder()
                .email("other@insureflow.com")
                .password(passwordEncoder.encode("password"))
                .fullName("Jane Stranger")
                .roles(Collections.singleton(customerRole))
                .enabled(true)
                .build());

        User userOfficer = userRepository.save(User.builder()
                .email("officer@insureflow.com")
                .password(passwordEncoder.encode("password"))
                .fullName("Officer Pete")
                .roles(Collections.singleton(officerRole))
                .enabled(true)
                .build());

        User userAgent = userRepository.save(User.builder()
                .email("agent@insureflow.com")
                .password(passwordEncoder.encode("password"))
                .fullName("Agent Jack")
                .roles(Collections.singleton(agentRole))
                .enabled(true)
                .build());

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

        customerJwtToken = jwtUtils.generateJwtTokenFromUsername(user1.getEmail());
        otherCustomerJwtToken = jwtUtils.generateJwtTokenFromUsername(user2.getEmail());
        claimOfficerJwtToken = jwtUtils.generateJwtTokenFromUsername(userOfficer.getEmail());
        agentJwtToken = jwtUtils.generateJwtTokenFromUsername(userAgent.getEmail());

        // Create health policy for owner
        customerPolicy = policyRepository.save(HealthPolicy.builder()
                .policyNumber("POL-OWNER123")
                .customer(customerProfile)
                .policyType(PolicyType.HEALTH)
                .status(PolicyStatus.ACTIVE)
                .premiumAmount(new BigDecimal("1200.00"))
                .coverageAmount(new BigDecimal("50000.00"))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .deductible(new BigDecimal("500.00"))
                .coPayPercentage(new BigDecimal("10.00"))
                .build());

        // Create health policy for other customer
        otherCustomerPolicy = policyRepository.save(HealthPolicy.builder()
                .policyNumber("POL-STRANGER")
                .customer(otherCustomerProfile)
                .policyType(PolicyType.HEALTH)
                .status(PolicyStatus.ACTIVE)
                .premiumAmount(new BigDecimal("1000.00"))
                .coverageAmount(new BigDecimal("30000.00"))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .deductible(new BigDecimal("300.00"))
                .coPayPercentage(new BigDecimal("5.00"))
                .build());
    }

    @Test
    void testRaiseClaim_Success() throws Exception {
        ClaimRequest request = ClaimRequest.builder()
                .policyId(customerPolicy.getId())
                .claimAmount(new BigDecimal("5000.00"))
                .description("Hospitalization claim for emergency room visit")
                .build();

        mockMvc.perform(post("/api/v1/claims").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.claimNumber").exists())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.claimAmount").value(5000.00))
                .andExpect(jsonPath("$.data.history", hasSize(1)));
    }

    @Test
    void testRaiseClaim_ExceedsCoverage_Fails() throws Exception {
        // coverage is 50000.00, raise claim for 50000.01
        ClaimRequest request = ClaimRequest.builder()
                .policyId(customerPolicy.getId())
                .claimAmount(new BigDecimal("50000.01"))
                .description("Overlimit claim")
                .build();

        mockMvc.perform(post("/api/v1/claims").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Claim amount cannot exceed the policy coverage amount of: 50000.00"));
    }

    @Test
    void testRaiseClaim_ForeignPolicy_Forbidden() throws Exception {
        // user1 tries to raise claim on user2's policy
        ClaimRequest request = ClaimRequest.builder()
                .policyId(otherCustomerPolicy.getId())
                .claimAmount(new BigDecimal("2000.00"))
                .description("Unauthorized claim")
                .build();

        mockMvc.perform(post("/api/v1/claims").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUploadClaimDocument_Success() throws Exception {
        // 1. Raise claim
        Claim claim = claimRepository.save(Claim.builder()
                .claimNumber("CLM-DOC1")
                .policy(customerPolicy)
                .claimAmount(new BigDecimal("1000.00"))
                .description("Document upload test")
                .status(ClaimStatus.SUBMITTED)
                .build());

        // 2. Perform multipart upload
        MockMultipartFile file = new MockMultipartFile("file", "medical_bill.pdf",
                MediaType.APPLICATION_PDF_VALUE, "mock-pdf-content".getBytes());

        mockMvc.perform(multipart("/api/v1/claims/" + claim.getId() + "/documents").file(file).contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testWorkflowTransitions_SuccessAndTerminalValidation() throws Exception {
        // 1. Raise claim
        ClaimRequest claimRequest = ClaimRequest.builder()
                .policyId(customerPolicy.getId())
                .claimAmount(new BigDecimal("1500.00"))
                .description("Workflow test claim")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/claims").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(claimRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<ClaimResponse> apiResponse = objectMapper.readValue(createResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, ClaimResponse.class));
        Long claimId = apiResponse.getData().getId();

        ClaimTransitionRequest transitionRequest = ClaimTransitionRequest.builder()
                .notes("Moving along the workflow stages")
                .build();

        // 2. Customer attempts transition review: Forbidden
        mockMvc.perform(put("/api/v1/claims/" + claimId + "/review").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transitionRequest)))
                .andExpect(status().isForbidden());

        // 3. Officer reviews claim: Success
        mockMvc.perform(put("/api/v1/claims/" + claimId + "/review").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + claimOfficerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transitionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.data.notes").value("Moving along the workflow stages"))
                .andExpect(jsonPath("$.data.history", hasSize(2)));

        // 4. Officer approves directly: Success
        mockMvc.perform(put("/api/v1/claims/" + claimId + "/approve").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + claimOfficerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transitionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // 5. Trying to reject an APPROVED claim: Fails (terminal state check)
        mockMvc.perform(put("/api/v1/claims/" + claimId + "/reject").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + claimOfficerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transitionRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Claim is already APPROVED. No further transitions allowed."));
    }

    @Test
    void testGetAndSearchClaimsIsolation() throws Exception {
        // Raise claim for customer 1
        Claim claim1 = claimRepository.save(Claim.builder()
                .claimNumber("CLM-USER1")
                .policy(customerPolicy)
                .claimAmount(new BigDecimal("100.00"))
                .description("Claim for customer 1")
                .status(ClaimStatus.SUBMITTED)
                .build());

        // Raise claim for customer 2
        Claim claim2 = claimRepository.save(Claim.builder()
                .claimNumber("CLM-USER2")
                .policy(otherCustomerPolicy)
                .claimAmount(new BigDecimal("200.00"))
                .description("Claim for customer 2")
                .status(ClaimStatus.SUBMITTED)
                .build());

        // 1. Customer 1 retrieves claim 1: Success
        mockMvc.perform(get("/api/v1/claims/" + claim1.getId()).contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.claimNumber").value("CLM-USER1"));

        // 2. Customer 1 retrieves claim 2: Forbidden (IDOR check)
        mockMvc.perform(get("/api/v1/claims/" + claim2.getId()).contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken))
                .andExpect(status().isForbidden());

        // 3. Customer 1 lists claims: Returns only claim 1 (size 1)
        mockMvc.perform(get("/api/v1/claims").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].claimNumber").value("CLM-USER1"));

        // 4. Claim Officer lists claims: Returns both claims (size 2)
        mockMvc.perform(get("/api/v1/claims").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + claimOfficerJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }
}

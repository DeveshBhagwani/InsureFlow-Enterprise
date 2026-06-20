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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuditLogTests {

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
    private PaymentRepository paymentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String customerJwtToken;
    private String adminJwtToken;
    private String officerJwtToken;

    private Customer customerProfile;
    private Policy customerPolicy;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE audit_logs");
        jdbcTemplate.execute("TRUNCATE TABLE payments");
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
        Role adminRole = roleRepository.save(Role.builder().name(UserRole.ADMIN).build());
        Role officerRole = roleRepository.save(Role.builder().name(UserRole.CLAIM_OFFICER).build());

        User userCustomer = userRepository.save(User.builder()
                .email("customer@insureflow.com")
                .password(passwordEncoder.encode("password"))
                .fullName("John Owner")
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

        User userAdmin = userRepository.save(User.builder()
                .email("admin@insureflow.com")
                .password(passwordEncoder.encode("password"))
                .fullName("Admin Jack")
                .roles(new HashSet<>(Arrays.asList(customerRole, adminRole)))
                .enabled(true)
                .build());

        customerProfile = customerRepository.save(Customer.builder()
                .user(userCustomer)
                .phone("11111")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .deleted(false)
                .build());

        customerJwtToken = jwtUtils.generateJwtTokenFromUsername(userCustomer.getEmail());
        adminJwtToken = jwtUtils.generateJwtTokenFromUsername(userAdmin.getEmail());
        officerJwtToken = jwtUtils.generateJwtTokenFromUsername(userOfficer.getEmail());

        // Create health policy
        customerPolicy = policyRepository.save(HealthPolicy.builder()
                .policyNumber("POL-AUDIT123")
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
    }

    @Test
    void testAuditLogGeneratedOnOperations() throws Exception {
        // Assert initial logs are empty
        assertEquals(0, auditLogRepository.count());

        // 1. Update customer profile: triggers UPDATE_CUSTOMER audit log
        CustomerUpdateRequest updateRequest = new CustomerUpdateRequest();
        updateRequest.setPhone("99999");
        updateRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));
        updateRequest.setNominees(Collections.emptyList());

        mockMvc.perform(put("/api/v1/customers/" + customerProfile.getId()).contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        // Assert 1 audit log is registered
        List<AuditLog> logs = auditLogRepository.findAll();
        assertEquals(1, logs.size());
        AuditLog log1 = logs.get(0);
        assertEquals("UPDATE_CUSTOMER", log1.getAction());
        assertEquals("Customer", log1.getEntityName());
        assertEquals(customerProfile.getId(), log1.getEntityId());
        assertEquals("customer@insureflow.com", log1.getUserEmail());
        assertNotNull(log1.getBeforeState());
        assertNotNull(log1.getAfterState());
        assertTrue(log1.getBeforeState().contains("\"phone\":\"11111\""));
        assertTrue(log1.getAfterState().contains("\"phone\":\"99999\""));

        // 2. Raise claim: triggers CREATE_CLAIM audit log
        ClaimRequest claimRequest = ClaimRequest.builder()
                .policyId(customerPolicy.getId())
                .claimAmount(new BigDecimal("1500.00"))
                .description("Hospital test audit")
                .build();

        MvcResult claimResult = mockMvc.perform(post("/api/v1/claims").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(claimRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<ClaimResponse> claimResponse = objectMapper.readValue(claimResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, ClaimResponse.class));
        Long claimId = claimResponse.getData().getId();

        // Assert 2 audit logs now
        logs = auditLogRepository.findAll();
        assertEquals(2, logs.size());
        AuditLog log2 = logs.stream().filter(l -> l.getAction().equals("CREATE_CLAIM")).findFirst().orElseThrow();
        assertNull(log2.getBeforeState()); // created, so no before state
        assertNotNull(log2.getAfterState());
        assertTrue(log2.getAfterState().contains("\"claimAmount\":1500"));

        // 3. Officer reviews claim: triggers REVIEW_CLAIM audit log
        ClaimTransitionRequest transitionRequest = ClaimTransitionRequest.builder()
                .notes("Reviewed for audit compliance")
                .build();

        mockMvc.perform(put("/api/v1/claims/" + claimId + "/review").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + officerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transitionRequest)))
                .andExpect(status().isOk());

        // Assert 3 audit logs now
        logs = auditLogRepository.findAll();
        assertEquals(3, logs.size());
        AuditLog log3 = logs.stream().filter(l -> l.getAction().equals("REVIEW_CLAIM")).findFirst().orElseThrow();
        assertEquals("officer@insureflow.com", log3.getUserEmail());
        assertTrue(log3.getBeforeState().contains("\"status\":\"SUBMITTED\""));
        assertTrue(log3.getAfterState().contains("\"status\":\"UNDER_REVIEW\""));
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "stranger@insureflow.com", roles = "CUSTOMER")
    void testGetAuditLogs_CustomerForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs").contextPath("/api/v1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetAuditLogs_AdminSuccess() throws Exception {
        // Seed an audit log
        auditLogRepository.save(AuditLog.builder()
                .userEmail("someone@insureflow.com")
                .action("TEST_ACTION")
                .entityName("Customer")
                .entityId(1L)
                .beforeState("{}")
                .afterState("{}")
                .build());

        mockMvc.perform(get("/api/v1/audit-logs").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].action").value("TEST_ACTION"));
    }
}

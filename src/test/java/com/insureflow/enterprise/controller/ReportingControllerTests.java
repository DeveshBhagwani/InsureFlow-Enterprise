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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ReportingControllerTests {

    @Autowired
    private MockMvc mockMvc;

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
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String customerJwtToken;
    private String agentJwtToken;
    private String adminJwtToken;

    private User customerUser;
    private User otherCustomerUser;
    private User agentUser;
    private User adminUser;

    private Customer customerProfile;
    private Customer otherCustomerProfile;

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
        jdbcTemplate.execute("TRUNCATE TABLE payments");
        jdbcTemplate.execute("TRUNCATE TABLE customers");
        jdbcTemplate.execute("TRUNCATE TABLE user_roles");
        jdbcTemplate.execute("TRUNCATE TABLE users");
        jdbcTemplate.execute("TRUNCATE TABLE role_permissions");
        jdbcTemplate.execute("TRUNCATE TABLE roles");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        Role customerRole = roleRepository.save(Role.builder().name(UserRole.CUSTOMER).build());
        Role agentRole = roleRepository.save(Role.builder().name(UserRole.AGENT).build());
        Role adminRole = roleRepository.save(Role.builder().name(UserRole.ADMIN).build());

        customerUser = userRepository.save(User.builder()
                .email("customer@insureflow.com")
                .password(passwordEncoder.encode("password"))
                .fullName("John Owner")
                .roles(Collections.singleton(customerRole))
                .enabled(true)
                .build());

        otherCustomerUser = userRepository.save(User.builder()
                .email("other@insureflow.com")
                .password(passwordEncoder.encode("password"))
                .fullName("Jane Stranger")
                .roles(Collections.singleton(customerRole))
                .enabled(true)
                .build());

        agentUser = userRepository.save(User.builder()
                .email("agent@insureflow.com")
                .password(passwordEncoder.encode("password"))
                .fullName("Agent Jack")
                .roles(Collections.singleton(agentRole))
                .enabled(true)
                .build());

        adminUser = userRepository.save(User.builder()
                .email("admin@insureflow.com")
                .password(passwordEncoder.encode("password"))
                .fullName("Admin Boss")
                .roles(Collections.singleton(adminRole))
                .enabled(true)
                .build());

        customerProfile = customerRepository.save(Customer.builder()
                .user(customerUser)
                .phone("11111")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .deleted(false)
                .build());

        otherCustomerProfile = customerRepository.save(Customer.builder()
                .user(otherCustomerUser)
                .phone("22222")
                .dateOfBirth(LocalDate.of(1992, 2, 2))
                .deleted(false)
                .build());

        customerJwtToken = jwtUtils.generateJwtTokenFromUsername(customerUser.getEmail());
        agentJwtToken = jwtUtils.generateJwtTokenFromUsername(agentUser.getEmail());
        adminJwtToken = jwtUtils.generateJwtTokenFromUsername(adminUser.getEmail());
    }

    @Test
    void testSummaryReport_Success() throws Exception {
        // Seed 2 active policies
        Policy p1 = policyRepository.save(HealthPolicy.builder()
                .policyNumber("POL-H1")
                .customer(customerProfile)
                .policyType(PolicyType.HEALTH)
                .status(PolicyStatus.ACTIVE)
                .premiumAmount(new BigDecimal("1000.00"))
                .coverageAmount(new BigDecimal("50000.00"))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .deductible(new BigDecimal("500.00"))
                .coPayPercentage(new BigDecimal("10.00"))
                .build());

        Policy p2 = policyRepository.save(HealthPolicy.builder()
                .policyNumber("POL-V1")
                .customer(otherCustomerProfile)
                .policyType(PolicyType.VEHICLE)
                .status(PolicyStatus.ACTIVE)
                .premiumAmount(new BigDecimal("500.00"))
                .coverageAmount(new BigDecimal("20000.00"))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .deductible(new BigDecimal("250.00"))
                .coPayPercentage(new BigDecimal("5.00"))
                .build());

        // Seed 1 inactive policy (should not be counted in active policies)
        policyRepository.save(HealthPolicy.builder()
                .policyNumber("POL-INACTIVE")
                .customer(customerProfile)
                .policyType(PolicyType.LIFE)
                .status(PolicyStatus.EXPIRED)
                .premiumAmount(new BigDecimal("1500.00"))
                .coverageAmount(new BigDecimal("100000.00"))
                .startDate(LocalDate.now().minusYears(1))
                .endDate(LocalDate.now().minusDays(1))
                .deductible(BigDecimal.ZERO)
                .coPayPercentage(BigDecimal.ZERO)
                .build());

        // Seed Claims
        claimRepository.save(Claim.builder()
                .claimNumber("CLM-001")
                .policy(p1)
                .claimAmount(new BigDecimal("1500.00"))
                .description("Hospital check")
                .status(ClaimStatus.SUBMITTED)
                .build());

        // Seed Payments & Refunds
        paymentRepository.save(Payment.builder()
                .paymentNumber("PAY-001")
                .policy(p1)
                .amount(new BigDecimal("1000.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.SUCCESSFUL)
                .transactionType(TransactionType.PAYMENT)
                .build());

        paymentRepository.save(Payment.builder()
                .paymentNumber("REF-001")
                .policy(p1)
                .amount(new BigDecimal("200.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.SUCCESSFUL)
                .transactionType(TransactionType.REFUND)
                .build());

        // Call summary endpoint
        mockMvc.perform(get("/api/v1/reports/summary").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activePoliciesCount").value(2))
                .andExpect(jsonPath("$.data.totalClaimsCount").value(1))
                .andExpect(jsonPath("$.data.totalCustomersCount").value(2))
                .andExpect(jsonPath("$.data.totalRevenue").value(800.00));
    }

    @Test
    void testPolicyReport_Success() throws Exception {
        // Seed active policies of different types
        policyRepository.save(HealthPolicy.builder()
                .policyNumber("POL-H2")
                .customer(customerProfile)
                .policyType(PolicyType.HEALTH)
                .status(PolicyStatus.ACTIVE)
                .premiumAmount(new BigDecimal("1200.00"))
                .coverageAmount(new BigDecimal("60000.00"))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .deductible(new BigDecimal("500.00"))
                .coPayPercentage(new BigDecimal("10.00"))
                .build());

        policyRepository.save(HealthPolicy.builder()
                .policyNumber("POL-H3")
                .customer(customerProfile)
                .policyType(PolicyType.HEALTH)
                .status(PolicyStatus.ACTIVE)
                .premiumAmount(new BigDecimal("800.00"))
                .coverageAmount(new BigDecimal("40000.00"))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .deductible(new BigDecimal("500.00"))
                .coPayPercentage(new BigDecimal("10.00"))
                .build());

        policyRepository.save(HealthPolicy.builder()
                .policyNumber("POL-V2")
                .customer(otherCustomerProfile)
                .policyType(PolicyType.VEHICLE)
                .status(PolicyStatus.ACTIVE)
                .premiumAmount(new BigDecimal("600.00"))
                .coverageAmount(new BigDecimal("25000.00"))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .deductible(new BigDecimal("250.00"))
                .coPayPercentage(new BigDecimal("5.00"))
                .build());

        // Hit policies endpoint
        mockMvc.perform(get("/api/v1/reports/policies").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                // We check that type HEALTH has totalPremium = 2000.00 and count = 2
                // We check that type VEHICLE has totalPremium = 600.00 and count = 1
                .andExpect(jsonPath("$.data[?(@.policyType == 'HEALTH')].count").value(2))
                .andExpect(jsonPath("$.data[?(@.policyType == 'HEALTH')].totalPremium").value(2000.00))
                .andExpect(jsonPath("$.data[?(@.policyType == 'VEHICLE')].count").value(1))
                .andExpect(jsonPath("$.data[?(@.policyType == 'VEHICLE')].totalPremium").value(600.00));
    }

    @Test
    void testClaimReport_Success() throws Exception {
        Policy p = policyRepository.save(HealthPolicy.builder()
                .policyNumber("POL-C1")
                .customer(customerProfile)
                .policyType(PolicyType.HEALTH)
                .status(PolicyStatus.ACTIVE)
                .premiumAmount(new BigDecimal("1000.00"))
                .coverageAmount(new BigDecimal("50000.00"))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .deductible(new BigDecimal("500.00"))
                .coPayPercentage(new BigDecimal("10.00"))
                .build());

        // 2 SUBMITTED claims
        claimRepository.save(Claim.builder()
                .claimNumber("CLM-002")
                .policy(p)
                .claimAmount(new BigDecimal("1500.00"))
                .description("Doctor checkup")
                .status(ClaimStatus.SUBMITTED)
                .build());

        claimRepository.save(Claim.builder()
                .claimNumber("CLM-003")
                .policy(p)
                .claimAmount(new BigDecimal("2500.00"))
                .description("Pharmacy cost")
                .status(ClaimStatus.SUBMITTED)
                .build());

        // 1 APPROVED claim
        claimRepository.save(Claim.builder()
                .claimNumber("CLM-004")
                .policy(p)
                .claimAmount(new BigDecimal("5000.00"))
                .description("Surgery cost")
                .status(ClaimStatus.APPROVED)
                .build());

        mockMvc.perform(get("/api/v1/reports/claims").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[?(@.status == 'SUBMITTED')].count").value(2))
                .andExpect(jsonPath("$.data[?(@.status == 'SUBMITTED')].totalAmount").value(4000.00))
                .andExpect(jsonPath("$.data[?(@.status == 'APPROVED')].count").value(1))
                .andExpect(jsonPath("$.data[?(@.status == 'APPROVED')].totalAmount").value(5000.00));
    }

    @Test
    void testRevenueReport_Success() throws Exception {
        Policy p = policyRepository.save(HealthPolicy.builder()
                .policyNumber("POL-R1")
                .customer(customerProfile)
                .policyType(PolicyType.HEALTH)
                .status(PolicyStatus.ACTIVE)
                .premiumAmount(new BigDecimal("1000.00"))
                .coverageAmount(new BigDecimal("50000.00"))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .deductible(new BigDecimal("500.00"))
                .coPayPercentage(new BigDecimal("10.00"))
                .build());

        // Successful Payment: 1500.00
        paymentRepository.save(Payment.builder()
                .paymentNumber("PAY-SUCCESS")
                .policy(p)
                .amount(new BigDecimal("1500.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.SUCCESSFUL)
                .transactionType(TransactionType.PAYMENT)
                .build());

        // Failed Payment: 800.00 (should not be counted)
        paymentRepository.save(Payment.builder()
                .paymentNumber("PAY-FAILED")
                .policy(p)
                .amount(new BigDecimal("800.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.FAILED)
                .transactionType(TransactionType.PAYMENT)
                .build());

        // Successful Refund: 300.00
        paymentRepository.save(Payment.builder()
                .paymentNumber("REF-SUCCESS")
                .policy(p)
                .amount(new BigDecimal("300.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.SUCCESSFUL)
                .transactionType(TransactionType.REFUND)
                .build());

        // Failed Refund: 50.00 (should not be counted)
        paymentRepository.save(Payment.builder()
                .paymentNumber("REF-FAILED")
                .policy(p)
                .amount(new BigDecimal("50.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.FAILED)
                .transactionType(TransactionType.REFUND)
                .build());

        mockMvc.perform(get("/api/v1/reports/revenue").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalPremiumCollected").value(1500.00))
                .andExpect(jsonPath("$.data.totalRefundsIssued").value(300.00))
                .andExpect(jsonPath("$.data.netRevenue").value(1200.00));
    }

    @Test
    void testCustomerGrowthReport_Success() throws Exception {
        // Manually update registration dates using jdbcTemplate to test date grouping
        jdbcTemplate.update("UPDATE users SET created_at = ? WHERE id = ?",
                java.sql.Timestamp.valueOf(LocalDateTime.of(2026, 6, 1, 10, 0)), customerUser.getId());

        jdbcTemplate.update("UPDATE users SET created_at = ? WHERE id = ?",
                java.sql.Timestamp.valueOf(LocalDateTime.of(2026, 6, 1, 15, 30)), otherCustomerUser.getId());

        mockMvc.perform(get("/api/v1/reports/customers").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].date").value("2026-06-01"))
                .andExpect(jsonPath("$.data[0].signupsCount").value(2));
    }

    @Test
    void testAccessDenied_ForCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/reports/summary").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAccessDenied_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/reports/summary").contextPath("/api/v1"))
                .andExpect(status().isUnauthorized());
    }
}

package com.insureflow.enterprise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insureflow.enterprise.dto.ApiResponse;
import com.insureflow.enterprise.dto.PaymentRequest;
import com.insureflow.enterprise.dto.PaymentResponse;
import com.insureflow.enterprise.dto.RefundRequest;
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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PaymentControllerTests {

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
    private PaymentRepository paymentRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String customerJwtToken;
    private String otherCustomerJwtToken;
    private String adminJwtToken;

    private Customer customerProfile;
    private Customer otherCustomerProfile;
    private Policy customerPolicy;
    private Policy cancelledPolicy;
    private Policy otherCustomerPolicy;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
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

        User userAdmin = userRepository.save(User.builder()
                .email("admin@insureflow.com")
                .password(passwordEncoder.encode("password"))
                .fullName("Admin Jack")
                .roles(new HashSet<>(Arrays.asList(customerRole, adminRole)))
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
        adminJwtToken = jwtUtils.generateJwtTokenFromUsername(userAdmin.getEmail());

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

        // Create cancelled policy
        cancelledPolicy = policyRepository.save(HealthPolicy.builder()
                .policyNumber("POL-CANCELLED")
                .customer(customerProfile)
                .policyType(PolicyType.HEALTH)
                .status(PolicyStatus.CANCELLED)
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
    void testPayPremium_Success() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .policyId(customerPolicy.getId())
                .amount(new BigDecimal("1200.00"))
                .paymentMethod("CREDIT_CARD")
                .simulateFailure(false)
                .build();

        mockMvc.perform(post("/api/v1/payments").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentNumber").exists())
                .andExpect(jsonPath("$.data.status").value("SUCCESSFUL"))
                .andExpect(jsonPath("$.data.transactionType").value("PAYMENT"));
    }

    @Test
    void testPayPremium_SimulateFailure() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .policyId(customerPolicy.getId())
                .amount(new BigDecimal("1200.00"))
                .paymentMethod("CREDIT_CARD")
                .simulateFailure(true)
                .build();

        mockMvc.perform(post("/api/v1/payments").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.notes").value("Payment simulation failed"));
    }

    @Test
    void testPayPremium_CancelledPolicy_Fails() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .policyId(cancelledPolicy.getId())
                .amount(new BigDecimal("1200.00"))
                .paymentMethod("CREDIT_CARD")
                .simulateFailure(false)
                .build();

        mockMvc.perform(post("/api/v1/payments").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot process premium payment for a CANCELLED policy."));
    }

    @Test
    void testPayPremium_ForeignPolicy_Forbidden() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .policyId(otherCustomerPolicy.getId())
                .amount(new BigDecimal("1000.00"))
                .paymentMethod("CREDIT_CARD")
                .simulateFailure(false)
                .build();

        mockMvc.perform(post("/api/v1/payments").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testProcessRefund_SuccessAndConstraints() throws Exception {
        // 1. First, pay premium to obtain transaction ID
        PaymentRequest request = PaymentRequest.builder()
                .policyId(customerPolicy.getId())
                .amount(new BigDecimal("1200.00"))
                .paymentMethod("CREDIT_CARD")
                .simulateFailure(false)
                .build();

        MvcResult paymentResult = mockMvc.perform(post("/api/v1/payments").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<PaymentResponse> paymentResponse = objectMapper.readValue(paymentResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, PaymentResponse.class));
        Long paymentId = paymentResponse.getData().getId();

        // 2. Customer attempts to process refund: Forbidden
        RefundRequest refundRequest = RefundRequest.builder()
                .originalPaymentId(paymentId)
                .amount(new BigDecimal("500.00"))
                .reason("Partial refund for discount application")
                .build();

        mockMvc.perform(post("/api/v1/payments/refund").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isForbidden());

        // 3. Admin processes refund exceeding payment amount: Fails
        RefundRequest overlimitRefund = RefundRequest.builder()
                .originalPaymentId(paymentId)
                .amount(new BigDecimal("1200.01"))
                .reason("Too much money")
                .build();

        mockMvc.perform(post("/api/v1/payments/refund").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlimitRefund)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Refund amount cannot exceed the original payment amount of: 1200.00"));

        // 4. Admin processes valid refund: Success
        mockMvc.perform(post("/api/v1/payments/refund").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESSFUL"))
                .andExpect(jsonPath("$.data.transactionType").value("REFUND"))
                .andExpect(jsonPath("$.data.originalPaymentId").value(paymentId));

        // 5. Original payment status should now be REFUNDED
        mockMvc.perform(get("/api/v1/payments/" + paymentId).contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));

        // 6. Admin attempts duplicate refund on already refunded transaction: Fails
        mockMvc.perform(post("/api/v1/payments/refund").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Original transaction is not eligible for a refund. It must be a SUCCESSFUL payment transaction."));
    }

    @Test
    void testGetAndSearchPaymentsIsolation() throws Exception {
        // Create payment for customer 1
        Payment pay1 = paymentRepository.save(Payment.builder()
                .paymentNumber("TXN-CUST1")
                .policy(customerPolicy)
                .amount(new BigDecimal("1200.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.SUCCESSFUL)
                .transactionType(TransactionType.PAYMENT)
                .build());

        // Create payment for customer 2
        Payment pay2 = paymentRepository.save(Payment.builder()
                .paymentNumber("TXN-CUST2")
                .policy(otherCustomerPolicy)
                .amount(new BigDecimal("1000.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.SUCCESSFUL)
                .transactionType(TransactionType.PAYMENT)
                .build());

        // 1. Customer 1 retrieves payment 1: Success
        mockMvc.perform(get("/api/v1/payments/" + pay1.getId()).contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentNumber").value("TXN-CUST1"));

        // 2. Customer 1 retrieves payment 2: Forbidden (IDOR check)
        mockMvc.perform(get("/api/v1/payments/" + pay2.getId()).contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken))
                .andExpect(status().isForbidden());

        // 3. Customer 1 lists payments: Returns only payment 1 (size 1)
        mockMvc.perform(get("/api/v1/payments").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].paymentNumber").value("TXN-CUST1"));

        // 4. Admin lists payments: Returns both payments (size 2)
        mockMvc.perform(get("/api/v1/payments").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }
}

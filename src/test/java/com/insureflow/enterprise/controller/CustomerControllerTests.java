package com.insureflow.enterprise.controller;

import java.io.File;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insureflow.enterprise.dto.*;
import com.insureflow.enterprise.model.*;
import com.insureflow.enterprise.repository.*;
import com.insureflow.enterprise.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CustomerControllerTests {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String customerJwtToken;
    private String otherCustomerJwtToken;
    private String agentJwtToken;

    private User customerUser;
    private User otherCustomerUser;
    private User agentUser;

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
        Role customerRole = roleRepository.save(Role.builder().name(UserRole.CUSTOMER).build());
        Role agentRole = roleRepository.save(Role.builder().name(UserRole.AGENT).build());
        roleRepository.save(Role.builder().name(UserRole.CLAIM_OFFICER).build());
        roleRepository.save(Role.builder().name(UserRole.ADMIN).build());

        // Create Customer User
        customerUser = User.builder()
                .email("customer@insureflow.com")
                .password(passwordEncoder.encode("password123"))
                .fullName("John Owner")
                .roles(Collections.singleton(customerRole))
                .enabled(true)
                .build();
        userRepository.save(customerUser);

        // Create Other Customer User
        otherCustomerUser = User.builder()
                .email("other@insureflow.com")
                .password(passwordEncoder.encode("password123"))
                .fullName("Jane Stranger")
                .roles(Collections.singleton(customerRole))
                .enabled(true)
                .build();
        userRepository.save(otherCustomerUser);

        // Create Agent User
        agentUser = User.builder()
                .email("agent@insureflow.com")
                .password(passwordEncoder.encode("password123"))
                .fullName("Agent Smith")
                .roles(Collections.singleton(agentRole))
                .enabled(true)
                .build();
        userRepository.save(agentUser);

        // Generate JWT Tokens
        customerJwtToken = jwtUtils.generateJwtTokenFromUsername(customerUser.getEmail());
        otherCustomerJwtToken = jwtUtils.generateJwtTokenFromUsername(otherCustomerUser.getEmail());
        agentJwtToken = jwtUtils.generateJwtTokenFromUsername(agentUser.getEmail());
    }

    @Test
    void testCreateCustomerProfileSelfService() throws Exception {
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setPhone("+1234567890");
        request.setDateOfBirth(LocalDate.of(1990, 5, 15));

        AddressDto address = AddressDto.builder()
                .street("123 Main St")
                .city("New York")
                .state("NY")
                .zipCode("10001")
                .country("USA")
                .build();
        request.setAddress(address);

        NomineeDto nominee = NomineeDto.builder()
                .name("Mary Owner")
                .relationship("Spouse")
                .percentage(new BigDecimal("100.00"))
                .phone("+1987654321")
                .build();
        request.setNominees(Collections.singletonList(nominee));

        mockMvc.perform(post("/api/v1/customers").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("John Owner"))
                .andExpect(jsonPath("$.data.phone").value("+1234567890"))
                .andExpect(jsonPath("$.data.address.street").value("123 Main St"))
                .andExpect(jsonPath("$.data.nominees", hasSize(1)))
                .andExpect(jsonPath("$.data.nominees[0].name").value("Mary Owner"));
    }

    @Test
    void testGetCustomerProfileSecurityAndIdorChecks() throws Exception {
        // 1. Create a customer profile for John Owner
        Customer customer = Customer.builder()
                .user(customerUser)
                .phone("+1234567890")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .deleted(false)
                .build();
        customerRepository.save(customer);

        // 2. Customer fetching own profile: Success
        mockMvc.perform(get("/api/v1/customers/" + customer.getId()).contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("John Owner"));

        // 3. Agent fetching John Owner profile: Success
        mockMvc.perform(get("/api/v1/customers/" + customer.getId()).contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 4. Other customer fetching John Owner profile: Fails (IDOR blocker)
        mockMvc.perform(get("/api/v1/customers/" + customer.getId()).contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherCustomerJwtToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testUpdateCustomerNomineeValidation() throws Exception {
        Customer customer = Customer.builder()
                .user(customerUser)
                .phone("+1234567890")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .deleted(false)
                .build();
        customerRepository.save(customer);

        // Update request with over allocation (101.00% nominee allocation)
        CustomerUpdateRequest updateRequest = new CustomerUpdateRequest();
        updateRequest.setPhone("+1234567890");
        updateRequest.setDateOfBirth(LocalDate.of(1990, 5, 15));

        NomineeDto nominee1 = NomineeDto.builder()
                .name("Nominee A")
                .relationship("Son")
                .percentage(new BigDecimal("50.00"))
                .build();
        NomineeDto nominee2 = NomineeDto.builder()
                .name("Nominee B")
                .relationship("Daughter")
                .percentage(new BigDecimal("51.00")) // Sum = 101.00
                .build();
        ArrayList<NomineeDto> nominees = new ArrayList<>();
        nominees.add(nominee1);
        nominees.add(nominee2);
        updateRequest.setNominees(nominees);

        mockMvc.perform(put("/api/v1/customers/" + customer.getId()).contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Total nominee percentage allocation cannot exceed 100.00%"));
    }

    @Test
    void testUploadKycAndVerifyStatusUpdates() throws Exception {
        Customer customer = Customer.builder()
                .user(customerUser)
                .phone("+1234567890")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .deleted(false)
                .build();
        customerRepository.save(customer);

        // Upload KYC multipart file
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "kyc_doc.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "dummy pdf content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/customers/" + customer.getId() + "/kyc").file(file).contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Fetch user from DB, verify status is PENDING
        Customer saved = customerRepository.findById(customer.getId()).orElseThrow();
        assertEquals(KycStatus.PENDING, saved.getKycStatus());
        assertNotNull(saved.getKycDocumentPath());

        // Agent approves KYC
        mockMvc.perform(put("/api/v1/customers/" + customer.getId() + "/kyc/status").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentJwtToken)
                        .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kycStatus").value("APPROVED"));

        // Clean up file if created
        File doc = new File(saved.getKycDocumentPath());
        if (doc.exists()) {
            doc.delete();
        }
    }

    @Test
    void testSoftDeleteAndSearchCustomers() throws Exception {
        // Create profiles
        Customer customer1 = Customer.builder()
                .user(customerUser)
                .phone("+11111")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .deleted(false)
                .build();
        customerRepository.save(customer1);

        Customer customer2 = Customer.builder()
                .user(otherCustomerUser)
                .phone("+22222")
                .dateOfBirth(LocalDate.of(1995, 8, 20))
                .deleted(false)
                .build();
        customerRepository.save(customer2);

        // Search profiles as Agent - should return both
        mockMvc.perform(get("/api/v1/customers").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentJwtToken)
                        .param("query", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));

        // Search profiles as Customer - fails (pre-authorize blocks non-Agent)
        mockMvc.perform(get("/api/v1/customers").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerJwtToken))
                .andExpect(status().isForbidden());

        // Delete profile 2 (other customer)
        mockMvc.perform(delete("/api/v1/customers/" + customer2.getId()).contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Customer profile soft-deleted successfully"));

        // Search profiles again as Agent - should only return customer 1
        mockMvc.perform(get("/api/v1/customers").contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].fullName").value("John Owner"));
    }
}

package com.insureflow.enterprise.service;

import com.insureflow.enterprise.dto.*;
import com.insureflow.enterprise.exception.BusinessException;
import com.insureflow.enterprise.exception.ResourceNotFoundException;
import com.insureflow.enterprise.model.*;
import com.insureflow.enterprise.repository.CustomerRepository;
import com.insureflow.enterprise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.insureflow.enterprise.config.Auditable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    // Define local upload directory relative to project root
    private static final String UPLOAD_DIR = "uploads/kyc";

    @Auditable(action = "CREATE_CUSTOMER", entityType = "Customer")
    @Transactional
    public CustomerResponse createCustomer(CustomerCreateRequest request, String currentUserEmail) {
        User targetUser;
        
        if (request.getUserId() != null) {
            // If creating for another user, caller must be ADMIN or AGENT
            checkAdminOrAgentAccess();
            targetUser = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getUserId()));
        } else {
            // Default to logged-in user
            targetUser = userRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUserEmail));
        }

        if (customerRepository.existsByUserId(targetUser.getId())) {
            throw new BusinessException("Customer profile already exists for user: " + targetUser.getEmail());
        }

        Customer customer = Customer.builder()
                .user(targetUser)
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .kycStatus(KycStatus.PENDING)
                .deleted(false)
                .build();

        // Map Address
        if (request.getAddress() != null) {
            Address address = mapAddressDtoToEntity(request.getAddress());
            customer.setAddress(address);
        }

        // Map Nominees
        if (request.getNominees() != null && !request.getNominees().isEmpty()) {
            validateNomineesPercentage(request.getNominees());
            request.getNominees().forEach(dto -> {
                Nominee nominee = mapNomineeDtoToEntity(dto);
                customer.addNominee(nominee);
            });
        }

        Customer savedCustomer = customerRepository.save(customer);
        log.info("Created customer profile for user: {}", targetUser.getEmail());
        return mapCustomerToResponse(savedCustomer);
    }

    @Auditable(action = "UPDATE_CUSTOMER", entityType = "Customer")
    @Transactional
    public CustomerResponse updateCustomer(Long customerId, CustomerUpdateRequest request, String currentUserEmail) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        checkProfileAccess(customer, currentUserEmail);

        customer.setPhone(request.getPhone());
        customer.setDateOfBirth(request.getDateOfBirth());

        // Update or Create Address
        if (request.getAddress() != null) {
            if (customer.getAddress() != null) {
                updateAddressFromDto(customer.getAddress(), request.getAddress());
            } else {
                Address address = mapAddressDtoToEntity(request.getAddress());
                customer.setAddress(address);
            }
        } else {
            customer.setAddress(null);
        }

        // Update Nominees (Orphan removal handles DB deletion of removed items)
        if (request.getNominees() != null) {
            validateNomineesPercentage(request.getNominees());
            // Clear current list manually to trigger orphan removal
            customer.getNominees().clear();
            request.getNominees().forEach(dto -> {
                Nominee nominee = mapNomineeDtoToEntity(dto);
                customer.addNominee(nominee);
            });
        } else {
            customer.getNominees().clear();
        }

        Customer updatedCustomer = customerRepository.save(customer);
        log.info("Updated customer profile with ID: {}", customerId);
        return mapCustomerToResponse(updatedCustomer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long customerId, String currentUserEmail) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        checkProfileAccess(customer, currentUserEmail);
        return mapCustomerToResponse(customer);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> searchCustomers(String query, int page, int size) {
        checkAdminOrAgentAccess();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Customer> customers = customerRepository.searchCustomers(query, pageable);
        return customers.map(this::mapCustomerToResponse);
    }

    @Transactional
    public String uploadKycDocument(Long customerId, MultipartFile file, String currentUserEmail) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        checkProfileAccess(customer, currentUserEmail);

        if (file.isEmpty()) {
            throw new BusinessException("Cannot upload empty file");
        }

        try {
            // Ensure folder exists
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String fileExtension = "";
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = customerId + "_" + UUID.randomUUID() + fileExtension;
            Path filePath = uploadPath.resolve(fileName);

            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            customer.setKycDocumentPath(filePath.toString());
            customer.setKycStatus(KycStatus.PENDING);
            customerRepository.save(customer);

            log.info("Uploaded KYC file for customer ID {}: {}", customerId, filePath);
            return filePath.toString();
        } catch (IOException e) {
            log.error("Failed to store KYC file", e);
            throw new BusinessException("Failed to store KYC file: " + e.getMessage());
        }
    }

    @Transactional
    public CustomerResponse updateKycStatus(Long customerId, KycStatus status) {
        checkAdminOrAgentAccess();
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        customer.setKycStatus(status);
        Customer saved = customerRepository.save(customer);
        log.info("Updated KYC status of customer ID {} to: {}", customerId, status);
        return mapCustomerToResponse(saved);
    }

    @Auditable(action = "DELETE_CUSTOMER", entityType = "Customer")
    @Transactional
    public String softDeleteCustomer(Long customerId) {
        checkAdminOrAgentAccess();
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        customer.setDeleted(true);
        customerRepository.save(customer);
        log.info("Soft-deleted customer profile with ID: {}", customerId);
        return "Customer profile soft-deleted successfully";
    }

    // Security Authorization Helpers
    private void checkProfileAccess(Customer customer, String currentUserEmail) {
        if (customer.getUser().getEmail().equals(currentUserEmail)) {
            return; // Owner is authorized
        }

        boolean isAuthorized = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_AGENT"));

        if (!isAuthorized) {
            throw new AccessDeniedException("Access Denied: You do not have permission to access this customer profile.");
        }
    }

    private void checkAdminOrAgentAccess() {
        boolean isAuthorized = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_AGENT"));

        if (!isAuthorized) {
            throw new AccessDeniedException("Access Denied: Admin or Agent privileges required.");
        }
    }

    // Mapping Helper functions
    private Address mapAddressDtoToEntity(AddressDto dto) {
        return Address.builder()
                .street(dto.getStreet())
                .city(dto.getCity())
                .state(dto.getState())
                .zipCode(dto.getZipCode())
                .country(dto.getCountry())
                .build();
    }

    private void updateAddressFromDto(Address address, AddressDto dto) {
        address.setStreet(dto.getStreet());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setZipCode(dto.getZipCode());
        address.setCountry(dto.getCountry());
    }

    private Nominee mapNomineeDtoToEntity(NomineeDto dto) {
        return Nominee.builder()
                .name(dto.getName())
                .relationship(dto.getRelationship())
                .percentage(dto.getPercentage())
                .phone(dto.getPhone())
                .build();
    }

    private AddressDto mapAddressToDto(Address address) {
        if (address == null) return null;
        return AddressDto.builder()
                .id(address.getId())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .country(address.getCountry())
                .build();
    }

    private NomineeDto mapNomineeToDto(Nominee nominee) {
        return NomineeDto.builder()
                .id(nominee.getId())
                .name(nominee.getName())
                .relationship(nominee.getRelationship())
                .percentage(nominee.getPercentage())
                .phone(nominee.getPhone())
                .build();
    }

    private CustomerResponse mapCustomerToResponse(Customer customer) {
        AddressDto addressDto = mapAddressToDto(customer.getAddress());
        List<NomineeDto> nomineeDtos = customer.getNominees().stream()
                .map(this::mapNomineeToDto)
                .collect(Collectors.toList());

        return CustomerResponse.builder()
                .id(customer.getId())
                .userId(customer.getUser().getId())
                .email(customer.getUser().getEmail())
                .fullName(customer.getUser().getFullName())
                .phone(customer.getPhone())
                .dateOfBirth(customer.getDateOfBirth())
                .kycDocumentPath(customer.getKycDocumentPath())
                .kycStatus(customer.getKycStatus().name())
                .address(addressDto)
                .nominees(nomineeDtos)
                .build();
    }

    private void validateNomineesPercentage(List<NomineeDto> nominees) {
        BigDecimal totalPercentage = nominees.stream()
                .map(NomineeDto::getPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPercentage.compareTo(new BigDecimal("100.00")) > 0) {
            throw new BusinessException("Total nominee percentage allocation cannot exceed 100.00%");
        }
    }
}

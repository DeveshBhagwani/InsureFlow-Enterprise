package com.insureflow.enterprise.controller;

import com.insureflow.enterprise.dto.*;
import com.insureflow.enterprise.model.KycStatus;
import com.insureflow.enterprise.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Customer Management", description = "Endpoints to manage customer profiles, addresses, nominees, and KYC uploads")
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "Create customer profile for logged-in user or by AGENT/ADMIN")
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CustomerCreateRequest request,
            Authentication authentication) {
        CustomerResponse response = customerService.createCustomer(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Customer profile created successfully"));
    }

    @Operation(summary = "Update customer details")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerUpdateRequest request,
            Authentication authentication) {
        CustomerResponse response = customerService.updateCustomer(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Customer profile updated successfully"));
    }

    @Operation(summary = "Get customer details by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(
            @PathVariable Long id,
            Authentication authentication) {
        CustomerResponse response = customerService.getCustomerById(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Customer profile retrieved successfully"));
    }

    @Operation(summary = "Upload KYC document for customer")
    @PostMapping(value = "/{id}/kyc", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadKyc(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        String path = customerService.uploadKycDocument(id, file, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(path, "KYC document uploaded successfully"));
    }

    @Operation(summary = "Update customer KYC verification status (AGENT/ADMIN only)")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @PutMapping("/{id}/kyc/status")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateKycStatus(
            @PathVariable Long id,
            @RequestParam KycStatus status) {
        CustomerResponse response = customerService.updateKycStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(response, "KYC verification status updated successfully"));
    }

    @Operation(summary = "Search and list customer profiles (AGENT/ADMIN only)")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> searchCustomers(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<CustomerResponse> response = customerService.searchCustomers(query, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Customer search completed successfully"));
    }

    @Operation(summary = "Soft delete customer profile (AGENT/ADMIN only)")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteCustomer(@PathVariable Long id) {
        String result = customerService.softDeleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success(result, "Customer profile deleted successfully"));
    }
}

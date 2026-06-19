package com.insureflow.enterprise.controller;

import com.insureflow.enterprise.dto.*;
import com.insureflow.enterprise.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Policy Management", description = "Endpoints for policy issuance, renewals, cancellations, tracking, and search histories")
@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PolicyController {

    private final PolicyService policyService;

    @Operation(summary = "Issue/Create a new policy (AGENT/ADMIN only)")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<PolicyResponse>> createPolicy(
            @Valid @RequestBody PolicyCreateRequest request,
            Authentication authentication) {
        PolicyResponse response = policyService.createPolicy(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Policy issued successfully"));
    }

    @Operation(summary = "Renew an existing policy (AGENT/ADMIN only)")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @PutMapping("/{id}/renew")
    public ResponseEntity<ApiResponse<PolicyResponse>> renewPolicy(
            @PathVariable Long id,
            @Valid @RequestBody PolicyRenewRequest request,
            Authentication authentication) {
        PolicyResponse response = policyService.renewPolicy(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Policy renewed successfully"));
    }

    @Operation(summary = "Cancel a policy")
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<PolicyResponse>> cancelPolicy(
            @PathVariable Long id,
            @Valid @RequestBody PolicyCancelRequest request,
            Authentication authentication) {
        PolicyResponse response = policyService.cancelPolicy(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Policy cancelled successfully"));
    }

    @Operation(summary = "Get policy details by ID (Owner, AGENT, or ADMIN)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PolicyResponse>> getPolicy(
            @PathVariable Long id,
            Authentication authentication) {
        PolicyResponse response = policyService.getPolicyById(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Policy retrieved successfully"));
    }

    @Operation(summary = "List and search policies (filtered by customer profile, or complete search for AGENT/ADMIN)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PolicyResponse>>> listPolicies(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Page<PolicyResponse> response = policyService.listPolicies(query, page, size, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Policies retrieved successfully"));
    }

    @Operation(summary = "Calculate premium dynamically for prospective inputs")
    @PostMapping("/premium/calculate")
    public ResponseEntity<ApiResponse<PremiumCalculationResponse>> calculatePremium(
            @Valid @RequestBody PremiumCalculationRequest request) {
        PremiumCalculationResponse response = policyService.calculatePremium(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Premium calculated successfully"));
    }
}

package com.insureflow.enterprise.controller;

import com.insureflow.enterprise.dto.*;
import com.insureflow.enterprise.service.ClaimService;
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

@Tag(name = "Claim Management", description = "Endpoints for raising claims, uploading documents, tracking claim state transitions, and searching records")
@RestController
@RequestMapping("/claims")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ClaimController {

    private final ClaimService claimService;

    @Operation(summary = "Raise a new claim (CUSTOMER owner, AGENT or ADMIN)")
    @PostMapping
    public ResponseEntity<ApiResponse<ClaimResponse>> raiseClaim(
            @Valid @RequestBody ClaimRequest request,
            Authentication authentication) {
        ClaimResponse response = claimService.createClaim(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Claim raised successfully"));
    }

    @Operation(summary = "Upload supporting document for claim")
    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadClaimDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        String path = claimService.uploadClaimDocument(id, file, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(path, "Document uploaded successfully"));
    }

    @Operation(summary = "Move claim status to UNDER_REVIEW (CLAIM_OFFICER/ADMIN only)")
    @PreAuthorize("hasAnyRole('CLAIM_OFFICER', 'ADMIN')")
    @PutMapping("/{id}/review")
    public ResponseEntity<ApiResponse<ClaimResponse>> reviewClaim(
            @PathVariable Long id,
            @RequestBody ClaimTransitionRequest request,
            Authentication authentication) {
        ClaimResponse response = claimService.reviewClaim(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Claim status updated to UNDER_REVIEW"));
    }

    @Operation(summary = "Move claim status to INVESTIGATION (CLAIM_OFFICER/ADMIN only)")
    @PreAuthorize("hasAnyRole('CLAIM_OFFICER', 'ADMIN')")
    @PutMapping("/{id}/investigate")
    public ResponseEntity<ApiResponse<ClaimResponse>> investigateClaim(
            @PathVariable Long id,
            @RequestBody ClaimTransitionRequest request,
            Authentication authentication) {
        ClaimResponse response = claimService.investigateClaim(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Claim status updated to INVESTIGATION"));
    }

    @Operation(summary = "Move claim status to APPROVED (CLAIM_OFFICER/ADMIN only)")
    @PreAuthorize("hasAnyRole('CLAIM_OFFICER', 'ADMIN')")
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ClaimResponse>> approveClaim(
            @PathVariable Long id,
            @RequestBody ClaimTransitionRequest request,
            Authentication authentication) {
        ClaimResponse response = claimService.approveClaim(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Claim approved successfully"));
    }

    @Operation(summary = "Move claim status to REJECTED (CLAIM_OFFICER/ADMIN only)")
    @PreAuthorize("hasAnyRole('CLAIM_OFFICER', 'ADMIN')")
    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ClaimResponse>> rejectClaim(
            @PathVariable Long id,
            @RequestBody ClaimTransitionRequest request,
            Authentication authentication) {
        ClaimResponse response = claimService.rejectClaim(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Claim rejected successfully"));
    }

    @Operation(summary = "Get claim details by ID (Owner, AGENT, CLAIM_OFFICER or ADMIN)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClaimResponse>> getClaim(
            @PathVariable Long id,
            Authentication authentication) {
        ClaimResponse response = claimService.getClaimById(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Claim retrieved successfully"));
    }

    @Operation(summary = "List and search claims (filtered by customer profile, or complete search for staff)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ClaimResponse>>> listClaims(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Page<ClaimResponse> response = claimService.listClaims(query, page, size, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Claims retrieved successfully"));
    }
}

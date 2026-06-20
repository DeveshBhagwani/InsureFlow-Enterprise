package com.insureflow.enterprise.service;

import com.insureflow.enterprise.dto.*;
import com.insureflow.enterprise.exception.BusinessException;
import com.insureflow.enterprise.exception.ResourceNotFoundException;
import com.insureflow.enterprise.model.*;
import com.insureflow.enterprise.repository.ClaimHistoryRepository;
import com.insureflow.enterprise.repository.ClaimRepository;
import com.insureflow.enterprise.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimHistoryRepository claimHistoryRepository;
    private final PolicyRepository policyRepository;

    private static final String UPLOAD_DIR = "uploads/claims";

    @Transactional
    public ClaimResponse createClaim(ClaimRequest request, String currentUserEmail) {
        Policy policy = policyRepository.findById(request.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with ID: " + request.getPolicyId()));

        // Security check: Only customer owner, agent, or admin can raise a claim
        checkPolicyAccess(policy, currentUserEmail);

        // Validation rule: claimAmount <= policy.coverageAmount
        if (request.getClaimAmount().compareTo(policy.getCoverageAmount()) > 0) {
            throw new BusinessException("Claim amount cannot exceed the policy coverage amount of: " + policy.getCoverageAmount());
        }

        String claimNumber = "CLM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Claim claim = Claim.builder()
                .claimNumber(claimNumber)
                .policy(policy)
                .claimAmount(request.getClaimAmount())
                .description(request.getDescription())
                .status(ClaimStatus.SUBMITTED)
                .notes(request.getDescription())
                .build();

        Claim savedClaim = claimRepository.save(claim);

        // Write history
        ClaimHistory history = ClaimHistory.builder()
                .claim(savedClaim)
                .fromStatus(ClaimStatus.SUBMITTED) // starting from submitted
                .toStatus(ClaimStatus.SUBMITTED)
                .action("Claim Raised")
                .notes(request.getDescription())
                .changedBy(currentUserEmail)
                .build();
        claimHistoryRepository.save(history);

        log.info("Claim {} raised successfully for Policy ID {}", claimNumber, policy.getId());
        return mapClaimToResponse(savedClaim);
    }

    @Transactional
    public String uploadClaimDocument(Long claimId, MultipartFile file, String currentUserEmail) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with ID: " + claimId));

        checkClaimAccess(claim, currentUserEmail);

        if (file.isEmpty()) {
            throw new BusinessException("Cannot upload empty file");
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileExtension = "";
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = claimId + "_" + UUID.randomUUID() + fileExtension;
            Path filePath = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            claim.addDocumentPath(filePath.toString());
            claimRepository.save(claim);

            log.info("Uploaded document for Claim ID {}: {}", claimId, filePath);
            return filePath.toString();
        } catch (IOException e) {
            log.error("Failed to store claim document", e);
            throw new BusinessException("Failed to store claim document: " + e.getMessage());
        }
    }

    @Transactional
    public ClaimResponse reviewClaim(Long claimId, ClaimTransitionRequest request, String currentUserEmail) {
        checkClaimOfficerOrAdminAccess();
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with ID: " + claimId));

        ClaimStatus fromStatus = claim.getStatus();
        claim.review(request.getNotes(), currentUserEmail);
        claim.setNotes(request.getNotes());
        Claim saved = claimRepository.save(claim);

        recordHistoryLog(saved, fromStatus, ClaimStatus.UNDER_REVIEW, "Reviewed Claim", request.getNotes(), currentUserEmail);

        log.info("Claim ID {} transitioned to UNDER_REVIEW by {}", claimId, currentUserEmail);
        return mapClaimToResponse(saved);
    }

    @Transactional
    public ClaimResponse investigateClaim(Long claimId, ClaimTransitionRequest request, String currentUserEmail) {
        checkClaimOfficerOrAdminAccess();
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with ID: " + claimId));

        ClaimStatus fromStatus = claim.getStatus();
        claim.investigate(request.getNotes(), currentUserEmail);
        claim.setNotes(request.getNotes());
        Claim saved = claimRepository.save(claim);

        recordHistoryLog(saved, fromStatus, ClaimStatus.INVESTIGATION, "Investigated Claim", request.getNotes(), currentUserEmail);

        log.info("Claim ID {} transitioned to INVESTIGATION by {}", claimId, currentUserEmail);
        return mapClaimToResponse(saved);
    }

    @Transactional
    public ClaimResponse approveClaim(Long claimId, ClaimTransitionRequest request, String currentUserEmail) {
        checkClaimOfficerOrAdminAccess();
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with ID: " + claimId));

        ClaimStatus fromStatus = claim.getStatus();
        claim.approve(request.getNotes(), currentUserEmail);
        claim.setNotes(request.getNotes());
        Claim saved = claimRepository.save(claim);

        recordHistoryLog(saved, fromStatus, ClaimStatus.APPROVED, "Approved Claim", request.getNotes(), currentUserEmail);

        log.info("Claim ID {} transitioned to APPROVED by {}", claimId, currentUserEmail);
        return mapClaimToResponse(saved);
    }

    @Transactional
    public ClaimResponse rejectClaim(Long claimId, ClaimTransitionRequest request, String currentUserEmail) {
        checkClaimOfficerOrAdminAccess();
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with ID: " + claimId));

        ClaimStatus fromStatus = claim.getStatus();
        claim.reject(request.getNotes(), currentUserEmail);
        claim.setNotes(request.getNotes());
        Claim saved = claimRepository.save(claim);

        recordHistoryLog(saved, fromStatus, ClaimStatus.REJECTED, "Rejected Claim", request.getNotes(), currentUserEmail);

        log.info("Claim ID {} transitioned to REJECTED by {}", claimId, currentUserEmail);
        return mapClaimToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ClaimResponse getClaimById(Long claimId, String currentUserEmail) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with ID: " + claimId));

        checkClaimAccess(claim, currentUserEmail);
        return mapClaimToResponse(claim);
    }

    @Transactional(readOnly = true)
    public Page<ClaimResponse> listClaims(String query, int page, int size, String currentUserEmail) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        boolean isStaff = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                               a.getAuthority().equals("ROLE_AGENT") ||
                               a.getAuthority().equals("ROLE_CLAIM_OFFICER"));

        Page<Claim> claims;
        if (isStaff) {
            claims = claimRepository.searchClaims(query, pageable);
        } else {
            claims = claimRepository.findByCustomerUserEmail(currentUserEmail, pageable);
        }

        return claims.map(this::mapClaimToResponse);
    }

    // Access authorization helpers
    private void checkPolicyAccess(Policy policy, String currentUserEmail) {
        if (policy.getCustomer().getUser().getEmail().equals(currentUserEmail)) {
            return; // Owner access
        }

        boolean isAuthorized = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_AGENT"));

        if (!isAuthorized) {
            throw new AccessDeniedException("Access Denied: You do not have permission to perform operations on this policy.");
        }
    }

    private void checkClaimAccess(Claim claim, String currentUserEmail) {
        if (claim.getPolicy().getCustomer().getUser().getEmail().equals(currentUserEmail)) {
            return; // Owner access
        }

        boolean isAuthorized = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                               a.getAuthority().equals("ROLE_AGENT") ||
                               a.getAuthority().equals("ROLE_CLAIM_OFFICER"));

        if (!isAuthorized) {
            throw new AccessDeniedException("Access Denied: You do not have permission to access this claim.");
        }
    }

    private void checkClaimOfficerOrAdminAccess() {
        boolean isAuthorized = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_CLAIM_OFFICER"));

        if (!isAuthorized) {
            throw new AccessDeniedException("Access Denied: Claim Officer or Admin privileges required.");
        }
    }

    private void recordHistoryLog(Claim claim, ClaimStatus from, ClaimStatus to, String action, String notes, String actor) {
        ClaimHistory history = ClaimHistory.builder()
                .claim(claim)
                .fromStatus(from)
                .toStatus(to)
                .action(action)
                .notes(notes)
                .changedBy(actor)
                .build();
        claimHistoryRepository.save(history);
    }

    private ClaimResponse mapClaimToResponse(Claim claim) {
        List<ClaimHistory> historyList = claimHistoryRepository.findByClaimIdOrderByChangedAtDesc(claim.getId());

        List<ClaimHistoryDto> historyDtos = historyList.stream()
                .map(h -> ClaimHistoryDto.builder()
                        .id(h.getId())
                        .fromStatus(h.getFromStatus() != null ? h.getFromStatus().name() : null)
                        .toStatus(h.getToStatus().name())
                        .action(h.getAction())
                        .notes(h.getNotes())
                        .changedBy(h.getChangedBy())
                        .changedAt(h.getChangedAt())
                        .build())
                .collect(Collectors.toList());

        return ClaimResponse.builder()
                .id(claim.getId())
                .claimNumber(claim.getClaimNumber())
                .policyId(claim.getPolicy().getId())
                .policyNumber(claim.getPolicy().getPolicyNumber())
                .claimAmount(claim.getClaimAmount())
                .description(claim.getDescription())
                .status(claim.getStatus().name())
                .notes(claim.getNotes())
                .documentPaths(claim.getDocumentPaths())
                .createdAt(claim.getCreatedAt())
                .updatedAt(claim.getUpdatedAt())
                .history(historyDtos)
                .build();
    }
}

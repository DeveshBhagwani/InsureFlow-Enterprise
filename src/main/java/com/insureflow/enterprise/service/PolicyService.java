package com.insureflow.enterprise.service;

import com.insureflow.enterprise.dto.*;
import com.insureflow.enterprise.exception.BusinessException;
import com.insureflow.enterprise.exception.ResourceNotFoundException;
import com.insureflow.enterprise.model.*;
import com.insureflow.enterprise.repository.CustomerRepository;
import com.insureflow.enterprise.repository.PolicyHistoryRepository;
import com.insureflow.enterprise.repository.PolicyRepository;
import com.insureflow.enterprise.strategy.PremiumCalculationEngine;
import com.insureflow.enterprise.strategy.PremiumCalculationInput;
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

import com.insureflow.enterprise.config.Auditable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final CustomerRepository customerRepository;
    private final PolicyHistoryRepository policyHistoryRepository;
    private final PremiumCalculationEngine premiumCalculationEngine;

    @Auditable(action = "CREATE_POLICY", entityType = "Policy")
    @Transactional
    public PolicyResponse createPolicy(PolicyCreateRequest request, String currentUserEmail) {
        checkAdminOrAgentAccess();

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + request.getCustomerId()));

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BusinessException("Policy start date must be before end date");
        }

        PolicyType type;
        try {
            type = PolicyType.valueOf(request.getPolicyType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid policy type. Allowed types: HEALTH, VEHICLE, LIFE");
        }

        String policyNumber = "POL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Policy policy;
        switch (type) {
            case HEALTH:
                if (request.getHealthDetails() == null) {
                    throw new BusinessException("Health details must be provided for a HEALTH policy type");
                }
                policy = HealthPolicy.builder()
                        .deductible(request.getHealthDetails().getDeductible())
                        .coPayPercentage(request.getHealthDetails().getCoPayPercentage())
                        .preExistingConditions(request.getHealthDetails().getPreExistingConditions())
                        .build();
                break;
            case VEHICLE:
                if (request.getVehicleDetails() == null) {
                    throw new BusinessException("Vehicle details must be provided for a VEHICLE policy type");
                }
                policy = VehiclePolicy.builder()
                        .vehicleMake(request.getVehicleDetails().getVehicleMake())
                        .vehicleModel(request.getVehicleDetails().getVehicleModel())
                        .licensePlate(request.getVehicleDetails().getLicensePlate())
                        .vehicleValue(request.getVehicleDetails().getVehicleValue())
                        .build();
                break;
            case LIFE:
                if (request.getLifeDetails() == null) {
                    throw new BusinessException("Life details must be provided for a LIFE policy type");
                }
                policy = LifePolicy.builder()
                        .termYears(request.getLifeDetails().getTermYears())
                        .smoker(request.getLifeDetails().isSmoker())
                        .medicalHistorySummary(request.getLifeDetails().getMedicalHistorySummary())
                        .build();
                break;
            default:
                throw new BusinessException("Unhandled policy type");
        }

        // Map general attributes
        policy.setPolicyNumber(policyNumber);
        policy.setCustomer(customer);
        policy.setPolicyType(type);
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setPremiumAmount(request.getPremiumAmount());
        policy.setCoverageAmount(request.getCoverageAmount());
        policy.setStartDate(request.getStartDate());
        policy.setEndDate(request.getEndDate());

        Policy savedPolicy = policyRepository.save(policy);

        // Record history log
        recordHistoryLog(savedPolicy, PolicyStatus.ACTIVE, "Policy issued.", currentUserEmail);

        log.info("Policy {} successfully issued for Customer ID {}", policyNumber, customer.getId());
        return mapPolicyToResponse(savedPolicy);
    }

    @Auditable(action = "RENEW_POLICY", entityType = "Policy")
    @Transactional
    public PolicyResponse renewPolicy(Long policyId, PolicyRenewRequest request, String currentUserEmail) {
        checkAdminOrAgentAccess();

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with ID: " + policyId));

        if (policy.getStatus() == PolicyStatus.CANCELLED) {
            throw new BusinessException("Cancelled policies cannot be renewed");
        }

        if (request.getNewEndDate().isBefore(policy.getEndDate())) {
            throw new BusinessException("New end date must be after current policy end date");
        }

        policy.setEndDate(request.getNewEndDate());
        if (request.getUpdatedPremiumAmount() != null) {
            policy.setPremiumAmount(request.getUpdatedPremiumAmount());
        }

        // Revive status if EXPIRED
        policy.setStatus(PolicyStatus.ACTIVE);

        Policy savedPolicy = policyRepository.save(policy);
        
        // Log history entry
        recordHistoryLog(savedPolicy, PolicyStatus.ACTIVE, "Policy renewed until: " + request.getNewEndDate(), currentUserEmail);

        log.info("Policy ID {} successfully renewed", policyId);
        return mapPolicyToResponse(savedPolicy);
    }

    @Auditable(action = "CANCEL_POLICY", entityType = "Policy")
    @Transactional
    public PolicyResponse cancelPolicy(Long policyId, PolicyCancelRequest request, String currentUserEmail) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with ID: " + policyId));

        // Owner customer or Agent/Admin can cancel
        checkPolicyAccess(policy, currentUserEmail);

        if (policy.getStatus() == PolicyStatus.CANCELLED) {
            throw new BusinessException("Policy is already cancelled");
        }

        policy.setStatus(PolicyStatus.CANCELLED);
        Policy savedPolicy = policyRepository.save(policy);

        // Log history entry
        recordHistoryLog(savedPolicy, PolicyStatus.CANCELLED, "Policy cancelled. Reason: " + request.getReason(), currentUserEmail);

        log.info("Policy ID {} cancelled", policyId);
        return mapPolicyToResponse(savedPolicy);
    }

    @Transactional(readOnly = true)
    public PolicyResponse getPolicyById(Long policyId, String currentUserEmail) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with ID: " + policyId));

        checkPolicyAccess(policy, currentUserEmail);
        return mapPolicyToResponse(policy);
    }

    @Transactional(readOnly = true)
    public Page<PolicyResponse> listPolicies(String query, int page, int size, String currentUserEmail) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        boolean isAdminOrAgent = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_AGENT"));

        Page<Policy> policies;
        if (isAdminOrAgent) {
            // Search all records
            policies = policyRepository.searchPolicies(query, pageable);
        } else {
            // Customers see only their own policies
            policies = policyRepository.findByCustomerUserEmail(currentUserEmail, pageable);
        }

        return policies.map(this::mapPolicyToResponse);
    }

    @Transactional(readOnly = true)
    public PremiumCalculationResponse calculatePremium(PremiumCalculationRequest request) {
        PolicyType type;
        try {
            type = PolicyType.valueOf(request.getPolicyType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid policy type. Allowed types: HEALTH, VEHICLE, LIFE");
        }

        PremiumCalculationInput input = PremiumCalculationInput.builder()
                .age(request.getAge())
                .policyType(type)
                .riskScore(request.getRiskScore())
                .existingClaims(request.getExistingClaims())
                .occupation(request.getOccupation())
                .build();

        java.math.BigDecimal premium = premiumCalculationEngine.calculate(input);
        return new PremiumCalculationResponse(premium);
    }

    // Logging helper
    private void recordHistoryLog(Policy policy, PolicyStatus status, String notes, String user) {
        PolicyHistory history = PolicyHistory.builder()
                .policy(policy)
                .status(status)
                .notes(notes)
                .changedBy(user)
                .changedAt(LocalDateTime.now())
                .build();
        policyHistoryRepository.save(history);
    }

    // Access checks
    private void checkPolicyAccess(Policy policy, String currentUserEmail) {
        if (policy.getCustomer().getUser().getEmail().equals(currentUserEmail)) {
            return; // Owner access granted
        }

        boolean isAuthorized = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_AGENT"));

        if (!isAuthorized) {
            throw new AccessDeniedException("Access Denied: You do not have permission to access this policy profile.");
        }
    }

    private void checkAdminOrAgentAccess() {
        boolean isAuthorized = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_AGENT"));

        if (!isAuthorized) {
            throw new AccessDeniedException("Access Denied: Admin or Agent privileges required.");
        }
    }

    // Mapping Helpers
    private PolicyResponse mapPolicyToResponse(Policy policy) {
        PolicyResponse.PolicyResponseBuilder builder = PolicyResponse.builder()
                .id(policy.getId())
                .policyNumber(policy.getPolicyNumber())
                .customerId(policy.getCustomer().getId())
                .customerName(policy.getCustomer().getUser().getFullName())
                .customerEmail(policy.getCustomer().getUser().getEmail())
                .policyType(policy.getPolicyType().name())
                .status(policy.getStatus().name())
                .premiumAmount(policy.getPremiumAmount())
                .coverageAmount(policy.getCoverageAmount())
                .startDate(policy.getStartDate())
                .endDate(policy.getEndDate())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt());

        // Map subclass details conditionally
        if (policy instanceof HealthPolicy hp) {
            builder.healthDetails(PolicyResponse.HealthDetailsResponse.builder()
                    .deductible(hp.getDeductible())
                    .coPayPercentage(hp.getCoPayPercentage())
                    .preExistingConditions(hp.getPreExistingConditions())
                    .build());
        } else if (policy instanceof VehiclePolicy vp) {
            builder.vehicleDetails(PolicyResponse.VehicleDetailsResponse.builder()
                    .vehicleMake(vp.getVehicleMake())
                    .vehicleModel(vp.getVehicleModel())
                    .licensePlate(vp.getLicensePlate())
                    .vehicleValue(vp.getVehicleValue() != null ? vp.getVehicleValue().toString() : null)
                    .build());
        } else if (policy instanceof LifePolicy lp) {
            builder.lifeDetails(PolicyResponse.LifeDetailsResponse.builder()
                    .termYears(lp.getTermYears())
                    .smoker(lp.isSmoker())
                    .medicalHistorySummary(lp.getMedicalHistorySummary())
                    .build());
        }

        // Map History if available
        List<PolicyHistory> historyList = policyHistoryRepository.findByPolicyIdOrderByChangedAtDesc(policy.getId());
        if (historyList != null && !historyList.isEmpty()) {
            builder.history(historyList.stream()
                    .map(h -> PolicyHistoryDto.builder()
                            .id(h.getId())
                            .status(h.getStatus().name())
                            .notes(h.getNotes())
                            .changedBy(h.getChangedBy())
                            .changedAt(h.getChangedAt())
                            .build())
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}

package com.insureflow.enterprise.service;

import com.insureflow.enterprise.dto.*;
import com.insureflow.enterprise.model.*;
import com.insureflow.enterprise.repository.ClaimRepository;
import com.insureflow.enterprise.repository.CustomerRepository;
import com.insureflow.enterprise.repository.PolicyRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportingService {

    private final CustomerRepository customerRepository;
    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public ReportSummaryResponse getSummaryReport() {
        checkStaffAccess();

        long activePoliciesCount = policyRepository.countByStatus(PolicyStatus.ACTIVE);
        long totalClaimsCount = claimRepository.count();
        long totalCustomersCount = customerRepository.count();

        BigDecimal netRevenue = calculateNetRevenue();

        return ReportSummaryResponse.builder()
                .activePoliciesCount(activePoliciesCount)
                .totalClaimsCount(totalClaimsCount)
                .totalCustomersCount(totalCustomersCount)
                .totalRevenue(netRevenue)
                .build();
    }

    @Transactional(readOnly = true)
    public List<PolicyReportResponse> getPolicyReport() {
        checkStaffAccess();

        List<Object[]> results = policyRepository.findPolicyReportByStatus(PolicyStatus.ACTIVE);
        return results.stream()
                .map(row -> PolicyReportResponse.builder()
                        .policyType(((PolicyType) row[0]).name())
                        .count((Long) row[1])
                        .totalPremium((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClaimReportResponse> getClaimReport() {
        checkStaffAccess();

        List<Object[]> results = entityManager.createQuery(
                "SELECT c.status, COUNT(c), SUM(c.claimAmount) FROM Claim c GROUP BY c.status", Object[].class)
                .getResultList();

        return results.stream()
                .map(row -> ClaimReportResponse.builder()
                        .status(((ClaimStatus) row[0]).name())
                        .count((Long) row[1])
                        .totalAmount((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport() {
        checkStaffAccess();

        BigDecimal totalPremium = entityManager.createQuery(
                "SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status AND p.transactionType = :type", BigDecimal.class)
                .setParameter("status", PaymentStatus.SUCCESSFUL)
                .setParameter("type", TransactionType.PAYMENT)
                .getSingleResult();

        BigDecimal totalRefunds = entityManager.createQuery(
                "SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status AND p.transactionType = :type", BigDecimal.class)
                .setParameter("status", PaymentStatus.SUCCESSFUL)
                .setParameter("type", TransactionType.REFUND)
                .getSingleResult();

        BigDecimal netRevenue = totalPremium.subtract(totalRefunds);

        return RevenueReportResponse.builder()
                .totalPremiumCollected(totalPremium)
                .totalRefundsIssued(totalRefunds)
                .netRevenue(netRevenue)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CustomerGrowthResponse> getCustomerGrowthReport() {
        checkStaffAccess();

        List<LocalDateTime> signupDates = customerRepository.findAllSignupDates();

        Map<LocalDate, Long> growthMap = signupDates.stream()
                .collect(Collectors.groupingBy(LocalDateTime::toLocalDate, Collectors.counting()));

        return growthMap.entrySet().stream()
                .map(e -> CustomerGrowthResponse.builder()
                        .date(e.getKey().toString())
                        .signupsCount(e.getValue())
                        .build())
                .sorted(Comparator.comparing(CustomerGrowthResponse::getDate))
                .collect(Collectors.toList());
    }

    private BigDecimal calculateNetRevenue() {
        BigDecimal totalPremiumCollected = entityManager.createQuery(
                "SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status AND p.transactionType = :type", BigDecimal.class)
                .setParameter("status", PaymentStatus.SUCCESSFUL)
                .setParameter("type", TransactionType.PAYMENT)
                .getSingleResult();

        BigDecimal totalRefundsIssued = entityManager.createQuery(
                "SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status AND p.transactionType = :type", BigDecimal.class)
                .setParameter("status", PaymentStatus.SUCCESSFUL)
                .setParameter("type", TransactionType.REFUND)
                .getSingleResult();

        return totalPremiumCollected.subtract(totalRefundsIssued);
    }

    private void checkStaffAccess() {
        boolean isStaff = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_AGENT"));

        if (!isStaff) {
            throw new AccessDeniedException("Access Denied: You do not have permission to view reporting statistics.");
        }
    }
}

package com.insureflow.enterprise.service;

import com.insureflow.enterprise.dto.PaymentRequest;
import com.insureflow.enterprise.dto.PaymentResponse;
import com.insureflow.enterprise.dto.RefundRequest;
import com.insureflow.enterprise.exception.BusinessException;
import com.insureflow.enterprise.exception.ResourceNotFoundException;
import com.insureflow.enterprise.model.*;
import com.insureflow.enterprise.repository.PaymentRepository;
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

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PolicyRepository policyRepository;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request, String currentUserEmail) {
        Policy policy = policyRepository.findById(request.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with ID: " + request.getPolicyId()));

        checkPolicyAccess(policy, currentUserEmail);

        if (policy.getStatus() == PolicyStatus.CANCELLED) {
            throw new BusinessException("Cannot process premium payment for a CANCELLED policy.");
        }

        String paymentNumber = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        PaymentStatus status = request.isSimulateFailure() ? PaymentStatus.FAILED : PaymentStatus.SUCCESSFUL;
        String notes = request.isSimulateFailure() ? "Payment simulation failed" : "Premium payment successful";

        Payment payment = Payment.builder()
                .paymentNumber(paymentNumber)
                .policy(policy)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(status)
                .transactionType(TransactionType.PAYMENT)
                .notes(notes)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment transaction {} processed with status: {}", paymentNumber, status);
        return mapPaymentToResponse(saved);
    }

    @Transactional
    public PaymentResponse processRefund(RefundRequest request, String currentUserEmail) {
        checkAdminAccess();

        Payment originalPayment = paymentRepository.findById(request.getOriginalPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Original payment transaction not found with ID: " + request.getOriginalPaymentId()));

        if (originalPayment.getTransactionType() != TransactionType.PAYMENT ||
                originalPayment.getStatus() != PaymentStatus.SUCCESSFUL) {
            throw new BusinessException("Original transaction is not eligible for a refund. It must be a SUCCESSFUL payment transaction.");
        }

        if (request.getAmount().compareTo(originalPayment.getAmount()) > 0) {
            throw new BusinessException("Refund amount cannot exceed the original payment amount of: " + originalPayment.getAmount());
        }

        String refundNumber = "TXN-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Save refund transaction
        Payment refund = Payment.builder()
                .paymentNumber(refundNumber)
                .policy(originalPayment.getPolicy())
                .amount(request.getAmount())
                .paymentMethod(originalPayment.getPaymentMethod())
                .status(PaymentStatus.SUCCESSFUL)
                .transactionType(TransactionType.REFUND)
                .originalPayment(originalPayment)
                .notes(request.getReason())
                .build();

        Payment savedRefund = paymentRepository.save(refund);

        // Update status of original payment
        originalPayment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(originalPayment);

        log.info("Processed refund transaction {} for original payment ID {}", refundNumber, originalPayment.getId());
        return mapPaymentToResponse(savedRefund);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id, String currentUserEmail) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found with ID: " + id));

        checkPaymentAccess(payment, currentUserEmail);
        return mapPaymentToResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> listPayments(String query, int page, int size, String currentUserEmail) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        boolean isStaff = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                               a.getAuthority().equals("ROLE_AGENT") ||
                               a.getAuthority().equals("ROLE_CLAIM_OFFICER"));

        Page<Payment> payments;
        if (isStaff) {
            payments = paymentRepository.searchPayments(query, pageable);
        } else {
            payments = paymentRepository.findByCustomerUserEmail(currentUserEmail, pageable);
        }

        return payments.map(this::mapPaymentToResponse);
    }

    // Authorization helpers
    private void checkPolicyAccess(Policy policy, String currentUserEmail) {
        if (policy.getCustomer().getUser().getEmail().equals(currentUserEmail)) {
            return; // Owner access
        }

        boolean isAuthorized = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_AGENT"));

        if (!isAuthorized) {
            throw new AccessDeniedException("Access Denied: You do not have permission to pay premiums for this policy.");
        }
    }

    private void checkPaymentAccess(Payment payment, String currentUserEmail) {
        if (payment.getPolicy().getCustomer().getUser().getEmail().equals(currentUserEmail)) {
            return; // Owner access
        }

        boolean isAuthorized = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                               a.getAuthority().equals("ROLE_AGENT") ||
                               a.getAuthority().equals("ROLE_CLAIM_OFFICER"));

        if (!isAuthorized) {
            throw new AccessDeniedException("Access Denied: You do not have permission to access this payment transaction.");
        }
    }

    private void checkAdminAccess() {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new AccessDeniedException("Access Denied: Admin privileges required to process refunds.");
        }
    }

    private PaymentResponse mapPaymentToResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .paymentNumber(p.getPaymentNumber())
                .policyId(p.getPolicy().getId())
                .policyNumber(p.getPolicy().getPolicyNumber())
                .amount(p.getAmount())
                .paymentMethod(p.getPaymentMethod())
                .status(p.getStatus().name())
                .transactionType(p.getTransactionType().name())
                .notes(p.getNotes())
                .originalPaymentId(p.getOriginalPayment() != null ? p.getOriginalPayment().getId() : null)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}

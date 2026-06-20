package com.insureflow.enterprise.controller;

import com.insureflow.enterprise.dto.*;
import com.insureflow.enterprise.service.PaymentService;
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

@Tag(name = "Payment Management", description = "Endpoints for premium payments, history tracking, refunds, and transaction logging")
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Submit a premium payment (CUSTOMER owner, AGENT or ADMIN)")
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> payPremium(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        PaymentResponse response = paymentService.processPayment(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Premium payment processed successfully"));
    }

    @Operation(summary = "Process a refund (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> processRefund(
            @Valid @RequestBody RefundRequest request,
            Authentication authentication) {
        PaymentResponse response = paymentService.processRefund(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Refund processed successfully"));
    }

    @Operation(summary = "Get transaction details by ID (Owner, AGENT, CLAIM_OFFICER or ADMIN)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @PathVariable Long id,
            Authentication authentication) {
        PaymentResponse response = paymentService.getPaymentById(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Payment transaction retrieved successfully"));
    }

    @Operation(summary = "List and search payment transactions (filtered by customer profile, or complete search for staff)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> listPayments(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Page<PaymentResponse> response = paymentService.listPayments(query, page, size, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Payments retrieved successfully"));
    }
}
